package com.mags

import com.badlogic.gdx.graphics.Color
import com.mags.bullet.Bullet
import com.mags.bullet.BulletType
import com.mags.bullet.BulletTypes

class Magazine(
    val name: String,
    val capacity: Int,
    val color: Color
) {
    private val bullets = mutableListOf<BulletType>()
    
    val bulletCount: Int get() = bullets.size
    val bulletList: List<BulletType> get() = bullets.toList()
    val isEmpty: Boolean get() = bullets.isEmpty()
    val topBullet: BulletType? get() = bullets.lastOrNull()
    
    private var reloadTimer = 0f
    private var rearrangeTimer = 0f
    private var currentReloadTime = 0f
    private var currentRearrangeTime = 0f
    
    val isReloading: Boolean get() = reloadTimer > 0
    val isRearranging: Boolean get() = rearrangeTimer > 0
    val canShoot: Boolean get() = !isReloading
    val canRearrange: Boolean get() = !isReloading && !isRearranging
    
    val reloadProgress: Float get() = if (currentReloadTime > 0) 1f - (reloadTimer / currentReloadTime).coerceIn(0f, 1f) else 0f
    val rearrangeProgress: Float get() = if (currentRearrangeTime > 0) 1f - (rearrangeTimer / currentRearrangeTime).coerceIn(0f, 1f) else 0f
    
    fun update(delta: Float) {
        if (reloadTimer > 0) {
            reloadTimer -= delta
            if (reloadTimer < 0) reloadTimer = 0f
        }
        if (rearrangeTimer > 0) {
            rearrangeTimer -= delta
            if (rearrangeTimer < 0) rearrangeTimer = 0f
        }
    }
    
    fun addBullet(bullet: BulletType): Boolean {
        if (bullets.size < capacity) {
            bullets.add(bullet)
            return true
        }
        return false
    }
    
    fun addBulletToBottom(bullet: BulletType): Boolean {
        if (bullets.size < capacity) {
            bullets.add(0, bullet)
            return true
        }
        return false
    }
    
    fun peekTop(): BulletType? = bullets.lastOrNull()
    
    fun shoot(x: Float, y: Float, angle: Float): Bullet? {
        if (!canShoot || bullets.isEmpty()) return null
        
        val bulletType = bullets.removeAt(bullets.lastIndex)
        val bullet = bulletType.createBullet(x, y, angle)
        
        currentReloadTime = bulletType.reloadTime
        reloadTimer = currentReloadTime
        
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
    
    fun rearrange(): Boolean {
        if (!canRearrange || bullets.isEmpty()) return false
        
        val bulletType = bullets.removeAt(bullets.lastIndex)
        bullets.add(0, bulletType)
        
        currentRearrangeTime = bulletType.rearrangeTime
        rearrangeTimer = currentRearrangeTime
        
        return true
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
    
    val selectedMagazine: Magazine get() = magazines[selectedIndex]
    val allMagazines: List<Magazine> get() = magazines
    
    init {
        magazines.forEach { mag ->
            mag.addBullet(BulletTypes.INFINITE)
            repeat(mag.capacity - 1) {
                BulletTypes.getRandomDrop()?.let { mag.addBullet(it) }
            }
        }
    }
    
    fun update(delta: Float) {
        magazines.forEach { it.update(delta) }
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
    
    fun rearrange(): Boolean {
        return selectedMagazine.rearrange()
    }
    
    fun addBulletToMagazine(bullet: BulletType, magazineIndex: Int): Boolean {
        if (magazineIndex in magazines.indices) {
            return magazines[magazineIndex].addBullet(bullet)
        }
        return false
    }
    
    fun addBulletToFirstAvailable(bullet: BulletType): Boolean {
        for (magazine in magazines) {
            if (magazine.addBullet(bullet)) {
                return true
            }
        }
        return false
    }
}
