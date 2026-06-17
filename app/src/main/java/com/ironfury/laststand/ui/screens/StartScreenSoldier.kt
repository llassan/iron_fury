package com.ironfury.laststand.ui.screens

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import com.ironfury.laststand.cosmetics.CharacterSkin
import kotlin.math.sin

/**
 * Procedural renderer for the iconic Iron Fury soldier shown on the start
 * screen. Self-contained — takes only [canvas], a center point, a [scale]
 * factor, and an animation [time] in seconds.
 *
 * Extracted out of `GameState.renderStartScreenSoldier` so the start screen
 * artwork can be iterated on without scrolling past a thousand lines of
 * game-loop logic.
 */
object StartScreenSoldier {

    fun render(
        canvas: Canvas,
        centerX: Float,
        centerY: Float,
        scale: Float,
        time: Float,
        skin: CharacterSkin = CharacterSkin.RECRUIT
    ) {
        val p = Paint().apply { isAntiAlias = true; style = Paint.Style.FILL }
        val sp = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE }
        val s = 80f * scale
        val t = time
        val bob = (sin(t * 2.5f) * 2.0f)
        val breathe = (sin(t * 3.0f) * s * 0.008f)

        // Multiply an RGB colour by [f] to derive a darker/lighter shade so the
        // sprite keeps its built-in depth across any character palette.
        fun shade(c: Int, f: Float) = Color.rgb(
            (Color.red(c) * f).toInt().coerceIn(0, 255),
            (Color.green(c) * f).toInt().coerceIn(0, 255),
            (Color.blue(c) * f).toInt().coerceIn(0, 255)
        )

        // === POWER STANCE — legs wide, gun up ===

        // Back leg (lunging back)
        p.color = shade(skin.pants, 0.85f)
        val backLegPath = Path().apply {
            moveTo(centerX - s * 0.08f, centerY + s * 0.32f + bob)
            lineTo(centerX - s * 0.35f, centerY + s * 0.7f + bob)
            lineTo(centerX - s * 0.42f, centerY + s * 0.72f + bob)
            lineTo(centerX - s * 0.28f, centerY + s * 0.72f + bob)
            lineTo(centerX - s * 0.02f, centerY + s * 0.38f + bob)
            close()
        }
        canvas.drawPath(backLegPath, p)
        // Back boot
        p.color = shade(skin.boots, 0.82f)
        canvas.drawRoundRect(
            centerX - s * 0.44f, centerY + s * 0.67f + bob,
            centerX - s * 0.24f, centerY + s * 0.75f + bob,
            3f * scale, 3f * scale, p
        )

        // Front leg (forward lunge)
        p.color = skin.pantsHi
        val frontLegPath = Path().apply {
            moveTo(centerX + s * 0.08f, centerY + s * 0.32f + bob)
            lineTo(centerX + s * 0.28f, centerY + s * 0.65f + bob)
            lineTo(centerX + s * 0.35f, centerY + s * 0.72f + bob)
            lineTo(centerX + s * 0.18f, centerY + s * 0.72f + bob)
            lineTo(centerX + s * 0.02f, centerY + s * 0.38f + bob)
            close()
        }
        canvas.drawPath(frontLegPath, p)
        // Front boot
        p.color = skin.boots
        canvas.drawRoundRect(
            centerX + s * 0.16f, centerY + s * 0.67f + bob,
            centerX + s * 0.38f, centerY + s * 0.75f + bob,
            3f * scale, 3f * scale, p
        )

        // Belt with ammo pouches
        p.color = Color.rgb(60, 50, 35)
        canvas.drawRoundRect(
            centerX - s * 0.22f, centerY + s * 0.28f + bob,
            centerX + s * 0.22f, centerY + s * 0.35f + bob,
            2f * scale, 2f * scale, p
        )
        p.color = Color.rgb(80, 65, 40)
        for (i in -2..1) {
            canvas.drawRect(
                centerX + s * (i * 0.1f), centerY + s * 0.29f + bob,
                centerX + s * (i * 0.1f + 0.07f), centerY + s * 0.34f + bob, p
            )
        }

        // === TORSO — muscular, armored vest ===
        p.color = skin.shirt
        canvas.drawRoundRect(
            centerX - s * 0.22f + breathe, centerY - s * 0.1f + bob,
            centerX + s * 0.22f - breathe, centerY + s * 0.33f + bob,
            6f * scale, 6f * scale, p
        )
        // Vest highlights / armor plates
        p.color = skin.shirtHi
        canvas.drawRoundRect(
            centerX - s * 0.18f, centerY - s * 0.06f + bob,
            centerX - s * 0.02f, centerY + s * 0.12f + bob,
            3f * scale, 3f * scale, p
        )
        canvas.drawRoundRect(
            centerX + s * 0.02f, centerY - s * 0.06f + bob,
            centerX + s * 0.18f, centerY + s * 0.12f + bob,
            3f * scale, 3f * scale, p
        )
        // Dog tags
        p.color = Color.rgb(180, 180, 190)
        canvas.drawCircle(centerX, centerY + s * 0.05f + bob, 2.5f * scale, p)

        // === MUSCULAR ARMS ===
        // Back arm (left, behind body — holding gun grip)
        p.color = shade(skin.skin, 0.9f)
        val backArmPath = Path().apply {
            moveTo(centerX - s * 0.2f, centerY - s * 0.05f + bob)
            lineTo(centerX - s * 0.12f, centerY + s * 0.15f + bob)
            lineTo(centerX + s * 0.15f, centerY - s * 0.2f + bob)
            lineTo(centerX + s * 0.1f, centerY - s * 0.26f + bob)
            close()
        }
        canvas.drawPath(backArmPath, p)

        // === BIG GUN — raised diagonally, aiming up-right ===
        val gunAngle = -30f + (sin(t * 1.5f) * 3.0f)
        canvas.save()
        val gunPivotX = centerX + s * 0.12f
        val gunPivotY = centerY - s * 0.15f + bob
        canvas.rotate(gunAngle, gunPivotX, gunPivotY)

        // Gun body (large assault rifle)
        p.color = Color.rgb(50, 50, 55)
        canvas.drawRoundRect(
            gunPivotX - s * 0.08f, gunPivotY - s * 0.04f,
            gunPivotX + s * 0.65f, gunPivotY + s * 0.04f,
            3f * scale, 3f * scale, p
        )
        // Barrel
        p.color = Color.rgb(40, 40, 45)
        canvas.drawRoundRect(
            gunPivotX + s * 0.5f, gunPivotY - s * 0.025f,
            gunPivotX + s * 0.75f, gunPivotY + s * 0.025f,
            2f * scale, 2f * scale, p
        )
        // Stock
        p.color = Color.rgb(70, 55, 40)
        canvas.drawRoundRect(
            gunPivotX - s * 0.18f, gunPivotY - s * 0.035f,
            gunPivotX - s * 0.05f, gunPivotY + s * 0.05f,
            2f * scale, 2f * scale, p
        )
        // Magazine
        p.color = Color.rgb(55, 55, 60)
        canvas.drawRect(
            gunPivotX + s * 0.12f, gunPivotY + s * 0.03f,
            gunPivotX + s * 0.2f, gunPivotY + s * 0.12f, p
        )
        // Scope
        p.color = Color.rgb(60, 60, 65)
        canvas.drawRoundRect(
            gunPivotX + s * 0.2f, gunPivotY - s * 0.07f,
            gunPivotX + s * 0.38f, gunPivotY - s * 0.035f,
            2f * scale, 2f * scale, p
        )
        // Scope lens glint
        p.color = Color.rgb(100, 180, 255)
        canvas.drawCircle(gunPivotX + s * 0.36f, gunPivotY - s * 0.052f, 2f * scale, p)

        // Muzzle flash (animated)
        val flashCycle = (t * 6).toInt() % 5
        if (flashCycle < 2) {
            val flashX = gunPivotX + s * 0.76f
            val flashY = gunPivotY
            val flashSize = s * (0.08f + flashCycle * 0.04f)
            // Outer glow
            p.color = Color.rgb(255, 180, 50)
            canvas.drawCircle(flashX, flashY, flashSize * 1.5f, p)
            // Core flash
            p.color = Color.rgb(255, 240, 180)
            canvas.drawCircle(flashX, flashY, flashSize, p)
            p.color = Color.WHITE
            canvas.drawCircle(flashX, flashY, flashSize * 0.5f, p)
            // Flash streaks
            p.color = Color.rgb(255, 200, 80)
            sp.color = Color.rgb(255, 200, 80)
            sp.strokeWidth = 2f * scale
            canvas.drawLine(flashX, flashY, flashX + flashSize * 2f, flashY - flashSize, sp)
            canvas.drawLine(flashX, flashY, flashX + flashSize * 2.2f, flashY + flashSize * 0.5f, sp)
            canvas.drawLine(flashX, flashY, flashX + flashSize * 1.5f, flashY + flashSize * 1.2f, sp)
        }

        canvas.restore()

        // Front arm (right, over body — gripping gun)
        p.color = skin.skin
        val frontArmPath = Path().apply {
            moveTo(centerX + s * 0.2f, centerY - s * 0.05f + bob)
            lineTo(centerX + s * 0.25f, centerY + s * 0.08f + bob)
            lineTo(centerX + s * 0.18f, centerY - s * 0.18f + bob)
            lineTo(centerX + s * 0.1f, centerY - s * 0.2f + bob)
            close()
        }
        canvas.drawPath(frontArmPath, p)
        // Fingerless glove
        p.color = Color.rgb(40, 35, 30)
        canvas.drawCircle(centerX + s * 0.14f, centerY - s * 0.18f + bob, 3.5f * scale, p)

        // === HEAD — tough, battle-hardened ===
        // Neck (thick)
        p.color = shade(skin.skin, 0.94f)
        canvas.drawRect(
            centerX - s * 0.08f, centerY - s * 0.18f + bob,
            centerX + s * 0.08f, centerY - s * 0.08f + bob, p
        )

        // Head
        p.color = skin.skin
        canvas.drawOval(
            centerX - s * 0.14f, centerY - s * 0.38f + bob,
            centerX + s * 0.14f, centerY - s * 0.14f + bob, p
        )

        // Jaw (stronger/wider)
        p.color = shade(skin.skin, 0.92f)
        canvas.drawRoundRect(
            centerX - s * 0.12f, centerY - s * 0.22f + bob,
            centerX + s * 0.12f, centerY - s * 0.14f + bob,
            4f * scale, 4f * scale, p
        )

        // Buzz cut hair
        p.color = skin.hair
        val hairPath = Path().apply {
            moveTo(centerX - s * 0.13f, centerY - s * 0.28f + bob)
            quadTo(centerX - s * 0.14f, centerY - s * 0.4f + bob,
                centerX, centerY - s * 0.39f + bob)
            quadTo(centerX + s * 0.14f, centerY - s * 0.4f + bob,
                centerX + s * 0.13f, centerY - s * 0.28f + bob)
            close()
        }
        canvas.drawPath(hairPath, p)

        // Bandana (iconic — tinted per character)
        p.color = skin.bandana
        canvas.drawRoundRect(
            centerX - s * 0.15f, centerY - s * 0.32f + bob,
            centerX + s * 0.15f, centerY - s * 0.26f + bob,
            3f * scale, 3f * scale, p
        )
        // Bandana tail (flowing in wind)
        val tailWave = (sin(t * 5.0f) * s * 0.03f)
        val tailPath = Path().apply {
            moveTo(centerX - s * 0.14f, centerY - s * 0.3f + bob)
            quadTo(centerX - s * 0.3f, centerY - s * 0.32f + bob + tailWave,
                centerX - s * 0.38f, centerY - s * 0.26f + bob + tailWave * 1.5f)
            quadTo(centerX - s * 0.32f, centerY - s * 0.28f + bob + tailWave * 0.5f,
                centerX - s * 0.14f, centerY - s * 0.27f + bob)
            close()
        }
        canvas.drawPath(tailPath, p)

        // Determined eye
        p.color = Color.WHITE
        canvas.drawOval(
            centerX + s * 0.02f, centerY - s * 0.28f + bob,
            centerX + s * 0.1f, centerY - s * 0.23f + bob, p
        )
        p.color = Color.rgb(40, 60, 40)
        canvas.drawCircle(centerX + s * 0.065f, centerY - s * 0.255f + bob, 2f * scale, p)
        p.color = Color.BLACK
        canvas.drawCircle(centerX + s * 0.065f, centerY - s * 0.255f + bob, 1.2f * scale, p)
        // Eyebrow (furrowed)
        sp.color = skin.hair
        sp.strokeWidth = 2.5f * scale
        canvas.drawLine(
            centerX + s * 0.01f, centerY - s * 0.29f + bob,
            centerX + s * 0.11f, centerY - s * 0.31f + bob, sp
        )

        // War paint / scar across cheek
        sp.color = Color.rgb(80, 30, 30)
        sp.strokeWidth = 1.5f * scale
        canvas.drawLine(
            centerX + s * 0.04f, centerY - s * 0.22f + bob,
            centerX + s * 0.12f, centerY - s * 0.19f + bob, sp
        )

        // Stubble shadow on jaw
        p.color = Color.argb(40, 30, 25, 20)
        canvas.drawRoundRect(
            centerX - s * 0.08f, centerY - s * 0.2f + bob,
            centerX + s * 0.1f, centerY - s * 0.15f + bob,
            2f * scale, 2f * scale, p
        )

        // === GROUND SHADOW ===
        p.color = Color.argb(50, 0, 0, 0)
        canvas.drawOval(
            centerX - s * 0.35f, centerY + s * 0.72f + bob,
            centerX + s * 0.35f, centerY + s * 0.78f + bob, p
        )
    }
}
