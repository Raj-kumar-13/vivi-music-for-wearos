package com.music.vivi.wear.ui.library

import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
import java.util.Locale

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (WearSong) -> Unit = {},
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val searchResults by viewModel.searchResults.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    var searchQuery by remember { mutableStateOf("") }

    // Voice search launcher
    val voiceSearchLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                searchQuery = matches[0]
                viewModel.search(searchQuery)
            }
        }
    }

    fun launchVoiceSearch() {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for music")
        }
        voiceSearchLauncher.launch(intent)
    }

    ScreenScaffold(
        scrollState = ScalingLazyColumnDefaults.scrollState(),
        timeText = { TimeText() }
    ) {
        item {
            Text(
                text = "Search",
                style = MaterialTheme.typography.title3,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Voice Search Button
        item {
            Button(
                onClick = { launchVoiceSearch() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
            ) {
                Text("🎤 Voice Search")
            }
        }

        // Current Query Display
        if (searchQuery.isNotEmpty()) {
            item {
                Text(
                    text = "Searching: $searchQuery",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }

        // Loading State
        if (isLoading) {
            item {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.padding(8.dp))
                Text(
                    text = "Searching...",
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

        // Search Results
        if (searchResults.isNotEmpty()) {
            item {
                Text(
                    text = "Results",
                    style = MaterialTheme.typography.caption2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            searchResults.forEach { song ->
                item {
                    SongCard(
                        song = song,
                        onClick = { onSongClick(song) }
                    )
                }
            }
        } else if (!isLoading && error == null && searchQuery.isEmpty()) {
            // Empty state
            item {
                Text(
                    text = "Tap the microphone to search",
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