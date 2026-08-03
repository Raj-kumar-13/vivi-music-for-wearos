package com.music.vivi.wear.ui.downloads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.LinearProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.data.models.WearSong
import java.text.DecimalFormat

@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val downloads by viewModel.downloads.collectAsState()
    val storageUsage by viewModel.storageUsage.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    ScreenScaffold(
        scrollState = ScalingLazyColumnDefaults.scrollState(),
        timeText = { TimeText() }
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
                CircularProgressIndicator()
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Loading downloads...",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        // Downloads List
        if (downloads.isNotEmpty()) {
            downloads.forEach { download ->
                item {
                    DownloadCard(
                        song = download.song,
                        progress = download.progress,
                        state = download.state,
                        onClick = { viewModel.removeDownload(download.song.id) }
                    )
                }
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

@Composable
fun StorageUsageCard(
    usedBytes: Long,
    totalBytes: Long,
    downloadCount: Int
) {
    val usedMB = usedBytes / (1024 * 1024)
    val totalMB = totalBytes / (1024 * 1024)
    val usagePercentage = if (totalBytes > 0) (usedBytes * 100) / totalBytes else 0

    Card(
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
        Spacer(modifier = Modifier.padding(4.dp))
        LinearProgressIndicator(
            progress = { usagePercentage / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
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
        if (song.artist != null) {
            Text(
                text = song.artist,
                style = MaterialTheme.typography.caption1,
                modifier = Modifier.padding(horizontal = 8.dp),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        when (state) {
            DownloadState.DOWNLOADING -> {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                )
                Text(
                    text = "${(progress * 100).toInt()}%",
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

@Composable
fun TimeText() {
    androidx.wear.compose.material.TimeText()
}

@Composable
fun Spacer(modifier: Modifier = Modifier) {
    androidx.compose.foundation.layout.Spacer(modifier = modifier)
}

fun formatBytes(bytes: Long): String {
    val decimalFormat = DecimalFormat("#.#")
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${decimalFormat.format(bytes / 1024.0)} KB"
        bytes < 1024 * 1024 * 1024 -> "${decimalFormat.format(bytes / (1024.0 * 1024.0))} MB"
        else -> "${decimalFormat.format(bytes / (1024.0 * 1024.0 * 1024.0))} GB"
    }
}

enum class DownloadState {
    DOWNLOADING,
    COMPLETED,
    FAILED,
    PENDING
}

data class DownloadItem(
    val song: WearSong,
    val progress: Float,
    val state: DownloadState
)

data class StorageUsage(
    val usedBytes: Long,
    val totalBytes: Long
)