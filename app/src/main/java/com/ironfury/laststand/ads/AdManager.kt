package com.ironfury.laststand.ads

import android.app.Activity
import android.content.Context
import android.util.Log

/**
 * Single entry point for showing ads. Build variants / user toggle decide
 * whether a real ad provider is wired in.
 *
 * To enable AdMob for the Play Store release:
 *   1. Add to app/build.gradle.kts:
 *        implementation("com.google.android.gms:play-services-ads:23.6.0")
 *   2. Add the AdMob App ID to AndroidManifest.xml:
 *        <meta-data
 *            android:name="com.google.android.gms.ads.APPLICATION_ID"
 *            android:value="ca-app-pub-XXXXXXXX~YYYYYYYY"/>
 *   3. Replace the body of [showInterstitial] with InterstitialAd.load(...) + show().
 *   4. Replace [showBanner] with an AdView wired into the activity layout.
 *
 * Right now this class is a no-op stub so the rest of the game can call into
 * it freely; production ads are gated by [AdSettings.adsEnabled].
 */
object AdManager {
    private const val TAG = "AdManager"

    fun init(context: Context) {
        // MobileAds.initialize(context) {} — wire up when AdMob is added.
        Log.d(TAG, "AdManager initialized (stub)")
    }

    /** Call at natural break points: after Game Over, between levels. */
    fun showInterstitial(activity: Activity?, placement: String) {
        if (!AdSettings.shouldShowAds(activity?.applicationContext)) return
        Log.d(TAG, "showInterstitial: $placement (stub — no provider wired)")
        // TODO(adsense): load + show InterstitialAd here.
    }

    /** Banner ad management (optional). */
    fun showBanner(activity: Activity?) {
        if (!AdSettings.shouldShowAds(activity?.applicationContext)) return
        Log.d(TAG, "showBanner (stub)")
    }

    fun hideBanner(activity: Activity?) {
        Log.d(TAG, "hideBanner (stub)")
    }
}
