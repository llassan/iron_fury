package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Direction
import com.ironfury.laststand.utils.Vector2
import kotlin.math.sin

class Enemy(
    startX: Float,
    private val patrolLeft: Float,
    private val patrolRight: Float,
    surfaceY: Float = Constants.GROUND_Y
) : Entity(
    position = Vector2(startX, surfaceY - Constants.ENEMY_HEIGHT),
    width = Constants.ENEMY_WIDTH,
    height = Constants.ENEMY_HEIGHT
) {
    private var movingRight = true
    private var fireTimer = 0.3f + Math.random().toFloat() * 0.3f  // Start shooting much sooner (0.3-0.6s)
    var wantsToFire = false
        private set
    var aimDirection = Direction.LEFT
        private set
    // Continuous-angle aim (with lead). Used for bullet velocity & spawn offset.
    // aimDirection above is kept as the 8-way snap for sprite flipping only.
    var aimVector: Vector2 = Vector2(-1f, 0f)
        private set
    var muzzleFlash = false
        private set

    private var animTime = 0f

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val outlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        isAntiAlias = true
    }

    // Colors - Military enemy
    private val skinColor = Color.rgb(220, 180, 140)
    private val uniformColor = Color.rgb(80, 90, 70)      // Olive drab
    private val uniformDark = Color.rgb(60, 70, 50)
    private val helmetColor = Color.rgb(70, 80, 60)
    private val helmetHighlight = Color.rgb(100, 110, 90)
    private val bootsColor = Color.rgb(40, 35, 30)
    private val beltColor = Color.rgb(50, 40, 30)
    private val gunMetal = Color.rgb(50, 50, 55)

    override fun update(deltaTime: Float) {
        // Patrol movement
        velocity.x = if (movingRight) Constants.ENEMY_SPEED else -Constants.ENEMY_SPEED

        super.update(deltaTime)

        // Reverse at patrol boundaries
        if (position.x <= patrolLeft) {
            position.x = patrolLeft
            movingRight = true
        } else if (position.x + width >= patrolRight) {
            position.x = patrolRight - width
            movingRight = false
        }

        // Fire timer
        fireTimer -= deltaTime
        wantsToFire = fireTimer <= 0
        if (wantsToFire) {
            fireTimer = Constants.ENEMY_FIRE_RATE
            muzzleFlash = true
        } else {
            muzzleFlash = false
        }

        animTime += deltaTime
    }

    fun updateAimDirection(
        playerX: Float,
        playerY: Float,
        playerVx: Float = 0f,
        playerVy: Float = 0f
    ) {
        // Lead the shot: predict where the player will be when the bullet arrives.
        // One Newton-style iteration is plenty for our speeds.
        val sx = centerX
        val sy = centerY
        val bulletSpeed = Constants.ENEMY_BULLET_SPEED
        // Lead horizontally only — vertical motion is gravity-dominated, so a
        // constant-velocity prediction would point at empty sky during jumps.
        // Cap prediction time so long-range shots don't overshoot the screen.
        var ax = playerX
        val ay = playerY
        repeat(2) {
            val dist = kotlin.math.sqrt((ax - sx) * (ax - sx) + (ay - sy) * (ay - sy))
            val t = (dist / bulletSpeed).coerceAtMost(0.6f)
            ax = playerX + playerVx * t
        }

        val dx = ax - sx
        val dy = ay - sy
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len > 0.0001f) {
            aimVector = Vector2(dx / len, dy / len)
        }

        // 8-way snap for sprite flipping
        val isRight = dx > 0
        val isUp = dy < -20
        val isDown = dy > 20
        aimDirection = when {
            isRight && isUp -> Direction.UP_RIGHT
            isRight && isDown -> Direction.DOWN_RIGHT
            isRight -> Direction.RIGHT
            !isRight && isUp -> Direction.UP_LEFT
            !isRight && isDown -> Direction.DOWN_LEFT
            else -> Direction.LEFT
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

        canvas.save()

        // Flip based on aim direction
        val facingRight = aimDirection == Direction.RIGHT ||
                         aimDirection == Direction.UP_RIGHT ||
                         aimDirection == Direction.DOWN_RIGHT
        if (!facingRight) {
            canvas.scale(-1f, 1f, screenX + w / 2, screenY + h / 2)
        }

        // Animation
        val legOffset = sin(animTime * 10) * h * 0.06f
        val bodyBob = sin(animTime * 20) * h * 0.015f

        // === LEGS ===
        paint.color = uniformDark
        // Back leg
        canvas.drawRoundRect(
            screenX + w * 0.30f,
            screenY + h * 0.60f - legOffset,
            screenX + w * 0.48f,
            screenY + h * 0.88f - legOffset,
            3f * scale, 3f * scale, paint
        )
        // Front leg
        paint.color = uniformColor
        canvas.drawRoundRect(
            screenX + w * 0.52f,
            screenY + h * 0.60f + legOffset,
            screenX + w * 0.70f,
            screenY + h * 0.88f + legOffset,
            3f * scale, 3f * scale, paint
        )

        // Boots
        paint.color = bootsColor
        canvas.drawRoundRect(
            screenX + w * 0.28f,
            screenY + h * 0.85f - legOffset,
            screenX + w * 0.50f,
            screenY + h * 0.98f - legOffset,
            2f * scale, 2f * scale, paint
        )
        canvas.drawRoundRect(
            screenX + w * 0.50f,
            screenY + h * 0.85f + legOffset,
            screenX + w * 0.72f,
            screenY + h * 0.98f + legOffset,
            2f * scale, 2f * scale, paint
        )

        // === TORSO ===
        paint.color = uniformColor
        canvas.drawRoundRect(
            screenX + w * 0.22f,
            screenY + h * 0.30f + bodyBob,
            screenX + w * 0.78f,
            screenY + h * 0.65f + bodyBob,
            5f * scale, 5f * scale, paint
        )

        // Belt
        paint.color = beltColor
        canvas.drawRect(
            screenX + w * 0.20f,
            screenY + h * 0.55f + bodyBob,
            screenX + w * 0.80f,
            screenY + h * 0.62f + bodyBob,
            paint
        )
        // Belt buckle
        paint.color = Color.rgb(180, 150, 50)
        canvas.drawRect(
            screenX + w * 0.45f,
            screenY + h * 0.56f + bodyBob,
            screenX + w * 0.55f,
            screenY + h * 0.61f + bodyBob,
            paint
        )

        // === ARM WITH GUN ===
        val armDir = Vector2.direction(aimDirection)
        val shoulderX = screenX + w * 0.60f
        val shoulderY = screenY + h * 0.40f + bodyBob

        // Arm
        paint.color = uniformColor
        outlinePaint.strokeWidth = 7f * scale
        outlinePaint.color = uniformColor
        val handX = shoulderX + armDir.x * w * 0.35f
        val handY = shoulderY + armDir.y * w * 0.35f
        canvas.drawLine(shoulderX, shoulderY, handX, handY, outlinePaint)

        // Gun
        paint.color = gunMetal
        val gunLen = w * 0.4f
        val gunEndX = handX + armDir.x * gunLen
        val gunEndY = handY + armDir.y * gunLen
        outlinePaint.strokeWidth = 8f * scale
        outlinePaint.color = gunMetal
        canvas.drawLine(handX, handY, gunEndX, gunEndY, outlinePaint)

        // Muzzle flash
        if (muzzleFlash) {
            paint.color = Color.rgb(255, 200, 100)
            val flashSize = w * 0.15f
            canvas.drawCircle(gunEndX + armDir.x * flashSize * 0.5f,
                            gunEndY + armDir.y * flashSize * 0.5f, flashSize, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(gunEndX + armDir.x * flashSize * 0.3f,
                            gunEndY + armDir.y * flashSize * 0.3f, flashSize * 0.4f, paint)
        }

        // === HEAD ===
        // Neck
        paint.color = skinColor
        canvas.drawRect(
            screenX + w * 0.42f,
            screenY + h * 0.24f + bodyBob,
            screenX + w * 0.58f,
            screenY + h * 0.34f + bodyBob,
            paint
        )

        // Head
        paint.color = skinColor
        canvas.drawOval(
            screenX + w * 0.25f,
            screenY + h * 0.05f + bodyBob,
            screenX + w * 0.75f,
            screenY + h * 0.30f + bodyBob,
            paint
        )

        // Helmet
        paint.color = helmetColor
        val helmetPath = Path().apply {
            moveTo(screenX + w * 0.20f, screenY + h * 0.18f + bodyBob)
            quadTo(screenX + w * 0.25f, screenY - h * 0.02f + bodyBob,
                   screenX + w * 0.50f, screenY - h * 0.02f + bodyBob)
            quadTo(screenX + w * 0.75f, screenY - h * 0.02f + bodyBob,
                   screenX + w * 0.80f, screenY + h * 0.18f + bodyBob)
            lineTo(screenX + w * 0.78f, screenY + h * 0.14f + bodyBob)
            quadTo(screenX + w * 0.50f, screenY + h * 0.08f + bodyBob,
                   screenX + w * 0.22f, screenY + h * 0.14f + bodyBob)
            close()
        }
        canvas.drawPath(helmetPath, paint)

        // Helmet highlight
        paint.color = helmetHighlight
        canvas.drawArc(
            screenX + w * 0.30f,
            screenY + h * 0.02f + bodyBob,
            screenX + w * 0.60f,
            screenY + h * 0.12f + bodyBob,
            200f, 80f, true, paint
        )

        // Eye (menacing)
        paint.color = Color.WHITE
        canvas.drawOval(
            screenX + w * 0.50f,
            screenY + h * 0.14f + bodyBob,
            screenX + w * 0.65f,
            screenY + h * 0.22f + bodyBob,
            paint
        )
        paint.color = Color.BLACK
        canvas.drawCircle(
            screenX + w * 0.58f,
            screenY + h * 0.18f + bodyBob,
            2f * scale,
            paint
        )

        // Angry eyebrow
        paint.color = Color.rgb(60, 50, 40)
        canvas.drawLine(
            screenX + w * 0.48f,
            screenY + h * 0.11f + bodyBob,
            screenX + w * 0.66f,
            screenY + h * 0.14f + bodyBob,
            outlinePaint.apply { strokeWidth = 3f * scale; color = Color.rgb(60, 50, 40) }
        )

        canvas.restore()
    }

    fun getBulletSpawnPosition(): Vector2 {
        // Use continuous aim vector so muzzle aligns with the actual shot trajectory.
        return Vector2(
            centerX + aimVector.x * width * 0.5f,
            centerY + aimVector.y * height * 0.2f
        )
    }
}
