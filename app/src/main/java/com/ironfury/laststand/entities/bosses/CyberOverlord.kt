package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.sin
import kotlin.math.sqrt

/** Level 5 boss — neon cyber robot with laser grid, EMP burst, targeted fire, matrix rain. */
class CyberOverlord(
    startX: Float,
    bossName: String = "CYBER OVERLORD",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 5) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Laser grid — horizontal and vertical lines
                for (i in -3..3) {
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(i * 0.3f, -1f), 200f))
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(-1f, i * 0.3f), 200f))
                }
                attackCooldown = 1.5f
            }
            AttackPhase.PHASE2 -> {
                // EMP burst — fast expanding ring
                for (i in 0..15) {
                    val angle = Math.toRadians(i * 24.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(kotlin.math.cos(angle).toFloat(), sin(angle).toFloat()), 250f, 0.8f))
                }
                attackCooldown = 1.2f
            }
            AttackPhase.PHASE3 -> {
                // Targeted rapid fire
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    for (i in -1..1) {
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(dx / len + i * 0.08f, dy / len), Constants.ENEMY_BULLET_SPEED * 2f))
                    }
                }
                attackCooldown = 0.2f
            }
            AttackPhase.PHASE4 -> {
                // Matrix rain + charge
                if (!isCharging) {
                    beginCharge(playerX)
                }
                for (i in 0..3) {
                    val x = cx + (Math.random() * 160 - 80).toFloat()
                    pendingBullets.add(BulletData(Vector2(x, cy - 40f), Vector2(0f, 1f), 180f + (Math.random() * 60).toFloat()))
                }
                attackCooldown = 0.2f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Dark body
        paint.color = if (flashing) Color.WHITE else Color.rgb(30, 20, 50)
        canvas.drawRoundRect(x + w * 0.15f, y + h * 0.2f, x + w * 0.85f, y + h * 0.85f, 8f * scale, 8f * scale, paint)

        // Neon trim lines
        paint.color = if (flashing) Color.WHITE else Color.rgb(0, 255, 255)
        paint.strokeWidth = 2f * scale
        paint.style = Paint.Style.STROKE
        canvas.drawRoundRect(x + w * 0.17f, y + h * 0.22f, x + w * 0.83f, y + h * 0.83f, 6f * scale, 6f * scale, paint)
        paint.style = Paint.Style.FILL

        // Visor
        val visorPulse = (sin(animTime * 8) * 0.3f + 0.7f)
        paint.color = Color.rgb(0, (255 * visorPulse).toInt(), (255 * visorPulse).toInt())
        canvas.drawRect(x + w * 0.25f, y + h * 0.3f, x + w * 0.75f, y + h * 0.42f, paint)

        // Antenna
        paint.color = if (flashing) Color.WHITE else Color.rgb(50, 40, 80)
        canvas.drawRect(x + w * 0.48f, y, x + w * 0.52f, y + h * 0.2f, paint)
        paint.color = Color.rgb(255, 0, 255)
        canvas.drawCircle(x + w * 0.5f, y, 4f * scale, paint)

        // Arm cannons
        paint.color = if (flashing) Color.WHITE else Color.rgb(40, 30, 60)
        canvas.drawRoundRect(x - w * 0.05f, y + h * 0.35f, x + w * 0.18f, y + h * 0.65f, 4f * scale, 4f * scale, paint)
        canvas.drawRoundRect(x + w * 0.82f, y + h * 0.35f, x + w * 1.05f, y + h * 0.65f, 4f * scale, 4f * scale, paint)
        // Cannon glow
        paint.color = Color.rgb(0, (200 * visorPulse).toInt(), (255 * visorPulse).toInt())
        canvas.drawCircle(x + w * 0.06f, y + h * 0.5f, 4f * scale, paint)
        canvas.drawCircle(x + w * 0.94f, y + h * 0.5f, 4f * scale, paint)

        // Legs
        paint.color = if (flashing) Color.WHITE else Color.rgb(35, 25, 55)
        canvas.drawRect(x + w * 0.25f, y + h * 0.8f, x + w * 0.4f, y + h, paint)
        canvas.drawRect(x + w * 0.6f, y + h * 0.8f, x + w * 0.75f, y + h, paint)
    }
}
