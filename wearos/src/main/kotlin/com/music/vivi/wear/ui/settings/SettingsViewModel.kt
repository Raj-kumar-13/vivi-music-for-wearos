package com.music.vivi.wear.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.vivi.wear.auth.WearAuthStorage
import com.music.vivi.wear.network.StreamingQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val authStorage: WearAuthStorage
) : ViewModel() {

    private val _audioQuality = MutableStateFlow(StreamingQuality.MEDIUM)
    val audioQuality: StateFlow<StreamingQuality> = _audioQuality.asStateFlow()

    private val _downloadSizeCap = MutableStateFlow(1024L) // 1GB default
    val downloadSizeCap: StateFlow<Long> = _downloadSizeCap.asStateFlow()

    private val _authStatus = MutableStateFlow(false)
    val authStatus: StateFlow<Boolean> = _authStatus.asStateFlow()

    private val _versionInfo = MutableStateFlow("0.1.0")
    val versionInfo: StateFlow<String> = _versionInfo.asStateFlow()

    init {
        loadSettings()
    }

    private fun loadSettings() {
        viewModelScope.launch {
            try {
                // Load audio quality preference
                val savedQuality = authStorage.getAudioQuality()
                _audioQuality.value = savedQuality ?: StreamingQuality.MEDIUM

                // Load download size cap preference
                val savedCap = authStorage.getDownloadSizeCap()
                _downloadSizeCap.value = savedCap ?: 1024L

                // Check auth status
                _authStatus.value = authStorage.hasAuthCookie()

                // Load version info
                _versionInfo.value = com.music.vivi.wear.BuildConfig.VERSION_NAME

                Timber.d("Settings loaded successfully")
            } catch (e: Exception) {
                Timber.e(e, "Failed to load settings")
            }
        }
    }

    fun cycleAudioQuality() {
        val qualities = listOf(
            StreamingQuality.HIGH,
            StreamingQuality.MEDIUM,
            StreamingQuality.LOW
        )
        val currentIndex = qualities.indexOf(_audioQuality.value)
        val nextIndex = (currentIndex + 1) % qualities.size
        val previousValue = _audioQuality.value
        val newValue = qualities[nextIndex]

        viewModelScope.launch {
            try {
                authStorage.saveAudioQuality(newValue)
                _audioQuality.value = newValue
                Timber.d("Audio quality changed to $newValue")
            } catch (e: Exception) {
                _audioQuality.value = previousValue
                Timber.e(e, "Failed to save audio quality")
            }
        }
    }

    fun cycleDownloadSizeCap() {
        val caps = listOf(512L, 1024L, 2048L, 3072L) // 512MB, 1GB, 2GB, 3GB
        val currentIndex = caps.indexOf(_downloadSizeCap.value)
        val nextIndex = (currentIndex + 1) % caps.size
        val previousValue = _downloadSizeCap.value
        val newValue = caps[nextIndex]

        viewModelScope.launch {
            try {
                authStorage.saveDownloadSizeCap(newValue)
                _downloadSizeCap.value = newValue
                Timber.d("Download size cap changed to ${newValue}MB")
            } catch (e: Exception) {
                _downloadSizeCap.value = previousValue
                Timber.e(e, "Failed to save download size cap")
            }
        }
    }

    fun requestAuthReconnect() {
        viewModelScope.launch {
            try {
                // Trigger auth reconnect flow
                // This would typically show a prompt to send auth from phone
                Timber.d("Auth reconnect requested")
                _authStatus.value = authStorage.hasAuthCookie()
            } catch (e: Exception) {
                Timber.e(e, "Failed to request auth reconnect")
            }
        }
    }
}