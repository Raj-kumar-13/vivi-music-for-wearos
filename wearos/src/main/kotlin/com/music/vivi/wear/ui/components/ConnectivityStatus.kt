package com.music.vivi.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.horologist.networks.ui.NetworkStatusIndicator
import com.music.vivi.wear.network.NetworkState
import com.music.vivi.wear.network.ConnectivityMode

/**
 * Connectivity status using Horologist's NetworkStatusIndicator.
 * This provides battery-optimized network status display.
 */
@Composable
fun ConnectivityStatus(
    networkState: NetworkState,
    connectivityMode: ConnectivityMode,
    modifier: Modifier = Modifier
) {
    // Use Horologist's NetworkStatusIndicator for battery-optimized network status
    val isAvailable = networkState is NetworkState.Available
    NetworkStatusIndicator(
        status = if (isAvailable) {
            com.google.android.horologist.networks.awareness.NetworkStatus.Available
        } else {
            com.google.android.horologist.networks.awareness.NetworkStatus.Unavailable
        },
        modifier = modifier
    )
}