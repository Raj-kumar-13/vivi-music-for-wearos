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
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.data.models.WearSong

@Composable
fun LibraryScreen(
    viewModel: LibraryViewModel = hiltViewModel(),
    onNavigateToSearch: () -> Unit = {},
    onNavigateToPlaylist: (String) -> Unit = {},
    onNavigateToNowPlaying: () -> Unit = {},
    onNavigateToDownloads: () -> Unit = {},
    onNavigateToSettings: () -> Unit = {},
    onSongClick: (WearSong) -> Unit = {}
) {
    val recentSongs by viewModel.recentSongs.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    val downloadedSongs by viewModel.downloadedSongs.collectAsState()

    ScreenScaffold(
        scrollState = ScalingLazyColumnDefaults.scrollState(),
        timeText = { TimeText() }
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

            recentSongs.forEach { song ->
                item {
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
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

            playlists.forEach { playlist ->
                item {
                    PlaylistCard(
                        playlistName = playlist.name,
                        songCount = playlist.songCount,
                        onClick = { onNavigateToPlaylist(playlist.id) }
                    )
                }
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

            downloadedSongs.forEach { song ->
                item {
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        }

        // Now Playing Button
        item {
            Button(
                onClick = onNavigateToNowPlaying,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Now Playing")
            }
        }

        // Downloads Button
        item {
            Button(
                onClick = onNavigateToDownloads,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Downloads")
            }
        }

        // Settings Button
        item {
            Button(
                onClick = onNavigateToSettings,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Settings")
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
fun PlaylistCard(
    playlistName: String,
    songCount: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = playlistName,
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(8.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = "$songCount songs",
            style = MaterialTheme.typography.caption1,
            modifier = Modifier.padding(horizontal = 8.dp, bottom = 8.dp)
        )
    }
}

@Composable
fun TimeText() {
    androidx.wear.compose.material.TimeText()
}