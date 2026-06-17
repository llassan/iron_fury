package com.ironfury.laststand.effects

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.ironfury.laststand.utils.Vector2

class Explosion(
    val position: Vector2,
    private val maxRadius: Float = 60f
) {
    var isActive = true
        private set

    private var timer = 0f
    private val duration = 0.4f

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    // Particle positions
    private val particles = mutableListOf<Particle>()

    init {
        // Create explosion particles
        for (i in 0..12) {
            val angle = (i * 30f) * Math.PI / 180f
            val speed = 100f + (Math.random() * 150f).toFloat()
            particles.add(Particle(
                x = position.x,
                y = position.y,
                vx = (Math.cos(angle) * speed).toFloat(),
                vy = (Math.sin(angle) * speed).toFloat(),
                size = 4f + (Math.random() * 6f).toFloat()
            ))
        }
    }

    fun update(deltaTime: Float) {
        timer += deltaTime

        if (timer >= duration) {
            isActive = false
            return
        }

        // Update particles
        for (particle in particles) {
            particle.x += particle.vx * deltaTime
            particle.y += particle.vy * deltaTime
            particle.vy += 200f * deltaTime  // gravity
        }
    }

    fun render(canvas: Canvas, cameraX: Float, scale: Float) {
        if (!isActive) return

        val screenX = (position.x - cameraX) * scale
        val screenY = position.y * scale
        val progress = timer / duration

        // Main explosion flash (fades out)
        val flashAlpha = ((1f - progress) * 255).toInt().coerceIn(0, 255)
        val currentRadius = maxRadius * scale * (0.3f + progress * 0.7f)

        // Outer glow (orange)
        paint.color = Color.argb(flashAlpha / 2, 255, 100, 0)
        canvas.drawCircle(screenX, screenY, currentRadius * 1.3f, paint)

        // Main explosion (yellow/white)
        paint.color = Color.argb(flashAlpha, 255, 200, 50)
        canvas.drawCircle(screenX, screenY, currentRadius, paint)

        // Core (white)
        paint.color = Color.argb(flashAlpha, 255, 255, 255)
        canvas.drawCircle(screenX, screenY, currentRadius * 0.5f, paint)

        // Particles
        val particleAlpha = ((1f - progress * 0.8f) * 255).toInt().coerceIn(0, 255)
        for (particle in particles) {
            val px = (particle.x - cameraX) * scale
            val py = particle.y * scale
            val pSize = particle.size * scale * (1f - progress * 0.5f)

            // Smoke/debris particles
            paint.color = Color.argb(particleAlpha, 100, 100, 100)
            canvas.drawCircle(px, py, pSize, paint)

            // Fire core
            paint.color = Color.argb(particleAlpha, 255, 150, 50)
            canvas.drawCircle(px, py, pSize * 0.5f, paint)
        }
    }

    private data class Particle(
        var x: Float,
        var y: Float,
        var vx: Float,
        var vy: Float,
        var size: Float
    )
}
