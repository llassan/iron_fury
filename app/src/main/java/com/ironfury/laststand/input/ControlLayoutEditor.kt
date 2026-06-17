package com.ironfury.laststand.input

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.ironfury.laststand.utils.SettingsManager

class ControlLayoutEditor(
    private val screenWidth: Int,
    private val screenHeight: Int,
    controller: TouchController,
    private val settings: SettingsManager
) {
    var isDone = false
    var shouldReset = false

    // Current positions (copied from controller)
    private var dpadX = controller.dpadCenterX
    private var dpadY = controller.dpadCenterY
    private val dpadRadius = controller.dpadRadius

    private var jumpX = controller.jumpButton.left
    private var jumpY = controller.jumpButton.top
    private val btnSize = controller.jumpButton.width()

    private var fireX = controller.fireButton.left
    private var fireY = controller.fireButton.top

    // Dragging state
    private var dragging: DragTarget? = null
    private var dragOffsetX = 0f
    private var dragOffsetY = 0f

    // Buttons
    private val doneRect = RectF()
    private val resetRect = RectF()

    private val paint = Paint().apply { isAntiAlias = true }
    private val textPaint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }
    private val strokePaint = Paint().apply {
        isAntiAlias = true
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }

    private enum class DragTarget { DPAD, JUMP, FIRE }

    fun onTouchEvent(event: MotionEvent) {
        val x = event.x
        val y = event.y

        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                // Check done/reset buttons first
                if (doneRect.contains(x, y)) { isDone = true; return }
                if (resetRect.contains(x, y)) { isDone = true; shouldReset = true; return }

                // Check which control is being touched
                val dpadDist = dist(x, y, dpadX, dpadY)
                if (dpadDist < dpadRadius * 1.5f) {
                    dragging = DragTarget.DPAD
                    dragOffsetX = dpadX - x
                    dragOffsetY = dpadY - y
                    return
                }

                val jumpRect = RectF(jumpX, jumpY, jumpX + btnSize, jumpY + btnSize)
                if (jumpRect.contains(x, y) || dist(x, y, jumpRect.centerX(), jumpRect.centerY()) < btnSize) {
                    dragging = DragTarget.JUMP
                    dragOffsetX = jumpX - x
                    dragOffsetY = jumpY - y
                    return
                }

                val fireRect = RectF(fireX, fireY, fireX + btnSize, fireY + btnSize)
                if (fireRect.contains(x, y) || dist(x, y, fireRect.centerX(), fireRect.centerY()) < btnSize) {
                    dragging = DragTarget.FIRE
                    dragOffsetX = fireX - x
                    dragOffsetY = fireY - y
                    return
                }
            }

            MotionEvent.ACTION_MOVE -> {
                when (dragging) {
                    DragTarget.DPAD -> {
                        dpadX = (x + dragOffsetX).coerceIn(dpadRadius, screenWidth - dpadRadius)
                        dpadY = (y + dragOffsetY).coerceIn(dpadRadius, screenHeight - dpadRadius)
                    }
                    DragTarget.JUMP -> {
                        jumpX = (x + dragOffsetX).coerceIn(0f, screenWidth - btnSize)
                        jumpY = (y + dragOffsetY).coerceIn(0f, screenHeight - btnSize)
                    }
                    DragTarget.FIRE -> {
                        fireX = (x + dragOffsetX).coerceIn(0f, screenWidth - btnSize)
                        fireY = (y + dragOffsetY).coerceIn(0f, screenHeight - btnSize)
                    }
                    null -> {}
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                dragging = null
            }
        }
    }

    fun save() {
        if (!shouldReset) {
            settings.saveDpadPosition(dpadX / screenWidth, dpadY / screenHeight)
            settings.saveJumpPosition(jumpX / screenWidth, jumpY / screenHeight)
            settings.saveFirePosition(fireX / screenWidth, fireY / screenHeight)
        }
    }

    fun render(canvas: Canvas) {
        // Dark background
        canvas.drawColor(Color.rgb(15, 12, 25))

        // Grid lines for alignment help
        paint.color = Color.argb(25, 255, 255, 255)
        for (i in 1..3) {
            val x = screenWidth * i / 4f
            canvas.drawLine(x, 0f, x, screenHeight.toFloat(), paint)
        }
        for (i in 1..2) {
            val y = screenHeight * i / 3f
            canvas.drawLine(0f, y, screenWidth.toFloat(), y, paint)
        }

        // Title
        textPaint.textSize = screenHeight * 0.05f
        textPaint.color = Color.rgb(255, 120, 100)
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK)
        canvas.drawText("DRAG CONTROLS TO REPOSITION", screenWidth / 2f, screenHeight * 0.08f, textPaint)
        textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)

        // Draw D-Pad (with highlight if dragging)
        val dpadHighlight = dragging == DragTarget.DPAD
        paint.color = if (dpadHighlight) Color.argb(120, 80, 120, 255) else Color.argb(80, 50, 50, 50)
        canvas.drawCircle(dpadX, dpadY, dpadRadius, paint)
        strokePaint.color = if (dpadHighlight) Color.rgb(100, 150, 255) else Color.argb(150, 150, 150, 150)
        canvas.drawCircle(dpadX, dpadY, dpadRadius, strokePaint)

        // D-Pad arrows
        paint.color = if (dpadHighlight) Color.argb(220, 150, 180, 255) else Color.argb(150, 120, 120, 120)
        val ad = dpadRadius * 0.5f
        val as_ = dpadRadius * 0.25f
        canvas.drawCircle(dpadX - ad, dpadY, as_, paint)
        canvas.drawCircle(dpadX + ad, dpadY, as_, paint)
        canvas.drawCircle(dpadX, dpadY - ad, as_, paint)
        canvas.drawCircle(dpadX, dpadY + ad, as_, paint)

        // Label
        textPaint.textSize = screenHeight * 0.025f
        textPaint.color = Color.argb(200, 200, 200, 200)
        canvas.drawText("D-PAD", dpadX, dpadY + dpadRadius + screenHeight * 0.04f, textPaint)

        // Draw Jump button
        val jumpHighlight = dragging == DragTarget.JUMP
        val jumpRect = RectF(jumpX, jumpY, jumpX + btnSize, jumpY + btnSize)
        paint.color = if (jumpHighlight) Color.argb(120, 80, 200, 120) else Color.argb(80, 50, 50, 50)
        canvas.drawOval(jumpRect, paint)
        strokePaint.color = if (jumpHighlight) Color.rgb(100, 255, 150) else Color.argb(150, 150, 150, 150)
        canvas.drawOval(jumpRect, strokePaint)
        // Arrow icon
        paint.color = if (jumpHighlight) Color.WHITE else Color.argb(200, 200, 200, 200)
        val jcx = jumpRect.centerX(); val jcy = jumpRect.centerY()
        val ji = btnSize * 0.2f
        val arrowPath = android.graphics.Path().apply {
            moveTo(jcx, jcy - ji * 1.2f)
            lineTo(jcx - ji, jcy + ji * 0.2f)
            lineTo(jcx + ji, jcy + ji * 0.2f)
            close()
        }
        canvas.drawPath(arrowPath, paint)
        canvas.drawText("JUMP", jumpRect.centerX(), jumpRect.bottom + screenHeight * 0.04f, textPaint)

        // Draw Fire button
        val fireHighlight = dragging == DragTarget.FIRE
        val fireRect = RectF(fireX, fireY, fireX + btnSize, fireY + btnSize)
        paint.color = if (fireHighlight) Color.argb(120, 200, 80, 80) else Color.argb(80, 50, 50, 50)
        canvas.drawOval(fireRect, paint)
        strokePaint.color = if (fireHighlight) Color.rgb(255, 100, 100) else Color.argb(150, 150, 150, 150)
        canvas.drawOval(fireRect, strokePaint)
        // Crosshair icon
        paint.color = if (fireHighlight) Color.WHITE else Color.argb(200, 200, 200, 200)
        val fcx = fireRect.centerX(); val fcy = fireRect.centerY()
        canvas.drawCircle(fcx, fcy, btnSize * 0.15f, paint)
        canvas.drawText("FIRE", fireRect.centerX(), fireRect.bottom + screenHeight * 0.04f, textPaint)

        // Done button
        val btnW = screenWidth * 0.15f
        val btnH = screenHeight * 0.08f
        doneRect.set(screenWidth / 2f - btnW - 10f, screenHeight * 0.88f,
            screenWidth / 2f - 10f, screenHeight * 0.88f + btnH)
        paint.color = Color.rgb(40, 100, 50)
        canvas.drawRoundRect(doneRect, 10f, 10f, paint)
        strokePaint.color = Color.rgb(80, 200, 100)
        canvas.drawRoundRect(doneRect, 10f, 10f, strokePaint)
        textPaint.textSize = screenHeight * 0.03f
        textPaint.color = Color.WHITE
        canvas.drawText("DONE", doneRect.centerX(), doneRect.centerY() + screenHeight * 0.012f, textPaint)

        // Reset button
        resetRect.set(screenWidth / 2f + 10f, screenHeight * 0.88f,
            screenWidth / 2f + btnW + 10f, screenHeight * 0.88f + btnH)
        paint.color = Color.rgb(100, 40, 40)
        canvas.drawRoundRect(resetRect, 10f, 10f, paint)
        strokePaint.color = Color.rgb(200, 80, 80)
        canvas.drawRoundRect(resetRect, 10f, 10f, strokePaint)
        textPaint.color = Color.WHITE
        canvas.drawText("RESET", resetRect.centerX(), resetRect.centerY() + screenHeight * 0.012f, textPaint)
    }

    private fun dist(x1: Float, y1: Float, x2: Float, y2: Float): Float {
        val dx = x1 - x2; val dy = y1 - y2
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }
}
