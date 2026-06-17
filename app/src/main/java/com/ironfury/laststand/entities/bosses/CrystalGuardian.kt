package com.ironfury.laststand.entities.bosses

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Path
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.cos
import kotlin.math.sin

/** Level 9 boss — prismatic crystal with shard burst, sweeping prismatic beam, gem spiral, shatter. */
class CrystalGuardian(
    startX: Float,
    bossName: String = "CRYSTAL GUARDIAN",
    healthMultiplier: Float = 1f,
    levelWidth: Float = Constants.LEVEL_WIDTH
) : Boss(startX, bossName, healthMultiplier, levelWidth, levelNumber = 9) {

    override fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    ) {
        when (phase) {
            AttackPhase.PHASE1 -> {
                // Shard burst — 8-way fast
                for (i in 0..7) {
                    val angle = Math.toRadians(i * 45.0 + specialTimer * 15)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 220f, 1.2f))
                }
                attackCooldown = 1.0f
            }
            AttackPhase.PHASE2 -> {
                // Prismatic beam — sweeping laser
                laserAngle += laserSweepDir * 4f
                if (laserAngle > 60f || laserAngle < -60f) laserSweepDir *= -1f
                for (i in 0..2) {
                    val angle = Math.toRadians(-90.0 + laserAngle + i * 5)
                    pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 260f, 0.8f))
                }
                attackCooldown = 0.08f
            }
            AttackPhase.PHASE3 -> {
                // Gem spiral — triple helix
                for (j in 0..2) {
                    for (i in 0..3) {
                        val angle = Math.toRadians(specialTimer * 200.0 + j * 120.0 + i * 30.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 150f + i * 20f, 1.3f))
                    }
                }
                attackCooldown = 0.25f
            }
            AttackPhase.PHASE4 -> {
                if (!isCharging) {
                    beginCharge(playerX)
                    // Shatter — massive burst
                    for (i in 0..23) {
                        val angle = Math.toRadians(i * 15.0)
                        pendingBullets.add(BulletData(Vector2(cx, cy), Vector2(cos(angle).toFloat(), sin(angle).toFloat()), 130f + (i % 3) * 40f, 1.5f))
                    }
                }
                attackCooldown = 0.6f
            }
        }
    }

    override fun renderBody(canvas: Canvas, x: Float, y: Float, w: Float, h: Float, scale: Float, flashing: Boolean) {
        // Crystal body (hexagonal-ish)
        paint.color = if (flashing) Color.WHITE else Color.rgb(120, 60, 160)
        val bodyPath = Path().apply {
            moveTo(x + w * 0.5f, y + h * 0.1f)
            lineTo(x + w * 0.8f, y + h * 0.3f)
            lineTo(x + w * 0.85f, y + h * 0.65f)
            lineTo(x + w * 0.7f, y + h * 0.9f)
            lineTo(x + w * 0.3f, y + h * 0.9f)
            lineTo(x + w * 0.15f, y + h * 0.65f)
            lineTo(x + w * 0.2f, y + h * 0.3f)
            close()
        }
        canvas.drawPath(bodyPath, paint)

        // Crystal facets (lighter)
        paint.color = if (flashing) Color.WHITE else Color.rgb(160, 100, 200)
        val facet = Path().apply {
            moveTo(x + w * 0.5f, y + h * 0.1f)
            lineTo(x + w * 0.65f, y + h * 0.45f)
            lineTo(x + w * 0.5f, y + h * 0.6f)
            lineTo(x + w * 0.35f, y + h * 0.45f)
            close()
        }
        canvas.drawPath(facet, paint)

        // Floating crystal shards around body
        val shardPulse = sin(animTime * 4)
        paint.color = if (flashing) Color.WHITE else Color.rgb(200, 130, 255)
        for (i in 0..5) {
            val angle = animTime * 2 + i * 1.05f
            val dist = w * 0.45f + shardPulse * w * 0.05f
            val sx = x + w * 0.5f + cos(angle) * dist
            val sy = y + h * 0.5f + sin(angle) * dist * 0.6f
            val shardPath = Path().apply {
                moveTo(sx, sy - 6f * scale)
                lineTo(sx - 3f * scale, sy)
                lineTo(sx, sy + 6f * scale)
                lineTo(sx + 3f * scale, sy)
                close()
            }
            canvas.drawPath(shardPath, paint)
        }

        // Central eye
        val eg = (sin(animTime * 5) * 0.3f + 0.7f)
        paint.color = Color.rgb((255 * eg).toInt(), (100 * eg).toInt(), (255 * eg).toInt())
        canvas.drawOval(x + w * 0.38f, y + h * 0.35f, x + w * 0.62f, y + h * 0.55f, paint)
        paint.color = Color.rgb(255, 200, 255)
        canvas.drawOval(x + w * 0.45f, y + h * 0.4f, x + w * 0.55f, y + h * 0.5f, paint)
    }
}
