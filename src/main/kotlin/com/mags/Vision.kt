package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class VisionSystem(
    private val viewDistance: Float = 1000f,
    private val rayCount: Int = 500
) {
    private val visiblePoints = mutableListOf<Pair<Float, Float>>()
    
    fun updateVision(playerX: Float, playerY: Float, obstacles: List<Obstacle>) {
        visiblePoints.clear()
        
        val angleStep = (2 * Math.PI / rayCount).toFloat()
        
        for (i in 0 until rayCount) {
            val angle = i * angleStep
            val endX = playerX + cos(angle) * viewDistance
            val endY = playerY + sin(angle) * viewDistance
            
            val hitPoint = castRay(playerX, playerY, endX, endY, obstacles)
            visiblePoints.add(hitPoint)
        }
        
        // Mark obstacles that are visible
        obstacles.forEach { obstacle ->
            if (isPointVisible(playerX, playerY, obstacle.x, obstacle.y, obstacles)) {
                obstacle.hasBeenSeen = true
            }
        }
    }
    
    private fun castRay(x1: Float, y1: Float, x2: Float, y2: Float, obstacles: List<Obstacle>): Pair<Float, Float> {
        var closestDist = Float.MAX_VALUE
        var hitX = x2
        var hitY = y2
        
        val dx = x2 - x1
        val dy = y2 - y1
        val rayLen = sqrt(dx * dx + dy * dy)
        val dirX = dx / rayLen
        val dirY = dy / rayLen
        
        for (obstacle in obstacles) {
            val intersection = rayBoxIntersection(x1, y1, dirX, dirY, obstacle)
            if (intersection != null) {
                val dist = sqrt((intersection.first - x1) * (intersection.first - x1) + 
                               (intersection.second - y1) * (intersection.second - y1))
                if (dist < closestDist && dist < rayLen) {
                    closestDist = dist
                    hitX = intersection.first
                    hitY = intersection.second
                }
            }
        }
        
        return Pair(hitX, hitY)
    }
    
    private fun rayBoxIntersection(
        rayX: Float, rayY: Float,
        dirX: Float, dirY: Float,
        obstacle: Obstacle
    ): Pair<Float, Float>? {
        val tMin: Float
        val tMax: Float
        
        if (dirX != 0f) {
            val t1 = (obstacle.left - rayX) / dirX
            val t2 = (obstacle.right - rayX) / dirX
            tMin = minOf(t1, t2)
            tMax = maxOf(t1, t2)
        } else {
            if (rayX < obstacle.left || rayX > obstacle.right) return null
            tMin = Float.NEGATIVE_INFINITY
            tMax = Float.POSITIVE_INFINITY
        }
        
        val tyMin: Float
        val tyMax: Float
        
        if (dirY != 0f) {
            val t1 = (obstacle.bottom - rayY) / dirY
            val t2 = (obstacle.top - rayY) / dirY
            tyMin = minOf(t1, t2)
            tyMax = maxOf(t1, t2)
        } else {
            if (rayY < obstacle.bottom || rayY > obstacle.top) return null
            tyMin = Float.NEGATIVE_INFINITY
            tyMax = Float.POSITIVE_INFINITY
        }
        
        val tEnter = maxOf(tMin, tyMin)
        val tExit = minOf(tMax, tyMax)
        
        if (tEnter > tExit || tExit < 0) return null
        
        val t = if (tEnter > 0) tEnter else tExit
        if (t < 0) return null
        
        return Pair(rayX + dirX * t, rayY + dirY * t)
    }
    
    fun isPointVisible(playerX: Float, playerY: Float, targetX: Float, targetY: Float, obstacles: List<Obstacle>): Boolean {
        val dx = targetX - playerX
        val dy = targetY - playerY
        val dist = sqrt(dx * dx + dy * dy)
        
        if (dist > viewDistance) return false
        
        for (obstacle in obstacles) {
            if (obstacle.blocksSightLine(playerX, playerY, targetX, targetY)) {
                return false
            }
        }
        return true
    }
    
    fun isCircleVisible(playerX: Float, playerY: Float, cx: Float, cy: Float, radius: Float, obstacles: List<Obstacle>): Boolean {
        // Check center and a few points around the edge
        if (isPointVisible(playerX, playerY, cx, cy, obstacles)) return true
        
        val checkPoints = 8
        for (i in 0 until checkPoints) {
            val angle = (i.toFloat() / checkPoints) * 2 * Math.PI.toFloat()
            val px = cx + cos(angle) * radius
            val py = cy + sin(angle) * radius
            if (isPointVisible(playerX, playerY, px, py, obstacles)) return true
        }
        return false
    }
    
    @Suppress("UNUSED_PARAMETER")
    fun drawFog(renderer: ShapeRenderer, playerX: Float, playerY: Float, screenWidth: Float, screenHeight: Float) {
        // Fog drawing is handled in GameScreen.drawFogOfWar()
    }
    
    fun getVisiblePolygon(): List<Pair<Float, Float>> = visiblePoints.toList()
}
