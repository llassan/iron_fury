package com.ironfury.laststand.cosmetics

import android.graphics.Color

/**
 * Selectable player characters. Each entry is a self-contained colour palette
 * applied to the in-game player sprite ([com.ironfury.laststand.entities.Player.applySkin])
 * and the start-screen / select-screen previews.
 *
 * [RECRUIT] is the free default (price 0) — everything else is locked until the
 * player buys it with coins, mirroring the weapon-unlock economy in
 * [com.ironfury.laststand.weapons.WeaponManager].
 */
enum class CharacterSkin(
    val displayName: String,
    val price: Int,
    val skin: Int,
    val hair: Int,
    val shirt: Int,
    val shirtHi: Int,
    val pants: Int,
    val pantsHi: Int,
    val boots: Int,
    val bandana: Int
) {
    RECRUIT(
        displayName = "RECRUIT",
        price = 0,
        skin = Color.rgb(255, 200, 150),
        hair = Color.rgb(40, 30, 20),
        shirt = Color.rgb(30, 60, 120),
        shirtHi = Color.rgb(50, 90, 160),
        pants = Color.rgb(40, 80, 40),
        pantsHi = Color.rgb(60, 110, 60),
        boots = Color.rgb(60, 40, 30),
        bandana = Color.rgb(180, 40, 40)
    ),
    COMMANDO(
        displayName = "COMMANDO",
        price = 1200,
        skin = Color.rgb(225, 180, 140),
        hair = Color.rgb(50, 40, 25),
        shirt = Color.rgb(70, 80, 45),
        shirtHi = Color.rgb(98, 108, 62),
        pants = Color.rgb(58, 64, 40),
        pantsHi = Color.rgb(84, 90, 55),
        boots = Color.rgb(45, 38, 28),
        bandana = Color.rgb(95, 30, 30)
    ),
    ARCTIC_OPS(
        displayName = "ARCTIC OPS",
        price = 2000,
        skin = Color.rgb(240, 210, 180),
        hair = Color.rgb(70, 70, 80),
        shirt = Color.rgb(200, 210, 225),
        shirtHi = Color.rgb(232, 240, 250),
        pants = Color.rgb(150, 165, 185),
        pantsHi = Color.rgb(182, 196, 212),
        boots = Color.rgb(80, 90, 100),
        bandana = Color.rgb(60, 150, 200)
    ),
    NIGHT_OWL(
        displayName = "NIGHT OWL",
        price = 2800,
        skin = Color.rgb(210, 175, 150),
        hair = Color.rgb(20, 20, 25),
        shirt = Color.rgb(35, 35, 52),
        shirtHi = Color.rgb(62, 62, 88),
        pants = Color.rgb(28, 28, 40),
        pantsHi = Color.rgb(52, 52, 72),
        boots = Color.rgb(20, 20, 28),
        bandana = Color.rgb(130, 70, 210)
    ),
    DESERT_FOX(
        displayName = "DESERT FOX",
        price = 3500,
        skin = Color.rgb(235, 195, 150),
        hair = Color.rgb(120, 90, 50),
        shirt = Color.rgb(200, 175, 120),
        shirtHi = Color.rgb(226, 202, 150),
        pants = Color.rgb(180, 155, 105),
        pantsHi = Color.rgb(206, 182, 132),
        boots = Color.rgb(120, 95, 60),
        bandana = Color.rgb(205, 140, 60)
    ),
    CRIMSON_ELITE(
        displayName = "CRIMSON ELITE",
        price = 5000,
        skin = Color.rgb(245, 205, 165),
        hair = Color.rgb(30, 25, 25),
        shirt = Color.rgb(120, 30, 35),
        shirtHi = Color.rgb(172, 50, 56),
        pants = Color.rgb(45, 40, 45),
        pantsHi = Color.rgb(72, 64, 70),
        boots = Color.rgb(30, 28, 30),
        bandana = Color.rgb(225, 45, 45)
    );

    val isDefault: Boolean get() = price == 0

    companion object {
        /** Resolve a stored id back to a skin, defaulting to [RECRUIT]. */
        fun fromId(id: String?): CharacterSkin =
            entries.firstOrNull { it.name == id } ?: RECRUIT
    }
}
