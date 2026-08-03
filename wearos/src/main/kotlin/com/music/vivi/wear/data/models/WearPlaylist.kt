package com.music.vivi.wear.data.models

data class WearPlaylist(
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val songs: List<WearSong> = emptyList()
)