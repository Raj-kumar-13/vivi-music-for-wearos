package com.music.vivi.wear.ui.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.db.dao.WearPlaylistDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class PlaylistViewModel @Inject constructor(
    private val playlistDao: WearPlaylistDao
) : ViewModel() {

    private val _playlistInfo = MutableStateFlow<PlaylistInfo?>(null)
    val playlistInfo: StateFlow<PlaylistInfo?> = _playlistInfo.asStateFlow()

    private val _playlistSongs = MutableStateFlow<List<WearSong>>(emptyList())
    val playlistSongs: StateFlow<List<WearSong>> = _playlistSongs.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun loadPlaylist(playlistId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Timber.d("Loading playlist: $playlistId")
                
                // Load playlist with songs
                val playlistWithSongs = playlistDao.getPlaylistWithSongs(playlistId)
                if (playlistWithSongs != null) {
                    _playlistInfo.value = PlaylistInfo(
                        id = playlistWithSongs.playlist.id,
                        name = playlistWithSongs.playlist.name,
                        songCount = playlistWithSongs.songs.size
                    )

                    _playlistSongs.value = playlistWithSongs.songs.map { songEntity ->
                        WearSong(
                            id = songEntity.id,
                            title = songEntity.title,
                            artist = songEntity.artist,
                            album = songEntity.album,
                            thumbnailUrl = songEntity.thumbnailUrl,
                            duration = songEntity.duration
                        )
                    }
                } else {
                    _error.value = "Playlist not found"
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to load playlist")
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
