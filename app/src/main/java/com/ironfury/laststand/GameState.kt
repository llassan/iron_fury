package com.ironfury.laststand

import android.content.Context
import com.ironfury.laststand.R
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import com.ironfury.laststand.cosmetics.CharacterSkin
import com.ironfury.laststand.effects.Explosion
import com.ironfury.laststand.entities.Boss
import com.ironfury.laststand.entities.Bullet
import com.ironfury.laststand.entities.Coin
import com.ironfury.laststand.entities.Enemy
import com.ironfury.laststand.entities.Player
import com.ironfury.laststand.entities.PowerUp
import com.ironfury.laststand.input.TouchController
import com.ironfury.laststand.level.Camera
import com.ironfury.laststand.level.Level
import com.ironfury.laststand.ui.WeaponSelector
import com.ironfury.laststand.ui.screens.StartScreenSoldier
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.ControlSize
import com.ironfury.laststand.utils.Vector2
import com.ironfury.laststand.audio.MusicManager
import com.ironfury.laststand.audio.SoundManager
import com.ironfury.laststand.weapons.WeaponManager
import com.ironfury.laststand.weapons.WeaponType

enum class GameStatus {
    START_SCREEN, PLAYING, GAME_OVER, VICTORY, LEVEL_COMPLETE, SETTINGS, CHARACTERS
}

class GameState(private val context: Context) {
    val player = Player()
    val bullets = mutableListOf<Bullet>()
    val enemies = mutableListOf<Enemy>()
    val explosions = mutableListOf<Explosion>()
    val coins = mutableListOf<Coin>()
    val powerUps = mutableListOf<PowerUp>()

    var boss: Boss? = null
        private set
    private var bossSpawned = false
    private var bossDefeated = false

    val weaponManager = WeaponManager(context)

    val camera = Camera()
    val level = Level()

    var currentLevel = 1
        private set
    private var levelCompleteTime = 0f

    companion object {
        const val MAX_LEVEL = 20
    }

    var weaponSelector: WeaponSelector? = null
        private set

    var status = GameStatus.START_SCREEN
        private set

    // Start screen animation
    private var startScreenTime = 0f

    // Settings screen
    private var settingsButtonRect = RectF()
    private val settingsSizeRects = mutableListOf<RectF>()
    private var settingsBackRect = RectF()
    private var settingsLayoutRect = RectF()
    private var settingsAdsRect = RectF()
    var wantsLayoutEditor = false
    private var settingsScreenW = 0
    private var settingsScreenH = 0

    // Character select screen
    private var charactersButtonRect = RectF()           // "CHARACTERS" button on start screen
    private val characterCardRects = mutableListOf<RectF>()
    private var characterBackRect = RectF()
    private var characterScreenW = 0
    private var characterScreenH = 0

    // Arcade pixel font used across all UI text.
    val pixelFont: android.graphics.Typeface? =
        androidx.core.content.res.ResourcesCompat.getFont(context, R.font.press_start_2p)
            .also { com.ironfury.laststand.ui.UiFonts.pixel = it }

    private val hudPaint = Paint().apply {
        color = Color.WHITE
        textSize = 24f
        typeface = pixelFont
        isAntiAlias = false
        setShadowLayer(2f, 2f, 2f, Color.BLACK)
    }

    private val gameOverPaint = Paint().apply {
        color = Color.WHITE
        textSize = 48f
        textAlign = Paint.Align.CENTER
        typeface = pixelFont
        isAntiAlias = false
        setShadowLayer(4f, 4f, 4f, Color.BLACK)
    }

    fun initWeaponSelector(screenWidth: Int, screenHeight: Int) {
        if (weaponSelector == null) {
            weaponSelector = WeaponSelector(screenWidth, screenHeight, weaponManager)
        }
    }

    fun startGame() {
        if (status == GameStatus.START_SCREEN) {
            applySelectedCharacter()
            syncLevelWidth()
            status = GameStatus.PLAYING
            MusicManager.play(MusicManager.Music.GAMEPLAY)
        }
    }

    /** Push the player's saved character choice onto the live player sprite. */
    private fun applySelectedCharacter() {
        player.applySkin(weaponManager.selectedCharacter)
    }

    private fun syncLevelWidth() {
        val lw = level.width
        camera.levelWidth = lw
        player.currentLevelWidth = lw
    }

    fun advanceLevel() {
        if (currentLevel >= MAX_LEVEL) return
        com.ironfury.laststand.ads.AdManager.showInterstitial(
            context as? android.app.Activity, "level_complete"
        )
        currentLevel++
        level.loadLevel(currentLevel)
        bullets.clear()
        enemies.clear()
        explosions.clear()
        coins.clear()
        powerUps.clear()
        boss = null
        bossSpawned = false
        bossDefeated = false
        camera.reset()
        player.resetForNewLevel()
        player.respawn()
        syncLevelWidth()
        status = GameStatus.PLAYING
        MusicManager.play(MusicManager.Music.GAMEPLAY)
    }

    fun handleWeaponTouch(x: Float, y: Float): Boolean {
        return weaponSelector?.handleTouch(x, y) ?: false
    }

    fun update(deltaTime: Float, controller: TouchController) {
        if (status == GameStatus.START_SCREEN || status == GameStatus.SETTINGS) {
            startScreenTime += deltaTime
            return
        }
        if (status == GameStatus.LEVEL_COMPLETE ||
            status == GameStatus.GAME_OVER ||
            status == GameStatus.VICTORY) {
            levelCompleteTime += deltaTime
            return
        }
        if (status != GameStatus.PLAYING) return

        // Sync weapon from selector
        weaponSelector?.let {
            player.currentWeapon = it.currentWeapon
        }

        // Update player input
        player.apply {
            moveLeft = controller.left
            moveRight = controller.right
            aimUp = controller.up
            aimDown = controller.down
            jumping = controller.jump
            firing = controller.fire
        }

        // Update player
        player.update(deltaTime)

        // Handle player shooting
        if (player.canFire) {
            // Check rocket ammo
            if (player.currentWeapon == WeaponType.ROCKET_LAUNCHER && player.rocketAmmo <= 0) {
                // No rockets — don't fire
            } else {
                if (player.currentWeapon == WeaponType.ROCKET_LAUNCHER) {
                    player.rocketAmmo--
                }
                firePlayerWeapon()
            }
        }

        // Platform collision for player
        checkPlatformCollision(player)

        // Update camera
        camera.update(player.position.x)

        // Update checkpoint every ~500 units of progress
        val checkpointInterval = 500f
        val newCheckpoint = (player.position.x / checkpointInterval).toInt() * checkpointInterval
        if (newCheckpoint > player.checkpointX) {
            player.checkpointX = newCheckpoint
        }

        // Spawn enemies as camera reveals them
        spawnEnemies()

        // Update enemies
        for (enemy in enemies) {
            if (enemy.isActive) {
                enemy.update(deltaTime)
                enemy.updateAimDirection(
                    player.centerX,
                    player.centerY,
                    player.velocity.x,
                    player.velocity.y
                )

                // Enemy shooting — use continuous aim vector so shots actually track the player.
                if (enemy.wantsToFire && camera.isVisible(enemy.position.x, enemy.width)) {
                    val spawnPos = enemy.getBulletSpawnPosition()
                    bullets.add(
                        Bullet(
                            spawnPos,
                            enemy.aimVector,
                            WeaponType.MACHINE_GUN,
                            0f,
                            false,
                            null,
                            1f
                        )
                    )
                }
            }
        }

        // Spawn boss near end of level
        val lw = level.width
        if (!bossSpawned && player.position.x >= lw - 600f) {
            val t = level.theme
            boss = Boss.create(
                level = currentLevel,
                startX = lw - 200f,
                bossName = t.bossName,
                healthMultiplier = t.bossHealthMultiplier,
                levelWidth = lw
            )
            bossSpawned = true
            MusicManager.play(MusicManager.Music.BOSS_BATTLE)
        }

        // Update boss
        boss?.let { b ->
            if (b.isActive) {
                b.update(deltaTime, player.centerX, player.centerY)

                // Process boss bullets
                for (bulletData in b.pendingBullets) {
                    bullets.add(Bullet(
                        bulletData.position.copy(),
                        bulletData.direction,
                        WeaponType.MACHINE_GUN,
                        0f,
                        false,
                        bulletData.speed,
                        bulletData.size
                    ))
                }
                b.pendingBullets.clear()
            }
        }

        // Update bullets
        for (bullet in bullets) {
            if (bullet.isActive) {
                bullet.update(deltaTime)

                // Check if bullet is off-screen
                if (bullet.isOffScreen(camera.x)) {
                    bullet.isActive = false
                }
            }
        }

        // Update explosions
        for (explosion in explosions) {
            explosion.update(deltaTime)
        }

        // Update coins
        for (coin in coins) {
            coin.update(deltaTime)
        }

        // Update power-ups
        for (powerUp in powerUps) {
            powerUp.update(deltaTime)
        }

        // Collect coins and power-ups
        checkCoinCollection()
        checkPowerUpCollection()

        // Collision detection
        checkCollisions()

        // Cleanup inactive entities
        bullets.removeAll { !it.isActive }
        enemies.removeAll { !it.isActive }
        explosions.removeAll { !it.isActive }
        coins.removeAll { !it.isActive }
        powerUps.removeAll { !it.isActive }

        // Check level completion (boss defeated and reached end of level)
        if (bossDefeated && player.position.x >= level.width - 100) {
            if (currentLevel >= MAX_LEVEL) {
                SoundManager.play(SoundManager.Sound.VICTORY)
                MusicManager.play(MusicManager.Music.VICTORY)
                weaponManager.highScore = player.score
                status = GameStatus.VICTORY
            } else {
                SoundManager.play(SoundManager.Sound.VICTORY)
                levelCompleteTime = 0f
                status = GameStatus.LEVEL_COMPLETE
            }
        }
    }

    private fun firePlayerWeapon() {
        val weapon = player.currentWeapon
        val spawnPos = player.getBulletSpawnPosition()

        // Play weapon sound
        when (weapon) {
            WeaponType.MACHINE_GUN -> SoundManager.play(SoundManager.Sound.SHOOT_MACHINE_GUN)
            WeaponType.SPREAD_GUN -> SoundManager.play(SoundManager.Sound.SHOOT_SPREAD)
            WeaponType.LASER -> SoundManager.play(SoundManager.Sound.SHOOT_LASER)
            WeaponType.ROCKET_LAUNCHER -> SoundManager.play(SoundManager.Sound.SHOOT_ROCKET)
            WeaponType.FLAMETHROWER -> SoundManager.play(SoundManager.Sound.SHOOT_FLAME)
        }

        when {
            weapon.bulletsPerShot > 1 -> {
                // Spread weapon - fire multiple bullets
                val totalSpread = weapon.spreadAngle * 2
                val angleStep = totalSpread / (weapon.bulletsPerShot - 1)
                val startAngle = -weapon.spreadAngle

                for (i in 0 until weapon.bulletsPerShot) {
                    val spreadOffset = startAngle + i * angleStep
                    bullets.add(Bullet(spawnPos.copy(), player.aimDirection, weapon, spreadOffset, true))
                }
            }
            weapon == WeaponType.FLAMETHROWER -> {
                // Flamethrower has random spread
                val randomSpread = (-weapon.spreadAngle + Math.random() * weapon.spreadAngle * 2).toFloat()
                bullets.add(Bullet(spawnPos, player.aimDirection, weapon, randomSpread, true))
            }
            else -> {
                // Single bullet weapons
                bullets.add(Bullet(spawnPos, player.aimDirection, weapon, 0f, true))
            }
        }
    }

    private fun spawnEnemies() {
        val spawns = level.getSpawnableEnemies(camera.x)
        for (spawn in spawns) {
            enemies.add(Enemy(spawn.x, spawn.patrolLeft, spawn.patrolRight, spawn.surfaceY))
            spawn.spawned = true
        }
    }

    private fun checkPlatformCollision(player: Player) {
        // Only check when falling
        if (player.velocity.y <= 0) return

        for (platform in level.platforms) {
            val platformTop = platform.y
            val wasAbove = player.position.y + player.height - player.velocity.y * Constants.DELTA_TIME <= platformTop

            if (wasAbove &&
                player.position.x + player.width > platform.x &&
                player.position.x < platform.x + platform.width &&
                player.position.y + player.height >= platformTop &&
                player.position.y + player.height <= platformTop + 20
            ) {
                player.position.y = platformTop - player.height
                player.velocity.y = 0f
                player.isOnGround = true
                break
            }
        }
    }

    private fun checkCollisions() {
        // Player bullets vs enemies
        for (bullet in bullets) {
            if (!bullet.isActive || !bullet.isPlayerBullet) continue

            // Check for explosive bullets
            if (bullet.weaponType.isExplosive) {
                var hitSomething = false

                // Check enemies
                for (enemy in enemies) {
                    if (!enemy.isActive) continue

                    if (bulletHitsEntity(bullet, enemy)) {
                        createExplosion(bullet.position.copy(), bullet.weaponType.explosionRadius)
                        bullet.isActive = false
                        handleExplosionDamage(bullet.position, bullet.weaponType.explosionRadius)
                        hitSomething = true
                        break
                    }
                }

                // Check boss
                if (!hitSomething) {
                    boss?.let { b ->
                        if (b.isActive && bulletHitsEntity(bullet, b)) {
                            createExplosion(bullet.position.copy(), bullet.weaponType.explosionRadius)
                            bullet.isActive = false
                            handleBossDamage(b, bullet.weaponType.damage * 2)  // Explosions do extra damage
                        }
                    }
                }
            } else {
                var hitEnemy = false

                // Normal bullets vs enemies
                for (enemy in enemies) {
                    if (!enemy.isActive) continue

                    if (bulletHitsEntity(bullet, enemy)) {
                        bullet.isActive = false
                        spawnCoinsAt(enemy.centerX, enemy.centerY)
                        enemy.isActive = false
                        player.score += Constants.ENEMY_SCORE
                        SoundManager.play(SoundManager.Sound.ENEMY_DEATH)
                        hitEnemy = true
                        break
                    }
                }

                // Normal bullets vs boss
                if (!hitEnemy) {
                    boss?.let { b ->
                        if (b.isActive && bulletHitsEntity(bullet, b)) {
                            bullet.isActive = false
                            handleBossDamage(b, bullet.weaponType.damage)
                        }
                    }
                }
            }
        }

        // Enemy bullets vs player
        for (bullet in bullets) {
            if (!bullet.isActive || bullet.isPlayerBullet) continue

            if (bulletHitsEntity(bullet, player)) {
                bullet.isActive = false
                handlePlayerHit()
            }
        }

        // Direct enemy collision with player
        for (enemy in enemies) {
            if (!enemy.isActive) continue

            if (entityCollision(player, enemy)) {
                handlePlayerHit()
            }
        }

        // Boss collision with player
        boss?.let { b ->
            if (b.isActive && entityCollision(player, b)) {
                handlePlayerHit()
            }
        }
    }

    private fun handleBossDamage(boss: Boss, damage: Int) {
        if (boss.takeDamage(damage)) {
            // Boss defeated!
            bossDefeated = true
            player.score += 5000  // Big score bonus

            SoundManager.play(SoundManager.Sound.BOSS_DEATH)

            // Spawn lots of coins
            for (i in 0 until 20) {
                val offsetX = (Math.random() * boss.width - boss.width / 2).toFloat()
                val offsetY = (Math.random() * boss.height - boss.height / 2).toFloat()
                val coinValue = if (Math.random() < 0.3) 25 else 10
                coins.add(Coin(Vector2(boss.centerX + offsetX, boss.centerY + offsetY), coinValue))
            }

            // Create big explosion
            createExplosion(Vector2(boss.centerX, boss.centerY), 100f)
        } else {
            SoundManager.play(SoundManager.Sound.BOSS_HIT)
        }
    }

    private fun createExplosion(position: Vector2, radius: Float) {
        explosions.add(Explosion(position, radius))
        SoundManager.play(SoundManager.Sound.EXPLOSION)
    }

    private fun handleExplosionDamage(center: Vector2, radius: Float) {
        var killedAny = false
        for (enemy in enemies) {
            if (!enemy.isActive) continue

            val dx = enemy.centerX - center.x
            val dy = enemy.centerY - center.y
            val distance = kotlin.math.sqrt(dx * dx + dy * dy)

            if (distance <= radius) {
                spawnCoinsAt(enemy.centerX, enemy.centerY)
                enemy.isActive = false
                player.score += Constants.ENEMY_SCORE
                killedAny = true
            }
        }
        if (killedAny) {
            SoundManager.play(SoundManager.Sound.ENEMY_DEATH)
        }
    }

    private fun spawnCoinsAt(x: Float, y: Float) {
        // Spawn 1-3 coins at the enemy's position
        val coinCount = 1 + (Math.random() * 3).toInt()
        for (i in 0 until coinCount) {
            val coinValue = if (Math.random() < 0.2) 25 else 10
            coins.add(Coin(Vector2(x, y), coinValue))
        }

        // Chance to drop a weapon power-up
        if (Math.random() < PowerUp.DROP_CHANCE) {
            powerUps.add(PowerUp(Vector2(x, y), PowerUp.randomWeapon()))
        }
    }

    private fun checkCoinCollection() {
        for (coin in coins) {
            if (!coin.isActive) continue

            // Check if player touches coin (with slightly larger hitbox for easier collection)
            val playerRect = RectF(
                player.position.x - 10f,
                player.position.y - 10f,
                player.position.x + player.width + 10f,
                player.position.y + player.height + 10f
            )
            val coinRect = RectF(
                coin.position.x,
                coin.position.y,
                coin.position.x + coin.width,
                coin.position.y + coin.height
            )

            if (RectF.intersects(playerRect, coinRect)) {
                weaponManager.addCoins(coin.value)
                coin.isActive = false
                SoundManager.play(SoundManager.Sound.COIN_COLLECT)
            }
        }
    }

    private fun checkPowerUpCollection() {
        val playerRect = RectF(
            player.position.x - 10f,
            player.position.y - 10f,
            player.position.x + player.width + 10f,
            player.position.y + player.height + 10f
        )
        for (powerUp in powerUps) {
            if (!powerUp.isActive) continue
            val puRect = RectF(
                powerUp.position.x,
                powerUp.position.y,
                powerUp.position.x + powerUp.width,
                powerUp.position.y + powerUp.height
            )
            if (RectF.intersects(playerRect, puRect)) {
                player.currentWeapon = powerUp.weaponType
                weaponSelector?.let { it.currentWeapon = powerUp.weaponType }
                powerUp.isActive = false
                SoundManager.play(SoundManager.Sound.WEAPON_SWITCH)
            }
        }
    }

    private fun bulletHitsEntity(bullet: Bullet, entity: com.ironfury.laststand.entities.Entity): Boolean {
        val bulletRect = RectF(
            bullet.position.x - bullet.width / 2,
            bullet.position.y - bullet.height / 2,
            bullet.position.x + bullet.width / 2,
            bullet.position.y + bullet.height / 2
        )
        return entity.hitbox.intersect(bulletRect)
    }

    private fun entityCollision(a: com.ironfury.laststand.entities.Entity, b: com.ironfury.laststand.entities.Entity): Boolean {
        return RectF.intersects(a.hitbox, b.hitbox)
    }

    private fun handlePlayerHit() {
        if (player.takeDamage()) {
            SoundManager.play(SoundManager.Sound.PLAYER_DEATH)
            SoundManager.play(SoundManager.Sound.GAME_OVER)
            MusicManager.play(MusicManager.Music.GAME_OVER)
            weaponManager.highScore = player.score  // Save high score
            status = GameStatus.GAME_OVER
            com.ironfury.laststand.ads.AdManager.showInterstitial(
                context as? android.app.Activity, "game_over"
            )
        } else {
            SoundManager.play(SoundManager.Sound.PLAYER_HIT)
            player.respawn()
            // Move camera to checkpoint position
            camera.jumpTo(player.checkpointX)
        }
    }

    fun render(canvas: Canvas, screenWidth: Int, screenHeight: Int, controller: TouchController) {
        val scale = screenHeight / Constants.GAME_HEIGHT

        // Render start screen or settings
        if (status == GameStatus.START_SCREEN) {
            renderStartScreen(canvas, screenWidth, screenHeight, scale)
            return
        }
        if (status == GameStatus.SETTINGS) {
            renderSettingsScreen(canvas, screenWidth, screenHeight, scale)
            return
        }
        if (status == GameStatus.CHARACTERS) {
            renderCharacterScreen(canvas, screenWidth, screenHeight, scale)
            return
        }

        // Render level background
        level.render(canvas, camera.x, scale, screenWidth, screenHeight)

        // Render enemies
        for (enemy in enemies) {
            enemy.render(canvas, camera.x, scale)
        }

        // Render boss
        boss?.render(canvas, camera.x, scale)

        // Render player
        player.render(canvas, camera.x, scale)

        // Render bullets
        for (bullet in bullets) {
            bullet.render(canvas, camera.x, scale)
        }

        // Render explosions
        for (explosion in explosions) {
            explosion.render(canvas, camera.x, scale)
        }

        // Render coins
        for (coin in coins) {
            coin.render(canvas, camera.x, scale)
        }

        // Render power-ups
        for (powerUp in powerUps) {
            powerUp.render(canvas, camera.x, scale)
        }

        // Render controls
        controller.render(canvas)

        // Render weapon selector
        weaponSelector?.render(canvas)

        // Render HUD
        renderHUD(canvas, screenWidth, screenHeight, scale)

        // Render game over / victory screen
        if (status != GameStatus.PLAYING) {
            renderEndScreen(canvas, screenWidth, screenHeight)
        }
    }

    private fun renderHUD(canvas: Canvas, screenWidth: Int, screenHeight: Int, scale: Float) {
        hudPaint.setShadowLayer(1.5f, 1f, 1f, Color.argb(120, 0, 0, 0))

        // Hearts (top left) — pixel-art chunky hearts
        val heartSize = 14f * scale
        val heartY = 12f * scale
        for (i in 0 until Constants.PLAYER_MAX_LIVES) {
            val hx = 10f * scale + i * (heartSize * 1.25f)
            if (i < player.lives) {
                hudPaint.color = Color.rgb(220, 40, 40)
            } else {
                hudPaint.color = Color.argb(60, 100, 40, 40)
            }
            drawHeart(canvas, hx, heartY, heartSize, hudPaint)
        }

        // Level indicator (below hearts)
        hudPaint.textSize = 7f * scale
        hudPaint.color = Color.argb(180, 200, 220, 255)
        canvas.drawText(
            "LV.${"%02d".format(currentLevel)}  ${level.theme.name.uppercase()}",
            10f * scale,
            heartY + heartSize + 14f * scale,
            hudPaint
        )

        // Score (top right, under coin pill)
        hudPaint.textSize = 8f * scale
        hudPaint.color = Color.argb(180, 255, 255, 255)
        hudPaint.textAlign = Paint.Align.RIGHT
        canvas.drawText(
            "SCORE  ${"%,d".format(player.score)}",
            screenWidth - 14f * scale,
            heartY + heartSize + 14f * scale,
            hudPaint
        )
        hudPaint.textAlign = Paint.Align.LEFT

        // Rocket ammo (below score, only when using rockets)
        if (player.currentWeapon == WeaponType.ROCKET_LAUNCHER) {
            hudPaint.textSize = 11f * scale
            hudPaint.color = Color.argb(180, 255, 140, 80)
            hudPaint.textAlign = Paint.Align.RIGHT
            canvas.drawText("ROCKETS: ${player.rocketAmmo}/${Player.MAX_ROCKETS}", screenWidth - 12f * scale, 34f * scale, hudPaint)
            hudPaint.textAlign = Paint.Align.LEFT
        }

        // Weapon name is rendered by WeaponSelector itself; no duplicate label here.
        hudPaint.textAlign = Paint.Align.LEFT

        renderProgressBar(canvas, screenWidth, screenHeight, scale)

        hudPaint.setShadowLayer(2f, 2f, 2f, Color.BLACK)
    }

    private val progressPaint = Paint().apply { isAntiAlias = false }

    private fun renderProgressBar(canvas: Canvas, screenWidth: Int, screenHeight: Int, scale: Float) {
        // Start → Boss timeline, centered at the bottom of the play area.
        val lw = level.width
        // Boss spawns at lw - 600; treat that point as "100% to boss".
        val startX = 100f
        val bossX = lw - 600f
        val progress = ((player.position.x - startX) / (bossX - startX)).coerceIn(0f, 1f)

        val sw = screenWidth.toFloat()
        val barW = sw * 0.32f
        val barH = 8f * scale
        val barLeft = (sw - barW) / 2f
        val barTop = screenHeight - 28f * scale
        val labelOffset = 6f * scale

        // Labels
        progressPaint.typeface = pixelFont
        progressPaint.textSize = 8f * scale
        progressPaint.textAlign = Paint.Align.RIGHT
        progressPaint.color = Color.argb(180, 90, 220, 255)
        canvas.drawText("START", barLeft - labelOffset, barTop + barH * 0.85f, progressPaint)
        progressPaint.textAlign = Paint.Align.LEFT
        progressPaint.color = Color.argb(180, 255, 90, 90)
        canvas.drawText("BOSS", barLeft + barW + labelOffset, barTop + barH * 0.85f, progressPaint)

        // Track (dim segments)
        val segments = 12
        val segGap = 2f * scale
        val segW = (barW - segGap * (segments - 1)) / segments
        val filled = (progress * segments).toInt().coerceAtMost(segments)
        for (i in 0 until segments) {
            val x = barLeft + i * (segW + segGap)
            progressPaint.color = if (i < filled) {
                Color.rgb(90, 220, 255)
            } else {
                Color.argb(80, 90, 220, 255)
            }
            canvas.drawRect(x, barTop, x + segW, barTop + barH, progressPaint)
        }
        // Partial fill for current segment
        if (filled < segments) {
            val frac = (progress * segments) - filled
            if (frac > 0f) {
                val x = barLeft + filled * (segW + segGap)
                progressPaint.color = Color.argb((200 * frac).toInt().coerceIn(0, 255), 90, 220, 255)
                canvas.drawRect(x, barTop, x + segW * frac, barTop + barH, progressPaint)
            }
        }
    }

    private fun drawHeart(canvas: Canvas, cx: Float, cy: Float, size: Float, paint: Paint) {
        // Pixel-art heart: 7-wide x 6-tall block grid.
        // Row patterns (1 = filled), forming a chunky heart with two lobes.
        val rows = arrayOf(
            "0110110",
            "1111111",
            "1111111",
            "0111110",
            "0011100",
            "0001000"
        )
        val px = size / 7f
        val savedStyle = paint.style
        paint.style = Paint.Style.FILL
        for ((r, row) in rows.withIndex()) {
            for ((c, ch) in row.withIndex()) {
                if (ch == '1') {
                    val x = cx + c * px
                    val y = cy + r * px
                    canvas.drawRect(x, y, x + px, y + px, paint)
                }
            }
        }
        paint.style = savedStyle
    }

    private fun renderEndScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int) {
        canvas.drawColor(Color.argb(180, 0, 0, 0))

        val scale = screenHeight / Constants.GAME_HEIGHT
        val centerX = screenWidth / 2f
        val centerY = screenHeight / 2f

        when (status) {
            GameStatus.GAME_OVER -> renderGameOverScreen(canvas, centerX, centerY, scale)
            GameStatus.LEVEL_COMPLETE -> renderLevelCompleteScreen(canvas, centerX, centerY, scale)
            GameStatus.VICTORY -> renderVictoryScreen(canvas, centerX, centerY, scale)
            else -> {}
        }
    }

    private fun drawTitleWithShadow(
        canvas: Canvas, text: String, x: Float, y: Float, size: Float,
        fill: Int, shadow: Int, offset: Float
    ) {
        gameOverPaint.textSize = size
        gameOverPaint.color = shadow
        canvas.drawText(text, x + offset, y + offset, gameOverPaint)
        gameOverPaint.color = fill
        canvas.drawText(text, x, y, gameOverPaint)
    }

    private fun drawStatBox(
        canvas: Canvas, cx: Float, cy: Float, w: Float, h: Float,
        border: Int = Color.argb(220, 255, 210, 70)
    ) {
        val rect = RectF(cx - w / 2f, cy - h / 2f, cx + w / 2f, cy + h / 2f)
        val p = Paint().apply { isAntiAlias = false }
        p.color = Color.argb(220, 18, 22, 30)
        canvas.drawRoundRect(rect, 4f, 4f, p)
        p.style = Paint.Style.STROKE
        p.strokeWidth = 2f
        p.color = border
        canvas.drawRoundRect(rect, 4f, 4f, p)
    }

    private fun renderGameOverScreen(canvas: Canvas, centerX: Float, centerY: Float, scale: Float) {
        // Big "GAME OVER" — red with darker red shadow
        drawTitleWithShadow(
            canvas, "GAME", centerX, centerY - 60f * scale,
            48f * scale, Color.rgb(255, 60, 60), Color.rgb(120, 0, 0), 4f * scale
        )
        drawTitleWithShadow(
            canvas, "OVER", centerX, centerY - 10f * scale,
            48f * scale, Color.rgb(255, 60, 60), Color.rgb(120, 0, 0), 4f * scale
        )

        // Stat strip
        drawStatBox(canvas, centerX, centerY + 30f * scale, 220f * scale, 26f * scale)
        gameOverPaint.textSize = 10f * scale
        gameOverPaint.color = Color.rgb(100, 255, 100)
        canvas.drawText("LV.${"%02d".format(currentLevel)}", centerX - 70f * scale, centerY + 34f * scale, gameOverPaint)
        gameOverPaint.color = Color.rgb(255, 100, 100)
        canvas.drawText("|", centerX - 30f * scale, centerY + 34f * scale, gameOverPaint)
        gameOverPaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("SCORE  ${"%,d".format(player.score)}", centerX + 30f * scale, centerY + 34f * scale, gameOverPaint)

        // Restart hint
        gameOverPaint.textSize = 7f * scale
        gameOverPaint.color = Color.rgb(120, 200, 255)
        canvas.drawText("↻ Retry Level ${"%02d".format(currentLevel)}", centerX, centerY + 60f * scale, gameOverPaint)

        // Pulsing CTA
        val pulseAlpha = ((kotlin.math.sin(levelCompleteTime * 5.0) * 0.3 + 0.7) * 255).toInt()
        drawTitleWithShadow(
            canvas, "▸  TAP TO RESTART  ◂", centerX, centerY + 95f * scale,
            14f * scale, Color.argb(pulseAlpha, 255, 210, 70), Color.argb(pulseAlpha / 2, 100, 60, 0), 2f * scale
        )
    }

    private fun renderLevelCompleteScreen(canvas: Canvas, centerX: Float, centerY: Float, scale: Float) {
        val pulse = (kotlin.math.sin(levelCompleteTime * 4.0) * 0.08 + 0.92).toFloat()

        // Big "LEVEL XX CLEAR!" — green with darker green shadow
        drawTitleWithShadow(
            canvas, "LEVEL ${"%02d".format(currentLevel)} CLEAR!",
            centerX, centerY - 50f * scale,
            28f * scale * pulse, Color.rgb(80, 240, 120), Color.rgb(20, 100, 40), 3f * scale
        )

        // Theme subtitle
        gameOverPaint.textSize = 10f * scale
        gameOverPaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("\"${level.theme.name}\" cleared", centerX, centerY - 18f * scale, gameOverPaint)

        // Stat box (score + coins + next)
        drawStatBox(canvas, centerX, centerY + 30f * scale, 280f * scale, 50f * scale)
        gameOverPaint.textSize = 10f * scale
        gameOverPaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("SCORE  ${"%,d".format(player.score)}", centerX, centerY + 20f * scale, gameOverPaint)

        if (currentLevel < MAX_LEVEL) {
            val nextTheme = com.ironfury.laststand.level.LevelThemes.get(currentLevel + 1)
            gameOverPaint.textSize = 8f * scale
            gameOverPaint.color = Color.rgb(120, 200, 255)
            canvas.drawText("NEXT  ${nextTheme.name.uppercase()}", centerX, centerY + 42f * scale, gameOverPaint)
        }

        // Pulsing CTA
        val tapAlpha = ((kotlin.math.sin(levelCompleteTime * 5.0) * 0.3 + 0.7) * 255).toInt()
        gameOverPaint.textSize = 12f * scale
        gameOverPaint.color = Color.argb(tapAlpha, 255, 255, 255)
        canvas.drawText("▸  TAP TO CONTINUE  ◂", centerX, centerY + 95f * scale, gameOverPaint)
    }

    private fun renderVictoryScreen(canvas: Canvas, centerX: Float, centerY: Float, scale: Float) {
        drawTitleWithShadow(
            canvas, "VICTORY!", centerX, centerY - 30f * scale,
            42f * scale, Color.rgb(255, 215, 70), Color.rgb(140, 80, 0), 4f * scale
        )

        gameOverPaint.textSize = 10f * scale
        gameOverPaint.color = Color.rgb(220, 220, 220)
        canvas.drawText("★  ALL 20 LEVELS CONQUERED  ★", centerX, centerY + 5f * scale, gameOverPaint)

        // Final score box
        drawStatBox(canvas, centerX, centerY + 45f * scale, 200f * scale, 28f * scale)
        gameOverPaint.textSize = 12f * scale
        gameOverPaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("FINAL  ·  ${"%,d".format(player.score)}", centerX, centerY + 50f * scale, gameOverPaint)

        val tapAlpha = ((kotlin.math.sin(levelCompleteTime * 5.0) * 0.3 + 0.7) * 255).toInt()
        gameOverPaint.textSize = 12f * scale
        gameOverPaint.color = Color.argb(tapAlpha, 255, 255, 255)
        canvas.drawText("▸  TAP TO PLAY AGAIN  ◂", centerX, centerY + 95f * scale, gameOverPaint)
    }

    private fun renderStartScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int, scale: Float) {
        val sh = screenHeight.toFloat()
        val sw = screenWidth.toFloat()

        // Animated background gradient
        val bgPaint = Paint()
        val gradientOffset = (kotlin.math.sin(startScreenTime * 0.5) * 50).toInt()
        bgPaint.shader = android.graphics.LinearGradient(
            0f, 0f, 0f, sh,
            intArrayOf(
                Color.rgb(20 + gradientOffset / 5, 10, 40),
                Color.rgb(40, 20 + gradientOffset / 5, 60),
                Color.rgb(20, 30, 50 + gradientOffset / 3)
            ),
            floatArrayOf(0f, 0.5f, 1f),
            android.graphics.Shader.TileMode.CLAMP
        )
        canvas.drawRect(0f, 0f, sw, sh, bgPaint)

        // Animated stars
        val starPaint = Paint().apply {
            color = Color.WHITE
            isAntiAlias = true
        }
        for (i in 0 until 50) {
            val starX = ((i * 137 + startScreenTime * 20 * (i % 3 + 1)) % screenWidth).toFloat()
            val starY = ((i * 89) % screenHeight).toFloat()
            val twinkle = (kotlin.math.sin(startScreenTime * 3 + i) * 0.5 + 0.5).toFloat()
            starPaint.alpha = (100 + twinkle * 155).toInt()
            canvas.drawCircle(starX, starY, 1.5f * scale * twinkle, starPaint)
        }

        val centerX = sw / 2f
        val titleBounce = (kotlin.math.sin(startScreenTime * 2.0) * 3.0).toFloat()

        val titlePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = pixelFont
            isAntiAlias = false
        }

        // === TITLE — top 25% of screen ===
        val titleY = sh * 0.18f + titleBounce

        titlePaint.textSize = 28f * scale
        titlePaint.color = Color.rgb(120, 0, 0)
        canvas.drawText("IRON FURY", centerX + 3f * scale, titleY + 3f * scale, titlePaint)
        titlePaint.color = Color.rgb(255, 60, 60)
        canvas.drawText("IRON FURY", centerX, titleY, titlePaint)

        // Subtitle with stars
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("★  LAST STAND  ★", centerX, titleY + 24f * scale, titlePaint)

        // === SOLDIER — center of screen ===
        renderStartScreenSoldier(canvas, centerX, sh * 0.52f, scale)

        // === TAP TO START — below soldier ===
        val pulseAlpha = ((kotlin.math.sin(startScreenTime * 4) * 0.3 + 0.7) * 255).toInt()
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.argb(pulseAlpha, 255, 255, 255)
        canvas.drawText("▸  TAP TO START  ◂", centerX, sh * 0.78f, titlePaint)

        // === Bottom info — bottom 15% ===
        // === Stats row: HI {score}   {coinIcon} {coins} ===
        val statsY = sh * 0.86f
        titlePaint.textSize = 8f * scale
        val hiLabel = "HI "
        val hiScore = String.format("%,d", weaponManager.highScore)
        val coinAmount = "${weaponManager.coins}"
        val gap = 24f * scale

        titlePaint.textAlign = Paint.Align.LEFT
        val labelW = titlePaint.measureText(hiLabel)
        val scoreW = titlePaint.measureText(hiScore)
        val coinIconW = 14f * scale
        val coinTextW = titlePaint.measureText(coinAmount)
        val totalW = labelW + scoreW + gap + coinIconW + 6f * scale + coinTextW
        var statsX = centerX - totalW / 2f

        titlePaint.color = Color.rgb(220, 220, 220)
        canvas.drawText(hiLabel, statsX, statsY, titlePaint)
        statsX += labelW
        titlePaint.color = Color.rgb(90, 220, 255)
        canvas.drawText(hiScore, statsX, statsY, titlePaint)
        statsX += scoreW + gap

        val coinPaint = Paint().apply { isAntiAlias = true }
        coinPaint.color = Color.rgb(255, 200, 50)
        canvas.drawCircle(statsX + coinIconW / 2f, statsY - 4f * scale, coinIconW / 2f, coinPaint)
        coinPaint.color = Color.rgb(255, 230, 120)
        canvas.drawCircle(statsX + coinIconW / 2f - 2f, statsY - 6f * scale, coinIconW / 4f, coinPaint)
        statsX += coinIconW + 6f * scale

        titlePaint.color = Color.rgb(255, 210, 70)
        canvas.drawText(coinAmount, statsX, statsY, titlePaint)
        titlePaint.textAlign = Paint.Align.CENTER

        // Tagline
        titlePaint.textSize = 7f * scale
        titlePaint.color = Color.rgb(110, 180, 110)
        canvas.drawText("20 LEVELS  \u00b7  20 BOSSES  \u00b7  ENDLESS ACTION", centerX, sh * 0.94f, titlePaint)

        // === SETTINGS GEAR BUTTON — top right ===
        val gearSize = 28f * scale
        val gearX = sw - gearSize * 1.5f
        val gearY = gearSize * 1.5f
        settingsButtonRect.set(gearX - gearSize, gearY - gearSize, gearX + gearSize, gearY + gearSize)

        // Gear icon
        val gearPaint = Paint().apply { isAntiAlias = true; color = Color.argb(180, 200, 200, 200) }
        gearPaint.style = Paint.Style.STROKE
        gearPaint.strokeWidth = 3f * scale
        canvas.drawCircle(gearX, gearY, gearSize * 0.4f, gearPaint)
        // Gear teeth
        for (i in 0..5) {
            val angle = Math.toRadians(i * 60.0 + startScreenTime * 20)
            val ix = gearX + (kotlin.math.cos(angle) * gearSize * 0.55f).toFloat()
            val iy = gearY + (kotlin.math.sin(angle) * gearSize * 0.55f).toFloat()
            gearPaint.style = Paint.Style.FILL
            canvas.drawCircle(ix, iy, 4f * scale, gearPaint)
        }

        // === CHARACTERS BUTTON — top left ===
        val cbH = 24f * scale
        val cbW = 96f * scale
        val cbX = gearSize * 0.6f
        val cbY = gearY - cbH / 2f
        charactersButtonRect.set(cbX, cbY, cbX + cbW, cbY + cbH)

        val cbPaint = Paint().apply { isAntiAlias = true }
        cbPaint.color = Color.argb(200, 40, 30, 70)
        canvas.drawRoundRect(charactersButtonRect, 4f * scale, 4f * scale, cbPaint)
        cbPaint.style = Paint.Style.STROKE
        cbPaint.strokeWidth = 2f * scale
        cbPaint.color = Color.rgb(150, 120, 230)
        canvas.drawRoundRect(charactersButtonRect, 4f * scale, 4f * scale, cbPaint)

        // Little head icon
        cbPaint.style = Paint.Style.FILL
        val iconCX = cbX + cbH * 0.55f
        val iconCY = charactersButtonRect.centerY()
        cbPaint.color = weaponManager.selectedCharacter.skin
        canvas.drawCircle(iconCX, iconCY - 1f * scale, cbH * 0.22f, cbPaint)
        cbPaint.color = weaponManager.selectedCharacter.bandana
        canvas.drawRect(iconCX - cbH * 0.22f, iconCY - cbH * 0.12f, iconCX + cbH * 0.22f, iconCY - cbH * 0.04f, cbPaint)

        titlePaint.textAlign = Paint.Align.LEFT
        titlePaint.textSize = 7f * scale
        titlePaint.color = Color.rgb(220, 210, 255)
        canvas.drawText("CHARACTERS", cbX + cbH * 0.9f, iconCY + 3f * scale, titlePaint)
        titlePaint.textAlign = Paint.Align.CENTER
    }

    fun isSettingsButtonTap(x: Float, y: Float): Boolean {
        return settingsButtonRect.contains(x, y)
    }

    fun openSettings() {
        status = GameStatus.SETTINGS
    }

    fun closeSettings() {
        status = GameStatus.START_SCREEN
    }

    fun isCharactersButtonTap(x: Float, y: Float): Boolean {
        return charactersButtonRect.contains(x, y)
    }

    fun openCharacters() {
        status = GameStatus.CHARACTERS
    }

    fun closeCharacters() {
        status = GameStatus.START_SCREEN
    }

    /**
     * Handle a tap on the character-select screen: back button, equip an owned
     * character, or buy a locked one if the player can afford it.
     */
    fun handleCharacterTap(x: Float, y: Float) {
        if (characterBackRect.contains(x, y)) {
            closeCharacters()
            return
        }
        val skins = CharacterSkin.entries
        for (i in skins.indices) {
            if (i < characterCardRects.size && characterCardRects[i].contains(x, y)) {
                val skin = skins[i]
                when {
                    weaponManager.isCharacterUnlocked(skin) -> {
                        weaponManager.selectedCharacter = skin
                        applySelectedCharacter()
                        SoundManager.play(SoundManager.Sound.WEAPON_SWITCH)
                    }
                    weaponManager.unlockCharacter(skin) -> {
                        // Purchase succeeded — equip immediately.
                        weaponManager.selectedCharacter = skin
                        applySelectedCharacter()
                        SoundManager.play(SoundManager.Sound.COIN_COLLECT)
                    }
                    // else: locked and not enough coins — no-op.
                }
                return
            }
        }
    }

    /** Returns the tapped ControlSize, or null for back/no-op */
    fun handleSettingsTap(x: Float, y: Float): ControlSize? {
        if (settingsBackRect.contains(x, y)) {
            closeSettings()
            return null
        }
        if (settingsLayoutRect.contains(x, y)) {
            wantsLayoutEditor = true
            return null
        }
        if (settingsAdsRect.contains(x, y)) {
            val current = com.ironfury.laststand.ads.AdSettings.adsEnabled(context)
            com.ironfury.laststand.ads.AdSettings.setAdsEnabled(context, !current)
            return null
        }
        val sizes = ControlSize.entries
        for (i in sizes.indices) {
            if (i < settingsSizeRects.size && settingsSizeRects[i].contains(x, y)) {
                return sizes[i]
            }
        }
        return null
    }

    private fun renderSettingsScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int, scale: Float) {
        val sw = screenWidth.toFloat()
        val sh = screenHeight.toFloat()
        canvas.drawColor(Color.rgb(12, 14, 30))

        val centerX = sw / 2f
        val titlePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = pixelFont
            isAntiAlias = false
        }

        // === Title: SETTINGS (yellow with red shadow) + CONFIG subtitle ===
        val titleY = sh * 0.12f
        titlePaint.textSize = 24f * scale
        titlePaint.color = Color.rgb(120, 30, 0)
        canvas.drawText("SETTINGS", centerX + 3f * scale, titleY + 3f * scale, titlePaint)
        titlePaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("SETTINGS", centerX, titleY, titlePaint)
        titlePaint.textSize = 8f * scale
        titlePaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("\u2500\u2500\u2500\u2500\u2500  CONFIG  \u2500\u2500\u2500\u2500\u2500", centerX, titleY + 18f * scale, titlePaint)

        // === Layout: left = controls, right = preview ===
        val leftX = sw * 0.10f
        val rightPaneLeft = sw * 0.55f
        val rightPaneRight = sw * 0.92f
        val contentTop = sh * 0.28f

        // "CONTROL SIZE" label
        titlePaint.textAlign = Paint.Align.LEFT
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.rgb(220, 220, 220)
        canvas.drawText("CONTROL SIZE", leftX, contentTop, titlePaint)

        // 2x2 size button grid
        val sizes = ControlSize.entries
        val btnW = sw * 0.18f
        val btnH = sh * 0.10f
        val gap = sw * 0.012f
        val gridTop = contentTop + 10f * scale

        if (screenWidth != settingsScreenW || screenHeight != settingsScreenH) {
            settingsSizeRects.clear()
            for (i in sizes.indices) {
                val col = i % 2
                val row = i / 2
                val bx = leftX + col * (btnW + gap)
                val by = gridTop + row * (btnH + gap)
                settingsSizeRects.add(RectF(bx, by, bx + btnW, by + btnH))
            }
            settingsScreenW = screenWidth
            settingsScreenH = screenHeight
        }

        val currentSize = com.ironfury.laststand.utils.SettingsManager(context).controlSize
        val btnPaint = Paint().apply { isAntiAlias = false }
        val btnStroke = Paint().apply { isAntiAlias = false; style = Paint.Style.STROKE; strokeWidth = 2f * scale }

        for (i in sizes.indices) {
            val rect = settingsSizeRects[i]
            val isSelected = sizes[i] == currentSize

            btnPaint.color = if (isSelected) Color.rgb(255, 210, 70) else Color.rgb(18, 22, 50)
            canvas.drawRoundRect(rect, 3f * scale, 3f * scale, btnPaint)
            btnStroke.color = if (isSelected) Color.rgb(255, 230, 120) else Color.rgb(80, 90, 130)
            canvas.drawRoundRect(rect, 3f * scale, 3f * scale, btnStroke)

            titlePaint.textAlign = Paint.Align.CENTER
            titlePaint.textSize = 9f * scale
            titlePaint.color = if (isSelected) Color.rgb(20, 20, 20) else Color.rgb(220, 220, 220)
            val fullLabel = sizes[i].name.replace('_', ' ')
            val label = if (isSelected) "\u25b8 $fullLabel" else fullLabel
            canvas.drawText(label, rect.centerX(), rect.centerY() + 4f * scale, titlePaint)
        }

        // Description of selected size
        val descY = gridTop + 2 * (btnH + gap) + 14f * scale
        titlePaint.textAlign = Paint.Align.LEFT
        titlePaint.textSize = 9f * scale
        titlePaint.color = Color.rgb(255, 210, 70)
        canvas.drawText(currentSize.name.replace('_', ' '), leftX, descY, titlePaint)
        titlePaint.textSize = 7f * scale
        titlePaint.color = Color.rgb(180, 180, 200)
        val desc = when (currentSize) {
            ControlSize.SMALL -> "Smaller buttons \u2022 More screen space"
            ControlSize.MEDIUM -> "Default size \u2022 Balanced"
            ControlSize.LARGE -> "Bigger buttons \u2022 Easier to tap"
            ControlSize.EXTRA_LARGE -> "Maximum size \u2022 Best for big fingers"
        }
        canvas.drawText(desc, leftX, descY + 14f * scale, titlePaint)

        // === Ads toggle row ===
        val adsRowY = descY + 36f * scale
        val adsLabelW = sw * 0.22f
        val toggleW = 56f * scale
        val toggleH = 22f * scale
        settingsAdsRect.set(leftX + adsLabelW, adsRowY - toggleH * 0.7f,
                            leftX + adsLabelW + toggleW, adsRowY + toggleH * 0.3f)

        titlePaint.textSize = 9f * scale
        titlePaint.color = Color.rgb(220, 220, 220)
        titlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText("SHOW ADS", leftX, adsRowY, titlePaint)

        val adsEnabled = com.ironfury.laststand.ads.AdSettings.adsEnabled(context)
        btnPaint.color = if (adsEnabled) Color.rgb(80, 200, 120) else Color.rgb(60, 30, 30)
        canvas.drawRoundRect(settingsAdsRect, toggleH * 0.5f, toggleH * 0.5f, btnPaint)
        btnStroke.color = if (adsEnabled) Color.rgb(140, 240, 170) else Color.rgb(120, 80, 80)
        canvas.drawRoundRect(settingsAdsRect, toggleH * 0.5f, toggleH * 0.5f, btnStroke)

        // Knob
        val knobR = toggleH * 0.36f
        val knobCY = settingsAdsRect.centerY()
        val knobCX = if (adsEnabled) settingsAdsRect.right - toggleH * 0.5f
                     else settingsAdsRect.left + toggleH * 0.5f
        btnPaint.color = Color.WHITE
        canvas.drawCircle(knobCX, knobCY, knobR, btnPaint)

        // ON / OFF label
        titlePaint.textSize = 7f * scale
        titlePaint.color = if (adsEnabled) Color.rgb(255, 255, 255) else Color.rgb(180, 140, 140)
        titlePaint.textAlign = Paint.Align.LEFT
        canvas.drawText(
            if (adsEnabled) "ON" else "OFF",
            settingsAdsRect.right + 8f * scale,
            adsRowY,
            titlePaint
        )

        // === Right pane: PREVIEW ===
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.rgb(220, 220, 220)
        canvas.drawText("PREVIEW", rightPaneLeft, contentTop, titlePaint)

        val previewRect = RectF(rightPaneLeft, contentTop + 10f * scale, rightPaneRight, contentTop + 10f * scale + sh * 0.4f)
        // Sky portion
        btnPaint.color = Color.rgb(28, 40, 70)
        canvas.drawRect(previewRect.left, previewRect.top, previewRect.right, previewRect.centerY() + previewRect.height() * 0.15f, btnPaint)
        // Ground portion
        btnPaint.color = Color.rgb(110, 120, 60)
        canvas.drawRect(previewRect.left, previewRect.centerY() + previewRect.height() * 0.15f, previewRect.right, previewRect.bottom, btnPaint)
        // Border
        btnStroke.color = Color.rgb(60, 80, 130)
        canvas.drawRect(previewRect, btnStroke)

        // Mini controls inside preview
        val pvMult = currentSize.multiplier
        val pvCx = previewRect.left + previewRect.width() * 0.20f
        val pvCy = previewRect.bottom - previewRect.height() * 0.20f
        val pvDpadR = 22f * scale * pvMult
        // Mini d-pad cross
        btnPaint.color = Color.rgb(180, 170, 50)
        val arm = pvDpadR * 0.4f
        canvas.drawRect(pvCx - arm, pvCy - pvDpadR / 2, pvCx + arm, pvCy + pvDpadR / 2, btnPaint)
        canvas.drawRect(pvCx - pvDpadR / 2, pvCy - arm, pvCx + pvDpadR / 2, pvCy + arm, btnPaint)
        // Mini jump/fire
        val pvBtn = 14f * scale * pvMult
        val pvBtnX = previewRect.right - previewRect.width() * 0.20f - pvBtn * 1.4f
        btnPaint.color = Color.rgb(90, 190, 240)
        canvas.drawCircle(pvBtnX, pvCy, pvBtn, btnPaint)
        btnPaint.color = Color.rgb(230, 70, 70)
        canvas.drawCircle(pvBtnX + pvBtn * 2.4f, pvCy, pvBtn, btnPaint)

        // === Bottom buttons ===
        // BACK (left)
        val btmY = sh * 0.85f
        val backW = sw * 0.16f
        val backH = sh * 0.08f
        settingsBackRect.set(leftX, btmY, leftX + backW, btmY + backH)
        btnPaint.color = Color.rgb(20, 22, 40)
        canvas.drawRoundRect(settingsBackRect, 3f * scale, 3f * scale, btnPaint)
        btnStroke.color = Color.rgb(120, 140, 200)
        canvas.drawRoundRect(settingsBackRect, 3f * scale, 3f * scale, btnStroke)
        titlePaint.textAlign = Paint.Align.CENTER
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.WHITE
        canvas.drawText("\u25c2 BACK", settingsBackRect.centerX(), settingsBackRect.centerY() + 4f * scale, titlePaint)

        // CUSTOMIZE LAYOUT (right, red/pink)
        val layoutW = sw * 0.28f
        val layoutX = sw - layoutW - sw * 0.08f
        settingsLayoutRect.set(layoutX, btmY, layoutX + layoutW, btmY + backH)
        btnPaint.color = Color.rgb(230, 60, 90)
        canvas.drawRoundRect(settingsLayoutRect, 3f * scale, 3f * scale, btnPaint)
        btnStroke.color = Color.rgb(255, 120, 150)
        canvas.drawRoundRect(settingsLayoutRect, 3f * scale, 3f * scale, btnStroke)
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.WHITE
        canvas.drawText("CUSTOMIZE LAYOUT \u25b8", settingsLayoutRect.centerX(), settingsLayoutRect.centerY() + 4f * scale, titlePaint)
    }

    private fun renderCharacterScreen(canvas: Canvas, screenWidth: Int, screenHeight: Int, scale: Float) {
        val sw = screenWidth.toFloat()
        val sh = screenHeight.toFloat()
        canvas.drawColor(Color.rgb(14, 12, 28))

        val centerX = sw / 2f
        val titlePaint = Paint().apply {
            textAlign = Paint.Align.CENTER
            typeface = pixelFont
            isAntiAlias = false
        }

        // === Title ===
        val titleY = sh * 0.12f
        titlePaint.textSize = 24f * scale
        titlePaint.color = Color.rgb(70, 30, 120)
        canvas.drawText("CHARACTERS", centerX + 3f * scale, titleY + 3f * scale, titlePaint)
        titlePaint.color = Color.rgb(190, 160, 255)
        canvas.drawText("CHARACTERS", centerX, titleY, titlePaint)

        // Coin balance (top-right)
        titlePaint.textAlign = Paint.Align.RIGHT
        titlePaint.textSize = 9f * scale
        val coinPaint = Paint().apply { isAntiAlias = true }
        val coinCx = sw * 0.88f
        coinPaint.color = Color.rgb(255, 200, 50)
        canvas.drawCircle(coinCx, titleY - 3f * scale, 7f * scale, coinPaint)
        coinPaint.color = Color.rgb(255, 230, 120)
        canvas.drawCircle(coinCx - 2f * scale, titleY - 5f * scale, 3f * scale, coinPaint)
        titlePaint.color = Color.rgb(255, 210, 70)
        canvas.drawText("${weaponManager.coins}", coinCx - 11f * scale, titleY, titlePaint)
        titlePaint.textAlign = Paint.Align.CENTER

        // === Card grid: 3 columns x 2 rows ===
        val skins = CharacterSkin.entries
        val cols = 3
        val gridLeft = sw * 0.06f
        val gridRight = sw * 0.94f
        val gapX = sw * 0.02f
        val gapY = sh * 0.035f
        val cardW = (gridRight - gridLeft - gapX * (cols - 1)) / cols
        val cardH = sh * 0.28f
        val gridTop = sh * 0.22f

        if (screenWidth != characterScreenW || screenHeight != characterScreenH) {
            characterCardRects.clear()
            for (i in skins.indices) {
                val col = i % cols
                val row = i / cols
                val cx = gridLeft + col * (cardW + gapX)
                val cy = gridTop + row * (cardH + gapY)
                characterCardRects.add(RectF(cx, cy, cx + cardW, cy + cardH))
            }
            characterScreenW = screenWidth
            characterScreenH = screenHeight
        }

        val cardPaint = Paint().apply { isAntiAlias = true }
        val cardStroke = Paint().apply { isAntiAlias = true; style = Paint.Style.STROKE; strokeWidth = 2f * scale }
        val selected = weaponManager.selectedCharacter

        for (i in skins.indices) {
            if (i >= characterCardRects.size) break
            val skin = skins[i]
            val rect = characterCardRects[i]
            val unlocked = weaponManager.isCharacterUnlocked(skin)
            val isSelected = skin == selected
            val affordable = weaponManager.coins >= skin.price

            // Card background
            cardPaint.color = when {
                isSelected -> Color.rgb(40, 50, 90)
                unlocked -> Color.rgb(26, 28, 50)
                else -> Color.rgb(20, 20, 30)
            }
            canvas.drawRoundRect(rect, 5f * scale, 5f * scale, cardPaint)
            cardStroke.color = when {
                isSelected -> Color.rgb(255, 210, 70)
                unlocked -> Color.rgb(90, 100, 150)
                else -> Color.rgb(60, 60, 80)
            }
            canvas.drawRoundRect(rect, 5f * scale, 5f * scale, cardStroke)

            // Avatar
            val avatarCY = rect.top + cardH * 0.38f
            drawCharacterAvatar(canvas, rect.centerX(), avatarCY, cardH * 0.30f, skin)

            // Dim overlay + lock for locked characters
            if (!unlocked) {
                cardPaint.color = Color.argb(150, 10, 10, 18)
                canvas.drawRoundRect(rect, 5f * scale, 5f * scale, cardPaint)
                // Padlock
                cardPaint.color = Color.rgb(200, 200, 210)
                val lkX = rect.centerX()
                val lkY = rect.top + cardH * 0.34f
                val lkS = cardH * 0.12f
                canvas.drawRoundRect(lkX - lkS * 0.7f, lkY, lkX + lkS * 0.7f, lkY + lkS, 2f * scale, 2f * scale, cardPaint)
                cardStroke.color = Color.rgb(200, 200, 210)
                cardStroke.strokeWidth = 2.5f * scale
                canvas.drawArc(lkX - lkS * 0.45f, lkY - lkS * 0.7f, lkX + lkS * 0.45f, lkY + lkS * 0.2f, 180f, 180f, false, cardStroke)
                cardStroke.strokeWidth = 2f * scale
            }

            // Name
            titlePaint.textSize = 7.5f * scale
            titlePaint.color = if (unlocked) Color.rgb(235, 235, 245) else Color.rgb(150, 150, 165)
            canvas.drawText(skin.displayName, rect.centerX(), rect.top + cardH * 0.72f, titlePaint)

            // Status line
            titlePaint.textSize = 7f * scale
            val statusY = rect.top + cardH * 0.90f
            when {
                isSelected -> {
                    titlePaint.color = Color.rgb(120, 255, 140)
                    canvas.drawText("✓ EQUIPPED", rect.centerX(), statusY, titlePaint)
                }
                unlocked -> {
                    titlePaint.color = Color.rgb(255, 210, 70)
                    canvas.drawText("TAP TO EQUIP", rect.centerX(), statusY, titlePaint)
                }
                else -> {
                    titlePaint.color = if (affordable) Color.rgb(255, 220, 90) else Color.rgb(220, 90, 90)
                    canvas.drawText("◆ ${skin.price}", rect.centerX(), statusY, titlePaint)
                }
            }
        }

        // === BACK button ===
        val backW = sw * 0.16f
        val backH = sh * 0.09f
        val backX = sw * 0.06f
        val backY = sh * 0.88f
        characterBackRect.set(backX, backY, backX + backW, backY + backH)
        cardPaint.color = Color.rgb(20, 22, 40)
        canvas.drawRoundRect(characterBackRect, 3f * scale, 3f * scale, cardPaint)
        cardStroke.color = Color.rgb(120, 140, 200)
        canvas.drawRoundRect(characterBackRect, 3f * scale, 3f * scale, cardStroke)
        titlePaint.textSize = 10f * scale
        titlePaint.color = Color.WHITE
        canvas.drawText("◂ BACK", characterBackRect.centerX(), characterBackRect.centerY() + 4f * scale, titlePaint)

        // Hint
        titlePaint.textSize = 7f * scale
        titlePaint.color = Color.rgb(150, 150, 180)
        canvas.drawText("EARN COINS BY PLAYING  ·  TAP A LOCKED HERO TO BUY", centerX, sh * 0.95f, titlePaint)
    }

    /** Compact standing-soldier avatar used on the character-select cards. */
    private fun drawCharacterAvatar(canvas: Canvas, cx: Float, cy: Float, s: Float, skin: CharacterSkin) {
        val p = Paint().apply { isAntiAlias = true }
        // Legs
        p.color = skin.pants
        canvas.drawRect(cx - s * 0.30f, cy + s * 0.18f, cx - s * 0.04f, cy + s * 0.68f, p)
        canvas.drawRect(cx + s * 0.04f, cy + s * 0.18f, cx + s * 0.30f, cy + s * 0.68f, p)
        // Boots
        p.color = skin.boots
        canvas.drawRect(cx - s * 0.32f, cy + s * 0.60f, cx - s * 0.02f, cy + s * 0.72f, p)
        canvas.drawRect(cx + s * 0.02f, cy + s * 0.60f, cx + s * 0.32f, cy + s * 0.72f, p)
        // Arms
        p.color = skin.skin
        canvas.drawRect(cx - s * 0.46f, cy - s * 0.18f, cx - s * 0.30f, cy + s * 0.14f, p)
        canvas.drawRect(cx + s * 0.30f, cy - s * 0.18f, cx + s * 0.46f, cy + s * 0.14f, p)
        // Torso
        p.color = skin.shirt
        canvas.drawRoundRect(cx - s * 0.34f, cy - s * 0.24f, cx + s * 0.34f, cy + s * 0.22f, s * 0.08f, s * 0.08f, p)
        // Vest plate highlight
        p.color = skin.shirtHi
        canvas.drawRect(cx - s * 0.10f, cy - s * 0.18f, cx + s * 0.10f, cy + s * 0.16f, p)
        // Head
        p.color = skin.skin
        canvas.drawCircle(cx, cy - s * 0.42f, s * 0.20f, p)
        // Hair (top arc)
        p.color = skin.hair
        canvas.drawArc(cx - s * 0.20f, cy - s * 0.64f, cx + s * 0.20f, cy - s * 0.24f, 180f, 180f, true, p)
        // Bandana band
        p.color = skin.bandana
        canvas.drawRect(cx - s * 0.21f, cy - s * 0.48f, cx + s * 0.21f, cy - s * 0.40f, p)
        // Eye
        p.color = Color.WHITE
        canvas.drawCircle(cx + s * 0.07f, cy - s * 0.40f, s * 0.035f, p)
        p.color = Color.BLACK
        canvas.drawCircle(cx + s * 0.075f, cy - s * 0.40f, s * 0.018f, p)
    }

    private fun renderStartScreenSoldier(canvas: Canvas, centerX: Float, centerY: Float, scale: Float) {
        StartScreenSoldier.render(canvas, centerX, centerY, scale, startScreenTime, weaponManager.selectedCharacter)
    }

    fun continueFromCheckpoint() {
        player.apply {
            lives = Constants.PLAYER_MAX_LIVES
            respawn()
        }
        bullets.clear()
        enemies.clear()
        explosions.clear()
        camera.jumpTo(player.checkpointX)
        status = GameStatus.PLAYING
        MusicManager.play(MusicManager.Music.GAMEPLAY)
    }

    /**
     * Restart the run from the CURRENT level after losing all lives. Refills
     * lives and resets the level's enemies/score-for-this-attempt, but keeps the
     * player on whatever level they died on instead of sending them back to 1.
     */
    fun restartCurrentLevel() {
        level.loadLevel(currentLevel)
        player.apply {
            lives = Constants.PLAYER_MAX_LIVES
            score = 0
            currentWeapon = WeaponType.MACHINE_GUN
            resetForNewLevel()
            respawn()
        }
        bullets.clear()
        enemies.clear()
        explosions.clear()
        coins.clear()
        powerUps.clear()
        boss = null
        bossSpawned = false
        bossDefeated = false
        camera.reset()
        syncLevelWidth()
        status = GameStatus.PLAYING
        MusicManager.play(MusicManager.Music.GAMEPLAY)
    }

    fun restart() {
        currentLevel = 1
        level.loadLevel(1)
        player.apply {
            lives = Constants.PLAYER_MAX_LIVES
            score = 0
            currentWeapon = WeaponType.MACHINE_GUN
            resetForNewLevel()
            respawn()
        }
        bullets.clear()
        enemies.clear()
        explosions.clear()
        coins.clear()
        powerUps.clear()
        boss = null
        bossSpawned = false
        bossDefeated = false
        camera.reset()
        syncLevelWidth()
        status = GameStatus.PLAYING

        // Restart gameplay music
        MusicManager.play(MusicManager.Music.GAMEPLAY)
    }
}
