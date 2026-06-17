package com.ironfury.laststand.utils

import kotlin.math.sqrt

data class Vector2(var x: Float = 0f, var y: Float = 0f) {

    fun set(x: Float, y: Float): Vector2 {
        this.x = x
        this.y = y
        return this
    }

    fun set(other: Vector2): Vector2 {
        this.x = other.x
        this.y = other.y
        return this
    }

    fun add(other: Vector2): Vector2 {
        x += other.x
        y += other.y
        return this
    }

    fun add(dx: Float, dy: Float): Vector2 {
        x += dx
        y += dy
        return this
    }

    fun subtract(other: Vector2): Vector2 {
        x -= other.x
        y -= other.y
        return this
    }

    fun scale(scalar: Float): Vector2 {
        x *= scalar
        y *= scalar
        return this
    }

    fun length(): Float = sqrt(x * x + y * y)

    fun normalize(): Vector2 {
        val len = length()
        if (len > 0) {
            x /= len
            y /= len
        }
        return this
    }

    fun normalized(): Vector2 {
        val len = length()
        return if (len > 0) Vector2(x / len, y / len) else Vector2()
    }

    fun copy(): Vector2 = Vector2(x, y)

    operator fun plus(other: Vector2): Vector2 = Vector2(x + other.x, y + other.y)
    operator fun minus(other: Vector2): Vector2 = Vector2(x - other.x, y - other.y)
    operator fun times(scalar: Float): Vector2 = Vector2(x * scalar, y * scalar)

    companion object {
        val ZERO get() = Vector2(0f, 0f)
        val UP get() = Vector2(0f, -1f)
        val DOWN get() = Vector2(0f, 1f)
        val LEFT get() = Vector2(-1f, 0f)
        val RIGHT get() = Vector2(1f, 0f)

        fun direction(angle: Direction): Vector2 {
            return when (angle) {
                Direction.RIGHT -> Vector2(1f, 0f)
                Direction.UP_RIGHT -> Vector2(0.707f, -0.707f)
                Direction.UP -> Vector2(0f, -1f)
                Direction.UP_LEFT -> Vector2(-0.707f, -0.707f)
                Direction.LEFT -> Vector2(-1f, 0f)
                Direction.DOWN_LEFT -> Vector2(-0.707f, 0.707f)
                Direction.DOWN -> Vector2(0f, 1f)
                Direction.DOWN_RIGHT -> Vector2(0.707f, 0.707f)
            }
        }
    }
}

enum class Direction {
    RIGHT, UP_RIGHT, UP, UP_LEFT, LEFT, DOWN_LEFT, DOWN, DOWN_RIGHT
}
