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
import java.util.concurrent.TimeUnit
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
        private const val DOWNLOAD_CACHE_SIZE = 500L * 1024 * 1024 // 500MB for downloads
        private const val DOWNLOAD_CACHE_NAME = "wear_downloads"
        private const val STREAM_CACHE_NAME = "wear_stream_cache"
        private const val MAX_PARALLEL_DOWNLOADS = 1 // Conservative for battery
    }

    private val databaseProvider = StandaloneDatabaseProvider(context)

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(5, TimeUnit.MINUTES)
        .build()

    @Volatile
    private var _streamCache: Cache? = null
    private val streamCacheLock = Any()

    lateinit var downloadManager: DownloadManager
        private set
    private lateinit var downloadNotificationHelper: DownloadNotificationHelper

    private val _downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
    val downloads = _downloads.asStateFlow()

    private fun ensureInitialized() {
        if (!::downloadManager.isInitialized) {
            initialize()
        }
    }

    private fun initialize() {
        if (!::downloadManager.isInitialized) {
            val downloadDirectory = File(context.getExternalFilesDir(null), DOWNLOAD_CACHE_NAME)
            downloadDirectory.mkdirs()

            val downloadCache = SimpleCache(
                downloadDirectory,
                LeastRecentlyUsedCacheEvictor(DOWNLOAD_CACHE_SIZE),
                databaseProvider
            )

            downloadManager = DownloadManager(
                context,
                databaseProvider,
                downloadCache,
                OkHttpDataSource.Factory(okHttpClient),
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
        ensureInitialized()

        val request = DownloadRequest.Builder(songId, android.net.Uri.parse(streamUrl))
            .setData(title.toByteArray(Charsets.UTF_8))
            .build()

        downloadManager.addDownload(request)

        Timber.d("Started download for song: $title")
    }

    /**
     * Remove a downloaded song.
     */
    fun removeDownload(songId: String) {
        ensureInitialized()

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
        return _streamCache ?: synchronized(streamCacheLock) {
            _streamCache ?: SimpleCache(
                File(context.cacheDir, STREAM_CACHE_NAME),
                LeastRecentlyUsedCacheEvictor(STREAM_CACHE_SIZE),
                databaseProvider
            ).also { _streamCache = it }
        }
    }

    /**
     * Get the total size of all downloads in bytes.
     */
    fun getTotalDownloadSize(): Long {
        ensureInitialized()

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
        ensureInitialized()

        return _downloads.value.size
    }

    /**
     * Clean up resources.
     */
    fun release() {
        if (::downloadManager.isInitialized) {
            downloadManager.release()
        }
        synchronized(streamCacheLock) {
            _streamCache?.release()
            _streamCache = null
        }
    }
}
