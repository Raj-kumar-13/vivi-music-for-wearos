package com.music.vivi.wear.playback

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.session.MediaSession
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.ListenableFuture

class WearSessionCallback : MediaSession.Callback {
    override fun onAddMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        // Handle adding media items from search/library
        return super.onAddMediaItems(session, controller, mediaItems)
    }

    override fun onPlay(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): Int {
        // Play is handled by Horologist's BluetoothAudioGate
        return SessionResult.RESULT_SUCCESS
    }

    override fun onPause(
        session: MediaSession,
        controller: MediaSession.ControllerInfo
    ): Int {
        return SessionResult.RESULT_SUCCESS
    }

    override fun onSeekTo(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        position: Long
    ): Int {
        return SessionResult.RESULT_SUCCESS
    }

    override fun onSetPlaybackSpeed(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        playbackSpeed: Float
    ): Int {
        return SessionResult.RESULT_SUCCESS
    }
}