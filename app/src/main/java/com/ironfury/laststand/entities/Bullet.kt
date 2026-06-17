package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Direction
import com.ironfury.laststand.utils.Vector2
import com.ironfury.laststand.weapons.WeaponType
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Bullet(
    startPosition: Vector2,
    direction: Direction,
    val weaponType: WeaponType = WeaponType.MACHINE_GUN,
    private val spreadOffset: Float = 0f,  // angle offset for spread weapons
    val isPlayerBullet: Boolean = true,
    private val customSpeed: Float? = null,  // Override weapon speed (for boss bullets)
    private val sizeMultiplier: Float = 1f   // Scale bullet size (for boss bullets)
) : Entity(
    position = startPosition.copy(),
    width = weaponType.bulletWidth * sizeMultiplier,
    height = weaponType.bulletHeight * sizeMultiplier
) {
    private val paint = Paint().apply {
        color = weaponType.bulletColor
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private var lifetime = 3f
    private var angle: Float = 0f

    // Flame specific
    private var flameSize = 1f
    private var flameAlpha = 255

    // Rocket trail
    private val trailPositions = mutableListOf<Vector2>()

    // For Direction-based bullets (player/enemies)
    private var directionVector: Vector2? = null

    init {
        val dir = directionVector ?: Vector2.direction(direction)

        // Apply spread offset
        if (spreadOffset != 0f) {
            val radians = Math.toRadians(spreadOffset.toDouble())
            val newX = dir.x * cos(radians) - dir.y * sin(radians)
            val newY = dir.x * sin(radians) + dir.y * cos(radians)
            dir.x = newX.toFloat()
            dir.y = newY.toFloat()
        }

        val speed = customSpeed ?: weaponType.bulletSpeed
        velocity.set(dir.x * speed, dir.y * speed)
        angle = Math.toDegrees(atan2(dir.y.toDouble(), dir.x.toDouble())).toFloat()

        // Cap player-bullet range so the player can't snipe across the whole
        // screen and rush through levels. Per-weapon to keep weapon feel distinct.
        if (isPlayerBullet) {
            val playerRange = when (weaponType) {
                WeaponType.MACHINE_GUN -> 500f
                WeaponType.SPREAD_GUN -> 380f
                WeaponType.LASER -> 620f
                WeaponType.ROCKET_LAUNCHER -> 600f
                WeaponType.FLAMETHROWER -> 320f
            }
            lifetime = playerRange / speed
        }

        // Flame particles have a short, bounded life so they don't grow forever.
        if (weaponType == WeaponType.FLAMETHROWER) {
            // Longer, more powerful flame stream per design — overrides the
            // range cap above intentionally so the flame reaches far enemies.
            lifetime = 1.0f
        }
    }

    // Secondary constructor for Vector2 direction (boss bullets)
    constructor(
        startPosition: Vector2,
        dirVector: Vector2,
        weaponType: WeaponType,
        spreadOffset: Float,
        isPlayerBullet: Boolean,
        customSpeed: Float?,
        sizeMultiplier: Float
    ) : this(startPosition, Direction.RIGHT, weaponType, spreadOffset, isPlayerBullet, customSpeed, sizeMultiplier) {
        // Override velocity with the provided vector direction
        val speed = customSpeed ?: weaponType.bulletSpeed
        velocity.set(dirVector.x * speed, dirVector.y * speed)
        angle = Math.toDegrees(atan2(dirVector.y.toDouble(), dirVector.x.toDouble())).toFloat()
    }

    override fun update(deltaTime: Float) {
        // Save trail for rockets
        if (weaponType == WeaponType.ROCKET_LAUNCHER) {
            trailPositions.add(0, position.copy())
            if (trailPositions.size > 8) {
                trailPositions.removeAt(trailPositions.size - 1)
            }
        }

        super.update(deltaTime)

        lifetime -= deltaTime

        // Flame grows briefly then fades. Growth capped so a stream doesn't paint the screen.
        if (weaponType == WeaponType.FLAMETHROWER) {
            flameSize = (flameSize + deltaTime * 2f).coerceAtMost(2.0f)
            flameAlpha = (255 * (lifetime / 1.0f)).toInt().coerceIn(0, 255)
            if (lifetime <= 0 || flameAlpha <= 0) {
                isActive = false
            }
        }

        if (lifetime <= 0) {
            isActive = false
        }

        // Deactivate if off-screen vertically
        if (position.y < -100 || position.y > Constants.GAME_HEIGHT + 100) {
            isActive = false
        }
    }

    override fun render(canvas: Canvas, cameraX: Float, scale: Float) {
        if (!isActive) return

        val screenX = (position.x - cameraX) * scale
        val screenY = position.y * scale

        // Don't render if off-screen
        if (screenX < -50 || screenX > canvas.width + 50) return

        // Enemy & boss projectiles are always red so the player can instantly tell
        // incoming fire from their own shots, regardless of the firing weapon type.
        if (!isPlayerBullet) {
            renderEnemyBullet(canvas, screenX, screenY, scale)
            return
        }

        when (weaponType) {
            WeaponType.MACHINE_GUN -> renderMachineGunBullet(canvas, screenX, screenY, scale)
            WeaponType.SPREAD_GUN -> renderSpreadBullet(canvas, screenX, screenY, scale)
            WeaponType.LASER -> renderLaserBullet(canvas, screenX, screenY, scale)
            WeaponType.ROCKET_LAUNCHER -> renderRocket(canvas, screenX, screenY, scale, cameraX)
            WeaponType.FLAMETHROWER -> renderFlame(canvas, screenX, screenY, scale)
        }
    }

    private fun renderEnemyBullet(canvas: Canvas, screenX: Float, screenY: Float, scale: Float) {
        val w = width * scale
        val h = height * scale
        val r = maxOf(w, h) / 2f

        canvas.save()
        canvas.rotate(angle, screenX, screenY)

        // Soft red glow halo
        paint.color = Color.argb(110, 255, 40, 40)
        canvas.drawCircle(screenX, screenY, r + 3f, paint)

        // Red bullet body
        paint.color = Color.rgb(225, 30, 30)
        canvas.drawOval(screenX - w / 2, screenY - h / 2, screenX + w / 2, screenY + h / 2, paint)

        // Hot pink-white core
        paint.color = Color.rgb(255, 160, 160)
        canvas.drawOval(screenX - w / 4, screenY - h / 3, screenX + w / 4, screenY + h / 3, paint)

        canvas.restore()
    }

    private fun renderMachineGunBullet(canvas: Canvas, screenX: Float, screenY: Float, scale: Float) {
        val w = width * scale
        val h = height * scale

        canvas.save()
        canvas.rotate(angle, screenX, screenY)

        // Bullet core
        paint.color = Color.YELLOW
        canvas.drawOval(screenX - w / 2, screenY - h / 2, screenX + w / 2, screenY + h / 2, paint)

        // Bright center
        paint.color = Color.WHITE
        canvas.drawOval(screenX - w / 3, screenY - h / 3, screenX + w / 4, screenY + h / 3, paint)

        canvas.restore()
    }

    private fun renderSpreadBullet(canvas: Canvas, screenX: Float, screenY: Float, scale: Float) {
        val w = width * scale
        val h = height * scale

        canvas.save()
        canvas.rotate(angle, screenX, screenY)

        // Orange bullet
        paint.color = Color.rgb(255, 150, 50)
        canvas.drawOval(screenX - w / 2, screenY - h / 2, screenX + w / 2, screenY + h / 2, paint)

        // Yellow center
        paint.color = Color.YELLOW
        canvas.drawCircle(screenX, screenY, h / 2, paint)

        canvas.restore()
    }

    private fun renderLaserBullet(canvas: Canvas, screenX: Float, screenY: Float, scale: Float) {
        val w = width * scale
        val h = height * scale

        canvas.save()
        canvas.rotate(angle, screenX, screenY)

        // Outer glow
        paint.color = Color.argb(100, 0, 255, 255)
        canvas.drawOval(screenX - w / 2 - 4, screenY - h - 2, screenX + w / 2 + 4, screenY + h + 2, paint)

        // Laser beam
        paint.color = Color.CYAN
        canvas.drawRect(screenX - w / 2, screenY - h / 2, screenX + w / 2, screenY + h / 2, paint)

        // Bright core
        paint.color = Color.WHITE
        canvas.drawRect(screenX - w / 2 + 2, screenY - h / 4, screenX + w / 2 - 2, screenY + h / 4, paint)

        canvas.restore()
    }

    private fun renderRocket(canvas: Canvas, screenX: Float, screenY: Float, scale: Float, cameraX: Float) {
        // Draw smoke trail
        for ((index, pos) in trailPositions.withIndex()) {
            val trailX = (pos.x - cameraX) * scale
            val trailY = pos.y * scale
            val alpha = 150 - index * 18
            val size = (8 - index) * scale

            if (alpha > 0) {
                paint.color = Color.argb(alpha, 150, 150, 150)
                canvas.drawCircle(trailX, trailY, size, paint)
            }
        }

        val w = width * scale
        val h = height * scale

        canvas.save()
        canvas.rotate(angle, screenX, screenY)

        // Rocket body
        paint.color = Color.rgb(80, 80, 90)
        canvas.drawRoundRect(screenX - w / 2, screenY - h / 2, screenX + w / 2, screenY + h / 2,
                            h / 2, h / 2, paint)

        // Rocket tip (red)
        paint.color = Color.rgb(200, 50, 50)
        val tipPath = Path().apply {
            moveTo(screenX + w / 2, screenY)
            lineTo(screenX + w / 2 + w / 4, screenY - h / 3)
            lineTo(screenX + w / 2 + w / 4, screenY + h / 3)
            close()
        }
        canvas.drawPath(tipPath, paint)

        // Fins
        paint.color = Color.rgb(60, 60, 70)
        canvas.drawRect(screenX - w / 2, screenY - h, screenX - w / 3, screenY - h / 2, paint)
        canvas.drawRect(screenX - w / 2, screenY + h / 2, screenX - w / 3, screenY + h, paint)

        // Flame exhaust
        paint.color = Color.rgb(255, 200, 50)
        canvas.drawOval(screenX - w / 2 - w / 3, screenY - h / 3, screenX - w / 2, screenY + h / 3, paint)
        paint.color = Color.rgb(255, 100, 0)
        canvas.drawOval(screenX - w / 2 - w / 2, screenY - h / 4, screenX - w / 2 - w / 6, screenY + h / 4, paint)

        canvas.restore()
    }

    private fun renderFlame(canvas: Canvas, screenX: Float, screenY: Float, scale: Float) {
        val size = width * scale * flameSize

        // Outer flame (orange/red)
        paint.color = Color.argb(flameAlpha, 255, 100, 0)
        canvas.drawCircle(screenX, screenY, size, paint)

        // Inner flame (yellow)
        paint.color = Color.argb(flameAlpha, 255, 200, 50)
        canvas.drawCircle(screenX, screenY, size * 0.6f, paint)

        // Core (white/yellow)
        paint.color = Color.argb(flameAlpha, 255, 255, 200)
        canvas.drawCircle(screenX, screenY, size * 0.3f, paint)
    }

    fun isOffScreen(cameraX: Float): Boolean {
        return position.x < cameraX - 100 || position.x > cameraX + Constants.GAME_WIDTH + 100
    }
}
