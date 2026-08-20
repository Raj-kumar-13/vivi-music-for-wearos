package com.music.vivi.wear.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.db.dao.WearPlaybackHistoryDao
import com.music.vivi.wear.db.dao.WearPlaylistDao
import com.music.vivi.wear.db.dao.WearDownloadDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
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

    private var recentSongsJob: Job? = null
    private var playlistsJob: Job? = null
    private var downloadedSongsJob: Job? = null

    init {
        loadLibraryData()
    }

    private fun loadLibraryData() {
        recentSongsJob?.cancel()
        playlistsJob?.cancel()
        downloadedSongsJob?.cancel()

        recentSongsJob = viewModelScope.launch {
            // Load recent songs from playback history
            playbackHistoryDao.getRecentHistoryWithSongs(limit = 10)
                .collect { historyWithSongs ->
                    _recentSongs.value = historyWithSongs.map { item ->
                        val song = item.song
                        WearSong(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            thumbnailUrl = song.thumbnailUrl,
                            duration = song.duration
                        )
                    }
                }
        }

        playlistsJob = viewModelScope.launch {
            // Load playlists with song count
            playlistDao.getAllPlaylistsWithCount()
                .collect { playlistWithCounts ->
                    _playlists.value = playlistWithCounts.map { item ->
                        PlaylistInfo(
                            id = item.playlist.id,
                            name = item.playlist.name,
                            songCount = item.songCount
                        )
                    }
                }
        }

        downloadedSongsJob = viewModelScope.launch {
            // Load downloaded songs
            downloadDao.getDownloadsWithSongs()
                .collect { downloadWithSongs ->
                    _downloadedSongs.value = downloadWithSongs.map { item ->
                        val song = item.song
                        WearSong(
                            id = song.id,
                            title = song.title,
                            artist = song.artist,
                            album = song.album,
                            thumbnailUrl = song.thumbnailUrl,
                            duration = song.duration
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
