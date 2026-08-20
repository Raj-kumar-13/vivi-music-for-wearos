package com.music.vivi.wear.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.logging.ErrorReporter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@UnstableApi
@AndroidEntryPoint
class WearPlaybackService : MediaSessionService() {

    @Inject
    lateinit var audioOutputSelector: AudioOutputSelector

    @Inject
    lateinit var errorReporter: ErrorReporter

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        player = ExoPlayer.Builder(this)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    // Battery-optimized audio attributes
                    .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
                    .build(),
                true // Handle audio focus
            )
            .setHandleAudioBecomingNoisy(true) // Pause on audio output loss
            .setWakeMode(C.WAKE_MODE_LOCAL) // Use local wake lock instead of network
            .build()

        // Apply Horologist's Bluetooth audio enforcement
        // audioOutputSelector.connect(player) // connect() not available in this Horologist version

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(WearSessionCallback())
            .build()

        // errorReporter.connect() // connect() not available in this Horologist version
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        // errorReporter.disconnect()
        // audioOutputSelector.disconnect()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        // Stop playback if the app is removed from recent tasks
        player.stop()
        stopSelf()
    }
}