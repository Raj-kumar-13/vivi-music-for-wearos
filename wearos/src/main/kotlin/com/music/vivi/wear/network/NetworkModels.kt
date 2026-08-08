package com.music.vivi.wear.network

/**
 * Enumeration of connectivity modes for Wear OS.
 */
enum class ConnectivityMode {
    ONLINE,
    OFFLINE,
    UNKNOWN;

    fun getStreamingQuality(): StreamingQuality = when (this) {
        ONLINE -> StreamingQuality.MEDIUM
        OFFLINE -> StreamingQuality.OFFLINE
        UNKNOWN -> StreamingQuality.LOW
    }
}

/**
 * Enumeration of streaming qualities for Wear OS.
 */
enum class StreamingQuality {
    HIGH,
    MEDIUM,
    LOW,
    OFFLINE
}
