package com.music.vivi.wear.download

import android.app.Notification
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Scheduler
import com.music.vivi.wear.R
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Foreground service that handles music downloads for Wear OS.
 * Required by Media3 DownloadManager for background downloads.
 */
@UnstableApi
@AndroidEntryPoint
class WearDownloadService : DownloadService(
    DOWNLOAD_NOTIFICATION_ID,
    1000L,
    DOWNLOAD_NOTIFICATION_CHANNEL_ID,
    R.string.app_name,
    R.string.app_name
) {

    @Inject
    lateinit var wearDownloadManager: WearDownloadManager

    companion object {
        private const val DOWNLOAD_NOTIFICATION_ID = 1
        private const val DOWNLOAD_NOTIFICATION_CHANNEL_ID = "wear_download_channel"
    }

    override fun getDownloadManager(): DownloadManager {
        return wearDownloadManager.downloadManager
    }

    override fun getScheduler(): Scheduler? {
        return null
    }

    override fun getForegroundNotification(
        downloads: MutableList<Download>,
        notMetRequirements: Int
    ): Notification {
        return WearDownloadNotificationPresenter(this, downloads).getNotification()
    }
}
