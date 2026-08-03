package com.music.vivi.wear.auth

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Secure storage for authentication credentials using EncryptedSharedPreferences.
 */
@Singleton
class WearAuthStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences: SharedPreferences by lazy {
        try {
            EncryptedSharedPreferences.create(
                context,
                "vivi_auth",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to create EncryptedSharedPreferences, falling back to regular")
            // Fallback to regular SharedPreferences if encryption fails
            context.getSharedPreferences("vivi_auth_fallback", Context.MODE_PRIVATE)
        }
    }

    companion object {
        private const val KEY_AUTH_COOKIE = "auth_cookie"
        private const val KEY_TIMESTAMP = "auth_timestamp"
        private const val KEY_AUDIO_QUALITY = "audio_quality"
        private const val KEY_DOWNLOAD_SIZE_CAP = "download_size_cap"
    }

    fun storeAuthCookie(cookie: String) {
        sharedPreferences.edit()
            .putString(KEY_AUTH_COOKIE, cookie)
            .putLong(KEY_TIMESTAMP, System.currentTimeMillis())
            .apply()
        Timber.d("Auth cookie stored")
    }

    fun getAuthCookie(): String? {
        return sharedPreferences.getString(KEY_AUTH_COOKIE, null)
    }

    fun getAuthTimestamp(): Long {
        return sharedPreferences.getLong(KEY_TIMESTAMP, 0L)
    }

    fun clearAuthCookie() {
        sharedPreferences.edit()
            .remove(KEY_AUTH_COOKIE)
            .remove(KEY_TIMESTAMP)
            .apply()
        Timber.d("Auth cookie cleared")
    }

    fun hasAuthCookie(): Boolean {
        return sharedPreferences.contains(KEY_AUTH_COOKIE)
    }

    // Settings-related methods
    fun saveAudioQuality(quality: com.music.vivi.wear.network.StreamingQuality) {
        sharedPreferences.edit()
            .putString(KEY_AUDIO_QUALITY, quality.name)
            .apply()
        Timber.d("Audio quality saved: $quality")
    }

    fun getAudioQuality(): com.music.vivi.wear.network.StreamingQuality? {
        val qualityName = sharedPreferences.getString(KEY_AUDIO_QUALITY, null)
        return try {
            com.music.vivi.wear.network.StreamingQuality.valueOf(qualityName ?: "MEDIUM")
        } catch (e: Exception) {
            null
        }
    }

    fun saveDownloadSizeCap(capMB: Long) {
        sharedPreferences.edit()
            .putLong(KEY_DOWNLOAD_SIZE_CAP, capMB)
            .apply()
        Timber.d("Download size cap saved: $capMB MB")
    }

    fun getDownloadSizeCap(): Long? {
        return if (sharedPreferences.contains(KEY_DOWNLOAD_SIZE_CAP)) {
            sharedPreferences.getLong(KEY_DOWNLOAD_SIZE_CAP, 1024L)
        } else {
            null
        }
    }
}