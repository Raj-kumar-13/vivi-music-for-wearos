package com.music.vivi.wear.di

import android.content.Context
import android.net.ConnectivityManager
import com.google.android.horologist.networks.status.NetworkRepository
import com.google.android.horologist.networks.status.NetworkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkRepository(
        @ApplicationContext context: Context
    ): NetworkRepository {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        // Horologist 0.7.15 requires a CoroutineScope
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        return NetworkRepositoryImpl(connectivityManager, scope)
    }
}
