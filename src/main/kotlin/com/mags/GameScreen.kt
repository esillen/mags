package com.mags

import com.badlogic.gdx.Gdx
import com.badlogic.gdx.Input
import com.badlogic.gdx.graphics.Color
import com.badlogic.gdx.graphics.OrthographicCamera
import com.badlogic.gdx.graphics.glutils.ShapeRenderer
import com.badlogic.gdx.math.Vector2
import com.badlogic.gdx.math.Vector3
import com.badlogic.gdx.utils.ScreenUtils
import ktx.app.KtxScreen

class GameScreen(private val game: MagsGame) : KtxScreen {
    private val worldCamera = OrthographicCamera()
    private val uiCamera = OrthographicCamera()
    private val shapeRenderer = ShapeRenderer()
    
    private val player = Player(0f, 0f)
    private val enemies = mutableListOf<Enemy>()
    private val bullets = mutableListOf<Bullet>()
    private val magazineManager = MagazineManager()
    private val magazineUI = MagazineUI()
    
    private var spawnTimer = 0f
    private val spawnInterval = 2f
    
    init {
        worldCamera.setToOrtho(false, 1280f, 720f)
        uiCamera.setToOrtho(false, 1280f, 720f)
        enemies.add(Enemy(300f, 300f))
    }
    
    override fun render(delta: Float) {
        handleInput(delta)
        update(delta)
        draw()
    }
    
    private fun handleInput(delta: Float) {
        var dx = 0f
        var dy = 0f
        
        if (Gdx.input.isKeyPressed(Input.Keys.W)) dy += 1f
        if (Gdx.input.isKeyPressed(Input.Keys.S)) dy -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.A)) dx -= 1f
        if (Gdx.input.isKeyPressed(Input.Keys.D)) dx += 1f
        
        player.move(dx, dy, delta)
        
        val mouseScreen = Vector3(Gdx.input.x.toFloat(), Gdx.input.y.toFloat(), 0f)
        worldCamera.unproject(mouseScreen)
        player.aimAt(mouseScreen.x, mouseScreen.y)
        
        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            val bullet = magazineManager.shoot(player.x, player.y, player.aimAngle)
            if (bullet != null) {
                bullets.add(bullet)
            }
        }
        
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_1)) magazineManager.selectMagazine(0)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_2)) magazineManager.selectMagazine(1)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_3)) magazineManager.selectMagazine(2)
        if (Gdx.input.isKeyJustPressed(Input.Keys.NUM_4)) magazineManager.selectMagazine(3)
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q)) magazineManager.cyclePrevious()
        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) magazineManager.cycleNext()
    }
    
    private fun update(delta: Float) {
        player.update(delta)
        
        enemies.forEach { it.update(delta, player.x, player.y) }
        enemies.removeAll { it.isDead }
        
        bullets.forEach { it.update(delta) }
        
        bullets.forEach { bullet ->
            enemies.forEach { enemy ->
                if (bullet.isAlive && enemy.collidesWith(bullet.x, bullet.y, bullet.radius)) {
                    enemy.takeDamage(bullet.damage)
                    bullet.onHit()
                }
            }
        }
        bullets.removeAll { !it.isAlive }
        
        spawnTimer += delta
        if (spawnTimer >= spawnInterval) {
            spawnTimer = 0f
            spawnEnemy()
        }
        
        worldCamera.position.set(player.x, player.y, 0f)
        worldCamera.update()
    }
    
    private fun spawnEnemy() {
        val angle = Math.random().toFloat() * 360f
        val distance = 400f + Math.random().toFloat() * 200f
        val x = player.x + kotlin.math.cos(Math.toRadians(angle.toDouble())).toFloat() * distance
        val y = player.y + kotlin.math.sin(Math.toRadians(angle.toDouble())).toFloat() * distance
        enemies.add(Enemy(x, y))
    }
    
    private fun draw() {
        ScreenUtils.clear(0.1f, 0.1f, 0.15f, 1f)
        
        shapeRenderer.projectionMatrix = worldCamera.combined
        
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled)
        
        drawGrid()
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
    }
}
