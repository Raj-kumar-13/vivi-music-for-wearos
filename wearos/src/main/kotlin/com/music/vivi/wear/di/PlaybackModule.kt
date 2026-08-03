package com.music.vivi.wear.di

import android.content.Context
import com.google.android.horologist.audio.AudioOutput
import com.google.android.horologist.audio.SystemAudioManager
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.offload.AudioOffloadManager
import com.music.vivi.wear.data.WearAuthManager
import com.music.vivi.wear.data.WearYouTubeRepository
import com.music.vivi.wear.download.WearDownloadManager
import com.music.vivi.wear.network.ConnectivityManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule {

    @Provides
    @Singleton
    fun provideSystemAudioManager(
        @ApplicationContext context: Context
    ): SystemAudioManager {
        return SystemAudioManager(context)
    }

    @Provides
    @Singleton
    fun provideAudioOutputSelector(
        audioOutput: AudioOutput
    ): AudioOutputSelector {
        return AudioOutputSelector(audioOutput)
    }

    @Provides
    @Singleton
    fun provideAudioOutput(
        systemAudioManager: SystemAudioManager
    ): AudioOutput {
        return systemAudioManager
    }

    @Provides
    @Singleton
    fun provideErrorReporter(
        @ApplicationContext context: Context
    ): ErrorReporter {
        return ErrorReporter(
            context = context,
            tag = "ViviWear"
        )
    }

    @Provides
    @Singleton
    fun provideAudioOffloadManager(
        @ApplicationContext context: Context
    ): AudioOffloadManager {
        return AudioOffloadManager(context)
    }

    @Provides
    @Singleton
    fun provideWearAuthManager(
        authStorage: com.music.vivi.wear.auth.WearAuthStorage
    ): WearAuthManager {
        return WearAuthManager(authStorage)
    }

    @Provides
    @Singleton
    fun provideWearYouTubeRepository(
        authManager: WearAuthManager
    ): WearYouTubeRepository {
        return WearYouTubeRepository(authManager)
    }

    @Provides
    @Singleton
    fun provideWearDownloadManager(
        @ApplicationContext context: Context
    ): WearDownloadManager {
        val downloadManager = WearDownloadManager(context)
        downloadManager.initialize()
        return downloadManager
    }

    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager {
        val connectivityManager = ConnectivityManager(context)
        connectivityManager.initialize()
        return connectivityManager
    }
}