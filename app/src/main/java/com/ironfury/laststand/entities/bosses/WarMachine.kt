package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.cos
import kotlin.math.sin

/** Level 1 boss — grey tank/mech with spread shots, sweeping laser, bombs, and a charge. */
class WarMachine(
    startX: Float,
    bossName: String = "WAR MACHINE",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 1) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                for (i in -2..2) {
                    val angle = Math.toRadians(-90.0 + i * 20.0)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat())))
                }
                attackCooldown = 1.5f
            }
            AttackPhase.PHASE2 -> {
                laserAngle += laserSweepDir * 3f
                if (laserAngle > 45f || laserAngle < -45f) laserSweepDir *= -1f
                val angle = Math.toRadians(-90.0 + laserAngle)
                pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), Constants.ENEMY_BULLET_SPEED * 1.5f))
                attackCooldown = 0.1f
            }
            AttackPhase.PHASE3 -> {
                for (i in 0..2) {
                    val x = position.x + width * (0.2f + i * 0.3f)
                    pendingBullets.add(BulletData(Vector2(x, position.y + height), Vector2(0f, 1f), 100f, 2f))
                }
                attackCooldown = 2f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                    for (i in 0..7) {
                        val angle = Math.toRadians(i * 45.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat())))
                    }
                }
                attackCooldown = 0.5f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        paint.color = if (flashing) Color.WHITE else Color.rgb(80, 80, 90)
        canvas.drawRoundRect(x + w * 0.1f, y + h * 0.3f, x + w * 0.9f, y + h * 0.85f, 15f * scale, 15f * scale, paint)

        paint.color = if (flashing) Color.WHITE else Color.rgb(100, 50, 50)
        canvas.drawRoundRect(x + w * 0.15f, y + h * 0.35f, x + w * 0.85f, y + h * 0.55f, 10f * scale, 10f * scale, paint)

        paint.color = if (flashing) Color.WHITE else Color.rgb(40, 40, 50)
        canvas.drawOval(x + w * 0.35f, y + h * 0.15f, x + w * 0.65f, y + h * 0.4f, paint)

        val eg = (sin(animTime * 5) * 0.3f + 0.7f)
        paint.color = Color.rgb((255 * eg).toInt(), (50 * eg).toInt(), (50 * eg).toInt())
        canvas.drawOval(x + w * 0.42f, y + h * 0.22f, x + w * 0.58f, y + h * 0.35f, paint)

        paint.color = if (flashing) Color.WHITE else Color.rgb(60, 60, 70)
        canvas.drawRect(x, y + h * 0.4f, x + w * 0.15f, y + h * 0.6f, paint)
        canvas.drawRect(x + w * 0.85f, y + h * 0.4f, x + w, y + h * 0.6f, paint)

        renderThrusters(canvas, x, y, w, h, scale, flashing)
    }
}
