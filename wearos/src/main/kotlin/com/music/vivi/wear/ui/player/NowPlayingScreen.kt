package com.music.vivi.wear.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.media.ui.screens.PlayerScreen
import com.music.vivi.wear.network.NetworkStateObserver

/**
 * Now Playing Screen using Horologist's PlayerScreen.
 * This provides a complete, battery-optimized player UI with rotary input support.
 */
@Composable
fun NowPlayingScreen(
    viewModel: WearPlayerViewModel = hiltViewModel(),
    networkObserver: NetworkStateObserver = hiltViewModel(),
    onNavigateToLibrary: () -> Unit = {}
) {
    val playerViewModel by viewModel.playerViewModel.collectAsState()
    val isOfflineMode by networkObserver.isOfflineMode.collectAsState()

    // Use Horologist's PlayerScreen for battery-optimized, rotary-input-friendly UI
    PlayerScreen(
        playerViewModel = playerViewModel,
        marquee = true, // Enable scrolling text for long titles
    ) {
        // Empty state when no media is playing
        androidx.wear.compose.material.Text(
            text = if (isOfflineMode) {
                "Offline mode - only downloads available"
            } else {
                "No media playing"
            },
            style = androidx.wear.compose.material.MaterialTheme.typography.body2
        )
    }
}