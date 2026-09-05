package com.music.vivi.wear.ui.library

import android.app.Activity
import android.app.RemoteInput
import android.content.Intent
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.input.RemoteInputIntentHelper
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScreenScaffold
import com.google.android.horologist.compose.layout.rememberResponsiveColumnState
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.ui.components.SongCard
import com.music.vivi.wear.ui.components.WearTimeText
import timber.log.Timber

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (WearSong) -> Unit = {},
    onNavigateBack: () -> Unit = {},
) {
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK && result.data != null) {
            // Check RemoteInput result first (Wear OS Voice/Keyboard/Smart Reply)
            val remoteInputResults = RemoteInput.getResultsFromIntent(result.data)
            val remoteInputQuery = remoteInputResults?.getCharSequence("search_query")?.toString()

            // Check standard RecognizerIntent speech result as fallback
            val speechMatches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            val speechQuery = speechMatches?.firstOrNull()

            val query = remoteInputQuery ?: speechQuery
            if (!query.isNullOrBlank()) {
                viewModel.search(query)
            }
        }
    }

    fun launchInput() {
        try {
            val remoteInput = RemoteInput.Builder("search_query")
                .setLabel("Search music")
                .build()

            val intent = RemoteInputIntentHelper.createActionRemoteInputIntent()
            RemoteInputIntentHelper.putRemoteInputsExtra(intent, listOf(remoteInput))
            speechLauncher.launch(intent)
        } catch (e: Exception) {
            Timber.e(e, "RemoteInputIntentHelper failed, trying RecognizerIntent")
            try {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Search for music")
                }
                speechLauncher.launch(intent)
            } catch (ex: Exception) {
                Timber.e(ex, "Failed to launch voice input")
            }
        }
    }

    // Auto-launch voice/text input prompt when opening search if no results yet
    LaunchedEffect(Unit) {
        if (searchResults.isEmpty() && !isLoading) {
            launchInput()
        }
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
                    text = "Search",
                    style = MaterialTheme.typography.title3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            // Voice or Keyboard Search Trigger
            item {
                Button(
                    onClick = { launchInput() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text("Tap to speak or type")
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

            // Results
            if (searchResults.isNotEmpty()) {
                items(searchResults.size, key = { searchResults[it].id }) { index ->
                    val song = searchResults[index]
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
