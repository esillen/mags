package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mags.bullet.BulletType
import com.mags.bullet.BulletTypes
import com.mags.bullet.Element
import com.mags.bullet.StatusEffectType
import com.mags.effect.BurningEffect
import com.mags.effect.FrozenEffect
import com.mags.effect.StatusEffectManager
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class Shield(val element: Element) {
    var isActive = true
    val damageReduction = 0.5f
    
    fun tryBreak(bulletElement: Element?): Boolean {
        if (bulletElement == element) {
            isActive = false
            return true
        }
        return false
    }
}

class Enemy(var x: Float, var y: Float, hasShield: Boolean = false) {
    val radius = 25f
    private val baseSpeed = 80f
    private val maxHealth = 100f
    private var health = maxHealth
    
    private val statusEffects = StatusEffectManager()
    
    var shield: Shield? = if (hasShield) Shield(Element.entries.random()) else null
        private set
    
    val isDead: Boolean get() = health <= 0
    val hasActiveShield: Boolean get() = shield?.isActive == true
    
    private var droppedBullet: BulletType? = null
    val bulletDrop: BulletType? get() = droppedBullet
    
    private var shieldPulse = 0f
    
    fun update(delta: Float, playerX: Float, playerY: Float, obstacles: List<Obstacle> = emptyList()) {
        val effectDamage = statusEffects.update(delta)
        if (effectDamage > 0) {
            health -= effectDamage
        }
        
        val speedMultiplier = statusEffects.getEffect(FrozenEffect::class.java)?.getSpeedMultiplier() ?: 1f
        val speed = baseSpeed * speedMultiplier
        
        var dx = playerX - x
        var dy = playerY - y
        val len = sqrt(dx * dx + dy * dy)
        
        if (len > radius) {
            // Normalize direction
            dx /= len
            dy /= len
            
            // Simple obstacle avoidance - check nearby obstacles and steer away
            var avoidX = 0f
            var avoidY = 0f
            val avoidRadius = radius + 50f
            
            obstacles.forEach { obstacle ->
                val obstDx = x - obstacle.x
                val obstDy = y - obstacle.y
                val obstDist = sqrt(obstDx * obstDx + obstDy * obstDy)
                
                if (obstDist < avoidRadius + obstacle.width / 2) {
                    val pushStrength = 1f - (obstDist / (avoidRadius + obstacle.width / 2))
                    if (obstDist > 0.1f) {
                        avoidX += (obstDx / obstDist) * pushStrength * 2f
                        avoidY += (obstDy / obstDist) * pushStrength * 2f
                    }
                }
            }
            
            // Combine direction towards player with avoidance
            dx += avoidX
            dy += avoidY
            
            val finalLen = sqrt(dx * dx + dy * dy)
            if (finalLen > 0.1f) {
                dx /= finalLen
                dy /= finalLen
            }
            
            // Move
            val newX = x + dx * speed * delta
            val newY = y + dy * speed * delta
            
            // Check collision and push out
            var finalX = newX
            var finalY = newY
            
            obstacles.forEach { obstacle ->
                obstacle.pushOutCircle(finalX, finalY, radius)?.let { (pushX, pushY) ->
                    finalX = pushX
                    finalY = pushY
                }
            }
            
            x = finalX
            y = finalY
        }
        
        shieldPulse += delta * 3f
    }
    
    fun collidesWith(px: Float, py: Float, pr: Float): Boolean {
        val dx = px - x
        val dy = py - y
        val dist = sqrt(dx * dx + dy * dy)
        return dist < radius + pr
    }
    
    fun takeDamage(amount: Float, statusEffect: StatusEffectType? = null, bulletElement: Element? = null) {
        var finalDamage = amount
        
        shield?.let { s ->
            if (s.isActive) {
                if (s.tryBreak(bulletElement)) {
                    // Shield broken by matching element - full damage
                } else {
                    finalDamage *= s.damageReduction
                }
            }
        }
        
        health -= finalDamage
        
        statusEffect?.let { applyStatusEffect(it) }
        
        if (isDead && droppedBullet == null) {
            droppedBullet = BulletTypes.getRandomDrop()
        }
    }
    
    private fun applyStatusEffect(type: StatusEffectType) {
        when (type) {
            StatusEffectType.BURNING -> statusEffects.addEffect(BurningEffect())
            StatusEffectType.FROZEN -> statusEffects.addEffect(FrozenEffect())
        }
    }
    
    fun draw(renderer: ShapeRenderer) {
        if (hasActiveShield) {
            drawShield(renderer)
        }
        
        val baseColor = when {
            statusEffects.hasEffect(FrozenEffect::class.java) -> Color(0.5f, 0.7f, 0.9f, 1f)
            statusEffects.hasEffect(BurningEffect::class.java) -> Color(1f, 0.5f, 0.3f, 1f)
            else -> Color(0.9f, 0.3f, 0.3f, 1f)
        }
        renderer.color = baseColor
        renderer.rect(x - radius, y - radius, radius * 2, radius * 2)
    }
    
    private fun drawShield(renderer: ShapeRenderer) {
        val s = shield ?: return
        val shieldRadius = radius + 8f + sin(shieldPulse) * 2f
        
        renderer.color = Color(s.element.color.r, s.element.color.g, s.element.color.b, 0.3f)
        renderer.circle(x, y, shieldRadius)
        
        renderer.color = Color(s.element.color.r, s.element.color.g, s.element.color.b, 0.7f)
        val segments = 8
        for (i in 0 until segments) {
            val angle = (i.toFloat() / segments) * Math.PI.toFloat() * 2 + shieldPulse * 0.5f
            val px = x + cos(angle) * shieldRadius
            val py = y + sin(angle) * shieldRadius
            renderer.circle(px, py, 3f)
        }
    }
    
    fun drawHealthBar(renderer: ShapeRenderer) {
        val barWidth = 50f
        val barHeight = 6f
        val barY = y + radius + 10f
        
        renderer.color = Color.DARK_GRAY
        renderer.rect(x - barWidth / 2, barY, barWidth, barHeight)
    }
    
    fun drawHealthBarFill(renderer: ShapeRenderer) {
        val barWidth = 50f
        val barHeight = 6f
        val barY = y + radius + 10f
        val healthPercent = health / maxHealth
        
        renderer.color = Color(0.2f, 0.2f, 0.2f, 1f)
        renderer.rect(x - barWidth / 2, barY, barWidth, barHeight)
        
        renderer.color = when {
            healthPercent > 0.6f -> Color(0.3f, 0.8f, 0.3f, 1f)
            healthPercent > 0.3f -> Color(0.8f, 0.8f, 0.3f, 1f)
            else -> Color(0.8f, 0.3f, 0.3f, 1f)
        }
        renderer.rect(x - barWidth / 2, barY, barWidth * healthPercent, barHeight)
        
        if (hasActiveShield) {
            val shieldY = barY + barHeight + 2f
            renderer.color = shield!!.element.color
            renderer.rect(x - barWidth / 2, shieldY, barWidth, 3f)
        }
        
        statusEffects.drawIndicators(renderer, x, y, barY + barHeight + (if (hasActiveShield) 7f else 3f))
    }
}
