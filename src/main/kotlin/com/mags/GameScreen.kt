package com.mags

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import com.mags.bullet.Bullet
import com.mags.bullet.BulletBehavior
import com.mags.bullet.BulletType
import ktx.app.KtxScreen
import kotlin.math.sqrt

class GameScreen(private val game: MagsGame) : KtxScreen {
    private val worldCamera = OrthographicCamera()
    private val uiCamera = OrthographicCamera()
    private val shapeRenderer = ShapeRenderer()
    
    private val player = Player(0f, 0f)
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val droppedBullets = mutableListOf<DroppedBullet>()
    private val magazineManager = MagazineManager()
    private val magazineUI = MagazineUI()
    
    private var spawnTimer = 0f
    private val spawnInterval = 2f
    
    private val minTimeScale = 0.02f
    private val maxTimeScale = 1f
    private var currentTimeScale = minTimeScale
    private val timeScaleLerpSpeed = 12f
    
    init {
        worldCamera.setToOrtho(false, 1280f, 720f)
        uiCamera.setToOrtho(false, 1280f, 720f)
        enemies.add(Enemy(300f, 300f))
    }
    
    override fun render(delta: Float) {
        val (isMoving, dx, dy) = getMovementInput()
        val timeKeyPressed = Gdx.input.isKeyPressed(Input.Keys.SPACE)
        
        val targetTimeScale = if (isMoving || timeKeyPressed) maxTimeScale else minTimeScale
        currentTimeScale += (targetTimeScale - currentTimeScale) * timeScaleLerpSpeed * delta
        currentTimeScale = currentTimeScale.coerceIn(minTimeScale, maxTimeScale)
        
        val worldDelta = delta * currentTimeScale
        
        handleInput(delta, worldDelta, dx, dy)
        update(worldDelta)
        draw()
    }
    
    private fun getMovementInput(): Triple<Boolean, Float, Float> {
        var dx = 0f
        var dy = 0f
        
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f
        
        val isMoving = dx != 0f || dy != 0f
        return Triple(isMoving, dx, dy)
    }
    
    private fun handleInput(realDelta: Float, @Suppress("UNUSED_PARAMETER") worldDelta: Float, dx: Float, dy: Float) {
        // Player moves at real time speed for responsive controls
        player.move(dx, dy, realDelta)
        
        val mouseScreen = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        worldCamera.unproject(mouseScreen)
        player.aimAt(mouseScreen.x, mouseScreen.y)
        
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            val bulletType = magazineManager.selectedMagazine.peekTop()
            if (bulletType != null && bulletType.behavior == BulletBehavior.BOMB) {
                if (magazineManager.selectedMagazine.canShoot) {
                    val bomb = magazineManager.shoot(player.x, player.y, player.aimAngle)
                    if (bomb != null) {
                        player.triggerMuzzleFlash()
                        applyBombDamage(bomb)
                    }
                }
            } else {
                val bullet = magazineManager.shoot(player.gunTipX, player.gunTipY, player.aimAngle)
                if (bullet != null) {
                    player.triggerMuzzleFlash()
                    bullets.add(bullet)
                }
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            magazineManager.rearrange()
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) magazineManager.selectMagazine(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) magazineManager.selectMagazine(1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) magazineManager.selectMagazine(2)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) magazineManager.selectMagazine(3)
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) magazineManager.cyclePrevious()
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) magazineManager.cycleNext()
    }
    
    private fun applyBombDamage(bomb: Bullet) {
        enemies.forEach { enemy ->
            val dx = enemy.x - player.x
            val dy = enemy.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bomb.radius) {
                enemy.takeDamage(bomb.damage, bomb.statusEffect, bomb.element)
            }
        }
    }
    
    private fun update(delta: Float) {
        player.update(Gdx.graphics.deltaTime)
        magazineManager.update(delta)
        
        enemies.forEach { it.update(delta, player.x, player.y) }
        
        val deadEnemies = enemies.filter { it.isDead }
        deadEnemies.forEach { enemy ->
            enemy.bulletDrop?.let { bulletType ->
                droppedBullets.add(DroppedBullet(enemy.x, enemy.y, bulletType))
            }
        }
        enemies.removeAll { it.isDead }
        
        bullets.forEach { it.update(delta) }
        
        bullets.forEach { bullet ->
            if (bullet.isAlive) {
                enemies.forEach { enemy ->
                    if (bullet.isAlive && enemy.collidesWith(bullet.x, bullet.y, bullet.radius)) {
                        enemy.takeDamage(bullet.damage, bullet.statusEffect, bullet.element)
                        bullet.onHit()
                        
                        if (bullet.hasExploded && bullet.splashRadius > 0) {
                            applyGrenadeSplash(bullet)
                        }
                    }
                }
            }
        }
        
        val explodedGrenades = bullets.filter { !it.isAlive && it.hasExploded && it.splashRadius > 0 }
        explodedGrenades.forEach { grenade ->
            applyGrenadeSplash(grenade)
        }
        bullets.removeAll { !it.isAlive }
        
        pickupDroppedBullets()
        
        spawnTimer += delta
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnEnemy()
        }
        
        worldCamera.position.set(player.x, player.y, 0f)
        worldCamera.update()
    }
    
    private fun applyGrenadeSplash(grenade: Bullet) {
        enemies.forEach { enemy ->
            val dx = enemy.x - grenade.x
            val dy = enemy.y - grenade.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < grenade.splashRadius) {
                enemy.takeDamage(grenade.splashDamage, grenade.statusEffect, grenade.element)
            }
        }
    }
    
    private fun pickupDroppedBullets() {
        val pickupRadius = 50f
        val toRemove = mutableListOf<DroppedBullet>()
        
        droppedBullets.forEach { dropped ->
            val dx = dropped.x - player.x
            val dy = dropped.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < pickupRadius) {
                if (magazineManager.addBulletToFirstAvailable(dropped.bulletType)) {
                    toRemove.add(dropped)
                }
            }
        }
        droppedBullets.removeAll(toRemove)
    }
    
    private fun spawnEnemy() {
        val angle = Math.random().toFloat() * 360f
        val distance = 400f + Math.random().toFloat() * 200f
        val x = player.x + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance
        val y = player.y + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance
        val hasShield = Math.random() < 0.2
        enemies.add(Enemy(x, y, hasShield))
    }
    
    private fun draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f)
        
        shapeRenderer.projectionMatrix = worldCamera.combined
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        drawGrid()
        droppedBullets.forEach { it.draw(shapeRenderer) }
        bullets.forEach { it.draw(shapeRenderer) }
        enemies.forEach { it.draw(shapeRenderer) }
        player.draw(shapeRenderer)
        
        shapeRenderer.end()
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        enemies.forEach { it.drawHealthBar(shapeRenderer) }
        shapeRenderer.end()
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        enemies.forEach { it.drawHealthBarFill(shapeRenderer) }
        shapeRenderer.end()
        
        shapeRenderer.projectionMatrix = uiCamera.combined
        magazineUI.draw(shapeRenderer, magazineManager)
        
        drawTimeIndicator()
    }
    
    private fun drawTimeIndicator() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        val barWidth = 100f
        val barHeight = 4f
        val barX = 1280f - barWidth - 20f
        val barY = 720f - 20f
        
        shapeRenderer.color = Color(0.2f, 0.2f, 0.25f, 0.8f)
        shapeRenderer.rect(barX, barY, barWidth, barHeight)
        
        val timeColor = Color(
            0.3f + 0.7f * currentTimeScale,
            0.5f + 0.5f * currentTimeScale,
            0.9f,
            1f
        )
        shapeRenderer.color = timeColor
        shapeRenderer.rect(barX, barY, barWidth * currentTimeScale, barHeight)
        
        shapeRenderer.end()
    }
    
    private fun drawGrid() {
        shapeRenderer.color = Color(0.15f, 0.15f, 0.2f, 1f)
        val gridSize = 100f
        val startX = ((worldCamera.position.x - 700) / gridSize).toInt() * gridSize.toInt()
        val startY = ((worldCamera.position.y - 400) / gridSize).toInt() * gridSize.toInt()
        
        for (x in startX..(startX + 1500) step gridSize.toInt()) {
            shapeRenderer.rectLine(x.toFloat(), startY.toFloat(), x.toFloat(), startY + 900f, 1f)
        }
        for (y in startY..(startY + 900) step gridSize.toInt()) {
            shapeRenderer.rectLine(startX.toFloat(), y.toFloat(), startX + 1500f, y.toFloat(), 1f)
        }
    }
    
    override fun dispose() {
        shapeRenderer.dispose()
        magazineUI.dispose()
    }
}

class DroppedBullet(
    val x: Float,
    val y: Float,
    val bulletType: BulletType
) {
    private var bobOffset = 0f
    private var bobTimer = Math.random().toFloat() * 6.28f
    
    fun draw(renderer: ShapeRenderer) {
        bobTimer += Gdx.graphics.deltaTime * 3f
        bobOffset = kotlin.math.sin(bobTimer) * 3f
        
        renderer.color = Color(0.2f, 0.2f, 0.25f, 0.8f)
        renderer.circle(x, y + bobOffset, 14f)
        
        renderer.color = bulletType.color
        renderer.circle(x, y + bobOffset, 10f)
        
        renderer.color = Color(1f, 1f, 1f, 0.3f)
        renderer.circle(x - 3f, y + bobOffset + 3f, 3f)
    }
}
