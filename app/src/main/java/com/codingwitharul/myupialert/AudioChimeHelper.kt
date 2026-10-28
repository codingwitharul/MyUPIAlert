package com.codingwitharul.myupialert

import android.content.Context
import android.hardware.camera2.CameraManager
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.sin

/**
 * 100% Offline Audio Chime Synthesizer, Bluetooth Keep-Alive, and Flashlight Strobe.
 * Generates instant, crisp dual-tone merchant soundbox chimes without relying on external media files.
 */
object AudioChimeHelper {
    private const val TAG = "AudioChimeHelper"
    private const val SAMPLE_RATE = 44100

    /**
     * Synthesizes and plays a dual-tone crystal chime (1046 Hz -> 2093 Hz) with an exponential decay envelope.
     */
    fun playPaymentChime(volumePercent: Int = 100) {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val durationMs = 240
                val totalSamples = (SAMPLE_RATE * (durationMs / 1000.0)).toInt()
                val halfSamples = totalSamples / 2
                val buffer = ShortArray(totalSamples)

                val freq1 = 1046.5 // C6 note
                val freq2 = 2093.0 // C7 note

                for (i in 0 until totalSamples) {
                    val freq = if (i < halfSamples) freq1 else freq2
                    val t = i.toDouble() / SAMPLE_RATE
                    val sine = sin(2.0 * Math.PI * freq * t)

                    // Exponential decay envelope for a sweet, clean bell chime sound
                    val segmentProgress = if (i < halfSamples) {
                        i.toDouble() / halfSamples
                    } else {
                        (i - halfSamples).toDouble() / halfSamples
                    }
                    val envelope = (1.0 - segmentProgress * 0.75) * (1.0 - segmentProgress * segmentProgress * 0.25)
                    val sampleValue = (sine * envelope * Short.MAX_VALUE * 0.85).toInt()
                    buffer[i] = sampleValue.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                }

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANCE_SONIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                val audioFormat = AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()

                val bufferSizeInBytes = buffer.size * 2
                val audioTrack = AudioTrack.Builder()
                    .setAudioAttributes(audioAttributes)
                    .setAudioFormat(audioFormat)
                    .setBufferSizeInBytes(bufferSizeInBytes)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                audioTrack.write(buffer, 0, buffer.size)

                val vol = (volumePercent / 100f).coerceIn(0.1f, 1.0f)
                audioTrack.setVolume(vol)
                audioTrack.play()

                delay(durationMs.toLong() + 50)
                audioTrack.stop()
                audioTrack.release()
            } catch (e: Exception) {
                Log.w(TAG, "Failed to synthesize payment chime: ${e.message}")
            }
        }
    }

    /**
     * Inaudible sub-sonic pulse to keep external Bluetooth speakers from entering sleep mode.
     */
    fun playBluetoothKeepAlive() {
        CoroutineScope(Dispatchers.Default).launch {
            try {
                val samples = 441 // 10ms at 44.1kHz
                val buffer = ShortArray(samples) { 1 } // Minimal DC offset tick

                val track = AudioTrack.Builder()
                    .setAudioAttributes(
                        AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .build()
                    )
                    .setAudioFormat(
                        AudioFormat.Builder()
                            .setSampleRate(SAMPLE_RATE)
                            .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                            .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                            .build()
                    )
                    .setBufferSizeInBytes(samples * 2)
                    .setTransferMode(AudioTrack.MODE_STATIC)
                    .build()

                track.write(buffer, 0, samples)
                track.setVolume(0.01f) // inaudible
                track.play()
                delay(20)
                track.stop()
                track.release()
            } catch (e: Exception) {
                Log.d(TAG, "BT keep-alive tick: ${e.message}")
            }
        }
    }

    /**
     * Strobe camera flashlight 3 times to visually alert merchant in noisy environments.
     */
    fun strobeFlashlight(context: Context, times: Int = 3) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return

        CoroutineScope(Dispatchers.Default).launch {
            try {
                val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager ?: return@launch
                val cameraId = cameraManager.cameraIdList.firstOrNull { id ->
                    val chars = cameraManager.getCameraCharacteristics(id)
                    chars.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
                } ?: return@launch

                for (i in 0 until times) {
                    cameraManager.setTorchMode(cameraId, true)
                    delay(120)
                    cameraManager.setTorchMode(cameraId, false)
                    delay(120)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Strobe flashlight failed: ${e.message}")
            }
        }
    }
}
