package com.music.vivi.wear.playback

import android.content.Intent
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.music.vivi.wear.download.WearDownloadManager
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
    lateinit var wearDownloadManager: WearDownloadManager

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession
    private lateinit var bluetoothRouteObserver: BluetoothRouteObserver
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()

        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(wearDownloadManager.getStreamCache())
            .setUpstreamDataSourceFactory(DefaultDataSource.Factory(this))
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
                    .setAllowedCapturePolicy(C.ALLOW_CAPTURE_BY_NONE)
                    .build(),
                true
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()

        bluetoothRouteObserver = BluetoothRouteObserver.createWithAutoPause(this, player)
        bluetoothRouteObserver.register()

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(WearSessionCallback())
            .build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return mediaSession
    }

    override fun onDestroy() {
        serviceScope.cancel()
        bluetoothRouteObserver.unregister()
        mediaSession.release()
        player.release()
        super.onDestroy()
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        player.stop()
        stopSelf()
    }
}
