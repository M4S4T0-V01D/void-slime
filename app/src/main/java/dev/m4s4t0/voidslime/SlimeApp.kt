package dev.m4s4t0.voidslime

import android.app.Application
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.model.Game

class SlimeApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Themes.init(this)
        Game.init(this)
        Haptics.init(this)
        Audio.start()
    }
}
