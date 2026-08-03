package com.music.vivi.wear.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "wear_download",
    foreignKeys = [
        ForeignKey(
            entity = WearSongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WearDownloadEntity(
    @PrimaryKey
    val songId: String,
    val filePath: String,
    val downloadedAt: Long = System.currentTimeMillis(),
    val sizeBytes: Long
)