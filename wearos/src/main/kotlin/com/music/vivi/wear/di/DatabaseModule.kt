package com.music.vivi.wear.di

import android.content.Context
import androidx.room.Room
import com.music.vivi.wear.db.WearMusicDatabase
import com.music.vivi.wear.db.dao.WearDownloadDao
import com.music.vivi.wear.db.dao.WearPlaybackHistoryDao
import com.music.vivi.wear.db.dao.WearPlaylistDao
import com.music.vivi.wear.db.dao.WearSongDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideWearMusicDatabase(
        @ApplicationContext context: Context
    ): WearMusicDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            WearMusicDatabase::class.java,
            "wear_music.db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideWearSongDao(database: WearMusicDatabase): WearSongDao {
        return database.songDao()
    }

    @Provides
    @Singleton
    fun provideWearPlaylistDao(database: WearMusicDatabase): WearPlaylistDao {
        return database.playlistDao()
    }

    @Provides
    @Singleton
    fun provideWearDownloadDao(database: WearMusicDatabase): WearDownloadDao {
        return database.downloadDao()
    }

    @Provides
    @Singleton
    fun provideWearPlaybackHistoryDao(database: WearMusicDatabase): WearPlaybackHistoryDao {
        return database.playbackHistoryDao()
    }
}