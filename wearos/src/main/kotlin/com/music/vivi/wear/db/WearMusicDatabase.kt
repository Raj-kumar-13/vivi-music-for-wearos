package com.music.vivi.wear.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.music.vivi.wear.db.dao.WearDownloadDao
import com.music.vivi.wear.db.dao.WearPlaybackHistoryDao
import com.music.vivi.wear.db.dao.WearPlaylistDao
import com.music.vivi.wear.db.dao.WearSongDao
import com.music.vivi.wear.db.entities.WearDownloadEntity
import com.music.vivi.wear.db.entities.WearPlaybackHistoryEntity
import com.music.vivi.wear.db.entities.WearPlaylistEntity
import com.music.vivi.wear.db.entities.WearPlaylistSongMap
import com.music.vivi.wear.db.entities.WearSongEntity

@Database(
    entities = [
        WearSongEntity::class,
        WearPlaylistEntity::class,
        WearPlaylistSongMap::class,
        WearDownloadEntity::class,
        WearPlaybackHistoryEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class WearMusicDatabase : RoomDatabase() {
    abstract fun songDao(): WearSongDao
    abstract fun playlistDao(): WearPlaylistDao
    abstract fun downloadDao(): WearDownloadDao
    abstract fun playbackHistoryDao(): WearPlaybackHistoryDao

    companion object {
        private const val DATABASE_NAME = "wear_music.db"

        @Volatile
        private var INSTANCE: WearMusicDatabase? = null

        fun getDatabase(context: Context): WearMusicDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WearMusicDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}