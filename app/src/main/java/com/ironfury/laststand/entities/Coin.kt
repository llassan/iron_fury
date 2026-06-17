package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.sin

class Coin(
    startPosition: Vector2,
    val value: Int = 10
) : Entity(
    position = startPosition.copy(),
    width = 16f,
    height = 16f
) {
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var animTime = 0f
    private var lifetime = 8f  // Coins disappear after 8 seconds
    private var bobOffset = (Math.random() * Math.PI * 2).toFloat()

    // Physics for bouncing when spawned
    private var bounceVelocityY = -200f
    private var hasLanded = false

    init {
        // Random horizontal spread when spawned
        velocity.x = (-50 + Math.random() * 100).toFloat()
        velocity.y = bounceVelocityY
    }

    override fun update(deltaTime: Float) {
        animTime += deltaTime
        lifetime -= deltaTime

        if (lifetime <= 0) {
            isActive = false
            return
        }

        // Apply gravity until landed
        if (!hasLanded) {
            velocity.y += Constants.GRAVITY * deltaTime
            super.update(deltaTime)

            // Ground check
            if (position.y + height >= Constants.GROUND_Y) {
                position.y = Constants.GROUND_Y - height
                velocity.y = 0f
                velocity.x = 0f
                hasLanded = true
            }
        }
    }

    override fun render(canvas: Canvas, cameraX: Float, scale: Float) {
        if (!isActive) return

        val screenX = (position.x - cameraX) * scale
        val screenY = position.y * scale

        // Don't render if off-screen
        if (screenX < -width * scale || screenX > canvas.width + width * scale) return

        val w = width * scale
        val h = height * scale

        // Bobbing animation
        val bob = sin(animTime * 5 + bobOffset) * 3f * scale

        // Flashing when about to disappear
        if (lifetime < 2f && (System.currentTimeMillis() / 100) % 2 == 0L) {
            return
        }

        // Spinning effect (scale X based on sin)
        val spinScale = 0.3f + kotlin.math.abs(sin(animTime * 8)) * 0.7f

        // Outer glow
        paint.color = Color.argb(100, 255, 215, 0)
        canvas.drawCircle(screenX + w / 2, screenY + h / 2 + bob, w * 0.8f, paint)

        // Main coin body (gold)
        paint.color = Color.rgb(255, 215, 0)
        canvas.drawOval(
            screenX + w * (0.5f - 0.4f * spinScale),
            screenY + h * 0.1f + bob,
            screenX + w * (0.5f + 0.4f * spinScale),
            screenY + h * 0.9f + bob,
            paint
        )

        // Highlight
        paint.color = Color.rgb(255, 245, 150)
        canvas.drawOval(
            screenX + w * (0.5f - 0.25f * spinScale),
            screenY + h * 0.2f + bob,
            screenX + w * (0.5f + 0.1f * spinScale),
            screenY + h * 0.5f + bob,
            paint
        )

        // Dollar sign or value indicator
        if (spinScale > 0.5f) {
            paint.color = Color.rgb(180, 140, 0)
            paint.textSize = h * 0.5f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("$", screenX + w / 2, screenY + h * 0.7f + bob, paint)
        }
    }
}
