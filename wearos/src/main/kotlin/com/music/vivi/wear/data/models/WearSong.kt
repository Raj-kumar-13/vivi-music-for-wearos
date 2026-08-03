package com.music.vivi.wear.data.models

data class WearSong(
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long? = null
)

fun WearSong.toEntity() = com.music.vivi.wear.db.entities.WearSongEntity(
    id = id,
    title = title,
    artist = artist,
    album = album,
    thumbnailUrl = thumbnailUrl,
    duration = duration,
    cachedAt = System.currentTimeMillis()
)