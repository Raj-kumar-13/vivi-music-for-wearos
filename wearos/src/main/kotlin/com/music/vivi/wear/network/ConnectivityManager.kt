package com.music.vivi.wear.network

import android.content.Context
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.networks.data.Networks
import com.google.android.horologist.networks.status.NetworkRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalHorologistApi::class)
@Singleton
class ConnectivityManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val networkRepository: NetworkRepository
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var collectionJob: Job? = null
    
    private val _networks = MutableStateFlow(Networks(null, emptyList()))
    val networks: StateFlow<Networks> = _networks.asStateFlow()

    private val _connectivityMode = MutableStateFlow(ConnectivityMode.UNKNOWN)
    val connectivityMode: StateFlow<ConnectivityMode> = _connectivityMode.asStateFlow()

    // Backward compatibility flow
    val networkStatus: StateFlow<Networks> = networks

    fun initialize() {
        Timber.d("Initializing ConnectivityManager")
        collectionJob = scope.launch {
            try {
                networkRepository.networkStatus.collect { status ->
                    _networks.value = status
                    updateConnectivityMode(status)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error collecting network status")
            }
        }
    }

    private fun updateConnectivityMode(status: Networks) {
        _connectivityMode.value = if (status.networks.isNotEmpty()) {
            ConnectivityMode.ONLINE
        } else {
            ConnectivityMode.OFFLINE
        }
    }

    fun stopObserving() {
        Timber.d("Stopping ConnectivityManager observations")
        collectionJob?.cancel()
        collectionJob = null
    }

    fun release() {
        scope.cancel()
    }
}
