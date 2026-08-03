package com.music.vivi.wear.download

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.media3.download.Download
import androidx.media3.download.NotificationManager.Presenter

/**
 * Notification presenter for download progress on Wear OS.
 */
class WearDownloadNotificationPresenter(
    private val context: Context,
    private val downloads: MutableMap<String, Download>
) : Presenter {

    override fun getNotification(): Notification {
        // Create a simple notification showing download progress
        val channelId = "wear_download_channel"
        createNotificationChannel(channelId)

        val intent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val totalDownloads = downloads.size
        val completedDownloads = downloads.values.count { it.state == Download.STATE_COMPLETED }
        val progress = if (totalDownloads > 0) {
            (completedDownloads * 100) / totalDownloads
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Wear OS Downloads",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Music download progress"
            }

            val notificationManager = context.getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
}