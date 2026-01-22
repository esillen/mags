package com.mags

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.g2d.BitmapFont
import com.badlogic.gdx.graphics.g2d.GlyphLayout
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
    private val tooltipFont = BitmapFont().apply {
        data.setScale(0.7f)
        color = Color.WHITE
    }
    private val glyphLayout = GlyphLayout()
    
    private var hoveredBullet: BulletType? = null
    private var tooltipX = 0f
    private var tooltipY = 0f
    
    fun draw(renderer: ShapeRenderer, manager: MagazineManager) {
        val magazines = manager.allMagazines
        val totalWidth = magazines.size * magWidth + (magazines.size - 1) * magSpacing
        val startX = (1280f - totalWidth) / 2
        
        // Get mouse position in UI coordinates
        val mouseX = Gdx.input.x.toFloat()
        val mouseY = 720f - Gdx.input.y.toFloat()
        
        // Find hovered bullet
        hoveredBullet = null
        findHoveredBullet(magazines, startX, mouseX, mouseY)
        
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        
        magazines.forEachIndexed { index, magazine ->
            val x = startX + index * (magWidth + magSpacing)
            val y = bottomPadding
            val isSelected = index == manager.selectedIndex
            
            drawMagazine(renderer, magazine, x, y, isSelected, mouseX, mouseY)
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
        
        // Draw tooltip if hovering over a bullet
        hoveredBullet?.let { bullet ->
            drawTooltip(renderer, bullet)
        }
    }
    
    private fun findHoveredBullet(magazines: List<Magazine>, startX: Float, mouseX: Float, mouseY: Float) {
        val innerPadding = 6f
        val bulletTotalHeight = bulletHeight + bulletSpacing
        
        magazines.forEachIndexed { magIndex, magazine ->
            val magX = startX + magIndex * (magWidth + magSpacing)
            val magY = bottomPadding
            
            val bullets = magazine.bulletList
            bullets.forEachIndexed { bulletIndex, bulletType ->
                if (bulletIndex < magazine.capacity) {
                    val bulletX = magX + magWidth / 2 - 8
                    val bulletY = magY + innerPadding + 8 + bulletIndex * bulletTotalHeight + bulletHeight / 2
                    
                    // Check if mouse is over this bullet
                    val hitLeft = bulletX - bulletWidth / 2 - 5
                    val hitRight = bulletX + bulletWidth / 2 + 20
                    val hitBottom = bulletY - bulletHeight / 2 - 3
                    val hitTop = bulletY + bulletHeight / 2 + 3
                    
                    if (mouseX >= hitLeft && mouseX <= hitRight && mouseY >= hitBottom && mouseY <= hitTop) {
                        hoveredBullet = bulletType
                        tooltipX = mouseX + 15
                        tooltipY = mouseY + 10
                    }
                }
            }
        }
    }
    
    private fun drawTooltip(renderer: ShapeRenderer, bullet: BulletType) {
        val lines = buildTooltipLines(bullet)
        val lineHeight = 14f
        val padding = 8f
        
        // Calculate tooltip size
        var maxWidth = 0f
        lines.forEach { line ->
            glyphLayout.setText(tooltipFont, line)
            if (glyphLayout.width > maxWidth) maxWidth = glyphLayout.width
        }
        
        val tooltipWidth = maxWidth + padding * 2
        val tooltipHeight = lines.size * lineHeight + padding * 2
        
        // Adjust position to keep tooltip on screen
        var drawX = tooltipX
        var drawY = tooltipY
        if (drawX + tooltipWidth > 1280f) drawX = 1280f - tooltipWidth - 5
        if (drawY + tooltipHeight > 720f) drawY = 720f - tooltipHeight - 5
        
        // Draw tooltip background
        renderer.begin(ShapeRenderer.ShapeType.Filled)
        renderer.color = Color(0.1f, 0.1f, 0.15f, 0.95f)
        renderer.rect(drawX, drawY, tooltipWidth, tooltipHeight)
        
        // Draw bullet color indicator
        renderer.color = bullet.color
        renderer.rect(drawX + 3, drawY + tooltipHeight - padding - 10, 4f, 12f)
        
        renderer.end()
        
        // Draw tooltip border
        renderer.begin(ShapeRenderer.ShapeType.Line)
        renderer.color = Color(0.4f, 0.4f, 0.5f, 1f)
        renderer.rect(drawX, drawY, tooltipWidth, tooltipHeight)
        renderer.end()
        
        // Draw tooltip text
        spriteBatch.begin()
        tooltipFont.color = Color.WHITE
        
        lines.forEachIndexed { index, line ->
            val textY = drawY + tooltipHeight - padding - index * lineHeight
            val textX = if (index == 0) drawX + padding + 8 else drawX + padding
            
            // Color code certain values
            when {
                line.contains("DMG") -> tooltipFont.color = Color(1f, 0.7f, 0.7f, 1f)
                line.contains("SPD") -> tooltipFont.color = Color(0.7f, 0.9f, 1f, 1f)
                line.contains("RLD") -> tooltipFont.color = Color(1f, 0.9f, 0.6f, 1f)
                line.contains("ARR") -> tooltipFont.color = Color(0.7f, 0.8f, 1f, 1f)
                line.contains("Effect") -> tooltipFont.color = Color(0.9f, 0.7f, 1f, 1f)
                line.contains("Element") -> tooltipFont.color = Color(0.7f, 1f, 0.8f, 1f)
                else -> tooltipFont.color = Color.WHITE
            }
            
            tooltipFont.draw(spriteBatch, line, textX, textY)
        }
        
        spriteBatch.end()
    }
    
    private fun buildTooltipLines(bullet: BulletType): List<String> {
        val lines = mutableListOf<String>()
        
        lines.add(bullet.name)
        lines.add("DMG: ${bullet.damage.toInt()}")
        lines.add("SPD: ${bullet.speed.toInt()}")
        lines.add("RLD: ${String.format("%.2f", bullet.reloadTime)}s")
        lines.add("ARR: ${String.format("%.2f", bullet.rearrangeTime)}s")
        
        if (bullet.splashDamage > 0) {
            lines.add("Splash: ${bullet.splashDamage.toInt()}")
        }
        
        bullet.statusEffect?.let {
            lines.add("Effect: ${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }}")
        }
        
        bullet.element?.let {
            lines.add("Element: ${it.name.lowercase().replaceFirstChar { c -> c.uppercase() }}")
        }
        
        val uses = when {
            bullet.isInfinite -> "∞"
            else -> "${bullet.usesRemaining}"
        }
        lines.add("Uses: $uses")
        
        return lines
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
        mouseX: Float,
        mouseY: Float
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
                
                // Check if this bullet is hovered
                val isHovered = hoveredBullet == bulletType && 
                    mouseX >= bulletX - bulletWidth / 2 - 5 && 
                    mouseX <= bulletX + bulletWidth / 2 + 20 &&
                    mouseY >= bulletY - bulletHeight / 2 - 3 && 
                    mouseY <= bulletY + bulletHeight / 2 + 3
                
                val brightness = when {
                    isHovered -> 1.2f
                    isTop -> 1f
                    else -> 0.7f
                }
                
                // Draw highlight behind hovered bullet
                if (isHovered) {
                    renderer.color = Color(1f, 1f, 1f, 0.15f)
                    renderer.rect(bulletX - bulletWidth / 2 - 3, bulletY - bulletHeight / 2 - 2, 
                                  bulletWidth + 25, bulletHeight + 4)
                }
                
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
        
        // Draw timer bars above magazine for all magazines that have active timers
        val barY = y + magHeight + 5
        val barHeight = 5f
        
        if (magazine.isReloading) {
            renderer.color = Color(0.1f, 0.1f, 0.1f, 0.8f)
            renderer.rect(x, barY, magWidth, barHeight)
            renderer.color = Color(0.9f, 0.4f, 0.2f, 1f)
            renderer.rect(x, barY, magWidth * magazine.reloadProgress, barHeight)
        } else if (magazine.isRearranging) {
            renderer.color = Color(0.1f, 0.1f, 0.1f, 0.8f)
            renderer.rect(x, barY, magWidth, barHeight)
            renderer.color = Color(0.4f, 0.6f, 0.9f, 1f)
            renderer.rect(x, barY, magWidth * magazine.rearrangeProgress, barHeight)
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
            (bulletType.color.r * brightness).coerceAtMost(1f),
            (bulletType.color.g * brightness).coerceAtMost(1f),
            (bulletType.color.b * brightness).coerceAtMost(1f),
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
                renderer.color = Color((color.r * 1.2f).coerceAtMost(1f), (color.g * 1.2f).coerceAtMost(1f), color.b * 0.8f, 1f)
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
                renderer.color = Color(1f, 0.8f, 0.2f, brightness.coerceAtMost(1f))
                renderer.circle(x + bulletWidth / 2 - 3, y - 4, 4f)
                renderer.circle(x + bulletWidth / 2, y + 3, 3f)
                renderer.color = Color(1f, 0.3f, 0.1f, brightness.coerceAtMost(1f))
                renderer.circle(x + bulletWidth / 2 + 4, y, 3f)
            }
            BulletDisplayShape.ICE -> {
                renderer.rect(x - bulletWidth / 2 + 6, y - bulletHeight / 2, bulletWidth - 12, bulletHeight)
                renderer.circle(x - bulletWidth / 2 + 6, y, bulletHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 6, y, bulletHeight / 2)
                renderer.color = Color(0.9f, 0.95f, 1f, brightness.coerceAtMost(1f))
                renderer.circle(x, y, 4f)
                renderer.color = Color(0.7f, 0.9f, 1f, (brightness * 0.8f).coerceAtMost(1f))
                renderer.rect(x - 3, y - bulletHeight / 2 - 4, 6f, 4f)
                renderer.rect(x - 3, y + bulletHeight / 2, 6f, 4f)
            }
            BulletDisplayShape.ELEMENTAL -> {
                renderer.rect(x - bulletWidth / 2 + 6, y - bulletHeight / 2, bulletWidth - 12, bulletHeight)
                renderer.circle(x - bulletWidth / 2 + 6, y, bulletHeight / 2)
                renderer.circle(x + bulletWidth / 2 - 6, y, bulletHeight / 2)
                renderer.color = Color(1f, 1f, 1f, (brightness * 0.7f).coerceAtMost(1f))
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
        tooltipFont.dispose()
    }
}
