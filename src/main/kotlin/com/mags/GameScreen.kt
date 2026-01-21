package com.mags

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.GL20
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import com.mags.bullet.Bullet
import com.mags.bullet.BulletBehavior
import com.mags.bullet.BulletType
import ktx.app.KtxScreen
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

class GameScreen(private val game: MagsGame) : KtxScreen {
    private val worldCamera = OrthographicCamera()
    private val uiCamera = OrthographicCamera()
    private val shapeRenderer = ShapeRenderer()
    
    private val player = Player(0f, 0f)
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val droppedBullets = mutableListOf<DroppedBullet>()
    private val obstacles = mutableListOf<Obstacle>()
    private val impactEffects = mutableListOf<ImpactEffect>()
    private val magazineManager = MagazineManager()
    private val magazineUI = MagazineUI()
    private val visionSystem = VisionSystem()
    
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
        
        // Spawn some initial obstacles
        spawnInitialObstacles()
    }
    
    private fun spawnInitialObstacles() {
        val positions = listOf(
            Pair(200f, 200f), Pair(-150f, 250f), Pair(350f, -100f),
            Pair(-200f, -200f), Pair(100f, -300f), Pair(-350f, 50f),
            Pair(400f, 300f), Pair(-100f, 400f)
        )
        positions.forEach { (x, y) ->
            val width = 50f + (Math.random() * 60f).toFloat()
            val height = 50f + (Math.random() * 60f).toFloat()
            obstacles.add(Obstacle(x, y, width, height))
        }
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
        
        // Update vision
        visionSystem.updateVision(player.x, player.y, obstacles)
        
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
        player.move(dx, dy, realDelta)
        
        // Push player out of obstacles
        obstacles.forEach { obstacle ->
            obstacle.pushOutCircle(player.x, player.y, player.radius)?.let { (newX, newY) ->
                player.x = newX
                player.y = newY
            }
        }
        
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
        obstacles.forEach { obstacle ->
            val dx = obstacle.x - player.x
            val dy = obstacle.y - player.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < bomb.radius) {
                obstacle.takeDamage(bomb.damage)
            }
        }
    }
    
    private fun update(delta: Float) {
        player.update(Gdx.graphics.deltaTime)
        magazineManager.update(delta)
        
        enemies.forEach { enemy ->
            enemy.update(delta, player.x, player.y, obstacles)
        }
        
        val deadEnemies = enemies.filter { it.isDead }
        deadEnemies.forEach { enemy ->
            enemy.bulletDrop?.let { bulletType ->
                droppedBullets.add(DroppedBullet(enemy.x, enemy.y, bulletType))
            }
        }
        enemies.removeAll { it.isDead }
        
        bullets.forEach { it.update(delta) }
        
        // Bullet-obstacle collision
        bullets.forEach { bullet ->
            if (bullet.isAlive) {
                obstacles.forEach { obstacle ->
                    if (obstacle.collidesWithCircle(bullet.x, bullet.y, bullet.radius)) {
                        obstacle.takeDamage(bullet.damage)
                        impactEffects.add(ImpactEffect(bullet.x, bullet.y, bullet.color))
                        bullet.onHit()
                        
                        if (bullet.hasExploded && bullet.splashRadius > 0) {
                            applyGrenadeSplashToObstacles(bullet)
                        }
                    }
                }
            }
        }
        
        // Update impact effects
        impactEffects.forEach { it.update(Gdx.graphics.deltaTime) }
        impactEffects.removeAll { it.isFinished }
        
        // Bullet-enemy collision
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
            applyGrenadeSplashToObstacles(grenade)
        }
        bullets.removeAll { !it.isAlive }
        
        // Remove destroyed obstacles
        obstacles.removeAll { it.isDead }
        
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
    
    private fun applyGrenadeSplashToObstacles(grenade: Bullet) {
        obstacles.forEach { obstacle ->
            val dx = obstacle.x - grenade.x
            val dy = obstacle.y - grenade.y
            val dist = sqrt(dx * dx + dy * dy)
            if (dist < grenade.splashRadius) {
                obstacle.takeDamage(grenade.splashDamage)
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
        var x: Float
        var y: Float
        var attempts = 0
        
        do {
            val angle = Math.random().toFloat() * 360f
            val distance = 400f + Math.random().toFloat() * 200f
            x = player.x + cos(Math.toRadians(angle.toDouble())).toFloat() * distance
            y = player.y + sin(Math.toRadians(angle.toDouble())).toFloat() * distance
            attempts++
        } while (obstacles.any { it.collidesWithCircle(x, y, 25f) } && attempts < 10)
        
        if (attempts < 10) {
            val hasShield = Math.random() < 0.2
            enemies.add(Enemy(x, y, hasShield))
        }
    }
    
    private fun draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f)
        
        shapeRenderer.projectionMatrix = worldCamera.combined
        
        // Enable blending for transparency
        Gdx.gl.glEnable(GL20.GL_BLEND)
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA)
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        drawGrid()
        
        // Draw revealed but not currently visible obstacles (dimmed)
        obstacles.filter { it.hasBeenSeen && !isObstacleVisible(it) }.forEach { 
            it.drawRevealed(shapeRenderer) 
        }
        
        // Draw visible obstacles
        obstacles.filter { isObstacleVisible(it) }.forEach { it.draw(shapeRenderer) }
        
        // Draw visible dropped bullets
        droppedBullets.filter { isPointVisible(it.x, it.y) }.forEach { it.draw(shapeRenderer) }
        
        // Draw visible bullets
        bullets.filter { isPointVisible(it.x, it.y) }.forEach { it.draw(shapeRenderer) }
        
        // Draw impact effects (visible ones)
        impactEffects.filter { isPointVisible(it.x, it.y) }.forEach { it.draw(shapeRenderer) }
        
        // Draw visible enemies
        enemies.filter { isEnemyVisible(it) }.forEach { it.draw(shapeRenderer) }
        
        // Always draw player
        player.draw(shapeRenderer)
        
        shapeRenderer.end()
        
        // Draw fog of war
        drawFogOfWar()
        
        // Draw health bars for visible enemies
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line)
        enemies.filter { isEnemyVisible(it) }.forEach { it.drawHealthBar(shapeRenderer) }
        shapeRenderer.end()
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        enemies.filter { isEnemyVisible(it) }.forEach { it.drawHealthBarFill(shapeRenderer) }
        shapeRenderer.end()
        
        shapeRenderer.projectionMatrix = uiCamera.combined
        magazineUI.draw(shapeRenderer, magazineManager)
        
        drawTimeIndicator()
    }
    
    private fun isPointVisible(x: Float, y: Float): Boolean {
        return visionSystem.isPointVisible(player.x, player.y, x, y, obstacles)
    }
    
    private fun isEnemyVisible(enemy: Enemy): Boolean {
        return visionSystem.isCircleVisible(player.x, player.y, enemy.x, enemy.y, enemy.radius, obstacles)
    }
    
    private fun isObstacleVisible(obstacle: Obstacle): Boolean {
        // Check if any corner or center is visible
        val points = listOf(
            Pair(obstacle.x, obstacle.y),
            Pair(obstacle.left, obstacle.bottom),
            Pair(obstacle.right, obstacle.bottom),
            Pair(obstacle.left, obstacle.top),
            Pair(obstacle.right, obstacle.top)
        )
        return points.any { (px, py) -> isPointVisible(px, py) }
    }
    
    private fun drawFogOfWar() {
        val visiblePolygon = visionSystem.getVisiblePolygon()
        if (visiblePolygon.isEmpty()) return
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        shapeRenderer.color = Color(0.05f, 0.05f, 0.08f, 0.9f)
        
        // Draw fog triangles between rays to create shadow effect
        val playerX = player.x
        val playerY = player.y
        val farDist = 1500f
        
        for (i in visiblePolygon.indices) {
            val current = visiblePolygon[i]
            val next = visiblePolygon[(i + 1) % visiblePolygon.size]
            
            // Calculate direction to extend rays
            val dx1 = current.first - playerX
            val dy1 = current.second - playerY
            val len1 = sqrt(dx1 * dx1 + dy1 * dy1)
            
            val dx2 = next.first - playerX
            val dy2 = next.second - playerY
            val len2 = sqrt(dx2 * dx2 + dy2 * dy2)
            
            if (len1 > 0 && len2 > 0) {
                val farX1 = playerX + (dx1 / len1) * farDist
                val farY1 = playerY + (dy1 / len1) * farDist
                val farX2 = playerX + (dx2 / len2) * farDist
                val farY2 = playerY + (dy2 / len2) * farDist
                
                // Draw shadow quad from ray hit points to far distance
                shapeRenderer.triangle(
                    current.first, current.second,
                    farX1, farY1,
                    farX2, farY2
                )
                shapeRenderer.triangle(
                    current.first, current.second,
                    farX2, farY2,
                    next.first, next.second
                )
            }
        }
        
        shapeRenderer.end()
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
