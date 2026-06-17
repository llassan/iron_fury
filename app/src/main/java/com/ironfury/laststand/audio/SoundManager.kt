package com.ironfury.laststand.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import kotlin.math.PI
import kotlin.math.sin
import kotlin.concurrent.thread

object SoundManager {
    private const val SAMPLE_RATE = 22050
    private var isMuted = false

    enum class Sound {
        SHOOT_MACHINE_GUN,
        SHOOT_SPREAD,
        SHOOT_LASER,
        SHOOT_ROCKET,
        SHOOT_FLAME,
        EXPLOSION,
        ENEMY_DEATH,
        PLAYER_HIT,
        PLAYER_DEATH,
        COIN_COLLECT,
        BOSS_HIT,
        BOSS_DEATH,
        WEAPON_SWITCH,
        JUMP,
        VICTORY,
        GAME_OVER
    }

    fun play(sound: Sound) {
        if (isMuted) return

        thread {
            try {
                val samples = when (sound) {
                    Sound.SHOOT_MACHINE_GUN -> generateShootSound(800.0, 0.05)
                    Sound.SHOOT_SPREAD -> generateShootSound(600.0, 0.08)
                    Sound.SHOOT_LASER -> generateLaserSound()
                    Sound.SHOOT_ROCKET -> generateRocketSound()
                    Sound.SHOOT_FLAME -> generateFlameSound()
                    Sound.EXPLOSION -> generateExplosionSound()
                    Sound.ENEMY_DEATH -> generateEnemyDeathSound()
                    Sound.PLAYER_HIT -> generatePlayerHitSound()
                    Sound.PLAYER_DEATH -> generateDeathSound()
                    Sound.COIN_COLLECT -> generateCoinSound()
                    Sound.BOSS_HIT -> generateBossHitSound()
                    Sound.BOSS_DEATH -> generateBossDeathSound()
                    Sound.WEAPON_SWITCH -> generateWeaponSwitchSound()
                    Sound.JUMP -> generateJumpSound()
                    Sound.VICTORY -> generateVictorySound()
                    Sound.GAME_OVER -> generateGameOverSound()
                }
                playSound(samples)
            } catch (e: Exception) {
                // Silently fail if audio doesn't work
            }
        }
    }

    fun setMuted(muted: Boolean) {
        isMuted = muted
    }

    private fun playSound(samples: ShortArray) {
        val bufferSize = samples.size * 2
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
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STATIC)
            .build()

        audioTrack.write(samples, 0, samples.size)
        audioTrack.play()

        Thread.sleep((samples.size * 1000L / SAMPLE_RATE) + 50)
        audioTrack.release()
    }

    // Harsh metallic gunshot — quick frequency drop with noise burst
    private fun generateShootSound(baseFreq: Double, duration: Double): ShortArray {
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            // Rapid frequency drop — sharp crack, not musical
            val freq = baseFreq * (1.0 - progress * 0.7)
            val envelope = (1.0 - progress) * (1.0 - progress) // Squared for snappier decay
            val wave = if (sin(2.0 * PI * freq * t) > 0) 1.0 else -1.0
            val noise = (Math.random() - 0.5) * 0.4 * (1.0 - progress)

            samples[i] = ((wave * 0.7 + noise) * envelope * 9000).toInt().toShort()
        }
        return samples
    }

    // Electric zap — harsh saw sweep
    private fun generateLaserSound(): ShortArray {
        val duration = 0.08
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 3000.0 - progress * 2500.0
            val envelope = (1.0 - progress)
            val saw = ((t * freq) % 1.0) * 2.0 - 1.0

            samples[i] = (saw * envelope * 5000).toInt().toShort()
        }
        return samples
    }

    // Heavy thud with rumble
    private fun generateRocketSound(): ShortArray {
        val duration = 0.18
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 100.0 + progress * 80.0
            val envelope = if (progress < 0.1) progress * 10 else (1.0 - (progress - 0.1) / 0.9)

            val wave = sin(2.0 * PI * freq * t) * 0.6 + (Math.random() - 0.5) * 0.6 * (1.0 - progress)

            samples[i] = (wave * envelope * 8000).toInt().toShort()
        }
        return samples
    }

    // Crackling hiss
    private fun generateFlameSound(): ShortArray {
        val duration = 0.06
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples
            val envelope = (1.0 - progress)

            // Filtered noise with subtle low tone
            val noise = (Math.random() - 0.5) * 2.0
            val tone = sin(2.0 * PI * 180.0 * t) * 0.2

            samples[i] = ((noise * 0.8 + tone) * envelope * 3500).toInt().toShort()
        }
        return samples
    }

    // Heavy boom with debris noise
    private fun generateExplosionSound(): ShortArray {
        val duration = 0.35
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 60.0 * (1.0 - progress * 0.5)
            val envelope = (1.0 - progress) * (1.0 - progress)

            val boom = sin(2.0 * PI * freq * t) + sin(2.0 * PI * freq * 1.5 * t) * 0.3
            val noise = (Math.random() - 0.5) * (1.0 - progress * 0.6)

            samples[i] = ((boom * 0.6 + noise * 0.4) * envelope * 11000).toInt().toShort()
        }
        return samples
    }

    // Sharp burst — distorted pop
    private fun generateEnemyDeathSound(): ShortArray {
        val duration = 0.12
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 500.0 - progress * 400.0
            val envelope = (1.0 - progress) * (1.0 - progress)

            // Distorted square + noise
            val square = if (sin(2.0 * PI * freq * t) > 0) 1.0 else -1.0
            val noise = (Math.random() - 0.5) * 0.5

            samples[i] = ((square * 0.6 + noise) * envelope * 7000).toInt().toShort()
        }
        return samples
    }

    // Harsh buzz
    private fun generatePlayerHitSound(): ShortArray {
        val duration = 0.15
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 180.0
            val envelope = 1.0 - progress

            val wave = if ((sin(2.0 * PI * freq * t) > 0) xor (sin(2.0 * PI * freq * 1.5 * t) > 0)) 1.0 else -1.0

            samples[i] = (wave * envelope * 5000).toInt().toShort()
        }
        return samples
    }

    // Descending gritty tone
    private fun generateDeathSound(): ShortArray {
        val duration = 0.5
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 350.0 * (1.0 - progress * 0.7)
            val envelope = (1.0 - progress)

            val saw = ((t * freq) % 1.0) * 2.0 - 1.0
            val noise = (Math.random() - 0.5) * progress * 0.4

            samples[i] = ((saw * 0.7 + noise) * envelope * 6000).toInt().toShort()
        }
        return samples
    }

    // Metallic clink — short high ping, not musical
    private fun generateCoinSound(): ShortArray {
        val duration = 0.08
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            // Metallic ring — two inharmonic frequencies
            val envelope = (1.0 - progress) * (1.0 - progress)
            val ring1 = sin(2.0 * PI * 2200.0 * t)
            val ring2 = sin(2.0 * PI * 3350.0 * t) * 0.5
            val ring3 = sin(2.0 * PI * 4100.0 * t) * 0.25

            samples[i] = ((ring1 + ring2 + ring3) * envelope * 4000).toInt().toShort()
        }
        return samples
    }

    // Heavy metallic impact
    private fun generateBossHitSound(): ShortArray {
        val duration = 0.12
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 250.0 - progress * 150.0
            val envelope = (1.0 - progress) * (1.0 - progress)

            val hit = sin(2.0 * PI * freq * t) + sin(2.0 * PI * freq * 0.5 * t) * 0.5
            val metal = sin(2.0 * PI * 1800.0 * t) * (1.0 - progress) * 0.2

            samples[i] = ((hit + metal) * envelope * 8000).toInt().toShort()
        }
        return samples
    }

    // Massive layered explosion
    private fun generateBossDeathSound(): ShortArray {
        val duration = 1.0
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq1 = 50.0 * (1.0 - progress * 0.3)
            val freq2 = 90.0 * (1.0 - progress * 0.4)

            val envelope = (1.0 - progress)
            val boom1 = sin(2.0 * PI * freq1 * t)
            val boom2 = sin(2.0 * PI * freq2 * t)
            val noise = (Math.random() - 0.5) * (1.0 - progress * 0.5)

            // Secondary explosions at intervals
            val secondary = if ((progress * 5).toInt() % 2 == 0 && progress < 0.6)
                sin(2.0 * PI * 120.0 * t) * 0.3 else 0.0

            samples[i] = ((boom1 * 0.3 + boom2 * 0.2 + noise * 0.3 + secondary) * envelope * 12000).toInt().toShort()
        }
        return samples
    }

    // Mechanical click-clack
    private fun generateWeaponSwitchSound(): ShortArray {
        val duration = 0.06
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val envelope = if (progress < 0.3) 1.0 else (1.0 - (progress - 0.3) / 0.7)
            // Two clicks
            val click1 = if (progress < 0.4) sin(2.0 * PI * 1500.0 * t) else 0.0
            val click2 = if (progress > 0.3) sin(2.0 * PI * 2000.0 * t) * 0.7 else 0.0

            samples[i] = ((click1 + click2) * envelope * 4000).toInt().toShort()
        }
        return samples
    }

    // Short whoosh — air push, not musical bounce
    private fun generateJumpSound(): ShortArray {
        val duration = 0.07
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val envelope = (1.0 - progress)
            // Noise burst with slight upward sweep
            val noise = (Math.random() - 0.5) * 0.7
            val sweep = sin(2.0 * PI * (400.0 + progress * 600.0) * t) * 0.3

            samples[i] = ((noise + sweep) * envelope * 3000).toInt().toShort()
        }
        return samples
    }

    // Military brass stab — short triumphant chord
    private fun generateVictorySound(): ShortArray {
        val duration = 0.9
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val envelope = if (progress < 0.05) progress * 20.0 else (1.0 - (progress - 0.05) * 0.6)

            // Power chord — root + fifth + octave
            val root = if (progress < 0.5) 220.0 else 293.66 // A3 then D4
            val wave = sin(2.0 * PI * root * t) * 0.4 +
                    sin(2.0 * PI * root * 1.5 * t) * 0.3 +
                    sin(2.0 * PI * root * 2.0 * t) * 0.2 +
                    sin(2.0 * PI * root * 3.0 * t) * 0.1

            samples[i] = (wave * envelope * 7000).toInt().toShort()
        }
        return samples
    }

    // Dark descending drone
    private fun generateGameOverSound(): ShortArray {
        val duration = 0.7
        val numSamples = (SAMPLE_RATE * duration).toInt()
        val samples = ShortArray(numSamples)

        for (i in 0 until numSamples) {
            val t = i.toDouble() / SAMPLE_RATE
            val progress = i.toDouble() / numSamples

            val freq = 250.0 * (1.0 - progress * 0.5)
            val fadeOut = 1.0 - progress * 0.7

            // Dark saw + sub bass
            val saw = ((t * freq) % 1.0) * 2.0 - 1.0
            val sub = sin(2.0 * PI * freq * 0.5 * t) * 0.5

            samples[i] = ((saw * 0.5 + sub) * fadeOut * 6000).toInt().toShort()
        }
        return samples
    }
}
