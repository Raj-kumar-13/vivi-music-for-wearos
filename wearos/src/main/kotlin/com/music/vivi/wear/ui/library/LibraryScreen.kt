package com.music.vivi.wear.ui.library

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.ui.components.SongCard
import com.music.vivi.wear.ui.components.PlaylistCard
import com.music.vivi.wear.ui.components.WearTimeText

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSongClick: (WearSong) -> Unit = {},
) {
    val recentSongs by viewModel.recentSongs.collectAsStateWithLifecycle()
    val playlists by viewModel.playlists.collectAsStateWithLifecycle()
    val downloadedSongs by viewModel.downloadedSongs.collectAsStateWithLifecycle()

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
                    text = "Library",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Voice Search Button
            item {
                Button(
                    onClick = onNavigateToSearch,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Search with Voice")
                }
            }

            // Recent Plays Section
            if (recentSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent",
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(recentSongs.size, key = { recentSongs[it].id }) { index ->
                    val song = recentSongs[index]
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }

            // Playlists Section
            if (playlists.isNotEmpty()) {
                item {
                    Text(
                        text = "Playlists",
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(playlists.size, key = { playlists[it].id }) { index ->
                    val playlist = playlists[index]
                    PlaylistCard(
                        playlistName = playlist.name,
                        songCount = playlist.songCount,
                        onClick = { onNavigateToPlaylist(playlist.id) }
                    )
                }
            }

            // Downloads Section
            if (downloadedSongs.isNotEmpty()) {
                item {
                    Text(
                        text = "Downloads",
                        style = MaterialTheme.typography.caption2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                }

                items(downloadedSongs.size, key = { downloadedSongs[it].id }) { index ->
                    val song = downloadedSongs[index]
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }

            // Navigation Buttons
            item {
                Button(
                    onClick = onNavigateToNowPlaying,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Now Playing")
                }
            }

            item {
                Button(
                    onClick = onNavigateToDownloads,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Downloads")
                }
            }

            item {
                Button(
                    onClick = onNavigateToSettings,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Settings")
                }
            }
        }
    }
}
