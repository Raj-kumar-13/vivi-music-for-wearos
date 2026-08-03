package com.music.vivi.wear.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wear_song")
data class WearSongEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val artist: String? = null,
    val album: String? = null,
    val thumbnailUrl: String? = null,
    val duration: Long? = null,
    val cachedAt: Long = System.currentTimeMillis()
)