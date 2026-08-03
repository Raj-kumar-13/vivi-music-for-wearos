package com.music.vivi.wear.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.download.Download
import androidx.media3.download.DownloadManager
import androidx.media3.download.DownloadNotificationHelper
import androidx.media3.download.DownloadService
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages music downloads for Wear OS using Media3 DownloadManager.
 * Implements separate cache pools for streaming vs. explicit downloads.
 * Battery-optimized with conservative parallel download limits.
 */
@UnstableApi
@Singleton
class WearDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val STREAM_CACHE_SIZE = 200L * 1024 * 1024 // 200MB for streaming
        private const val DOWNLOAD_CACHE_NAME = "wear_downloads"
        private const val STREAM_CACHE_NAME = "wear_stream_cache"
        private const val MAX_PARALLEL_DOWNLOADS = 1 // Conservative for battery
    }

    private val streamCache: Cache by lazy {
        val streamCacheDir = File(context.cacheDir, STREAM_CACHE_NAME)
        SimpleCache(
            streamCacheDir,
            LeastRecentlyUsedCacheEvictor(STREAM_CACHE_SIZE),
            StandaloneDatabaseProvider(context)
        )
    }

    private lateinit var downloadManager: DownloadManager
    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    fun initialize() {
        if (!::downloadManager.isInitialized) {
            val downloadDirectory = File(context.getExternalFilesDir(null), DOWNLOAD_CACHE_NAME)
            downloadDirectory.mkdirs()

            downloadManager = DownloadManager.Builder(
                context,
                DownloadIndexImpl(StandaloneDatabaseProvider(context), DOWNLOAD_CACHE_NAME),
                DownloadService::class.java
            )
                .setMaxParallelDownloads(MAX_PARALLEL_DOWNLOADS) // Battery-optimized
                .build()

            downloadNotificationHelper = DownloadNotificationHelper(context, "Vivi Wear Downloads")

            Timber.d("WearDownloadManager initialized")
        }
    }

    /**
     * Download a song for offline playback.
     * @param songId The YouTube video ID
     * @param streamUrl The stream URL to download
     * @param title Song title for notification
     */
    fun downloadSong(songId: String, streamUrl: String, title: String) {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        val request = DownloadRequest.Builder(songId, streamUrl.toUri())
            .setCustomKey("title", title)
            .build()

        downloadManager.send(request)

        Timber.d("Started download for song: $title")
    }

    /**
     * Remove a downloaded song.
     * @param songId The YouTube video ID
     */
    fun removeDownload(songId: String) {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        downloadManager.remove(songId)
        Timber.d("Removed download for song: $songId")
    }

    /**
     * Get the download state for a specific song.
     * @param songId The YouTube video ID
     * @return Flow of Download state or null if not downloaded
     */
    fun getDownloadState(songId: String): Flow<Download?> {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        return downloadManager.downloads.map { downloads ->
            downloads[songId]
        }
    }

    /**
     * Get all current downloads.
     * @return Flow of all downloads
     */
    fun getAllDownloads(): Flow<Map<String, Download>> {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        return downloadManager.downloads
    }

    /**
     * Get the stream cache for streaming playback.
     */
    fun getStreamCache(): Cache {
        return streamCache
    }

    /**
     * Get the total size of all downloads in bytes.
     */
    fun getTotalDownloadSize(): Long {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        var totalSize = 0L
        downloadManager.downloads.value.values.forEach { download ->
            totalSize += download.downloadedBytes
        }
        return totalSize
    }

    /**
     * Get the number of downloaded songs.
     */
    fun getDownloadCount(): Int {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        return downloadManager.downloads.value.size
    }

    /**
     * Clean up resources.
     */
    fun release() {
        if (::downloadManager.isInitialized) {
            downloadManager.release()
        }
        streamCache.release()
    }
}

private fun String.toUri() = android.net.Uri.parse(this)