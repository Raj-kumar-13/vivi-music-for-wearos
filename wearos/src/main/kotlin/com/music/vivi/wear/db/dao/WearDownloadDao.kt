package com.music.vivi.wear.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import com.music.vivi.wear.db.entities.WearDownloadEntity
import com.music.vivi.wear.db.entities.WearSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WearDownloadDao {
    @Query("SELECT * FROM wear_download")
    suspend fun getAllDownloadsList(): List<WearDownloadEntity>

    @Query("SELECT * FROM wear_download")
    fun getAllDownloads(): Flow<List<WearDownloadEntity>>

    @Transaction
    @Query("SELECT * FROM wear_download")
    fun getDownloadsWithSongs(): Flow<List<DownloadWithSong>>

    @Query("SELECT * FROM wear_download WHERE songId = :songId")
    suspend fun getDownloadBySongId(songId: String): WearDownloadEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownload(download: WearDownloadEntity)

    @Delete
    suspend fun deleteDownload(download: WearDownloadEntity)

    @Query("DELETE FROM wear_download WHERE songId = :songId")
    suspend fun deleteDownloadById(songId: String)

    @Query("SELECT * FROM wear_download ORDER BY downloadedAt ASC")
    suspend fun getAllDownloadsSortedByDate(): List<WearDownloadEntity>

    @Query("DELETE FROM wear_download")
    suspend fun deleteAllDownloads()

    @Query("SELECT SUM(sizeBytes) FROM wear_download")
    suspend fun getTotalDownloadSize(): Long?

    @Query("SELECT COUNT(*) FROM wear_download")
    suspend fun getDownloadCount(): Int

    @Query("SELECT * FROM wear_download WHERE downloadedAt < :timestamp")
    suspend fun getStaleDownloads(timestamp: Long): List<WearDownloadEntity>
}

data class DownloadWithSong(
    @Embedded val download: WearDownloadEntity,
    @Relation(
        parentColumn = "songId",
        entityColumn = "id"
    )
    val song: WearSongEntity
)