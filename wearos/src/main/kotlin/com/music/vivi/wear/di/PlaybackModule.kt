package com.music.vivi.wear.di

import android.content.Context
import com.google.android.horologist.audio.AudioOutput
import com.google.android.horologist.audio.SystemAudioRepository
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.logging.ErrorReporter
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
    fun provideSystemAudioRepository(
        @ApplicationContext context: Context
    ): SystemAudioRepository {
        return SystemAudioRepository.fromContext(context)
    }

    @Provides
    @Singleton
    fun provideAudioOutput(
        systemAudioRepository: SystemAudioRepository
    ): AudioOutput {
        return systemAudioRepository.audioOutput.value
    }

    /**
     * Provides a default AudioOutputSelector.
     * In Horologist 0.7.x, AudioOutputSelector is an interface.
     * We use a NO-OP implementation for now to satisfy Hilt.
     */
    @com.google.android.horologist.annotations.ExperimentalHorologistApi
    @Provides
    @Singleton
    fun provideAudioOutputSelector(): AudioOutputSelector {
        return object : AudioOutputSelector {
            override suspend fun selectNewOutput(currentAudioOutput: AudioOutput): AudioOutput? = null
            override fun launchSelector() {}
        }
    }

    @Provides
    @Singleton
    fun provideErrorReporter(): ErrorReporter {
        return object : ErrorReporter {
            override fun showMessage(message: Int) {}
            override fun logMessage(message: String, category: ErrorReporter.Category, level: ErrorReporter.Level) {
                when (level) {
                    ErrorReporter.Level.Error -> timber.log.Timber.e("[$category] $message")
                    ErrorReporter.Level.Info -> timber.log.Timber.i("[$category] $message")
                }
            }
        }
    }
}
