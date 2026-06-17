package com.ironfury.laststand.level

import android.graphics.Color

/**
 * Per-level visual + difficulty data.
 *
 * Each entry in [LevelThemes.get] is a self-contained [LevelTheme] describing
 * the sky gradient, sun, clouds, mountains, foliage, ground, platform palette
 * and boss-health scaling for one of the 20 levels. Keeping this data table
 * out of [Level] makes it easy to tweak palettes without touching the level
 * geometry code.
 */
object LevelThemes {

    /** Returns the theme for the given 1-indexed level, falling back to level 1. */
    fun get(level: Int): LevelTheme = when (level) {
        1 -> forestDawn
        2 -> desertStorm
        3 -> frozenPeaks
        4 -> volcanicDepths
        5 -> neonCity
        6 -> sunkenRuins
        7 -> skyFortress
        8 -> hauntedSwamp
        9 -> crystalCaverns
        10 -> finalAssault
        11 -> toxicWasteland
        12 -> abyssalTrench
        13 -> moltenCore
        14 -> auroraTundra
        15 -> orbitalStation
        16 -> bloodMarsh
        17 -> goldenDunes
        18 -> voidNebula
        19 -> ashenBattlefield
        20 -> dragonsThrone
        else -> forestDawn
    }

    private val forestDawn = LevelTheme(
        name = "Forest Dawn",
        bossName = "WAR MACHINE",
        levelWidth = 5000f,
        skyColors = intArrayOf(
            Color.rgb(135, 180, 255),
            Color.rgb(180, 210, 255),
            Color.rgb(255, 220, 180),
            Color.rgb(255, 180, 140)
        ),
        skyPositions = floatArrayOf(0f, 0.4f, 0.75f, 1f),
        sunColor = Color.rgb(255, 250, 220),
        sunGlowColor = Color.argb(60, 255, 255, 200),
        cloudColor = Color.rgb(255, 255, 255),
        farMountainColor = Color.rgb(120, 110, 140),
        nearMountainColor = Color.rgb(90, 105, 85),
        nearMountainHighlight = Color.rgb(110, 125, 100),
        foliageColors = intArrayOf(Color.rgb(40, 80, 45), Color.rgb(45, 90, 50), Color.rgb(35, 75, 40)),
        foliageHighlights = intArrayOf(Color.rgb(50, 95, 55), Color.rgb(55, 105, 60), Color.rgb(45, 85, 50)),
        grassTopColor = Color.rgb(80, 140, 60),
        grassBottomColor = Color.rgb(60, 110, 45),
        dirtTopColor = Color.rgb(120, 85, 55),
        dirtBottomColor = Color.rgb(80, 55, 35),
        grassTuftColor = Color.rgb(70, 130, 55),
        bossHealthMultiplier = 1f
    )

    private val desertStorm = LevelTheme(
        name = "Desert Storm",
        bossName = "SAND SCORPION",
        levelWidth = 5500f,
        skyColors = intArrayOf(
            Color.rgb(255, 200, 120),
            Color.rgb(255, 180, 100),
            Color.rgb(255, 160, 80),
            Color.rgb(220, 130, 60)
        ),
        skyPositions = floatArrayOf(0f, 0.3f, 0.65f, 1f),
        sunColor = Color.rgb(255, 240, 180),
        sunGlowColor = Color.argb(80, 255, 220, 120),
        cloudColor = Color.rgb(255, 240, 210),
        cloudAlpha = 120,
        farMountainColor = Color.rgb(180, 150, 110),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(160, 130, 90),
        nearMountainHighlight = Color.rgb(180, 150, 110),
        trunkColor = Color.rgb(140, 110, 60),
        foliageColors = intArrayOf(Color.rgb(80, 120, 40), Color.rgb(50, 100, 35), Color.rgb(70, 110, 40)),
        foliageHighlights = intArrayOf(Color.rgb(95, 135, 50), Color.rgb(65, 115, 45), Color.rgb(85, 125, 50)),
        grassTopColor = Color.rgb(200, 180, 120),
        grassBottomColor = Color.rgb(180, 160, 100),
        dirtTopColor = Color.rgb(210, 180, 130),
        dirtBottomColor = Color.rgb(180, 150, 100),
        grassTuftColor = Color.rgb(170, 150, 90),
        platformBodyColor = Color.rgb(160, 130, 80),
        platformTopGradientTop = Color.rgb(190, 165, 110),
        platformTopGradientBottom = Color.rgb(170, 145, 90),
        platformGrainColor = Color.rgb(140, 110, 70),
        bossHealthMultiplier = 1.2f
    )

    private val frozenPeaks = LevelTheme(
        name = "Frozen Peaks",
        bossName = "ICE TITAN",
        levelWidth = 5800f,
        skyColors = intArrayOf(
            Color.rgb(180, 210, 240),
            Color.rgb(200, 220, 245),
            Color.rgb(220, 230, 250),
            Color.rgb(240, 240, 255)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(240, 245, 255),
        sunGlowColor = Color.argb(40, 200, 220, 255),
        cloudColor = Color.rgb(230, 240, 255),
        cloudAlpha = 180,
        farMountainColor = Color.rgb(160, 170, 190),
        snowCapColor = Color.argb(220, 255, 255, 255),
        nearMountainColor = Color.rgb(140, 155, 175),
        nearMountainHighlight = Color.rgb(170, 185, 200),
        trunkColor = Color.rgb(70, 60, 55),
        foliageColors = intArrayOf(Color.rgb(30, 65, 55), Color.rgb(35, 70, 60), Color.rgb(25, 60, 50)),
        foliageHighlights = intArrayOf(Color.rgb(40, 80, 70), Color.rgb(45, 85, 75), Color.rgb(35, 75, 65)),
        grassTopColor = Color.rgb(200, 215, 230),
        grassBottomColor = Color.rgb(180, 195, 210),
        dirtTopColor = Color.rgb(160, 170, 185),
        dirtBottomColor = Color.rgb(130, 140, 155),
        grassTuftColor = Color.rgb(210, 225, 240),
        platformBodyColor = Color.rgb(150, 165, 185),
        platformTopGradientTop = Color.rgb(220, 230, 245),
        platformTopGradientBottom = Color.rgb(190, 205, 220),
        platformGrainColor = Color.rgb(170, 185, 200),
        bossHealthMultiplier = 1.4f
    )

    private val volcanicDepths = LevelTheme(
        name = "Volcanic Depths",
        bossName = "MAGMA LORD",
        levelWidth = 6000f,
        skyColors = intArrayOf(
            Color.rgb(60, 20, 20),
            Color.rgb(100, 30, 15),
            Color.rgb(160, 60, 20),
            Color.rgb(200, 80, 30)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(255, 120, 40),
        sunGlowColor = Color.argb(80, 255, 80, 20),
        cloudColor = Color.rgb(100, 60, 40),
        cloudAlpha = 100,
        farMountainColor = Color.rgb(60, 30, 25),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(80, 40, 20),
        nearMountainHighlight = Color.rgb(120, 60, 30),
        trunkColor = Color.rgb(50, 30, 20),
        foliageColors = intArrayOf(Color.rgb(80, 40, 15), Color.rgb(70, 35, 15), Color.rgb(90, 45, 15)),
        foliageHighlights = intArrayOf(Color.rgb(100, 55, 25), Color.rgb(90, 50, 25), Color.rgb(110, 60, 25)),
        grassTopColor = Color.rgb(80, 50, 30),
        grassBottomColor = Color.rgb(60, 35, 20),
        dirtTopColor = Color.rgb(70, 40, 25),
        dirtBottomColor = Color.rgb(50, 25, 15),
        grassTuftColor = Color.rgb(100, 60, 30),
        platformBodyColor = Color.rgb(90, 50, 30),
        platformTopGradientTop = Color.rgb(140, 70, 30),
        platformTopGradientBottom = Color.rgb(110, 55, 25),
        platformGrainColor = Color.rgb(70, 35, 20),
        bossHealthMultiplier = 1.6f
    )

    private val neonCity = LevelTheme(
        name = "Neon City",
        bossName = "CYBER OVERLORD",
        levelWidth = 6500f,
        skyColors = intArrayOf(
            Color.rgb(15, 10, 40),
            Color.rgb(25, 15, 60),
            Color.rgb(40, 20, 80),
            Color.rgb(30, 15, 50)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(200, 100, 255),
        sunGlowColor = Color.argb(60, 180, 80, 255),
        showSun = true,
        cloudColor = Color.rgb(60, 40, 100),
        cloudAlpha = 100,
        farMountainColor = Color.rgb(30, 20, 50),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(40, 25, 65),
        nearMountainHighlight = Color.rgb(60, 40, 90),
        trunkColor = Color.rgb(40, 30, 60),
        foliageColors = intArrayOf(Color.rgb(0, 200, 200), Color.rgb(200, 0, 200), Color.rgb(200, 200, 0)),
        foliageHighlights = intArrayOf(Color.rgb(0, 255, 255), Color.rgb(255, 0, 255), Color.rgb(255, 255, 0)),
        grassTopColor = Color.rgb(30, 30, 50),
        grassBottomColor = Color.rgb(20, 20, 35),
        dirtTopColor = Color.rgb(25, 25, 40),
        dirtBottomColor = Color.rgb(15, 15, 25),
        grassTuftColor = Color.rgb(0, 180, 180),
        platformBodyColor = Color.rgb(40, 30, 70),
        platformTopGradientTop = Color.rgb(0, 220, 220),
        platformTopGradientBottom = Color.rgb(180, 0, 220),
        platformGrainColor = Color.rgb(100, 0, 200),
        bossHealthMultiplier = 1.8f
    )

    private val sunkenRuins = LevelTheme(
        name = "Sunken Ruins",
        bossName = "KRAKEN",
        levelWidth = 6500f,
        skyColors = intArrayOf(
            Color.rgb(10, 40, 60),
            Color.rgb(15, 60, 80),
            Color.rgb(20, 80, 100),
            Color.rgb(30, 100, 120)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(100, 200, 220),
        sunGlowColor = Color.argb(40, 80, 180, 200),
        cloudColor = Color.rgb(40, 100, 120),
        cloudAlpha = 80,
        farMountainColor = Color.rgb(20, 60, 70),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(25, 70, 85),
        nearMountainHighlight = Color.rgb(35, 90, 105),
        trunkColor = Color.rgb(30, 60, 50),
        foliageColors = intArrayOf(Color.rgb(20, 100, 80), Color.rgb(25, 110, 90), Color.rgb(15, 90, 70)),
        foliageHighlights = intArrayOf(Color.rgb(30, 130, 100), Color.rgb(35, 140, 110), Color.rgb(25, 120, 90)),
        grassTopColor = Color.rgb(30, 80, 70),
        grassBottomColor = Color.rgb(20, 60, 50),
        dirtTopColor = Color.rgb(35, 70, 60),
        dirtBottomColor = Color.rgb(20, 45, 40),
        grassTuftColor = Color.rgb(25, 100, 85),
        platformBodyColor = Color.rgb(40, 80, 90),
        platformTopGradientTop = Color.rgb(30, 120, 110),
        platformTopGradientBottom = Color.rgb(20, 90, 80),
        platformGrainColor = Color.rgb(25, 65, 60),
        bossHealthMultiplier = 2.0f
    )

    private val skyFortress = LevelTheme(
        name = "Sky Fortress",
        bossName = "STORM EMPEROR",
        levelWidth = 7000f,
        skyColors = intArrayOf(
            Color.rgb(100, 160, 255),
            Color.rgb(140, 190, 255),
            Color.rgb(200, 220, 255),
            Color.rgb(255, 255, 255)
        ),
        skyPositions = floatArrayOf(0f, 0.3f, 0.65f, 1f),
        sunColor = Color.rgb(255, 255, 230),
        sunGlowColor = Color.argb(70, 255, 255, 200),
        cloudColor = Color.rgb(255, 255, 255),
        cloudAlpha = 230,
        farMountainColor = Color.rgb(180, 200, 230),
        snowCapColor = Color.argb(200, 255, 255, 255),
        nearMountainColor = Color.rgb(160, 185, 215),
        nearMountainHighlight = Color.rgb(190, 210, 235),
        trunkColor = Color.rgb(120, 130, 150),
        foliageColors = intArrayOf(Color.rgb(150, 180, 210), Color.rgb(160, 190, 220), Color.rgb(140, 170, 200)),
        foliageHighlights = intArrayOf(Color.rgb(180, 210, 240), Color.rgb(190, 220, 245), Color.rgb(170, 200, 230)),
        grassTopColor = Color.rgb(180, 200, 220),
        grassBottomColor = Color.rgb(160, 180, 200),
        dirtTopColor = Color.rgb(150, 165, 185),
        dirtBottomColor = Color.rgb(130, 145, 165),
        grassTuftColor = Color.rgb(200, 220, 240),
        platformBodyColor = Color.rgb(170, 185, 210),
        platformTopGradientTop = Color.rgb(220, 235, 255),
        platformTopGradientBottom = Color.rgb(190, 210, 235),
        platformGrainColor = Color.rgb(150, 170, 195),
        bossHealthMultiplier = 2.2f
    )

    private val hauntedSwamp = LevelTheme(
        name = "Haunted Swamp",
        bossName = "SWAMP HORROR",
        levelWidth = 7000f,
        skyColors = intArrayOf(
            Color.rgb(20, 25, 15),
            Color.rgb(30, 40, 20),
            Color.rgb(40, 50, 30),
            Color.rgb(50, 55, 35)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(150, 180, 100),
        sunGlowColor = Color.argb(30, 120, 150, 80),
        cloudColor = Color.rgb(50, 60, 40),
        cloudAlpha = 120,
        farMountainColor = Color.rgb(30, 35, 20),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(35, 45, 25),
        nearMountainHighlight = Color.rgb(50, 60, 35),
        trunkColor = Color.rgb(40, 35, 25),
        foliageColors = intArrayOf(Color.rgb(30, 60, 25), Color.rgb(35, 55, 30), Color.rgb(25, 50, 20)),
        foliageHighlights = intArrayOf(Color.rgb(45, 75, 35), Color.rgb(50, 70, 40), Color.rgb(40, 65, 30)),
        grassTopColor = Color.rgb(45, 65, 30),
        grassBottomColor = Color.rgb(35, 50, 22),
        dirtTopColor = Color.rgb(50, 45, 30),
        dirtBottomColor = Color.rgb(35, 30, 20),
        grassTuftColor = Color.rgb(40, 70, 30),
        platformBodyColor = Color.rgb(55, 50, 35),
        platformTopGradientTop = Color.rgb(50, 75, 35),
        platformTopGradientBottom = Color.rgb(40, 60, 28),
        platformGrainColor = Color.rgb(40, 35, 25),
        bossHealthMultiplier = 2.5f
    )

    private val crystalCaverns = LevelTheme(
        name = "Crystal Caverns",
        bossName = "CRYSTAL GUARDIAN",
        levelWidth = 7500f,
        skyColors = intArrayOf(
            Color.rgb(30, 10, 50),
            Color.rgb(50, 20, 80),
            Color.rgb(80, 30, 120),
            Color.rgb(60, 20, 90)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(220, 150, 255),
        sunGlowColor = Color.argb(50, 200, 130, 255),
        cloudColor = Color.rgb(80, 50, 120),
        cloudAlpha = 90,
        farMountainColor = Color.rgb(50, 25, 70),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(60, 30, 85),
        nearMountainHighlight = Color.rgb(90, 50, 120),
        trunkColor = Color.rgb(60, 40, 80),
        foliageColors = intArrayOf(Color.rgb(180, 80, 220), Color.rgb(80, 180, 220), Color.rgb(220, 80, 180)),
        foliageHighlights = intArrayOf(Color.rgb(210, 120, 250), Color.rgb(120, 210, 250), Color.rgb(250, 120, 210)),
        grassTopColor = Color.rgb(60, 40, 80),
        grassBottomColor = Color.rgb(45, 28, 60),
        dirtTopColor = Color.rgb(50, 30, 70),
        dirtBottomColor = Color.rgb(35, 20, 50),
        grassTuftColor = Color.rgb(150, 80, 200),
        platformBodyColor = Color.rgb(70, 40, 100),
        platformTopGradientTop = Color.rgb(180, 100, 240),
        platformTopGradientBottom = Color.rgb(120, 60, 180),
        platformGrainColor = Color.rgb(100, 50, 150),
        bossHealthMultiplier = 2.8f
    )

    private val finalAssault = LevelTheme(
        name = "Final Assault",
        bossName = "SUPREME COMMANDER",
        levelWidth = 8000f,
        skyColors = intArrayOf(
            Color.rgb(20, 5, 5),
            Color.rgb(50, 10, 10),
            Color.rgb(80, 20, 15),
            Color.rgb(120, 30, 20)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(255, 80, 50),
        sunGlowColor = Color.argb(70, 255, 50, 30),
        cloudColor = Color.rgb(80, 30, 25),
        cloudAlpha = 140,
        farMountainColor = Color.rgb(40, 15, 15),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(55, 20, 18),
        nearMountainHighlight = Color.rgb(80, 35, 30),
        trunkColor = Color.rgb(45, 25, 20),
        foliageColors = intArrayOf(Color.rgb(60, 30, 25), Color.rgb(55, 25, 20), Color.rgb(65, 35, 28)),
        foliageHighlights = intArrayOf(Color.rgb(80, 45, 35), Color.rgb(75, 40, 30), Color.rgb(85, 50, 38)),
        grassTopColor = Color.rgb(60, 35, 25),
        grassBottomColor = Color.rgb(45, 25, 18),
        dirtTopColor = Color.rgb(55, 30, 22),
        dirtBottomColor = Color.rgb(35, 18, 12),
        grassTuftColor = Color.rgb(70, 40, 28),
        platformBodyColor = Color.rgb(70, 35, 28),
        platformTopGradientTop = Color.rgb(100, 50, 35),
        platformTopGradientBottom = Color.rgb(80, 40, 28),
        platformGrainColor = Color.rgb(50, 25, 18),
        bossHealthMultiplier = 3.0f
    )

    // ====================================================================
    // LEVELS 11-20 — the extended campaign. Harder bosses, bolder palettes.
    // ====================================================================

    private val toxicWasteland = LevelTheme(
        name = "Toxic Wasteland",
        bossName = "PLAGUE WARDEN",
        levelWidth = 8200f,
        skyColors = intArrayOf(
            Color.rgb(25, 35, 15),
            Color.rgb(45, 60, 20),
            Color.rgb(80, 100, 25),
            Color.rgb(120, 140, 40)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(180, 220, 80),
        sunGlowColor = Color.argb(70, 150, 200, 40),
        cloudColor = Color.rgb(70, 90, 40),
        cloudAlpha = 120,
        farMountainColor = Color.rgb(45, 55, 25),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(55, 70, 30),
        nearMountainHighlight = Color.rgb(80, 100, 40),
        trunkColor = Color.rgb(50, 45, 25),
        foliageColors = intArrayOf(Color.rgb(90, 130, 30), Color.rgb(70, 110, 25), Color.rgb(100, 140, 35)),
        foliageHighlights = intArrayOf(Color.rgb(120, 160, 50), Color.rgb(100, 140, 45), Color.rgb(130, 170, 55)),
        grassTopColor = Color.rgb(80, 110, 40),
        grassBottomColor = Color.rgb(60, 85, 30),
        dirtTopColor = Color.rgb(70, 75, 35),
        dirtBottomColor = Color.rgb(45, 50, 22),
        grassTuftColor = Color.rgb(110, 150, 45),
        platformBodyColor = Color.rgb(75, 85, 40),
        platformTopGradientTop = Color.rgb(120, 150, 50),
        platformTopGradientBottom = Color.rgb(90, 115, 40),
        platformGrainColor = Color.rgb(60, 70, 30),
        bossHealthMultiplier = 3.3f
    )

    private val abyssalTrench = LevelTheme(
        name = "Abyssal Trench",
        bossName = "LEVIATHAN",
        levelWidth = 8400f,
        skyColors = intArrayOf(
            Color.rgb(2, 8, 25),
            Color.rgb(5, 18, 45),
            Color.rgb(10, 30, 70),
            Color.rgb(15, 45, 95)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(60, 140, 200),
        sunGlowColor = Color.argb(40, 40, 120, 200),
        cloudColor = Color.rgb(20, 50, 90),
        cloudAlpha = 70,
        farMountainColor = Color.rgb(10, 30, 55),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(15, 40, 70),
        nearMountainHighlight = Color.rgb(25, 60, 95),
        trunkColor = Color.rgb(20, 45, 60),
        foliageColors = intArrayOf(Color.rgb(20, 90, 110), Color.rgb(25, 100, 120), Color.rgb(15, 80, 100)),
        foliageHighlights = intArrayOf(Color.rgb(30, 120, 150), Color.rgb(35, 130, 160), Color.rgb(25, 110, 140)),
        grassTopColor = Color.rgb(20, 60, 80),
        grassBottomColor = Color.rgb(12, 45, 62),
        dirtTopColor = Color.rgb(18, 50, 70),
        dirtBottomColor = Color.rgb(10, 32, 48),
        grassTuftColor = Color.rgb(25, 90, 115),
        platformBodyColor = Color.rgb(20, 55, 85),
        platformTopGradientTop = Color.rgb(30, 100, 140),
        platformTopGradientBottom = Color.rgb(18, 70, 105),
        platformGrainColor = Color.rgb(15, 45, 70),
        bossHealthMultiplier = 3.6f
    )

    private val moltenCore = LevelTheme(
        name = "Molten Core",
        bossName = "INFERNO COLOSSUS",
        levelWidth = 8600f,
        skyColors = intArrayOf(
            Color.rgb(30, 5, 5),
            Color.rgb(90, 15, 8),
            Color.rgb(170, 45, 12),
            Color.rgb(240, 110, 25)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(255, 150, 40),
        sunGlowColor = Color.argb(90, 255, 90, 20),
        cloudColor = Color.rgb(120, 50, 30),
        cloudAlpha = 110,
        farMountainColor = Color.rgb(70, 25, 18),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(95, 35, 18),
        nearMountainHighlight = Color.rgb(150, 65, 25),
        trunkColor = Color.rgb(55, 28, 18),
        foliageColors = intArrayOf(Color.rgb(110, 45, 15), Color.rgb(95, 38, 12), Color.rgb(125, 55, 18)),
        foliageHighlights = intArrayOf(Color.rgb(160, 75, 25), Color.rgb(140, 65, 22), Color.rgb(180, 90, 30)),
        grassTopColor = Color.rgb(95, 45, 25),
        grassBottomColor = Color.rgb(70, 32, 18),
        dirtTopColor = Color.rgb(80, 38, 22),
        dirtBottomColor = Color.rgb(50, 22, 12),
        grassTuftColor = Color.rgb(180, 80, 30),
        platformBodyColor = Color.rgb(100, 45, 25),
        platformTopGradientTop = Color.rgb(220, 90, 30),
        platformTopGradientBottom = Color.rgb(150, 60, 25),
        platformGrainColor = Color.rgb(80, 35, 20),
        bossHealthMultiplier = 3.9f
    )

    private val auroraTundra = LevelTheme(
        name = "Aurora Tundra",
        bossName = "FROST REVENANT",
        levelWidth = 8800f,
        skyColors = intArrayOf(
            Color.rgb(10, 20, 45),
            Color.rgb(20, 50, 80),
            Color.rgb(30, 110, 120),
            Color.rgb(60, 180, 150)
        ),
        skyPositions = floatArrayOf(0f, 0.4f, 0.75f, 1f),
        sunColor = Color.rgb(180, 255, 230),
        sunGlowColor = Color.argb(60, 100, 255, 200),
        cloudColor = Color.rgb(120, 200, 210),
        cloudAlpha = 150,
        farMountainColor = Color.rgb(90, 130, 160),
        snowCapColor = Color.argb(230, 255, 255, 255),
        nearMountainColor = Color.rgb(110, 150, 180),
        nearMountainHighlight = Color.rgb(150, 200, 220),
        trunkColor = Color.rgb(60, 70, 85),
        foliageColors = intArrayOf(Color.rgb(40, 110, 120), Color.rgb(50, 130, 140), Color.rgb(35, 100, 110)),
        foliageHighlights = intArrayOf(Color.rgb(70, 160, 170), Color.rgb(80, 180, 190), Color.rgb(60, 150, 160)),
        grassTopColor = Color.rgb(190, 230, 235),
        grassBottomColor = Color.rgb(160, 205, 215),
        dirtTopColor = Color.rgb(140, 165, 185),
        dirtBottomColor = Color.rgb(110, 135, 155),
        grassTuftColor = Color.rgb(120, 220, 210),
        platformBodyColor = Color.rgb(130, 175, 195),
        platformTopGradientTop = Color.rgb(200, 245, 250),
        platformTopGradientBottom = Color.rgb(160, 215, 230),
        platformGrainColor = Color.rgb(140, 185, 205),
        bossHealthMultiplier = 4.2f
    )

    private val orbitalStation = LevelTheme(
        name = "Orbital Station",
        bossName = "AI SENTINEL",
        levelWidth = 9000f,
        skyColors = intArrayOf(
            Color.rgb(2, 2, 12),
            Color.rgb(8, 8, 28),
            Color.rgb(18, 14, 45),
            Color.rgb(30, 20, 60)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(120, 200, 255),
        sunGlowColor = Color.argb(70, 80, 160, 255),
        showSun = true,
        cloudColor = Color.rgb(30, 30, 70),
        cloudAlpha = 70,
        farMountainColor = Color.rgb(20, 20, 45),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(30, 30, 60),
        nearMountainHighlight = Color.rgb(50, 50, 90),
        trunkColor = Color.rgb(50, 55, 75),
        foliageColors = intArrayOf(Color.rgb(0, 220, 255), Color.rgb(120, 120, 255), Color.rgb(0, 255, 200)),
        foliageHighlights = intArrayOf(Color.rgb(120, 255, 255), Color.rgb(180, 180, 255), Color.rgb(120, 255, 230)),
        grassTopColor = Color.rgb(35, 40, 60),
        grassBottomColor = Color.rgb(22, 26, 42),
        dirtTopColor = Color.rgb(28, 32, 50),
        dirtBottomColor = Color.rgb(16, 18, 32),
        grassTuftColor = Color.rgb(0, 200, 230),
        platformBodyColor = Color.rgb(45, 50, 80),
        platformTopGradientTop = Color.rgb(0, 220, 255),
        platformTopGradientBottom = Color.rgb(80, 100, 220),
        platformGrainColor = Color.rgb(40, 60, 140),
        bossHealthMultiplier = 4.5f
    )

    private val bloodMarsh = LevelTheme(
        name = "Blood Marsh",
        bossName = "GORE TYRANT",
        levelWidth = 9200f,
        skyColors = intArrayOf(
            Color.rgb(35, 8, 12),
            Color.rgb(70, 15, 20),
            Color.rgb(110, 25, 30),
            Color.rgb(150, 45, 40)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(220, 90, 80),
        sunGlowColor = Color.argb(60, 200, 40, 40),
        cloudColor = Color.rgb(90, 35, 35),
        cloudAlpha = 120,
        farMountainColor = Color.rgb(55, 20, 22),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(70, 28, 28),
        nearMountainHighlight = Color.rgb(100, 42, 40),
        trunkColor = Color.rgb(50, 30, 28),
        foliageColors = intArrayOf(Color.rgb(80, 50, 30), Color.rgb(95, 40, 35), Color.rgb(70, 45, 28)),
        foliageHighlights = intArrayOf(Color.rgb(120, 65, 45), Color.rgb(130, 55, 50), Color.rgb(100, 60, 40)),
        grassTopColor = Color.rgb(70, 45, 35),
        grassBottomColor = Color.rgb(52, 32, 26),
        dirtTopColor = Color.rgb(65, 35, 32),
        dirtBottomColor = Color.rgb(42, 22, 20),
        grassTuftColor = Color.rgb(110, 50, 45),
        platformBodyColor = Color.rgb(80, 38, 35),
        platformTopGradientTop = Color.rgb(130, 55, 50),
        platformTopGradientBottom = Color.rgb(95, 42, 40),
        platformGrainColor = Color.rgb(60, 28, 26),
        bossHealthMultiplier = 4.8f
    )

    private val goldenDunes = LevelTheme(
        name = "Golden Dunes",
        bossName = "SUN PHARAOH",
        levelWidth = 9400f,
        skyColors = intArrayOf(
            Color.rgb(255, 220, 140),
            Color.rgb(255, 195, 100),
            Color.rgb(255, 165, 70),
            Color.rgb(235, 130, 50)
        ),
        skyPositions = floatArrayOf(0f, 0.3f, 0.65f, 1f),
        sunColor = Color.rgb(255, 250, 200),
        sunGlowColor = Color.argb(90, 255, 230, 130),
        cloudColor = Color.rgb(255, 235, 190),
        cloudAlpha = 110,
        farMountainColor = Color.rgb(200, 165, 110),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(190, 150, 90),
        nearMountainHighlight = Color.rgb(220, 185, 120),
        trunkColor = Color.rgb(150, 115, 65),
        foliageColors = intArrayOf(Color.rgb(180, 150, 70), Color.rgb(160, 130, 55), Color.rgb(195, 165, 80)),
        foliageHighlights = intArrayOf(Color.rgb(215, 185, 95), Color.rgb(195, 165, 80), Color.rgb(230, 200, 110)),
        grassTopColor = Color.rgb(230, 200, 130),
        grassBottomColor = Color.rgb(205, 175, 105),
        dirtTopColor = Color.rgb(220, 185, 125),
        dirtBottomColor = Color.rgb(185, 150, 95),
        grassTuftColor = Color.rgb(210, 180, 100),
        platformBodyColor = Color.rgb(195, 160, 100),
        platformTopGradientTop = Color.rgb(235, 205, 135),
        platformTopGradientBottom = Color.rgb(205, 170, 105),
        platformGrainColor = Color.rgb(175, 140, 85),
        bossHealthMultiplier = 5.1f
    )

    private val voidNebula = LevelTheme(
        name = "Void Nebula",
        bossName = "STAR DEVOURER",
        levelWidth = 9600f,
        skyColors = intArrayOf(
            Color.rgb(8, 0, 20),
            Color.rgb(30, 5, 55),
            Color.rgb(70, 15, 100),
            Color.rgb(110, 30, 140)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(230, 140, 255),
        sunGlowColor = Color.argb(70, 190, 80, 255),
        showSun = true,
        cloudColor = Color.rgb(60, 25, 100),
        cloudAlpha = 90,
        farMountainColor = Color.rgb(35, 12, 60),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(50, 18, 80),
        nearMountainHighlight = Color.rgb(80, 35, 120),
        trunkColor = Color.rgb(55, 30, 85),
        foliageColors = intArrayOf(Color.rgb(160, 60, 240), Color.rgb(90, 70, 250), Color.rgb(200, 70, 220)),
        foliageHighlights = intArrayOf(Color.rgb(200, 110, 255), Color.rgb(140, 120, 255), Color.rgb(240, 120, 250)),
        grassTopColor = Color.rgb(50, 30, 80),
        grassBottomColor = Color.rgb(36, 20, 60),
        dirtTopColor = Color.rgb(42, 24, 68),
        dirtBottomColor = Color.rgb(28, 14, 48),
        grassTuftColor = Color.rgb(150, 70, 220),
        platformBodyColor = Color.rgb(60, 32, 100),
        platformTopGradientTop = Color.rgb(170, 80, 240),
        platformTopGradientBottom = Color.rgb(110, 50, 180),
        platformGrainColor = Color.rgb(90, 45, 150),
        bossHealthMultiplier = 5.4f
    )

    private val ashenBattlefield = LevelTheme(
        name = "Ashen Battlefield",
        bossName = "WAR COLOSSUS",
        levelWidth = 9800f,
        skyColors = intArrayOf(
            Color.rgb(30, 28, 28),
            Color.rgb(55, 50, 48),
            Color.rgb(85, 78, 72),
            Color.rgb(120, 105, 95)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(220, 190, 160),
        sunGlowColor = Color.argb(50, 200, 160, 120),
        cloudColor = Color.rgb(80, 72, 68),
        cloudAlpha = 150,
        farMountainColor = Color.rgb(55, 50, 48),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(70, 64, 60),
        nearMountainHighlight = Color.rgb(95, 88, 82),
        trunkColor = Color.rgb(50, 45, 42),
        foliageColors = intArrayOf(Color.rgb(70, 64, 55), Color.rgb(60, 55, 48), Color.rgb(80, 72, 62)),
        foliageHighlights = intArrayOf(Color.rgb(100, 92, 80), Color.rgb(90, 82, 72), Color.rgb(110, 100, 88)),
        grassTopColor = Color.rgb(85, 78, 70),
        grassBottomColor = Color.rgb(62, 56, 50),
        dirtTopColor = Color.rgb(72, 64, 58),
        dirtBottomColor = Color.rgb(48, 42, 38),
        grassTuftColor = Color.rgb(100, 90, 78),
        platformBodyColor = Color.rgb(78, 70, 64),
        platformTopGradientTop = Color.rgb(115, 104, 94),
        platformTopGradientBottom = Color.rgb(85, 76, 68),
        platformGrainColor = Color.rgb(60, 54, 48),
        bossHealthMultiplier = 5.7f
    )

    private val dragonsThrone = LevelTheme(
        name = "Dragon's Throne",
        bossName = "OMEGA DESTROYER",
        levelWidth = 10000f,
        skyColors = intArrayOf(
            Color.rgb(15, 2, 25),
            Color.rgb(60, 8, 30),
            Color.rgb(140, 25, 30),
            Color.rgb(220, 70, 35)
        ),
        skyPositions = floatArrayOf(0f, 0.35f, 0.7f, 1f),
        sunColor = Color.rgb(255, 120, 60),
        sunGlowColor = Color.argb(90, 255, 60, 30),
        showSun = true,
        cloudColor = Color.rgb(90, 25, 35),
        cloudAlpha = 140,
        farMountainColor = Color.rgb(45, 12, 25),
        showSnowCaps = false,
        nearMountainColor = Color.rgb(65, 18, 28),
        nearMountainHighlight = Color.rgb(110, 35, 35),
        trunkColor = Color.rgb(55, 25, 25),
        foliageColors = intArrayOf(Color.rgb(150, 40, 35), Color.rgb(120, 30, 30), Color.rgb(180, 55, 40)),
        foliageHighlights = intArrayOf(Color.rgb(200, 80, 50), Color.rgb(170, 60, 45), Color.rgb(230, 100, 60)),
        grassTopColor = Color.rgb(75, 35, 30),
        grassBottomColor = Color.rgb(55, 25, 22),
        dirtTopColor = Color.rgb(65, 30, 26),
        dirtBottomColor = Color.rgb(42, 18, 16),
        grassTuftColor = Color.rgb(180, 60, 45),
        platformBodyColor = Color.rgb(80, 32, 30),
        platformTopGradientTop = Color.rgb(200, 70, 45),
        platformTopGradientBottom = Color.rgb(140, 50, 35),
        platformGrainColor = Color.rgb(60, 24, 22),
        bossHealthMultiplier = 6.0f
    )
}
