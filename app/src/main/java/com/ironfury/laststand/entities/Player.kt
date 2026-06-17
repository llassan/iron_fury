package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.LinearGradient
import android.graphics.Shader
import com.ironfury.laststand.audio.SoundManager
import com.ironfury.laststand.cosmetics.CharacterSkin
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Direction
import com.ironfury.laststand.utils.Vector2
import com.ironfury.laststand.weapons.WeaponType
import kotlin.math.sin

class Player : Entity(
    position = Vector2(100f, Constants.GROUND_Y - Constants.PLAYER_HEIGHT),
    width = Constants.PLAYER_WIDTH,
    height = Constants.PLAYER_HEIGHT
) {
    var lives = Constants.PLAYER_MAX_LIVES
    var score = 0
    var currentLevelWidth = Constants.LEVEL_WIDTH

    var isOnGround = true
    var facingRight = true
    var aimDirection = Direction.RIGHT

    // Prone (lie down) stance — shrinks hitbox and forces horizontal fire
    var isProne = false
        private set

    // Weapon system
    var currentWeapon = WeaponType.MACHINE_GUN

    // Rocket ammo
    var rocketAmmo = MAX_ROCKETS
    private var rocketRefillTimer = 0f

    private var invincibilityTimer = 0f
    val isInvincible: Boolean get() = invincibilityTimer > 0f

    private var fireTimer = 0f
    var canFire: Boolean = false
        private set

    // Double jump
    private var jumpCount = 0
    private var wasJumping = false

    // Flip animation
    private var flipAngle = 0f
    val isFlipping: Boolean get() = !isOnGround && flipAngle != 0f

    // Checkpoint
    var checkpointX = 100f

    // Animation
    private var animTime = 0f
    private var runFrame = 0
    var muzzleFlash = false
        private set

    companion object {
        const val MAX_ROCKETS = 5
        const val ROCKET_REFILL_TIME = 8f // seconds to refill one rocket
    }

    // Input state
    var moveLeft = false
    var moveRight = false
    var aimUp = false
    var aimDown = false
    var jumping = false
    var firing = false

    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val outlinePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 2f
        color = Color.BLACK
        isAntiAlias = true
    }

    // Reused path for building oriented gun shapes
    private val gunPath = Path()

    // Character palette — driven by the selected CharacterSkin (see applySkin).
    // Defaults to the RECRUIT look so the player renders correctly even before a
    // skin is applied. Gun colours stay constant (they belong to the weapon).
    private var skinColor = CharacterSkin.RECRUIT.skin
    private var hairColor = CharacterSkin.RECRUIT.hair
    private var shirtColor = CharacterSkin.RECRUIT.shirt
    private var shirtHighlight = CharacterSkin.RECRUIT.shirtHi
    private var pantsColor = CharacterSkin.RECRUIT.pants
    private var pantsHighlight = CharacterSkin.RECRUIT.pantsHi
    private var bootsColor = CharacterSkin.RECRUIT.boots
    private val gunMetal = Color.rgb(60, 60, 70)
    private val gunHighlight = Color.rgb(100, 100, 110)
    private var bandanaColor = CharacterSkin.RECRUIT.bandana

    /** Swap the player's appearance to the given character skin. */
    fun applySkin(s: CharacterSkin) {
        skinColor = s.skin
        hairColor = s.hair
        shirtColor = s.shirt
        shirtHighlight = s.shirtHi
        pantsColor = s.pants
        pantsHighlight = s.pantsHi
        bootsColor = s.boots
        bandanaColor = s.bandana
    }

    override fun update(deltaTime: Float) {
        // Prone stance: hold down while standing still on the ground.
        // Moving or jumping cancels it (you stand up to move). While airborne,
        // "down" keeps its aim-down behavior instead of going prone.
        setProne(aimDown && isOnGround && !moveLeft && !moveRight && !jumping)

        // Horizontal movement
        velocity.x = when {
            moveLeft && !moveRight -> {
                facingRight = false
                -Constants.PLAYER_SPEED
            }
            moveRight && !moveLeft -> {
                facingRight = true
                Constants.PLAYER_SPEED
            }
            else -> 0f
        }

        // Determine aim direction (8-way)
        aimDirection = calculateAimDirection()

        // Double jump — trigger on press edge (not hold)
        val jumpPressed = jumping && !wasJumping
        wasJumping = jumping

        if (jumpPressed && jumpCount < 2) {
            velocity.y = Constants.PLAYER_JUMP_VELOCITY * (if (jumpCount == 1) 0.85f else 1f)
            isOnGround = false
            jumpCount++
            flipAngle = if (jumpCount == 2) 1f else 0f // Start flip on double jump
            SoundManager.play(SoundManager.Sound.JUMP)
        }

        // Flip animation during double jump
        if (flipAngle > 0f && flipAngle < 360f) {
            flipAngle += 720f * deltaTime // Full rotation in 0.5s
            if (flipAngle >= 360f) flipAngle = 360f
        }

        // Always apply gravity
        velocity.y += Constants.GRAVITY * deltaTime

        // Update position
        super.update(deltaTime)

        // Reset ground state - will be set true by collision checks
        isOnGround = false

        // Ground collision
        if (position.y + height >= Constants.GROUND_Y) {
            position.y = Constants.GROUND_Y - height
            velocity.y = 0f
            isOnGround = true
            jumpCount = 0
            flipAngle = 0f
        }

        // Rocket ammo refill
        if (rocketAmmo < MAX_ROCKETS) {
            rocketRefillTimer += deltaTime
            if (rocketRefillTimer >= ROCKET_REFILL_TIME) {
                rocketRefillTimer -= ROCKET_REFILL_TIME
                rocketAmmo++
            }
        } else {
            rocketRefillTimer = 0f
        }

        // Level boundaries
        if (position.x < 0) position.x = 0f
        if (position.x + width > currentLevelWidth) {
            position.x = currentLevelWidth - width
        }

        // Invincibility timer
        if (invincibilityTimer > 0) {
            invincibilityTimer -= deltaTime
        }

        // Fire rate (uses current weapon's fire rate)
        if (fireTimer > 0) {
            fireTimer -= deltaTime
        }
        canFire = firing && fireTimer <= 0f
        if (canFire) {
            fireTimer = currentWeapon.fireRate
            muzzleFlash = true
        } else {
            muzzleFlash = false
        }

        // Animation timer
        animTime += deltaTime
    }

    private fun setProne(prone: Boolean) {
        if (prone == isProne) return
        // Keep the feet anchored so changing the hitbox height doesn't sink the
        // player into the floor or pop them upward.
        val bottom = position.y + height
        isProne = prone
        height = if (prone) Constants.PLAYER_PRONE_HEIGHT else Constants.PLAYER_HEIGHT
        position.y = bottom - height
    }

    private fun calculateAimDirection(): Direction {
        // While prone the soldier can only fire straight ahead along the ground.
        if (isProne) return if (facingRight) Direction.RIGHT else Direction.LEFT

        val horizontal = when {
            moveRight && !moveLeft -> 1
            moveLeft && !moveRight -> -1
            else -> if (facingRight) 1 else -1
        }
        val vertical = when {
            aimUp && !aimDown -> -1
            aimDown && !aimUp && !isOnGround -> 1
            else -> 0
        }

        return when {
            horizontal > 0 && vertical < 0 -> Direction.UP_RIGHT
            horizontal > 0 && vertical > 0 -> Direction.DOWN_RIGHT
            horizontal > 0 -> Direction.RIGHT
            horizontal < 0 && vertical < 0 -> Direction.UP_LEFT
            horizontal < 0 && vertical > 0 -> Direction.DOWN_LEFT
            horizontal < 0 -> Direction.LEFT
            vertical < 0 -> Direction.UP
            vertical > 0 -> Direction.DOWN
            else -> if (facingRight) Direction.RIGHT else Direction.LEFT
        }
    }

    override fun render(canvas: Canvas, cameraX: Float, scale: Float) {
        if (!isActive) return

        // Flicker when invincible
        if (isInvincible && (System.currentTimeMillis() / 100) % 2 == 0L) {
            return
        }

        val screenX = (position.x - cameraX) * scale
        val screenY = position.y * scale
        val w = width * scale
        val h = height * scale

        // Lying-down pose has its own sprite
        if (isProne) {
            renderProne(canvas, screenX, screenY, w, h, scale)
            return
        }

        canvas.save()

        // Flip if facing left
        if (!facingRight) {
            canvas.scale(-1f, 1f, screenX + w / 2, screenY + h / 2)
        }

        // Somersault rotation during double jump
        if (flipAngle > 0f && flipAngle < 360f) {
            canvas.rotate(flipAngle, screenX + w / 2, screenY + h / 2)
        }

        // Animation offsets
        val isRunning = (moveLeft || moveRight) && isOnGround
        val legOffset = if (isRunning) sin(animTime * 12) * h * 0.08f else 0f
        val bodyBob = if (isRunning) sin(animTime * 24) * h * 0.02f else 0f
        val jumpSquash = if (!isOnGround) 0.9f else 1f

        // === LEGS ===
        // Back leg
        paint.color = pantsColor
        canvas.drawRoundRect(
            screenX + w * 0.35f,
            screenY + h * 0.58f - legOffset,
            screenX + w * 0.52f,
            screenY + h * 0.85f - legOffset,
            4f * scale, 4f * scale, paint
        )
        // Back boot
        paint.color = bootsColor
        canvas.drawRoundRect(
            screenX + w * 0.32f,
            screenY + h * 0.82f - legOffset,
            screenX + w * 0.54f,
            screenY + h * 0.95f - legOffset,
            3f * scale, 3f * scale, paint
        )

        // Front leg
        paint.color = pantsHighlight
        canvas.drawRoundRect(
            screenX + w * 0.48f,
            screenY + h * 0.58f + legOffset,
            screenX + w * 0.65f,
            screenY + h * 0.85f + legOffset,
            4f * scale, 4f * scale, paint
        )
        // Front boot
        paint.color = bootsColor
        canvas.drawRoundRect(
            screenX + w * 0.46f,
            screenY + h * 0.82f + legOffset,
            screenX + w * 0.70f,
            screenY + h * 0.98f + legOffset,
            3f * scale, 3f * scale, paint
        )

        // === TORSO ===
        paint.color = shirtColor
        canvas.drawRoundRect(
            screenX + w * 0.25f,
            screenY + h * 0.28f + bodyBob,
            screenX + w * 0.75f,
            screenY + h * 0.62f + bodyBob,
            6f * scale, 6f * scale, paint
        )
        // Shirt highlight
        paint.color = shirtHighlight
        canvas.drawRoundRect(
            screenX + w * 0.30f,
            screenY + h * 0.30f + bodyBob,
            screenX + w * 0.55f,
            screenY + h * 0.45f + bodyBob,
            4f * scale, 4f * scale, paint
        )

        // === ARM WITH GUN ===
        // The canvas is h-flipped when facing left, which already mirrors the gun.
        // So draw the arm/gun in facing-right space (un-flip the horizontal aim
        // component) and let the canvas flip point it left — otherwise the gun
        // flips twice and ends up pointing the wrong way (only the head turns).
        val aimVec = Vector2.direction(aimDirection)
        val armDir = if (facingRight) aimVec else Vector2(-aimVec.x, aimVec.y)
        val shoulderX = screenX + w * 0.55f
        val shoulderY = screenY + h * 0.38f + bodyBob

        // Arm
        paint.color = skinColor
        outlinePaint.strokeWidth = 8f * scale
        outlinePaint.color = skinColor
        val elbowX = shoulderX + armDir.x * w * 0.25f
        val elbowY = shoulderY + armDir.y * w * 0.25f
        canvas.drawLine(shoulderX, shoulderY, elbowX, elbowY, outlinePaint)

        // Gun-space basis: forward = armDir, perpendicular (perpX,perpY) where
        // negative perp = top of the weapon, positive = under-barrel.
        // Force perp toward the screen-bottom so weapon top/bottom features stay
        // consistent whether the player faces left or right. (Facing left h-flips
        // the canvas, which would otherwise render the gun vertically inverted.)
        var perpX = -armDir.y
        var perpY = armDir.x
        if (perpY < 0f) { perpX = -perpX; perpY = -perpY }
        val ox = elbowX
        val oy = elbowY

        // Gloved trigger hand at the grip
        gunDot(canvas, ox, oy, armDir.x, armDir.y, perpX, perpY, w * 0.02f, w * 0.04f, 4.5f * scale, Color.rgb(35, 35, 42))

        // Draw weapon based on current type
        val gunEndX: Float
        val gunEndY: Float

        when (currentWeapon) {
            WeaponType.MACHINE_GUN -> {
                // Belt-fed squad automatic weapon (see drawMachineGun)
                gunEndX = ox + armDir.x * w * 1.28f
                gunEndY = oy + armDir.y * w * 1.28f
                drawMachineGun(canvas, ox, oy, armDir.x, armDir.y, perpX, perpY, w, scale)
            }
            WeaponType.SPREAD_GUN -> {
                gunEndX = ox + armDir.x * w * 0.95f
                gunEndY = oy + armDir.y * w * 0.95f
                drawSpreadGun(canvas, ox, oy, armDir.x, armDir.y, perpX, perpY, w, scale)
            }
            WeaponType.LASER -> {
                gunEndX = ox + armDir.x * w * 0.98f
                gunEndY = oy + armDir.y * w * 0.98f
                drawLaser(canvas, ox, oy, armDir.x, armDir.y, perpX, perpY, w, scale)
            }
            WeaponType.ROCKET_LAUNCHER -> {
                gunEndX = ox + armDir.x * w * 0.88f
                gunEndY = oy + armDir.y * w * 0.88f
                drawRocketLauncher(canvas, ox, oy, armDir.x, armDir.y, perpX, perpY, w, scale)
            }
            WeaponType.FLAMETHROWER -> {
                gunEndX = ox + armDir.x * w * 0.80f
                gunEndY = oy + armDir.y * w * 0.80f
                drawFlamethrower(canvas, ox, oy, armDir.x, armDir.y, perpX, perpY, w, scale, shoulderX, shoulderY)
            }
        }

        // Muzzle flash (weapon-specific colors) — layered star-burst
        if (muzzleFlash) {
            val flashSize = w * 0.22f
            val flashColor = when (currentWeapon) {
                WeaponType.LASER -> Color.CYAN
                WeaponType.FLAMETHROWER -> Color.rgb(255, 100, 0)
                WeaponType.ROCKET_LAUNCHER -> Color.rgb(255, 200, 100)
                else -> Color.rgb(255, 200, 40)
            }
            val mx = gunEndX + armDir.x * flashSize * 0.6f
            val my = gunEndY + armDir.y * flashSize * 0.6f
            // Four-point spark: long lobes along the barrel, short across it
            val a = Color.alpha(flashColor)
            gunPath.reset()
            gunPath.moveTo(mx + armDir.x * flashSize * 1.7f, my + armDir.y * flashSize * 1.7f)
            gunPath.lineTo(mx + perpX * flashSize * 0.55f, my + perpY * flashSize * 0.55f)
            gunPath.lineTo(mx - armDir.x * flashSize * 0.7f, my - armDir.y * flashSize * 0.7f)
            gunPath.lineTo(mx - perpX * flashSize * 0.55f, my - perpY * flashSize * 0.55f)
            gunPath.close()
            paint.color = Color.argb(a, Color.red(flashColor), Color.green(flashColor), Color.blue(flashColor))
            canvas.drawPath(gunPath, paint)
            // Outer glow
            paint.color = Color.argb(90, Color.red(flashColor), Color.green(flashColor), Color.blue(flashColor))
            canvas.drawCircle(mx, my, flashSize * 1.1f, paint)
            // Hot white core
            paint.color = Color.WHITE
            canvas.drawCircle(mx, my, flashSize * 0.45f, paint)
        }

        // === HEAD ===
        // Neck
        paint.color = skinColor
        canvas.drawRect(
            screenX + w * 0.42f,
            screenY + h * 0.22f + bodyBob,
            screenX + w * 0.58f,
            screenY + h * 0.32f + bodyBob,
            paint
        )

        // Head shape
        paint.color = skinColor
        canvas.drawOval(
            screenX + w * 0.28f,
            screenY + h * 0.02f + bodyBob,
            screenX + w * 0.72f,
            screenY + h * 0.28f + bodyBob,
            paint
        )

        // Hair
        paint.color = hairColor
        val hairPath = Path().apply {
            moveTo(screenX + w * 0.30f, screenY + h * 0.15f + bodyBob)
            quadTo(screenX + w * 0.35f, screenY - h * 0.02f + bodyBob,
                   screenX + w * 0.50f, screenY + h * 0.01f + bodyBob)
            quadTo(screenX + w * 0.70f, screenY - h * 0.01f + bodyBob,
                   screenX + w * 0.72f, screenY + h * 0.12f + bodyBob)
            lineTo(screenX + w * 0.68f, screenY + h * 0.08f + bodyBob)
            quadTo(screenX + w * 0.50f, screenY + h * 0.05f + bodyBob,
                   screenX + w * 0.35f, screenY + h * 0.10f + bodyBob)
            close()
        }
        canvas.drawPath(hairPath, paint)

        // Bandana
        paint.color = bandanaColor
        canvas.drawRect(
            screenX + w * 0.28f,
            screenY + h * 0.10f + bodyBob,
            screenX + w * 0.72f,
            screenY + h * 0.16f + bodyBob,
            paint
        )
        // Bandana tail
        if (facingRight || !facingRight) {
            val tailPath = Path().apply {
                moveTo(screenX + w * 0.28f, screenY + h * 0.12f + bodyBob)
                lineTo(screenX + w * 0.10f, screenY + h * 0.18f + bodyBob)
                lineTo(screenX + w * 0.15f, screenY + h * 0.22f + bodyBob)
                lineTo(screenX + w * 0.28f, screenY + h * 0.16f + bodyBob)
                close()
            }
            canvas.drawPath(tailPath, paint)
        }

        // Eye
        paint.color = Color.WHITE
        canvas.drawOval(
            screenX + w * 0.52f,
            screenY + h * 0.12f + bodyBob,
            screenX + w * 0.64f,
            screenY + h * 0.20f + bodyBob,
            paint
        )
        paint.color = Color.BLACK
        canvas.drawCircle(
            screenX + w * 0.59f,
            screenY + h * 0.16f + bodyBob,
            2.5f * scale,
            paint
        )

        canvas.restore()
    }

    // Draws a filled, oriented rectangular "slab" along the gun's aim direction so
    // detailed weapons read correctly at any of the 8 aim angles.
    //   (ox,oy) grip origin · (dx,dy) unit aim · (px,py) unit perpendicular
    //   along0..along1 = extent down the barrel · perpOffset shifts it sideways
    //   (negative = "up"/top of gun, positive = "down"/under-barrel) · halfThick = half slab width
    private fun gunSlab(
        canvas: Canvas,
        ox: Float, oy: Float, dx: Float, dy: Float, px: Float, py: Float,
        along0: Float, along1: Float, perpOffset: Float, halfThick: Float, color: Int
    ) {
        val ax0 = ox + dx * along0; val ay0 = oy + dy * along0
        val ax1 = ox + dx * along1; val ay1 = oy + dy * along1
        val n0x = px * (perpOffset - halfThick); val n0y = py * (perpOffset - halfThick)
        val n1x = px * (perpOffset + halfThick); val n1y = py * (perpOffset + halfThick)
        gunPath.reset()
        gunPath.moveTo(ax0 + n0x, ay0 + n0y)
        gunPath.lineTo(ax1 + n0x, ay1 + n0y)
        gunPath.lineTo(ax1 + n1x, ay1 + n1y)
        gunPath.lineTo(ax0 + n1x, ay0 + n1y)
        gunPath.close()
        paint.color = color
        canvas.drawPath(gunPath, paint)
    }

    // A dot positioned in gun-space (along the barrel + perpendicular offset).
    private fun gunDot(
        canvas: Canvas,
        ox: Float, oy: Float, dx: Float, dy: Float, px: Float, py: Float,
        along: Float, perp: Float, radius: Float, color: Int
    ) {
        paint.color = color
        canvas.drawCircle(ox + dx * along + px * perp, oy + dy * along + py * perp, radius, paint)
    }

    // Belt-fed SAW drawn in gun-space from grip origin (ox,oy): forward (dx,dy),
    // perpendicular (px,py) pointing toward screen-bottom. Shared by standing & prone.
    private fun drawMachineGun(
        canvas: Canvas, ox: Float, oy: Float,
        dx: Float, dy: Float, px: Float, py: Float, w: Float, scale: Float
    ) {
        // Shoulder stock
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.30f * w, 0.0f * w, -0.03f * w, 0.10f * w, Color.rgb(40, 40, 48))
        // Hanging ammo box
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.18f * w, 0.50f * w, 0.20f * w, 0.13f * w, Color.rgb(55, 62, 48))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.20f * w, 0.32f * w, 0.15f * w, 0.05f * w, Color.rgb(82, 90, 70))
        // Large receiver
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.06f * w, 0.64f * w, 0.0f * w, 0.15f * w, gunMetal)
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.0f * w, 0.58f * w, -0.10f * w, 0.03f * w, gunHighlight)
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.06f * w, 0.42f * w, -0.13f * w, 0.04f * w, Color.rgb(70, 70, 82))
        // Optic on a riser
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.16f * w, 0.22f * w, -0.20f * w, 0.03f * w, gunMetal)
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.10f * w, 0.34f * w, -0.27f * w, 0.055f * w, Color.rgb(30, 30, 36))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.34f * w, -0.27f * w, 0.04f * w, Color.rgb(120, 200, 220))
        // Heavy barrel + handguard with vent holes
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.64f * w, 1.16f * w, -0.02f * w, 0.06f * w, Color.rgb(45, 45, 52))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.64f * w, 0.92f * w, -0.02f * w, 0.085f * w, Color.rgb(58, 58, 68))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.72f * w, -0.02f * w, 0.022f * w, Color.rgb(25, 25, 30))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.82f * w, -0.02f * w, 0.022f * w, Color.rgb(25, 25, 30))
        // Front sight post, big flash hider, folded bipod
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.98f * w, 1.02f * w, -0.14f * w, 0.022f * w, gunMetal)
        gunSlab(canvas, ox, oy, dx, dy, px, py, 1.14f * w, 1.30f * w, -0.02f * w, 0.10f * w, Color.rgb(28, 28, 34))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.90f * w, 1.10f * w, 0.10f * w, 0.018f * w, Color.rgb(40, 40, 46))
        // Ammo belt last, on top: brass rounds drooping from the box into the feed tray
        val brass = Color.rgb(216, 178, 80)
        val brassTip = Color.rgb(150, 118, 44)
        val n = 7
        for (i in 0..n) {
            val t = i / n.toFloat()
            val a = (0.12f + t * 0.34f) * w
            val sag = (0.10f + 0.20f * sin((t * Math.PI).toFloat())) * w
            gunDot(canvas, ox, oy, dx, dy, px, py, a, sag, 0.04f * w, brass)
            gunDot(canvas, ox, oy, dx, dy, px, py, a, sag - 0.03f * w, 0.022f * w, brassTip)
        }
    }

    // Double-barrel combat shotgun: walnut stock + steel over/under barrels.
    private fun drawSpreadGun(
        canvas: Canvas, ox: Float, oy: Float,
        dx: Float, dy: Float, px: Float, py: Float, w: Float, scale: Float
    ) {
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.24f * w, 0.04f * w, 0.02f * w, 0.10f * w, Color.rgb(120, 78, 42))
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.22f * w, -0.02f * w, -0.03f * w, 0.03f * w, Color.rgb(155, 105, 60))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.02f * w, 0.30f * w, 0f, 0.13f * w, Color.rgb(95, 95, 105))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.26f * w, 0.94f * w, -0.055f * w, 0.05f * w, Color.rgb(100, 100, 112))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.26f * w, 0.92f * w, 0.055f * w, 0.05f * w, Color.rgb(72, 72, 82))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.34f * w, 0.58f * w, 0.005f * w, 0.085f * w, Color.rgb(120, 78, 42))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.30f * w, 0.88f * w, -0.10f * w, 0.015f * w, Color.rgb(150, 150, 160))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.93f * w, -0.055f * w, 0.04f * w, Color.rgb(20, 20, 24))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.91f * w, 0.055f * w, 0.04f * w, Color.rgb(20, 20, 24))
    }

    // Energy rifle: white casing, glowing cyan core, twin emitter prongs.
    private fun drawLaser(
        canvas: Canvas, ox: Float, oy: Float,
        dx: Float, dy: Float, px: Float, py: Float, w: Float, scale: Float
    ) {
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.06f * w, 0.62f * w, 0f, 0.12f * w, Color.rgb(70, 92, 112))
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.02f * w, 0.52f * w, -0.05f * w, 0.05f * w, Color.rgb(205, 215, 225))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.0f * w, 0.12f * w, 0.12f * w, 0.06f * w, Color.rgb(55, 72, 90))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.24f * w, 0f, 0.13f * w, Color.argb(70, 0, 220, 255))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.24f * w, 0f, 0.08f * w, Color.argb(150, 80, 230, 255))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.24f * w, 0f, 0.04f * w, Color.WHITE)
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.62f * w, 0.86f * w, 0f, 0.035f * w, Color.rgb(0, 180, 220))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.84f * w, 1.0f * w, -0.06f * w, 0.02f * w, Color.CYAN)
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.84f * w, 1.0f * w, 0.06f * w, 0.02f * w, Color.CYAN)
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.99f * w, 0f, 0.05f * w, Color.argb(180, 180, 255, 255))
    }

    // Bulky shoulder-fired rocket launcher: tube, rear flare, top scope, warhead tip.
    private fun drawRocketLauncher(
        canvas: Canvas, ox: Float, oy: Float,
        dx: Float, dy: Float, px: Float, py: Float, w: Float, scale: Float
    ) {
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.30f * w, -0.10f * w, 0f, 0.16f * w, Color.rgb(52, 62, 50))
        gunDot(canvas, ox, oy, dx, dy, px, py, -0.28f * w, 0f, 0.10f * w, Color.rgb(25, 28, 24))
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.02f * w, 0.10f * w, 0.17f * w, 0.05f * w, Color.rgb(40, 46, 38))
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.10f * w, 0.84f * w, 0f, 0.15f * w, Color.rgb(72, 86, 66))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.0f * w, 0.72f * w, -0.08f * w, 0.04f * w, Color.rgb(98, 116, 90))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.80f * w, 0.90f * w, 0f, 0.18f * w, Color.rgb(56, 66, 52))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.86f * w, 0f, 0.11f * w, Color.rgb(22, 22, 22))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.84f * w, 0f, 0.055f * w, Color.rgb(200, 55, 45))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.22f * w, 0.46f * w, -0.20f * w, 0.04f * w, Color.rgb(40, 42, 46))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.46f * w, -0.20f * w, 0.035f * w, Color.rgb(120, 200, 220))
    }

    // Industrial flamethrower: back tank, hose, wide nozzle, flickering pilot flame.
    // (backX,backY) anchors the fuel tank on the operator's back.
    private fun drawFlamethrower(
        canvas: Canvas, ox: Float, oy: Float,
        dx: Float, dy: Float, px: Float, py: Float, w: Float, scale: Float,
        backX: Float, backY: Float
    ) {
        gunDot(canvas, backX, backY, dx, dy, px, py, -0.34f * w, 0f, 9f * scale, Color.rgb(185, 55, 50))
        gunDot(canvas, backX, backY, dx, dy, px, py, -0.34f * w, -0.04f * w, 4.5f * scale, Color.rgb(225, 90, 80))
        gunDot(canvas, backX, backY, dx, dy, px, py, -0.34f * w, 0.16f * w, 3f * scale, Color.rgb(120, 120, 130))
        outlinePaint.strokeWidth = 4f * scale
        outlinePaint.color = Color.rgb(45, 45, 50)
        canvas.drawLine(
            backX - dx * w * 0.22f, backY,
            ox + dx * w * 0.05f + px * w * 0.08f, oy + dy * w * 0.05f + py * w * 0.08f,
            outlinePaint
        )
        gunSlab(canvas, ox, oy, dx, dy, px, py, -0.02f * w, 0.56f * w, 0f, 0.09f * w, Color.rgb(78, 78, 86))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.04f * w, 0.48f * w, -0.045f * w, 0.025f * w, Color.rgb(120, 120, 128))
        gunSlab(canvas, ox, oy, dx, dy, px, py, 0.52f * w, 0.78f * w, 0f, 0.13f * w, Color.rgb(60, 60, 70))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.76f * w, 0f, 0.07f * w, Color.rgb(25, 25, 28))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.20f * w, -0.10f * w, 0.04f * w, Color.rgb(200, 200, 210))
        val pilot = 0.5f + 0.5f * sin(animTime * 30f)
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.82f * w, 0f, (0.05f + 0.02f * pilot) * w, Color.rgb(255, 150, 50))
        gunDot(canvas, ox, oy, dx, dy, px, py, 0.82f * w, 0f, 0.025f * w, Color.rgb(255, 230, 120))
    }

    // Soldier lying on their belly, gun pointed forward along the ground.
    // Drawn relative to the short prone hitbox; limbs/gun extend past it cosmetically.
    private fun renderProne(canvas: Canvas, screenX: Float, screenY: Float, w: Float, h: Float, scale: Float) {
        canvas.save()
        // Flip the whole pose to face left
        if (!facingRight) {
            canvas.scale(-1f, 1f, screenX + w / 2, screenY + h / 2)
        }

        // === LEGS (trailing behind, to the back/left) ===
        paint.color = pantsColor
        canvas.drawRoundRect(
            screenX - w * 0.25f, screenY + h * 0.45f,
            screenX + w * 0.45f, screenY + h * 0.82f,
            3f * scale, 3f * scale, paint
        )
        paint.color = bootsColor
        canvas.drawRoundRect(
            screenX - w * 0.40f, screenY + h * 0.48f,
            screenX - w * 0.22f, screenY + h * 0.80f,
            2f * scale, 2f * scale, paint
        )

        // === TORSO ===
        paint.color = shirtColor
        canvas.drawRoundRect(
            screenX + w * 0.18f, screenY + h * 0.32f,
            screenX + w * 0.82f, screenY + h * 0.86f,
            4f * scale, 4f * scale, paint
        )
        paint.color = shirtHighlight
        canvas.drawRoundRect(
            screenX + w * 0.24f, screenY + h * 0.36f,
            screenX + w * 0.55f, screenY + h * 0.55f,
            3f * scale, 3f * scale, paint
        )

        // === HEAD (at the front, leading the body) ===
        paint.color = skinColor
        canvas.drawOval(
            screenX + w * 0.70f, screenY + h * 0.18f,
            screenX + w * 1.06f, screenY + h * 0.74f,
            paint
        )
        // Bandana
        paint.color = bandanaColor
        canvas.drawRect(
            screenX + w * 0.74f, screenY + h * 0.18f,
            screenX + w * 1.02f, screenY + h * 0.36f,
            paint
        )
        // Eye
        paint.color = Color.WHITE
        canvas.drawOval(
            screenX + w * 0.88f, screenY + h * 0.40f,
            screenX + w * 0.98f, screenY + h * 0.52f,
            paint
        )
        paint.color = Color.BLACK
        canvas.drawCircle(screenX + w * 0.94f, screenY + h * 0.46f, 1.8f * scale, paint)

        // === ARM + GUN (pointing forward, level with the ground) ===
        // Local space is already h-flipped for facing: forward = +x, perp = +y (down).
        val gunY = screenY + h * 0.42f
        val handX = screenX + w * 0.52f
        // Arm
        outlinePaint.strokeWidth = 6f * scale
        outlinePaint.color = skinColor
        canvas.drawLine(screenX + w * 0.50f, screenY + h * 0.58f, handX, gunY, outlinePaint)

        // Each weapon uses the same silhouette as standing, drawn in prone gun-space.
        // Tank anchor for the flamethrower sits on the prone soldier's back.
        val backX = screenX + w * 0.28f
        val backY = screenY + h * 0.40f
        val gunEndX: Float = when (currentWeapon) {
            WeaponType.MACHINE_GUN -> {
                drawMachineGun(canvas, handX, gunY, 1f, 0f, 0f, 1f, w, scale)
                handX + w * 1.28f
            }
            WeaponType.SPREAD_GUN -> {
                drawSpreadGun(canvas, handX, gunY, 1f, 0f, 0f, 1f, w, scale)
                handX + w * 0.95f
            }
            WeaponType.LASER -> {
                drawLaser(canvas, handX, gunY, 1f, 0f, 0f, 1f, w, scale)
                handX + w * 0.98f
            }
            WeaponType.ROCKET_LAUNCHER -> {
                drawRocketLauncher(canvas, handX, gunY, 1f, 0f, 0f, 1f, w, scale)
                handX + w * 0.88f
            }
            WeaponType.FLAMETHROWER -> {
                drawFlamethrower(canvas, handX, gunY, 1f, 0f, 0f, 1f, w, scale, backX, backY)
                handX + w * 0.80f
            }
        }

        // Muzzle flash
        if (muzzleFlash) {
            val flashColor = when (currentWeapon) {
                WeaponType.LASER -> Color.CYAN
                WeaponType.FLAMETHROWER -> Color.rgb(255, 100, 0)
                WeaponType.ROCKET_LAUNCHER -> Color.rgb(255, 200, 100)
                else -> Color.YELLOW
            }
            paint.color = flashColor
            canvas.drawCircle(gunEndX + w * 0.12f, gunY, w * 0.18f, paint)
            paint.color = Color.WHITE
            canvas.drawCircle(gunEndX + w * 0.06f, gunY, w * 0.10f, paint)
        }

        canvas.restore()
    }

    fun takeDamage(): Boolean {
        if (isInvincible) return false

        lives--
        invincibilityTimer = Constants.PLAYER_INVINCIBILITY_TIME

        return lives <= 0
    }

    fun respawn() {
        isProne = false
        height = Constants.PLAYER_HEIGHT
        position.set(checkpointX, Constants.GROUND_Y - height)
        velocity.set(0f, 0f)
        isOnGround = true
        jumpCount = 0
        flipAngle = 0f
        invincibilityTimer = Constants.PLAYER_INVINCIBILITY_TIME
    }

    fun resetForNewLevel() {
        checkpointX = 100f
        rocketAmmo = MAX_ROCKETS
        rocketRefillTimer = 0f
        jumpCount = 0
        flipAngle = 0f
        isProne = false
        height = Constants.PLAYER_HEIGHT
        // Reward clearing a level by restoring lives — symmetric with the
        // checkpoint-resume on game over, and makes the boss kill feel earned.
        lives = Constants.PLAYER_MAX_LIVES
    }

    fun getBulletSpawnPosition(): Vector2 {
        val dir = Vector2.direction(aimDirection)
        // Originate from the gun's shoulder pivot so shots leave the barrel, not the
        // body center. The arm/gun are drawn from (0.55*width, 0.38*height) and extend
        // in width units; prone holds the gun lower at 0.50*height.
        val pivotX = position.x + width * 0.55f
        val pivotY = position.y + height * (if (isProne) 0.50f else 0.38f)
        val reach = width * 0.55f
        return Vector2(pivotX + dir.x * reach, pivotY + dir.y * reach)
    }
}
