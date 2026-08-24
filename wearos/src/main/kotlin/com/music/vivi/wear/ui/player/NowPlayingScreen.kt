package com.music.vivi.wear.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.CompactButton
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.music.vivi.wear.network.NetworkStateObserver
import com.music.vivi.wear.ui.components.WearTimeText

/**
 * Now Playing Screen for Wear OS with full playback controls.
 */
@Composable
fun NowPlayingScreen(
    viewModel: WearPlayerViewModel = hiltViewModel(),
    networkObserver: NetworkStateObserver = hiltViewModel(),
    onNavigateToLibrary: () -> Unit = {}
) {
    val currentSong by viewModel.currentSong.collectAsStateWithLifecycle()
    val isPlaying by viewModel.isPlaying.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val isOfflineMode by networkObserver.isOfflineMode.collectAsStateWithLifecycle()
    val position by viewModel.position.collectAsStateWithLifecycle()
    val duration by viewModel.duration.collectAsStateWithLifecycle()

    ScreenScaffold(
        timeText = { WearTimeText() }
    ) {
        val song = currentSong
        when {
            isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            song != null -> {
                // Main player UI with progress ring
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    // Background progress ring
                    if (duration > 0) {
                        CircularProgressIndicator(
                            progress = (position.toFloat() / duration.toFloat()).coerceIn(0f, 1f),
                            modifier = Modifier.fillMaxSize(),
                            strokeWidth = 4.dp,
                            trackColor = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                            indicatorColor = MaterialTheme.colors.primary
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp, vertical = 32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        // Song title
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.body1,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )

                        // Artist
                        song.artist?.let {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.caption2,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.7f),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Time display
                        if (duration > 0) {
                            Text(
                                text = "${formatTime(position)} / ${formatTime(duration)}",
                                style = MaterialTheme.typography.caption2,
                                color = MaterialTheme.colors.onSurface.copy(alpha = 0.5f)
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Playback controls: Previous | Play/Pause | Next
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Previous
                            CompactButton(
                                onClick = { viewModel.skipPrevious() },
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Text(
                                    text = "\u23EE",
                                    style = MaterialTheme.typography.title3
                                )
                            }

                            // Play/Pause
                            Button(
                                onClick = { viewModel.togglePlayPause() },
                                modifier = Modifier.size(ButtonDefaults.DefaultButtonSize)
                            ) {
                                Text(
                                    text = if (isPlaying) "\u23F8" else "\u25B6",
                                    style = MaterialTheme.typography.title2
                                )
                            }

                            // Next
                            CompactButton(
                                onClick = { viewModel.skipNext() },
                                colors = ButtonDefaults.secondaryButtonColors()
                            ) {
                                Text(
                                    text = "\u23ED",
                                    style = MaterialTheme.typography.title3
                                )
                            }
                        }
                    }
                }
            }
            error != null -> {
                // Error state with retry
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = error ?: "Unknown error",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.error,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { viewModel.retry() }) {
                        Text("Retry")
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    CompactButton(
                        onClick = onNavigateToLibrary,
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text("Library")
                    }
                }
            }
            else -> {
                // No song playing - idle state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (isOfflineMode) {
                            "Offline mode\nOnly downloads available"
                        } else {
                            "No media playing"
                        },
                        style = MaterialTheme.typography.body2,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(onClick = onNavigateToLibrary) {
                        Text("Library")
                    }
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = ms / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
