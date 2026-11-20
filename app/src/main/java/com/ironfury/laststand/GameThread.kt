package com.ironfury.laststand

import android.graphics.Canvas
import android.os.Build
import android.view.SurfaceHolder
import com.ironfury.laststand.utils.Constants

class GameThread(
    private val surfaceHolder: SurfaceHolder,
    private val gameView: GameView
) : Thread() {

    var running = false
        private set

    private var canvas: Canvas? = null

    override fun run() {
        var lastTime = System.nanoTime()
        var accumulator = 0.0
        val frameTimeNs = Constants.FRAME_TIME_MS * 1_000_000

        while (running) {
            val currentTime = System.nanoTime()
            val elapsedTime = currentTime - lastTime
            lastTime = currentTime
            accumulator += elapsedTime

            // Fixed timestep updates
            while (accumulator >= frameTimeNs) {
                gameView.update(Constants.DELTA_TIME)
                accumulator -= frameTimeNs
            }

            // Render
            canvas = null
            try {
                // GPU-backed canvas (API 26+) so the sprite blits, gradient shaders,
                // saveLayer and colour filters run on the GPU instead of the CPU.
                // lockCanvas() alone returns a SOFTWARE canvas — far too slow for the
                // pixel-art pipeline. Fall back to software if hardware is unavailable.
                canvas = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    try { surfaceHolder.lockHardwareCanvas() }
                    catch (e: Throwable) { surfaceHolder.lockCanvas() }
                } else {
                    surfaceHolder.lockCanvas()
                }
                synchronized(surfaceHolder) {
                    canvas?.let { gameView.render(it) }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                canvas?.let {
                    try {
                        surfaceHolder.unlockCanvasAndPost(it)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    fun startThread() {
        running = true
        start()
    }

    fun stopThread() {
        running = false
        try {
            join(1000)
        } catch (e: InterruptedException) {
            e.printStackTrace()
        }
    }
}
