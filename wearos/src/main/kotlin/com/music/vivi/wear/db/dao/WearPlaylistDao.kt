package com.music.vivi.wear.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.music.vivi.wear.db.entities.WearPlaylistEntity
import com.music.vivi.wear.db.entities.WearPlaylistSongMap
import com.music.vivi.wear.db.entities.WearSongEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WearPlaylistDao {
    @Query("SELECT * FROM wear_playlist")
    fun getAllPlaylists(): Flow<List<WearPlaylistEntity>>

    @Query("SELECT * FROM wear_playlist WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): WearPlaylistEntity?

    @Transaction
    @Query("SELECT * FROM wear_playlist WHERE id = :playlistId")
    suspend fun getPlaylistWithSongs(playlistId: String): PlaylistWithSongs?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: WearPlaylistEntity)

    @Update
    suspend fun updatePlaylist(playlist: WearPlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: WearPlaylistEntity)

    @Query("DELETE FROM wear_playlist WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongMap(map: WearPlaylistSongMap)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistSongMaps(maps: List<WearPlaylistSongMap>)

    @Query("DELETE FROM wear_playlist_song_map WHERE playlistId = :playlistId")
    suspend fun deletePlaylistSongs(playlistId: String)

    @Query("DELETE FROM wear_playlist_song_map WHERE playlistId = :playlistId AND songId = :songId")
    suspend fun deleteSongFromPlaylist(playlistId: String, songId: String)
}

data class PlaylistWithSongs(
    val playlist: WearPlaylistEntity,
    val songs: List<WearSongEntity>
)