package com.example.emulator

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack

class AtariAudioPlayer {
    companion object {
        const val SAMPLE_RATE = 22050
        const val SAMPLES_PER_FRAME = SAMPLE_RATE / 60 // 367
    }

    private var track: AudioTrack? = null
    private var volumeGain: Float = 0.8f
    private var muted: Boolean = false

    fun start() {
        if (track != null) return
        val minBuf = AudioTrack.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(SAMPLES_PER_FRAME * 4)

        val audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(minBuf * 2)
            .build()

        audioTrack.setVolume(if (muted) 0f else volumeGain)
        audioTrack.play()
        track = audioTrack
    }

    fun setVolume(percent: Int, isMuted: Boolean) {
        volumeGain = (percent.coerceIn(0, 100) / 100f)
        muted = isMuted
        track?.setVolume(if (muted) 0f else volumeGain)
    }

    fun writeSamples(samples: ShortArray, count: Int) {
        val t = track ?: return
        if (t.playState == AudioTrack.PLAYSTATE_PLAYING) {
            t.write(samples, 0, count, AudioTrack.WRITE_NON_BLOCKING)
        }
    }

    fun stop() {
        track?.runCatching {
            pause()
            flush()
            release()
        }
        track = null
    }
}
