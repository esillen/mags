package com.mags

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.SpriteBatch
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.mags.bullet.BulletDisplayShape
import com.mags.bullet.BulletType

class MagazineUI {
    private val magWidth = 100f
    private val magHeight = 160f
    private val magSpacing = 14f
    private val bottomPadding = 15f
    private val bulletWidth = 36f
    private val bulletHeight = 12f
    private val bulletSpacing = 6f
    
    private val spriteBatch = SpriteBatch()
    private val font = BitmapFont().apply {
        data.setScale(0.8f)
        color = Color.WHITE
    }
    
    fun draw(renderer: ShapeRenderer, manager: MagazineManager) {
        val magazines = manager.allMagazines
        val totalWidth = magazines.size * magWidth + (magazines.size - 1) * magSpacing
        val startX = (1280f - totalWidth) / 2
        
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        
        magazines.forEachIndexed { index, magazine ->
            val x = startX + index * (magWidth + magSpacing)
            val y = bottomPadding
            val isSelected = index == manager.selectedIndex
            
            drawMagazine(renderer, magazine, x, y, isSelected, manager)
        }
        
        renderer.end()
        
        renderer.begin(ShapeRenderer.ShapeType.Line)
        magazines.forEachIndexed { index, _ ->
            val x = startX + index * (magWidth + magSpacing)
            val y = bottomPadding
            val isSelected = index == manager.selectedIndex
            
            if (isSelected) {
                renderer.color = Color.WHITE
                renderer.rect(x - 3, y - 3, magWidth + 6, magHeight + 6)
            }
        }
        renderer.end()
        
        // Draw text labels
        spriteBatch.projectionMatrix = renderer.projectionMatrix
        spriteBatch.begin()
        
        magazines.forEachIndexed { index, magazine ->
            val x = startX + index * (magWidth + magSpacing)
            val y = bottomPadding
            
            val bullets = magazine.bulletList
            val bulletTotalHeight = bulletHeight + bulletSpacing
            val innerPadding = 6f
            
            bullets.forEachIndexed { bulletIndex, bulletType ->
                if (bulletIndex < magazine.capacity) {
                    val bulletX = x + magWidth / 2
                    val bulletY = y + innerPadding + 8 + bulletIndex * bulletTotalHeight + bulletHeight / 2
                    
                    drawUsesText(bulletType, bulletX, bulletY)
                }
            }
        }
        
        spriteBatch.end()
    }
    
    private fun drawUsesText(bulletType: BulletType, x: Float, y: Float) {
        val text = when {
            bulletType.isInfinite -> "∞"
            bulletType.usesRemaining > 1 -> "x${bulletType.usesRemaining}"
            else -> null
        }
        
        text?.let {
            font.color = Color(0.9f, 0.9f, 0.9f, 0.95f)
            font.draw(spriteBatch, it, x + bulletWidth / 2 + 4, y + 5)
        }
    }
    
    private fun drawMagazine(
        renderer: ShapeRenderer,
        magazine: Magazine,
        x: Float,
        y: Float,
        isSelected: Boolean,
        manager: MagazineManager
    ) {
        val bgColor = if (isSelected) {
            Color(magazine.color.r * 0.8f, magazine.color.g * 0.8f, magazine.color.b * 0.8f, 1f)
        } else {
            Color(magazine.color.r * 0.4f, magazine.color.g * 0.4f, magazine.color.b * 0.4f, 1f)
        }
        renderer.color = bgColor
        renderer.rect(x, y, magWidth, magHeight)
        
        renderer.color = Color(0.2f, 0.2f, 0.25f, 1f)
        val innerPadding = 6f
        renderer.rect(x + innerPadding, y + innerPadding, magWidth - innerPadding * 2, magHeight - innerPadding * 2 - 20)
        
        val bullets = magazine.bulletList
        val bulletTotalHeight = bulletHeight + bulletSpacing
        
        bullets.forEachIndexed { index, bulletType ->
            if (index < magazine.capacity) {
                val bulletX = x + magWidth / 2 - 8
                val bulletY = y + innerPadding + 8 + index * bulletTotalHeight + bulletHeight / 2
                
                val isTop = index == bullets.lastIndex
                val brightness = if (isTop) 1f else 0.7f
                
                drawBulletShape(renderer, bulletType, bulletX, bulletY, brightness)
            }
        }
        
        for (i in bullets.size until magazine.capacity) {
            val bulletX = x + magWidth / 2 - 8
            val bulletY = y + innerPadding + 8 + i * bulletTotalHeight + bulletHeight / 2
            renderer.color = Color(0.15f, 0.15f, 0.18f, 1f)
            renderer.rect(bulletX - bulletWidth / 2, bulletY - bulletHeight / 2, bulletWidth, bulletHeight)
        }
        
        renderer.color = Color(0.3f, 0.3f, 0.35f, 1f)
        val keySize = 18f
        renderer.rect(x + magWidth / 2 - keySize / 2, y + magHeight - 22, keySize, keySize)
        
        if (isSelected && (manager.isReloading || manager.isRearranging)) {
            val progress = if (manager.isReloading) manager.reloadProgress else manager.rearrangeProgress
            val barColor = if (manager.isReloading) Color(0.8f, 0.4f, 0.2f, 1f) else Color(0.4f, 0.6f, 0.8f, 1f)
            
            renderer.color = Color(0.1f, 0.1f, 0.1f, 0.8f)
            renderer.rect(x, y + magHeight + 5, magWidth, 5f)
            renderer.color = barColor
            renderer.rect(x, y + magHeight + 5, magWidth * progress, 5f)
        }
    }
    
    private fun drawBulletShape(
        renderer: ShapeRenderer,
        bulletType: BulletType,
        x: Float,
        y: Float,
        brightness: Float
    ) {
        val color = Color(
            bulletType.color.r * brightness,
            bulletType.color.g * brightness,
            bulletType.color.b * brightness,
            1f
        )
        renderer.color = color
        
        when (bulletType.displayShape) {
            BulletDisplayShape.CIRCLE -> {
                renderer.rect(x - bulletWidth / 2 + 6, y - bulletHeight / 2, bulletWidth - 12, bulletHeight)
                renderer.circle(x - bulletWidth / 2 + 6, y, bulletHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 6, y, bulletHeight / 2)
            }
            BulletDisplayShape.LONG -> {
                renderer.rect(x - bulletWidth / 2, y - bulletHeight / 2, bulletWidth - 8, bulletHeight)
                renderer.color = Color(color.r * 1.2f, color.g * 1.2f, color.b * 0.8f, 1f)
                renderer.triangle(
                    x + bulletWidth / 2 - 8, y - bulletHeight / 2,
                    x + bulletWidth / 2 - 8, y + bulletHeight / 2,
                    x + bulletWidth / 2, y
                )
            }
            BulletDisplayShape.BIG_CIRCLE -> {
                val bigHeight = bulletHeight + 4
                renderer.rect(x - bulletWidth / 2 + 4, y - bigHeight / 2, bulletWidth - 8, bigHeight)
                renderer.circle(x - bulletWidth / 2 + 4, y, bigHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 4, y, bigHeight / 2)
                renderer.color = Color(color.r * 0.6f, color.g * 0.6f, color.b * 0.6f, 1f)
                renderer.circle(x, y, 5f)
            }
            BulletDisplayShape.FLAME -> {
                renderer.rect(x - bulletWidth / 2 + 6, y - bulletHeight / 2, bulletWidth - 12, bulletHeight)
                renderer.circle(x - bulletWidth / 2 + 6, y, bulletHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 6, y, bulletHeight / 2)
                renderer.color = Color(1f, 0.8f, 0.2f, brightness)
                renderer.circle(x + bulletWidth / 2 - 3, y - 4, 4f)
                renderer.circle(x + bulletWidth / 2, y + 3, 3f)
                renderer.color = Color(1f, 0.3f, 0.1f, brightness)
                renderer.circle(x + bulletWidth / 2 + 4, y, 3f)
            }
            BulletDisplayShape.ICE -> {
                renderer.rect(x - bulletWidth / 2 + 6, y - bulletHeight / 2, bulletWidth - 12, bulletHeight)
                renderer.circle(x - bulletWidth / 2 + 6, y, bulletHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 6, y, bulletHeight / 2)
                renderer.color = Color(0.9f, 0.95f, 1f, brightness)
                renderer.circle(x, y, 4f)
                renderer.color = Color(0.7f, 0.9f, 1f, brightness * 0.8f)
                renderer.rect(x - 3, y - bulletHeight / 2 - 4, 6f, 4f)
                renderer.rect(x - 3, y + bulletHeight / 2, 6f, 4f)
            }
            BulletDisplayShape.ELEMENTAL -> {
                renderer.rect(x - bulletWidth / 2 + 6, y - bulletHeight / 2, bulletWidth - 12, bulletHeight)
                renderer.circle(x - bulletWidth / 2 + 6, y, bulletHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 6, y, bulletHeight / 2)
                renderer.color = Color(1f, 1f, 1f, brightness * 0.7f)
                renderer.circle(x, y, 5f)
                renderer.color = color
                renderer.circle(x - 10, y, 4f)
                renderer.circle(x + 10, y, 4f)
            }
        }
    }
    
    fun dispose() {
        spriteBatch.dispose()
        font.dispose()
    }
}
