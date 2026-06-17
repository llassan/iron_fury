package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import com.ironfury.laststand.weapons.WeaponType
import kotlin.math.abs
import kotlin.math.sin

class PowerUp(
    startPosition: Vector2,
    val weaponType: WeaponType
) : Entity(
    position = startPosition.copy(),
    width = 22f,
    height = 22f
) {
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var animTime = 0f
    private var lifetime = 10f
    private var bobOffset = (Math.random() * Math.PI * 2).toFloat()
    private var hasLanded = false

    init {
        velocity.x = (-30 + Math.random() * 60).toFloat()
        velocity.y = -250f
    }

    override fun update(deltaTime: Float) {
        animTime += deltaTime
        lifetime -= deltaTime

        if (lifetime <= 0) {
            isActive = false
            return
        }

        if (!hasLanded) {
            velocity.y += Constants.GRAVITY * deltaTime
            super.update(deltaTime)

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

        if (screenX < -width * scale || screenX > canvas.width + width * scale) return

        val w = width * scale
        val h = height * scale
        val cx = screenX + w / 2
        val cy = screenY + h / 2

        val bob = sin(animTime * 4 + bobOffset) * 4f * scale

        // Flashing when about to disappear
        if (lifetime < 3f && (System.currentTimeMillis() / 120) % 2 == 0L) return

        // Pulsing glow
        val pulse = (sin(animTime * 6) * 0.3f + 0.7f)
        val glowColor = getWeaponGlowColor()
        val r = Color.red(glowColor)
        val g = Color.green(glowColor)
        val b = Color.blue(glowColor)

        // Outer glow ring
        paint.color = Color.argb((80 * pulse).toInt(), r, g, b)
        canvas.drawCircle(cx, cy + bob, w * 1.0f, paint)

        // Background capsule
        paint.color = Color.argb(200, 20, 20, 30)
        canvas.drawRoundRect(
            screenX + w * 0.05f, screenY + h * 0.05f + bob,
            screenX + w * 0.95f, screenY + h * 0.95f + bob,
            8f * scale, 8f * scale, paint
        )

        // Border with weapon color
        paint.color = Color.argb((200 * pulse).toInt(), r, g, b)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f * scale
        canvas.drawRoundRect(
            screenX + w * 0.05f, screenY + h * 0.05f + bob,
            screenX + w * 0.95f, screenY + h * 0.95f + bob,
            8f * scale, 8f * scale, paint
        )
        paint.style = Paint.Style.FILL

        // Weapon icon inside
        renderWeaponIcon(canvas, cx, cy + bob, w * 0.35f, scale)

        // Letter indicator
        paint.color = Color.WHITE
        paint.textSize = h * 0.28f
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(weaponType.iconChar, cx, cy + bob + h * 0.38f, paint)
    }

    private fun renderWeaponIcon(canvas: Canvas, cx: Float, cy: Float, size: Float, scale: Float) {
        paint.color = getWeaponGlowColor()

        when (weaponType) {
            WeaponType.SPREAD_GUN -> {
                // Three spread lines
                paint.strokeWidth = 2f * scale
                paint.style = Paint.Style.STROKE
                canvas.drawLine(cx - size, cy, cx + size, cy - size * 0.6f, paint)
                canvas.drawLine(cx - size, cy, cx + size, cy, paint)
                canvas.drawLine(cx - size, cy, cx + size, cy + size * 0.6f, paint)
                paint.style = Paint.Style.FILL
            }
            WeaponType.LASER -> {
                paint.color = Color.CYAN
                canvas.drawRect(cx - size, cy - size * 0.15f, cx + size, cy + size * 0.15f, paint)
                paint.color = Color.WHITE
                canvas.drawRect(cx - size * 0.7f, cy - size * 0.06f, cx + size * 0.7f, cy + size * 0.06f, paint)
            }
            WeaponType.ROCKET_LAUNCHER -> {
                canvas.drawOval(cx - size, cy - size * 0.35f, cx + size * 0.4f, cy + size * 0.35f, paint)
                paint.color = Color.RED
                val tip = Path().apply {
                    moveTo(cx + size * 0.3f, cy - size * 0.25f)
                    lineTo(cx + size, cy)
                    lineTo(cx + size * 0.3f, cy + size * 0.25f)
                    close()
                }
                canvas.drawPath(tip, paint)
            }
            WeaponType.FLAMETHROWER -> {
                paint.color = Color.rgb(255, 100, 0)
                canvas.drawCircle(cx, cy, size * 0.6f, paint)
                paint.color = Color.rgb(255, 200, 50)
                canvas.drawCircle(cx - size * 0.1f, cy, size * 0.35f, paint)
                paint.color = Color.rgb(255, 255, 200)
                canvas.drawCircle(cx - size * 0.15f, cy, size * 0.15f, paint)
            }
            else -> {
                // Machine gun (shouldn't drop, but just in case)
                canvas.drawRect(cx - size, cy - size * 0.2f, cx + size, cy + size * 0.2f, paint)
            }
        }
    }

    private fun getWeaponGlowColor(): Int = when (weaponType) {
        WeaponType.MACHINE_GUN -> Color.YELLOW
        WeaponType.SPREAD_GUN -> Color.rgb(255, 150, 50)
        WeaponType.LASER -> Color.CYAN
        WeaponType.ROCKET_LAUNCHER -> Color.rgb(255, 80, 80)
        WeaponType.FLAMETHROWER -> Color.rgb(255, 120, 0)
    }

    companion object {
        // Weapons that can drop as power-ups (not machine gun — that's the default)
        val DROPPABLE_WEAPONS = listOf(
            WeaponType.SPREAD_GUN,
            WeaponType.LASER,
            WeaponType.ROCKET_LAUNCHER,
            WeaponType.FLAMETHROWER
        )

        fun randomWeapon(): WeaponType = DROPPABLE_WEAPONS.random()

        /** Drop chance: 15% base */
        const val DROP_CHANCE = 0.15f
    }
}
