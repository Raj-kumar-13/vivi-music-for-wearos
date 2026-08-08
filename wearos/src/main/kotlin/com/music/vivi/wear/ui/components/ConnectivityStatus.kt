package com.music.vivi.wear.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.MaterialTheme
import com.music.vivi.wear.network.NetworkState
import com.music.vivi.wear.network.ConnectivityMode

/**
 * Connectivity status display.
 */
@Composable
fun ConnectivityStatus(
    networkState: NetworkState,
    connectivityMode: ConnectivityMode,
    modifier: Modifier = Modifier
) {
    val isAvailable = networkState is NetworkState.Available
    Text(
        text = if (isAvailable) "Online" else "Offline",
        style = MaterialTheme.typography.caption2,
        color = if (isAvailable) MaterialTheme.colors.primary else MaterialTheme.colors.error,
        modifier = modifier
    )
}
