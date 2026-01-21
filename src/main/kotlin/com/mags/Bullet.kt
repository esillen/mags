package com.mags

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
    val color: Color
) {
    var isAlive = true
        private set
    
    private val maxDistance = 800f
    private var traveledDistance = 0f
    
    fun update(delta: Float) {
        val moveDistance = speed * delta
        x += cos(angle) * moveDistance
        y += sin(angle) * moveDistance
        traveledDistance += moveDistance
        
        if (traveledDistance > maxDistance) {
            isAlive = false
        }
    }
    
    fun onHit() {
        isAlive = false
    }
    
    fun draw(renderer: ShapeRenderer) {
        renderer.color = color
        renderer.circle(x, y, radius)
    }
}
