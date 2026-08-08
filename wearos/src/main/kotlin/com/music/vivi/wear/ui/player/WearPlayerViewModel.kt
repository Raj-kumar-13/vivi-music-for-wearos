package com.music.vivi.wear.ui.player

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.util.UnstableApi
import com.google.android.horologist.media3.audio.AudioOutputSelector
import com.google.android.horologist.media3.logging.ErrorReporter
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
 * Wear Player ViewModel.
 */
@UnstableApi
@HiltViewModel
class WearPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: WearYouTubeRepository,
    private val audioOutputSelector: AudioOutputSelector,
    private val errorReporter: ErrorReporter
) : ViewModel() {

    private val _currentSong = MutableStateFlow<WearSong?>(null)
    val currentSong: StateFlow<WearSong?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    fun playSong(song: WearSong) {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                Timber.d("Playing song: ${song.title}")
                
                val streamUrlResult = youTubeRepository.getStreamUrl(song.id)
                if (streamUrlResult.isSuccess) {
                    val streamUrl = streamUrlResult.getOrNull()
                    if (streamUrl != null) {
                        _currentSong.value = song
                        _isPlaying.value = true
                        // Playback logic would go here
                    } else {
                        _error.value = "No stream URL found"
                    }
                } else {
                    _error.value = "Failed to get stream URL"
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to play song")
                _error.value = e.message ?: "Unknown error"
                errorReporter.logMessage(
                    "Error playing song: ${e.message}",
                    ErrorReporter.Category.Playback,
                    ErrorReporter.Level.Error
                )
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun togglePlayPause() {
        _isPlaying.value = !_isPlaying.value
    }

    fun clearError() {
        _error.value = null
    }
}
