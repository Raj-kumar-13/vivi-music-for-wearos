package com.music.vivi.wear.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import com.google.android.horologist.audio.AudioOutput
import com.google.android.horologist.audio.SystemAudioManager
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.logging.ErrorReporter
import com.google.android.horologist.media3.loader.Media3UaLoader
import com.google.android.horologist.media3.offload.AudioOffloadManager
import com.google.android.horologist.media.repository.PlayerRepository
import com.google.android.horologist.media.ui.state.model.PlaylistUiModel
import com.google.android.horologist.media3.util.toMediaItem
import com.music.vivi.wear.data.WearYouTubeRepository
import com.music.vivi.wear.data.models.WearSong
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Wear Player ViewModel that wraps Horologist's PlayerViewModel.
 * Provides battery-optimized playback using Horologist's components.
 */
@UnstableApi
@HiltViewModel
class WearPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: android.content.Context,
    private val youTubeRepository: WearYouTubeRepository,
    private val audioOffloadManager: AudioOffloadManager,
    private val audioOutputSelector: AudioOutputSelector,
    private val errorReporter: ErrorReporter
) : ViewModel() {

    private val _playerViewModel = MutableStateFlow<com.google.android.horologist.media.ui.state.PlayerViewModel?>(null)
    val playerViewModel: StateFlow<com.google.android.horologist.media.ui.state.PlayerViewModel?> = _playerViewModel.asStateFlow()

    private val _currentSong = MutableStateFlow<WearSong?>(null)
    val currentSong: StateFlow<WearSong?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _playbackPosition = MutableStateFlow(0L)
    val playbackPosition: StateFlow<Long> = _playbackPosition.asStateFlow()

    private val _playbackDuration = MutableStateFlow(0L)
    val playbackDuration: StateFlow<Long> = _playbackDuration.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    init {
        initializePlayerViewModel()
    }

    private fun initializePlayerViewModel() {
        viewModelScope.launch {
            try {
                // Create battery-optimized PlayerViewModel using Horologist's components
                val systemAudioManager = SystemAudioManager(context)
                val audioOutput: AudioOutput = systemAudioManager

                val playerRepository = PlayerRepository.Builder()
                    .setAudioOffloadManager(audioOffloadManager)
                    .setAudioOutputSelector(audioOutputSelector)
                    .build()

                val playerViewModel = com.google.android.horologist.media.ui.state.PlayerViewModel(
                    repository = playerRepository,
                    errorReporter = errorReporter
                )

                _playerViewModel.value = playerViewModel
                Timber.d("Horologist PlayerViewModel initialized")
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize PlayerViewModel")
                _error.value = "Failed to initialize player"
            }
        }
    }

    fun playSong(song: WearSong) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Timber.d("Playing song: ${song.title}")
                
                // Get stream URL
                val streamUrlResult = youTubeRepository.getStreamUrl(song.id)
                if (streamUrlResult.isSuccess) {
                    val streamUrl = streamUrlResult.getOrNull()
                    if (streamUrl != null) {
                        _currentSong.value = song
                        
                        // Create MediaItem and play using Horologist's PlayerViewModel
                        val mediaItem = MediaItem.fromUri(streamUrl)
                        _playerViewModel.value?.playMediaItem(mediaItem)
                        
                        _isPlaying.value = true
                    } else {
                        _error.value = "No stream URL found"
                    }
                } else {
                    _error.value = "Failed to get stream URL"
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to play song")
                _error.value = e.message ?: "Unknown error"
                errorReporter.reportError(e, "playSong")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        _playerViewModel.value?.playPause()
    }

    fun seekTo(position: Long) {
        _playerViewModel.value?.seekTo(position)
    }

    fun skipToNext() {
        _playerViewModel.value?.skipToNext()
    }

    fun skipToPrevious() {
        _playerViewModel.value?.skipToPrevious()
    }

    fun clearError() {
        _error.value = null
    }

    override fun onCleared() {
        super.onCleared()
        _playerViewModel.value?.release()
    }
}