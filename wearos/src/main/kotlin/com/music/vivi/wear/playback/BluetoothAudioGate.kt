package com.music.vivi.wear.playback

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

/**
 * Manual fallback for Bluetooth audio enforcement.
 * This provides a custom UX around the block if needed.
 * 
 * Primary enforcement is handled by Horologist's AudioOutputSelector.
 * This class can be used for custom "connect headphones" screens.
 */
class BluetoothAudioGate(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

    fun isBluetoothAudioConnected(): Boolean {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).any { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        }
    }

    fun getConnectedBluetoothDevices(): List<AudioDeviceInfo> {
        return audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS).filter { device ->
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
            device.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
            device.type == AudioDeviceInfo.TYPE_BLE_HEADSET ||
            device.type == AudioDeviceInfo.TYPE_BLE_SPEAKER
        }
    }
}