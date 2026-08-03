package com.music.vivi.wear.data

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.response.SearchResponse
import com.music.innertube.models.response.PlayerResponse
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.data.models.toEntity
import com.vivi.vivimusic.R
import io.ktor.client.statement.bodyAsText
import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

/**
 * Repository wrapping the :innertube YouTube client for Wear OS.
 * Handles auth injection and error handling.
 */
class WearYouTubeRepository(
    private val authManager: WearAuthManager
) {

    suspend fun search(query: String): Result<List<WearSong>> = withContext(Dispatchers.IO) {
        try {
            // Inject auth cookie
            authManager.injectAuth()

            Timber.d("Searching for: $query")
            val response = YouTube.search(query)

            val songs = response.items
                .filterIsInstance<SongItem>()
                .map { it.toWearSong() }

            Result.success(songs)
        } catch (e: Exception) {
            Timber.e(e, "Search failed")
            handleAuthError(e)
            Result.failure(e)
        }
    }

    suspend fun getStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Inject auth cookie
            authManager.injectAuth()

            Timber.d("Getting stream URL for: $videoId")
            val response = YouTube.player(videoId)

            val streamUrl = response.streamingData?.adaptiveFormats?.firstOrNull()?.url
                ?: response.streamingData?.formats?.firstOrNull()?.url

            if (streamUrl != null) {
                Result.success(streamUrl)
            } else {
                Result.failure(Exception("No stream URL found"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stream URL")
            handleAuthError(e)
            Result.failure(e)
        }
    }

    suspend fun getSongDetails(videoId: String): Result<WearSong> = withContext(Dispatchers.IO) {
        try {
            // Inject auth cookie
            authManager.injectAuth()

            Timber.d("Getting song details for: $videoId")
            val response = YouTube.player(videoId)

            val song = response.toWearSong(videoId)
            Result.success(song)
        } catch (e: Exception) {
            Timber.e(e, "Failed to get song details")
            handleAuthError(e)
            Result.failure(e)
        }
    }

    private fun handleAuthError(error: Exception) {
        // Check if this is an auth-related error (401/403)
        if (error.message?.contains("401") == true || 
            error.message?.contains("403") == true ||
            error.message?.contains("Unauthorized") == true ||
            error.message?.contains("Forbidden") == true) {
            Timber.w("Auth error detected, clearing auth")
            authManager.clearAuth()
        }
    }
}

// Extension functions to convert innertube models to Wear models

private fun SongItem.toWearSong(): WearSong {
    return WearSong(
        id = videoId,
        title = info.title,
        artist = artists?.joinToString { it.name },
        album = album?.name,
        thumbnailUrl = thumbnails?.maxByOrNull { it.height }?.url,
        duration = info.durationSeconds?.toLong()
    )
}

private fun PlayerResponse.toWearSong(videoId: String): WearSong {
    return WearSong(
        id = videoId,
        title = videoDetails?.title ?: "Unknown",
        artist = videoDetails?.author,
        album = null,
        thumbnailUrl = videoDetails?.thumbnail?.thumbnails?.maxByOrNull { it.height }?.url,
        duration = videoDetails?.lengthSeconds
    )
}