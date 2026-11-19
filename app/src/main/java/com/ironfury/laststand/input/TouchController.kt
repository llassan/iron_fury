package com.ironfury.laststand.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.RectF
import android.graphics.Shader
import android.view.KeyEvent
import android.view.MotionEvent
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.SettingsManager

class TouchController(
    private val screenWidth: Int,
    private val screenHeight: Int,
    private val sizeMultiplier: Float = 1f,
    private val settings: SettingsManager? = null
) {
    // D-Pad state
    var left = false; private set
    var right = false; private set
    var up = false; private set
    var down = false; private set

    // Buttons
    var jump = false; private set
    var fire = false; private set

    // Touch tracking
    private val activeTouches = mutableMapOf<Int, TouchType>()

    // Mutable control positions
    var dpadCenterX: Float
    var dpadCenterY: Float
    val dpadRadius: Float
    var jumpButton: RectF
    var fireButton: RectF
    private val btnSize: Float

    private val paint = Paint().apply { style = Paint.Style.FILL }
    private val strokePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 3f
        color = Color.WHITE
    }
    private val labelPaint = Paint().apply {
        textAlign = Paint.Align.CENTER
        isAntiAlias = false
        typeface = com.ironfury.laststand.ui.UiFonts.pixel
        color = Color.WHITE
    }

    init {
        val scale = screenHeight / Constants.GAME_HEIGHT

        dpadRadius = Constants.DPAD_SIZE * scale * sizeMultiplier / 2
        btnSize = Constants.BUTTON_SIZE * scale * sizeMultiplier
        val dpadMargin = Constants.CONTROL_MARGIN * scale
        val btnMargin = Constants.CONTROL_MARGIN * scale

        // Default positions
        val defDpadX = dpadMargin + dpadRadius
        val defDpadY = screenHeight - dpadMargin - dpadRadius
        val defFireX = screenWidth - btnMargin - btnSize
        val defFireY = screenHeight - btnMargin - btnSize
        val defJumpX = screenWidth - btnMargin * 2 - btnSize * 2
        val defJumpY = screenHeight - btnMargin - btnSize

        // Apply custom positions if saved
        if (settings != null && settings.hasCustomLayout) {
            val dnx = settings.getDpadNx(); val dny = settings.getDpadNy()
            dpadCenterX = if (dnx >= 0) dnx * screenWidth else defDpadX
            dpadCenterY = if (dny >= 0) dny * screenHeight else defDpadY

            val jnx = settings.getJumpNx(); val jny = settings.getJumpNy()
            val jx = if (jnx >= 0) jnx * screenWidth else defJumpX
            val jy = if (jny >= 0) jny * screenHeight else defJumpY
            jumpButton = RectF(jx, jy, jx + btnSize, jy + btnSize)

            val fnx = settings.getFireNx(); val fny = settings.getFireNy()
            val fx = if (fnx >= 0) fnx * screenWidth else defFireX
            val fy = if (fny >= 0) fny * screenHeight else defFireY
            fireButton = RectF(fx, fy, fx + btnSize, fy + btnSize)
        } else {
            dpadCenterX = defDpadX
            dpadCenterY = defDpadY
            fireButton = RectF(defFireX, defFireY, defFireX + btnSize, defFireY + btnSize)
            jumpButton = RectF(defJumpX, defJumpY, defJumpX + btnSize, defJumpY + btnSize)
        }
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                val x = event.getX(pointerIndex)
                val y = event.getY(pointerIndex)
                handleTouchDown(pointerId, x, y)
            }
            MotionEvent.ACTION_MOVE -> {
                for (i in 0 until event.pointerCount) {
                    val id = event.getPointerId(i)
                    val x = event.getX(i)
                    val y = event.getY(i)
                    handleTouchMove(id, x, y)
                }
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                handleTouchUp(pointerId)
            }
            MotionEvent.ACTION_CANCEL -> {
                resetAll()
            }
        }
        return true
    }

    private fun handleTouchDown(pointerId: Int, x: Float, y: Float) {
        val touchType = getTouchType(x, y)
        activeTouches[pointerId] = touchType
        updateFromTouch(touchType, x, y, true)
    }

    private fun handleTouchMove(pointerId: Int, x: Float, y: Float) {
        val touchType = activeTouches[pointerId] ?: return
        if (touchType == TouchType.DPAD) updateDPadFromPosition(x, y)
    }

    private fun handleTouchUp(pointerId: Int) {
        val touchType = activeTouches.remove(pointerId) ?: return
        updateFromTouch(touchType, 0f, 0f, false)
    }

    private fun getTouchType(x: Float, y: Float): TouchType {
        val dx = x - dpadCenterX
        val dy = y - dpadCenterY
        if (dx * dx + dy * dy <= dpadRadius * dpadRadius * 2.5f) return TouchType.DPAD

        if (jumpButton.contains(x, y)) return TouchType.JUMP
        if (fireButton.contains(x, y)) return TouchType.FIRE

        // Extended touch — closest control in lower half
        if (y > screenHeight * 0.4f) {
            val dpadDist = dx * dx + dy * dy
            val jumpDist = distSq(x, y, jumpButton.centerX(), jumpButton.centerY())
            val fireDist = distSq(x, y, fireButton.centerX(), fireButton.centerY())
            val threshold = (dpadRadius * 3f) * (dpadRadius * 3f)

            if (dpadDist < jumpDist && dpadDist < fireDist && dpadDist < threshold) return TouchType.DPAD
            if (jumpDist < fireDist && jumpDist < threshold) return TouchType.JUMP
            if (fireDist < threshold) return TouchType.FIRE
        }

        return TouchType.NONE
    }

    private fun distSq(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2; return dx * dx + dy * dy
    }

    private fun updateFromTouch(touchType: TouchType, x: Float, y: Float, pressed: Boolean) {
        when (touchType) {
            TouchType.DPAD -> {
                if (pressed) updateDPadFromPosition(x, y)
                else { left = false; right = false; up = false; down = false }
            }
            TouchType.JUMP -> jump = pressed
            TouchType.FIRE -> fire = pressed
            TouchType.NONE -> {}
        }
    }

    private fun updateDPadFromPosition(x: Float, y: Float) {
        val dx = x - dpadCenterX
        val dy = y - dpadCenterY
        val deadzone = dpadRadius * 0.3f

        left = dx < -deadzone
        right = dx > deadzone
        up = dy < -deadzone
        down = dy > deadzone
    }

    private fun resetAll() {
        activeTouches.clear()
        left = false; right = false; up = false; down = false
        jump = false; fire = false
    }

    fun onKeyEvent(keyCode: Int, pressed: Boolean): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_LEFT -> { left = pressed; return true }
            KeyEvent.KEYCODE_D, KeyEvent.KEYCODE_DPAD_RIGHT -> { right = pressed; return true }
            KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_DPAD_UP -> { up = pressed; return true }
            KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_DPAD_DOWN -> { down = pressed; return true }
            KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_K -> { jump = pressed; return true }
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_F, KeyEvent.KEYCODE_ENTER -> { fire = pressed; return true }
        }
        return false
    }

    // ---- Polished custom-drawn controls (glass discs + accent rings + icons) ----
    private val cFill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.STROKE }
    private val cIcon = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND; strokeJoin = Paint.Join.ROUND
    }
    private val cGlow = Paint(Paint.ANTI_ALIAS_FLAG)
    private val iPath = Path()
    private val accentJump = Color.rgb(95, 205, 255)   // cyan
    private val accentFire = Color.rgb(255, 95, 70)    // red-orange
    private val accentDpad = Color.rgb(170, 200, 235)  // cool white

    fun render(canvas: Canvas) {
        drawDpad(canvas)
        drawActionButton(canvas, jumpButton, accentJump, jump, ICON_JUMP)
        drawActionButton(canvas, fireButton, accentFire, fire, ICON_FIRE)
    }

    /** Translucent glass disc with a thin accent ring + optional press glow. */
    private fun glassDisc(canvas: Canvas, cx: Float, cy: Float, r: Float, accent: Int, pressed: Boolean) {
        if (pressed) {
            cGlow.shader = RadialGradient(
                cx, cy, r * 1.4f,
                Color.argb(130, Color.red(accent), Color.green(accent), Color.blue(accent)),
                Color.argb(0, Color.red(accent), Color.green(accent), Color.blue(accent)),
                Shader.TileMode.CLAMP
            )
            canvas.drawCircle(cx, cy, r * 1.4f, cGlow)
            cGlow.shader = null
        }
        // Glass body: lighter top, darker bottom. Kept subtle/see-through so it
        // doesn't cover the gameplay; brightens noticeably while pressed.
        cFill.shader = RadialGradient(
            cx, cy - r * 0.35f, r * 1.25f,
            intArrayOf(
                Color.argb(if (pressed) 140 else 62, 60, 70, 92),
                Color.argb(if (pressed) 110 else 40, 16, 19, 30)
            ),
            null, Shader.TileMode.CLAMP
        )
        canvas.drawCircle(cx, cy, r, cFill)
        cFill.shader = null
        // Accent ring.
        cRing.strokeWidth = r * 0.085f
        cRing.color = if (pressed) accent
        else Color.argb(110, Color.red(accent), Color.green(accent), Color.blue(accent))
        canvas.drawCircle(cx, cy, r * 0.94f, cRing)
    }

    private fun drawDpad(canvas: Canvas) {
        val cx = dpadCenterX; val cy = dpadCenterY; val r = dpadRadius * 0.92f
        glassDisc(canvas, cx, cy, r, accentDpad, left || right || up || down)
        val d = r * 0.55f
        val s = r * 0.3f
        drawChevron(canvas, cx - d, cy, s, 180f, left)
        drawChevron(canvas, cx + d, cy, s, 0f, right)
        drawChevron(canvas, cx, cy - d, s, 270f, up)
        drawChevron(canvas, cx, cy + d, s, 90f, down)
        cFill.color = Color.argb(60, 210, 220, 235)
        canvas.drawCircle(cx, cy, r * 0.08f, cFill)
    }

    /** A thick rounded chevron pointing right, rotated by [rot]; lit when pressed. */
    private fun drawChevron(canvas: Canvas, cx: Float, cy: Float, size: Float, rot: Float, pressed: Boolean) {
        cIcon.style = Paint.Style.STROKE
        cIcon.strokeWidth = size * 0.34f
        cIcon.color = if (pressed) accentJump else Color.argb(150, 225, 232, 245)
        canvas.save()
        canvas.rotate(rot, cx, cy)
        iPath.reset()
        iPath.moveTo(cx - size * 0.28f, cy - size * 0.62f)
        iPath.lineTo(cx + size * 0.42f, cy)
        iPath.lineTo(cx - size * 0.28f, cy + size * 0.62f)
        canvas.drawPath(iPath, cIcon)
        canvas.restore()
        cIcon.style = Paint.Style.FILL
    }

    private fun drawActionButton(canvas: Canvas, rect: RectF, accent: Int, pressed: Boolean, icon: Int) {
        val cx = rect.centerX(); val cy = rect.centerY()
        val r = minOf(rect.width(), rect.height()) / 2f * 0.9f
        glassDisc(canvas, cx, cy, r, accent, pressed)
        cIcon.color = if (pressed) Color.WHITE else Color.argb(175, 236, 242, 252)
        when (icon) {
            ICON_JUMP -> {
                // Upward double-chevron (jump).
                cIcon.style = Paint.Style.STROKE
                cIcon.strokeWidth = r * 0.15f
                iPath.reset()
                iPath.moveTo(cx - r * 0.42f, cy + r * 0.02f)
                iPath.lineTo(cx, cy - r * 0.42f)
                iPath.lineTo(cx + r * 0.42f, cy + r * 0.02f)
                canvas.drawPath(iPath, cIcon)
                iPath.reset()
                iPath.moveTo(cx - r * 0.42f, cy + r * 0.42f)
                iPath.lineTo(cx, cy - r * 0.02f)
                iPath.lineTo(cx + r * 0.42f, cy + r * 0.42f)
                canvas.drawPath(iPath, cIcon)
                cIcon.style = Paint.Style.FILL
            }
            ICON_FIRE -> {
                // Target reticle (shoot).
                cIcon.style = Paint.Style.STROKE
                cIcon.strokeWidth = r * 0.11f
                canvas.drawCircle(cx, cy, r * 0.42f, cIcon)
                canvas.drawLine(cx - r * 0.66f, cy, cx - r * 0.3f, cy, cIcon)
                canvas.drawLine(cx + r * 0.3f, cy, cx + r * 0.66f, cy, cIcon)
                canvas.drawLine(cx, cy - r * 0.66f, cx, cy - r * 0.3f, cIcon)
                canvas.drawLine(cx, cy + r * 0.3f, cx, cy + r * 0.66f, cIcon)
                cIcon.style = Paint.Style.FILL
                canvas.drawCircle(cx, cy, r * 0.12f, cIcon)
            }
        }
    }

    private companion object {
        const val ICON_JUMP = 1
        const val ICON_FIRE = 2
    }

    private enum class TouchType {
        NONE, DPAD, JUMP, FIRE
    }
}
