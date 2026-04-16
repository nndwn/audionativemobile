package com.nndwn.nativeaudio

import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioManager

object HeadsetChecker {
    @JvmStatic
    fun isHeadsetPlugged(context: Context) : Boolean {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
        return devices.any { devices ->
            devices.type == AudioDeviceInfo.TYPE_WIRED_HEADSET ||
                    devices.type == AudioDeviceInfo.TYPE_WIRED_HEADPHONES ||
                    devices.type == AudioDeviceInfo.TYPE_BLUETOOTH_A2DP ||
                    devices.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
        }
    }
}