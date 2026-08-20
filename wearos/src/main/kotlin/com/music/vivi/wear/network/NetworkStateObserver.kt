package com.music.vivi.wear.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.networks.data.Status
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel to observe and manage network state across the application.
 * Provides reactive state for UI components to adapt to connectivity changes.
 */
@OptIn(ExperimentalHorologistApi::class)
@HiltViewModel
class NetworkStateObserver @Inject constructor(
    private val connectivityManager: ConnectivityManager
) : ViewModel() {

    private val _networkState = MutableStateFlow<NetworkState>(NetworkState.Unknown)
    val networkState: StateFlow<NetworkState> = _networkState.asStateFlow()

    private val _streamingQuality = MutableStateFlow(StreamingQuality.MEDIUM)
    val streamingQuality: StateFlow<StreamingQuality> = _streamingQuality.asStateFlow()

    private val _isOfflineMode = MutableStateFlow(false)
    val isOfflineMode: StateFlow<Boolean> = _isOfflineMode.asStateFlow()

    private val _connectivityMode = MutableStateFlow(ConnectivityMode.UNKNOWN)
    val connectivityMode: StateFlow<ConnectivityMode> = _connectivityMode.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityManager.networkStatus.collect { networks ->
                updateNetworkState(networks.status)
            }
        }

        viewModelScope.launch {
            connectivityManager.connectivityMode.collect { mode ->
                _connectivityMode.value = mode
                _streamingQuality.value = mode.getStreamingQuality()
                _isOfflineMode.value = mode == ConnectivityMode.OFFLINE
                Timber.d("Connectivity mode: $mode, Streaming quality: ${_streamingQuality.value}")
            }
        }
    }

    private fun updateNetworkState(status: Status) {
        val newState = when (status) {
            is Status.Available -> NetworkState.Available
            is Status.Losing -> NetworkState.Transient
            is Status.Lost -> NetworkState.Lost
            is Status.Unknown -> NetworkState.Unknown
            else -> NetworkState.Unavailable
        }
        _networkState.value = newState
    }

    fun forceOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        Timber.d("Force offline mode: $enabled")
    }

    override fun onCleared() {
        super.onCleared()
        // viewModelScope cancellation automatically stops our flow collections
        // Do NOT call connectivityManager.stopObserving() — it's a singleton shared by other components
    }
}

/**
 * Simple network state for UI consumption.
 */
sealed class NetworkState {
    object Available : NetworkState()
    object Lost : NetworkState()
    object Recovering : NetworkState()
    object Transient : NetworkState()
    object Unknown : NetworkState()
    object Unavailable : NetworkState()
}
