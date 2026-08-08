package com.music.vivi.wear.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.ui.components.SongCard
import com.music.vivi.wear.ui.components.WearTimeText

@Composable
fun PlaylistScreen(
    playlistId: String,
    viewModel: PlaylistViewModel = hiltViewModel(),
    onSongClick: (WearSong) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val playlistInfo by viewModel.playlistInfo.collectAsState()
    val playlistSongs by viewModel.playlistSongs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    LaunchedEffect(playlistId) {
        viewModel.loadPlaylist(playlistId)
    }

    val columnState = rememberResponsiveColumnState(
        contentPadding = ScalingLazyColumnDefaults.padding(
            first = ScalingLazyColumnDefaults.ItemType.Text,
            last = ScalingLazyColumnDefaults.ItemType.SingleButton,
        )
    )

    ScreenScaffold(
        scrollState = columnState,
        timeText = { WearTimeText() }
    ) {
        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.fillMaxSize()
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
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Error State
            error?.let {
                item {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Playlist Songs
            if (playlistSongs.isNotEmpty()) {
                items(playlistSongs.size) { index ->
                    val song = playlistSongs[index]
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
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
}
