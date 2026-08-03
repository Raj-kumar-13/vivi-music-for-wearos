package com.music.vivi.wear

import android.content.Context
import android.widget.Toast
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.tasks.await
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class to sync authentication from phone to Wear OS watch.
 * Uses the Wearable Data Layer API to send encrypted auth cookies.
 */
@Singleton
class WearAuthSyncHelper @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val WEAR_AUTH_PATH = "/vivi/auth"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_VISITOR_DATA = "visitor_data"
        private const val KEY_DATA_SYNC_ID = "data_sync_id"
    }

    private val dataClient: DataClient by lazy {
        Wearable.getDataClient(context)
    }

    /**
     * Send authentication data to connected Wear OS watch.
     * @param cookie The InnerTube auth cookie
     * @param visitorData The visitor data from InnerTube
     * @param dataSyncId The data sync ID
     * @return true if sent successfully, false otherwise
     */
    suspend fun sendAuthToWatch(
        cookie: String,
        visitorData: String = "",
        dataSyncId: String = ""
    ): Boolean {
        return try {
            Timber.d("Sending auth to Wear OS watch")
            
            val putDataMapRequest = PutDataMapRequest.create(WEAR_AUTH_PATH).apply {
                dataMap.putString(KEY_COOKIE, cookie)
                if (visitorData.isNotEmpty()) {
                    dataMap.putString(KEY_VISITOR_DATA, visitorData)
                }
                if (dataSyncId.isNotEmpty()) {
                    dataMap.putString(KEY_DATA_SYNC_ID, dataSyncId)
                }
            }
            
            val result = dataClient.putDataItem(putDataMapRequest.asPutDataRequest()).await()
            
            Timber.d("Auth sent to watch successfully: ${result.uri}")
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to send auth to watch")
            false
        }
    }

    /**
     * Check if there are any connected Wear OS devices.
     * @return true if at least one device is connected, false otherwise
     */
    suspend fun hasConnectedWearDevice(): Boolean {
        return try {
            val nodes = dataClient.connectedNodes.await()
            val hasConnected = nodes.isNotEmpty()
            Timber.d("Connected Wear OS nodes: ${nodes.size}")
            hasConnected
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for connected Wear OS devices")
            false
        }
    }

    /**
     * Show a toast message about the auth sync result.
     */
    fun showAuthSyncResult(success: Boolean) {
        val message = if (success) {
            "Login sent to watch"
        } else {
            "Failed to send login to watch"
        }
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}