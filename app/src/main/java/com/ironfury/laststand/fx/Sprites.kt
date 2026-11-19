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

    // ---- Enemy soldier frames on enemies_sheet.png (2-frame walk/aim, face left) ----
    private val enemyRects: Map<String, IntArray> = mapOf(
        "walk0" to intArrayOf(6, 5, 63, 89),
        "walk1" to intArrayOf(65, 5, 125, 89)
    )

    private var assets: android.content.res.AssetManager? = null

    // One biome's parallax layers kept in memory at a time, back (0) to front.
    // Layer count varies per biome (Admurin: 6-9, Vircon32: 4), so this is a list.
    private var bgBiome: String? = null
    private var bgLayers: List<Bitmap> = emptyList()

    /** Decode all layers for [biome] from backgrounds/<biome>/<n>.png (0=back). */
    fun loadBackground(biome: String) {
        if (biome == bgBiome) return
        val am = assets ?: return
        val list = ArrayList<Bitmap>()
        var i = 0
        while (i < 16) {
            // Layers may be .png (Admurin/Vircon32) or .webp (space backdrops).
            val bmp = decodeAsset(am, "backgrounds/$biome/$i.png")
                ?: decodeAsset(am, "backgrounds/$biome/$i.webp")
                ?: break
            list.add(bmp)
            i++
        }
        // No recycle(): the render thread may still hold the previous biome's bitmaps.
        bgLayers = list
        bgBiome = biome
    }

    private fun decodeAsset(am: android.content.res.AssetManager, path: String): Bitmap? =
        try { am.open(path).use { BitmapFactory.decodeStream(it) } } catch (e: java.io.IOException) { null }

    fun bgLayerCount(): Int = bgLayers.size
    fun bgLayer(i: Int): Bitmap? = bgLayers.getOrNull(i)
    fun hasBackground(): Boolean = bgLayers.isNotEmpty()

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

        // Enemy soldiers (Fierce Soldier enemy sheet). They face LEFT in the sheet,
        // so pre-flip to face RIGHT — same base orientation as the hero, so the one
        // drawFeetAnchored() facing-flip rule works for both.
        val enemySheet = decodeAsset(context.assets, "sprites/fierce/enemies_sheet.png")
        if (enemySheet != null) {
            val flip = android.graphics.Matrix().apply { preScale(-1f, 1f) }
            for ((name, r) in enemyRects) {
                val w = r[2] - r[0]
                val h = r[3] - r[1]
                if (w <= 0 || h <= 0) continue
                runCatching {
                    val sub = Bitmap.createBitmap(enemySheet, r[0], r[1], w, h)
                    cache["enemy_$name"] = Bitmap.createBitmap(sub, 0, 0, w, h, flip, false)
                }
            }
        }

        // FX sheets: explosion (10x5 grid of 100x100 = 50 frames) + muzzle (4x square).
        decodeAsset(context.assets, "sprites/fx/explosion.png")?.let { sheet ->
            val cols = 10; val rows = 5
            val fw = sheet.width / cols; val fh = sheet.height / rows
            val frames = ArrayList<Bitmap>()
            for (r in 0 until rows) for (c in 0 until cols) {
                runCatching { frames.add(Bitmap.createBitmap(sheet, c * fw, r * fh, fw, fh)) }
            }
            explosionFrames = frames
        }
        decodeAsset(context.assets, "sprites/fx/muzzle.png")?.let { sheet ->
            val fw = sheet.height
            val n = (sheet.width / fw).coerceAtLeast(1)
            val frames = ArrayList<Bitmap>()
            for (i in 0 until n) runCatching { frames.add(Bitmap.createBitmap(sheet, i * fw, 0, fw, sheet.height)) }
            muzzleFrames = frames
        }

        // On-screen controls (Kenney, CC0).
        dpadSprite = decodeAsset(context.assets, "controls/dpad.png")
        buttonSprite = decodeAsset(context.assets, "controls/button.png")

        ready = cache.isNotEmpty()
    }

    var dpadSprite: Bitmap? = null; private set
    var buttonSprite: Bitmap? = null; private set

    private var explosionFrames: List<Bitmap> = emptyList()
    private var muzzleFrames: List<Bitmap> = emptyList()
    val explosionFrameCount: Int get() = explosionFrames.size
    fun explosionFrame(i: Int): Bitmap? = explosionFrames.getOrNull(i)
    val muzzleFrameCount: Int get() = muzzleFrames.size
    fun muzzleFrame(i: Int): Bitmap? = muzzleFrames.getOrNull(i)

    private val tintPaint = Paint()

    /** Draw [bmp] centred on ([cx],[cy]) at [sizePx] tall, optionally rotated/tinted. */
    fun drawCentered(canvas: Canvas, bmp: Bitmap, cx: Float, cy: Float, sizePx: Float, rotationDeg: Float = 0f, tint: Int? = null) {
        val aspect = bmp.width.toFloat() / bmp.height
        val h = sizePx; val w = sizePx * aspect
        src.set(0, 0, bmp.width, bmp.height)
        dst.set(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        val p = if (tint != null) tintPaint.apply { colorFilter = PorterDuffColorFilter(tint, PorterDuff.Mode.SRC_ATOP) } else paint
        if (rotationDeg != 0f) {
            canvas.save(); canvas.rotate(rotationDeg, cx, cy); canvas.drawBitmap(bmp, src, dst, p); canvas.restore()
        } else {
            canvas.drawBitmap(bmp, src, dst, p)
        }
    }

    fun hero(name: String): Bitmap? = cache["hero_$name"]
    fun enemy(name: String): Bitmap? = cache["enemy_$name"]

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
