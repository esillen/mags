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
    
    private val gunLength = 30f
    
    val gunTipX: Float get() = x + cos(aimAngle) * (radius + gunLength)
    val gunTipY: Float get() = y + sin(aimAngle) * (radius + gunLength)
    
    private var muzzleFlashTimer = 0f
    private val muzzleFlashDuration = 0.08f
    
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
    
    fun triggerMuzzleFlash() {
        muzzleFlashTimer = muzzleFlashDuration
    }
    
    fun update(delta: Float) {
        if (muzzleFlashTimer > 0) {
            muzzleFlashTimer -= delta
        }
    }
    
    fun draw(renderer: ShapeRenderer) {
        renderer.color = Color(0.3f, 0.7f, 0.9f, 1f)
        renderer.circle(x, y, radius)
        
        renderer.color = Color(0.9f, 0.9f, 0.9f, 1f)
        val gunWidth = 6f
        val gunX = x + cos(aimAngle) * radius
        val gunY = y + sin(aimAngle) * radius
        val endX = gunX + cos(aimAngle) * gunLength
        val endY = gunY + sin(aimAngle) * gunLength
        renderer.rectLine(gunX, gunY, endX, endY, gunWidth)
        
        if (muzzleFlashTimer > 0) {
            drawMuzzleFlash(renderer)
        }
    }
    
    private fun drawMuzzleFlash(renderer: ShapeRenderer) {
        val flashProgress = muzzleFlashTimer / muzzleFlashDuration
        val flashSize = 12f + flashProgress * 8f
        
        renderer.color = Color(1f, 0.9f, 0.4f, flashProgress)
        renderer.circle(gunTipX, gunTipY, flashSize)
        
        renderer.color = Color(1f, 1f, 0.8f, flashProgress)
        renderer.circle(gunTipX, gunTipY, flashSize * 0.5f)
        
        val spikeLength = flashSize * 1.5f
        renderer.color = Color(1f, 0.8f, 0.3f, flashProgress * 0.8f)
        
        for (i in 0 until 4) {
            val spikeAngle = aimAngle + (i - 1.5f) * 0.3f
            val spikeEndX = gunTipX + cos(spikeAngle) * spikeLength
            val spikeEndY = gunTipY + sin(spikeAngle) * spikeLength
            renderer.rectLine(gunTipX, gunTipY, spikeEndX, spikeEndY, 3f * flashProgress)
        }
    }
}
