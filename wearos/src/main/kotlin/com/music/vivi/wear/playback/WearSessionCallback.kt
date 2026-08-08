package com.music.vivi.wear.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
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
}
