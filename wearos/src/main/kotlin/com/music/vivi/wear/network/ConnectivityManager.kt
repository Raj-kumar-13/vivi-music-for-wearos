package com.music.vivi.wear.network

import android.content.Context
import com.google.android.horologist.networks.awareness.NetworkAwareness
import com.google.android.horologist.networks.awareness.NetworkAwarenessObserver
import com.google.android.horologist.networks.awareness.NetworkStatus
import com.google.android.horologist.networks.awareness.NetworkStatus.Lost
import com.google.android.horologist.networks.awareness.NetworkStatus.Recovering
import com.google.android.horologist.networks.awareness.NetworkStatus.Transient
import com.google.android.horologist.networks.awareness.NetworkStatus.Unavailable
import com.google.android.horologist.networks.awareness.NetworkStatus.Available
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages network connectivity awareness for Wear OS.
 * Handles three connectivity modes: Bluetooth-relayed, standalone Wi-Fi, and offline.
 */
@Singleton
class ConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val _networkStatus = MutableStateFlow<NetworkStatus>(Available)
    val networkStatus: StateFlow<NetworkStatus> = _networkStatus.asStateFlow()

    private val _connectivityMode = MutableStateFlow<ConnectivityMode>(ConnectivityMode.UNKNOWN)
    val connectivityMode: StateFlow<ConnectivityMode> = _connectivityMode.asStateFlow()

    private val _isOnline = MutableStateFlow(true)
    val isOnline: StateFlow<Boolean> = _isOnline.asStateFlow()

    private val networkAwareness: NetworkAwareness by lazy {
        NetworkAwareness.create(context)
    }

    private val networkObserver: NetworkAwarenessObserver by lazy {
        networkAwareness.getNetworkStatusObserver()
    }

    fun initialize() {
        Timber.d("Initializing ConnectivityManager")
        networkObserver.startObserving()
        networkObserver.networkStatus.observeForever { status ->
            handleNetworkStatusChange(status)
        }
    }

    private fun handleNetworkStatusChange(status: NetworkStatus) {
        Timber.d("Network status changed: $status")
        _networkStatus.value = status

        when (status) {
            is Available -> {
                _isOnline.value = true
                _connectivityMode.value = determineConnectivityMode()
            }
            is Lost -> {
                _isOnline.value = false
                _connectivityMode.value = ConnectivityMode.OFFLINE
            }
            is Recovering -> {
                _isOnline.value = false
                _connectivityMode.value = ConnectivityMode.RECOVERING
            }
            is Transient -> {
                _isOnline.value = true
                _connectivityMode.value = determineConnectivityMode()
            }
            is Unavailable -> {
                _isOnline.value = false
                _connectivityMode.value = ConnectivityMode.OFFLINE
            }
        }
    }

    private fun determineConnectivityMode(): ConnectivityMode {
        // This is a simplified implementation
        // In production, you'd use NetworkCapabilities to determine the exact type
        return ConnectivityMode.WIFI // Default to Wi-Fi for simplicity
    }

    fun stopObserving() {
        networkObserver.stopObserving()
    }

    fun release() {
        stopObserving()
    }
}

enum class ConnectivityMode {
    BLUETOOTH_RELAYED,  // Phone nearby, using Bluetooth for internet
    WIFI,               // Standalone Wi-Fi connection
    OFFLINE,            // No connectivity
    RECOVERING,         // Network is recovering
    UNKNOWN             // Initial state
}

/**
 * Determine the appropriate streaming quality based on connectivity mode.
 */
fun ConnectivityMode.getStreamingQuality(): StreamingQuality {
    return when (this) {
        ConnectivityMode.BLUETOOTH_RELAYED -> StreamingQuality.LOW
        ConnectivityMode.WIFI -> StreamingQuality.HIGH
        ConnectivityMode.OFFLINE -> StreamingQuality.OFFLINE
        ConnectivityMode.RECOVERING -> StreamingQuality.LOW
        ConnectivityMode.UNKNOWN -> StreamingQuality.MEDIUM
    }
}

enum class StreamingQuality {
    HIGH,    // High bitrate for Wi-Fi
    MEDIUM,  // Medium bitrate
    LOW,     // Low bitrate for Bluetooth-relayed
    OFFLINE  // No streaming (offline only)
}