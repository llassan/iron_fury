package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Level 2 boss — desert scorpion with aimed stinger, poison spray, tail whip, and burrow charge. */
class SandScorpion(
    startX: Float,
    bossName: String = "SAND SCORPION",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 2) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Aimed stinger shots
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    for (i in -1..1) {
                        val spread = i * 0.15f
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(dx / len + spread, dy / len), Constants.ENEMY_BULLET_SPEED * 1.2f, 1.5f))
                    }
                }
                attackCooldown = 1.2f
            }
            AttackPhase.PHASE2 -> {
                // Poison spray — wide arc of slow bullets
                for (i in -4..4) {
                    val angle = Math.toRadians(-90.0 + i * 12.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 130f, 1.2f))
                }
                attackCooldown = 1.8f
            }
            AttackPhase.PHASE3 -> {
                // Rapid tail whip — fast targeted shots
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    pendingBullets.add(BulletData(Vector2(cx, cy - 20f), Vector2(dx / len, dy / len), Constants.ENEMY_BULLET_SPEED * 2f, 1.8f))
                }
                attackCooldown = 0.3f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                    // Scatter poison on charge
                    for (i in 0..5) {
                        val angle = Math.toRadians(i * 60.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 100f, 1.5f))
                    }
                }
                attackCooldown = 0.6f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Body segments
        paint.color = if (flashing) Color.WHITE else Color.rgb(160, 120, 50)
        canvas.drawOval(x + w * 0.2f, y + h * 0.4f, x + w * 0.8f, y + h * 0.9f, paint)
        paint.color = if (flashing) Color.WHITE else Color.rgb(180, 140, 60)
        canvas.drawOval(x + w * 0.25f, y + h * 0.45f, x + w * 0.6f, y + h * 0.75f, paint)

        // Claws
        paint.color = if (flashing) Color.WHITE else Color.rgb(140, 100, 40)
        canvas.drawOval(x - w * 0.05f, y + h * 0.5f, x + w * 0.2f, y + h * 0.8f, paint)
        canvas.drawOval(x + w * 0.8f, y + h * 0.5f, x + w * 1.05f, y + h * 0.8f, paint)
        // Claw pincers
        paint.color = if (flashing) Color.WHITE else Color.rgb(120, 80, 30)
        canvas.drawOval(x - w * 0.1f, y + h * 0.55f, x + w * 0.05f, y + h * 0.7f, paint)
        canvas.drawOval(x + w * 0.95f, y + h * 0.55f, x + w * 1.1f, y + h * 0.7f, paint)

        // Tail (curved upward)
        paint.color = if (flashing) Color.WHITE else Color.rgb(150, 110, 45)
        val tailWave = sin(animTime * 3) * w * 0.05f
        canvas.drawRoundRect(x + w * 0.4f, y + h * 0.1f, x + w * 0.6f, y + h * 0.45f, 8f * scale, 8f * scale, paint)
        // Stinger
        paint.color = if (flashing) Color.WHITE else Color.rgb(200, 50, 50)
        val stingerPath = Path().apply {
            moveTo(x + w * 0.5f + tailWave, y)
            lineTo(x + w * 0.42f, y + h * 0.15f)
            lineTo(x + w * 0.58f, y + h * 0.15f)
            close()
        }
        canvas.drawPath(stingerPath, paint)

        // Eyes
        paint.color = Color.rgb(255, 200, 50)
        canvas.drawCircle(x + w * 0.38f, y + h * 0.52f, 5f * scale, paint)
        canvas.drawCircle(x + w * 0.55f, y + h * 0.52f, 5f * scale, paint)
        paint.color = Color.BLACK
        canvas.drawCircle(x + w * 0.39f, y + h * 0.52f, 2.5f * scale, paint)
        canvas.drawCircle(x + w * 0.56f, y + h * 0.52f, 2.5f * scale, paint)
    }
}
