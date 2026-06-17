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

/** Level 10 final boss — combines every attack type into the ultimate challenge. */
class SupremeCommander(
    startX: Float,
    bossName: String = "SUPREME COMMANDER",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 10) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Wide spread + aimed
                for (i in -3..3) {
                    val angle = Math.toRadians(-90.0 + i * 15.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 200f))
                }
                val dx = playerX - cx; val dy = playerY - cy
                val len = sqrt(dx * dx + dy * dy)
                if (len > 0) {
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(dx / len, dy / len), 280f, 2f))
                }
                attackCooldown = 1.0f
            }
            AttackPhase.PHASE2 -> {
                // Ring + spiral combo
                for (i in 0..11) {
                    val angle = Math.toRadians(i * 30.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 180f))
                }
                for (i in 0..5) {
                    val angle = Math.toRadians(specialTimer * 200.0 + i * 60.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 140f, 1.5f))
                }
                attackCooldown = 0.7f
            }
            AttackPhase.PHASE3 -> {
                // Triple helix + rain
                for (j in 0..2) {
                    val angle = Math.toRadians(specialTimer * 250.0 + j * 120.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 200f, 1.3f))
                }
                for (i in 0..2) {
                    val x = cx + (Math.random() * 160 - 80).toFloat()
                    pendingBullets.add(BulletData(Vector2(x, cy - 40f), Vector2(0f, 1f), 220f, 2f))
                }
                attackCooldown = 0.12f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                    // Ultimate burst
                    for (i in 0..23) {
                        val angle = Math.toRadians(i * 15.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 100f + (i % 4) * 30f, 2f))
                    }
                }
                // Constant rain while charging
                for (i in 0..3) {
                    val angle = Math.toRadians(Math.random() * 360.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 150f + (Math.random() * 100).toFloat(), 1.5f))
                }
                attackCooldown = 0.1f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Massive armored body
        paint.color = if (flashing) Color.WHITE else Color.rgb(50, 15, 15)
        canvas.drawRoundRect(x + w * 0.1f, y + h * 0.2f, x + w * 0.9f, y + h * 0.9f, 10f * scale, 10f * scale, paint)

        // Red armor plates
        paint.color = if (flashing) Color.WHITE else Color.rgb(140, 30, 30)
        canvas.drawRoundRect(x + w * 0.15f, y + h * 0.3f, x + w * 0.85f, y + h * 0.5f, 6f * scale, 6f * scale, paint)
        canvas.drawRoundRect(x + w * 0.2f, y + h * 0.6f, x + w * 0.8f, y + h * 0.8f, 6f * scale, 6f * scale, paint)

        // Shoulder cannons
        paint.color = if (flashing) Color.WHITE else Color.rgb(60, 20, 20)
        canvas.drawRoundRect(x - w * 0.1f, y + h * 0.15f, x + w * 0.2f, y + h * 0.55f, 6f * scale, 6f * scale, paint)
        canvas.drawRoundRect(x + w * 0.8f, y + h * 0.15f, x + w * 1.1f, y + h * 0.55f, 6f * scale, 6f * scale, paint)
        // Cannon barrels
        paint.color = if (flashing) Color.WHITE else Color.rgb(40, 10, 10)
        canvas.drawRect(x - w * 0.15f, y + h * 0.25f, x - w * 0.05f, y + h * 0.35f, paint)
        canvas.drawRect(x + w * 1.05f, y + h * 0.25f, x + w * 1.15f, y + h * 0.35f, paint)

        // Helmet
        paint.color = if (flashing) Color.WHITE else Color.rgb(70, 20, 20)
        val helmetPath = Path().apply {
            moveTo(x + w * 0.5f, y)
            lineTo(x + w * 0.25f, y + h * 0.25f)
            lineTo(x + w * 0.75f, y + h * 0.25f)
            close()
        }
        canvas.drawPath(helmetPath, paint)

        // Visor (menacing V-shape)
        val eg = (sin(animTime * 7) * 0.3f + 0.7f)
        paint.color = Color.rgb((255 * eg).toInt(), (40 * eg).toInt(), 0)
        val visorPath = Path().apply {
            moveTo(x + w * 0.3f, y + h * 0.28f)
            lineTo(x + w * 0.5f, y + h * 0.35f)
            lineTo(x + w * 0.7f, y + h * 0.28f)
            lineTo(x + w * 0.65f, y + h * 0.32f)
            lineTo(x + w * 0.5f, y + h * 0.38f)
            lineTo(x + w * 0.35f, y + h * 0.32f)
            close()
        }
        canvas.drawPath(visorPath, paint)

        // Thrusters with intense flames
        renderThrusters(canvas, x, y, w, h, scale, flashing)

        // Battle damage glow lines
        paint.color = Color.rgb((200 * eg).toInt(), (60 * eg).toInt(), 0)
        paint.strokeWidth = 2f * scale
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x + w * 0.25f, y + h * 0.5f, x + w * 0.45f, y + h * 0.55f, paint)
        canvas.drawLine(x + w * 0.55f, y + h * 0.55f, x + w * 0.75f, y + h * 0.5f, paint)
        paint.style = Paint.Style.FILL
    }
}
