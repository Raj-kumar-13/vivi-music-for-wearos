package com.music.vivi.wear.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    tableName = "wear_playback_history",
    foreignKeys = [
        ForeignKey(
            entity = WearSongEntity::class,
            parentColumns = ["id"],
            childColumns = ["songId"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class WearPlaybackHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val songId: String,
    val playedAt: Long = System.currentTimeMillis(),
    val completionPercent: Int? = null
)