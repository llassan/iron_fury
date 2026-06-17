package com.ironfury.laststand.entities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import com.ironfury.laststand.entities.bosses.CrystalGuardian
import com.ironfury.laststand.entities.bosses.CyberOverlord
import com.ironfury.laststand.entities.bosses.IceTitan
import com.ironfury.laststand.entities.bosses.Kraken
import com.ironfury.laststand.entities.bosses.MagmaLord
import com.ironfury.laststand.entities.bosses.SandScorpion
import com.ironfury.laststand.entities.bosses.StormEmperor
import com.ironfury.laststand.entities.bosses.SupremeCommander
import com.ironfury.laststand.entities.bosses.SwampHorror
import com.ironfury.laststand.entities.bosses.WarMachine
import com.ironfury.laststand.utils.Constants
import com.ironfury.laststand.utils.Vector2
import kotlin.math.abs
import kotlin.math.sin

/**
 * Abstract base for the boss roster (10 unique fights, reused across the 20
 * levels with scaled health). Owns the shared update/render scaffolding:
 * movement, charging, four-phase health gating, damage flashing, the health bar
 * and the thruster effect. Each level's boss is a subclass that overrides
 * [executePhaseAttack] for its unique attack table and [renderBody] for its
 * unique visual.
 *
 * To add a new boss: create a new file in [com.ironfury.laststand.entities.bosses],
 * extend [Boss], implement the two abstract methods, and register it in
 * [Boss.create].
 */
abstract class Boss(
    startX: Float,
    val bossName: String,
    healthMultiplier: Float = 1f,
    protected val levelWidth: Float = Constants.LEVEL_WIDTH,
    /** Level number 1..20. Used to scale move/charge speed; subclasses normally
     *  shouldn't rely on it for behaviour — override hooks instead. */
    protected val levelNumber: Int = 1
) : Entity(
    position = Vector2(startX, Constants.GROUND_Y - BOSS_HEIGHT),
    width = BOSS_WIDTH,
    height = BOSS_HEIGHT
) {
    companion object {
        const val BOSS_WIDTH = 120f
        const val BOSS_HEIGHT = 100f
        const val BASE_HEALTH = 50
        const val COIN_DROP = 200

        /**
         * Factory: returns the correct concrete boss for the given level (1..20).
         * Falls back to [WarMachine] for out-of-range values so the game never crashes
         * on a misconfigured level table.
         */
        fun create(
            level: Int,
            startX: Float,
            bossName: String,
            healthMultiplier: Float,
            levelWidth: Float
        ): Boss = when (level) {
            1 -> WarMachine(startX, bossName, healthMultiplier, levelWidth)
            2 -> SandScorpion(startX, bossName, healthMultiplier, levelWidth)
            3 -> IceTitan(startX, bossName, healthMultiplier, levelWidth)
            4 -> MagmaLord(startX, bossName, healthMultiplier, levelWidth)
            5 -> CyberOverlord(startX, bossName, healthMultiplier, levelWidth)
            6 -> Kraken(startX, bossName, healthMultiplier, levelWidth)
            7 -> StormEmperor(startX, bossName, healthMultiplier, levelWidth)
            8 -> SwampHorror(startX, bossName, healthMultiplier, levelWidth)
            9 -> CrystalGuardian(startX, bossName, healthMultiplier, levelWidth)
            10 -> SupremeCommander(startX, bossName, healthMultiplier, levelWidth)
            // Levels 11-20 reuse the existing boss roster (cycled) but with the
            // higher health multipliers carried in their LevelTheme, so each
            // encounter is meaningfully tougher than its level 1-10 counterpart.
            11 -> SwampHorror(startX, bossName, healthMultiplier, levelWidth)
            12 -> Kraken(startX, bossName, healthMultiplier, levelWidth)
            13 -> MagmaLord(startX, bossName, healthMultiplier, levelWidth)
            14 -> IceTitan(startX, bossName, healthMultiplier, levelWidth)
            15 -> CyberOverlord(startX, bossName, healthMultiplier, levelWidth)
            16 -> SwampHorror(startX, bossName, healthMultiplier, levelWidth)
            17 -> SandScorpion(startX, bossName, healthMultiplier, levelWidth)
            18 -> CrystalGuardian(startX, bossName, healthMultiplier, levelWidth)
            19 -> StormEmperor(startX, bossName, healthMultiplier, levelWidth)
            20 -> SupremeCommander(startX, bossName, healthMultiplier, levelWidth)
            else -> WarMachine(startX, bossName, healthMultiplier, levelWidth)
        }
    }

    val maxHealth: Int = (BASE_HEALTH * healthMultiplier).toInt()
    var health: Int = maxHealth
        private set

    // Subclasses use these to draw and emit bullets.
    protected val paint: Paint = Paint().apply { isAntiAlias = true }
    protected val textPaint: Paint = Paint().apply {
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
        isFakeBoldText = true
    }

    private var moveDirection = -1f
    private val moveSpeed = 35f + levelNumber * 5f
    private var hoverOffset = 0f

    protected enum class AttackPhase { PHASE1, PHASE2, PHASE3, PHASE4 }

    protected var currentPhase: AttackPhase = AttackPhase.PHASE1
        private set
    private var phaseTimer = 0f
    protected var attackCooldown: Float = 0f
    protected var animTime: Float = 0f
        private set

    // Shared attack-pattern state. Several subclasses reuse the sweeping-laser
    // and charge mechanics, so we keep them on the base.
    protected var laserAngle: Float = 0f
    protected var laserSweepDir: Float = 1f
    protected var isCharging: Boolean = false
        private set
    private var chargeTargetX = 0f
    private var damageFlashTime = 0f

    /** Free-form timer subclasses use for their own animations. */
    protected var specialTimer: Float = 0f
        private set

    val pendingBullets: MutableList<BulletData> = mutableListOf()

    data class BulletData(
        val position: Vector2,
        val direction: Vector2,
        val speed: Float = Constants.ENEMY_BULLET_SPEED,
        val size: Float = 1f
    )

    fun update(deltaTime: Float, playerX: Float, playerY: Float) {
        if (!isActive) return

        animTime += deltaTime
        specialTimer += deltaTime
        hoverOffset = sin(animTime * 2f) * 5f

        if (damageFlashTime > 0) damageFlashTime -= deltaTime

        currentPhase = when {
            health > maxHealth * 0.75f -> AttackPhase.PHASE1
            health > maxHealth * 0.5f -> AttackPhase.PHASE2
            health > maxHealth * 0.25f -> AttackPhase.PHASE3
            else -> AttackPhase.PHASE4
        }

        if (!isCharging) {
            position.x += moveDirection * moveSpeed * deltaTime
            val minX = levelWidth - 400f
            val maxX = levelWidth - 150f
            if (position.x <= minX) { position.x = minX; moveDirection = 1f }
            else if (position.x + width >= maxX) { position.x = maxX - width; moveDirection = -1f }
        }

        attackCooldown -= deltaTime
        phaseTimer += deltaTime

        if (attackCooldown <= 0) {
            val cx = position.x + width / 2
            val cy = position.y + height / 2
            executePhaseAttack(currentPhase, cx, cy, playerX, playerY)
        }

        if (isCharging) {
            val chargeSpeed = 250f + levelNumber * 15f
            val dx = chargeTargetX - position.x
            if (abs(dx) > 10f) {
                position.x += (if (dx > 0) 1f else -1f) * chargeSpeed * deltaTime
            } else {
                isCharging = false
                attackCooldown = 1.5f
            }
        }
    }

    /**
     * Subclass hook: queue this boss's attack for the given phase by appending
     * to [pendingBullets] and setting [attackCooldown]. Call [beginCharge] to
     * start a horizontal charge toward the player.
     */
    protected abstract fun executePhaseAttack(
        phase: AttackPhase,
        cx: Float,
        cy: Float,
        playerX: Float,
        playerY: Float
    )

    /** Subclasses call this to start a charge attack toward [targetX]. */
    protected fun beginCharge(targetX: Float) {
        isCharging = true
        chargeTargetX = targetX - width / 2
    }

    fun takeDamage(amount: Int = 1): Boolean {
        health -= amount
        damageFlashTime = 0.1f
        if (health <= 0) { isActive = false; return true }
        return false
    }

    final override fun render(canvas: Canvas, cameraX: Float, scale: Float) {
        if (!isActive) return

        val screenX = (position.x - cameraX) * scale
        val screenY = (position.y + hoverOffset) * scale
        val w = width * scale
        val h = height * scale

        if (screenX + w < 0 || screenX > canvas.width) return

        val flashing = damageFlashTime > 0
        renderBody(canvas, screenX, screenY, w, h, scale, flashing)
        renderHealthBar(canvas, screenX, screenY, w, h, scale)
    }

    /**
     * Subclass hook: draw the boss body at the given screen coordinates. The
     * health bar and thruster effect are provided separately ([renderThrusters]
     * can be called from inside renderBody when appropriate).
     */
    protected abstract fun renderBody(
        canvas: Canvas,
        x: Float,
        y: Float,
        w: Float,
        h: Float,
        scale: Float,
        flashing: Boolean
    )

    /** Shared thruster effect used by mech-style bosses. */
    protected fun renderThrusters(c: Canvas, x: Float, y: Float, w: Float, h: Float, s: Float, f: Boolean) {
        paint.color = if (f) Color.WHITE else Color.rgb(50, 50, 60)
        c.drawRect(x + w * 0.2f, y + h * 0.8f, x + w * 0.35f, y + h, paint)
        c.drawRect(x + w * 0.65f, y + h * 0.8f, x + w * 0.8f, y + h, paint)

        val ff = (sin(animTime * 20) * 0.3f + 0.7f)
        paint.color = Color.rgb(255, (150 * ff).toInt(), 0)
        c.drawOval(x + w * 0.22f, y + h * 0.95f, x + w * 0.33f, y + h * 1.1f, paint)
        c.drawOval(x + w * 0.67f, y + h * 0.95f, x + w * 0.78f, y + h * 1.1f, paint)
        paint.color = Color.rgb(255, (220 * ff).toInt(), 100)
        c.drawOval(x + w * 0.25f, y + h * 0.95f, x + w * 0.30f, y + h * 1.05f, paint)
        c.drawOval(x + w * 0.70f, y + h * 0.95f, x + w * 0.75f, y + h * 1.05f, paint)
    }

    private fun renderHealthBar(c: Canvas, screenX: Float, screenY: Float, w: Float, h: Float, s: Float) {
        val barWidth = w * 0.8f
        val barHeight = 12f * s
        val barX = screenX + w * 0.1f
        val barY = screenY - 25f * s

        paint.color = Color.rgb(40, 40, 40)
        c.drawRoundRect(barX - 2f, barY - 2f, barX + barWidth + 2f, barY + barHeight + 2f, 4f, 4f, paint)

        paint.color = Color.rgb(80, 20, 20)
        c.drawRoundRect(barX, barY, barX + barWidth, barY + barHeight, 3f, 3f, paint)

        val healthPercent = health.toFloat() / maxHealth
        paint.color = when {
            healthPercent > 0.5f -> Color.rgb(50, 200, 50)
            healthPercent > 0.25f -> Color.rgb(255, 200, 0)
            else -> Color.rgb(255, 50, 50)
        }
        c.drawRoundRect(barX, barY, barX + barWidth * healthPercent, barY + barHeight, 3f, 3f, paint)

        textPaint.textSize = 14f * s
        textPaint.color = Color.WHITE
        textPaint.setShadowLayer(2f, 1f, 1f, Color.BLACK)
        c.drawText(bossName, screenX + w / 2, barY - 5f * s, textPaint)
        textPaint.setShadowLayer(0f, 0f, 0f, Color.TRANSPARENT)

        val phaseName = when (currentPhase) {
            AttackPhase.PHASE1 -> "PHASE 1"
            AttackPhase.PHASE2 -> "PHASE 2"
            AttackPhase.PHASE3 -> "PHASE 3"
            AttackPhase.PHASE4 -> "RAGE!"
        }
        textPaint.textSize = 10f * s
        textPaint.color = if (currentPhase == AttackPhase.PHASE4) Color.RED else Color.YELLOW
        c.drawText(phaseName, screenX + w / 2, barY + barHeight + 12f * s, textPaint)
    }
}
