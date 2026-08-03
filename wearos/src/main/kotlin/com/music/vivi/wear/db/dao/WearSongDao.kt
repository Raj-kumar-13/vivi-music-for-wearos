package com.music.vivi.wear.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.music.vivi.wear.db.entities.WearSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WearSongDao {
    @Query("SELECT * FROM wear_song WHERE id = :songId")
    suspend fun getSongById(songId: String): WearSongEntity?

    @Query("SELECT * FROM wear_song")
    fun getAllSongs(): Flow<List<WearSongEntity>>

    @Query("SELECT * FROM wear_song ORDER BY cachedAt DESC LIMIT :limit")
    fun getRecentSongs(limit: Int = 50): Flow<List<WearSongEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: WearSongEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<WearSongEntity>)

    @Update
    suspend fun updateSong(song: WearSongEntity)

    @Delete
    suspend fun deleteSong(song: WearSongEntity)

    @Query("DELETE FROM wear_song WHERE id = :songId")
    suspend fun deleteSongById(songId: String)

    @Query("DELETE FROM wear_song")
    suspend fun deleteAllSongs()

    @Query("SELECT COUNT(*) FROM wear_song")
    suspend fun getSongCount(): Int
}