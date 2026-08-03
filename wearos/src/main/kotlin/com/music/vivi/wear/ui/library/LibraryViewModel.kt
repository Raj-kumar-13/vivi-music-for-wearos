package com.music.vivi.wear.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.db.dao.WearPlaybackHistoryDao
import com.music.vivi.wear.db.dao.WearPlaylistDao
import com.music.vivi.wear.db.dao.WearDownloadDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel @Inject constructor(
    private val playbackHistoryDao: WearPlaybackHistoryDao,
    private val playlistDao: WearPlaylistDao,
    private val downloadDao: WearDownloadDao
) : ViewModel() {

    private val _recentSongs = MutableStateFlow<List<WearSong>>(emptyList())
    val recentSongs: StateFlow<List<WearSong>> = _recentSongs.asStateFlow()

    private val _playlists = MutableStateFlow<List<PlaylistInfo>>(emptyList())
    val playlists: StateFlow<List<PlaylistInfo>> = _playlists.asStateFlow()

    private val _downloadedSongs = MutableStateFlow<List<WearSong>>(emptyList())
    val downloadedSongs: StateFlow<List<WearSong>> = _downloadedSongs.asStateFlow()

    init {
        loadLibraryData()
    }

    private fun loadLibraryData() {
        viewModelScope.launch {
            // Load recent songs from playback history
            playbackHistoryDao.getRecentHistory(limit = 10)
                .collect { historyEntities ->
                    _recentSongs.value = historyEntities.map { entity ->
                        WearSong(
                            id = entity.songId,
                            title = entity.title ?: "Unknown",
                            artist = entity.artist,
                            album = entity.album,
                            thumbnailUrl = entity.thumbnailUrl,
                            duration = entity.duration
                        )
                    }
                }
        }

        viewModelScope.launch {
            // Load playlists
            playlistDao.getAllPlaylists()
                .collect { playlistEntities ->
                    _playlists.value = playlistEntities.map { entity ->
                        PlaylistInfo(
                            id = entity.id.toString(),
                            name = entity.name,
                            songCount = entity.songCount ?: 0
                        )
                    }
                }
        }

        viewModelScope.launch {
            // Load downloaded songs
            downloadDao.getAllDownloads()
                .collect { downloadEntities ->
                    _downloadedSongs.value = downloadEntities.map { entity ->
                        WearSong(
                            id = entity.songId,
                            title = entity.title ?: "Unknown",
                            artist = entity.artist,
                            album = entity.album,
                            thumbnailUrl = entity.thumbnailUrl,
                            duration = entity.duration
                        )
                    }
                }
        }
    }

    fun refreshLibrary() {
        loadLibraryData()
    }
}

data class PlaylistInfo(
    val id: String,
    val name: String,
    val songCount: Int
)