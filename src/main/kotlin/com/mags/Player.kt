package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

class Player(var x: Float, var y: Float) {
    val radius = 20f
    private val speed = 250f
    var aimAngle = 0f
        private set
    
    fun move(dx: Float, dy: Float, delta: Float) {
        val len = kotlin.math.sqrt(dx * dx + dy * dy)
        if (len > 0) {
            x += (dx / len) * speed * delta
            y += (dy / len) * speed * delta
        }
    }
    
    fun aimAt(targetX: Float, targetY: Float) {
        aimAngle = atan2(targetY - y, targetX - x)
    }
    
    fun update(@Suppress("UNUSED_PARAMETER") delta: Float) {
    }
    
    fun draw(renderer: ShapeRenderer) {
        renderer.color = Color(0.3f, 0.7f, 0.9f, 1f)
        renderer.circle(x, y, radius)
        
        renderer.color = Color(0.9f, 0.9f, 0.9f, 1f)
        val gunLength = 30f
        val gunWidth = 6f
        val gunX = x + cos(aimAngle) * radius
        val gunY = y + sin(aimAngle) * radius
        val endX = gunX + cos(aimAngle) * gunLength
        val endY = gunY + sin(aimAngle) * gunLength
        renderer.rectLine(gunX, gunY, endX, endY, gunWidth)
    }
}
