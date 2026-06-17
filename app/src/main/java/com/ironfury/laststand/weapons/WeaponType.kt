package com.ironfury.laststand.weapons

import android.graphics.Color

enum class WeaponType(
    val displayName: String,
    val fireRate: Float,        // seconds between shots
    val bulletSpeed: Float,
    val damage: Int,
    val bulletsPerShot: Int,
    val spreadAngle: Float,     // degrees for spread weapons
    val isExplosive: Boolean,
    val explosionRadius: Float,
    val bulletColor: Int,
    val bulletWidth: Float,
    val bulletHeight: Float,
    val iconChar: String        // character to display in UI
) {
    MACHINE_GUN(
        displayName = "Machine Gun",
        fireRate = 0.12f,
        bulletSpeed = 450f,
        damage = 1,
        bulletsPerShot = 1,
        spreadAngle = 0f,
        isExplosive = false,
        explosionRadius = 0f,
        bulletColor = Color.YELLOW,
        bulletWidth = 10f,
        bulletHeight = 4f,
        iconChar = "M"
    ),

    SPREAD_GUN(
        displayName = "Spread Gun",
        fireRate = 0.25f,
        bulletSpeed = 400f,
        damage = 1,
        bulletsPerShot = 5,
        spreadAngle = 15f,
        isExplosive = false,
        explosionRadius = 0f,
        bulletColor = Color.rgb(255, 150, 50),
        bulletWidth = 8f,
        bulletHeight = 4f,
        iconChar = "S"
    ),

    LASER(
        displayName = "Laser",
        fireRate = 0.05f,
        bulletSpeed = 800f,
        damage = 1,
        bulletsPerShot = 1,
        spreadAngle = 0f,
        isExplosive = false,
        explosionRadius = 0f,
        bulletColor = Color.CYAN,
        bulletWidth = 20f,
        bulletHeight = 3f,
        iconChar = "L"
    ),

    ROCKET_LAUNCHER(
        displayName = "Rocket",
        fireRate = 0.8f,
        bulletSpeed = 250f,
        damage = 3,
        bulletsPerShot = 1,
        spreadAngle = 0f,
        isExplosive = true,
        explosionRadius = 60f,
        bulletColor = Color.rgb(255, 100, 50),
        bulletWidth = 16f,
        bulletHeight = 8f,
        iconChar = "R"
    ),

    FLAMETHROWER(
        displayName = "Flame",
        fireRate = 0.03f,
        bulletSpeed = 320f,
        damage = 2,
        bulletsPerShot = 1,
        spreadAngle = 8f,
        isExplosive = false,
        explosionRadius = 0f,
        bulletColor = Color.rgb(255, 100, 0),
        bulletWidth = 12f,
        bulletHeight = 12f,
        iconChar = "F"
    );

    companion object {
        fun getAll(): List<WeaponType> = values().toList()
    }
}
