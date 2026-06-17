package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.RectF
import com.ironfury.laststand.utils.Vector2

abstract class Entity(
    var position: Vector2 = Vector2(),
    var velocity: Vector2 = Vector2(),
    var width: Float = 0f,
    var height: Float = 0f
) {
    var isActive: Boolean = true

    val hitbox: RectF
        get() = RectF(
            position.x,
            position.y,
            position.x + width,
            position.y + height
        )

    val centerX: Float get() = position.x + width / 2
    val centerY: Float get() = position.y + height / 2

    open fun update(deltaTime: Float) {
        position.x += velocity.x * deltaTime
        position.y += velocity.y * deltaTime
    }

    abstract fun render(canvas: Canvas, cameraX: Float, scale: Float)

    fun collidesWith(other: Entity): Boolean {
        if (!isActive || !other.isActive) return false
        return hitbox.intersect(other.hitbox)
    }

    fun collidesWithRect(rect: RectF): Boolean {
        if (!isActive) return false
        return hitbox.intersect(rect)
    }
}
