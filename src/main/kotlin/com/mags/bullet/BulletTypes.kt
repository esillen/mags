package com.mags.bullet

import com.badlogic.gdx.graphics.Color

enum class BulletBehavior {
    NORMAL,
    GRENADE,
    BOMB
}

enum class BulletRarity(val dropWeight: Float) {
    COMMON(1.0f),
    MEDIUM(0.6f),
    RARE(0.2f),
    SHOP_ONLY(0f)
}

enum class Element(val color: Color) {
    RED(Color(1f, 0.3f, 0.3f, 1f)),
    GREEN(Color(0.3f, 1f, 0.3f, 1f)),
    YELLOW(Color(1f, 1f, 0.3f, 1f))
}

data class BulletType(
    val name: String,
    val speed: Float,
    val damage: Float,
    val radius: Float,
    val color: Color,
    val usesRemaining: Int,
    val reloadTime: Float,
    val rearrangeTime: Float,
    val behavior: BulletBehavior = BulletBehavior.NORMAL,
    val splashDamage: Float = 0f,
    val splashRadius: Float = 0f,
    val statusEffect: StatusEffectType? = null,
    val rarity: BulletRarity = BulletRarity.COMMON,
    val displayShape: BulletDisplayShape = BulletDisplayShape.CIRCLE,
    val element: Element? = null
) {
    fun createBullet(x: Float, y: Float, angle: Float): Bullet {
        return Bullet(
            x = x,
            y = y,
            angle = angle,
            speed = speed,
            damage = damage,
            radius = radius,
            color = color,
            behavior = behavior,
            splashDamage = splashDamage,
            splashRadius = splashRadius,
            statusEffect = statusEffect,
            element = element
        )
    }
    
    fun withDecrementedUse(): BulletType {
        return if (usesRemaining > 0) copy(usesRemaining = usesRemaining - 1) else this
    }
    
    val isInfinite: Boolean get() = usesRemaining == -1
    val isExhausted: Boolean get() = usesRemaining == 0
}

enum class StatusEffectType {
    BURNING,
    FROZEN
}

enum class BulletDisplayShape {
    CIRCLE,
    LONG,
    BIG_CIRCLE,
    FLAME,
    ICE,
    ELEMENTAL
}

object BulletTypes {
    val INFINITE = BulletType(
        name = "Infinite",
        speed = 350f,
        damage = 10f,
        radius = 5f,
        color = Color(0.9f, 0.9f, 0.5f, 1f),
        usesRemaining = -1,
        reloadTime = 0.15f,
        rearrangeTime = 0.05f,
        rarity = BulletRarity.SHOP_ONLY,
        displayShape = BulletDisplayShape.CIRCLE
    )
    
    val RIFLE = BulletType(
        name = "Rifle",
        speed = 800f,
        damage = 45f,
        radius = 4f,
        color = Color(0.8f, 0.7f, 0.3f, 1f),
        usesRemaining = 3,
        reloadTime = 0.8f,
        rearrangeTime = 0.4f,
        rarity = BulletRarity.COMMON,
        displayShape = BulletDisplayShape.LONG
    )
    
    val FIRE = BulletType(
        name = "Fire",
        speed = 300f,
        damage = 20f,
        radius = 6f,
        color = Color(1f, 0.4f, 0.1f, 1f),
        usesRemaining = 2,
        reloadTime = 0.5f,
        rearrangeTime = 0.2f,
        statusEffect = StatusEffectType.BURNING,
        rarity = BulletRarity.MEDIUM,
        displayShape = BulletDisplayShape.FLAME
    )
    
    val ICE = BulletType(
        name = "Ice",
        speed = 320f,
        damage = 18f,
        radius = 6f,
        color = Color(0.5f, 0.8f, 1f, 1f),
        usesRemaining = 4,
        reloadTime = 0.35f,
        rearrangeTime = 0.2f,
        statusEffect = StatusEffectType.FROZEN,
        rarity = BulletRarity.MEDIUM,
        displayShape = BulletDisplayShape.ICE
    )
    
    val BOMB = BulletType(
        name = "Bomb",
        speed = 0f,
        damage = 60f,
        radius = 150f,
        color = Color(0.9f, 0.2f, 0.2f, 1f),
        usesRemaining = 1,
        reloadTime = 1.2f,
        rearrangeTime = 0.5f,
        behavior = BulletBehavior.BOMB,
        rarity = BulletRarity.RARE,
        displayShape = BulletDisplayShape.BIG_CIRCLE
    )
    
    val GRENADE = BulletType(
        name = "Grenade",
        speed = 200f,
        damage = 40f,
        radius = 10f,
        color = Color(0.3f, 0.8f, 0.3f, 1f),
        usesRemaining = 2,
        reloadTime = 0.6f,
        rearrangeTime = 0.45f,
        behavior = BulletBehavior.GRENADE,
        splashDamage = 30f,
        splashRadius = 80f,
        rarity = BulletRarity.RARE,
        displayShape = BulletDisplayShape.BIG_CIRCLE
    )
    
    val RED_ELEMENTAL = BulletType(
        name = "Red Bolt",
        speed = 350f,
        damage = 15f,
        radius = 5f,
        color = Element.RED.color,
        usesRemaining = 4,
        reloadTime = 0.2f,
        rearrangeTime = 0.1f,
        rarity = BulletRarity.COMMON,
        displayShape = BulletDisplayShape.ELEMENTAL,
        element = Element.RED
    )
    
    val GREEN_ELEMENTAL = BulletType(
        name = "Green Bolt",
        speed = 350f,
        damage = 15f,
        radius = 5f,
        color = Element.GREEN.color,
        usesRemaining = 4,
        reloadTime = 0.2f,
        rearrangeTime = 0.1f,
        rarity = BulletRarity.COMMON,
        displayShape = BulletDisplayShape.ELEMENTAL,
        element = Element.GREEN
    )
    
    val YELLOW_ELEMENTAL = BulletType(
        name = "Yellow Bolt",
        speed = 350f,
        damage = 15f,
        radius = 5f,
        color = Element.YELLOW.color,
        usesRemaining = 4,
        reloadTime = 0.2f,
        rearrangeTime = 0.1f,
        rarity = BulletRarity.COMMON,
        displayShape = BulletDisplayShape.ELEMENTAL,
        element = Element.YELLOW
    )
    
    val ALL_DROPPABLE = listOf(RIFLE, FIRE, ICE, BOMB, GRENADE, RED_ELEMENTAL, GREEN_ELEMENTAL, YELLOW_ELEMENTAL)
    
    fun getRandomDrop(): BulletType? {
        val totalWeight = ALL_DROPPABLE.sumOf { it.rarity.dropWeight.toDouble() }.toFloat()
        var roll = Math.random().toFloat() * totalWeight
        
        for (type in ALL_DROPPABLE) {
            roll -= type.rarity.dropWeight
            if (roll <= 0) {
                return type.copy(usesRemaining = getRandomUses(type))
            }
        }
        return null
    }
    
    private fun getRandomUses(type: BulletType): Int {
        return when (type.name) {
            "Rifle" -> (2..4).random()
            "Fire" -> (1..2).random()
            "Ice" -> (3..5).random()
            "Bomb" -> 1
            "Grenade" -> (1..2).random()
            "Red Bolt", "Green Bolt", "Yellow Bolt" -> (3..5).random()
            else -> type.usesRemaining
        }
    }
}
