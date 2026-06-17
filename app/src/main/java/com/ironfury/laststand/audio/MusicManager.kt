package com.ironfury.laststand.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.concurrent.thread

object MusicManager {
    private const val SAMPLE_RATE = 22050
    private var currentTrack: AudioTrack? = null
    private var isPlaying = false
    private var currentMusic: Music? = null
    private var isMuted = false

    enum class Music {
        GAMEPLAY,
        BOSS_BATTLE,
        VICTORY,
        GAME_OVER
    }

    fun play(music: Music) {
        if (music == currentMusic && isPlaying) return

        stop()
        currentMusic = music

        if (isMuted) return

        isPlaying = true

        thread {
            try {
                when (music) {
                    Music.GAMEPLAY -> playGameplayMusic()
                    Music.BOSS_BATTLE -> playBossMusic()
                    Music.VICTORY -> playVictoryMusic()
                    Music.GAME_OVER -> playGameOverMusic()
                }
            } catch (e: Exception) {
                // Silently fail
            }
        }
    }

    fun stop() {
        isPlaying = false
        currentMusic = null
        currentTrack?.let {
            try {
                it.stop()
                it.release()
            } catch (e: Exception) {
                // Ignore
            }
        }
        currentTrack = null
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
        if (muted) stop()
    }

    // Note frequencies (Hz) — using minor keys for military/dark feel
    private object N {
        const val C3 = 130.81; const val D3 = 146.83; const val Eb3 = 155.56
        const val E3 = 164.81; const val F3 = 174.61; const val G3 = 196.00
        const val Ab3 = 207.65; const val A3 = 220.00; const val Bb3 = 233.08
        const val B3 = 246.94

        const val C4 = 261.63; const val D4 = 293.66; const val Eb4 = 311.13
        const val E4 = 329.63; const val F4 = 349.23; const val G4 = 392.00
        const val Ab4 = 415.30; const val A4 = 440.00; const val Bb4 = 466.16
        const val B4 = 493.88

        const val C5 = 523.25; const val D5 = 587.33; const val Eb5 = 622.25
        const val E5 = 659.25; const val F5 = 698.46; const val G5 = 783.99
        const val A5 = 880.00

        const val R = 0.0 // Rest
    }

    // ================================================================
    // GAMEPLAY — Driving E minor military march, original composition
    // ================================================================
    private fun playGameplayMusic() {
        val bpm = 138.0
        val beat = 60.0 / bpm
        val eighth = beat / 2

        // Melody: E minor pentatonic riff — aggressive, rhythmic
        val melody = listOf(
            // Bar 1: Opening riff
            N.E4, N.R, N.G4, N.E4, N.R, N.D4, N.E4, N.R,
            // Bar 2: Climb
            N.G4, N.R, N.A4, N.G4, N.R, N.E4, N.D4, N.R,
            // Bar 3: Tension
            N.B4, N.R, N.A4, N.G4, N.R, N.E4, N.G4, N.R,
            // Bar 4: Resolve down
            N.A4, N.R, N.G4, N.R, N.E4, N.D4, N.E4, N.R,
            // Bar 5: Variation — higher energy
            N.E5, N.R, N.D5, N.B4, N.R, N.A4, N.G4, N.R,
            // Bar 6: Descending run
            N.B4, N.A4, N.G4, N.R, N.E4, N.R, N.D4, N.R,
            // Bar 7: Low growl section
            N.E4, N.E4, N.R, N.G4, N.E4, N.R, N.D4, N.E4,
            // Bar 8: Turnaround
            N.G4, N.R, N.A4, N.B4, N.A4, N.G4, N.E4, N.R
        )

        // Bass: Driving E minor root movement
        val bass = listOf(
            N.E3, N.E3, N.E3, N.E3, N.G3, N.G3, N.A3, N.A3,
            N.E3, N.E3, N.E3, N.E3, N.C3, N.C3, N.D3, N.D3,
            N.E3, N.E3, N.E3, N.E3, N.G3, N.G3, N.A3, N.A3,
            N.B3, N.B3, N.A3, N.A3, N.G3, N.G3, N.E3, N.E3
        )

        // Drum pattern — militaristic march
        val kick =  listOf(1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 1, 0, 0, 0)
        val snare = listOf(0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0)
        val hat =   listOf(1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0, 1, 0)

        val loopSamples = (melody.size * eighth * SAMPLE_RATE).toInt()
        val samples = ShortArray(loopSamples)

        for (i in 0 until loopSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val melIdx = ((t / eighth) % melody.size).toInt()
            val bassIdx = ((t / beat) % bass.size).toInt()
            val drumIdx = ((t / (beat / 4)) % kick.size).toInt()
            val noteT = (t % eighth) / eighth

            var sample = 0.0

            // Melody — pulse wave (duty cycle 25%) for gritty NES-like tone
            if (melody[melIdx] > 0) {
                val env = if (noteT < 0.08) noteT * 12.5 else 1.0 - noteT * 0.4
                val phase = (t * melody[melIdx]) % 1.0
                val pulse = if (phase < 0.25) 1.0 else -1.0
                sample += pulse * env * 0.22
            }

            // Bass — triangle wave for warm low end
            if (bass[bassIdx] > 0) {
                val env = 0.8
                sample += triangleWave(t, bass[bassIdx]) * env * 0.28
            }

            // Drums
            val drumT = (t % (beat / 4)) / (beat / 4)
            if (kick[drumIdx] == 1 && drumT < 0.25) {
                val kickEnv = (1.0 - drumT / 0.25)
                sample += sin(2.0 * PI * 55.0 * (1.0 - drumT) * t) * kickEnv * 0.2
                sample += noise() * kickEnv * 0.1
            }
            if (snare[drumIdx] == 1 && drumT < 0.15) {
                sample += noise() * (1.0 - drumT / 0.15) * 0.18
            }
            if (hat[drumIdx] == 1 && drumT < 0.05) {
                sample += noise() * (1.0 - drumT / 0.05) * 0.06
            }

            samples[i] = (sample * 10000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playLoop(samples)
    }

    // ================================================================
    // BOSS BATTLE — Intense A minor, fast and aggressive
    // ================================================================
    private fun playBossMusic() {
        val bpm = 165.0
        val beat = 60.0 / bpm
        val eighth = beat / 2

        // Melody: A minor, aggressive and chromatic
        val melody = listOf(
            // Bar 1-2: Stabbing riff
            N.A4, N.R, N.A4, N.C5, N.R, N.A4, N.Ab4, N.A4,
            N.R, N.E5, N.R, N.D5, N.C5, N.R, N.A4, N.R,
            // Bar 3-4: Rising tension
            N.A4, N.R, N.C5, N.D5, N.R, N.E5, N.R, N.F5,
            N.E5, N.R, N.D5, N.R, N.C5, N.A4, N.R, N.R,
            // Bar 5-6: Chromatic chaos
            N.E5, N.Eb5, N.D5, N.R, N.C5, N.R, N.A4, N.R,
            N.C5, N.R, N.D5, N.Eb5, N.E5, N.R, N.R, N.R,
            // Bar 7-8: Fierce ending
            N.A4, N.C5, N.A4, N.R, N.E4, N.R, N.A4, N.R,
            N.G4, N.R, N.A4, N.R, N.E5, N.D5, N.C5, N.R
        )

        // Bass: Relentless eighth note drive
        val bass = listOf(
            N.A3, N.A3, N.A3, N.A3, N.A3, N.A3, N.G3, N.G3,
            N.F3, N.F3, N.F3, N.F3, N.E3, N.E3, N.E3, N.E3,
            N.A3, N.A3, N.A3, N.A3, N.C3, N.C3, N.D3, N.D3,
            N.E3, N.E3, N.E3, N.E3, N.A3, N.A3, N.E3, N.E3
        )

        val loopSamples = (melody.size * eighth * SAMPLE_RATE).toInt()
        val samples = ShortArray(loopSamples)

        for (i in 0 until loopSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val melIdx = ((t / eighth) % melody.size).toInt()
            val bassIdx = ((t / eighth) % bass.size).toInt()
            val noteT = (t % eighth) / eighth

            var sample = 0.0

            // Melody — sawtooth for aggressive edge
            if (melody[melIdx] > 0) {
                val env = if (noteT < 0.04) noteT * 25 else 1.0 - noteT * 0.25
                sample += sawtoothWave(t, melody[melIdx]) * env * 0.22
            }

            // Bass — pulsing square wave
            if (bass[bassIdx] > 0) {
                val env = 0.6 + sin(t * 10 * PI) * 0.3
                sample += squareWave(t, bass[bassIdx]) * env * 0.3
            }

            // Fast kick on every beat
            val drumT = (t % (beat / 2)) / (beat / 2)
            if (drumT < 0.15) {
                val kickEnv = (1.0 - drumT / 0.15)
                sample += noise() * kickEnv * 0.2
                sample += sin(2.0 * PI * 45.0 * t) * kickEnv * 0.15
            }
            // Snare every other beat
            val snareT = (t % beat) / beat
            if (snareT > 0.45 && snareT < 0.6) {
                sample += noise() * (1.0 - (snareT - 0.45) / 0.15) * 0.15
            }

            samples[i] = (sample * 10000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playLoop(samples)
    }

    // ================================================================
    // VICTORY — D major power chord fanfare
    // ================================================================
    private fun playVictoryMusic() {
        val bpm = 110.0
        val beat = 60.0 / bpm

        val melody = listOf(
            // Rising D major triumph
            N.D4, N.R, N.E4, N.R, N.A4, N.R, N.R, N.R,
            N.D5, N.R, N.R, N.R, N.E5, N.R, N.R, N.R,
            N.A4, N.R, N.D5, N.R, N.A5, N.R, N.R, N.R,
            N.R, N.R, N.R, N.R, N.R, N.R, N.R, N.R
        )

        val bass = listOf(
            N.D3, N.D3, N.A3, N.A3, N.D3, N.D3, N.G3, N.G3,
            N.D3, N.D3, N.A3, N.A3, N.D3, N.D3, N.D3, N.D3
        )

        val eighth = beat / 2
        val totalSamples = (melody.size * eighth * SAMPLE_RATE).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val melIdx = (t / eighth).toInt().coerceAtMost(melody.size - 1)
            val bassIdx = (t / beat).toInt().coerceAtMost(bass.size - 1)
            val noteT = (t % eighth) / eighth
            val progress = i.toDouble() / totalSamples

            var sample = 0.0

            if (melody[melIdx] > 0) {
                val env = if (noteT < 0.1) noteT * 10 else 1.0 - (noteT - 0.1) * 0.4
                // Rich chord — root + fifth + octave
                sample += sin(2.0 * PI * melody[melIdx] * t) * env * 0.25
                sample += sin(2.0 * PI * melody[melIdx] * 1.5 * t) * env * 0.15
                sample += sin(2.0 * PI * melody[melIdx] * 2.0 * t) * env * 0.1
            }

            if (bass[bassIdx] > 0) {
                sample += triangleWave(t, bass[bassIdx]) * 0.3
            }

            // Fade at end
            val fade = if (progress > 0.8) (1.0 - progress) / 0.2 else 1.0
            samples[i] = (sample * fade * 10000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playOnce(samples)
    }

    // ================================================================
    // GAME OVER — Dark C minor descent
    // ================================================================
    private fun playGameOverMusic() {
        val bpm = 60.0
        val beat = 60.0 / bpm

        val melody = listOf(
            N.Eb4, N.R, N.D4, N.R, N.C4, N.R, N.R, N.R,
            N.Bb3, N.R, N.Ab3, N.R, N.G3, N.R, N.R, N.R,
            N.R, N.R, N.R, N.R, N.R, N.R, N.R, N.R
        )

        val bass = listOf(
            N.C3, N.R, N.Ab3, N.R, N.G3, N.R, N.C3, N.R
        )

        val eighth = beat / 2
        val totalSamples = (melody.size * eighth * SAMPLE_RATE).toInt()
        val samples = ShortArray(totalSamples)

        for (i in 0 until totalSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / totalSamples
            val melIdx = (t / eighth).toInt().coerceAtMost(melody.size - 1)
            val bassIdx = (t / beat).toInt().coerceAtMost(bass.size - 1)
            val noteT = (t % eighth) / eighth

            var sample = 0.0
            val fadeOut = 1.0 - progress * 0.6

            if (melody[melIdx] > 0) {
                val env = (1.0 - noteT * 0.6) * fadeOut
                sample += triangleWave(t, melody[melIdx]) * env * 0.3
            }

            if (bass[bassIdx] > 0) {
                sample += triangleWave(t, bass[bassIdx]) * 0.2 * fadeOut
            }

            samples[i] = (sample * 10000).toInt().coerceIn(-32767, 32767).toShort()
        }

        playOnce(samples)
    }

    // ================================================================
    // PLAYBACK
    // ================================================================
    private fun playLoop(samples: ShortArray) {
        val bufferSize = samples.size * 2

        currentTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        currentTrack?.let { track ->
            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)
            track.play()

            while (isPlaying && track.playState == AudioTrack.PLAYSTATE_PLAYING) {
                Thread.sleep(100)
            }
        }
    }

    private fun playOnce(samples: ShortArray) {
        val bufferSize = samples.size * 2

        currentTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_GAME)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(SAMPLE_RATE)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        currentTrack?.let { track ->
            track.write(samples, 0, samples.size)
            track.play()

            val duration = samples.size * 1000L / SAMPLE_RATE
            Thread.sleep(duration)

            track.stop()
            track.release()
            currentTrack = null
            isPlaying = false
        }
    }

    // ================================================================
    // WAVEFORMS
    // ================================================================
    private fun squareWave(t: Double, freq: Double): Double {
        return if (sin(2.0 * PI * freq * t) > 0) 1.0 else -1.0
    }

    private fun triangleWave(t: Double, freq: Double): Double {
        val phase = (t * freq) % 1.0
        return if (phase < 0.5) 4.0 * phase - 1.0 else 3.0 - 4.0 * phase
    }

    private fun sawtoothWave(t: Double, freq: Double): Double {
        return ((t * freq) % 1.0) * 2.0 - 1.0
    }

    private fun noise(): Double {
        return Math.random() * 2.0 - 1.0
    }
}
