package com.music.vivi.wear.download

import android.content.Intent
import androidx.media3.common.util.UnstableApi
import androidx.media3.download.DownloadService
import androidx.media3.download.DownloadManager
import androidx.media3.download.NotificationManager
import dagger.hilt.android.AndroidEntryPoint

/**
 * Foreground service that handles music downloads for Wear OS.
 * Required by Media3 DownloadManager for background downloads.
 */
@UnstableApi
@AndroidEntryPoint
class WearDownloadService : DownloadService(
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    0x1,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.drawable.exo_icon_play
) {

    companion object {
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "wear_download_channel"
    }

    override fun getDownloadManager(): DownloadManager {
        // Return the download manager instance
        // This should be injected or accessed via a singleton
        return WearDownloadManagerProvider.getInstance(this).downloadManager
    }

    override fun getForegroundNotification(
        downloads: MutableMap<String, Download>,
        notMetRequirements: Int
    ): NotificationManager.Presenter {
        // Create notification presenter for download progress
        return WearDownloadNotificationPresenter(this, downloads)
    }
}