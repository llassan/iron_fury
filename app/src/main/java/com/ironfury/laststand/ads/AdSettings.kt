package com.ironfury.laststand.ads

import android.content.Context

/**
 * Persistent ad preferences. Stored alongside other settings so the user can
 * opt out of (or back into) ads from the Settings screen.
 *
 * Default behavior:
 *   • Debug builds: ads disabled by default (avoids dev noise).
 *   • Release builds: ads enabled by default — required for the Play Store
 *     monetization plan.
 */
object AdSettings {
    private const val PREFS = "ad_settings"
    private const val KEY_ENABLED = "ads_enabled"

    fun adsEnabled(context: Context?): Boolean {
        if (context == null) return false
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // Default OFF for debuggable builds, ON for release builds (Play Store).
        val isDebuggable = (context.applicationInfo.flags and
            android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0
        val default = !isDebuggable
        return prefs.getBoolean(KEY_ENABLED, default)
    }

    fun setAdsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_ENABLED, enabled).apply()
    }

    /** Whether to actually display an ad right now. */
    fun shouldShowAds(context: Context?): Boolean = adsEnabled(context)
}
