# Vivi Music Wear OS - Full Implementation Plan

## Project Overview
Creating a standalone Wear OS module (`:wearos`) for Vivi Music that runs on Xiaomi Watch 5, reusing the existing `:innertube` module for YouTube Music API access. The module will be completely isolated from the phone app (`:app`) with minimal modifications to the existing codebase.

## Architecture Principles

### Module Isolation
- **`:wearos`** - New standalone Wear OS application module
- **`:innertube`** - Existing API/network layer (read-only dependency, ✅ verified clean - no Android UI deps)
- **`:app`** - Phone application (minimal edits - only auth export button)

### Key Constraints
1. `:wearos` depends ONLY on `:innertube`, never on `:app`
2. No modifications to existing modules except one small auth export feature in `:app`
3. Bluetooth audio output enforcement (no watch speaker playback)
4. Aggressive feature stripping for watch constraints
5. Standalone operation with optional phone companion for auth sync

---

## Design Decisions Required

### 1. Package Naming & Application ID
**Question:** What package structure should we use for the Wear OS module?

**Options:**
- **A)** `com.music.vivi.wear` (namespace) / `com.vivi.vivimusic.wear` (applicationId)
  - Pros: Consistent with existing app structure
  - Cons: None
- **B)** Completely separate package like `com.vivi.wear`
  - Pros: Maximum isolation
  - Cons: Less clear relationship to main app

**Recommendation:** Option A for consistency

---

### 2. Database Strategy
**Question:** Should the Wear OS app have its own database or share/sync with the phone app?

**Options:**
- **A)** Separate, simplified database schema for Wear OS
  - Pros: Complete independence, optimized for watch use cases, no sync complexity
  - Cons: Downloaded songs/playlists not automatically synced from phone
  - Tables needed: Songs, Playlists, PlaylistSongMap, Downloads, PlaybackHistory
  
- **B)** Attempt to reuse existing MusicDatabase from `:app`
  - Pros: Potential data sharing
  - Cons: Violates module isolation, pulls in `:app` dependencies, complex migrations
  
- **C)** Hybrid: Minimal local cache + fetch from `:innertube` on demand
  - Pros: Simplest, most independent
  - Cons: Requires network for most operations

**Recommendation:** Option A - Separate simplified database
- Create `WearMusicDatabase` with only essential entities
- Keep downloaded tracks metadata, playback history, and user playlists
- Fetch everything else from `:innertube` on demand

---

### 3. Authentication Flow
**Question:** How should we handle the auth handoff from phone to watch?

**Implementation Plan (based on vivi-wearos.md Phase 5):**
1. **Phone app (`:app`) modification:**
   - Add single button in Settings: "Send login to watch"
   - Uses Wearable Data Layer API to send cookie
   - Path: `/vivi/auth` with encrypted cookie payload

2. **Watch app (`:wearos`):**
   - `WearableListenerService` receives auth data
   - Store in `EncryptedSharedPreferences` (AES256_GCM)
   - Inject into `:innertube` YouTube client calls

3. **Session expiration handling:**
   - Detect 401/403 responses from `:innertube`
   - Show clear "Reconnect" prompt
   - User taps "Send login to watch" button on phone again

**Question:** Should we also support watch-native login as fallback?
- **Recommendation:** No for v1.0 - too complex for watch UI, phone handoff is sufficient

---

### 4. Offline Downloads Strategy
**Question:** How should downloads work?

**Options:**
- **A)** Watch downloads directly from YouTube Music via `:innertube`
  - Pros: Truly standalone, works without phone
  - Cons: Uses watch battery/network
  
- **B)** Phone downloads and transfers to watch via Data Layer
  - Pros: More efficient, leverages phone's better connectivity
  - Cons: Requires phone nearby, doesn't help when phone is left behind
  
- **C)** Both (phone-assisted when available, direct when standalone)
  - Pros: Best of both worlds
  - Cons: More complex

**Recommendation:** Option A for v1.0, Option C for v2.0
- v1.0: Watch downloads directly using Media3 DownloadManager
- v2.0: Add phone-assisted transfer as optimization

**Download Management:**
- Max 2 parallel downloads (battery/thermal consideration)
- User-initiated downloads only (no auto-download)
- Separate cache pools:
  - Stream cache: 200MB LRU eviction
  - Explicit downloads: Persist until user deletes
- Downloads manager UI screen for management

---

### 5. UI Framework & Components
**Question:** Which Wear OS UI approach should we use?

**Recommendation:** Horologist Media Toolkit (as per vivi-wearos.md)
- `horologist-media-ui` for player screens
- `horologist-media-data` for player repository
- `horologist-media3-backend` for Bluetooth enforcement
- `horologist-network-awareness` for connectivity detection
- `horologist-compose-layout` for round display layouts

**Screen Structure:**
1. **Now Playing** (default launch screen if recently played)
   - Album art (static, no animations)
   - Track title + artist
   - Play/pause, skip controls
   - Progress bar
   - Volume control
   
2. **Library/Browse**
   - Search (voice input via RecognizerIntent)
   - Playlists
   - Recent plays
   - Downloaded tracks
   
3. **Downloads Manager**
   - List of downloaded tracks
   - Download progress
   - Delete actions
   - Storage usage indicator
   
4. **Settings**
   - Audio quality selector
   - Storage cleanup
   - Auth status / reconnect
   - About/version

5. **Tile** (quick access from watch face)
   - Current track
   - Play/pause/skip buttons

---

### 6. Bluetooth Audio Enforcement
**Question:** How strict should the Bluetooth requirement be?

**Implementation (as per vivi-wearos.md Phase 3.2):**
- Use Horologist's built-in Bluetooth check (primary method)
- Block play action if no BT audio device connected
- Auto-pause if BT disconnects mid-playback
- Show system Bluetooth settings prompt

**Supported device types:**
- `TYPE_BLUETOOTH_A2DP`
- `TYPE_BLUETOOTH_SCO`
- `TYPE_BLE_HEADSET`
- `TYPE_BLE_SPEAKER`

**Question:** Should we allow override for testing?
- **Recommendation:** Debug build only - add developer option to bypass check

---

### 7. Feature Stripping (NOT implementing)
Based on vivi-wearos.md Phase 2, explicitly EXCLUDE:

| Feature | Reason |
|---------|--------|
| Synced/karaoke lyrics | Static text only, no timed sync |
| Equalizer / audio effects | Default ExoPlayer audio sink only |
| Animated canvas / visualizers | Static album art only |
| Discord RPC / Last.fm scrobbling | No persistent network connections |
| Google Cast | No casting from watch |
| Home screen widgets | N/A on Wear OS |
| Android Auto | N/A on Wear OS |

---

### 8. Connectivity Modes
**Three states to handle (vivi-wearos.md Phase 6.1):**

1. **Bluetooth-relayed internet** (phone nearby)
   - Transparent to app, but slower
   - Use lower bitrate streaming
   
2. **Standalone Wi-Fi** (no phone needed)
   - Full speed streaming
   
3. **No connectivity** (offline mode)
   - Only downloaded tracks playable
   - Clear UI indication

**Implementation:**
- Use `horologist-network-awareness` to detect state
- Adjust streaming quality based on connection type
- Show connectivity status in UI
- Graceful degradation to offline mode

---

### 9. Storage & Cleanup Strategy
**Question:** How should we manage limited watch storage?

**Implementation (vivi-wearos.md Phase 7):**

1. **Cache Pools:**
   - Stream cache: 200MB, LRU eviction
   - Downloads: User-controlled, no auto-eviction
   
2. **Periodic Cleanup Worker:**
   - Runs weekly when battery not low + device idle
   - Removes orphaned cache entries
   - Notifies about stale downloads (90+ days unplayed)
   - Enforces user-configured download size cap
   
3. **Manual Cleanup:**
   - "Clean up now" button in Settings
   - Per-track delete in Downloads screen
   
4. **Download Validation:**
   - Periodically check if tracks still available upstream
   - Flag/remove dead entries

**Question:** Default download size cap?
- **Recommendation:** 1GB default, user-configurable (500MB - 3GB range)

---

### 10. Battery Optimization
**Implementation Guidelines (vivi-wearos.md Phase 7.4):**

- Wake lock only during active streaming, not local playback
- No polling loops - event-driven updates only
- Network maintenance only on unmetered connections
- No GPU-intensive effects (blur, palette extraction)
- Test with HyperOS battery management
- Document need for battery optimization exemption (sideloaded app)

---

## Implementation Phases

### Phase 0: Prerequisites ✓
- [x] Verify `:innertube` has no Android UI dependencies
- [x] Set up Wear OS emulator (API 33/34, Large Round)
- [ ] Enable ADB on Xiaomi Watch 5

### Phase 1: Module Scaffold
**Tasks:**
1. Add `:wearos` to `settings.gradle.kts`
2. Create `wearos/build.gradle.kts` with dependencies
3. Create `AndroidManifest.xml` with standalone config
4. Verify module isolation (`./gradlew :wearos:dependencies | grep ":app"` returns nothing)
5. Create basic MainActivity with "Hello Wear OS" screen
6. Test installation on emulator

**Files to create:**
- `wearos/build.gradle.kts`
- `wearos/src/main/AndroidManifest.xml`
- `wearos/src/main/kotlin/com/music/vivi/wear/MainActivity.kt`
- `wearos/proguard-rules.pro`

### Phase 2: Database Layer
**Tasks:**
1. Define simplified database schema
2. Create Room entities (Song, Playlist, Download, etc.)
3. Create DAOs
4. Create WearMusicDatabase
5. Add database migrations support

**Entities needed:**
- `WearSongEntity` (id, title, artist, album, thumbnailUrl, duration)
- `WearPlaylistEntity` (id, name, createdAt)
- `WearPlaylistSongMap` (playlistId, songId, position)
- `WearDownloadEntity` (songId, filePath, downloadedAt, size)
- `WearPlaybackHistoryEntity` (songId, playedAt, completionPercent)

### Phase 3: Playback Engine
**Tasks:**
1. Create `WearPlaybackService` extending `MediaSessionService`
2. Set up ExoPlayer with proper audio attributes
3. Implement Bluetooth audio enforcement (Horologist)
4. Create `BluetoothRouteObserver` for disconnect handling
5. Implement Media3 session callbacks
6. Test playback with BT headphones

**Files to create:**
- `playback/WearPlaybackService.kt`
- `playback/WearSessionCallback.kt`
- `playback/BluetoothAudioGate.kt`
- `playback/BluetoothRouteObserver.kt`

### Phase 4: InnerTube Integration
**Tasks:**
1. Create repository layer wrapping `:innertube` YouTube client
2. Implement auth injection from EncryptedSharedPreferences
3. Create data models for UI layer
4. Implement search functionality
5. Implement stream URL resolution
6. Handle 401/403 auth errors

**Files to create:**
- `data/WearYouTubeRepository.kt`
- `data/WearAuthManager.kt`
- `data/models/WearSong.kt`
- `data/models/WearPlaylist.kt`

### Phase 5: UI - Now Playing Screen
**Tasks:**
1. Set up Horologist PlayerViewModel
2. Create Now Playing screen with Horologist components
3. Implement playback controls
4. Add album art display (static)
5. Add progress bar
6. Implement volume control
7. Test with rotary input

**Files to create:**
- `ui/player/NowPlayingScreen.kt`
- `ui/player/WearPlayerViewModel.kt`
- `ui/theme/WearTheme.kt`

### Phase 6: UI - Library & Search
**Tasks:**
1. Create Library screen with ScalingLazyColumn
2. Implement voice search integration
3. Create search results screen
4. Implement playlist browsing
5. Add recent plays section
6. Test navigation flow

**Files to create:**
- `ui/library/LibraryScreen.kt`
- `ui/library/SearchScreen.kt`
- `ui/library/PlaylistScreen.kt`

### Phase 7: Downloads System
**Tasks:**
1. Set up Media3 DownloadManager
2. Create separate cache pools (stream vs. downloads)
3. Implement download UI screen
4. Add download progress tracking
5. Implement delete functionality
6. Add storage usage indicator

**Files to create:**
- `download/WearDownloadManager.kt`
- `download/WearDownloadService.kt`
- `ui/downloads/DownloadsScreen.kt`

### Phase 8: Phone Auth Sync
**Tasks:**
1. Add "Send login to watch" button in `:app` Settings
2. Implement Wearable Data Layer sender in `:app`
3. Create `AuthSyncListenerService` in `:wearos`
4. Implement EncryptedSharedPreferences storage
5. Add auth status UI in Settings
6. Test auth handoff flow

**Files to create in `:app`:**
- Modification to existing Settings screen (minimal)
- `wear/WearAuthSyncHelper.kt` (new file)

**Files to create in `:wearos`:**
- `auth/AuthSyncListenerService.kt`
- `auth/WearAuthStorage.kt`

### Phase 9: Connectivity & Network Awareness
**Tasks:**
1. Integrate horologist-network-awareness
2. Implement connectivity state detection
3. Add adaptive bitrate selection
4. Implement offline mode UI
5. Add connectivity status indicator
6. Test all three connectivity modes

**Files to create:**
- `network/ConnectivityManager.kt`
- `network/NetworkStateObserver.kt`

### Phase 10: Storage Cleanup
**Tasks:**
1. Create LibraryCleanupWorker
2. Implement orphaned cache cleanup
3. Add stale download detection
4. Implement size cap enforcement
5. Add manual cleanup UI
6. Schedule periodic cleanup

**Files to create:**
- `worker/LibraryCleanupWorker.kt`
- `ui/settings/StorageScreen.kt`

### Phase 11: Settings & Configuration
**Tasks:**
1. Create Settings screen
2. Add audio quality selector
3. Add download size cap setting
4. Add auth reconnect option
5. Add about/version info
6. Add manual cleanup trigger

**Files to create:**
- `ui/settings/SettingsScreen.kt`
- `ui/settings/SettingsViewModel.kt`

### Phase 12: Tile Implementation
**Tasks:**
1. Create Wear OS Tile
2. Add current track display
3. Add playback controls
4. Implement tile updates
5. Test from watch face

**Files to create:**
- `tile/WearMusicTile.kt`
- `tile/WearMusicTileService.kt`

### Phase 13: Testing & Polish
**Tasks:**
1. Run full test matrix (vivi-wearos.md Phase 9)
2. Test on Xiaomi Watch 5 hardware
3. Test HyperOS battery management
4. Fix bugs and edge cases
5. Performance optimization
6. UI polish

### Phase 14: Build & Distribution
**Tasks:**
1. Set up release signing
2. Create GitHub Actions workflow
3. Generate release APK
4. Create release notes
5. Publish to GitHub Releases
6. Document installation instructions

**Files to create:**
- `.github/workflows/build-wear.yml`
- `wearos/release.keystore` (gitignored)
- Release documentation

---

## Dependency Versions

Based on existing `libs.versions.toml`:

```toml
[versions]
# Existing versions to reuse
kotlin = "2.3.10"
media3 = "1.7.1"
room = "2.8.4"
ktor = "3.4.0"
ksp = "2.3.6"
coil = "3.3.0"
work = "2.10.0"

# New versions for Wear OS
wearCompose = "1.4.0"
horologist = "0.7.15"
playServicesWearable = "18.2.0"
securityCrypto = "1.1.0-alpha06"
```

---

## File Structure

```
wearos/
├── build.gradle.kts
├── proguard-rules.pro
└── src/
    └── main/
        ├── AndroidManifest.xml
        ├── kotlin/com/music/vivi/wear/
        │   ├── MainActivity.kt
        │   ├── WearApplication.kt
        │   ├── auth/
        │   │   ├── AuthSyncListenerService.kt
        │   │   └── WearAuthStorage.kt
        │   ├── data/
        │   │   ├── WearYouTubeRepository.kt
        │   │   ├── WearAuthManager.kt
        │   │   └── models/
        │   ├── db/
        │   │   ├── WearMusicDatabase.kt
        │   │   ├── WearDatabaseDao.kt
        │   │   └── entities/
        │   ├── download/
        │   │   ├── WearDownloadManager.kt
        │   │   └── WearDownloadService.kt
        │   ├── network/
        │   │   ├── ConnectivityManager.kt
        │   │   └── NetworkStateObserver.kt
        │   ├── playback/
        │   │   ├── WearPlaybackService.kt
        │   │   ├── WearSessionCallback.kt
        │   │   ├── BluetoothAudioGate.kt
        │   │   └── BluetoothRouteObserver.kt
        │   ├── tile/
        │   │   └── WearMusicTile.kt
        │   ├── ui/
        │   │   ├── player/
        │   │   │   ├── NowPlayingScreen.kt
        │   │   │   └── WearPlayerViewModel.kt
        │   │   ├── library/
        │   │   │   ├── LibraryScreen.kt
        │   │   │   ├── SearchScreen.kt
        │   │   │   └── PlaylistScreen.kt
        │   │   ├── downloads/
        │   │   │   └── DownloadsScreen.kt
        │   │   ├── settings/
        │   │   │   ├── SettingsScreen.kt
        │   │   │   └── StorageScreen.kt
        │   │   └── theme/
        │   │       └── WearTheme.kt
        │   └── worker/
        │       └── LibraryCleanupWorker.kt
        └── res/
            ├── values/
            │   ├── strings.xml
            │   └── themes.xml
            └── mipmap-*/
                └── ic_launcher.*
```

---

## Testing Checklist

### Functional Testing
- [ ] Fresh watch, no phone paired - app installs standalone
- [ ] No BT device - play blocked with prompt
- [ ] BT earbuds connected - playback works
- [ ] BT disconnect mid-song - auto-pause
- [ ] Watch on Wi-Fi, no phone - streaming works
- [ ] No connectivity - only downloads play
- [ ] Screen off - playback continues
- [ ] System media controls work
- [ ] Long session (1hr+) - no service kill
- [ ] Auth expires - clear reconnect prompt
- [ ] Low storage - cleanup works
- [ ] Voice search works
- [ ] Rotary input works
- [ ] Tile works from watch face

### Performance Testing
- [ ] Battery drain acceptable
- [ ] No polling loops
- [ ] Smooth UI on watch hardware
- [ ] Download speed reasonable
- [ ] Stream buffering minimal

### HyperOS Specific
- [ ] Battery optimization exemption documented
- [ ] Foreground service survives HyperOS management
- [ ] No unexpected kills during playback

---

## Open Questions for Discussion

### 1. Branding & Naming
- App name: "Vivi Wear" or "Vivi Music for Wear OS"?
- Icon: Reuse phone app icon or create watch-specific variant?

### 2. Feature Prioritization
- Should we include playlist creation/editing on watch, or read-only?
- Should we support queue management (reorder, remove)?
- Should we show lyrics at all (static only)?

### 3. Audio Quality Defaults
- Default to lowest bitrate for battery/bandwidth?
- Or let user choose on first launch?

### 4. Sync Features (Future)
- Should we sync playback position between phone and watch?
- Should we sync playlists?
- Should we sync download lists?

### 5. Testing Strategy
- Should we set up automated UI tests?
- What's the minimum test coverage target?

---

## Timeline Estimate

Assuming single developer, part-time work:

- **Phase 0-1:** 1 week (scaffold & setup)
- **Phase 2-3:** 2 weeks (database & playback)
- **Phase 4:** 1 week (InnerTube integration)
- **Phase 5-6:** 2 weeks (UI - player & library)
- **Phase 7:** 1 week (downloads)
- **Phase 8:** 1 week (auth sync)
- **Phase 9-10:** 1 week (network & cleanup)
- **Phase 11-12:** 1 week (settings & tile)
- **Phase 13:** 2 weeks (testing & polish)
- **Phase 14:** 1 week (build & release)

**Total: ~13 weeks (3 months)**

---

## Success Criteria

### MVP (v1.0)
- [ ] Standalone Wear OS app installs and runs
- [ ] Bluetooth audio enforcement works
- [ ] Can search and play music from YouTube Music
- [ ] Can download tracks for offline playback
- [ ] Auth syncs from phone app
- [ ] Basic playback controls work
- [ ] System media controls integration works
- [ ] Handles all three connectivity modes
- [ ] Storage cleanup works
- [ ] No crashes on Xiaomi Watch 5

### Future Enhancements (v2.0+)
- [ ] Phone-assisted downloads
- [ ] Playback position sync
- [ ] Playlist sync
- [ ] Material 3 UI upgrade (Horologist 0.8.x)
- [ ] Watch face complication
- [ ] Improved voice search
- [ ] Smart download suggestions
- [ ] Background sync optimization

---

## Risk Mitigation

### Technical Risks
1. **Horologist API changes**
   - Mitigation: Pin to stable version (0.7.15), monitor releases
   
2. **InnerTube API changes**
   - Mitigation: Reuse existing `:innertube` module, benefits from upstream fixes
   
3. **HyperOS battery management too aggressive**
   - Mitigation: Document exemption requirement, test thoroughly
   
4. **Watch storage too limited**
   - Mitigation: Aggressive cleanup, clear storage indicators

### User Experience Risks
1. **Auth expires frequently**
   - Mitigation: Clear reconnect flow, persistent notification option
   
2. **Bluetooth disconnects common**
   - Mitigation: Auto-pause, clear reconnect prompt
   
3. **Download management confusing**
   - Mitigation: Clear storage indicators, simple delete UI

---

## Next Steps

1. **Review this plan** - Discuss design decisions and open questions
2. **Finalize decisions** - Lock in package names, database strategy, etc.
3. **Set up development environment** - Emulator, watch ADB access
4. **Start Phase 1** - Create module scaffold
5. **Iterate** - Build, test, refine

---

## Notes

- This plan follows the vivi-wearos.md roadmap closely
- All design decisions are based on Wear OS best practices
- Module isolation is strictly maintained
- Feature set is intentionally minimal for v1.0
- Extensibility is built in for future enhancements
