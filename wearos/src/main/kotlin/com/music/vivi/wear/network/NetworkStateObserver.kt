package com.music.vivi.wear.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            connectivityManager.networkStatus.collect { status ->
                updateNetworkState(status)
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

    private fun updateNetworkState(status: com.google.android.horologist.networks.awareness.NetworkStatus) {
        val newState = when (status) {
            is com.google.android.horologist.networks.awareness.NetworkStatus.Available -> NetworkState.Available
            is com.google.android.horologist.networks.awareness.NetworkStatus.Lost -> NetworkState.Lost
            is com.google.android.horologist.networks.awareness.NetworkStatus.Recovering -> NetworkState.Recovering
            is com.google.android.horologist.networks.awareness.NetworkStatus.Transient -> NetworkState.Transient
            is com.google.android.horologist.networks.awareness.NetworkStatus.Unavailable -> NetworkState.Unavailable
        }
        _networkState.value = newState
    }

    fun forceOfflineMode(enabled: Boolean) {
        _isOfflineMode.value = enabled
        Timber.d("Force offline mode: $enabled")
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.stopObserving()
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