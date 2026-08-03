# Keep Compose classes
-keep class androidx.compose.** { *; }
-keep class androidx.wear.compose.** { *; }

# Keep Horologist classes
-keep class com.google.android.horologist.** { *; }

# Keep Room entities
-keep class com.music.vivi.wear.db.entities.** { *; }

# Keep Media3 classes
-keep class androidx.media3.** { *; }

# Keep Ktor classes
-keep class io.ktor.** { *; }

# Keep InnerTube models
-keep class com.music.innertube.** { *; }

# Keep Timber
-keep class timber.log.Timber { *; }
-keepclassmembers class * {
    *** Timber$Tree*;
}
-assumenosideeffects class timber.log.Timber {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}