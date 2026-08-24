package com.music.vivi.wear.data

import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YouTubeClient
import com.music.innertube.models.response.PlayerResponse
import com.music.vivi.wear.data.models.WearSong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WearYouTubeRepository @Inject constructor(
    private val authManager: WearAuthManager,
    private val authStorage: com.music.vivi.wear.auth.WearAuthStorage
) {

    /**
     * Search for songs on YouTube Music.
     */
    suspend fun search(query: String): Result<List<WearSong>> = withContext(Dispatchers.IO) {
        try {
            // Inject auth cookie before searching
            authManager.injectAuth()

            Timber.d("Searching for: $query")
            val result = YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)

            if (result.isSuccess) {
                val searchResult = result.getOrNull()
                val songs = searchResult?.items?.filterIsInstance<SongItem>()?.map { item ->
                    WearSong(
                        id = item.id,
                        title = item.title,
                        artist = item.artists.joinToString { it.name },
                        album = item.album?.name,
                        thumbnailUrl = item.thumbnail,
                        duration = item.duration?.toLong()
                    )
                } ?: emptyList()
                Result.success(songs)
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Search failed"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Search failed")
            Result.failure(e)
        }
    }

    /**
     * Get the stream URL for a given video ID.
     */
    suspend fun getStreamUrl(videoId: String): Result<String> = withContext(Dispatchers.IO) {
        try {
            // Inject auth cookie
            authManager.injectAuth()

            Timber.d("Getting stream URL for: $videoId")
            // Using WEB_REMIX client for YouTube Music
            val result = YouTube.player(videoId, client = YouTubeClient.WEB_REMIX)

            if (result.isSuccess) {
                val response = result.getOrNull()
                val audioFormats = response?.streamingData?.adaptiveFormats
                    ?.filter { it.isAudio }
                    ?.sortedBy { it.bitrate ?: Int.MAX_VALUE }
                val quality = authStorage.getAudioQuality()
                val streamUrl = when (quality) {
                    com.music.vivi.wear.network.StreamingQuality.HIGH ->
                        audioFormats?.lastOrNull()?.url // highest bitrate
                    com.music.vivi.wear.network.StreamingQuality.MEDIUM ->
                        audioFormats?.getOrNull(audioFormats.size / 2)?.url // middle bitrate
                    else ->
                        audioFormats?.firstOrNull()?.url // lowest bitrate (default for WearOS)
                } ?: response?.streamingData?.formats?.firstOrNull { it.url != null }?.url

                if (streamUrl != null) {
                    Result.success(streamUrl)
                } else {
                    Result.failure(Exception("No stream URL found"))
                }
            } else {
                val exception = result.exceptionOrNull() ?: Exception("Failed to get player response")
                handleAuthError(exception)
                Result.failure(exception)
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get stream URL")
            handleAuthError(e)
            Result.failure(e)
        }
    }

    /**
     * Get song details including title, artist, and thumbnails.
     */
    suspend fun getSongDetails(videoId: String): Result<WearSong> = withContext(Dispatchers.IO) {
        try {
            authManager.injectAuth()
            val result = YouTube.player(videoId, client = YouTubeClient.WEB_REMIX)

            if (result.isSuccess) {
                val response = result.getOrNull()
                if (response != null) {
                    Result.success(response.toWearSong(videoId))
                } else {
                    Result.failure(Exception("Player response is null"))
                }
            } else {
                Result.failure(result.exceptionOrNull() ?: Exception("Failed to get song details"))
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to get song details")
            Result.failure(e)
        }
    }

    private fun handleAuthError(error: Throwable) {
        if (error.message?.contains("401") == true || 
            error.message?.contains("403") == true ||
            error.message?.contains("Unauthorized") == true ||
            error.message?.contains("Forbidden") == true) {
            Timber.w("Auth error detected, clearing credentials")
            authManager.clearAuth()
        }
    }

    private fun PlayerResponse.toWearSong(videoId: String): WearSong {
        return WearSong(
            id = videoId,
            title = videoDetails?.title ?: "Unknown",
            artist = videoDetails?.author,
            thumbnailUrl = videoDetails?.thumbnail?.thumbnails
                ?.filter { (it.height ?: 0) <= 200 }
                ?.maxByOrNull { it.height ?: 0 }?.url
                ?: videoDetails?.thumbnail?.thumbnails?.minByOrNull { it.height ?: 0 }?.url,
            duration = videoDetails?.lengthSeconds?.toLongOrNull()
        )
    }
}
