package com.ironfury.laststand.level

import com.ironfury.laststand.utils.Constants

class Camera {
    var x: Float = 0f
        private set

    var levelWidth: Float = Constants.LEVEL_WIDTH

    fun update(playerX: Float) {
        val targetX = playerX - Constants.GAME_WIDTH / 3
        x += (targetX - x) * 0.1f
        x = x.coerceIn(0f, levelWidth - Constants.GAME_WIDTH)
    }

    fun reset() {
        x = 0f
    }

    fun jumpTo(playerX: Float) {
        x = (playerX - Constants.GAME_WIDTH / 3).coerceIn(0f, levelWidth - Constants.GAME_WIDTH)
    }

    fun isVisible(objectX: Float, objectWidth: Float): Boolean {
        return objectX + objectWidth > x && objectX < x + Constants.GAME_WIDTH
    }
}
