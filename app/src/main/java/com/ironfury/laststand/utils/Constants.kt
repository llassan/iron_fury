package com.ironfury.laststand.utils

object Constants {
    // Game dimensions (virtual pixels - scaled to screen)
    const val GAME_WIDTH = 480f
    const val GAME_HEIGHT = 270f

    // Frame rate
    const val TARGET_FPS = 60
    const val FRAME_TIME_MS = 1000.0 / TARGET_FPS
    const val DELTA_TIME = 1f / TARGET_FPS

    // Physics
    const val GRAVITY = 1800f
    const val GROUND_Y = 220f

    // Player settings
    const val PLAYER_WIDTH = 32f
    const val PLAYER_HEIGHT = 48f
    const val PLAYER_PRONE_HEIGHT = 20f // hitbox height while lying down (prone)
    const val PLAYER_SPEED = 150f
    const val PLAYER_JUMP_VELOCITY = -500f
    const val PLAYER_MAX_LIVES = 5
    const val PLAYER_INVINCIBILITY_TIME = 2f

    // Bullet settings
    const val BULLET_SPEED = 400f
    const val BULLET_WIDTH = 8f
    const val BULLET_HEIGHT = 4f
    const val PLAYER_FIRE_RATE = 0.15f

    // Enemy settings
    const val ENEMY_WIDTH = 28f
    const val ENEMY_HEIGHT = 44f
    const val ENEMY_SPEED = 60f
    const val ENEMY_FIRE_RATE = 0.9f  // Faster shooting (was 1.5f)
    const val ENEMY_BULLET_SPEED = 220f  // Slightly faster bullets
    const val ENEMY_SCORE = 100

    // Camera
    const val CAMERA_LEAD = 100f

    // Level
    const val LEVEL_WIDTH = 5000f

    // Controls
    const val DPAD_SIZE = 140f
    const val BUTTON_SIZE = 80f
    const val CONTROL_MARGIN = 20f
    const val CONTROL_ALPHA = 150
}

enum class ControlSize(val label: String, val multiplier: Float) {
    SMALL("S", 0.7f),
    MEDIUM("M", 1.0f),
    LARGE("L", 1.35f),
    EXTRA_LARGE("XL", 1.7f)
}
