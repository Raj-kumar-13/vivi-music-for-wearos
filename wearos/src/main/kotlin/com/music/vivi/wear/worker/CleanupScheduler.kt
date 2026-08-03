package com.music.vivi.wear.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.music.vivi.wear.worker.LibraryCleanupWorker
import java.util.concurrent.TimeUnit

/**
 * Scheduler for the library cleanup worker.
 * Runs weekly when device is idle, charging, and on unmetered network for battery optimization.
 */
object CleanupScheduler {

    fun scheduleWeeklyCleanup(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresDeviceIdle(true) // Only when device is idle
            .setRequiresCharging(true) // Only when charging to save battery
            .setRequiredNetworkType(NetworkType.UNMETERED) // Only on Wi-Fi to save data
            .setRequiresBatteryNotLow(true) // Only when battery is not low
            .build()

        val cleanupRequest = PeriodicWorkRequestBuilder<LibraryCleanupWorker>(
            7, TimeUnit.DAYS // Run weekly
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            LibraryCleanupWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            cleanupRequest
        )
    }

    fun cancelCleanup(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(LibraryCleanupWorker.WORK_NAME)
    }
}