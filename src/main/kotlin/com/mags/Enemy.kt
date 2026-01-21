package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.sqrt

class Enemy(var x: Float, var y: Float) {
    val radius = 25f
    private val speed = 80f
    private val maxHealth = 100f
    private var health = maxHealth
    
    val isDead: Boolean
        get() = health <= 0
    
    fun update(delta: Float, playerX: Float, playerY: Float) {
        val dx = playerX - x
        val dy = playerY - y
        val len = sqrt(dx * dx + dy * dy)
        if (len > radius) {
            x += (dx / len) * speed * delta
            y += (dy / len) * speed * delta
        }
    }
    
    fun collidesWith(px: Float, py: Float, pr: Float): Boolean {
        val dx = px - x
        val dy = py - y
        val dist = sqrt(dx * dx + dy * dy)
        return dist < radius + pr
    }
    
    fun takeDamage(amount: Float) {
        health -= amount
    }
    
    fun draw(renderer: ShapeRenderer) {
        renderer.color = Color(0.9f, 0.3f, 0.3f, 1f)
        renderer.rect(x - radius, y - radius, radius * 2, radius * 2)
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
    }
}
