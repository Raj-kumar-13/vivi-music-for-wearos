# Vivi Music Wear OS - Architecture Overview

## Module Structure

```
┌─────────────────────────────────────────────────────────────┐
│                     Vivi Music Project                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  ┌──────────────┐         ┌──────────────┐                 │
│  │   :app       │         │  :wearos     │                 │
│  │  (Phone)     │         │  (Watch)     │                 │
│  │              │         │              │                 │
│  │  - Full UI   │         │  - Wear UI   │                 │
│  │  - Features  │         │  - Minimal   │                 │
│  │  - Auth      │         │  - Playback  │                 │
│  └──────┬───────┘         └──────┬───────┘                 │
│         │                        │                          │
│         │                        │                          │
│         └────────┬───────────────┘                          │
│                  │                                          │
│                  ▼                                          │
│         ┌────────────────┐                                 │
│         │  :innertube    │                                 │
│         │                │                                 │
│         │  - YouTube API │                                 │
│         │  - Network     │                                 │
│         │  - Models      │                                 │
│         └────────────────┘                                 │
│                                                              │
└─────────────────────────────────────────────────────────────┘

Auth Sync (Wearable Data Layer):
:app ──────────────────────> :wearos
     (Send login to watch)
```

## Dependency Rules

### ✅ Allowed
- `:wearos` → `:innertube` (read-only)
- `:app` → `:innertube` (existing)
- `:app` → Wearable Data Layer → `:wearos` (auth sync only)

### ❌ Forbidden
- `:wearos` → `:app` (NEVER)
- `:wearos` → `:kizzy`, `:canvas`, `:artistvideo`, etc. (NEVER)
- `:innertube` → anything (stays pure)

## Layer Architecture (`:wearos` module)

```
┌─────────────────────────────────────────────────────────────┐
│                         UI Layer                             │
├─────────────────────────────────────────────────────────────┤
│  Compose Screens (Horologist Components)                    │
│  - NowPlayingScreen                                          │
│  - LibraryScreen                                             │
│  - SearchScreen                                              │
│  - DownloadsScreen                                           │
│  - SettingsScreen                                            │
│  - Tile                                                      │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                     ViewModel Layer                          │
├─────────────────────────────────────────────────────────────┤
│  - WearPlayerViewModel (extends Horologist PlayerViewModel) │
│  - LibraryViewModel                                          │
│  - DownloadsViewModel                                        │
│  - SettingsViewModel                                         │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Repository Layer                          │
├─────────────────────────────────────────────────────────────┤
│  - WearYouTubeRepository (wraps :innertube)                 │
│  - WearDownloadRepository                                    │
│  - WearAuthManager                                           │
│  - PlayerRepository (Horologist)                             │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                      Data Layer                              │
├─────────────────────────────────────────────────────────────┤
│  Local:                          Remote:                     │
│  - WearMusicDatabase (Room)      - :innertube (YouTube)     │
│  - EncryptedSharedPreferences    - Wearable Data Layer      │
│  - Media3 DownloadManager                                    │
│  - SimpleCache (stream + downloads)                          │
└─────────────────────────┬───────────────────────────────────┘
                          │
┌─────────────────────────▼───────────────────────────────────┐
│                    Playback Layer                            │
├─────────────────────────────────────────────────────────────┤
│  - WearPlaybackService (MediaSessionService)                │
│  - ExoPlayer                                                 │
│  - MediaSession                                              │
│  - BluetoothAudioGate (Horologist)                          │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow

### 1. Search & Play Flow
```
User Input (Voice/Text)
    │
    ▼
SearchScreen
    │
    ▼
LibraryViewModel
    │
    ▼
WearYouTubeRepository
    │
    ▼
:innertube (YouTube.search())
    │
    ▼
Display Results
    │
User Selects Track
    │
    ▼
WearPlayerViewModel
    │
    ▼
PlayerRepository (Horologist)
    │
    ▼
WearPlaybackService
    │
    ▼
ExoPlayer (with BluetoothAudioGate)
    │
    ▼
Bluetooth Headphones 🎧
```

### 2. Download Flow
```
User Taps Download
    │
    ▼
DownloadsScreen
    │
    ▼
DownloadsViewModel
    │
    ▼
WearDownloadRepository
    │
    ▼
WearYouTubeRepository (get stream URL)
    │
    ▼
:innertube (YouTube.player())
    │
    ▼
Media3 DownloadManager
    │
    ▼
Download to SimpleCache
    │
    ▼
Save metadata to WearMusicDatabase
    │
    ▼
Update UI with progress
```

### 3. Auth Sync Flow
```
Phone App (:app)
    │
User Taps "Send login to watch"
    │
    ▼
WearAuthSyncHelper
    │
    ▼
Wearable Data Layer API
    │
    ▼ (over Bluetooth or Wi-Fi)
    │
Watch App (:wearos)
    │
    ▼
AuthSyncListenerService
    │
    ▼
WearAuthStorage (EncryptedSharedPreferences)
    │
    ▼
WearAuthManager
    │
    ▼
Inject into :innertube YouTube client
```

### 4. Offline Playback Flow
```
User Selects Downloaded Track
    │
    ▼
LibraryScreen (Downloads tab)
    │
    ▼
WearPlayerViewModel
    │
    ▼
PlayerRepository
    │
    ▼
WearPlaybackService
    │
    ▼
ExoPlayer (reads from download cache)
    │
    ▼
No network needed ✓
    │
    ▼
Bluetooth Headphones 🎧
```

## Database Schema (`:wearos`)

### WearMusicDatabase

```sql
-- Songs (cached metadata)
CREATE TABLE wear_song (
    id TEXT PRIMARY KEY,
    title TEXT NOT NULL,
    artist TEXT,
    album TEXT,
    thumbnail_url TEXT,
    duration INTEGER,
    cached_at INTEGER
);

-- Playlists
CREATE TABLE wear_playlist (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    thumbnail_url TEXT,
    created_at INTEGER
);

-- Playlist-Song mapping
CREATE TABLE wear_playlist_song_map (
    playlist_id TEXT NOT NULL,
    song_id TEXT NOT NULL,
    position INTEGER NOT NULL,
    PRIMARY KEY (playlist_id, song_id),
    FOREIGN KEY (playlist_id) REFERENCES wear_playlist(id) ON DELETE CASCADE,
    FOREIGN KEY (song_id) REFERENCES wear_song(id) ON DELETE CASCADE
);

-- Downloads
CREATE TABLE wear_download (
    song_id TEXT PRIMARY KEY,
    file_path TEXT NOT NULL,
    downloaded_at INTEGER NOT NULL,
    size_bytes INTEGER NOT NULL,
    FOREIGN KEY (song_id) REFERENCES wear_song(id) ON DELETE CASCADE
);

-- Playback History
CREATE TABLE wear_playback_history (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    song_id TEXT NOT NULL,
    played_at INTEGER NOT NULL,
    completion_percent INTEGER,
    FOREIGN KEY (song_id) REFERENCES wear_song(id) ON DELETE CASCADE
);
```

## Connectivity States

### State Detection (Horologist Network Awareness)

```
┌─────────────────────────────────────────────────────────────┐
│                    Connectivity States                       │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  State 1: Bluetooth-Relayed Internet                        │
│  ┌────────────┐  BT   ┌────────┐  WiFi/LTE  ┌──────────┐  │
│  │   Watch    │◄─────►│ Phone  │◄──────────►│ Internet │  │
│  └────────────┘       └────────┘             └──────────┘  │
│  - Slower connection                                        │
│  - Use lower bitrate                                        │
│  - Phone must be nearby                                     │
│                                                              │
│  State 2: Standalone Wi-Fi                                  │
│  ┌────────────┐  WiFi  ┌──────────┐                        │
│  │   Watch    │◄──────►│ Internet │                        │
│  └────────────┘        └──────────┘                        │
│  - Full speed streaming                                     │
│  - No phone needed                                          │
│                                                              │
│  State 3: No Connectivity                                   │
│  ┌────────────┐                                             │
│  │   Watch    │  (offline)                                  │
│  └────────────┘                                             │
│  - Only downloaded tracks playable                          │
│  - Show clear offline indicator                             │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

## Storage Management

### Cache Pools

```
┌─────────────────────────────────────────────────────────────┐
│                    Watch Storage                             │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Stream Cache (Opportunistic)                               │
│  ┌────────────────────────────────────────────────────┐    │
│  │  /data/data/.../files/wear_stream_cache/           │    │
│  │  - Max 200MB                                        │    │
│  │  - LRU eviction                                     │    │
│  │  - Auto-managed                                     │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  Download Cache (Explicit)                                  │
│  ┌────────────────────────────────────────────────────┐    │
│  │  /data/data/.../files/wear_downloads/              │    │
│  │  - User-controlled size cap (default 1GB)          │    │
│  │  - No auto-eviction                                │    │
│  │  - Manual delete only                              │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  Database                                                    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  /data/data/.../databases/wear_music.db            │    │
│  │  - Minimal size (~1-5MB)                           │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Cleanup Strategy

```
LibraryCleanupWorker (runs weekly)
    │
    ├─► Remove orphaned cache entries
    │   (failed downloads, corrupted files)
    │
    ├─► Find stale downloads (90+ days unplayed)
    │   └─► Notify user (don't auto-delete)
    │
    ├─► Enforce download size cap
    │   └─► Evict LRU downloads if over cap
    │
    └─► Validate downloads still available upstream
        └─► Flag/remove dead entries
```

## Bluetooth Audio Enforcement

### Flow

```
User Presses Play
    │
    ▼
WearPlayerViewModel.play()
    │
    ▼
PlayerRepository.play()
    │
    ▼
Horologist BluetoothAudioGate
    │
    ├─► Check AudioDeviceInfo
    │   │
    │   ├─► BT Audio Connected? ──YES──► Allow Playback
    │   │                                      │
    │   │                                      ▼
    │   │                                 ExoPlayer.play()
    │   │                                      │
    │   │                                      ▼
    │   │                                 🎧 Bluetooth Output
    │   │
    │   └─► No BT Audio? ──NO──► Block Playback
    │                                  │
    │                                  ▼
    │                          Show BT Settings Prompt
    │
    └─► Monitor AudioDeviceCallback
        │
        └─► BT Disconnects Mid-Playback?
            │
            └─► Auto-Pause
```

## System Integration

### Media Session Integration

```
┌─────────────────────────────────────────────────────────────┐
│                    Wear OS System                            │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Media Controls Panel (Swipe-in)                            │
│  ┌────────────────────────────────────────────────────┐    │
│  │  🎵 Vivi Wear                                       │    │
│  │  Now Playing: Track Title                          │    │
│  │  Artist Name                                        │    │
│  │  [◄◄] [▶/❚❚] [►►]                                  │    │
│  └────────────────────────────────────────────────────┘    │
│                          ▲                                   │
│                          │                                   │
│                          │ MediaSession                      │
│                          │                                   │
│  ┌───────────────────────┴────────────────────────────┐    │
│  │  Vivi Wear App                                      │    │
│  │  WearPlaybackService (MediaSessionService)         │    │
│  └────────────────────────────────────────────────────┘    │
│                                                              │
│  Hardware Buttons                                            │
│  - Play/Pause via crown button                              │
│  - Volume via side buttons                                  │
│                                                              │
│  Assistant Integration                                       │
│  "Hey Google, what's playing?"                              │
│  → Reads from MediaSession metadata                         │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

### Tile Integration

```
Watch Face
    │
User Swipes to Tiles
    │
    ▼
┌─────────────────────────┐
│   Vivi Wear Tile        │
│                         │
│   🎵 Track Title        │
│   Artist Name           │
│                         │
│   [◄◄] [▶] [►►]        │
└─────────────────────────┘
    │
Tap to open full app
```

## Error Handling

### Auth Expiration

```
:innertube API call
    │
    ▼
401/403 Response
    │
    ▼
WearYouTubeRepository catches
    │
    ▼
Emit AuthExpiredState
    │
    ▼
UI shows banner:
"Authentication expired. 
 Tap 'Send login to watch' 
 on your phone to reconnect."
```

### Network Errors

```
Stream playback fails
    │
    ▼
Check connectivity state
    │
    ├─► State 3 (offline)
    │   └─► Show: "No connection. Play downloaded tracks."
    │
    ├─► State 1/2 (connected)
    │   └─► Show: "Playback error. Try again."
    │
    └─► Retry with exponential backoff
```

### Bluetooth Disconnection

```
AudioDeviceCallback.onAudioDevicesRemoved()
    │
    ▼
Check if BT audio device removed
    │
    ▼
If playing: ExoPlayer.pause()
    │
    ▼
Show notification:
"Bluetooth disconnected. 
 Playback paused."
```

## Performance Considerations

### Battery Optimization

```
✅ DO:
- Use WAKE_MODE_NETWORK only during streaming
- Event-driven updates (Player.Listener)
- Batch network requests
- Use WorkManager constraints (battery not low, device idle)
- Cache aggressively

❌ DON'T:
- Polling loops (Handler.postDelayed)
- Wake locks during local playback
- Frequent network requests on BT-relayed connection
- GPU-intensive effects (blur, palette extraction)
- Background work without constraints
```

### Memory Optimization

```
✅ DO:
- Load images at appropriate resolution for watch
- Clear old cache entries
- Use paging for large lists
- Release resources when not needed

❌ DON'T:
- Load full-resolution album art
- Keep entire library in memory
- Create unnecessary object allocations
```

## Security

### Credential Storage

```
EncryptedSharedPreferences
    │
    ├─► Key: MasterKey (AES256_GCM)
    ├─► Key Encryption: AES256_SIV
    └─► Value Encryption: AES256_GCM

Stored Data:
- YouTube Music auth cookie
- User preferences
- Nothing else sensitive
```

### Network Security

```
✅ All network traffic via HTTPS
✅ Certificate pinning (inherited from :innertube)
✅ No plaintext credentials
✅ Secure Data Layer communication (encrypted by OS)
```

## Testing Strategy

### Unit Tests (Future)
- Repository layer
- ViewModel logic
- Data transformations

### Integration Tests (Future)
- Database operations
- Download manager
- Auth flow

### Manual Testing (v1.0)
- All connectivity states
- BT connect/disconnect
- Auth sync
- Download/delete
- Long playback sessions
- Storage cleanup
- HyperOS battery management

## Monitoring & Debugging

### Logging Strategy

```kotlin
// Use Timber for structured logging
Timber.d("Playback state: ${player.playbackState}")
Timber.e(exception, "Download failed: ${song.title}")

// Log levels:
// - DEBUG: State changes, flow tracking
// - INFO: User actions, important events
// - WARN: Recoverable errors
// - ERROR: Unrecoverable errors
```

### Debug Tools

```
adb shell dumpsys batterystats
adb shell dumpsys deviceidle
adb logcat | grep "ViviWear"
```

## Future Architecture Enhancements (v2.0+)

### Planned Additions
- Hilt dependency injection
- Kotlin Coroutines Flow everywhere
- Material 3 UI (Horologist 0.8.x)
- Phone-assisted downloads
- Sync service for playlists/position
- Watch face complication provider
- Improved caching strategies

### Potential Optimizations
- Prefetch next track in queue
- Smart download suggestions
- Adaptive bitrate streaming
- Background sync optimization
- Better offline detection
