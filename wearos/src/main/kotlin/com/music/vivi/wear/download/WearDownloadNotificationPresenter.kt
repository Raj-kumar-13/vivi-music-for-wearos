package com.music.vivi.wear.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.Download
import com.music.vivi.wear.MainActivity
import com.music.vivi.wear.R

/**
 * Notification presenter for download progress on Wear OS.
 */
@UnstableApi
class WearDownloadNotificationPresenter(
    private val context: Context,
    private val downloads: List<Download>
) {

    companion object {
        @Volatile
        private var channelCreated = false
    }

    fun getNotification(): Notification {
        val channelId = "wear_download_channel"
        if (!channelCreated) {
            createNotificationChannel(channelId)
            channelCreated = true
        }

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalDownloads = downloads.size
        val completedDownloads = downloads.count { it.state == Download.STATE_COMPLETED }
        val progress = if (totalDownloads > 0) {
            val perDownloadProgress = downloads.sumOf { download ->
                if (download.state == Download.STATE_COMPLETED) 100.0
                else download.percentDownloaded.toDouble()
            }
            (perDownloadProgress / totalDownloads).toInt()
        } else {
            0
        }

        return NotificationCompat.Builder(context, channelId)
            .setContentTitle("Vivi Wear Downloads")
            .setContentText("$completedDownloads of $totalDownloads downloads completed")
            .setSmallIcon(R.drawable.exo_icon_play)
            .setOngoing(true)
            .setProgress(100, progress, false)
            .setContentIntent(pendingIntent)
            .build()
    }

    private fun createNotificationChannel(channelId: String) {
        val channel = NotificationChannel(
            channelId,
            "Wear OS Downloads",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Music download progress"
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager?.createNotificationChannel(channel)
    }
}
