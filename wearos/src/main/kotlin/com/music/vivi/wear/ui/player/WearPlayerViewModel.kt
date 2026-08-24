package com.music.vivi.wear.ui.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.MoreExecutors
import com.music.vivi.wear.data.WearYouTubeRepository
import com.music.vivi.wear.data.models.WearSong
import com.music.vivi.wear.data.models.toEntity
import com.music.vivi.wear.db.dao.WearPlaybackHistoryDao
import com.music.vivi.wear.db.dao.WearSongDao
import com.music.vivi.wear.db.entities.WearPlaybackHistoryEntity
import com.music.vivi.wear.playback.WearPlaybackService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Wear Player ViewModel.
 *
 * Connects to [WearPlaybackService] via [MediaController] to drive real
 * ExoPlayer-backed playback. All transport controls (play/pause, skip,
 * seek) are forwarded to the controller; playback state is synced back
 * through a [Player.Listener].
 */
@UnstableApi
@HiltViewModel
class WearPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val youTubeRepository: WearYouTubeRepository,
    private val songDao: WearSongDao,
    private val playbackHistoryDao: WearPlaybackHistoryDao
) : ViewModel() {

    // ── Media controller ────────────────────────────────────────────────
    private var controller: MediaController? = null

    // ── UI state ────────────────────────────────────────────────────────
    private val _currentSong = MutableStateFlow<WearSong?>(null)
    val currentSong: StateFlow<WearSong?> = _currentSong.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _position = MutableStateFlow(0L)
    val position: StateFlow<Long> = _position.asStateFlow()

    private val _duration = MutableStateFlow(0L)
    val duration: StateFlow<Long> = _duration.asStateFlow()

    /** The last song passed to [playSong] – kept for [retry]. */
    private var lastRequestedSong: WearSong? = null

    /** Job that periodically polls position from the controller. */
    private var positionJob: Job? = null

    // ── Player.Listener ─────────────────────────────────────────────────
    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _isPlaying.value = isPlaying
            if (isPlaying) startPositionPolling() else stopPositionPolling()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            when (playbackState) {
                Player.STATE_BUFFERING -> _isLoading.value = true
                Player.STATE_READY -> {
                    _isLoading.value = false
                    _duration.value = controller?.duration ?: 0L
                }
                Player.STATE_ENDED -> {
                    _isPlaying.value = false
                    stopPositionPolling()
                }
                Player.STATE_IDLE -> {
                    _isLoading.value = false
                }
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            syncCurrentSongFromMediaItem(mediaItem)
            _duration.value = controller?.duration?.coerceAtLeast(0L) ?: 0L
            _position.value = controller?.currentPosition?.coerceAtLeast(0L) ?: 0L
        }
    }

    // ── Init ────────────────────────────────────────────────────────────
    init {
        connectController()
    }

    private fun connectController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, WearPlaybackService::class.java)
        )
        val future = MediaController.Builder(context, sessionToken).buildAsync()

        Futures.addCallback(
            future,
            object : com.google.common.util.concurrent.FutureCallback<MediaController> {
                override fun onSuccess(result: MediaController) {
                    Timber.d("MediaController connected")
                    controller = result
                    result.addListener(playerListener)

                    // Sync initial state from an already-playing session
                    _isPlaying.value = result.isPlaying
                    _duration.value = result.duration.coerceAtLeast(0L)
                    _position.value = result.currentPosition.coerceAtLeast(0L)
                    syncCurrentSongFromMediaItem(result.currentMediaItem)

                    if (result.isPlaying) startPositionPolling()
                }

                override fun onFailure(t: Throwable) {
                    Timber.e(t, "Failed to connect MediaController")
                    _error.value = "Could not connect to playback service"
                }
            },
            MoreExecutors.directExecutor()
        )
    }

    // ── Public transport controls ───────────────────────────────────────

    /**
     * Resolve a stream URL for [song], build a [MediaItem] with metadata,
     * and send it to the controller for playback.
     */
    fun playSong(song: WearSong) {
        lastRequestedSong = song
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null

            try {
                Timber.d("Playing song: ${song.title}")

                val streamUrl = youTubeRepository.getStreamUrl(song.id).getOrThrow()

                val metadata = MediaMetadata.Builder()
                    .setTitle(song.title)
                    .setArtist(song.artist)
                    .setAlbumTitle(song.album)
                    .apply {
                        song.thumbnailUrl?.let { setArtworkUri(Uri.parse(it)) }
                    }
                    .build()

                val mediaItem = MediaItem.Builder()
                    .setMediaId(song.id)
                    .setUri(streamUrl)
                    .setMediaMetadata(metadata)
                    .build()

                val ctrl = controller
                if (ctrl != null) {
                    ctrl.setMediaItem(mediaItem)
                    ctrl.prepare()
                    ctrl.play()
                    _currentSong.value = song
                } else {
                    _error.value = "Player not connected"
                    Timber.w("MediaController is null, cannot play")
                }

                // Persist song & history in the background
                launch {
                    try {
                        songDao.insertSong(song.toEntity())
                        playbackHistoryDao.insertHistory(
                            WearPlaybackHistoryEntity(songId = song.id)
                        )
                    } catch (e: Exception) {
                        Timber.w(e, "Failed to persist song/history")
                    }
                }
            } catch (e: Exception) {
                Timber.e(e, "Failed to play song")
                _error.value = e.message ?: "Unknown error"
            } finally {
                _isLoading.value = false
            }
        }
    }

    /**
     * Play a list of songs starting at [startIndex].
     *
     * For now this resolves and plays only the song at [startIndex].
     * Full queue support (resolving all URIs or delegating resolution to
     * WearSessionCallback) can be added later.
     */
    fun playQueue(songs: List<WearSong>, startIndex: Int = 0) {
        val song = songs.getOrNull(startIndex) ?: return
        playSong(song)
    }

    fun togglePlayPause() {
        val ctrl = controller ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun skipNext() {
        controller?.seekToNextMediaItem()
    }

    fun skipPrevious() {
        controller?.seekToPreviousMediaItem()
    }

    fun seekTo(positionMs: Long) {
        controller?.seekTo(positionMs)
        _position.value = positionMs
    }

    /** Re-attempt playback of the last requested song. */
    fun retry() {
        lastRequestedSong?.let { playSong(it) }
    }

    fun clearError() {
        _error.value = null
    }

    // ── Position polling ────────────────────────────────────────────────

    private fun startPositionPolling() {
        if (positionJob?.isActive == true) return
        positionJob = viewModelScope.launch {
            while (isActive) {
                controller?.let { ctrl ->
                    _position.value = ctrl.currentPosition.coerceAtLeast(0L)
                    // Duration can change (e.g. after a seek into an unbounded stream)
                    val dur = ctrl.duration
                    if (dur > 0) _duration.value = dur
                }
                delay(1_000L)
            }
        }
    }

    private fun stopPositionPolling() {
        positionJob?.cancel()
        positionJob = null
    }

    // ── Helpers ─────────────────────────────────────────────────────────

    /**
     * Attempt to reconstruct a [WearSong] from the current [MediaItem]'s
     * metadata so the UI stays in sync even when the transition originated
     * outside this ViewModel (e.g. notification controls).
     */
    private fun syncCurrentSongFromMediaItem(mediaItem: MediaItem?) {
        if (mediaItem == null) {
            _currentSong.value = null
            return
        }
        val meta = mediaItem.mediaMetadata
        _currentSong.value = WearSong(
            id = mediaItem.mediaId,
            title = meta.title?.toString() ?: "Unknown",
            artist = meta.artist?.toString(),
            album = meta.albumTitle?.toString(),
            thumbnailUrl = meta.artworkUri?.toString()
        )
    }

    // ── Lifecycle ───────────────────────────────────────────────────────

    override fun onCleared() {
        stopPositionPolling()
        controller?.run {
            removeListener(playerListener)
            release()
        }
        controller = null
        super.onCleared()
    }
}
