package com.music.vivi.wear.data

import com.music.innertube.YouTube
import com.music.vivi.wear.auth.WearAuthStorage
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages authentication for the Wear OS app.
 * Handles injecting auth cookies into the :innertube client.
 */
@Singleton
class WearAuthManager @Inject constructor(
    private val authStorage: WearAuthStorage
) {

    /**
     * Inject the auth cookie into the :innertube YouTube client.
     * This should be called before any API request.
     */
    fun injectAuth() {
        try {
            val cookie = authStorage.getAuthCookie()
            if (cookie != null) {
                YouTube.cookie = cookie
                Timber.d("Auth cookie injected successfully")
            } else {
                Timber.w("No auth cookie found")
                YouTube.cookie = null
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to inject auth cookie")
            YouTube.cookie = null
        }
    }

    /**
     * Check if auth is available.
     */
    fun hasAuth(): Boolean {
        return authStorage.getAuthCookie() != null
    }

    /**
     * Clear auth (e.g., after 401/403 error).
     */
    fun clearAuth() {
        authStorage.clearAuthCookie()
        YouTube.cookie = null
        Timber.d("Auth cleared")
    }

    /**
     * Store auth cookie (from phone sync).
     */
    fun storeAuth(cookie: String) {
        authStorage.storeAuthCookie(cookie)
        YouTube.cookie = cookie
        Timber.d("Auth stored successfully")
    }
}