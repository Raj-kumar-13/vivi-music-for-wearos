package com.music.vivi.wear

import android.app.Application
import android.os.PowerManager
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import com.music.vivi.wear.worker.CleanupScheduler
import com.music.vivi.wear.network.ConnectivityManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application(), Configuration.Provider {

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .setMinimumLoggingLevel(android.util.Log.INFO)
            .build()

    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        Timber.d("Vivi Wear Application starting")

        // Move non-critical init off the main thread to avoid blocking first frame
        appScope.launch {
            try {
                connectivityManager.initialize()
            } catch (e: Exception) {
                Timber.e(e, "Failed to initialize ConnectivityManager")
            }
        }
        appScope.launch {
            try {
                CleanupScheduler.scheduleWeeklyCleanup(this@WearApplication)
            } catch (e: Exception) {
                Timber.e(e, "Failed to schedule weekly cleanup")
            }
        }
        appScope.launch {
            try {
                checkBatteryOptimization()
            } catch (e: Exception) {
                Timber.e(e, "Failed to check battery optimization")
            }
        }
    }

    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(PowerManager::class.java)
        val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations(packageName)
        
        if (!isIgnoringBatteryOptimizations) {
            Timber.w("Battery optimizations are active - this may affect music playback")
            Timber.w("Users should add Vivi Wear to battery optimization exceptions")
        } else {
            Timber.d("Battery optimizations are properly configured")
        }
    }
}