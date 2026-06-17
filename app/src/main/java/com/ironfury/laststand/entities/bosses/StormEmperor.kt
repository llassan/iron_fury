package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/** Level 7 boss — sky-fortress eagle with lightning bolts, thunder ring, tempest, storm charge. */
class StormEmperor(
    startX: Float,
    bossName: String = "STORM EMPEROR",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 7) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Lightning bolts — fast targeted
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    for (i in 0..2) {
                        val spread = (Math.random() * 0.3 - 0.15).toFloat()
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(dx / len + spread, dy / len), 280f, 0.8f))
                    }
                }
                attackCooldown = 0.8f
            }
            AttackPhase.PHASE2 -> {
                // Thunder ring + aimed shot
                for (i in 0..9) {
                    val angle = Math.toRadians(i * 36.0 + specialTimer * 20)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 170f))
                }
                attackCooldown = 1.0f
            }
            AttackPhase.PHASE3 -> {
                // Tempest — alternating spirals
                val dir = if ((specialTimer * 2).toInt() % 2 == 0) 1.0 else -1.0
                for (i in 0..5) {
                    val angle = Math.toRadians(dir * specialTimer * 180.0 + i * 60.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 190f, 1.3f))
                }
                attackCooldown = 0.2f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                }
                // Lightning storm while charging
                for (i in 0..2) {
                    val x = cx + (Math.random() * 200 - 100).toFloat()
                    pendingBullets.add(BulletData(Vector2(x, cy - 50f), Vector2((Math.random() * 0.4 - 0.2).toFloat(), 1f), 300f))
                }
                attackCooldown = 0.15f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Wings (animated flap)
        val wingFlap = sin(animTime * 5) * h * 0.1f
        paint.color = if (flashing) Color.WHITE else Color.rgb(180, 160, 100)
        val leftWing = Path().apply {
            moveTo(x + w * 0.2f, y + h * 0.4f)
            lineTo(x - w * 0.2f, y + h * 0.2f + wingFlap)
            lineTo(x - w * 0.1f, y + h * 0.55f + wingFlap * 0.5f)
            close()
        }
        canvas.drawPath(leftWing, paint)
        val rightWing = Path().apply {
            moveTo(x + w * 0.8f, y + h * 0.4f)
            lineTo(x + w * 1.2f, y + h * 0.2f + wingFlap)
            lineTo(x + w * 1.1f, y + h * 0.55f + wingFlap * 0.5f)
            close()
        }
        canvas.drawPath(rightWing, paint)

        // Body
        paint.color = if (flashing) Color.WHITE else Color.rgb(200, 180, 120)
        canvas.drawOval(x + w * 0.2f, y + h * 0.25f, x + w * 0.8f, y + h * 0.85f, paint)

        // Crown/crest
        paint.color = if (flashing) Color.WHITE else Color.rgb(255, 215, 0)
        for (i in 0..2) {
            val crownPath = Path().apply {
                moveTo(x + w * (0.35f + i * 0.12f), y + h * 0.25f)
                lineTo(x + w * (0.38f + i * 0.12f), y + h * 0.05f)
                lineTo(x + w * (0.41f + i * 0.12f), y + h * 0.25f)
                close()
            }
            canvas.drawPath(crownPath, paint)
        }

        // Eyes
        val eg = (sin(animTime * 6) * 0.3f + 0.7f)
        paint.color = Color.rgb(255, (255 * eg).toInt(), (100 * eg).toInt())
        canvas.drawOval(x + w * 0.32f, y + h * 0.35f, x + w * 0.45f, y + h * 0.48f, paint)
        canvas.drawOval(x + w * 0.55f, y + h * 0.35f, x + w * 0.68f, y + h * 0.48f, paint)

        // Lightning bolts (decorative)
        paint.color = Color.rgb(255, 255, 150)
        paint.strokeWidth = 2f * scale
        paint.style = Paint.Style.STROKE
        val boltOffset = sin(animTime * 10) * w * 0.03f
        canvas.drawLine(x + w * 0.1f, y + h * 0.6f, x + w * 0.15f + boltOffset, y + h * 0.8f, paint)
        canvas.drawLine(x + w * 0.15f + boltOffset, y + h * 0.8f, x + w * 0.1f, y + h, paint)
        canvas.drawLine(x + w * 0.9f, y + h * 0.6f, x + w * 0.85f - boltOffset, y + h * 0.8f, paint)
        canvas.drawLine(x + w * 0.85f - boltOffset, y + h * 0.8f, x + w * 0.9f, y + h, paint)
        paint.style = Paint.Style.FILL
    }
}
