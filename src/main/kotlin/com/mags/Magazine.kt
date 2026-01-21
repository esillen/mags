package com.mags

import com.badlogic.gdx.graphics.Color

data class BulletType(
    val name: String,
    val speed: Float,
    val damage: Float,
    val radius: Float,
    val color: Color,
    val usesRemaining: Int // -1 means infinite
) {
    fun createBullet(x: Float, y: Float, angle: Float): Bullet {
        return Bullet(x, y, angle, speed, damage, radius, color)
    }
    
    fun withDecrementedUse(): BulletType {
        return if (usesRemaining > 0) copy(usesRemaining = usesRemaining - 1) else this
    }
    
    val isInfinite: Boolean
        get() = usesRemaining == -1
    
    val isExhausted: Boolean
        get() = usesRemaining == 0
}

class Magazine(
    val name: String,
    val capacity: Int,
    val color: Color
) {
    private val bullets = mutableListOf<BulletType>()
    
    val bulletCount: Int
        get() = bullets.size
    
    val bulletList: List<BulletType>
        get() = bullets.toList()
    
    fun addBullet(bullet: BulletType) {
        if (bullets.size < capacity) {
            bullets.add(bullet)
        }
    }
    
    fun addBulletToBottom(bullet: BulletType) {
        if (bullets.size < capacity) {
            bullets.add(0, bullet)
        }
    }
    
    fun shoot(x: Float, y: Float, angle: Float): Bullet? {
        if (bullets.isEmpty()) return null
        
        val bulletType = bullets.removeAt(bullets.lastIndex)
        val bullet = bulletType.createBullet(x, y, angle)
        
        if (bulletType.isInfinite) {
            bullets.add(0, bulletType)
        } else {
            val decremented = bulletType.withDecrementedUse()
            if (!decremented.isExhausted) {
                bullets.add(0, decremented)
            }
        }
        
        return bullet
    }
}

class MagazineManager {
    private val magazines = listOf(
        Magazine("Primary", 6, Color(0.4f, 0.6f, 0.8f, 1f)),
        Magazine("Secondary", 4, Color(0.8f, 0.6f, 0.4f, 1f)),
        Magazine("Heavy", 3, Color(0.6f, 0.4f, 0.6f, 1f)),
        Magazine("Special", 8, Color(0.4f, 0.8f, 0.5f, 1f))
    )
    
    var selectedIndex = 0
        private set
    
    val selectedMagazine: Magazine
        get() = magazines[selectedIndex]
    
    val allMagazines: List<Magazine>
        get() = magazines
    
    init {
        val weakBullet = BulletType(
            name = "Weak",
            speed = 400f,
            damage = 15f,
            radius = 5f,
            color = Color(0.9f, 0.9f, 0.5f, 1f),
            usesRemaining = -1
        )
        magazines.forEach { it.addBullet(weakBullet) }
    }
    
    fun selectMagazine(index: Int) {
        if (index in magazines.indices) {
            selectedIndex = index
        }
    }
    
    fun cycleNext() {
        selectedIndex = (selectedIndex + 1) % magazines.size
    }
    
    fun cyclePrevious() {
        selectedIndex = (selectedIndex - 1 + magazines.size) % magazines.size
    }
    
    fun shoot(x: Float, y: Float, angle: Float): Bullet? {
        return selectedMagazine.shoot(x, y, angle)
    }
}
