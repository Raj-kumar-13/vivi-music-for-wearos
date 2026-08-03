package com.music.vivi.wear.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.music.vivi.wear.db.entities.WearPlaybackHistoryEntity
import com.music.vivi.wear.db.entities.WearSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WearPlaybackHistoryDao {
    @Query("SELECT * FROM wear_playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistory(limit: Int = 100): Flow<List<WearPlaybackHistoryEntity>>

    @Transaction
    @Query("SELECT * FROM wear_playback_history ORDER BY playedAt DESC LIMIT :limit")
    fun getRecentHistoryWithSongs(limit: Int = 100): Flow<List<HistoryWithSong>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WearPlaybackHistoryEntity)

    @Delete
    suspend fun deleteHistory(history: WearPlaybackHistoryEntity)

    @Query("DELETE FROM wear_playback_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("SELECT * FROM wear_playback_history ORDER BY playedAt DESC")
    suspend fun getAllHistory(): List<WearPlaybackHistoryEntity>

    @Query("DELETE FROM wear_playback_history WHERE songId = :songId")
    suspend fun deleteHistoryBySongId(songId: String)

    @Query("DELETE FROM wear_playback_history WHERE playedAt < :timestamp")
    suspend fun deleteHistoryBefore(timestamp: Long)

    @Query("DELETE FROM wear_playback_history")
    suspend fun deleteAllHistory()

    @Query("SELECT COUNT(*) FROM wear_playback_history")
    suspend fun getHistoryCount(): Int
}

data class HistoryWithSong(
    val history: WearPlaybackHistoryEntity,
    val song: WearSongEntity
)