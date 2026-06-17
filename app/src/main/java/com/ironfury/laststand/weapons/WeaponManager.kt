package com.ironfury.laststand.weapons

import android.content.Context
import android.content.SharedPreferences
import com.ironfury.laststand.cosmetics.CharacterSkin

class WeaponManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("weapon_data", Context.MODE_PRIVATE)

    // Weapon prices (in coins) — tuned higher so unlocks feel earned.
    private val weaponPrices = mapOf(
        WeaponType.MACHINE_GUN to 0,        // Free (starter weapon)
        WeaponType.SPREAD_GUN to 800,
        WeaponType.LASER to 1800,
        WeaponType.ROCKET_LAUNCHER to 3500,
        WeaponType.FLAMETHROWER to 2500
    )

    // Coins
    var coins: Int
        get() = prefs.getInt("coins", 0)
        set(value) {
            prefs.edit().putInt("coins", value).apply()
        }

    // High score tracking
    var highScore: Int
        get() = prefs.getInt("high_score", 0)
        set(value) {
            if (value > highScore) {
                prefs.edit().putInt("high_score", value).apply()
            }
        }

    fun isWeaponUnlocked(weapon: WeaponType): Boolean {
        if (weapon == WeaponType.MACHINE_GUN) return true  // Always unlocked
        return prefs.getBoolean("weapon_${weapon.name}", false)
    }

    fun unlockWeapon(weapon: WeaponType): Boolean {
        val price = getWeaponPrice(weapon)
        if (coins >= price && !isWeaponUnlocked(weapon)) {
            coins -= price
            prefs.edit().putBoolean("weapon_${weapon.name}", true).apply()
            return true
        }
        return false
    }

    fun getWeaponPrice(weapon: WeaponType): Int {
        return weaponPrices[weapon] ?: 0
    }

    fun getUnlockedWeapons(): List<WeaponType> {
        return WeaponType.values().filter { isWeaponUnlocked(it) }
    }

    fun addCoins(amount: Int) {
        coins += amount
    }

    // ---- Character skins (cosmetic; share the same coin balance as weapons) ----

    /** The currently equipped character. Defaults to the free RECRUIT. */
    var selectedCharacter: CharacterSkin
        get() = CharacterSkin.fromId(prefs.getString("character", null))
        set(value) { prefs.edit().putString("character", value.name).apply() }

    fun isCharacterUnlocked(skin: CharacterSkin): Boolean {
        if (skin.isDefault) return true   // RECRUIT is always available
        return prefs.getBoolean("character_${skin.name}", false)
    }

    /** Buy [skin] if it's affordable and not already owned. Returns true on purchase. */
    fun unlockCharacter(skin: CharacterSkin): Boolean {
        if (coins >= skin.price && !isCharacterUnlocked(skin)) {
            coins -= skin.price
            prefs.edit().putBoolean("character_${skin.name}", true).apply()
            return true
        }
        return false
    }

    // Reset all progress (for testing)
    fun resetProgress() {
        prefs.edit().clear().apply()
    }
}
