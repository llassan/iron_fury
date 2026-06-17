package com.ironfury.laststand

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.ironfury.laststand.audio.MusicManager
import com.ironfury.laststand.input.ControlLayoutEditor
import com.ironfury.laststand.input.TouchController
import com.ironfury.laststand.utils.ControlSize
import com.ironfury.laststand.utils.SettingsManager

class GameView(context: Context) : SurfaceView(context), SurfaceHolder.Callback {

    private var gameThread: GameThread? = null
    private var gameState: GameState? = null
    private var touchController: TouchController? = null
    private val settingsManager = SettingsManager(context)

    private var screenWidth = 0
    private var screenHeight = 0

    init {
        holder.addCallback(this)
        isFocusable = true
        isFocusableInTouchMode = true
        requestFocus()
    }

    override fun surfaceCreated(holder: SurfaceHolder) {}

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        screenWidth = width
        screenHeight = height

        if (touchController == null) {
            touchController = TouchController(width, height, settingsManager.controlSize.multiplier, settingsManager)
        }

        if (gameState == null) {
            gameState = GameState(context)
        }

        gameState?.initWeaponSelector(width, height)

        if (gameThread == null || !gameThread!!.running) {
            gameThread = GameThread(holder, this)
            gameThread?.startThread()
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        gameThread?.stopThread()
        gameThread = null
    }

    private fun applyControlSize(size: ControlSize) {
        settingsManager.controlSize = size
        touchController = TouchController(screenWidth, screenHeight, size.multiplier, settingsManager)
    }

    fun enterLayoutEditor() {
        touchController?.let { tc ->
            gameState?.status?.let {
                layoutEditor = ControlLayoutEditor(screenWidth, screenHeight, tc, settingsManager)
            }
        }
    }

    fun exitLayoutEditor() {
        layoutEditor?.save()
        layoutEditor = null
        // Recreate controller with new positions
        touchController = TouchController(screenWidth, screenHeight, settingsManager.controlSize.multiplier, settingsManager)
    }

    fun resetControlLayout() {
        settingsManager.resetLayout()
        touchController = TouchController(screenWidth, screenHeight, settingsManager.controlSize.multiplier, settingsManager)
    }

    private var layoutEditor: ControlLayoutEditor? = null

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // Layout editor handles all touch when active
        layoutEditor?.let { editor ->
            editor.onTouchEvent(event)
            if (editor.isDone) {
                if (editor.shouldReset) resetControlLayout()
                exitLayoutEditor()
                gameState?.closeSettings()
            }
            return true
        }

        if (event.action == MotionEvent.ACTION_DOWN) {
            gameState?.let { state ->
                // Start screen — check gear button first, then start game
                if (state.status == GameStatus.START_SCREEN) {
                    if (state.isSettingsButtonTap(event.x, event.y)) {
                        state.openSettings()
                    } else if (state.isCharactersButtonTap(event.x, event.y)) {
                        state.openCharacters()
                    } else {
                        state.startGame()
                    }
                    return true
                }

                // Character select — equip/buy a character or go back
                if (state.status == GameStatus.CHARACTERS) {
                    state.handleCharacterTap(event.x, event.y)
                    return true
                }

                // Settings screen — handle size selection or layout editor
                if (state.status == GameStatus.SETTINGS) {
                    val size = state.handleSettingsTap(event.x, event.y)
                    if (size != null) {
                        applyControlSize(size)
                    }
                    if (state.wantsLayoutEditor) {
                        state.wantsLayoutEditor = false
                        enterLayoutEditor()
                    }
                    return true
                }

                if (state.status == GameStatus.LEVEL_COMPLETE) {
                    state.advanceLevel()
                    return true
                }

                if (state.status == GameStatus.GAME_OVER) {
                    // All lives lost — retry the SAME level fresh (not the checkpoint, not level 1).
                    state.restartCurrentLevel()
                    return true
                }
                if (state.status == GameStatus.VICTORY) {
                    state.restart()
                    return true
                }

                if (state.handleWeaponTouch(event.x, event.y)) {
                    return true
                }
            }
        }

        gameState?.let { state ->
            if (state.status == GameStatus.PLAYING) {
                touchController?.onTouchEvent(event)
            }
        }

        return true
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        gameState?.let { state ->
            if (state.status == GameStatus.START_SCREEN) {
                state.startGame()
                return true
            }
            if (state.status == GameStatus.SETTINGS) {
                state.closeSettings()
                return true
            }
            if (state.status == GameStatus.CHARACTERS) {
                state.closeCharacters()
                return true
            }
            if (state.status == GameStatus.LEVEL_COMPLETE) {
                state.advanceLevel()
                return true
            }
            if (state.status == GameStatus.GAME_OVER) {
                // All lives lost — retry the SAME level fresh (not the checkpoint, not level 1).
                state.restartCurrentLevel()
                return true
            }
            if (state.status == GameStatus.VICTORY) {
                state.restart()
                return true
            }
        }
        if (touchController?.onKeyEvent(keyCode, true) == true) return true
        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        if (touchController?.onKeyEvent(keyCode, false) == true) return true
        return super.onKeyUp(keyCode, event)
    }

    fun update(deltaTime: Float) {
        touchController?.let { controller ->
            gameState?.update(deltaTime, controller)
        }
    }

    fun render(canvas: Canvas) {
        canvas.drawColor(Color.BLACK)
        layoutEditor?.let { it.render(canvas); return }
        touchController?.let { controller ->
            gameState?.render(canvas, screenWidth, screenHeight, controller)
        }
    }

    fun pause() {
        gameThread?.stopThread()
        MusicManager.stop()
    }

    fun resume() {
        if (screenWidth > 0 && screenHeight > 0 && gameThread?.running != true) {
            gameThread = GameThread(holder, this)
            gameThread?.startThread()

            gameState?.let { state ->
                when {
                    state.status != GameStatus.PLAYING -> {}
                    state.boss?.isActive == true -> MusicManager.play(MusicManager.Music.BOSS_BATTLE)
                    else -> MusicManager.play(MusicManager.Music.GAMEPLAY)
                }
            }
        }
    }
}
