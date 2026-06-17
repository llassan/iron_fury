package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Level 3 boss — frozen golem with ice shard rain, freeze breath, crystal storm, avalanche. */
class IceTitan(
    startX: Float,
    bossName: String = "ICE TITAN",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 3) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Ice shard rain from above
                for (i in 0..4) {
                    val x = cx - 80f + i * 40f
                    pendingBullets.add(BulletData(Vector2(x, cy - 40f), Vector2((Math.random() * 0.4 - 0.2).toFloat(), 1f), 150f, 1.3f))
                }
                attackCooldown = 1.4f
            }
            AttackPhase.PHASE2 -> {
                // Freeze breath — cone attack
                for (i in -3..3) {
                    val angle = Math.toRadians(-90.0 + i * 10.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 180f + abs(i) * 20f))
                }
                attackCooldown = 1.6f
            }
            AttackPhase.PHASE3 -> {
                // Crystal storm — spiral pattern
                for (i in 0..5) {
                    val angle = Math.toRadians(specialTimer * 120.0 + i * 60.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 160f, 1.5f))
                }
                attackCooldown = 0.4f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                    // Avalanche burst
                    for (i in 0..11) {
                        val angle = Math.toRadians(i * 30.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 120f, 1.8f))
                    }
                }
                attackCooldown = 0.8f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Icy body
        paint.color = if (flashing) Color.WHITE else Color.rgb(140, 180, 220)
        canvas.drawRoundRect(x + w * 0.15f, y + h * 0.25f, x + w * 0.85f, y + h * 0.9f, 12f * scale, 12f * scale, paint)

        // Ice crystal shoulders
        paint.color = if (flashing) Color.WHITE else Color.rgb(180, 210, 245)
        val leftShoulder = Path().apply {
            moveTo(x + w * 0.05f, y + h * 0.5f)
            lineTo(x + w * 0.2f, y + h * 0.2f)
            lineTo(x + w * 0.3f, y + h * 0.5f)
            close()
        }
        canvas.drawPath(leftShoulder, paint)
        val rightShoulder = Path().apply {
            moveTo(x + w * 0.7f, y + h * 0.5f)
            lineTo(x + w * 0.8f, y + h * 0.2f)
            lineTo(x + w * 0.95f, y + h * 0.5f)
            close()
        }
        canvas.drawPath(rightShoulder, paint)

        // Crystal crown
        paint.color = if (flashing) Color.WHITE else Color.rgb(200, 230, 255)
        for (i in 0..4) {
            val cxLocal = x + w * (0.3f + i * 0.1f)
            val crownH = h * (0.12f + (i % 2) * 0.08f)
            val crownPath = Path().apply {
                moveTo(cxLocal, y + h * 0.25f)
                lineTo(cxLocal - w * 0.03f, y + h * 0.25f - crownH)
                lineTo(cxLocal + w * 0.03f, y + h * 0.25f - crownH)
                close()
            }
            canvas.drawPath(crownPath, paint)
        }

        // Glowing eyes
        val eg = (sin(animTime * 4) * 0.3f + 0.7f)
        paint.color = Color.rgb(100, (200 * eg).toInt(), (255 * eg).toInt())
        canvas.drawCircle(x + w * 0.38f, y + h * 0.4f, 6f * scale, paint)
        canvas.drawCircle(x + w * 0.62f, y + h * 0.4f, 6f * scale, paint)

        // Ice legs
        paint.color = if (flashing) Color.WHITE else Color.rgb(150, 190, 225)
        canvas.drawRect(x + w * 0.25f, y + h * 0.85f, x + w * 0.4f, y + h, paint)
        canvas.drawRect(x + w * 0.6f, y + h * 0.85f, x + w * 0.75f, y + h, paint)
    }
}
