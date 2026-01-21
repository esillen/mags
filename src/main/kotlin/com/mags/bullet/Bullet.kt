package com.mags.bullet

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.cos
import kotlin.math.sin

class Bullet(
    var x: Float,
    var y: Float,
    private val angle: Float,
    val speed: Float,
    val damage: Float,
    val radius: Float,
    val color: Color,
    val behavior: BulletBehavior = BulletBehavior.NORMAL,
    val splashDamage: Float = 0f,
    val splashRadius: Float = 0f,
    val statusEffect: StatusEffectType? = null,
    val element: Element? = null
) {
    var isAlive = true
        private set
    
    private val maxDistance = 800f
    private var traveledDistance = 0f
    
    private val grenadeLifetime = 2f
    private var lifetime = 0f
    
    var hasExploded = false
        private set
    
    fun update(delta: Float) {
        if (behavior == BulletBehavior.BOMB) {
            isAlive = false
            return
        }
        
        val moveDistance = speed * delta
        x += cos(angle) * moveDistance
        y += sin(angle) * moveDistance
        traveledDistance += moveDistance
        lifetime += delta
        
        if (traveledDistance > maxDistance) {
            if (behavior == BulletBehavior.GRENADE && !hasExploded) {
                explode()
            } else {
                isAlive = false
            }
        }
        
        if (behavior == BulletBehavior.GRENADE && lifetime >= grenadeLifetime && !hasExploded) {
            explode()
        }
    }
    
    fun onHit() {
        if (behavior == BulletBehavior.GRENADE) {
            explode()
        } else {
            isAlive = false
        }
    }
    
    private fun explode() {
        hasExploded = true
        isAlive = false
    }
    
    fun draw(renderer: ShapeRenderer) {
        renderer.color = color
        
        when (behavior) {
            BulletBehavior.GRENADE -> {
                renderer.circle(x, y, radius)
                renderer.color = Color(0.2f, 0.5f, 0.2f, 1f)
                renderer.circle(x, y, radius * 0.5f)
            }
            else -> {
                renderer.circle(x, y, radius)
                if (element != null) {
                    renderer.color = Color(1f, 1f, 1f, 0.5f)
                    renderer.circle(x, y, radius * 0.4f)
                }
            }
        }
    }
}
