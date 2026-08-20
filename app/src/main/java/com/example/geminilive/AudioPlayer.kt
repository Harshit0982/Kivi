package com.example.geminilive

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.util.Log

class AudioPlayer {
    private val TAG = "AudioPlayer"
    private val SAMPLE_RATE = 24000  // Gemini Live output is 24kHz
    private var audioTrack: AudioTrack? = null
    private var isPlaying = false

    init {
        try {
            val minBufferSize = AudioTrack.getMinBufferSize(
                SAMPLE_RATE,
                AudioFormat.CHANNEL_OUT_MONO,
                AudioFormat.ENCODING_PCM_16BIT
            )
            audioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build()
                )
                .setBufferSizeInBytes(minBufferSize * 4)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
            audioTrack?.play()
            isPlaying = true
            Log.d(TAG, "AudioTrack initialized")
        } catch (e: Exception) {
            Log.e(TAG, "AudioTrack init failed: ${e.message}")
        }
    }

    fun play(pcmData: ByteArray) {
        try {
            if (isPlaying && audioTrack != null) {
                audioTrack?.write(pcmData, 0, pcmData.size)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Write failed: ${e.message}")
        }
    }

    fun release() {
        isPlaying = false
        try {
            audioTrack?.stop()
            audioTrack?.release()
            audioTrack = null
        } catch (e: Exception) {
            Log.e(TAG, "Release failed: ${e.message}")
        }
    }
}
