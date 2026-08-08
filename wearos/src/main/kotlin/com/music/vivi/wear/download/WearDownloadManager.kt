package com.music.vivi.wear.download

import android.content.Context
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadNotificationHelper
import androidx.media3.exoplayer.offline.DownloadRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import okhttp3.OkHttpClient
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.Executors

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

    private val _streamCache: Cache by lazy {
        val streamCacheDir = File(context.cacheDir, STREAM_CACHE_NAME)
        SimpleCache(
            streamCacheDir,
            LeastRecentlyUsedCacheEvictor(STREAM_CACHE_SIZE),
            StandaloneDatabaseProvider(context)
        )
    }

    lateinit var downloadManager: DownloadManager
        private set
    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads = _downloads.asStateFlow()

    init {
        initialize()
    }

    private fun initialize() {
        if (!::downloadManager.isInitialized) {
            val downloadDirectory = File(context.getExternalFilesDir(null), DOWNLOAD_CACHE_NAME)
            downloadDirectory.mkdirs()

            val databaseProvider = StandaloneDatabaseProvider(context)
            val downloadCache = SimpleCache(
                downloadDirectory,
                LeastRecentlyUsedCacheEvictor(500 * 1024 * 1024),
                databaseProvider
            )

            downloadManager = DownloadManager(
                context,
                databaseProvider,
                downloadCache,
                OkHttpDataSource.Factory(OkHttpClient()),
                Executors.newSingleThreadExecutor()
            ).apply {
                maxParallelDownloads = MAX_PARALLEL_DOWNLOADS
                addListener(object : DownloadManager.Listener {
                    override fun onDownloadChanged(downloadManager: DownloadManager, download: Download, finalException: Exception?) {
                        updateDownloads()
                    }
                    override fun onDownloadRemoved(downloadManager: DownloadManager, download: Download) {
                        updateDownloads()
                    }
                })
            }
            updateDownloads()

            downloadNotificationHelper = DownloadNotificationHelper(context, "Vivi Wear Downloads")

            Timber.d("WearDownloadManager initialized")
        }
    }

    private fun updateDownloads() {
        val cursor = downloadManager.downloadIndex.getDownloads()
        val map = mutableMapOf<String, Download>()
        while (cursor.moveToNext()) {
            val download = cursor.download
            map[download.request.id] = download
        }
        cursor.close()
        _downloads.value = map
    }

    /**
     * Download a song for offline playback.
     */
    fun downloadSong(songId: String, streamUrl: String, title: String) {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        val request = DownloadRequest.Builder(songId, android.net.Uri.parse(streamUrl))
            .setData(title.toByteArray())
            .build()

        downloadManager.addDownload(request)

        Timber.d("Started download for song: $title")
    }

    /**
     * Remove a downloaded song.
     */
    fun removeDownload(songId: String) {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        downloadManager.removeDownload(songId)
        Timber.d("Removed download for song: $songId")
    }

    /**
     * Get the download state for a specific song.
     */
    fun getDownloadState(songId: String): Flow<Download?> {
        return _downloads.map { it[songId] }
    }

    /**
     * Get all current downloads.
     */
    fun getAllDownloads(): Flow<Map<String, Download>> {
        return downloads
    }

    /**
     * Get the stream cache for streaming playback.
     */
    fun getStreamCache(): Cache {
        return _streamCache
    }

    /**
     * Get the total size of all downloads in bytes.
     */
    fun getTotalDownloadSize(): Long {
        if (!::downloadManager.isInitialized) {
            initialize()
        }

        var totalSize = 0L
        _downloads.value.values.forEach { download ->
            totalSize += download.bytesDownloaded
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

        return _downloads.value.size
    }

    /**
     * Clean up resources.
     */
    fun release() {
        if (::downloadManager.isInitialized) {
            downloadManager.release()
        }
        _streamCache.release()
    }
}
