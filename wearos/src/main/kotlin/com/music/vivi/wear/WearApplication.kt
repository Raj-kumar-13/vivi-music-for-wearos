package com.music.vivi.wear

import android.app.Application
import android.os.PowerManager
import dagger.hilt.android.HiltAndroidApp
import com.music.vivi.wear.worker.CleanupScheduler
import com.music.vivi.wear.network.ConnectivityManager
import timber.log.Timber
import javax.inject.Inject

@HiltAndroidApp
class WearApplication : Application() {

    @Inject
    lateinit var connectivityManager: ConnectivityManager

    override fun onCreate() {
        super.onCreate()
        Timber.d("Vivi Wear Application starting")

        // Initialize connectivity manager for network awareness
        connectivityManager.initialize()

        // Schedule weekly library cleanup with battery optimization
        CleanupScheduler.scheduleWeeklyCleanup(this)

        // Check battery optimization status
        checkBatteryOptimization()
    }

    private fun checkBatteryOptimization() {
        val powerManager = getSystemService(PowerManager::class.java)
        val isIgnoringBatteryOptimizations = powerManager.isIgnoringBatteryOptimizations()
        
        if (!isIgnoringBatteryOptimizations) {
            Timber.w("Battery optimizations are active - this may affect music playback")
            Timber.w("Users should add Vivi Wear to battery optimization exceptions")
        } else {
            Timber.d("Battery optimizations are properly configured")
        }
    }
}