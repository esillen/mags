package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class Obstacle(
    var x: Float,
    var y: Float,
    val width: Float = 60f,
    val height: Float = 60f
) {
    private val maxHealth = 1000f
    private var health = maxHealth
    
    val isDead: Boolean get() = health <= 0
    var hasBeenSeen = false
    
    val left: Float get() = x - width / 2
    val right: Float get() = x + width / 2
    val top: Float get() = y + height / 2
    val bottom: Float get() = y - height / 2
    
    fun takeDamage(amount: Float) {
        health -= amount
    }
    
    fun collidesWithCircle(cx: Float, cy: Float, radius: Float): Boolean {
        val closestX = max(left, min(cx, right))
        val closestY = max(bottom, min(cy, top))
        
        val dx = cx - closestX
        val dy = cy - closestY
        return (dx * dx + dy * dy) < (radius * radius)
    }
    
    fun collidesWithPoint(px: Float, py: Float): Boolean {
        return px >= left && px <= right && py >= bottom && py <= top
    }
    
    fun blocksSightLine(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        return lineIntersectsRect(x1, y1, x2, y2)
    }
    
    private fun lineIntersectsRect(x1: Float, y1: Float, x2: Float, y2: Float): Boolean {
        // Check if line segment intersects rectangle
        if (collidesWithPoint(x1, y1) || collidesWithPoint(x2, y2)) return true
        
        // Check intersection with each edge
        return lineIntersectsLine(x1, y1, x2, y2, left, bottom, left, top) ||
               lineIntersectsLine(x1, y1, x2, y2, right, bottom, right, top) ||
               lineIntersectsLine(x1, y1, x2, y2, left, bottom, right, bottom) ||
               lineIntersectsLine(x1, y1, x2, y2, left, top, right, top)
    }
    
    private fun lineIntersectsLine(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float
    ): Boolean {
        val denom = (y4 - y3) * (x2 - x1) - (x4 - x3) * (y2 - y1)
        if (abs(denom) < 0.0001f) return false
        
        val ua = ((x4 - x3) * (y1 - y3) - (y4 - y3) * (x1 - x3)) / denom
        val ub = ((x2 - x1) * (y1 - y3) - (y2 - y1) * (x1 - x3)) / denom
        
        return ua >= 0 && ua <= 1 && ub >= 0 && ub <= 1
    }
    
    fun pushOutCircle(cx: Float, cy: Float, radius: Float): Pair<Float, Float>? {
        if (!collidesWithCircle(cx, cy, radius)) return null
        
        val closestX = max(left, min(cx, right))
        val closestY = max(bottom, min(cy, top))
        
        var dx = cx - closestX
        var dy = cy - closestY
        val dist = kotlin.math.sqrt(dx * dx + dy * dy)
        
        if (dist < 0.001f) {
            // Circle center is inside rectangle, push to nearest edge
            val distToLeft = cx - left
            val distToRight = right - cx
            val distToBottom = cy - bottom
            val distToTop = top - cy
            
            val minDist = minOf(distToLeft, distToRight, distToBottom, distToTop)
            
            return when (minDist) {
                distToLeft -> Pair(left - radius, cy)
                distToRight -> Pair(right + radius, cy)
                distToBottom -> Pair(cx, bottom - radius)
                else -> Pair(cx, top + radius)
            }
        }
        
        dx /= dist
        dy /= dist
        val pushDist = radius - dist + 0.1f
        
        return Pair(cx + dx * pushDist, cy + dy * pushDist)
    }
    
    fun draw(renderer: ShapeRenderer) {
        val healthPercent = health / maxHealth
        val r = 0.4f + 0.2f * (1f - healthPercent)
        val g = 0.35f + 0.1f * healthPercent
        val b = 0.3f
        renderer.color = Color(r, g, b, 1f)
        renderer.rect(left, bottom, width, height)
        
        // Darker border
        renderer.color = Color(0.25f, 0.22f, 0.2f, 1f)
        val borderSize = 3f
        renderer.rect(left, bottom, width, borderSize)
        renderer.rect(left, top - borderSize, width, borderSize)
        renderer.rect(left, bottom, borderSize, height)
        renderer.rect(right - borderSize, bottom, borderSize, height)
    }
    
    fun drawRevealed(renderer: ShapeRenderer) {
        // Draw dimmed version when revealed but not currently visible
        renderer.color = Color(0.3f, 0.28f, 0.25f, 0.7f)
        renderer.rect(left, bottom, width, height)
    }
}
