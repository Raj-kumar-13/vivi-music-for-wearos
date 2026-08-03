package com.music.vivi.wear.download

import android.content.Context
import androidx.media3.download.DownloadManager
import androidx.media3.database.StandaloneDatabaseProvider

/**
 * Singleton provider for WearDownloadManager to ensure single instance across the app.
 */
object WearDownloadManagerProvider {
    private var downloadManager: DownloadManager? = null

    fun getInstance(context: Context): WearDownloadManagerProvider {
        if (downloadManager == null) {
            downloadManager = DownloadManager.Builder(
                context,
                DownloadIndexImpl(StandaloneDatabaseProvider(context), "wear_downloads"),
                WearDownloadService::class.java
            )
                .setMaxParallelDownloads(2)
                .build()
        }
        return this
    }

    val downloadManager: DownloadManager
        get() = requireNotNull(downloadManager) { "DownloadManager not initialized" }
}