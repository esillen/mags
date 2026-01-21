package com.mags.effect

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

sealed class StatusEffectTarget {
    object Enemy : StatusEffectTarget()
    object Player : StatusEffectTarget()
}

abstract class StatusEffect(
    val name: String,
    val duration: Float,
    val target: StatusEffectTarget
) {
    var timeRemaining = duration
    val isExpired: Boolean get() = timeRemaining <= 0
    
    open fun update(delta: Float) {
        timeRemaining -= delta
    }
    
    open fun onApply() {}
    open fun onExpire() {}
    open fun onTick(delta: Float): Float = 0f // returns damage dealt this tick
    
    abstract fun getIndicatorColor(): Color
}

class BurningEffect(
    duration: Float = 4f,
    private val damagePerSecond: Float = 8f
) : StatusEffect("Burning", duration, StatusEffectTarget.Enemy) {
    
    override fun onTick(delta: Float): Float {
        return damagePerSecond * delta
    }
    
    override fun getIndicatorColor(): Color = Color(1f, 0.4f, 0.1f, 1f)
}

class FrozenEffect(
    duration: Float = 3f,
    private val slowMultiplier: Float = 0.4f,
    private val shatterDamage: Float = 20f
) : StatusEffect("Frozen", duration, StatusEffectTarget.Enemy) {
    
    fun getSpeedMultiplier(): Float = slowMultiplier
    
    override fun onExpire() {}
    
    fun getShatterDamage(): Float = shatterDamage
    
    override fun getIndicatorColor(): Color = Color(0.5f, 0.8f, 1f, 1f)
}

class StatusEffectManager {
    private val effects = mutableListOf<StatusEffect>()
    
    val activeEffects: List<StatusEffect> get() = effects.toList()
    
    fun addEffect(effect: StatusEffect) {
        val existing = effects.find { it::class == effect::class }
        if (existing != null) {
            existing.timeRemaining = maxOf(existing.timeRemaining, effect.duration)
        } else {
            effect.onApply()
            effects.add(effect)
        }
    }
    
    fun update(delta: Float): Float {
        var totalDamage = 0f
        val expiredEffects = mutableListOf<StatusEffect>()
        
        effects.forEach { effect ->
            totalDamage += effect.onTick(delta)
            effect.update(delta)
            if (effect.isExpired) {
                expiredEffects.add(effect)
            }
        }
        
        expiredEffects.forEach { effect ->
            effect.onExpire()
            if (effect is FrozenEffect) {
                totalDamage += effect.getShatterDamage()
            }
        }
        effects.removeAll(expiredEffects)
        
        return totalDamage
    }
    
    fun hasEffect(effectClass: Class<out StatusEffect>): Boolean {
        return effects.any { effectClass.isInstance(it) }
    }
    
    fun <T : StatusEffect> getEffect(effectClass: Class<T>): T? {
        @Suppress("UNCHECKED_CAST")
        return effects.find { effectClass.isInstance(it) } as T?
    }
    
    fun clear() {
        effects.clear()
    }
    
    fun drawIndicators(renderer: ShapeRenderer, x: Float, @Suppress("UNUSED_PARAMETER") y: Float, baseY: Float) {
        var offsetX = 0f
        effects.forEach { effect ->
            renderer.color = effect.getIndicatorColor()
            renderer.rect(x - 12 + offsetX, baseY, 8f, 8f)
            offsetX += 10f
        }
    }
}
