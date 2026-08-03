package com.music.vivi.wear.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.ToggleChip
import androidx.wear.compose.material.ToggleChipDefaults
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.network.StreamingQuality

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val audioQuality by viewModel.audioQuality.collectAsState()
    val downloadSizeCap by viewModel.downloadSizeCap.collectAsState()
    val authStatus by viewModel.authStatus.collectAsState()
    val versionInfo by viewModel.versionInfo.collectAsState()

    ScreenScaffold(
        scrollState = ScalingLazyColumnDefaults.scrollState(),
        timeText = { TimeText() }
    ) {
        item {
            Text(
                text = "Settings",
                style = MaterialTheme.typography.title3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Audio Quality Setting
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Audio Quality",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = when (audioQuality) {
                        StreamingQuality.HIGH -> "High"
                        StreamingQuality.MEDIUM -> "Medium"
                        StreamingQuality.LOW -> "Low"
                        StreamingQuality.OFFLINE -> "Offline"
                    },
                    style = MaterialTheme.typography.caption1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Button(
                    onClick = { viewModel.cycleAudioQuality() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("Change")
                }
            }
        }

        // Download Size Cap Setting
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Download Size Cap",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = "${downloadSizeCap}MB",
                    style = MaterialTheme.typography.caption1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Button(
                    onClick = { viewModel.cycleDownloadSizeCap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp)
                ) {
                    Text("Change")
                }
            }
        }

        // Auth Status & Reconnect
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "Authentication",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = if (authStatus) "Connected" else "Not connected",
                    style = MaterialTheme.typography.caption1,
                    color = if (authStatus) MaterialTheme.colors.primary else MaterialTheme.colors.error,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                if (!authStatus) {
                    Button(
                        onClick = { viewModel.requestAuthReconnect() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp)
                    ) {
                        Text("Reconnect")
                    }
                }
            }
        }

        // About/Version Info
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text(
                    text = "About",
                    style = MaterialTheme.typography.body2,
                    modifier = Modifier.padding(8.dp)
                )
                Text(
                    text = "Vivi Wear ${versionInfo}",
                    style = MaterialTheme.typography.caption1,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text(
                    text = "Music for Wear OS",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier.padding(horizontal = 8.dp, bottom = 8.dp)
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
fun TimeText() {
    androidx.wear.compose.material.TimeText()
}