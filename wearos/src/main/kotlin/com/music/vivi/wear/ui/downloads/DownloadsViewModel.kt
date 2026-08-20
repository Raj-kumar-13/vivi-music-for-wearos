package com.music.vivi.wear.ui.downloads

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.db.dao.WearDownloadDao
import com.music.vivi.wear.download.WearDownloadManager
import com.music.vivi.wear.worker.LibraryCleanupWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class DownloadsViewModel @Inject constructor(
    private val downloadManager: WearDownloadManager,
    private val downloadDao: WearDownloadDao,
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
            combine(
                downloadDao.getDownloadsWithSongs(),
                downloadManager.downloads
            ) { dbDownloads, activeDownloads ->
                dbDownloads.map { item ->
                    val song = WearSong(
                        id = item.song.id,
                        title = item.song.title,
                        artist = item.song.artist,
                        album = item.song.album,
                        thumbnailUrl = item.song.thumbnailUrl,
                        duration = item.song.duration
                    )
                    
                    val activeDownload = activeDownloads[item.download.songId]
                    val state = when (activeDownload?.state) {
                        androidx.media3.exoplayer.offline.Download.STATE_DOWNLOADING -> DownloadState.DOWNLOADING
                        androidx.media3.exoplayer.offline.Download.STATE_COMPLETED -> DownloadState.COMPLETED
                        androidx.media3.exoplayer.offline.Download.STATE_FAILED -> DownloadState.FAILED
                        else -> if (activeDownload != null) DownloadState.PENDING else DownloadState.COMPLETED
                    }
                    
                    val progress = activeDownload?.let {
                        if (it.bytesDownloaded > 0 && it.contentLength > 0) {
                            it.bytesDownloaded.toFloat() / it.contentLength.toFloat()
                        } else if (it.state == androidx.media3.exoplayer.offline.Download.STATE_COMPLETED) {
                            1f
                        } else {
                            0f
                        }
                    } ?: 1f

                    DownloadItem(song, progress, state)
                }
            }.collect {
                _downloads.value = it
                _isLoading.value = false
            }
        }
    }

    private fun loadStorageUsage() {
        viewModelScope.launch {
            try {
                val usedBytes = downloadManager.getTotalDownloadSize()
                val stat = android.os.StatFs(context.filesDir.absolutePath)
                val totalBytes = stat.totalBytes
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
                downloadDao.deleteDownloadById(songId)
                Timber.d("Removed download: $songId")
                // loadDownloads is already collecting a flow, so it will update automatically
                loadStorageUsage()
            } catch (e: Exception) {
                Timber.e(e, "Failed to remove download: $songId")
            }
        }
    }

    fun refreshDownloads() {
        loadStorageUsage()
    }

    fun runManualCleanup() {
        viewModelScope.launch {
            try {
                _isLoading.value = true
                val workRequest = androidx.work.OneTimeWorkRequestBuilder<LibraryCleanupWorker>()
                    .build()
                val workManager = androidx.work.WorkManager.getInstance(context)
                workManager.enqueue(workRequest)
                Timber.d("Manual cleanup triggered")
                // Observe work completion instead of arbitrary delay
                workManager.getWorkInfoByIdFlow(workRequest.id).collect { workInfo ->
                    if (workInfo != null && workInfo.state.isFinished) {
                        refreshDownloads()
                        _isLoading.value = false
                        return@collect
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Manual cleanup failed")
                _isLoading.value = false
            }
        }
    }
}

data class DownloadItem(
    val song: WearSong,
    val progress: Float,
    val state: DownloadState
)

enum class DownloadState {
    PENDING,
    DOWNLOADING,
    COMPLETED,
    FAILED
}

data class StorageUsage(
    val usedBytes: Long,
    val totalBytes: Long
)
