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
    
    fun rearrangeTopToBottom(): BulletType? {
        if (bullets.isEmpty()) return null
        val top = bullets.removeAt(bullets.lastIndex)
        bullets.add(0, top)
        return top
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
    
    private var reloadTimer = 0f
    private var rearrangeTimer = 0f
    val isReloading: Boolean get() = reloadTimer > 0
    val isRearranging: Boolean get() = rearrangeTimer > 0
    val canAct: Boolean get() = !isReloading && !isRearranging
    
    var reloadProgress: Float = 0f
        private set
    var rearrangeProgress: Float = 0f
        private set
    
    private var currentReloadTime = 0f
    private var currentRearrangeTime = 0f
    
    init {
        magazines.forEach { mag ->
            mag.addBullet(BulletTypes.INFINITE)
            repeat(mag.capacity - 1) {
                BulletTypes.getRandomDrop()?.let { mag.addBullet(it) }
            }
        }
    }
    
    fun update(delta: Float) {
        if (reloadTimer > 0) {
            reloadTimer -= delta
            reloadProgress = 1f - (reloadTimer / currentReloadTime).coerceIn(0f, 1f)
            if (reloadTimer <= 0) {
                reloadTimer = 0f
                reloadProgress = 0f
            }
        }
        if (rearrangeTimer > 0) {
            rearrangeTimer -= delta
            rearrangeProgress = 1f - (rearrangeTimer / currentRearrangeTime).coerceIn(0f, 1f)
            if (rearrangeTimer <= 0) {
                rearrangeTimer = 0f
                rearrangeProgress = 0f
            }
        }
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
        if (!canAct) return null
        
        val bulletType = selectedMagazine.peekTop() ?: return null
        val bullet = selectedMagazine.shoot(x, y, angle) ?: return null
        
        currentReloadTime = bulletType.reloadTime
        reloadTimer = currentReloadTime
        
        return bullet
    }
    
    fun rearrange(): Boolean {
        if (!canAct) return false
        
        val bulletType = selectedMagazine.peekTop() ?: return false
        selectedMagazine.rearrangeTopToBottom() ?: return false
        
        currentRearrangeTime = bulletType.rearrangeTime
        rearrangeTimer = currentRearrangeTime
        
        return true
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
