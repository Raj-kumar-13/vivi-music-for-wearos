package com.music.vivi.wear

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.ui.downloads.DownloadsScreen
import com.music.vivi.wear.ui.library.LibraryScreen
import com.music.vivi.wear.ui.settings.SettingsScreen
import com.music.vivi.wear.ui.library.PlaylistScreen
import com.music.vivi.wear.ui.library.SearchScreen
import com.music.vivi.wear.ui.player.NowPlayingScreen
import com.music.vivi.wear.ui.player.WearPlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import androidx.hilt.navigation.compose.hiltViewModel

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            WearTheme {
                ViviWearApp()
            }
        }
    }
}

@Composable
fun WearTheme(content: @Composable () -> Unit) {
    MaterialTheme {
        content()
    }
}

@Composable
fun ViviWearApp() {
    val navController = rememberSwipeDismissableNavController()
    val playerViewModel: WearPlayerViewModel = hiltViewModel()

    SwipeDismissableNavHost(
        navController = navController,
        startDestination = "now_playing",
    ) {
        composable("now_playing") {
            NowPlayingScreen(
                viewModel = playerViewModel,
                onNavigateToLibrary = {
                    navController.navigate("library") {
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("library") {
            LibraryScreen(
                onNavigateToSearch = {
                    navController.navigate("search") {
                        launchSingleTop = true
                    }
                },
                onNavigateToPlaylist = { playlistId ->
                    navController.navigate("playlist/$playlistId") {
                        launchSingleTop = true
                    }
                },
                onNavigateToNowPlaying = {
                    navController.navigate("now_playing") {
                        popUpTo("now_playing") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateToDownloads = {
                    navController.navigate("downloads") {
                        launchSingleTop = true
                    }
                },
                onNavigateToSettings = {
                    navController.navigate("settings") {
                        launchSingleTop = true
                    }
                },
                onSongClick = { song ->
                    playerViewModel.playSong(song)
                    navController.navigate("now_playing") {
                        popUpTo("now_playing") { inclusive = true }
                        launchSingleTop = true
                    }
                }
            )
        }
        composable("downloads") {
            DownloadsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("search") {
            SearchScreen(
                onSongClick = { song ->
                    playerViewModel.playSong(song)
                    navController.navigate("now_playing") {
                        popUpTo("now_playing") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
        composable("playlist/{playlistId}") { backStackEntry ->
            val playlistId = backStackEntry.arguments?.getString("playlistId") ?: ""
            PlaylistScreen(
                playlistId = playlistId,
                onSongClick = { song ->
                    playerViewModel.playSong(song)
                    navController.navigate("now_playing") {
                        popUpTo("now_playing") { inclusive = true }
                        launchSingleTop = true
                    }
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}