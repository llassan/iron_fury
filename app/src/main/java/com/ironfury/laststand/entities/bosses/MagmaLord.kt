package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/** Level 4 boss — lava demon with eruption, fire ring, magma rain, dual-spiral inferno. */
class MagmaLord(
    startX: Float,
    bossName: String = "MAGMA LORD",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 4) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Eruption — upward burst that rains down
                for (i in -3..3) {
                    val angle = Math.toRadians(-90.0 + i * 15.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 200f, 2f))
                }
                attackCooldown = 1.3f
            }
            AttackPhase.PHASE2 -> {
                // Fire ring — expanding circle
                for (i in 0..11) {
                    val angle = Math.toRadians(i * 30.0 + specialTimer * 30)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 140f))
                }
                attackCooldown = 1.0f
            }
            AttackPhase.PHASE3 -> {
                // Magma rain — random positions
                for (i in 0..5) {
                    val x = cx + (Math.random() * 200 - 100).toFloat()
                    pendingBullets.add(BulletData(Vector2(x, cy - 30f), Vector2((Math.random() * 0.6 - 0.3).toFloat(), 1f), 120f, 2.5f))
                }
                attackCooldown = 0.8f
            }
            AttackPhase.PHASE4 -> {
                // Inferno — dual spiral
                for (i in 0..3) {
                    val a1 = Math.toRadians(specialTimer * 200.0 + i * 90.0)
                    val a2 = Math.toRadians(specialTimer * 200.0 + i * 90.0 + 45.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(a1).toFloat(), sin(a1).toFloat()), 160f, 1.5f))
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(a2).toFloat(), sin(a2).toFloat()), 130f, 1.2f))
                }
                attackCooldown = 0.15f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Lava body
        paint.color = if (flashing) Color.WHITE else Color.rgb(150, 40, 20)
        canvas.drawOval(x + w * 0.1f, y + h * 0.2f, x + w * 0.9f, y + h * 0.9f, paint)

        // Magma cracks
        paint.color = if (flashing) Color.WHITE else Color.rgb(255, 150, 30)
        paint.strokeWidth = 3f * scale
        paint.style = Paint.Style.STROKE
        canvas.drawLine(x + w * 0.3f, y + h * 0.35f, x + w * 0.5f, y + h * 0.6f, paint)
        canvas.drawLine(x + w * 0.5f, y + h * 0.6f, x + w * 0.7f, y + h * 0.4f, paint)
        canvas.drawLine(x + w * 0.4f, y + h * 0.5f, x + w * 0.35f, y + h * 0.75f, paint)
        canvas.drawLine(x + w * 0.6f, y + h * 0.45f, x + w * 0.65f, y + h * 0.7f, paint)
        paint.style = Paint.Style.FILL

        // Horns
        paint.color = if (flashing) Color.WHITE else Color.rgb(80, 20, 10)
        val leftHorn = Path().apply {
            moveTo(x + w * 0.25f, y + h * 0.25f)
            lineTo(x + w * 0.15f, y)
            lineTo(x + w * 0.35f, y + h * 0.25f)
            close()
        }
        canvas.drawPath(leftHorn, paint)
        val rightHorn = Path().apply {
            moveTo(x + w * 0.65f, y + h * 0.25f)
            lineTo(x + w * 0.85f, y)
            lineTo(x + w * 0.75f, y + h * 0.25f)
            close()
        }
        canvas.drawPath(rightHorn, paint)

        // Glowing eyes
        val eg = (sin(animTime * 6) * 0.3f + 0.7f)
        paint.color = Color.rgb(255, (200 * eg).toInt(), 0)
        canvas.drawOval(x + w * 0.3f, y + h * 0.35f, x + w * 0.45f, y + h * 0.48f, paint)
        canvas.drawOval(x + w * 0.55f, y + h * 0.35f, x + w * 0.7f, y + h * 0.48f, paint)

        // Lava drip animation
        val dripY = (animTime * 30 % h) * 0.3f
        paint.color = Color.rgb(255, 120, 20)
        canvas.drawCircle(x + w * 0.3f, y + h * 0.85f + dripY, 4f * scale, paint)
        canvas.drawCircle(x + w * 0.7f, y + h * 0.85f + dripY * 0.7f, 3f * scale, paint)
    }
}
