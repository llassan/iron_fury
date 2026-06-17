package com.ironfury.laststand.ui

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ironfury.laststand.audio.SoundManager
import com.ironfury.laststand.weapons.WeaponManager
import com.ironfury.laststand.weapons.WeaponType

class WeaponSelector(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val weaponManager: WeaponManager
) {
    var currentWeapon: WeaponType = WeaponType.MACHINE_GUN

    var isExpanded = false
        private set

    private val weapons = WeaponType.getAll()
    private val currentIndex: Int get() = weapons.indexOf(currentWeapon)

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private val textPaint = Paint().apply {
        isAntiAlias = false
        textAlign = Paint.Align.CENTER
        typeface = UiFonts.pixel
    }

    // UI dimensions
    private val slotSize = screenHeight * 0.09f
    private val margin = screenHeight * 0.015f
    private val topY = margin

    // Slot positions when expanded
    private val slotRects = mutableListOf<RectF>()

    // Current weapon indicator position (top center)
    private val currentWeaponRect: RectF

    // Coins display area
    private val coinsRect: RectF

    init {
        // Current weapon display (top center)
        val centerX = screenWidth / 2f
        currentWeaponRect = RectF(
            centerX - slotSize / 2,
            topY,
            centerX + slotSize / 2,
            topY + slotSize
        )

        // Coins pill (top right, next to score)
        val pillW = screenWidth * 0.12f
        val pillH = slotSize * 0.45f
        coinsRect = RectF(
            screenWidth - pillW - margin * 2,
            topY,
            screenWidth - margin * 2,
            topY + pillH
        )

        // Calculate slot positions for expanded view
        val totalWidth = weapons.size * slotSize + (weapons.size - 1) * margin
        val startX = (screenWidth - totalWidth) / 2f

        for (i in weapons.indices) {
            val x = startX + i * (slotSize + margin)
            slotRects.add(RectF(x, topY, x + slotSize, topY + slotSize))
        }
    }

    fun handleTouch(x: Float, y: Float): Boolean {
        if (isExpanded) {
            // Check if tapped on a weapon slot
            for ((index, rect) in slotRects.withIndex()) {
                if (rect.contains(x, y)) {
                    val weapon = weapons[index]

                    if (weaponManager.isWeaponUnlocked(weapon)) {
                        // Select the weapon
                        currentWeapon = weapon
                        isExpanded = false
                        SoundManager.play(SoundManager.Sound.WEAPON_SWITCH)
                    } else {
                        // Try to purchase
                        if (weaponManager.unlockWeapon(weapon)) {
                            currentWeapon = weapon
                            isExpanded = false
                            SoundManager.play(SoundManager.Sound.COIN_COLLECT)  // Purchase sound
                            SoundManager.play(SoundManager.Sound.WEAPON_SWITCH)
                        }
                        // If can't afford, just close
                    }
                    return true
                }
            }
            // Tap outside closes the selector
            isExpanded = false
            return true
        } else {
            // Check if tapped on current weapon (toggle expand)
            val expandedTouchRect = RectF(
                currentWeaponRect.left - slotSize * 0.5f,
                currentWeaponRect.top,
                currentWeaponRect.right + slotSize * 0.5f,
                currentWeaponRect.bottom + slotSize * 0.3f
            )
            if (expandedTouchRect.contains(x, y)) {
                isExpanded = true
                return true
            }
        }
        return false
    }

    fun selectNext() {
        val unlockedWeapons = weaponManager.getUnlockedWeapons()
        val currentIdx = unlockedWeapons.indexOf(currentWeapon)
        val nextIdx = (currentIdx + 1) % unlockedWeapons.size
        currentWeapon = unlockedWeapons[nextIdx]
    }

    fun selectPrevious() {
        val unlockedWeapons = weaponManager.getUnlockedWeapons()
        val currentIdx = unlockedWeapons.indexOf(currentWeapon)
        val prevIdx = if (currentIdx == 0) unlockedWeapons.size - 1 else currentIdx - 1
        currentWeapon = unlockedWeapons[prevIdx]
    }

    fun render(canvas: Canvas) {
        // Always render coins
        renderCoins(canvas)

        if (isExpanded) {
            renderExpanded(canvas)
        } else {
            renderCollapsed(canvas)
        }
    }

    private fun renderCoins(canvas: Canvas) {
        // Pill background
        paint.color = Color.argb(180, 25, 22, 18)
        canvas.drawRoundRect(coinsRect, 6f, 6f, paint)
        paint.color = Color.argb(220, 255, 200, 50)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2f
        canvas.drawRoundRect(coinsRect, 6f, 6f, paint)
        paint.style = Paint.Style.FILL

        // Coin icon
        val coinRadius = coinsRect.height() * 0.30f
        val coinX = coinsRect.left + coinRadius + 8f
        val coinY = coinsRect.centerY()
        paint.color = Color.rgb(255, 200, 50)
        canvas.drawCircle(coinX, coinY, coinRadius, paint)
        paint.color = Color.rgb(255, 230, 120)
        canvas.drawCircle(coinX - 1.5f, coinY - 1.5f, coinRadius * 0.5f, paint)

        // Coin count (pixel font)
        textPaint.textSize = coinsRect.height() * 0.45f
        textPaint.color = Color.rgb(255, 210, 70)
        textPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "${weaponManager.coins}",
            coinsRect.right - 8f,
            coinY + textPaint.textSize * 0.35f,
            textPaint
        )
    }

    private fun renderCollapsed(canvas: Canvas) {
        // Background
        paint.color = Color.argb(180, 0, 0, 0)
        canvas.drawRoundRect(currentWeaponRect, 12f, 12f, paint)

        // Border with weapon color
        paint.color = getWeaponColor(currentWeapon)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 3f
        canvas.drawRoundRect(currentWeaponRect, 12f, 12f, paint)
        paint.style = Paint.Style.FILL

        // Weapon icon
        renderWeaponIcon(canvas, currentWeaponRect, currentWeapon, true, true)

        // Hint text (small, dim, pixel font)
        textPaint.textSize = slotSize * 0.11f
        textPaint.color = Color.argb(120, 200, 200, 200)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText("TAP TO CHANGE", currentWeaponRect.centerX(),
                       currentWeaponRect.bottom + slotSize * 0.22f, textPaint)
    }

    private fun renderExpanded(canvas: Canvas) {
        // Darken background
        paint.color = Color.argb(150, 0, 0, 0)
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), topY + slotSize + margin * 3, paint)

        // Render all weapon slots
        for ((index, rect) in slotRects.withIndex()) {
            val weapon = weapons[index]
            val isSelected = weapon == currentWeapon
            val isUnlocked = weaponManager.isWeaponUnlocked(weapon)
            val canAfford = weaponManager.coins >= weaponManager.getWeaponPrice(weapon)

            // Background
            paint.color = when {
                isSelected -> Color.argb(220, 50, 70, 50)
                isUnlocked -> Color.argb(180, 40, 40, 40)
                canAfford -> Color.argb(180, 50, 50, 30)
                else -> Color.argb(150, 30, 30, 30)
            }
            canvas.drawRoundRect(rect, 12f, 12f, paint)

            // Border
            paint.color = when {
                isSelected -> getWeaponColor(weapon)
                isUnlocked -> Color.argb(150, 150, 150, 150)
                canAfford -> Color.rgb(255, 215, 0)
                else -> Color.argb(80, 100, 100, 100)
            }
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = if (isSelected) 4f else 2f
            canvas.drawRoundRect(rect, 12f, 12f, paint)
            paint.style = Paint.Style.FILL

            // Weapon icon
            renderWeaponIcon(canvas, rect, weapon, isSelected || isUnlocked, isUnlocked)

            // Lock overlay or price for locked weapons
            if (!isUnlocked) {
                renderLockOverlay(canvas, rect, weapon, canAfford)
            }
        }
    }

    private fun renderWeaponIcon(canvas: Canvas, rect: RectF, weapon: WeaponType, highlight: Boolean, unlocked: Boolean) {
        val centerX = rect.centerX()
        val centerY = rect.centerY() - rect.height() * 0.08f
        val iconSize = rect.width() * 0.30f

        // Weapon-specific icon
        val alpha = if (unlocked) 255 else 100
        paint.color = if (highlight) getWeaponColor(weapon) else Color.argb(alpha, 150, 150, 150)

        when (weapon) {
            WeaponType.MACHINE_GUN -> {
                // Gun shape
                canvas.drawRect(centerX - iconSize, centerY - iconSize * 0.2f,
                              centerX + iconSize, centerY + iconSize * 0.2f, paint)
                canvas.drawRect(centerX - iconSize * 0.3f, centerY,
                              centerX + iconSize * 0.1f, centerY + iconSize * 0.6f, paint)
            }
            WeaponType.SPREAD_GUN -> {
                // Three lines spreading out
                paint.strokeWidth = 4f
                paint.style = Paint.Style.STROKE
                canvas.drawLine(centerX - iconSize, centerY, centerX + iconSize, centerY - iconSize * 0.5f, paint)
                canvas.drawLine(centerX - iconSize, centerY, centerX + iconSize, centerY, paint)
                canvas.drawLine(centerX - iconSize, centerY, centerX + iconSize, centerY + iconSize * 0.5f, paint)
                paint.style = Paint.Style.FILL
            }
            WeaponType.LASER -> {
                // Beam shape
                val laserColor = if (highlight) Color.CYAN else Color.argb(alpha, 0, 150, 150)
                paint.color = laserColor
                canvas.drawRect(centerX - iconSize, centerY - iconSize * 0.1f,
                              centerX + iconSize, centerY + iconSize * 0.1f, paint)
                paint.color = Color.argb(alpha, 255, 255, 255)
                canvas.drawRect(centerX - iconSize * 0.8f, centerY - iconSize * 0.05f,
                              centerX + iconSize * 0.8f, centerY + iconSize * 0.05f, paint)
            }
            WeaponType.ROCKET_LAUNCHER -> {
                // Rocket shape
                canvas.drawOval(centerX - iconSize, centerY - iconSize * 0.3f,
                              centerX + iconSize * 0.5f, centerY + iconSize * 0.3f, paint)
                val tipColor = if (highlight) Color.RED else Color.argb(alpha, 150, 50, 50)
                paint.color = tipColor
                canvas.drawOval(centerX + iconSize * 0.3f, centerY - iconSize * 0.2f,
                              centerX + iconSize, centerY + iconSize * 0.2f, paint)
            }
            WeaponType.FLAMETHROWER -> {
                // Flame shape
                val flameColor = if (highlight) Color.rgb(255, 100, 0) else Color.argb(alpha, 200, 80, 0)
                paint.color = flameColor
                canvas.drawCircle(centerX, centerY, iconSize * 0.6f, paint)
                val innerColor = if (highlight) Color.rgb(255, 200, 50) else Color.argb(alpha, 200, 150, 40)
                paint.color = innerColor
                canvas.drawCircle(centerX - iconSize * 0.1f, centerY, iconSize * 0.35f, paint)
                paint.color = Color.argb(alpha, 255, 255, 200)
                canvas.drawCircle(centerX - iconSize * 0.2f, centerY, iconSize * 0.15f, paint)
            }
        }

        // Weapon name below icon (uppercase, pixel font)
        textPaint.textSize = rect.height() * 0.12f
        textPaint.color = if (highlight) Color.WHITE else Color.argb(alpha, 180, 180, 180)
        textPaint.textAlign = Paint.Align.CENTER
        canvas.drawText(weapon.displayName.uppercase(), centerX, rect.bottom - rect.height() * 0.06f, textPaint)
    }

    private fun renderLockOverlay(canvas: Canvas, rect: RectF, weapon: WeaponType, canAfford: Boolean) {
        // Semi-transparent overlay
        paint.color = Color.argb(120, 0, 0, 0)
        canvas.drawRoundRect(rect, 12f, 12f, paint)

        val centerX = rect.centerX()
        val centerY = rect.centerY()

        // Price tag
        val price = weaponManager.getWeaponPrice(weapon)

        // Coin icon
        val coinSize = rect.height() * 0.12f
        paint.color = if (canAfford) Color.rgb(255, 215, 0) else Color.rgb(150, 130, 80)
        canvas.drawCircle(centerX - rect.width() * 0.15f, centerY + rect.height() * 0.15f, coinSize, paint)

        // Price text
        textPaint.textSize = rect.height() * 0.18f
        textPaint.color = if (canAfford) Color.rgb(255, 215, 0) else Color.rgb(150, 130, 80)
        textPaint.textAlign = Paint.Align.LEFT
        canvas.drawText("$price", centerX - rect.width() * 0.02f, centerY + rect.height() * 0.22f, textPaint)

        // "BUY" or lock icon
        if (canAfford) {
            textPaint.textSize = rect.height() * 0.15f
            textPaint.color = Color.rgb(100, 255, 100)
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("TAP TO BUY", centerX, centerY - rect.height() * 0.15f, textPaint)
        } else {
            // Lock icon
            paint.color = Color.rgb(100, 100, 100)
            val lockSize = rect.height() * 0.15f
            // Lock body
            canvas.drawRect(
                centerX - lockSize,
                centerY - rect.height() * 0.15f,
                centerX + lockSize,
                centerY - rect.height() * 0.15f + lockSize * 1.2f,
                paint
            )
            // Lock shackle
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 3f
            canvas.drawArc(
                centerX - lockSize * 0.6f,
                centerY - rect.height() * 0.15f - lockSize * 0.8f,
                centerX + lockSize * 0.6f,
                centerY - rect.height() * 0.15f + lockSize * 0.4f,
                180f, 180f, false, paint
            )
            paint.style = Paint.Style.FILL
        }
    }

    private fun getWeaponColor(weapon: WeaponType): Int {
        return when (weapon) {
            WeaponType.MACHINE_GUN -> Color.YELLOW
            WeaponType.SPREAD_GUN -> Color.rgb(255, 150, 50)
            WeaponType.LASER -> Color.CYAN
            WeaponType.ROCKET_LAUNCHER -> Color.rgb(255, 80, 80)
            WeaponType.FLAMETHROWER -> Color.rgb(255, 120, 0)
        }
    }
}
