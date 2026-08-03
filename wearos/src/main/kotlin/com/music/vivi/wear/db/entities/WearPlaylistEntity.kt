package com.music.vivi.wear.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "wear_playlist")
data class WearPlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val thumbnailUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)