package com.music.vivi.wear.playback

import androidx.media3.common.MediaItem
import androidx.media3.session.MediaSession
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import timber.log.Timber

class WearSessionCallback : MediaSession.Callback {
    override fun onAddMediaItems(
        session: MediaSession,
        controller: MediaSession.ControllerInfo,
        mediaItems: List<MediaItem>
    ): ListenableFuture<List<MediaItem>> {
        val resolved = mediaItems.map { item ->
            if (item.localConfiguration?.uri != null) {
                item
            } else {
                // Item has mediaId but no URI — rebuild with requestMetadata so
                // the player can handle it
                item.buildUpon()
                    .setUri(item.requestMetadata.mediaUri)
                    .build()
            }
        }
        Timber.d("onAddMediaItems: resolved ${resolved.size} items")
        return Futures.immediateFuture(resolved)
    }
}
