package com.music.vivi.wear.tile

import android.content.ComponentName
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.RequestBuilders
import androidx.wear.tiles.TileBuilders
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.tiles.SuspendingTileService
import com.google.common.util.concurrent.FutureCallback
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.music.vivi.wear.playback.WearPlaybackService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wear OS Tile service for quick playback info from watch face.
 * Shows current track title from the active MediaSession.
 */
@OptIn(ExperimentalHorologistApi::class)
@AndroidEntryPoint
class WearMusicTileService : SuspendingTileService() {

    override suspend fun tileRequest(requestParams: RequestBuilders.TileRequest): TileBuilders.Tile {
        Timber.d("Tile request received")

        val currentTitle = try {
            val sessionToken = SessionToken(this, ComponentName(this, WearPlaybackService::class.java))
            val controller = suspendCancellableCoroutine { cont ->
                val future = MediaController.Builder(this@WearMusicTileService, sessionToken).buildAsync()
                Futures.addCallback(future, object : FutureCallback<MediaController> {
                    override fun onSuccess(result: MediaController) = cont.resume(result)
                    override fun onFailure(t: Throwable) = cont.resumeWithException(t)
                }, MoreExecutors.directExecutor())
                cont.invokeOnCancellation { future.cancel(true) }
            }
            val title = controller.mediaMetadata.title?.toString()
            val artist = controller.mediaMetadata.artist?.toString()
            controller.release()
            when {
                title != null && artist != null -> "$title\n$artist"
                title != null -> title
                else -> "No track playing"
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get current track for tile")
            "No track playing"
        }

        return TileBuilders.Tile.Builder()
            .setResourcesVersion(RESOURCE_VERSION)
            .setFreshnessIntervalMillis(30_000)
            .setTileTimeline(
                TimelineBuilders.Timeline.Builder()
                    .addTimelineEntry(
                        TimelineBuilders.TimelineEntry.Builder()
                            .setLayout(
                                LayoutElementBuilders.Layout.Builder()
                                    .setRoot(
                                        LayoutElementBuilders.Text.Builder()
                                            .setText(currentTitle)
                                            .build()
                                    )
                                    .build()
                            )
                            .build()
                    )
                    .build()
            )
            .build()
    }

    override suspend fun resourcesRequest(requestParams: RequestBuilders.ResourcesRequest): ResourceBuilders.Resources {
        return ResourceBuilders.Resources.Builder()
            .setVersion(RESOURCE_VERSION)
            .build()
    }

    override fun onTileEnterEvent(requestParams: EventBuilders.TileEnterEvent) {
        super.onTileEnterEvent(requestParams)
        getUpdater(this).requestUpdate(WearMusicTileService::class.java)
    }

    override fun onTileAddEvent(requestParams: EventBuilders.TileAddEvent) {
        super.onTileAddEvent(requestParams)
        Timber.d("Tile added")
    }

    override fun onTileRemoveEvent(requestParams: EventBuilders.TileRemoveEvent) {
        super.onTileRemoveEvent(requestParams)
        Timber.d("Tile removed")
    }

    override fun onTileLeaveEvent(requestParams: EventBuilders.TileLeaveEvent) {
        super.onTileLeaveEvent(requestParams)
        Timber.d("Tile left")
    }

    companion object {
        private const val RESOURCE_VERSION = "1"
    }
}
