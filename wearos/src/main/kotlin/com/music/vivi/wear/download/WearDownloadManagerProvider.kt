package com.music.vivi.wear.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import java.io.File
import java.util.concurrent.Executors

/**
 * Singleton provider for WearDownloadManager to ensure single instance across the app.
 */
@UnstableApi
object WearDownloadManagerProvider {
    private var _downloadManager: DownloadManager? = null

    fun getInstance(context: Context): WearDownloadManagerProvider {
        if (_downloadManager == null) {
            val databaseProvider = StandaloneDatabaseProvider(context)
            val downloadDir = File(context.getExternalFilesDir(null), "downloads")
            val downloadCache = SimpleCache(downloadDir, LeastRecentlyUsedCacheEvictor(100 * 1024 * 1024), databaseProvider)
            
            _downloadManager = DownloadManager(
                context,
                databaseProvider,
                downloadCache,
                DefaultHttpDataSource.Factory(),
                Executors.newSingleThreadExecutor()
            ).apply {
                maxParallelDownloads = 2
            }
        }
        return this
    }

    val downloadManager: DownloadManager
        get() = requireNotNull(_downloadManager) { "DownloadManager not initialized" }
}
