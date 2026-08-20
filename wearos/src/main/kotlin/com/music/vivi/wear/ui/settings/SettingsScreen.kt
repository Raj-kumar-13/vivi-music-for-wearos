package com.music.vivi.wear.ui.settings

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.music.vivi.wear.ui.components.WearTimeText

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit = {}
) {
    val audioQuality by viewModel.audioQuality.collectAsStateWithLifecycle()
    val downloadSizeCap by viewModel.downloadSizeCap.collectAsStateWithLifecycle()
    val authStatus by viewModel.authStatus.collectAsStateWithLifecycle()
    val versionInfo by viewModel.versionInfo.collectAsStateWithLifecycle()

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
                    text = "Settings",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Audio Quality
            item {
                Card(
                    onClick = { viewModel.cycleAudioQuality() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Streaming Quality",
                        style = MaterialTheme.typography.caption2
                    )
                    Text(
                        text = audioQuality.name,
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            // Download Size Cap
            item {
                Card(
                    onClick = { viewModel.cycleDownloadSizeCap() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Download Cap",
                        style = MaterialTheme.typography.caption2
                    )
                    Text(
                        text = "$downloadSizeCap MB",
                        style = MaterialTheme.typography.body2
                    )
                }
            }

            // Auth Status
            item {
                Card(
                    onClick = { viewModel.requestAuthReconnect() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Account Status",
                        style = MaterialTheme.typography.caption2
                    )
                    Text(
                        text = if (authStatus) "Logged In" else "Disconnected",
                        style = MaterialTheme.typography.body2,
                        color = if (authStatus) MaterialTheme.colors.primary else MaterialTheme.colors.error
                    )
                }
            }

            // Version Info
            item {
                Text(
                    text = "Version: $versionInfo",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                )
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
