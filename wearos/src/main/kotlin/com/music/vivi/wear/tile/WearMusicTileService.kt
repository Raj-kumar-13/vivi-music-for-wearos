package com.music.vivi.wear.tile

import android.content.Intent
import androidx.wear.protolayout.ActionBuilders
import androidx.wear.protolayout.LayoutElementBuilders
import androidx.wear.protolayout.ModifiersBuilders
import androidx.wear.protolayout.ResourceBuilders
import androidx.wear.protolayout.TimelineBuilders
import androidx.wear.protolayout.TypeBuilders.StringProp
import androidx.wear.protolayout.action.LaunchActionBuilders
import androidx.wear.tiles.TileProviders
import androidx.wear.tiles.EventBuilders
import androidx.wear.tiles.TileService
import androidx.wear.tiles.TimelineBuilders.Timeline
import androidx.wear.tiles.TimelineBuilders.TimelineEntry
import com.google.android.horologist.tiles.SuspendingTileService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Wear OS Tile service for quick playback controls from watch face.
 * Shows current track and provides play/pause/skip controls.
 */
@AndroidEntryPoint
class WearMusicTileService : SuspendingTileService() {

    companion object {
        private const val TILE_ID = "music_tile"
        private const val REFRESH_PERIOD_MS = TimeUnit.MINUTES.toMillis(1)
    }

    override suspend fun onTileRequest(requestParams: TileProviders.TileRequest): TileProviders.Tile {
        Timber.d("Tile request received")

        // In a real implementation, you'd get the current track info from your playback service
        val currentTitle = "No track playing"
        val currentArtist = ""
        val isPlaying = false

        return TileProviders.Tile.Builder()
            .setResourcesVersion(RESOURCE_VERSION)
            .setTimeline(
                TimelineBuilders.Timeline.fromLayoutElement(
                    LayoutElementBuilders.Text.Builder()
                        .setText(currentTitle)
                        .build()
                )
            )
            .build()
    }

    override suspend fun onTileAddEvent(requestParams: TileProviders.TileAddEvent) {
        super.onTileAddEvent(requestParams)
        Timber.d("Tile added")
    }

    override suspend fun onTileRemoveEvent(requestParams: TileProviders.TileRemoveEvent) {
        super.onTileRemoveEvent(requestParams)
        Timber.d("Tile removed")
    }

    override suspend fun onTileEnterEvent(requestParams: TileProviders.TileEnterEvent) {
        super.onTileEnterEvent(requestParams)
        Timber.d("Tile entered")
    }

    override suspend fun onTileLeaveEvent(requestParams: TileProviders.TileLeaveEvent) {
        super.onTileLeaveEvent(requestParams)
        Timber.d("Tile left")
    }

    companion object {
        private const val RESOURCE_VERSION = "1"
    }
}