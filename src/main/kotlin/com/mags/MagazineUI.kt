package com.mags

import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.glutils.ShapeRenderer

class MagazineUI {
    private val magWidth = 60f
    private val magHeight = 120f
    private val magSpacing = 20f
    private val bottomPadding = 20f
    private val bulletSize = 8f
    private val bulletSpacing = 4f
    
    fun draw(renderer: ShapeRenderer, manager: MagazineManager) {
        val magazines = manager.allMagazines
        val totalWidth = magazines.size * magWidth + (magazines.size - 1) * magSpacing
        val startX = (1280f - totalWidth) / 2
        
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        
        magazines.forEachIndexed { index, magazine ->
            val x = startX + index * (magWidth + magSpacing)
            val y = bottomPadding
            val isSelected = index == manager.selectedIndex
            
            drawMagazine(renderer, magazine, x, y, isSelected)
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
    }
    
    private fun drawMagazine(
        renderer: ShapeRenderer,
        magazine: Magazine,
        x: Float,
        y: Float,
        isSelected: Boolean
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
        renderer.rect(x + innerPadding, y + innerPadding, magWidth - innerPadding * 2, magHeight - innerPadding * 2 - 15)
        
        val bullets = magazine.bulletList
        val bulletTotalHeight = bulletSize + bulletSpacing
        
        bullets.forEachIndexed { index, bulletType ->
            if (index < magazine.capacity) {
                val bulletX = x + magWidth / 2
                val bulletY = y + innerPadding + 5 + index * bulletTotalHeight + bulletSize / 2
                
                val bulletColor = if (index == bullets.lastIndex) {
                    bulletType.color
                } else {
                    Color(bulletType.color.r * 0.7f, bulletType.color.g * 0.7f, bulletType.color.b * 0.7f, 1f)
                }
                renderer.color = bulletColor
                renderer.circle(bulletX, bulletY, bulletSize / 2)
                
                if (bulletType.isInfinite) {
                    renderer.color = Color.WHITE
                    renderer.circle(bulletX, bulletY, 2f)
                }
            }
        }
        
        for (i in bullets.size until magazine.capacity) {
            val bulletX = x + magWidth / 2
            val bulletY = y + innerPadding + 5 + i * bulletTotalHeight + bulletSize / 2
            renderer.color = Color(0.15f, 0.15f, 0.18f, 1f)
            renderer.circle(bulletX, bulletY, bulletSize / 2)
        }
        
        renderer.color = Color(0.3f, 0.3f, 0.35f, 1f)
        val keySize = 16f
        renderer.rect(x + magWidth / 2 - keySize / 2, y + magHeight - 18, keySize, keySize)
    }
}
