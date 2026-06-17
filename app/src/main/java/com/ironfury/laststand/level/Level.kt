package com.ironfury.laststand.level

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RadialGradient
import android.graphics.Shader
import com.ironfury.laststand.utils.Constants
import kotlin.math.sin

data class EnemySpawn(
    val x: Float,
    val patrolLeft: Float,
    val patrolRight: Float,
    val surfaceY: Float = Constants.GROUND_Y, // top of the surface the enemy stands on
    var spawned: Boolean = false
)

data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float = 20f
)

data class LevelTheme(
    val name: String,
    val bossName: String,
    val levelWidth: Float,
    // Sky gradient (top to bottom)
    val skyColors: IntArray,
    val skyPositions: FloatArray,
    // Sun
    val sunColor: Int,
    val sunGlowColor: Int,
    val showSun: Boolean = true,
    // Clouds
    val cloudColor: Int,
    val cloudAlpha: Int = 200,
    // Far mountains
    val farMountainColor: Int,
    val snowCapColor: Int = Color.argb(180, 255, 255, 255),
    val showSnowCaps: Boolean = true,
    // Near mountains
    val nearMountainColor: Int,
    val nearMountainHighlight: Int,
    // Trees
    val trunkColor: Int = Color.rgb(80, 55, 35),
    val foliageColors: IntArray, // 3 colors for 3 tree types
    val foliageHighlights: IntArray,
    // Ground
    val grassTopColor: Int,
    val grassBottomColor: Int,
    val dirtTopColor: Int,
    val dirtBottomColor: Int,
    val grassTuftColor: Int,
    // Platforms
    val platformBodyColor: Int = Color.rgb(100, 70, 45),
    val platformTopGradientTop: Int = Color.rgb(90, 150, 65),
    val platformTopGradientBottom: Int = Color.rgb(70, 120, 50),
    val platformGrainColor: Int = Color.rgb(80, 55, 35),
    // Boss health scaling
    val bossHealthMultiplier: Float = 1f
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LevelTheme) return false
        return name == other.name
    }

    override fun hashCode(): Int = name.hashCode()
}

class Level(levelNumber: Int = 1) {
    var currentLevel = levelNumber
        private set

    val theme: LevelTheme get() = LevelThemes.get(currentLevel)
    val width: Float get() = theme.levelWidth

    val enemySpawns = mutableListOf<EnemySpawn>()
    val platforms = mutableListOf<Platform>()

    private val paint = Paint().apply {
        isAntiAlias = true
    }

    private var skyGradient: LinearGradient? = null
    private var lastScreenWidth = 0
    private var lastScreenHeight = 0

    private val clouds = mutableListOf<FloatArray>()
    private val farMountains = mutableListOf<FloatArray>()
    private val nearMountains = mutableListOf<FloatArray>()
    private val trees = mutableListOf<FloatArray>()

    init {
        buildLevel()
    }

    fun loadLevel(levelNum: Int) {
        currentLevel = levelNum
        enemySpawns.clear()
        platforms.clear()
        clouds.clear()
        farMountains.clear()
        nearMountains.clear()
        trees.clear()
        skyGradient = null
        lastScreenWidth = 0
        lastScreenHeight = 0
        buildLevel()
    }

    private fun buildLevel() {
        setupLevel(currentLevel)
        addPlatformEnemies()
        generateClouds()
        generateMountains()
        generateTrees()
    }

    // Sprinkle some enemies onto platform tops so they don't all sit at ground
    // level. Runs after each level's platforms/spawns are defined, so it applies
    // to every level without editing the hand-authored spawn tables.
    private fun addPlatformEnemies() {
        for (p in platforms) {
            // Platform must be wide enough to host (and let it patrol on) an enemy.
            if (p.width < Constants.ENEMY_WIDTH + 12f) continue
            // Only ~35% of eligible platforms get a rooftop guard.
            if (Math.random() > 0.35) continue
            val left = p.x
            val right = p.x + p.width
            enemySpawns.add(
                EnemySpawn(
                    x = (left + right) / 2f - Constants.ENEMY_WIDTH / 2f,
                    patrolLeft = left,
                    patrolRight = right,
                    surfaceY = p.y
                )
            )
        }
    }

    private fun setupLevel(level: Int) {
        val g = Constants.GROUND_Y
        when (level) {
            1 -> setupLevel1(g)
            2 -> setupLevel2(g)
            3 -> setupLevel3(g)
            4 -> setupLevel4(g)
            5 -> setupLevel5(g)
            6 -> setupLevel6(g)
            7 -> setupLevel7(g)
            8 -> setupLevel8(g)
            9 -> setupLevel9(g)
            10 -> setupLevel10(g)
            in 11..20 -> setupProceduralLevel(level, g)
        }
    }

    // ============================================================
    // LEVELS 11-20: EXTENDED CAMPAIGN — procedurally generated from a
    // per-level seed so each layout is fixed and fair across retries, while
    // scaling enemy density and platform challenge with the level number.
    // ============================================================
    private fun setupProceduralLevel(level: Int, g: Float) {
        val width = LevelThemes.get(level).levelWidth

        // Deterministic PRNG seeded by the level number (LCG). Same level always
        // produces the same geometry — important so retrying a level isn't a
        // brand-new random map each time.
        var seed = (level.toLong() * 2654435761L) and 0x7FFFFFFFL
        fun rnd(): Float {
            seed = (seed * 1103515245L + 12345L) and 0x7FFFFFFFL
            return seed.toFloat() / 0x7FFFFFFF.toFloat()
        }
        fun range(min: Float, max: Float) = min + rnd() * (max - min)

        // Difficulty ramps 1..10 across levels 11..20.
        val diff = level - 10
        val enemySpacing = (260f - diff * 8f).coerceAtLeast(150f)
        val endX = width - 500f  // leave a clear arena for the boss at the end

        // Ground troops spread across the whole level.
        var ex = 320f
        while (ex < endX) {
            val patrol = range(80f, 170f)
            enemySpawns.add(EnemySpawn(ex, ex - patrol * 0.5f, ex + patrol * 0.5f))
            ex += range(enemySpacing * 0.8f, enemySpacing * 1.2f)
        }

        // Platforms in small clusters with staircase tendencies and varied
        // heights. addPlatformEnemies() garrisons a fraction of them with guards.
        var px = 300f
        while (px < endX) {
            val cluster = range(1f, 4.99f).toInt().coerceIn(1, 4)
            var ph = range(45f, 130f)
            for (i in 0 until cluster) {
                val pw = range(40f, 110f)
                platforms.add(Platform(px, g - ph, pw))
                px += pw + range(40f, 120f)
                ph = (ph + range(-30f, 45f)).coerceIn(40f, 150f)
                if (px >= endX) break
            }
            px += range(80f, 200f)
        }
    }

    // ============================================================
    // LEVEL 1: FOREST DAWN - Gentle introduction
    // ============================================================
    private fun setupLevel1(g: Float) {
        enemySpawns.apply {
            add(EnemySpawn(400f, 350f, 500f))
            add(EnemySpawn(650f, 600f, 750f))
            add(EnemySpawn(900f, 850f, 1000f))
            add(EnemySpawn(1150f, 1100f, 1250f))
            add(EnemySpawn(1300f, 1250f, 1400f))
            add(EnemySpawn(1500f, 1450f, 1600f))
            add(EnemySpawn(1700f, 1650f, 1800f))
            add(EnemySpawn(1950f, 1900f, 2050f))
            add(EnemySpawn(2100f, 2050f, 2200f))
            add(EnemySpawn(2300f, 2250f, 2400f))
            add(EnemySpawn(2550f, 2500f, 2650f))
            add(EnemySpawn(2700f, 2650f, 2800f))
            add(EnemySpawn(2850f, 2800f, 2950f))
            add(EnemySpawn(3000f, 2950f, 3100f))
            add(EnemySpawn(3250f, 3200f, 3350f))
            add(EnemySpawn(3450f, 3400f, 3550f))
            add(EnemySpawn(3650f, 3600f, 3750f))
            add(EnemySpawn(3900f, 3850f, 4000f))
            add(EnemySpawn(4100f, 4050f, 4200f))
            add(EnemySpawn(4300f, 4250f, 4400f))
            add(EnemySpawn(4500f, 4450f, 4600f))
            add(EnemySpawn(4700f, 4650f, 4850f))
        }
        platforms.apply {
            add(Platform(300f, g - 60f, 100f))
            add(Platform(500f, g - 100f, 80f))
            add(Platform(800f, g - 80f, 120f))
            add(Platform(1100f, g - 70f, 90f))
            add(Platform(1350f, g - 90f, 100f))
            add(Platform(1600f, g - 60f, 110f))
            add(Platform(1850f, g - 50f, 80f))
            add(Platform(1980f, g - 80f, 80f))
            add(Platform(2110f, g - 110f, 80f))
            add(Platform(2240f, g - 80f, 80f))
            add(Platform(2500f, g - 70f, 100f))
            add(Platform(2750f, g - 90f, 90f))
            add(Platform(3000f, g - 60f, 110f))
            add(Platform(3250f, g - 100f, 70f))
            add(Platform(3400f, g - 70f, 70f))
            add(Platform(3550f, g - 110f, 70f))
            add(Platform(3700f, g - 80f, 70f))
            add(Platform(3950f, g - 60f, 100f))
            add(Platform(4200f, g - 90f, 90f))
            add(Platform(4450f, g - 70f, 100f))
            add(Platform(4700f, g - 50f, 120f))
        }
    }

    // ============================================================
    // LEVEL 2: DESERT STORM - Scorching heat, wide open, cacti gaps
    // ============================================================
    private fun setupLevel2(g: Float) {
        enemySpawns.apply {
            // Scattered across open desert — enemies have wider patrol ranges
            add(EnemySpawn(350f, 280f, 500f))
            add(EnemySpawn(700f, 600f, 850f))
            add(EnemySpawn(1000f, 900f, 1150f))
            add(EnemySpawn(1350f, 1250f, 1500f))
            add(EnemySpawn(1550f, 1450f, 1700f))
            add(EnemySpawn(1800f, 1700f, 1950f))
            add(EnemySpawn(2050f, 1950f, 2200f))
            add(EnemySpawn(2350f, 2250f, 2500f))
            add(EnemySpawn(2600f, 2500f, 2750f))
            add(EnemySpawn(2850f, 2750f, 3000f))
            add(EnemySpawn(3100f, 3000f, 3250f))
            add(EnemySpawn(3400f, 3300f, 3550f))
            add(EnemySpawn(3650f, 3550f, 3800f))
            add(EnemySpawn(3900f, 3800f, 4050f))
            add(EnemySpawn(4150f, 4050f, 4300f))
            add(EnemySpawn(4400f, 4300f, 4550f))
            add(EnemySpawn(4650f, 4550f, 4800f))
            add(EnemySpawn(4900f, 4800f, 5050f))
        }
        platforms.apply {
            // Rocky desert outcrops — wider gaps force jumping
            add(Platform(400f, g - 50f, 70f))
            add(Platform(600f, g - 80f, 60f))
            add(Platform(900f, g - 60f, 90f))
            add(Platform(1200f, g - 90f, 50f)) // narrow!
            add(Platform(1400f, g - 70f, 80f))
            add(Platform(1700f, g - 100f, 60f))
            add(Platform(2000f, g - 55f, 100f))
            add(Platform(2300f, g - 85f, 55f))
            add(Platform(2550f, g - 65f, 70f))
            add(Platform(2800f, g - 100f, 50f))
            // Oasis area — staircase up and down
            add(Platform(3050f, g - 50f, 80f))
            add(Platform(3180f, g - 80f, 80f))
            add(Platform(3310f, g - 110f, 80f))
            add(Platform(3440f, g - 80f, 80f))
            add(Platform(3570f, g - 50f, 80f))
            add(Platform(3850f, g - 70f, 90f))
            add(Platform(4150f, g - 90f, 70f))
            add(Platform(4450f, g - 60f, 110f))
            add(Platform(4750f, g - 50f, 100f))
        }
    }

    // ============================================================
    // LEVEL 3: FROZEN PEAKS - Ice and snow, tricky narrow platforms
    // ============================================================
    private fun setupLevel3(g: Float) {
        enemySpawns.apply {
            add(EnemySpawn(300f, 250f, 400f))
            add(EnemySpawn(550f, 500f, 650f))
            add(EnemySpawn(800f, 750f, 900f))
            add(EnemySpawn(1050f, 1000f, 1150f))
            add(EnemySpawn(1250f, 1200f, 1350f))
            add(EnemySpawn(1450f, 1400f, 1550f))
            add(EnemySpawn(1650f, 1600f, 1750f))
            add(EnemySpawn(1850f, 1800f, 1950f))
            add(EnemySpawn(2050f, 2000f, 2150f))
            add(EnemySpawn(2250f, 2200f, 2350f))
            add(EnemySpawn(2500f, 2450f, 2600f))
            add(EnemySpawn(2700f, 2650f, 2800f))
            add(EnemySpawn(2900f, 2850f, 3000f))
            add(EnemySpawn(3150f, 3100f, 3250f))
            add(EnemySpawn(3350f, 3300f, 3450f))
            add(EnemySpawn(3600f, 3550f, 3700f))
            add(EnemySpawn(3850f, 3800f, 3950f))
            add(EnemySpawn(4100f, 4050f, 4200f))
            add(EnemySpawn(4350f, 4300f, 4450f))
            add(EnemySpawn(4600f, 4550f, 4700f))
            add(EnemySpawn(4850f, 4800f, 4950f))
            add(EnemySpawn(5100f, 5050f, 5200f))
            add(EnemySpawn(5300f, 5250f, 5400f))
        }
        platforms.apply {
            // Icy ledges — many narrow, some high
            add(Platform(250f, g - 60f, 70f))
            add(Platform(420f, g - 100f, 55f))
            add(Platform(580f, g - 70f, 60f))
            add(Platform(750f, g - 110f, 50f))
            add(Platform(920f, g - 80f, 65f))
            // Frozen stairway up
            add(Platform(1100f, g - 50f, 60f))
            add(Platform(1210f, g - 75f, 60f))
            add(Platform(1320f, g - 100f, 60f))
            add(Platform(1430f, g - 125f, 60f))
            add(Platform(1540f, g - 100f, 60f))
            add(Platform(1650f, g - 75f, 60f))
            // Ice bridge section
            add(Platform(1850f, g - 90f, 40f))
            add(Platform(1950f, g - 90f, 40f))
            add(Platform(2050f, g - 90f, 40f))
            add(Platform(2150f, g - 90f, 40f))
            // Descending ledges
            add(Platform(2400f, g - 110f, 55f))
            add(Platform(2550f, g - 85f, 55f))
            add(Platform(2700f, g - 60f, 80f))
            // Final climb
            add(Platform(3000f, g - 70f, 70f))
            add(Platform(3200f, g - 100f, 60f))
            add(Platform(3450f, g - 80f, 75f))
            add(Platform(3700f, g - 110f, 55f))
            add(Platform(3950f, g - 70f, 80f))
            add(Platform(4250f, g - 90f, 65f))
            add(Platform(4550f, g - 60f, 90f))
            add(Platform(4850f, g - 80f, 70f))
            add(Platform(5150f, g - 50f, 100f))
        }
    }

    // ============================================================
    // LEVEL 4: VOLCANIC DEPTHS - Lava and fire, aggressive enemies
    // ============================================================
    private fun setupLevel4(g: Float) {
        enemySpawns.apply {
            // Dense enemy placement — volcanic lair is well-guarded
            add(EnemySpawn(350f, 300f, 450f))
            add(EnemySpawn(550f, 500f, 650f))
            add(EnemySpawn(750f, 700f, 850f))
            add(EnemySpawn(950f, 900f, 1050f))
            add(EnemySpawn(1100f, 1050f, 1200f))
            add(EnemySpawn(1300f, 1250f, 1400f))
            add(EnemySpawn(1450f, 1400f, 1550f))
            add(EnemySpawn(1650f, 1600f, 1750f))
            add(EnemySpawn(1850f, 1800f, 1950f))
            add(EnemySpawn(2000f, 1950f, 2100f))
            add(EnemySpawn(2200f, 2150f, 2300f))
            add(EnemySpawn(2400f, 2350f, 2500f))
            add(EnemySpawn(2600f, 2550f, 2700f))
            add(EnemySpawn(2800f, 2750f, 2900f))
            add(EnemySpawn(3000f, 2950f, 3100f))
            add(EnemySpawn(3200f, 3150f, 3300f))
            add(EnemySpawn(3450f, 3400f, 3550f))
            add(EnemySpawn(3700f, 3650f, 3800f))
            add(EnemySpawn(3900f, 3850f, 4000f))
            add(EnemySpawn(4150f, 4100f, 4250f))
            add(EnemySpawn(4400f, 4350f, 4500f))
            add(EnemySpawn(4650f, 4600f, 4750f))
            add(EnemySpawn(4900f, 4850f, 5000f))
            add(EnemySpawn(5150f, 5100f, 5250f))
            add(EnemySpawn(5400f, 5350f, 5550f))
        }
        platforms.apply {
            // Rock pillars over lava
            add(Platform(300f, g - 70f, 80f))
            add(Platform(500f, g - 55f, 100f))
            add(Platform(750f, g - 90f, 60f))
            add(Platform(950f, g - 65f, 70f))
            // Rising lava escape — ascending fast
            add(Platform(1150f, g - 50f, 70f))
            add(Platform(1280f, g - 80f, 65f))
            add(Platform(1400f, g - 110f, 60f))
            add(Platform(1530f, g - 130f, 55f))
            add(Platform(1660f, g - 110f, 65f))
            add(Platform(1780f, g - 80f, 70f))
            // Zigzag over fire pits
            add(Platform(2000f, g - 60f, 60f))
            add(Platform(2120f, g - 100f, 55f))
            add(Platform(2250f, g - 60f, 60f))
            add(Platform(2380f, g - 100f, 55f))
            add(Platform(2510f, g - 60f, 60f))
            // Caldera rim
            add(Platform(2750f, g - 80f, 90f))
            add(Platform(3000f, g - 70f, 80f))
            add(Platform(3250f, g - 95f, 65f))
            add(Platform(3500f, g - 75f, 85f))
            add(Platform(3800f, g - 60f, 100f))
            add(Platform(4100f, g - 90f, 70f))
            add(Platform(4400f, g - 70f, 80f))
            add(Platform(4700f, g - 85f, 75f))
            add(Platform(5000f, g - 55f, 110f))
            add(Platform(5300f, g - 70f, 90f))
        }
    }

    // ============================================================
    // LEVEL 5: NEON CITY - Cyberpunk rooftops, electric atmosphere
    // ============================================================
    private fun setupLevel5(g: Float) {
        enemySpawns.apply {
            add(EnemySpawn(400f, 350f, 520f))
            add(EnemySpawn(700f, 620f, 800f))
            add(EnemySpawn(950f, 880f, 1050f))
            add(EnemySpawn(1200f, 1120f, 1300f))
            add(EnemySpawn(1400f, 1350f, 1500f))
            add(EnemySpawn(1600f, 1550f, 1700f))
            add(EnemySpawn(1850f, 1800f, 1950f))
            add(EnemySpawn(2050f, 2000f, 2150f))
            add(EnemySpawn(2250f, 2200f, 2350f))
            add(EnemySpawn(2500f, 2450f, 2600f))
            add(EnemySpawn(2700f, 2650f, 2800f))
            add(EnemySpawn(2950f, 2900f, 3050f))
            add(EnemySpawn(3150f, 3100f, 3250f))
            add(EnemySpawn(3400f, 3350f, 3500f))
            add(EnemySpawn(3650f, 3600f, 3750f))
            add(EnemySpawn(3900f, 3850f, 4000f))
            add(EnemySpawn(4150f, 4100f, 4250f))
            add(EnemySpawn(4400f, 4350f, 4500f))
            add(EnemySpawn(4650f, 4600f, 4750f))
            add(EnemySpawn(4900f, 4850f, 5050f))
            add(EnemySpawn(5150f, 5100f, 5300f))
            add(EnemySpawn(5400f, 5350f, 5550f))
            add(EnemySpawn(5650f, 5600f, 5750f))
            add(EnemySpawn(5900f, 5850f, 6050f))
        }
        platforms.apply {
            // Rooftop-to-rooftop jumping, neon signs as platforms
            add(Platform(350f, g - 55f, 120f))
            add(Platform(550f, g - 85f, 70f))
            add(Platform(750f, g - 60f, 90f))
            add(Platform(1000f, g - 100f, 60f))
            // Skyscraper staircase
            add(Platform(1200f, g - 50f, 65f))
            add(Platform(1320f, g - 70f, 65f))
            add(Platform(1440f, g - 90f, 65f))
            add(Platform(1560f, g - 110f, 65f))
            add(Platform(1680f, g - 130f, 65f))
            // Drop down
            add(Platform(1900f, g - 100f, 50f))
            add(Platform(2050f, g - 70f, 50f))
            // Billboard ledges — narrow
            add(Platform(2250f, g - 55f, 45f))
            add(Platform(2380f, g - 80f, 45f))
            add(Platform(2510f, g - 55f, 45f))
            add(Platform(2640f, g - 80f, 45f))
            add(Platform(2770f, g - 55f, 45f))
            // Wide neon rooftop
            add(Platform(3000f, g - 65f, 140f))
            add(Platform(3300f, g - 90f, 80f))
            add(Platform(3550f, g - 70f, 100f))
            // High-rise gap jumps
            add(Platform(3850f, g - 105f, 55f))
            add(Platform(4050f, g - 80f, 55f))
            add(Platform(4250f, g - 105f, 55f))
            add(Platform(4450f, g - 80f, 55f))
            add(Platform(4700f, g - 60f, 110f))
            add(Platform(5000f, g - 85f, 80f))
            add(Platform(5300f, g - 65f, 90f))
            add(Platform(5600f, g - 50f, 120f))
            add(Platform(5900f, g - 70f, 100f))
        }
    }

    // ============================================================
    // LEVEL 6: SUNKEN RUINS - Deep underwater temple feel
    // ============================================================
    private fun setupLevel6(g: Float) {
        enemySpawns.apply {
            add(EnemySpawn(350f, 300f, 470f))
            add(EnemySpawn(600f, 530f, 700f))
            add(EnemySpawn(850f, 780f, 950f))
            add(EnemySpawn(1050f, 980f, 1150f))
            add(EnemySpawn(1300f, 1230f, 1400f))
            add(EnemySpawn(1500f, 1430f, 1600f))
            add(EnemySpawn(1700f, 1650f, 1800f))
            add(EnemySpawn(1900f, 1850f, 2000f))
            add(EnemySpawn(2100f, 2050f, 2200f))
            add(EnemySpawn(2300f, 2250f, 2400f))
            add(EnemySpawn(2550f, 2500f, 2650f))
            add(EnemySpawn(2750f, 2700f, 2850f))
            add(EnemySpawn(2950f, 2900f, 3050f))
            add(EnemySpawn(3200f, 3150f, 3300f))
            add(EnemySpawn(3450f, 3400f, 3550f))
            add(EnemySpawn(3700f, 3650f, 3800f))
            add(EnemySpawn(3950f, 3900f, 4050f))
            add(EnemySpawn(4200f, 4150f, 4300f))
            add(EnemySpawn(4450f, 4400f, 4550f))
            add(EnemySpawn(4700f, 4650f, 4800f))
            add(EnemySpawn(4950f, 4900f, 5050f))
            add(EnemySpawn(5200f, 5150f, 5300f))
            add(EnemySpawn(5450f, 5400f, 5550f))
            add(EnemySpawn(5700f, 5650f, 5800f))
            add(EnemySpawn(5950f, 5900f, 6100f))
        }
        platforms.apply {
            // Ancient temple pillars and crumbling ruins
            add(Platform(300f, g - 60f, 110f))
            add(Platform(520f, g - 90f, 70f))
            add(Platform(720f, g - 65f, 85f))
            // Temple steps ascending
            add(Platform(950f, g - 45f, 75f))
            add(Platform(1070f, g - 65f, 75f))
            add(Platform(1190f, g - 85f, 75f))
            add(Platform(1310f, g - 105f, 75f))
            // Broken bridge — gaps!
            add(Platform(1550f, g - 80f, 50f))
            add(Platform(1680f, g - 80f, 50f))
            add(Platform(1820f, g - 80f, 50f))
            // Descending ruins
            add(Platform(2050f, g - 100f, 60f))
            add(Platform(2200f, g - 75f, 70f))
            add(Platform(2400f, g - 55f, 90f))
            // Coral reef pillars
            add(Platform(2650f, g - 70f, 55f))
            add(Platform(2800f, g - 95f, 55f))
            add(Platform(2950f, g - 70f, 55f))
            add(Platform(3100f, g - 95f, 55f))
            // Grand hall
            add(Platform(3350f, g - 60f, 130f))
            add(Platform(3600f, g - 80f, 80f))
            add(Platform(3850f, g - 100f, 65f))
            add(Platform(4100f, g - 75f, 90f))
            add(Platform(4400f, g - 60f, 100f))
            add(Platform(4700f, g - 85f, 75f))
            add(Platform(5000f, g - 65f, 90f))
            add(Platform(5300f, g - 80f, 70f))
            add(Platform(5600f, g - 55f, 110f))
            add(Platform(5900f, g - 70f, 90f))
        }
    }

    // ============================================================
    // LEVEL 7: SKY FORTRESS - Floating islands in the clouds
    // ============================================================
    private fun setupLevel7(g: Float) {
        enemySpawns.apply {
            add(EnemySpawn(400f, 350f, 500f))
            add(EnemySpawn(650f, 600f, 750f))
            add(EnemySpawn(900f, 850f, 1000f))
            add(EnemySpawn(1100f, 1050f, 1200f))
            add(EnemySpawn(1350f, 1300f, 1450f))
            add(EnemySpawn(1550f, 1500f, 1650f))
            add(EnemySpawn(1800f, 1750f, 1900f))
            add(EnemySpawn(2000f, 1950f, 2100f))
            add(EnemySpawn(2250f, 2200f, 2350f))
            add(EnemySpawn(2500f, 2450f, 2600f))
            add(EnemySpawn(2700f, 2650f, 2800f))
            add(EnemySpawn(2950f, 2900f, 3050f))
            add(EnemySpawn(3200f, 3150f, 3300f))
            add(EnemySpawn(3450f, 3400f, 3550f))
            add(EnemySpawn(3700f, 3650f, 3800f))
            add(EnemySpawn(3950f, 3900f, 4050f))
            add(EnemySpawn(4200f, 4150f, 4300f))
            add(EnemySpawn(4450f, 4400f, 4550f))
            add(EnemySpawn(4700f, 4650f, 4800f))
            add(EnemySpawn(4950f, 4900f, 5050f))
            add(EnemySpawn(5200f, 5150f, 5300f))
            add(EnemySpawn(5500f, 5450f, 5600f))
            add(EnemySpawn(5750f, 5700f, 5850f))
            add(EnemySpawn(6000f, 5950f, 6100f))
            add(EnemySpawn(6250f, 6200f, 6400f))
        }
        platforms.apply {
            // Floating cloud islands — lots of vertical variety
            add(Platform(300f, g - 70f, 100f))
            add(Platform(480f, g - 110f, 65f))
            add(Platform(650f, g - 80f, 75f))
            add(Platform(850f, g - 130f, 55f))
            add(Platform(1020f, g - 95f, 70f))
            // Cloud stairway to heaven
            add(Platform(1200f, g - 60f, 55f))
            add(Platform(1310f, g - 85f, 55f))
            add(Platform(1420f, g - 110f, 55f))
            add(Platform(1530f, g - 135f, 55f))
            add(Platform(1640f, g - 110f, 55f))
            add(Platform(1750f, g - 85f, 55f))
            add(Platform(1860f, g - 60f, 55f))
            // Floating islands
            add(Platform(2100f, g - 100f, 80f))
            add(Platform(2350f, g - 70f, 90f))
            add(Platform(2600f, g - 120f, 60f))
            add(Platform(2830f, g - 85f, 70f))
            // Wind-swept gap section — big jumps
            add(Platform(3100f, g - 65f, 50f))
            add(Platform(3300f, g - 95f, 50f))
            add(Platform(3500f, g - 65f, 50f))
            add(Platform(3700f, g - 95f, 50f))
            add(Platform(3900f, g - 65f, 50f))
            // Fortress approach
            add(Platform(4150f, g - 80f, 90f))
            add(Platform(4400f, g - 100f, 70f))
            add(Platform(4650f, g - 75f, 85f))
            add(Platform(4950f, g - 60f, 110f))
            add(Platform(5250f, g - 90f, 70f))
            add(Platform(5550f, g - 70f, 80f))
            add(Platform(5850f, g - 85f, 75f))
            add(Platform(6150f, g - 55f, 120f))
        }
    }

    // ============================================================
    // LEVEL 8: HAUNTED SWAMP - Dark, murky, claustrophobic
    // ============================================================
    private fun setupLevel8(g: Float) {
        enemySpawns.apply {
            // Enemies ambush from everywhere in the swamp
            add(EnemySpawn(300f, 260f, 400f))
            add(EnemySpawn(500f, 460f, 580f))
            add(EnemySpawn(680f, 640f, 760f))
            add(EnemySpawn(860f, 820f, 940f))
            add(EnemySpawn(1050f, 1010f, 1130f))
            add(EnemySpawn(1230f, 1190f, 1310f))
            add(EnemySpawn(1400f, 1360f, 1480f))
            add(EnemySpawn(1580f, 1540f, 1660f))
            add(EnemySpawn(1760f, 1720f, 1840f))
            add(EnemySpawn(1950f, 1910f, 2030f))
            add(EnemySpawn(2130f, 2090f, 2210f))
            add(EnemySpawn(2310f, 2270f, 2390f))
            add(EnemySpawn(2500f, 2460f, 2580f))
            add(EnemySpawn(2690f, 2650f, 2770f))
            add(EnemySpawn(2870f, 2830f, 2950f))
            add(EnemySpawn(3060f, 3020f, 3140f))
            add(EnemySpawn(3250f, 3210f, 3330f))
            add(EnemySpawn(3450f, 3410f, 3530f))
            add(EnemySpawn(3650f, 3610f, 3730f))
            add(EnemySpawn(3850f, 3810f, 3930f))
            add(EnemySpawn(4050f, 4010f, 4130f))
            add(EnemySpawn(4250f, 4210f, 4330f))
            add(EnemySpawn(4500f, 4460f, 4580f))
            add(EnemySpawn(4750f, 4710f, 4830f))
            add(EnemySpawn(5000f, 4960f, 5080f))
            add(EnemySpawn(5250f, 5210f, 5330f))
            add(EnemySpawn(5500f, 5460f, 5580f))
            add(EnemySpawn(5750f, 5710f, 5830f))
            add(EnemySpawn(6000f, 5960f, 6100f))
            add(EnemySpawn(6300f, 6250f, 6400f))
        }
        platforms.apply {
            // Low swamp logs and lily pads — many are small
            add(Platform(280f, g - 45f, 90f))
            add(Platform(440f, g - 65f, 50f))
            add(Platform(580f, g - 50f, 60f))
            add(Platform(720f, g - 75f, 45f))
            add(Platform(870f, g - 55f, 70f))
            add(Platform(1040f, g - 80f, 40f))
            add(Platform(1170f, g - 60f, 55f))
            add(Platform(1320f, g - 45f, 80f))
            add(Platform(1480f, g - 70f, 50f))
            add(Platform(1630f, g - 55f, 65f))
            // Dead tree bridge
            add(Platform(1800f, g - 85f, 35f))
            add(Platform(1900f, g - 85f, 35f))
            add(Platform(2000f, g - 85f, 35f))
            add(Platform(2100f, g - 85f, 35f))
            add(Platform(2200f, g - 85f, 35f))
            // Swamp maze
            add(Platform(2400f, g - 50f, 60f))
            add(Platform(2530f, g - 80f, 45f))
            add(Platform(2650f, g - 50f, 60f))
            add(Platform(2780f, g - 80f, 45f))
            add(Platform(2910f, g - 50f, 60f))
            add(Platform(3050f, g - 80f, 45f))
            // Graveyard section
            add(Platform(3250f, g - 65f, 70f))
            add(Platform(3450f, g - 90f, 55f))
            add(Platform(3650f, g - 70f, 65f))
            add(Platform(3850f, g - 55f, 80f))
            add(Platform(4050f, g - 85f, 50f))
            add(Platform(4250f, g - 65f, 70f))
            add(Platform(4500f, g - 50f, 90f))
            add(Platform(4750f, g - 75f, 60f))
            add(Platform(5000f, g - 60f, 80f))
            add(Platform(5250f, g - 80f, 55f))
            add(Platform(5500f, g - 55f, 90f))
            add(Platform(5750f, g - 70f, 70f))
            add(Platform(6050f, g - 50f, 110f))
            add(Platform(6300f, g - 65f, 80f))
        }
    }

    // ============================================================
    // LEVEL 9: CRYSTAL CAVERNS - Sparkling underground, tricky jumps
    // ============================================================
    private fun setupLevel9(g: Float) {
        enemySpawns.apply {
            add(EnemySpawn(350f, 300f, 450f))
            add(EnemySpawn(550f, 500f, 640f))
            add(EnemySpawn(740f, 700f, 830f))
            add(EnemySpawn(930f, 890f, 1020f))
            add(EnemySpawn(1120f, 1080f, 1210f))
            add(EnemySpawn(1310f, 1270f, 1400f))
            add(EnemySpawn(1500f, 1460f, 1590f))
            add(EnemySpawn(1690f, 1650f, 1780f))
            add(EnemySpawn(1880f, 1840f, 1970f))
            add(EnemySpawn(2070f, 2030f, 2160f))
            add(EnemySpawn(2260f, 2220f, 2350f))
            add(EnemySpawn(2450f, 2410f, 2540f))
            add(EnemySpawn(2640f, 2600f, 2730f))
            add(EnemySpawn(2830f, 2790f, 2920f))
            add(EnemySpawn(3020f, 2980f, 3110f))
            add(EnemySpawn(3210f, 3170f, 3300f))
            add(EnemySpawn(3400f, 3360f, 3490f))
            add(EnemySpawn(3600f, 3560f, 3690f))
            add(EnemySpawn(3800f, 3760f, 3890f))
            add(EnemySpawn(4000f, 3960f, 4090f))
            add(EnemySpawn(4200f, 4160f, 4290f))
            add(EnemySpawn(4450f, 4410f, 4540f))
            add(EnemySpawn(4700f, 4660f, 4790f))
            add(EnemySpawn(4950f, 4910f, 5040f))
            add(EnemySpawn(5200f, 5160f, 5290f))
            add(EnemySpawn(5450f, 5410f, 5540f))
            add(EnemySpawn(5700f, 5660f, 5790f))
            add(EnemySpawn(5950f, 5910f, 6040f))
            add(EnemySpawn(6200f, 6160f, 6290f))
            add(EnemySpawn(6500f, 6460f, 6600f))
            add(EnemySpawn(6750f, 6700f, 6900f))
        }
        platforms.apply {
            // Crystal formations — varied heights, some very high
            add(Platform(300f, g - 65f, 80f))
            add(Platform(470f, g - 95f, 55f))
            add(Platform(630f, g - 70f, 70f))
            add(Platform(820f, g - 120f, 45f))
            add(Platform(970f, g - 80f, 65f))
            // Crystal staircase (ascending high)
            add(Platform(1150f, g - 55f, 50f))
            add(Platform(1260f, g - 80f, 50f))
            add(Platform(1370f, g - 105f, 50f))
            add(Platform(1480f, g - 130f, 50f))
            add(Platform(1590f, g - 105f, 50f))
            // Crystal bridge — floating shards
            add(Platform(1780f, g - 90f, 35f))
            add(Platform(1870f, g - 100f, 35f))
            add(Platform(1960f, g - 90f, 35f))
            add(Platform(2050f, g - 100f, 35f))
            add(Platform(2140f, g - 90f, 35f))
            // Geode chamber — wide platforms with narrow connectors
            add(Platform(2350f, g - 60f, 120f))
            add(Platform(2550f, g - 85f, 40f))
            add(Platform(2670f, g - 60f, 120f))
            add(Platform(2870f, g - 85f, 40f))
            add(Platform(2990f, g - 60f, 120f))
            // Vertical shaft
            add(Platform(3200f, g - 75f, 60f))
            add(Platform(3350f, g - 110f, 50f))
            add(Platform(3500f, g - 75f, 60f))
            add(Platform(3650f, g - 110f, 50f))
            add(Platform(3800f, g - 75f, 60f))
            // Final crystal path
            add(Platform(4050f, g - 65f, 80f))
            add(Platform(4300f, g - 90f, 60f))
            add(Platform(4550f, g - 70f, 75f))
            add(Platform(4800f, g - 100f, 55f))
            add(Platform(5050f, g - 65f, 85f))
            add(Platform(5300f, g - 85f, 65f))
            add(Platform(5550f, g - 55f, 100f))
            add(Platform(5850f, g - 80f, 60f))
            add(Platform(6100f, g - 60f, 90f))
            add(Platform(6400f, g - 75f, 70f))
            add(Platform(6700f, g - 55f, 110f))
        }
    }

    // ============================================================
    // LEVEL 10: FINAL ASSAULT - Military fortress, epic finale
    // ============================================================
    private fun setupLevel10(g: Float) {
        enemySpawns.apply {
            // Maximum enemy density — final gauntlet
            add(EnemySpawn(300f, 260f, 400f))
            add(EnemySpawn(480f, 440f, 560f))
            add(EnemySpawn(650f, 610f, 730f))
            add(EnemySpawn(820f, 780f, 900f))
            add(EnemySpawn(990f, 950f, 1070f))
            add(EnemySpawn(1160f, 1120f, 1240f))
            add(EnemySpawn(1330f, 1290f, 1410f))
            add(EnemySpawn(1500f, 1460f, 1580f))
            add(EnemySpawn(1670f, 1630f, 1750f))
            add(EnemySpawn(1840f, 1800f, 1920f))
            add(EnemySpawn(2010f, 1970f, 2090f))
            add(EnemySpawn(2180f, 2140f, 2260f))
            add(EnemySpawn(2350f, 2310f, 2430f))
            add(EnemySpawn(2520f, 2480f, 2600f))
            add(EnemySpawn(2700f, 2660f, 2780f))
            add(EnemySpawn(2880f, 2840f, 2960f))
            add(EnemySpawn(3060f, 3020f, 3140f))
            add(EnemySpawn(3250f, 3210f, 3330f))
            add(EnemySpawn(3440f, 3400f, 3520f))
            add(EnemySpawn(3630f, 3590f, 3710f))
            add(EnemySpawn(3820f, 3780f, 3900f))
            add(EnemySpawn(4010f, 3970f, 4090f))
            add(EnemySpawn(4200f, 4160f, 4280f))
            add(EnemySpawn(4400f, 4360f, 4480f))
            add(EnemySpawn(4600f, 4560f, 4680f))
            add(EnemySpawn(4800f, 4760f, 4880f))
            add(EnemySpawn(5000f, 4960f, 5080f))
            add(EnemySpawn(5200f, 5160f, 5280f))
            add(EnemySpawn(5450f, 5410f, 5530f))
            add(EnemySpawn(5700f, 5660f, 5780f))
            add(EnemySpawn(5950f, 5910f, 6030f))
            add(EnemySpawn(6200f, 6160f, 6280f))
            add(EnemySpawn(6450f, 6410f, 6530f))
            add(EnemySpawn(6700f, 6660f, 6780f))
            add(EnemySpawn(6950f, 6910f, 7030f))
            add(EnemySpawn(7200f, 7160f, 7350f))
        }
        platforms.apply {
            // Military bunkers and trenches
            add(Platform(280f, g - 55f, 100f))
            add(Platform(450f, g - 80f, 60f))
            add(Platform(600f, g - 60f, 80f))
            add(Platform(780f, g - 90f, 55f))
            add(Platform(950f, g - 70f, 70f))
            // Barricade wall climb
            add(Platform(1130f, g - 45f, 55f))
            add(Platform(1240f, g - 70f, 55f))
            add(Platform(1350f, g - 95f, 55f))
            add(Platform(1460f, g - 120f, 55f))
            add(Platform(1570f, g - 95f, 55f))
            add(Platform(1680f, g - 70f, 55f))
            // Trench warfare — low cover
            add(Platform(1880f, g - 40f, 90f))
            add(Platform(2050f, g - 40f, 90f))
            add(Platform(2220f, g - 40f, 90f))
            // Tower assault — ascending
            add(Platform(2430f, g - 60f, 50f))
            add(Platform(2540f, g - 90f, 50f))
            add(Platform(2650f, g - 120f, 50f))
            add(Platform(2760f, g - 90f, 50f))
            add(Platform(2870f, g - 60f, 50f))
            // Fortress walls — narrow walkways
            add(Platform(3080f, g - 75f, 40f))
            add(Platform(3180f, g - 100f, 40f))
            add(Platform(3280f, g - 75f, 40f))
            add(Platform(3380f, g - 100f, 40f))
            add(Platform(3480f, g - 75f, 40f))
            add(Platform(3580f, g - 100f, 40f))
            // Open battleground
            add(Platform(3800f, g - 65f, 90f))
            add(Platform(4050f, g - 85f, 70f))
            add(Platform(4300f, g - 60f, 100f))
            add(Platform(4550f, g - 90f, 60f))
            // Death run — tiny platforms, big gaps
            add(Platform(4800f, g - 70f, 35f))
            add(Platform(4950f, g - 90f, 35f))
            add(Platform(5100f, g - 70f, 35f))
            add(Platform(5250f, g - 90f, 35f))
            add(Platform(5400f, g - 70f, 35f))
            // Final approach
            add(Platform(5650f, g - 55f, 100f))
            add(Platform(5900f, g - 80f, 70f))
            add(Platform(6150f, g - 65f, 90f))
            add(Platform(6400f, g - 85f, 60f))
            add(Platform(6700f, g - 60f, 110f))
            add(Platform(7000f, g - 75f, 80f))
            add(Platform(7300f, g - 50f, 130f))
        }
    }


    // ============================================================
    // BACKGROUND GENERATION
    // ============================================================
    private fun generateClouds() {
        val count = (width / 110f).toInt()
        for (i in 0..count) {
            clouds.add(floatArrayOf(
                i * 200f + (Math.random() * 100).toFloat(),
                20f + (Math.random() * 60).toFloat(),
                60f + (Math.random() * 80).toFloat(),
                0.3f + (Math.random() * 0.4).toFloat()
            ))
        }
    }

    private fun generateMountains() {
        val farCount = (width / 140f).toInt()
        for (i in 0..farCount) {
            farMountains.add(floatArrayOf(
                i * 180f,
                60f + (Math.random() * 40).toFloat(),
                120f + (Math.random() * 60).toFloat()
            ))
        }
        val nearCount = (width / 100f).toInt()
        for (i in 0..nearCount) {
            nearMountains.add(floatArrayOf(
                i * 140f,
                40f + (Math.random() * 30).toFloat(),
                80f + (Math.random() * 50).toFloat()
            ))
        }
    }

    private fun generateTrees() {
        val count = (width / 55f).toInt()
        for (i in 0..count) {
            trees.add(floatArrayOf(
                i * 70f + (Math.random() * 30).toFloat(),
                30f + (Math.random() * 25).toFloat(),
                12f + (Math.random() * 8).toFloat(),
                (Math.random() * 3).toInt().toFloat()
            ))
        }
    }

    // ============================================================
    // RENDERING
    // ============================================================
    fun render(canvas: Canvas, cameraX: Float, scale: Float, screenWidth: Int, screenHeight: Int) {
        val t = theme

        if (screenWidth != lastScreenWidth || screenHeight != lastScreenHeight) {
            createGradients(screenWidth, screenHeight, t)
            lastScreenWidth = screenWidth
            lastScreenHeight = screenHeight
        }

        // Sky
        paint.shader = skyGradient
        canvas.drawRect(0f, 0f, screenWidth.toFloat(), screenHeight.toFloat(), paint)
        paint.shader = null

        // Sun
        if (t.showSun) {
            val sunX = screenWidth * 0.8f - cameraX * 0.05f * scale
            val sunY = screenHeight * 0.15f
            val sunRadius = 40f * scale

            paint.shader = RadialGradient(
                sunX, sunY, sunRadius * 3,
                intArrayOf(t.sunGlowColor, Color.argb(0, 0, 0, 0)),
                null, Shader.TileMode.CLAMP
            )
            canvas.drawCircle(sunX, sunY, sunRadius * 3, paint)
            paint.shader = null

            paint.color = t.sunColor
            canvas.drawCircle(sunX, sunY, sunRadius, paint)
        }

        renderClouds(canvas, cameraX, scale, screenWidth, t)
        renderFarMountains(canvas, cameraX * 0.15f, scale, screenWidth, t)
        renderNearMountains(canvas, cameraX * 0.3f, scale, screenWidth, t)
        renderTrees(canvas, cameraX * 0.6f, scale, screenWidth, t)
        renderGround(canvas, cameraX, scale, screenWidth, screenHeight, t)
        renderPlatforms(canvas, cameraX, scale, screenWidth, t)
    }

    private fun createGradients(screenWidth: Int, screenHeight: Int, t: LevelTheme) {
        skyGradient = LinearGradient(
            0f, 0f, 0f, screenHeight.toFloat(),
            t.skyColors, t.skyPositions,
            Shader.TileMode.CLAMP
        )
    }

    private fun renderClouds(canvas: Canvas, cameraX: Float, scale: Float, screenWidth: Int, t: LevelTheme) {
        for (cloud in clouds) {
            val cloudX = cloud[0] * scale - (cameraX * cloud[3] * scale) % (screenWidth + 200f)
            val cloudY = cloud[1] * scale
            val cloudW = cloud[2] * scale

            if (cloudX > -cloudW && cloudX < screenWidth + cloudW) {
                paint.color = Color.argb(t.cloudAlpha,
                    Color.red(t.cloudColor), Color.green(t.cloudColor), Color.blue(t.cloudColor))

                canvas.drawOval(cloudX, cloudY, cloudX + cloudW * 0.6f, cloudY + cloudW * 0.3f, paint)
                canvas.drawOval(cloudX + cloudW * 0.2f, cloudY - cloudW * 0.1f, cloudX + cloudW * 0.7f, cloudY + cloudW * 0.25f, paint)
                canvas.drawOval(cloudX + cloudW * 0.4f, cloudY, cloudX + cloudW, cloudY + cloudW * 0.35f, paint)
                canvas.drawOval(cloudX + cloudW * 0.15f, cloudY + cloudW * 0.05f, cloudX + cloudW * 0.85f, cloudY + cloudW * 0.35f, paint)
            }
        }
    }

    private fun renderFarMountains(canvas: Canvas, offsetX: Float, scale: Float, screenWidth: Int, t: LevelTheme) {
        val baseY = Constants.GROUND_Y * scale

        for (mountain in farMountains) {
            val x = mountain[0] * scale - (offsetX * scale) % (180f * scale * (farMountains.size + 1))
            val h = mountain[1] * scale
            val w = mountain[2] * scale

            if (x > -w && x < screenWidth + w) {
                val path = Path().apply {
                    moveTo(x, baseY)
                    lineTo(x + w * 0.5f, baseY - h)
                    lineTo(x + w, baseY)
                    close()
                }
                paint.color = t.farMountainColor
                canvas.drawPath(path, paint)

                if (t.showSnowCaps && h > 70 * scale) {
                    paint.color = t.snowCapColor
                    val snowPath = Path().apply {
                        moveTo(x + w * 0.35f, baseY - h * 0.7f)
                        lineTo(x + w * 0.5f, baseY - h)
                        lineTo(x + w * 0.65f, baseY - h * 0.7f)
                        close()
                    }
                    canvas.drawPath(snowPath, paint)
                }
            }
        }
    }

    private fun renderNearMountains(canvas: Canvas, offsetX: Float, scale: Float, screenWidth: Int, t: LevelTheme) {
        val baseY = Constants.GROUND_Y * scale

        for (mountain in nearMountains) {
            val x = mountain[0] * scale - (offsetX * scale) % (140f * scale * (nearMountains.size + 1))
            val h = mountain[1] * scale
            val w = mountain[2] * scale

            if (x > -w && x < screenWidth + w) {
                val path = Path().apply {
                    moveTo(x, baseY)
                    lineTo(x + w * 0.3f, baseY - h * 0.6f)
                    lineTo(x + w * 0.5f, baseY - h)
                    lineTo(x + w * 0.7f, baseY - h * 0.7f)
                    lineTo(x + w, baseY)
                    close()
                }
                paint.color = t.nearMountainColor
                canvas.drawPath(path, paint)

                val highlightPath = Path().apply {
                    moveTo(x + w * 0.5f, baseY - h)
                    lineTo(x + w * 0.7f, baseY - h * 0.7f)
                    lineTo(x + w * 0.6f, baseY - h * 0.5f)
                    close()
                }
                paint.color = t.nearMountainHighlight
                canvas.drawPath(highlightPath, paint)
            }
        }
    }

    private fun renderTrees(canvas: Canvas, offsetX: Float, scale: Float, screenWidth: Int, t: LevelTheme) {
        val baseY = Constants.GROUND_Y * scale

        for (tree in trees) {
            val x = tree[0] * scale - (offsetX * scale) % (70f * scale * (trees.size + 1))
            val h = tree[1] * scale
            val trunkW = tree[2] * scale
            val treeType = tree[3].toInt()

            if (x > -50 && x < screenWidth + 50) {
                paint.color = t.trunkColor
                canvas.drawRect(x - trunkW / 2, baseY - h * 0.5f, x + trunkW / 2, baseY, paint)

                when (treeType) {
                    0 -> {
                        paint.color = t.foliageColors[0]
                        val foliagePath = Path().apply {
                            moveTo(x, baseY - h)
                            lineTo(x - h * 0.4f, baseY - h * 0.3f)
                            lineTo(x + h * 0.4f, baseY - h * 0.3f)
                            close()
                        }
                        canvas.drawPath(foliagePath, paint)

                        paint.color = t.foliageHighlights[0]
                        val foliagePath2 = Path().apply {
                            moveTo(x, baseY - h * 0.85f)
                            lineTo(x - h * 0.35f, baseY - h * 0.45f)
                            lineTo(x + h * 0.35f, baseY - h * 0.45f)
                            close()
                        }
                        canvas.drawPath(foliagePath2, paint)
                    }
                    1 -> {
                        paint.color = t.foliageColors[1]
                        canvas.drawCircle(x, baseY - h * 0.65f, h * 0.4f, paint)
                        paint.color = t.foliageHighlights[1]
                        canvas.drawCircle(x - h * 0.15f, baseY - h * 0.7f, h * 0.25f, paint)
                    }
                    else -> {
                        paint.color = t.foliageColors[2]
                        canvas.drawOval(x - h * 0.35f, baseY - h * 0.9f, x + h * 0.35f, baseY - h * 0.35f, paint)
                        paint.color = t.foliageHighlights[2]
                        canvas.drawOval(x - h * 0.25f, baseY - h * 0.95f, x + h * 0.3f, baseY - h * 0.5f, paint)
                    }
                }
            }
        }
    }

    private fun renderGround(canvas: Canvas, cameraX: Float, scale: Float, screenWidth: Int, screenHeight: Int, t: LevelTheme) {
        val groundY = Constants.GROUND_Y * scale

        paint.shader = LinearGradient(
            0f, groundY, 0f, groundY + 15f * scale,
            t.grassTopColor, t.grassBottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, groundY, screenWidth.toFloat(), groundY + 15f * scale, paint)
        paint.shader = null

        paint.shader = LinearGradient(
            0f, groundY + 15f * scale, 0f, screenHeight.toFloat(),
            t.dirtTopColor, t.dirtBottomColor,
            Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, groundY + 15f * scale, screenWidth.toFloat(), screenHeight.toFloat(), paint)
        paint.shader = null

        paint.color = t.grassTuftColor
        val grassSpacing = 8f * scale
        var grassX = -(cameraX * scale) % grassSpacing
        while (grassX < screenWidth) {
            val grassH = (3f + sin(grassX * 0.5f) * 2f) * scale
            canvas.drawLine(grassX, groundY, grassX, groundY - grassH, paint.apply { strokeWidth = 1.5f * scale })
            grassX += grassSpacing
        }
    }

    private fun renderPlatforms(canvas: Canvas, cameraX: Float, scale: Float, screenWidth: Int, t: LevelTheme) {
        for (platform in platforms) {
            val px = (platform.x - cameraX) * scale
            val py = platform.y * scale
            val pw = platform.width * scale
            val ph = platform.height * scale

            if (px + pw > 0 && px < screenWidth) {
                // Shadow
                paint.color = Color.argb(50, 0, 0, 0)
                canvas.drawRect(px + 4f * scale, py + 4f * scale, px + pw + 4f * scale, py + ph + 4f * scale, paint)

                // Body
                paint.color = t.platformBodyColor
                canvas.drawRect(px, py + 6f * scale, px + pw, py + ph, paint)

                // Top
                paint.shader = LinearGradient(
                    px, py, px, py + 8f * scale,
                    t.platformTopGradientTop, t.platformTopGradientBottom,
                    Shader.TileMode.CLAMP
                )
                canvas.drawRect(px, py, px + pw, py + 8f * scale, paint)
                paint.shader = null

                // Grain lines
                paint.color = t.platformGrainColor
                paint.strokeWidth = 1f * scale
                canvas.drawLine(px + pw * 0.2f, py + 8f * scale, px + pw * 0.2f, py + ph, paint)
                canvas.drawLine(px + pw * 0.5f, py + 8f * scale, px + pw * 0.5f, py + ph, paint)
                canvas.drawLine(px + pw * 0.8f, py + 8f * scale, px + pw * 0.8f, py + ph, paint)
            }
        }
    }

    fun getSpawnableEnemies(cameraX: Float): List<EnemySpawn> {
        val spawnRange = cameraX + Constants.GAME_WIDTH + 100f
        return enemySpawns.filter {
            !it.spawned && it.x < spawnRange && it.x > cameraX - 50
        }
    }

    fun reset() {
        enemySpawns.forEach { it.spawned = false }
    }
}
