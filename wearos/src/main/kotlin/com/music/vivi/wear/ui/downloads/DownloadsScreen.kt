package com.music.vivi.wear.ui.downloads

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.ui.components.WearTimeText
import java.text.DecimalFormat

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val downloads by viewModel.downloads.collectAsStateWithLifecycle()
    val storageUsage by viewModel.storageUsage.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()

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
                    text = "Downloads",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Storage Usage Indicator
            item {
                StorageUsageCard(
                    usedBytes = storageUsage.usedBytes,
                    totalBytes = storageUsage.totalBytes,
                    downloadCount = downloads.size
                )
            }

            // Manual Cleanup Button
            item {
                Button(
                    onClick = { viewModel.runManualCleanup() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Clean up now")
                }
            }

            // Loading State
            if (isLoading) {
                item {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }

            // Downloads List
            if (downloads.isNotEmpty()) {
                items(downloads.size, key = { downloads[it].song.id }) { index ->
                    val download = downloads[index]
                    DownloadCard(
                        song = download.song,
                        progress = download.progress,
                        state = download.state,
                        onClick = { viewModel.removeDownload(download.song.id) }
                    )
                }
            } else if (!isLoading) {
                // Empty State
                item {
                    Text(
                        text = "No downloads yet",
                        style = MaterialTheme.typography.body2,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
fun StorageUsageCard(
    usedBytes: Long,
    totalBytes: Long,
    downloadCount: Int
) {

    Card(
        onClick = { },
        enabled = false,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
    ) {
        Text(
            text = "Storage: $downloadCount songs",
            style = MaterialTheme.typography.caption2,
            modifier = Modifier.padding(8.dp)
        )
        Text(
            text = "${formatBytes(usedBytes)} / ${formatBytes(totalBytes)}",
            style = MaterialTheme.typography.body2,
            modifier = Modifier.padding(horizontal = 8.dp)
        )
    }
}

@Composable
fun DownloadCard(
    song: WearSong,
    progress: Float,
    state: DownloadState,
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
        song.artist?.let { artist ->
            Text(
                text = artist,
                style = MaterialTheme.typography.caption1,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (state) {
            DownloadState.DOWNLOADING -> {
                Text(
                    text = "Downloading: ${(progress * 100).toInt()}%",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(8.dp)
                )
            }
            DownloadState.COMPLETED -> {
                Text(
                    text = "Completed",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(8.dp)
                )
            }
            DownloadState.FAILED -> {
                Text(
                    text = "Failed",
                    style = MaterialTheme.typography.caption2,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(8.dp)
                )
            }
            DownloadState.PENDING -> {
                Text(
                    text = "Pending...",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(8.dp)
                )
            }
        }
    }
}

private val bytesFormat = DecimalFormat("#.#")

fun formatBytes(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytesFormat.format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytesFormat.format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${bytesFormat.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}
