package dev.m4s4t0.voidslime.gl

import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.sim.Grab
import dev.m4s4t0.voidslime.sim.SphereCollider

/** What a finger does to the slime. SLAP to WAND are bought in the shop (model.Toy, same names). */
enum class Tool { NONE, POKE, STRETCH, SQUISH, ROLL, BUBBLE, STIR, SLAP, RAKE, STAMP, SHAKER, MAGNET, WAND }

/** Which screen the scene is on: Home spins slowly, the Lab stirs, Rush spawns bubbles to pop. */
enum class SceneMode { HOME, PLAY, LAB, RUSH }

/** Called on the main thread. */
interface SceneListener {
    fun onStat(stat: String) {}
    fun onStir(progress: Float) {}
    fun onRushPop(points: Int) {}
}

/** Theme colours for the room (RGB 0..1). */
class Look(val bg: FloatArray, val accent: FloatArray, val accent2: FloatArray, val light: Boolean) {
    companion object {
        val DEFAULT = Look(floatArrayOf(0.04f, 0.03f, 0.09f), floatArrayOf(0.66f, 0.44f, 1f), floatArrayOf(0.13f, 0.89f, 1f), false)
    }
}

/** One add-in inside (or on) the slime, pinned to a spot on the skin. */
internal class Item(
    val addIn: AddIn, val tri: Int, val u: Float, val v: Float, val depth: Float,
    val r: Float, val g: Float, val b: Float, val size: Float, val angle: Float, val spin: Float,
) {
    var x = 0f; var y = 0f; var z = 0f
    var lit = 1f
    /** 1 when settled; below that it's still falling in (the lab). */
    var drop = 1f
    var delay = 0f
    var lastCrunch = -9f
}

internal class Bubble(val seed: Float) {
    var x = 0f; var y = 0f; var z = 0f
    var r = 0.05f
    var vx = 0f; var vy = 0f
    var growing = true
    var tri = 0; var u = 0f; var v = 0f
    var age = 0f
    var gold = false
    var fade = 1f
    var lastBlow = 0f
}

internal class Particle {
    var x = 0f; var y = 0f; var z = 0f
    var vx = 0f; var vy = 0f; var vz = 0f
    var life = 0f; var max = 1f
    var r = 1f; var g = 1f; var b = 1f
    var size = 0.06f
    var shape = 9f
    var angle = 0f
    var gravity = 2.2f
    /** Falls onto the slime and stays there a while (the sprinkle shaker). */
    var lands = false
    var landed = false
}

/** A tool drawn as a ball: rake tines, the stamp, the magnet, the shaker. */
internal class Prop(val x: Float, val y: Float, val z: Float, val r: Float, val rgb: FloatArray)

internal enum class Kind { ORBIT, FINGER, GRAB, PALM, PIN, BUBBLE, STIR, RAKE, STAMP, SHAKER, MAGNET, DONE }

internal class Touch(val id: Int, var x: Float, var y: Float, val start: Float) {
    var kind = Kind.ORBIT
    var depth = 0f
    var press = 0f
    var finger: SphereCollider? = null
    var grab: Grab? = null
    var pull = 0f
    var tickAt = 0f
    var bubble: Bubble? = null
    var angle = 0f
    var turn = 0f
    var tickTurn = 0f
    var travel = 0f
    var rolled = 0f
    var flick = 0f
    var lastContacts = 0
    var clickAcc = 0f
    var px = 0f; var py = 0f; var pz = 0f
    var counted = false
    /** Rake tines and stamp points. */
    var balls: List<SphereCollider> = emptyList()
    /** The stamp's face height, the magnet's and shaker's height. */
    var level = 0f
    var clock = 0f
    var ticks = 0
}
