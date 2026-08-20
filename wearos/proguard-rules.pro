# Room entities (needed for reflection-based DB access)
-keep class com.music.vivi.wear.db.entities.** { *; }

# Keep InnerTube models (serialization)
-keep class com.music.innertube.models.** { *; }
-keep class com.music.innertube.YouTube { *; }

# Ktor (serialization/reflection)
-keep class io.ktor.serialization.** { *; }
-dontwarn io.ktor.**

# Media3 (service binding)
-keep class androidx.media3.session.MediaSessionService { *; }
-keep class androidx.media3.exoplayer.offline.Download { *; }
-dontwarn androidx.media3.**

# Hilt-generated classes
-keep class dagger.hilt.** { *; }
-keep class * extends dagger.hilt.android.internal.managers.ViewComponentManager$FragmentContextWrapper { *; }

# Strip debug/verbose logging in release
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}

# Keep services referenced by name in AndroidManifest.xml
-keep class com.music.vivi.wear.auth.AuthSyncListenerService { *; }
-keep class com.music.vivi.wear.tile.WearMusicTileService { *; }
-keep class com.music.vivi.wear.download.WearDownloadService { *; }

# Compose keeps are handled by the Compose compiler plugin — no blanket keeps needed
-dontwarn androidx.compose.**