package com.music.vivi.wear.ui.library

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.data.models.WearSong

@Composable
fun PlaylistScreen(
    playlistId: String,
    viewModel: PlaylistViewModel = hiltViewModel(),
    onSongClick: (WearSong) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val playlistInfo by viewModel.playlistInfo.collectAsState()
    val playlistSongs by viewModel.playlistSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    ScreenScaffold(
        scrollState = ScalingLazyColumnDefaults.scrollState(),
        timeText = { TimeText() }
    ) {
        item {
            Text(
                text = playlistInfo?.name ?: "Playlist",
                style = MaterialTheme.typography.title3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Loading State
        if (isLoading) {
            item {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Error State
        if (error != null) {
            item {
                Text(
                    text = error ?: "Unknown error",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Playlist Songs
        if (playlistSongs.isNotEmpty()) {
            item {
                Text(
                    text = "${playlistSongs.size} songs",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            playlistSongs.forEach { song ->
                item {
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        } else if (!isLoading && error == null) {
            // Empty state
            item {
                Text(
                    text = "This playlist is empty",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    textAlign = TextAlign.Center
                )
            }
        }

        // Back Button
        item {
            Button(
                onClick = onNavigateBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Back")
            }
        }
    }
}

@Composable
fun SongCard(
    song: WearSong,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = song.title,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (song.artist != null) {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.caption1,
                modifier = Modifier.padding(horizontal = 8.dp, bottom = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun TimeText() {
    androidx.wear.compose.material.TimeText()
}