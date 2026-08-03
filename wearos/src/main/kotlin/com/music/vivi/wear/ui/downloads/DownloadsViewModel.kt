package com.music.vivi.wear.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.db.dao.WearDownloadDao
import com.music.vivi.wear.db.dao.WearSongDao
import com.music.vivi.wear.download.WearDownloadManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: WearDownloadManager,
    private val downloadDao: WearDownloadDao,
    private val songDao: WearSongDao,
    @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context
) : ViewModel() {

    private val _downloads = MutableStateFlow<List<DownloadItem>>(emptyList())
    val downloads: StateFlow<List<DownloadItem>> = _downloads.asStateFlow()

    private val _storageUsage = MutableStateFlow(StorageUsage(0L, 0L))
    val storageUsage: StateFlow<StorageUsage> = _storageUsage.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadDownloads()
        loadStorageUsage()
    }

    private fun loadDownloads() {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                // Load from database
                val downloadEntities = downloadDao.getAllDownloads()
                val downloadItems = downloadEntities.map { entity ->
                    val song = WearSong(
                        id = entity.songId,
                        title = entity.title ?: "Unknown",
                        artist = entity.artist,
                        album = entity.album,
                        thumbnailUrl = entity.thumbnailUrl,
                        duration = entity.duration
                    )
                    
                    // Get current download state from DownloadManager
                    val downloadState = downloadManager.getDownloadState(entity.songId)
                    val state = when (downloadState?.state) {
                        androidx.media3.download.Download.STATE_DOWNLOADING -> DownloadState.DOWNLOADING
                        androidx.media3.download.Download.STATE_COMPLETED -> DownloadState.COMPLETED
                        androidx.media3.download.Download.STATE_FAILED -> DownloadState.FAILED
                        else -> DownloadState.PENDING
                    }
                    
                    val progress = downloadState?.let {
                        if (it.downloadedBytes > 0 && it.totalBytes > 0) {
                            it.downloadedBytes.toFloat() / it.totalBytes.toFloat()
                        } else {
                            0f
                        }
                    } ?: 0f

                    DownloadItem(song, progress, state)
                }
                _downloads.value = downloadItems
            } catch (e: Exception) {
                Timber.e(e, "Failed to load downloads")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private fun loadStorageUsage() {
        viewModelScope.launch {
            try {
                val usedBytes = downloadManager.getTotalDownloadSize()
                // For Wear OS, we'll use a conservative estimate of available storage
                // In production, you'd use the actual available storage from the system
                val totalBytes = 2L * 1024 * 1024 * 1024 // Assume 2GB total for simplicity
                _storageUsage.value = StorageUsage(usedBytes, totalBytes)
            } catch (e: Exception) {
                Timber.e(e, "Failed to load storage usage")
            }
        }
    }

    fun removeDownload(songId: String) {
        viewModelScope.launch {
            try {
                downloadManager.removeDownload(songId)
                // Remove from database
                downloadDao.deleteDownload(songId)
                Timber.d("Removed download: $songId")
                // Reload downloads
                loadDownloads()
                loadStorageUsage()
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove download: $songId")
            }
        }
    }

    fun refreshDownloads() {
        loadDownloads()
        loadStorageUsage()
    }

    fun runManualCleanup() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                // Trigger immediate cleanup
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<LibraryCleanupWorker>()
                    .build()
                androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
                Timber.d("Manual cleanup triggered")
                // Refresh after cleanup
                kotlinx.coroutines.delay(2000) // Wait for cleanup to complete
                refreshDownloads()
            } catch (e: Exception) {
                Timber.e(e, "Manual cleanup failed")
            } finally {
                _isLoading.value = false
            }
        }
    }
}