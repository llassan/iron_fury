package com.ironfury.laststand.fx

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.Rect
import android.graphics.RectF

/**
 * Bitmap sprite cache for Iron Fury's visual overhaul.
 *
 * Art: "Fierce Soldier" by xiaocaibi (original art by Vircon32), CC-BY 4.0 — see
 * assets/sprites/CREDITS.txt. The three source sheets are loaded once at GameState
 * construction and sliced into named frames; entities draw through [drawFeetAnchored]
 * so they never touch a Context. Anything missing returns null / leaves [ready] false
 * and callers fall back to the original code-drawn vector art.
 *
 * Pixel-art is blitted with NO bitmap filtering (nearest-neighbour) so upscaled
 * pixels stay crisp instead of going blurry.
 */
object Sprites {

    /** True once the hero sheet decoded; entities gate their sprite path on this. */
    @Volatile
    var ready = false
        private set

    private val cache = HashMap<String, Bitmap>()
    private val src = Rect()
    private val dst = RectF()
    // No FILTER_BITMAP_FLAG: keep nearest-neighbour scaling for crisp pixel art.
    private val paint = Paint()
    private val flashPaint = Paint().apply {
        colorFilter = PorterDuffColorFilter(android.graphics.Color.WHITE, PorterDuff.Mode.SRC_ATOP)
    }

    // ---- Hero frame rectangles on hero_sheet.png (pixel-exact, from frame detection) ----
    private val heroRects: Map<String, IntArray> = mapOf(
        // run cycle (gun forward, stride) — row 2 i0..i3
        "run0" to intArrayOf(5, 176, 62, 260),
        "run1" to intArrayOf(67, 178, 124, 260),
        "run2" to intArrayOf(129, 176, 186, 260),
        "run3" to intArrayOf(191, 178, 248, 260),
        // idle / standing aim — row 1 i0, i1
        "idle0" to intArrayOf(5, 90, 62, 171),
        "idle1" to intArrayOf(67, 90, 124, 171),
        // jump (tucked) — row 0 i2
        "jump" to intArrayOf(127, 18, 187, 90),
        // prone / low kneel — row 0 i0
        "prone" to intArrayOf(5, 5, 61, 90),
        // death (lying) — row 1 i4
        "death" to intArrayOf(271, 94, 355, 175),
        // victory (pistol up) — row 2 i4
        "victory" to intArrayOf(292, 175, 337, 260)
    )

    private var assets: android.content.res.AssetManager? = null

    // One biome's 4 parallax layers kept in memory at a time (back=0 .. front=3).
    private var bgBiome: String? = null
    private val bgLayers = arrayOfNulls<Bitmap>(4)

    /** Decode the 4 parallax layers for [biome] on demand; only one biome resident. */
    fun loadBackground(biome: String) {
        if (biome == bgBiome) return
        val am = assets ?: return
        for (i in 0 until 4) {
            bgLayers[i] = try {
                am.open("backgrounds/${biome}_${i + 1}.png").use { BitmapFactory.decodeStream(it) }
            } catch (e: java.io.IOException) {
                null
            }
        }
        // No recycle(): the render thread may still hold the previous biome's bitmaps.
        bgBiome = biome
    }

    fun bgLayer(i: Int): Bitmap? = bgLayers.getOrNull(i)
    fun hasBackground(): Boolean = bgLayers[0] != null

    fun load(context: Context) {
        if (ready) return
        assets = context.assets
        val am = context.assets
        val hero = try {
            am.open("sprites/fierce/hero_sheet.png").use { BitmapFactory.decodeStream(it) }
        } catch (e: java.io.IOException) {
            null
        } ?: return

        for ((name, r) in heroRects) {
            val w = r[2] - r[0]
            val h = r[3] - r[1]
            if (w <= 0 || h <= 0) continue
            runCatching {
                cache["hero_$name"] = Bitmap.createBitmap(hero, r[0], r[1], w, h)
            }
        }
        ready = cache.isNotEmpty()
    }

    fun hero(name: String): Bitmap? = cache["hero_$name"]

    /**
     * Draw [bmp] so its bottom edge sits at ([cx], [feetY]) in screen pixels and its
     * height becomes [targetHpx], centred horizontally on [cx]. Flips horizontally when
     * [facingRight] is false. When [flash] is true the sprite is drawn solid white
     * (damage flash).
     */
    fun drawFeetAnchored(
        canvas: Canvas,
        bmp: Bitmap,
        cx: Float,
        feetY: Float,
        targetHpx: Float,
        facingRight: Boolean,
        flash: Boolean = false
    ) {
        val aspect = bmp.width.toFloat() / bmp.height.toFloat()
        val h = targetHpx
        val w = h * aspect
        src.set(0, 0, bmp.width, bmp.height)
        dst.set(cx - w / 2f, feetY - h, cx + w / 2f, feetY)
        val p = if (flash) flashPaint else paint
        if (facingRight) {
            canvas.drawBitmap(bmp, src, dst, p)
        } else {
            canvas.save()
            canvas.scale(-1f, 1f, cx, feetY)
            canvas.drawBitmap(bmp, src, dst, p)
            canvas.restore()
        }
    }
}
