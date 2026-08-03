# Porting Vivi Music to Wear OS (Xiaomi Watch 5) — Development Roadmap

Standalone `:wear` module inside the existing fork (`vivizzz007/vivi-music`),
reusing `:innertube`, sideloaded APK via GitHub Releases, Bluetooth-audio-only
playback, aggressively stripped feature set. Structured below in build order —
each phase assumes the previous one is working before you move on.

---

## Phase 0 — Audit & prerequisites

### 0.1 Environment

- Fork cloned, `:app` and `:innertube` building successfully.
- Xiaomi Watch 5 runs Wear OS with Xiaomi's HyperOS overlay on top — treat it
  as stock Wear OS 4/5 for API purposes; HyperOS mostly reskins system UI and
  adds its own battery management (relevant later, in Phase 7), but doesn't
  change the platform APIs you're targeting.
- Android Studio with a Wear OS emulator (API 33/34, "Wear OS Large Round"
  skin is closest to the Watch 5's ~1.43" round display) for fast iteration
  before flashing the real device.
- Developer mode + ADB over Wi-Fi enabled on the watch (Settings → About →
  tap Build number ×7, then Developer options → ADB debugging + Debug over
  Wi-Fi).

### 0.2 Audit `:innertube` before building on top of it

This repo's `:innertube` module descends from the InnerTune → OuterTune →
Metrolist → Vivi Music lineage, where `:innertube` has historically been a
thin Ktor-based protocol client — request signing, `browse`/`search`/
`player`/`next` endpoint calls, JSON deserialization into model classes —
with no playback, caching, download, or Android UI code living inside it
(that all sits in `:app`). It's reasonable to expect this holds in the
current fork, but confirm it yourself before wiring `:wear` to it:

```bash
./gradlew :innertube:dependencies | grep -i "android\|compose\|activity\|fragment"
```

If that comes back empty (or only shows core Kotlin/coroutines/serialization/
networking deps), you're clear — `:innertube` is safe to depend on directly
from `:wear` with no risk of pulling in phone-UI code. If it does show
Android UI dependencies, extract the network/model layer into a clean
sub-module first; that's a prerequisite, not optional, since `:wear` can
never depend on `:app`.

### 0.3 The module-isolation goal

Everything in this roadmap optimizes for one constraint: `:wear` depends on
`:innertube` only, never on `:app`, and `:app` gets at most one small opt-in
addition (the auth-export button in Phase 5) — never a refactor.

---

## Phase 1 — Scaffold the `:wear` module

### 1.1 `settings.gradle.kts`

```kotlin
include(":app")
include(":innertube")
include(":wear")   // new
```

### 1.2 `wear/build.gradle.kts`

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.yourpkg.wear"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.yourpkg.music.wear"
        minSdk = 30       // Wear OS 3.0 floor; Watch 5 ships newer but this is safe
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.14" }
    packaging { resources.excludes += "/META-INF/{AL2.0,LGPL2.1}" }
}

dependencies {
    implementation(project(":innertube"))   // scraping/API layer only, never :app

    // Wear OS UI
    implementation("androidx.wear.compose:compose-material:1.4.0")
    implementation("androidx.wear.compose:compose-foundation:1.4.0")
    implementation("androidx.wear.compose:compose-navigation:1.4.0")
    implementation("androidx.wear:wear-tooling-preview:1.0.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Playback
    implementation("androidx.media3:media3-exoplayer:1.4.1")
    implementation("androidx.media3:media3-session:1.4.1")
    implementation("androidx.media3:media3-datasource-okhttp:1.4.1")

    // Horologist Media Toolkit — Wear-specific UI/domain/data layers on Media3
    // (stable line; 0.8.x-alpha exists for Material 3 screens if preferred)
    val horologist = "0.7.15"
    implementation("com.google.android.horologist:horologist-media-ui:$horologist")
    implementation("com.google.android.horologist:horologist-media-data:$horologist")
    implementation("com.google.android.horologist:horologist-media3-backend:$horologist") // BT-output enforcement lives here
    implementation("com.google.android.horologist:horologist-audio-ui:$horologist")
    implementation("com.google.android.horologist:horologist-compose-layout:$horologist")
    implementation("com.google.android.horologist:horologist-network-awareness:$horologist") // Phase 6

    // Phone↔watch sync (Phase 5)
    implementation("com.google.android.gms:play-services-wearable:18.2.0")

    // Bluetooth / audio device state, local storage, coroutines
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.security:security-crypto:1.1.0-alpha06") // EncryptedSharedPreferences, Phase 5
    implementation("androidx.work:work-runtime-ktx:2.9.1")            // cleanup worker, Phase 7
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
```

Explicitly **do not** add: `media3-cast`, any Discord RPC library (`:kizzy`
in this repo), EQ/DSP libs, lyrics-sync parsing deps (`:lrclib`,
`:betterlyrics`, `:youlyplus`, `:paxsenixlyrics`), or the canvas/video
background modules (`:canvas`, `:applecanvas`, `:vivimusiccanvas`,
`:artistvideo`) — see Phase 2.

### 1.3 Verify module isolation

```bash
./gradlew :wear:dependencies | grep ":app"
```
Should return nothing, always. Re-run this check after any dependency change.

### 1.4 `AndroidManifest.xml` — standalone Wear app

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

    <uses-feature android:name="android.hardware.type.watch" />
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.BLUETOOTH_CONNECT" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />
    <uses-permission android:name="android.permission.WAKE_LOCK" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />

    <application
        android:label="Vivi Wear"
        android:icon="@mipmap/ic_launcher"
        android:allowBackup="false">

        <!-- Lets this install and run standalone, no paired-phone companion app required -->
        <meta-data android:name="com.google.android.wearable.standalone" android:value="true" />

        <uses-library android:name="com.google.android.wearable" android:required="false" />

        <activity
            android:name=".MainActivity"
            android:exported="true"
            android:theme="@style/WearAppTheme">
            <intent-filter>
                <action android:name="android.intent.action.MAIN" />
                <category android:name="android.intent.category.LAUNCHER" />
            </intent-filter>
        </activity>

        <service
            android:name=".playback.WearPlaybackService"
            android:exported="true"
            android:foregroundServiceType="mediaPlayback">
            <intent-filter>
                <action android:name="androidx.media3.session.MediaSessionService" />
            </intent-filter>
        </service>

        <!-- Phase 5: receives the auth handoff from :app -->
        <service android:name=".auth.AuthSyncListenerService" android:exported="true">
            <intent-filter>
                <action android:name="com.google.android.gms.wearable.DATA_CHANGED" />
            </intent-filter>
        </service>
    </application>
</manifest>
```

---

## Phase 2 — Decide what NOT to build (feature stripping)

Do this before writing playback/UI code, not after — it's much easier to
simply never implement these in `:wear` than to strip them out later.

| Feature (phone `:app`) | Wear OS decision |
|---|---|
| Synced/karaoke lyrics | Omit. Static "Now Playing" text only, no timed sync. |
| Equalizer / audio effects chain | Omit. Default ExoPlayer audio sink, no `AudioProcessor` chain. |
| Animated canvas / Apple Music-style visualizer | Omit. Static album art `Image` composable only. |
| Discord Rich Presence (`:kizzy`) / Last.fm scrobbling | Omit — no reason for a watch to hold these network connections open. |
| Google Cast | Omit. No `media3-cast`; avoids pulling in Play Services Cast framework entirely. |
| High-frequency (e.g. 200ms) widget/notification polling | Replace with event-driven `Player.Listener` updates only (Phase 3). |
| Home-screen widgets | N/A on Wear OS. |
| Multiple audio-quality picker UI | Optional to keep, but default to the lowest reasonable bitrate given Phase 6's connectivity constraints. |
| Android Auto integration | N/A — separate surface, not relevant to a watch build. |

Mechanical check for reachability from `:wear`:

```bash
grep -rl "Equalizer\|LrcParser\|CastPlayer\|Discord\|VideoBackground\|Canvas" app/src/main | xargs -I{} echo "verify {} is not reachable from :wear"
```

---

## Phase 3 — Core playback engine

### 3.1 Lightweight `MediaSessionService`

```kotlin
class WearPlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .build(),
                /* handleAudioFocus = */ true
            )
            .build()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(WearSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo) = mediaSession

    override fun onDestroy() {
        mediaSession.release()
        player.release()
        super.onDestroy()
    }
}
```

No polling loop anywhere — drive all UI state from `Player.Listener`
callbacks (`onIsPlayingChanged`, `onMediaItemTransition`,
`onPositionDiscontinuity`), which Horologist's `PlayerRepository` already
does for you once wired up in Phase 4.

### 3.2 Bluetooth audio routing enforcement

Block playback unless a Bluetooth audio output is actually connected, so
ExoPlayer never routes to the watch's internal notification speaker.

**Use Horologist's built-in guard as the default.** `horologist-media3-backend`
already decorates the `Player` to check the current audio output before
anything reaches ExoPlayer, and — if no Bluetooth device is connected —
launches the system Bluetooth settings flow automatically. Route every play
action (UI button, media-session callback, tile) through the
Horologist-wrapped player and this is handled without writing the check
yourself:

```kotlin
class WearPlayerRepository(
    private val player: Player // the Horologist-decorated Media3 Player
) : PlayerRepositoryImpl() {
    fun play() {
        player.play() // Horologist's decorator intercepts and checks AudioDeviceInfo first
    }
}
```

**Manual fallback (only if you need custom UX around the block, e.g. a
Vivi-branded "connect headphones" screen instead of jumping straight to
system Bluetooth settings):**

```kotlin
class BluetoothAudioGate(context: Context) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun isBluetoothAudioConnected(): Boolean {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any {
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            it.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        }
    }
}

class WearSessionCallback(
    private val bluetoothGate: BluetoothAudioGate
) : MediaSession.Callback {
    override fun onPlayerCommandRequest(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playerCommand: Int
    ): Int {
        if (playerCommand == Player.COMMAND_PLAY_PAUSE && !bluetoothGate.isBluetoothAudioConnected()) {
            return SessionResult.RESULT_ERROR_INVALID_STATE
        }
        return SessionResult.RESULT_SUCCESS
    }
}
```

### 3.3 Mid-playback disconnect handling

Regardless of which gate you use, pause immediately if earbuds disconnect
mid-track rather than letting audio silently fall back to the speaker:

```kotlin
class BluetoothRouteObserver(context: Context, private val onChanged: (Boolean) -> Unit) {
    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val gate = BluetoothAudioGate(context)
    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(added: Array<out AudioDeviceInfo>) = onChanged(gate.isBluetoothAudioConnected())
        override fun onAudioDevicesRemoved(removed: Array<out AudioDeviceInfo>) = onChanged(gate.isBluetoothAudioConnected())
    }
    fun register() = audioManager.registerAudioDeviceCallback(callback, null)
    fun unregister() = audioManager.unregisterAudioDeviceCallback(callback)
}
```

```kotlin
BluetoothRouteObserver(context) { connected ->
    if (!connected && player.isPlaying) player.pause()
}.also { it.register() }
```

### 3.4 This already gives you system media-control integration for free

Because playback lives in a real `MediaSessionService`/`MediaSession`, Wear
OS's standard system media control surface (the swipe-in controls panel,
hardware-button media controls, Assistant "what's playing" queries) will
automatically show and control Vivi Wear's playback — this is inherent to
implementing `MediaSessionService` correctly, not a separate feature to
build. Nothing further is needed here beyond what Phases 3.1–3.3 already do.

---

## Phase 4 — UI (Horologist Media Toolkit)

### 4.1 Architecture

- `horologist-media-data`'s `PlayerRepositoryImpl` wraps the Media3 `Player`
  and exposes a `PlayerRepository` domain interface the UI observes.
- `horologist-media-ui`'s `PlayerScreen` plus `TextMediaDisplay`/
  `PodcastControlButtons`/`SettingsButtons` give you the playback screen
  without hand-building rotary input, round-display layout, or a progress
  timer loop.
- Your `:innertube`-backed content repository (search/library/stream URLs)
  stays separate from Horologist's playback-state repository — don't
  conflate the two.

### 4.2 ViewModel

```kotlin
class ViviPlayerViewModel(
    playerRepository: PlayerRepositoryImpl
) : PlayerViewModel(playerRepository) {
    init {
        viewModelScope.launch { playerRepository.connect() }
    }
}
```

### 4.3 Now Playing screen

```kotlin
@Composable
fun ViviNowPlayingScreen(viewModel: ViviPlayerViewModel) {
    val playerUiState by viewModel.playerUiState.collectAsStateWithLifecycle()

    PlayerScreen(
        playerViewModel = viewModel,
        mediaDisplay = {
            TextMediaDisplay(
                title = playerUiState.media?.title.orEmpty(),
                artist = playerUiState.media?.artist.orEmpty()
            )
        },
        controlButtons = {
            PodcastControlButtons(playerUiController = viewModel, playerUiState = playerUiState)
        },
        buttons = {
            SettingsButtons(
                volumeUiState = viewModel.volumeUiState.collectAsStateWithLifecycle().value,
                onVolumeClick = { /* opens Horologist's audio-ui volume screen */ },
                enabled = playerUiState.connected
            )
        }
    )
}
```

Controls disable themselves automatically when `playerUiState.connected` is
false (i.e. no Bluetooth output) — this is the UI-side reflection of the
Phase 3.2 gate, so you don't separately wire a "blocked" state into the
button row.

### 4.4 Library/search screen (hand-rolled — Horologist doesn't opinionate content browsing)

```kotlin
@Composable
fun ViviLibraryScreen(items: List<LibraryItem>, onItemClick: (LibraryItem) -> Unit) {
    ScalingLazyColumn(modifier = Modifier.rotaryScrollable(/* ... */)) {
        items(items) { item ->
            Chip(
                label = { Text(item.title) },
                secondaryLabel = { Text(item.artist) },
                onClick = { onItemClick(item) }
            )
        }
    }
}
```

Route search entry through `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` rather
than the on-screen keyboard — typing on a round watch display is unpleasant,
and voice-first text entry is standard Wear OS convention.

### 4.5 Downloads manager screen

Same `ScalingLazyColumn` pattern as 4.4, backed by Media3's `DownloadManager`
state (Phase 6), with a delete action per row.

### 4.6 A Tile for quick access

Add a Tile (swipe-accessible from the watch face) showing the current track
plus play/pause/skip, so playback control doesn't require a full app launch
every time. Build with `androidx.wear.tiles`/ProtoLayout; Horologist has
helpers for capturing Compose content into Tile layouts if you want visual
parity with the in-app player. (Skipping a watch-face complication — a Tile
alone covers quick access without the added surface to maintain.)

### 4.7 Navigation depth

Core action (resume/play) reachable in at most 1–2 taps from cold launch —
default the launch screen to Now Playing if something was recently played,
Library only if there's no playback history yet. Flatten anything used
regularly to 2–3 taps maximum; phone-app drill-down depths don't translate
well to a watch.

---

## Phase 5 — Authentication & phone session sync

### 5.1 Reuse the existing phone app's session

Since the phone app is already installed and logged in, use the **Wearable
Data Layer API** rather than building a watch-native login flow — it's the
standard phone↔watch sync mechanism, and it works automatically over
whichever connectivity state (Phase 6) is currently active.

This is the one deliberate, small exception to "never touch `:app`" — a
single opt-in export point:

```kotlin
// In :app — the one small addition
fun sendSessionToWatch(cookie: String) {
    val request = PutDataMapRequest.create("/vivi/auth").apply {
        dataMap.putString("cookie", cookie)
        dataMap.putLong("timestamp", System.currentTimeMillis())
    }.asPutDataRequest().setUrgent()

    Wearable.getDataClient(context).putDataItem(request)
}
```

```kotlin
// In :wear — fully isolated from :app's code
class AuthSyncListenerService : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        events.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == "/vivi/auth") {
                val cookie = DataMapItem.fromDataItem(event.dataItem).dataMap.getString("cookie")
                storeSecurely(cookie) // see 5.3
            }
        }
    }
}
```

### 5.2 How long does the session last?

There's no fixed, guaranteed duration — Google doesn't publish one for this
kind of cookie-based session, and because Vivi Music (like this whole app
lineage) authenticates by reusing a browser-style session rather than an
official OAuth grant, it's inherently less stable than a "real" API
credential. In practice these sessions tend to persist for a long time
(often months) under normal use, but can be invalidated by:

- the user changing their Google password,
- Google flagging unusual/automated-looking traffic patterns,
- explicit revocation from Google account security settings,
- or periodic security-side cookie rotation you have no visibility into.

**Design for recurring re-auth, not a one-time setup step.** Detect
`401`/`403` responses from `:innertube` calls and surface a clear
"reconnect" prompt (pointing back at the "Send login to watch" button in
`:app`) rather than failing silently or looping retries against a dead
session. Treat the sync button in 5.1 as something a user might tap again
occasionally, not a first-run-only action.

### 5.3 Store the credential securely

```kotlin
val encryptedPrefs = EncryptedSharedPreferences.create(
    context,
    "vivi_auth",
    MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
)
```

Use this instead of plain `SharedPreferences` — a lost/stolen watch is a more
realistic threat model than people tend to assume, and this cookie is a
bearer credential.

---

## Phase 6 — Connectivity modes & offline downloads

### 6.1 Three connectivity states

The Watch 5 has no LTE, so the app needs to behave correctly in three
distinct states, decided by the OS, not your app code:

1. **Bluetooth-relayed internet** (phone nearby, BT connected) — Wear OS
   automatically tunnels IP traffic over the BT link when the watch has no
   active Wi-Fi of its own. Transparent to ExoPlayer/OkHttp; no BT-specific
   networking code needed. Audio bitrates fit comfortably in this tunnel.
2. **Standalone Wi-Fi** (no phone, watch on a known network) — streams
   directly, phone or no phone, wherever there's Wi-Fi.
3. **No connectivity** (no phone, no known Wi-Fi, no cellular fallback) —
   only already-downloaded tracks are playable.

Use `horologist-network-awareness` to detect which of states 1–2 is active
and adjust streaming bitrate accordingly — the BT-relay path is a slower,
shared pipe even though it also presents as "connected."

### 6.2 Downloads are what make state 3 usable

Treat Media3's `DownloadManager` (below) as core functionality, not optional
polish, if "leave the phone at home with no Wi-Fi around" is a real goal —
it's the only thing that works in state 3.

```kotlin
val downloadCache = SimpleCache(
    File(context.filesDir, "wear_downloads"),
    NoOpCacheEvictor(), // explicit downloads persist until user/cleanup removes them — see Phase 7
    databaseProvider
)

val downloadManager = DownloadManager(
    context, databaseProvider, downloadCache,
    DefaultHttpDataSource.Factory(),
    Executors.newFixedThreadPool(2)
).apply { maxParallelDownloads = 1 } // conservative on battery/thermal
```

Prefetch the next queued track a few seconds before the current one ends —
enough buffer to survive brief BT/Wi-Fi drops (elevator, subway, radio
power-save cycling) without needing a large cache.

**Build watch-native downloading only for v1.** A phone-assisted transfer
(phone downloads once, pushes the file to the watch via the Data Layer's
`ChannelClient`) is a valid later optimization when the phone is nearby, but
by definition doesn't help once you've left it behind — so it's additive,
not a replacement.

### 6.3 What actually needs phone sync

| Data | Needs phone sync? | Why |
|---|---|---|
| Search, library, playlists | No | Watch fetches directly via `:innertube`. |
| Auth session | Yes (Phase 5) | No comfortable watch-native login flow. |
| Downloaded tracks | No (v1) | Watch downloads independently. |
| Playback position handoff | Optional, separate feature | Not free from the platform for two independent app sessions — build only if wanted, via a small Data Layer message. Skip for v1. |

The watch app is functionally standalone once it has a valid session — the
phone's only required role is the occasional auth handoff.

---

## Phase 7 — Storage cleanup, battery & processing optimization

### 7.1 Split the cache into two pools

- **Opportunistic stream cache** (played but not explicitly downloaded):
  self-cleaning via `LeastRecentlyUsedCacheEvictor` with a hard cap —

```kotlin
val streamCache = SimpleCache(
    File(context.filesDir, "wear_stream_cache"),
    LeastRecentlyUsedCacheEvictor(200L * 1024 * 1024),
    databaseProvider
)
```

- **Explicit downloads** (Phase 6.2): separate cache/directory, persists
  until the user or the cleanup job below removes it — never let LRU
  eviction on the opportunistic cache silently delete something the user
  intentionally saved.

### 7.2 Periodic cleanup worker

```kotlin
val cleanupRequest = PeriodicWorkRequestBuilder<LibraryCleanupWorker>(7, TimeUnit.DAYS)
    .setConstraints(
        Constraints.Builder()
            .setRequiresBatteryNotLow(true)
            .setRequiresDeviceIdle(true)
            .build()
    )
    .build()

WorkManager.getInstance(context).enqueueUniquePeriodicWork(
    "library_cleanup", ExistingPeriodicWorkPolicy.KEEP, cleanupRequest
)
```

```kotlin
class LibraryCleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        if (isPlaybackActive()) return Result.retry() // never contend with playback for disk I/O

        removeOrphanedCacheEntries()                          // failed/partial downloads, corrupted index entries
        val stale = findDownloadsUnplayedSince(days = 90)
        if (stale.isNotEmpty()) notifyStaleDownloadsAvailableForCleanup(stale) // suggest, don't auto-delete intentional downloads
        enforceDownloadSizeCap(maxBytes = userConfiguredCapOrDefault())        // this one CAN auto-evict LRU-first

        return Result.success()
    }
}
```

Add a manual **"Clean up now"** action in Settings that runs the same worker
on demand.

### 7.3 Keep the library fresh, not just small

Periodically re-validate that downloaded tracks are still available upstream
— a track removed/privated on YouTube Music should be flagged or pruned
rather than sitting as a dead entry discovered only on playback failure.
Version-stamp download metadata so a future codec/cache-format change can
detect and offer to re-download affected tracks instead of leaving silently
incompatible entries behind.

### 7.4 General battery/CPU discipline

- `wakeMode = C.WAKE_MODE_NETWORK` only while actively streaming, not for
  local-cache playback.
- No `Handler.postDelayed` polling anywhere — audit for anything copied from
  `:app`'s widget-update code.
- Gate any network-requiring maintenance work (e.g. the availability
  revalidation in 7.3) behind `Constraints.setRequiredNetworkType(NetworkType.UNMETERED)`
  or similar, so it doesn't fire over a Bluetooth-relayed connection and
  compete with the phone's own radio use.
- No album-art blur/palette-extraction effects per track change — cheap on a
  phone GPU, non-trivial on the Watch 5's smaller SoC on battery.
- Test specifically for HyperOS's independent battery-freezing behavior
  during long sessions, not just short ones (Phase 9) — document for users
  that they may need to manually exempt the app from battery optimization,
  since a sideloaded app gets no Play Protect "trusted app" signal to soften
  this.

---

## Phase 8 — Build, sign, and distribute

### 8.1 Local install for testing

```bash
adb connect <watch-ip>:5555
./gradlew :wear:installDebug
```

### 8.2 Release build & signing

```bash
./gradlew :wear:assembleRelease
apksigner sign --ks release.keystore \
  --out vivi-wear-release-signed.apk \
  wear/build/outputs/apk/release/wear-release-unsigned.apk
apksigner verify vivi-wear-release-signed.apk
```

### 8.3 GitHub Releases + CI

```yaml
name: Build Wear APK
on:
  push:
    tags: ["wear-v*"]
jobs:
  build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - uses: actions/setup-java@v4
        with: { distribution: temurin, java-version: '17' }
      - run: ./gradlew :wear:assembleRelease
      - name: Sign APK
        run: |
          echo "${{ secrets.KEYSTORE_BASE64 }}" | base64 -d > release.keystore
          apksigner sign --ks release.keystore \
            --ks-pass pass:${{ secrets.KEYSTORE_PASSWORD }} \
            --out vivi-wear-release-signed.apk \
            wear/build/outputs/apk/release/wear-release-unsigned.apk
      - uses: softprops/action-gh-release@v2
        with: { files: vivi-wear-release-signed.apk }
```

Document minimum Wear OS version / Watch 5 explicitly in release notes,
since bypassing Play Store also bypasses its device-compatibility check.

### 8.4 In-app update check

No Play Store auto-update, so check the GitHub Releases API on launch and
show a simple "Update available" banner (compare tag to
`BuildConfig.VERSION_NAME`) rather than forcing an interrupt — link out to
the release page for manual sideload, or optionally prompt
`REQUEST_INSTALL_PACKAGES` for a smoother in-app install if you decide the
extra permission ask is worth it.

---

## Phase 9 — Test matrix

- Fresh watch, no phone paired at all → app installs and launches standalone.
- No Bluetooth device connected → play is blocked, prompt shown, no audio
  from the internal speaker.
- BT earbuds connected → playback works; disconnect mid-song → auto-pause.
- Watch on its own Wi-Fi, no phone nearby → streaming works (state 2).
- No phone, no Wi-Fi → only downloaded tracks play; live streaming fails
  gracefully with a clear message (state 3).
- Screen off / app backgrounded → playback continues via the foreground
  service; system media controls panel shows and controls it correctly.
- Long listening session (1hr+) on HyperOS specifically → check the
  foreground service isn't killed, via `adb shell dumpsys batterystats` /
  `dumpsys deviceidle`.
- Auth session expires (simulate a 401 from `:innertube`) → clear reconnect
  prompt appears rather than silent failure or retry loop.
- Low storage scenario → cleanup worker and size cap behave correctly rather
  than filling the watch.
- Long-run storage check → after simulated weeks of use, confirm the
  cleanup worker actually keeps download storage bounded.

---

## Phase 10 — Ongoing maintenance: staying current

Everything above reflects what's current as of when this guide was written
(Media3 1.4.1, Horologist 0.7.15). Wear OS and Horologist both move fast
enough that parts of this will be superseded during a real build-out.
Treat this as a recurring task, not a one-time setup step:

- Check [Horologist's releases](https://github.com/google/horologist/releases)
  each time you touch the `:wear` module — the Media Toolkit has been
  actively gaining features (network awareness, Material 3 media screens)
  release over release.
- Check the [Wear OS developer docs](https://developer.android.com/wear)
  whenever you bump `targetSdk` — new platform versions periodically ship
  battery-scheduling primitives or audio-routing APIs that may let you
  delete custom workaround code written for an earlier gap.
- Re-run the `:innertube` isolation check from Phase 0.2 after pulling
  upstream changes from the main fork, since that module's shape can drift
  as the phone app evolves.

---

## Appendix: Horologist as a reference implementation

Google's Horologist repo ships a runnable `media-sample` app on the exact
same stack this guide uses (Media3 + `media3-backend` + `media-ui`
`PlayerScreen`). Worth running side-by-side while building Phases 3–4,
particularly for how it wires `PlayerRepositoryImpl.connect()` together with
a download repository — that pairing is worth copying closely.

## Summary: what NOT to touch

- `:app` — zero edits except the single opt-in auth-export button (Phase 5.1).
- `:innertube` — read-only dependency; only touch if the Phase 0.2 audit
  finds Android-UI leakage that needs extracting first.
