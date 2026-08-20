package com.music.vivi.wear.auth

import android.content.Intent
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Service that receives authentication data from the phone app via Wearable Data Layer.
 */
@AndroidEntryPoint
class AuthSyncListenerService : WearableListenerService() {

    @Inject
    lateinit var authStorage: WearAuthStorage

    companion object {
        const val AUTH_DATA_PATH = "/vivi/auth"
        private const val KEY_COOKIE = "cookie"
        private const val KEY_TIMESTAMP = "timestamp"
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        // DataEventBuffer is auto-released by WearableListenerService after callback returns
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED) {
                val path = event.dataItem.uri.path
                if (path == AUTH_DATA_PATH) {
                    handleAuthData(event.dataItem)
                }
            }
        }
    }

    private fun handleAuthData(dataItem: com.google.android.gms.wearable.DataItem) {
        try {
            val dataMap = com.google.android.gms.wearable.DataMapItem.fromDataItem(dataItem).dataMap
            
            val cookie = dataMap.getString(KEY_COOKIE)
            val timestamp = dataMap.getLong(KEY_TIMESTAMP)

            if (cookie != null && timestamp > 0) {
                authStorage.storeAuthCookie(cookie)
                Timber.d("Auth received from phone: timestamp=$timestamp")
                
                // You could show a notification here to confirm sync
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to handle auth data")
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == AUTH_DATA_PATH) {
            try {
                val data = messageEvent.data
                if (data == null || data.isEmpty()) {
                    Timber.w("Received empty auth message data")
                    return
                }
                val cookie = String(data)
                authStorage.storeAuthCookie(cookie)
                Timber.d("Auth received via message from phone")
            } catch (e: Exception) {
                Timber.e(e, "Failed to process auth message")
            }
        }
    }
}