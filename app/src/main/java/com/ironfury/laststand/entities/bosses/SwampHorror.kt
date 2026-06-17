package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Level 8 boss — swamp blob with acid spit, toxic cloud, vine grab, consume burst. */
class SwampHorror(
    startX: Float,
    bossName: String = "SWAMP HORROR",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 8) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Acid spit — arced aimed shots
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    for (i in -2..2) {
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(dx / len + i * 0.1f, dy / len - 0.2f), 170f, 1.8f))
                    }
                }
                attackCooldown = 1.1f
            }
            AttackPhase.PHASE2 -> {
                // Toxic cloud — slow expanding mass
                for (i in 0..9) {
                    val angle = Math.toRadians(i * 36.0 + Math.random() * 15)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 80f + (Math.random() * 40).toFloat(), 2.2f))
                }
                attackCooldown = 1.5f
            }
            AttackPhase.PHASE3 -> {
                // Vine grab — fast targeted + ring
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(dx / len, dy / len), 250f, 2f))
                }
                for (i in 0..5) {
                    val angle = Math.toRadians(i * 60.0 + specialTimer * 50)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 140f, 1.5f))
                }
                attackCooldown = 0.5f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                }
                // Consume — massive bullet cloud
                for (i in 0..4) {
                    val angle = Math.toRadians(Math.random() * 360.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 100f + (Math.random() * 80).toFloat(), 2.5f))
                }
                attackCooldown = 0.2f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Pulsating blob body
        val pulse = sin(animTime * 3) * w * 0.03f

        // Base slime
        paint.color = if (flashing) Color.WHITE else Color.rgb(40, 70, 30)
        canvas.drawOval(x + w * 0.05f - pulse, y + h * 0.3f, x + w * 0.95f + pulse, y + h * 0.95f, paint)

        // Upper mass
        paint.color = if (flashing) Color.WHITE else Color.rgb(50, 85, 35)
        canvas.drawOval(x + w * 0.15f + pulse, y + h * 0.15f, x + w * 0.85f - pulse, y + h * 0.7f, paint)

        // Dripping tendrils
        paint.color = if (flashing) Color.WHITE else Color.rgb(35, 60, 25)
        for (i in 0..4) {
            val dripX = x + w * (0.15f + i * 0.17f)
            val dripLen = h * (0.15f + sin(animTime * 2 + i * 1.5f) * 0.08f)
            canvas.drawOval(dripX - 4f * scale, y + h * 0.9f, dripX + 4f * scale, y + h * 0.9f + dripLen, paint)
        }

        // Multiple eyes (creepy)
        val eyePositions = floatArrayOf(0.3f, 0.35f, 0.5f, 0.3f, 0.65f, 0.38f, 0.42f, 0.5f, 0.58f, 0.45f)
        for (i in 0 until eyePositions.size / 2) {
            val ex = x + w * eyePositions[i * 2]
            val ey = y + h * eyePositions[i * 2 + 1]
            val eyeSize = (3f + (i % 3)) * scale
            paint.color = Color.rgb(200, 255, 100)
            canvas.drawCircle(ex, ey, eyeSize, paint)
            paint.color = Color.BLACK
            canvas.drawCircle(ex + 1f * scale, ey, eyeSize * 0.5f, paint)
        }

        // Mouth
        paint.color = Color.rgb(80, 20, 40)
        canvas.drawOval(x + w * 0.35f, y + h * 0.55f, x + w * 0.65f, y + h * 0.7f, paint)
        // Teeth
        paint.color = Color.rgb(200, 200, 150)
        for (i in 0..3) {
            val tx = x + w * (0.38f + i * 0.07f)
            canvas.drawRect(tx, y + h * 0.55f, tx + 4f * scale, y + h * 0.6f, paint)
        }
    }
}
