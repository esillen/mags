package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.cos
import kotlin.math.sin

class ImpactEffect(
    val x: Float,
    val y: Float,
    private val color: Color,
    private val duration: Float = 0.2f
) {
    private var timer = 0f
    val isFinished: Boolean get() = timer >= duration
    
    private val particles = mutableListOf<ImpactParticle>()
    
    init {
        val particleCount = 6 + (Math.random() * 4).toInt()
        for (i in 0 until particleCount) {
            val angle = (Math.random() * Math.PI * 2).toFloat()
            val speed = 80f + (Math.random() * 120f).toFloat()
            val size = 2f + (Math.random() * 4f).toFloat()
            particles.add(ImpactParticle(x, y, angle, speed, size))
        }
    }
    
    fun update(delta: Float) {
        timer += delta
        particles.forEach { it.update(delta) }
    }
    
    fun draw(renderer: ShapeRenderer) {
        val progress = timer / duration
        val alpha = 1f - progress
        
        // Draw expanding ring
        val ringRadius = 5f + progress * 25f
        renderer.color = Color(color.r, color.g, color.b, alpha * 0.5f)
        renderer.circle(x, y, ringRadius)
        
        // Draw center flash
        val flashSize = (1f - progress) * 8f
        renderer.color = Color(1f, 1f, 0.8f, alpha)
        renderer.circle(x, y, flashSize)
        
        // Draw particles
        particles.forEach { particle ->
            val particleAlpha = alpha * 0.8f
            renderer.color = Color(color.r * 1.2f, color.g * 1.1f, color.b * 0.8f, particleAlpha)
            renderer.circle(particle.x, particle.y, particle.size * (1f - progress * 0.5f))
        }
    }
}

private class ImpactParticle(
    var x: Float,
    var y: Float,
    private val angle: Float,
    private val speed: Float,
    val size: Float
) {
    private val friction = 0.92f
    private var currentSpeed = speed
    
    fun update(delta: Float) {
        x += cos(angle) * currentSpeed * delta
        y += sin(angle) * currentSpeed * delta
        currentSpeed *= friction
    }
}
