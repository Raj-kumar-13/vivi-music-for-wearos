# Vivi Music Wear OS - Design Decisions Summary

This document summarizes the key design decisions needed before implementation begins.

## Critical Decisions (Must Decide Before Starting)

### 1. Database Strategy ⚠️ IMPORTANT

**Question:** Should the Wear OS app have its own database or try to sync with the phone app?

**Option A: Separate Simplified Database** ✅ RECOMMENDED
```
Pros:
- Complete module independence
- Optimized for watch use cases
- No sync complexity
- Simpler to maintain

Cons:
- Downloaded songs not automatically synced from phone
- User must manage downloads separately on watch

Implementation:
- Create WearMusicDatabase with minimal entities:
  * WearSongEntity (basic metadata)
  * WearPlaylistEntity
  * WearDownloadEntity
  * WearPlaybackHistoryEntity
- Fetch everything else from :innertube on demand
```

**Option B: Reuse Existing MusicDatabase**
```
Pros:
- Potential data sharing

Cons:
- Violates module isolation principle
- Pulls in :app dependencies
- Complex migration handling
- Not recommended
```

**Decision needed:** A or B?

---

### 2. Package Naming

**Question:** What package structure for the Wear OS module?

**Option A:** `com.music.vivi.wear` (namespace) / `com.vivi.vivimusic.wear` (applicationId)
- ✅ Consistent with existing structure
- ✅ Clear relationship to main app

**Option B:** `com.vivi.wear` (completely separate)
- ⚠️ Less clear relationship
- ⚠️ Breaks naming convention

**Recommendation:** Option A

**Decision needed:** Confirm or override?

---

### 3. Offline Downloads Strategy

**Question:** How should downloads work in v1.0?

**Option A: Watch Downloads Directly** ✅ RECOMMENDED FOR V1.0
```
Pros:
- Truly standalone
- Works without phone nearby
- Simpler implementation

Cons:
- Uses watch battery
- Uses watch network
```

**Option B: Phone Downloads & Transfers**
```
Pros:
- More efficient
- Leverages phone's better connectivity

Cons:
- Requires phone nearby
- Doesn't help when phone is left behind
- More complex
```

**Option C: Both (Hybrid)**
```
Pros:
- Best of both worlds

Cons:
- Much more complex
- Better for v2.0
```

**Recommendation:** 
- v1.0: Option A (watch downloads directly)
- v2.0: Add Option B as optimization

**Decision needed:** Confirm phased approach?

---

### 4. Authentication Flow

**Question:** Should we support watch-native login as fallback?

**Proposed Flow (Phone Handoff Only):**
1. User logs in on phone app (existing flow)
2. User taps "Send login to watch" button in phone Settings
3. Watch receives auth cookie via Wearable Data Layer
4. Watch stores in EncryptedSharedPreferences
5. If auth expires, user repeats step 2

**Alternative: Add Watch-Native Login**
- Pros: Fully independent
- Cons: Complex UI on watch, poor UX for typing

**Recommendation:** Phone handoff only for v1.0

**Decision needed:** Confirm or add watch-native login?

---

### 5. Feature Scope for v1.0

**Question:** Which features should be in v1.0 vs. future versions?

**Proposed v1.0 Scope (MVP):**
- ✅ Search & play music
- ✅ Download tracks for offline
- ✅ Basic playback controls
- ✅ Bluetooth audio enforcement
- ✅ Auth sync from phone
- ✅ System media controls
- ✅ Basic Tile
- ✅ Storage cleanup

**Deferred to v2.0+:**
- ❌ Playlist creation/editing (read-only in v1.0)
- ❌ Lyrics display (even static)
- ❌ Phone-assisted downloads
- ❌ Playback position sync between devices
- ❌ Playlist sync
- ❌ Watch face complication
- ❌ Smart download suggestions

**Decision needed:** Any must-haves to move from v2.0 to v1.0?

---

## Secondary Decisions (Can Decide During Implementation)

### 6. Branding & Naming

**App Name Options:**
- A) "Vivi Wear"
- B) "Vivi Music for Wear OS"
- C) "Vivi Music"

**Icon:**
- A) Reuse phone app icon
- B) Create watch-specific variant (simplified)

**Recommendation:** "Vivi Wear" + simplified icon variant

---

### 7. Audio Quality Defaults

**Question:** What should be the default streaming quality?

**Options:**
- A) Lowest bitrate (battery/bandwidth optimized)
- B) Medium bitrate (balanced)
- C) Ask user on first launch

**Recommendation:** A (lowest) with easy setting to change

**Rationale:** Watch is often on Bluetooth-relayed connection (slower)

---

### 8. Storage Limits

**Question:** Default download size cap?

**Recommendation:** 1GB default, user-configurable (500MB - 3GB range)

**Rationale:** 
- Xiaomi Watch 5 has limited storage
- 1GB = ~200-300 songs at reasonable quality
- User can adjust based on their watch's available space

---

### 9. Playlist Management

**Question:** Should playlists be read-only or editable on watch?

**Option A: Read-Only** ✅ RECOMMENDED FOR V1.0
- User can browse and play playlists
- Cannot create/edit/reorder on watch
- Simpler UI, less complexity

**Option B: Full Editing**
- Can create/edit/reorder playlists
- More complex UI on small screen
- Better for v2.0

**Recommendation:** Option A for v1.0

---

### 10. Bluetooth Enforcement Strictness

**Question:** How strict should Bluetooth audio requirement be?

**Proposed Implementation:**
- ✅ Block playback if no BT audio device connected
- ✅ Show system Bluetooth settings prompt
- ✅ Auto-pause if BT disconnects mid-playback
- ✅ Debug build: Allow override via developer option

**Supported Device Types:**
- TYPE_BLUETOOTH_A2DP
- TYPE_BLUETOOTH_SCO
- TYPE_BLE_HEADSET
- TYPE_BLE_SPEAKER

**Decision needed:** Confirm approach?

---

## Technical Decisions

### 11. Dependency Versions

**Proposed Versions (based on existing libs.versions.toml):**

```toml
[versions]
# Reuse existing
kotlin = "2.3.10"
media3 = "1.7.1"
room = "2.8.4"
ktor = "3.4.0"
coil = "3.3.0"

# New for Wear OS
wearCompose = "1.4.0"
horologist = "0.7.15"  # Stable, not alpha
playServicesWearable = "18.2.0"
securityCrypto = "1.1.0-alpha06"
```

**Question:** Use Horologist 0.7.15 (stable) or 0.8.x-alpha (Material 3)?

**Recommendation:** 0.7.15 for v1.0 stability

---

### 12. Minimum SDK Version

**Proposed:**
- minSdk = 30 (Wear OS 3.0)
- targetSdk = 34
- compileSdk = 34

**Rationale:** 
- Xiaomi Watch 5 runs Wear OS 4/5 (API 33+)
- API 30 floor provides good compatibility
- Matches Horologist requirements

**Decision needed:** Confirm or adjust?

---

## Implementation Approach

### 13. Development Order

**Proposed Phase Order:**
1. Module scaffold
2. Database layer
3. Playback engine
4. InnerTube integration
5. UI (Now Playing first)
6. Downloads system
7. Auth sync
8. Network awareness
9. Storage cleanup
10. Settings & Tile
11. Testing & polish
12. Build & release

**Decision needed:** Any phase reordering needed?

---

### 14. Testing Strategy

**Proposed:**
- Manual testing on emulator during development
- Manual testing on Xiaomi Watch 5 before each release
- No automated UI tests for v1.0 (add in v2.0)

**Test Matrix (from vivi-wearos.md Phase 9):**
- Fresh watch, no phone paired
- BT device connected/disconnected scenarios
- All three connectivity modes
- Long listening sessions
- Auth expiration
- Low storage scenarios
- HyperOS battery management

**Decision needed:** Add automated testing to v1.0 scope?

---

## Module Isolation Verification

**Critical Constraint:** `:wearos` must NEVER depend on `:app`

**Verification Command:**
```bash
./gradlew :wearos:dependencies | grep ":app"
```
Must return nothing, always.

**Allowed Dependencies:**
- ✅ `:innertube` (verified clean, no Android UI deps)
- ✅ AndroidX libraries
- ✅ Media3
- ✅ Horologist
- ✅ Ktor (already in :innertube)
- ✅ Room
- ✅ Coil

**Forbidden Dependencies:**
- ❌ `:app`
- ❌ `:kizzy` (Discord RPC)
- ❌ `:canvas`, `:applecanvas`, `:vivimusiccanvas`
- ❌ `:artistvideo`
- ❌ `:lyricsProvider`
- ❌ Any phone-specific modules

---

## Phone App Modifications

**Only ONE modification allowed to `:app`:**

**File:** `app/src/main/kotlin/com/music/vivi/ui/screens/settings/SettingsScreen.kt` (or similar)

**Addition:** Single button/option:
```kotlin
// In Settings screen
SettingsItem(
    title = "Send login to watch",
    description = "Sync authentication to Wear OS app",
    onClick = { 
        WearAuthSyncHelper.sendAuthToWatch(context, authCookie)
    }
)
```

**New File:** `app/src/main/kotlin/com/music/vivi/wear/WearAuthSyncHelper.kt`
```kotlin
object WearAuthSyncHelper {
    fun sendAuthToWatch(context: Context, cookie: String) {
        // Wearable Data Layer implementation
    }
}
```

**Decision needed:** Confirm this is acceptable minimal modification?

---

## Summary of Decisions Needed

### Must Decide Now:
1. ✅ Database strategy: Separate simplified DB (Option A)
2. ✅ Package naming: `com.music.vivi.wear` (Option A)
3. ✅ Downloads: Watch-direct for v1.0, phone-assisted for v2.0
4. ✅ Auth: Phone handoff only (no watch-native login)
5. ⚠️ **v1.0 feature scope: Confirm or adjust?**

### Can Decide Later:
6. App name & icon (recommendation: "Vivi Wear" + simplified icon)
7. Audio quality default (recommendation: lowest)
8. Storage cap default (recommendation: 1GB)
9. Playlist management (recommendation: read-only v1.0)
10. BT enforcement (recommendation: strict with debug override)

### Technical Confirmations:
11. Dependency versions (recommendation: use stable Horologist 0.7.15)
12. SDK versions (recommendation: min 30, target 34)
13. Phase order (recommendation: as listed)
14. Testing strategy (recommendation: manual only for v1.0)

---

## Next Steps

1. **Review & Approve** these design decisions
2. **Discuss any concerns** or alternative approaches
3. **Lock in decisions** before starting implementation
4. **Proceed to Phase 1** (Module Scaffold)

---

## Questions for Discussion

1. **Is the separate database approach acceptable?** This means downloads won't auto-sync from phone, but keeps the module truly independent.

2. **Is phone-only auth acceptable for v1.0?** Or is watch-native login a must-have?

3. **Is the v1.0 feature scope sufficient for MVP?** Or should we add playlist editing, lyrics, etc.?

4. **Is the single "Send login to watch" button in phone app acceptable?** This is the only modification to `:app`.

5. **Any concerns about the 3-month timeline estimate?** (Assuming part-time single developer)

6. **Should we add any automated testing to v1.0?** Or defer to v2.0?

7. **Any HyperOS-specific concerns?** (Battery management, permissions, etc.)

8. **Distribution via GitHub Releases acceptable?** (No Play Store for v1.0)
