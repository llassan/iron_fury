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

/** Level 6 boss — deep-sea kraken with tentacle slam, ink spray, whirlpool, tidal wave. */
class Kraken(
    startX: Float,
    bossName: String = "KRAKEN",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 6) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Tentacle slam — 3 aimed heavy shots
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    for (i in -1..1) {
                        pendingBullets.add(BulletData(Vector2(cx + i * 30f, cy + 20f), Vector2(dx / len, dy / len), 160f, 2.5f))
                    }
                }
                attackCooldown = 1.4f
            }
            AttackPhase.PHASE2 -> {
                // Ink spray — chaotic spread
                for (i in 0..7) {
                    val angle = Math.toRadians(-180.0 + Math.random() * 180.0)
                    val speed = 120f + (Math.random() * 100).toFloat()
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), speed, 1.5f))
                }
                attackCooldown = 1.0f
            }
            AttackPhase.PHASE3 -> {
                // Whirlpool — double spiral
                for (i in 0..3) {
                    val a1 = Math.toRadians(specialTimer * 150.0 + i * 90.0)
                    val a2 = Math.toRadians(-specialTimer * 150.0 + i * 90.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(a1).toFloat(), sin(a1).toFloat()), 150f))
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(a2).toFloat(), sin(a2).toFloat()), 120f, 1.8f))
                }
                attackCooldown = 0.15f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                    // Tidal wave burst
                    for (i in 0..15) {
                        val angle = Math.toRadians(i * 24.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 100f, 2f))
                    }
                }
                attackCooldown = 0.7f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Tentacles (animated wave)
        paint.color = if (flashing) Color.WHITE else Color.rgb(20, 80, 80)
        for (i in 0..5) {
            val tx = x + w * (0.1f + i * 0.15f)
            val wave = sin(animTime * 4 + i * 1.2f) * w * 0.05f
            canvas.drawRoundRect(tx + wave, y + h * 0.7f, tx + w * 0.08f + wave, y + h * 1.1f, 4f * scale, 4f * scale, paint)
            // Suction cups
            paint.color = if (flashing) Color.WHITE else Color.rgb(30, 100, 100)
            canvas.drawCircle(tx + w * 0.04f + wave, y + h * 0.8f, 3f * scale, paint)
            canvas.drawCircle(tx + w * 0.04f + wave, y + h * 0.95f, 3f * scale, paint)
            paint.color = if (flashing) Color.WHITE else Color.rgb(20, 80, 80)
        }

        // Main head
        paint.color = if (flashing) Color.WHITE else Color.rgb(30, 100, 100)
        canvas.drawOval(x + w * 0.15f, y + h * 0.1f, x + w * 0.85f, y + h * 0.75f, paint)
        paint.color = if (flashing) Color.WHITE else Color.rgb(40, 120, 120)
        canvas.drawOval(x + w * 0.2f, y + h * 0.15f, x + w * 0.7f, y + h * 0.55f, paint)

        // Eyes (large menacing)
        paint.color = Color.rgb(200, 255, 200)
        canvas.drawOval(x + w * 0.25f, y + h * 0.25f, x + w * 0.45f, y + h * 0.45f, paint)
        canvas.drawOval(x + w * 0.55f, y + h * 0.25f, x + w * 0.75f, y + h * 0.45f, paint)
        // Slit pupils
        paint.color = Color.BLACK
        val pupilOff = sin(animTime * 2) * w * 0.02f
        canvas.drawOval(x + w * 0.33f + pupilOff, y + h * 0.28f, x + w * 0.37f + pupilOff, y + h * 0.42f, paint)
        canvas.drawOval(x + w * 0.63f + pupilOff, y + h * 0.28f, x + w * 0.67f + pupilOff, y + h * 0.42f, paint)

        // Beak
        paint.color = if (flashing) Color.WHITE else Color.rgb(60, 50, 40)
        val beakPath = Path().apply {
            moveTo(x + w * 0.4f, y + h * 0.5f)
            lineTo(x + w * 0.5f, y + h * 0.65f)
            lineTo(x + w * 0.6f, y + h * 0.5f)
            close()
        }
        canvas.drawPath(beakPath, paint)
    }
}
