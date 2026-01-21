package com.mags

import ktx.app.KtxGame
import com.badlogic.gdx.Screen

class MagsGame : KtxGame<Screen>() {
    override fun create() {
        addScreen(GameScreen(this))
        setScreen<GameScreen>()
    }
}
