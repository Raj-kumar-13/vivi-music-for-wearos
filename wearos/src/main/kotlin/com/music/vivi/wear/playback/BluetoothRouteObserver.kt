package com.music.vivi.wear.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import androidx.media3.common.Player

/**
 * Observes audio route changes and handles Bluetooth disconnect events.
 * Automatically pauses playback if Bluetooth audio device disconnects.
 */
class BluetoothRouteObserver(
    private val context: Context,
    private val bluetoothGate: BluetoothAudioGate,
    private val onBluetoothStateChanged: (Boolean) -> Unit
) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    private val audioDeviceCallback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) {
            onBluetoothStateChanged(bluetoothGate.isBluetoothAudioConnected())
        }

        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) {
            onBluetoothStateChanged(bluetoothGate.isBluetoothAudioConnected())
        }
    }

    fun register() {
        audioManager.registerAudioDeviceCallback(audioDeviceCallback, null)
    }

    fun unregister() {
        audioManager.unregisterAudioDeviceCallback(audioDeviceCallback)
    }

    companion object {
        /**
         * Create a BluetoothRouteObserver that auto-pauses playback on disconnect.
         */
        fun createWithAutoPause(
            context: Context,
            player: Player
        ): BluetoothRouteObserver {
            val bluetoothGate = BluetoothAudioGate(context)
            
            return BluetoothRouteObserver(
                context = context,
                bluetoothGate = bluetoothGate,
                onBluetoothStateChanged = { connected ->
                    if (!connected && player.isPlaying) {
                        player.pause()
                    }
                }
            )
        }
    }
}