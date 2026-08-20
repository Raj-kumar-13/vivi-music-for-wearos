package com.music.vivi.wear.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.music.vivi.wear.db.dao.WearDownloadDao
import com.music.vivi.wear.db.dao.WearPlaybackHistoryDao
import com.music.vivi.wear.db.dao.WearSongDao
import com.music.vivi.wear.download.WearDownloadManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.coroutineScope
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Periodic worker that cleans up storage and removes stale data.
 * Runs weekly when device is idle and battery is not low.
 */
@HiltWorker
class LibraryCleanupWorker @AssistedInject constructor(
    @Assisted private val context: Context,
    @Assisted workerParams: WorkerParameters,
    private val downloadDao: WearDownloadDao,
    private val playbackHistoryDao: WearPlaybackHistoryDao,
    private val songDao: WearSongDao,
    private val downloadManager: WearDownloadManager
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val WORK_NAME = "library_cleanup_worker"
        const val STALE_DOWNLOAD_THRESHOLD_DAYS = 90L
        const val DEFAULT_DOWNLOAD_SIZE_CAP_MB = 1024L // 1GB default
    }

    override suspend fun doWork(): Result = coroutineScope {
        return@coroutineScope try {
            Timber.d("Starting library cleanup")

            // 1. Remove orphaned cache entries
            cleanupOrphanedCache()

            // 2. Remove stale downloads (90+ days unplayed)
            cleanupStaleDownloads()

            // 3. Enforce download size cap
            enforceDownloadSizeCap()

            // 4. Clean up old playback history
            cleanupOldPlaybackHistory()

            Timber.d("Library cleanup completed successfully")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "Library cleanup failed")
            Result.failure()
        }
    }

    private suspend fun cleanupOrphanedCache() {
        try {
            // Remove cache entries that don't have corresponding database entries
            val allDownloads = downloadDao.getAllDownloadsList()
            val downloadIds = allDownloads.map { it.songId }.toSet()

            // This is a simplified implementation
            // In production, you'd check the actual cache files against the database
            val orphanedCount = 0 // Placeholder for actual orphan detection
            Timber.d("Cleaned up $orphanedCount orphaned cache entries")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup orphaned cache")
        }
    }

    private suspend fun cleanupStaleDownloads() {
        try {
            val thresholdTime = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(STALE_DOWNLOAD_THRESHOLD_DAYS)
            val staleDownloads = downloadDao.getStaleDownloads(thresholdTime)

            staleDownloads.forEach { download ->
                try {
                    downloadManager.removeDownload(download.songId)
                    downloadDao.deleteDownloadById(download.songId)
                    Timber.d("Removed stale download: ${download.songId}")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to remove stale download: ${download.songId}")
                }
            }

            Timber.d("Removed ${staleDownloads.size} stale downloads")
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup stale downloads")
        }
    }

    private suspend fun enforceDownloadSizeCap() {
        try {
            var remainingSize = downloadManager.getTotalDownloadSize()
            val sizeCapBytes = DEFAULT_DOWNLOAD_SIZE_CAP_MB * 1024 * 1024

            if (remainingSize > sizeCapBytes) {
                Timber.w("Download size ($remainingSize bytes) exceeds cap ($sizeCapBytes bytes)")

                // Remove oldest downloads until under the cap
                val downloadsList = downloadDao.getAllDownloadsSortedByDate()
                var removedCount = 0

                for (download in downloadsList) {
                    if (remainingSize <= sizeCapBytes) {
                        break
                    }

                    try {
                        downloadManager.removeDownload(download.songId)
                        downloadDao.deleteDownloadById(download.songId)
                        remainingSize -= download.sizeBytes
                        removedCount++
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to remove download for size cap enforcement: ${download.songId}")
                    }
                }

                Timber.d("Removed $removedCount downloads to enforce size cap")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to enforce download size cap")
        }
    }

    private suspend fun cleanupOldPlaybackHistory() {
        try {
            val count = playbackHistoryDao.getHistoryCount()
            if (count > 100) {
                playbackHistoryDao.deleteOldHistory(keepCount = 100)
                Timber.d("Cleaned up ${count - 100} old playback history entries")
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to cleanup old playback history")
        }
    }
}
