package com.nndwn.nativeaudio

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import org.jtransforms.fft.DoubleFFT_1D
import kotlin.math.sqrt

object AudioProcessor {
    private var audioRecord: AudioRecord? = null
    private var isRecording = false

    private const val SAMPLE_RATE = 44100
    private const val FRAME_SIZE = 2048

    @JvmStatic var latestFrequency: Float = 0f
    @JvmStatic var latestVolume: Float = 0f


    @SuppressLint("MissingPermission")
    @JvmStatic
    fun startRecording() {
        if (isRecording) return

        val minBufSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

        audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            maxOf(minBufSize, FRAME_SIZE)
        )


        if (AcousticEchoCanceler.isAvailable()) {
            AcousticEchoCanceler.create(audioRecord!!.audioSessionId).enabled = true
        }

        audioRecord?.startRecording()
        isRecording = true

        val fft = DoubleFFT_1D(FRAME_SIZE.toLong())

        Thread {
            val audioBuffer = ShortArray(FRAME_SIZE)
            val fftBuffer = DoubleArray(FRAME_SIZE)

            while (isRecording) {
                val read = audioRecord?.read(audioBuffer, 0, FRAME_SIZE) ?: 0
                if (read > 0) {

                    var sum = 0.0
                    for (i in 0 until FRAME_SIZE) {
                        fftBuffer[i] = audioBuffer[i].toDouble()
                        sum += fftBuffer[i] * fftBuffer[i]
                    }
                    latestVolume = sqrt(sum / FRAME_SIZE).toFloat()

                    fft.realForward(fftBuffer)

                    latestFrequency = calculatePeakFrequency(fftBuffer)
                }
            }
        }.start()
    }

    private fun calculatePeakFrequency(fftBuffer: DoubleArray): Float {
        var maxMagnitude = -1.0
        var maxIndex = -1


        for (i in 0 until FRAME_SIZE / 2) {
            val re = fftBuffer[2 * i]
            val im = fftBuffer[2 * i + 1]
            val magnitude = sqrt(re * re + im * im)

            if (magnitude > maxMagnitude) {
                maxMagnitude = magnitude
                maxIndex = i
            }
        }


        return maxIndex.toFloat() * SAMPLE_RATE.toFloat() / FRAME_SIZE.toFloat()
    }

    @JvmStatic
    fun stopRecording() {
        isRecording = false
        audioRecord?.stop()
        audioRecord?.release()
        audioRecord = null
    }

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