package com.ironfury.laststand.utils

import android.content.Context

class SettingsManager(context: Context) {
    private val prefs = context.getSharedPreferences("game_settings", Context.MODE_PRIVATE)

    var controlSize: ControlSize
        get() {
            val name = prefs.getString("control_size", ControlSize.MEDIUM.name)
            return try { ControlSize.valueOf(name ?: ControlSize.MEDIUM.name) } catch (_: Exception) { ControlSize.MEDIUM }
        }
        set(value) {
            prefs.edit().putString("control_size", value.name).apply()
        }

    // Custom control positions (normalized 0..1 relative to screen)
    fun saveDpadPosition(nx: Float, ny: Float) {
        prefs.edit().putFloat("dpad_nx", nx).putFloat("dpad_ny", ny).putBoolean("custom_layout", true).apply()
    }

    fun saveJumpPosition(nx: Float, ny: Float) {
        prefs.edit().putFloat("jump_nx", nx).putFloat("jump_ny", ny).putBoolean("custom_layout", true).apply()
    }

    fun saveFirePosition(nx: Float, ny: Float) {
        prefs.edit().putFloat("fire_nx", nx).putFloat("fire_ny", ny).putBoolean("custom_layout", true).apply()
    }

    val hasCustomLayout: Boolean get() = prefs.getBoolean("custom_layout", false)

    fun getDpadNx(): Float = prefs.getFloat("dpad_nx", -1f)
    fun getDpadNy(): Float = prefs.getFloat("dpad_ny", -1f)
    fun getJumpNx(): Float = prefs.getFloat("jump_nx", -1f)
    fun getJumpNy(): Float = prefs.getFloat("jump_ny", -1f)
    fun getFireNx(): Float = prefs.getFloat("fire_nx", -1f)
    fun getFireNy(): Float = prefs.getFloat("fire_ny", -1f)

    fun resetLayout() {
        prefs.edit()
            .remove("dpad_nx").remove("dpad_ny")
            .remove("jump_nx").remove("jump_ny")
            .remove("fire_nx").remove("fire_ny")
            .remove("custom_layout")
            .apply()
    }
}
