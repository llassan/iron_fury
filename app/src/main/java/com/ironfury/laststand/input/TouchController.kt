package com.ironfury.laststand.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
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

    fun render(canvas: Canvas) {
        val alpha = Constants.CONTROL_ALPHA

        // D-Pad background
        paint.color = Color.argb(alpha / 2, 50, 50, 50)
        canvas.drawCircle(dpadCenterX, dpadCenterY, dpadRadius, paint)

        // D-Pad directions
        val arrowSize = dpadRadius * 0.35f
        val arrowDist = dpadRadius * 0.5f
        drawArrow(canvas, dpadCenterX - arrowDist, dpadCenterY, arrowSize, 180f, left)
        drawArrow(canvas, dpadCenterX + arrowDist, dpadCenterY, arrowSize, 0f, right)
        drawArrow(canvas, dpadCenterX, dpadCenterY - arrowDist, arrowSize, 270f, up)
        drawArrow(canvas, dpadCenterX, dpadCenterY + arrowDist, arrowSize, 90f, down)

        // Buttons
        drawButton(canvas, jumpButton, "JUMP", jump)
        drawButton(canvas, fireButton, "FIRE", fire)
    }

    private fun drawArrow(canvas: Canvas, cx: Float, cy: Float, size: Float, rotation: Float, pressed: Boolean) {
        paint.color = if (pressed) Color.argb(200, 150, 150, 255)
        else Color.argb(Constants.CONTROL_ALPHA, 100, 100, 100)

        canvas.save()
        canvas.rotate(rotation, cx, cy)
        val path = android.graphics.Path().apply {
            moveTo(cx + size, cy)
            lineTo(cx - size * 0.5f, cy - size * 0.7f)
            lineTo(cx - size * 0.5f, cy + size * 0.7f)
            close()
        }
        canvas.drawPath(path, paint)
        canvas.restore()
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, pressed: Boolean) {
        val isFire = label == "FIRE"
        // Inset the actual button slightly inside the touch rect so it looks
        // compact like the design instead of filling the bbox.
        val inset = rect.width() * 0.08f
        val body = RectF(rect.left + inset, rect.top + inset, rect.right - inset, rect.bottom - inset)

        // Translucent body so the game world stays visible behind the controls.
        val fillAlpha = if (pressed) 180 else 110
        paint.color = if (isFire) Color.argb(fillAlpha, 230, 70, 70)
                      else Color.argb(fillAlpha, 90, 190, 240)
        canvas.drawOval(body, paint)

        // Outline — slightly more opaque so the button shape is still readable
        // against bright backgrounds.
        strokePaint.color = if (isFire) Color.argb(220, 255, 140, 140)
                            else Color.argb(220, 170, 220, 255)
        strokePaint.strokeWidth = body.width() * 0.05f
        canvas.drawOval(body, strokePaint)
        strokePaint.color = Color.WHITE
        strokePaint.strokeWidth = 3f

        // Label — white with thin shadow so it reads on any background.
        labelPaint.color = Color.WHITE
        labelPaint.setShadowLayer(2f, 1f, 1f, Color.argb(180, 0, 0, 0))
        labelPaint.textSize = body.height() * 0.22f
        canvas.drawText(label, body.centerX(), body.centerY() + labelPaint.textSize * 0.35f, labelPaint)
        labelPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)
    }

    private enum class TouchType {
        NONE, DPAD, JUMP, FIRE
    }
}
