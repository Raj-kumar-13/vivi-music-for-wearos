package com.music.vivi.wear.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.CircularProgressIndicator
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.music.vivi.wear.network.NetworkStateObserver
import com.music.vivi.wear.ui.components.WearTimeText

/**
 * Now Playing Screen for Wear OS.
 */
@Composable
fun NowPlayingScreen(
    viewModel: WearPlayerViewModel = hiltViewModel(),
    networkObserver: NetworkStateObserver = hiltViewModel(),
    onNavigateToLibrary: () -> Unit = {}
) {
    val currentSong by viewModel.currentSong.collectAsState()
    val isPlaying by viewModel.isPlaying.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isOfflineMode by networkObserver.isOfflineMode.collectAsState()

    ScreenScaffold(
        timeText = { WearTimeText() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (isLoading) {
                CircularProgressIndicator()
            } else if (currentSong != null) {
                Text(
                    text = currentSong!!.title,
                    style = MaterialTheme.typography.body1,
                    maxLines = 2
                )
                currentSong!!.artist?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.caption1
                    )
                }
                
                Button(
                    onClick = { viewModel.togglePlayPause() },
                    modifier = Modifier.padding(top = 8.dp)
                ) {
                    Text(if (isPlaying) "Pause" else "Play")
                }
            } else {
                Text(
                    text = if (isOfflineMode) {
                        "Offline mode - only downloads available"
                    } else {
                        "No media playing"
                    },
                    style = MaterialTheme.typography.body2
                )
            }
            
            Button(
                onClick = onNavigateToLibrary,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text("Library")
            }
        }
    }
}
