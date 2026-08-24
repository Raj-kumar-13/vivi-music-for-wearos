package com.music.vivi.wear.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
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
    suspend fun deleteHistoryById(id: Long)

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

    @Query("DELETE FROM wear_playback_history WHERE id NOT IN (SELECT id FROM wear_playback_history ORDER BY playedAt DESC LIMIT :keepCount)")
    suspend fun deleteOldHistory(keepCount: Int)

    @Query("SELECT COUNT(*) FROM wear_playback_history WHERE songId = :songId")
    suspend fun getHistoryCountForSong(songId: String): Int
}

data class HistoryWithSong(
    @Embedded val history: WearPlaybackHistoryEntity,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: WearSongEntity
)