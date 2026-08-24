package com.music.vivi.wear.di

import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Playback-related Hilt module.
 * Audio output selection and Bluetooth enforcement are handled directly
 * in WearPlaybackService via BluetoothRouteObserver.
 */
@Module
@InstallIn(SingletonComponent::class)
object PlaybackModule
