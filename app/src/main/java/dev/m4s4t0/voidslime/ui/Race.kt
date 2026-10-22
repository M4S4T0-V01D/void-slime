package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.model.Cup
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.RIVALS
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

private const val LANES = 4
private const val HOP = 1.6f
private const val MAX_SPEED = 7f

/** One slime on the track. Distances are in lane-heights. */
private class Racer(val name: String, val color: Color, val you: Boolean, var lane: Int, val pace: Float) {
    var x = 0f
    var v = 0f
    /** Hop animation, 1 just hopped, falling to 0. */
    var hop = 0f
    var nextHop = 0f
    var boosted = 0f
    var stuck = false
    var finished = -1f
    /** Lane it's sliding to (drawn smoothly). */
    var laneDrawn = lane.toFloat()
}

/** A puddle (slows you down) or a boost pad (zoom) in a lane. */
private class Pad(val lane: Int, val x: Float, val len: Float, val boost: Boolean) {
    val used = HashSet<Racer>()
}

private class RaceRun(val cup: Cup, slime: Color, rnd: Random) {
    val racers: List<Racer>
    val pads = ArrayList<Pad>()
    var clock = -3f
    var done = false
    var place = -1
    val order = ArrayList<Racer>()

    init {
        val rivals = RIVALS.shuffled(rnd).take(LANES - 1)
        val youLane = 1 + rnd.nextInt(2)
        val lanes = (0 until LANES).filter { it != youLane }.shuffled(rnd)
        racers = listOf(Racer("You", slime, true, youLane, 1f)) +
            rivals.mapIndexed { i, (n, c) -> Racer(n, argbColor(c), false, lanes[i], cup.rivals * (0.92f + rnd.nextFloat() * 0.12f)) }
        var x = 8f
        while (x < cup.length - 4f) {
            val boost = rnd.nextFloat() < cup.boosts / (cup.boosts + cup.puddles)
            pads += Pad(rnd.nextInt(LANES), x, if (boost) 1.2f else 1.6f + rnd.nextFloat() * 1.2f, boost)
            x += (0.5f + rnd.nextFloat()) / (cup.boosts + cup.puddles) / 2f
        }
    }

    val you get() = racers.first()

    fun hop(r: Racer) {
        if (r.finished >= 0f || clock < 0f) return
        r.v = min(MAX_SPEED, r.v + HOP * (if (r.stuck) 0.45f else 1f))
        r.hop = 1f
    }

    fun step(dt: Float, rnd: Random, sound: (Racer, Snd) -> Unit) {
        val before = clock
        clock += dt
        if (before < 0f && clock >= 0f) sound(you, Snd.BELL)
        else if (before < -1f && clock >= -1f || before < -2f && clock >= -2f) sound(you, Snd.TAP)
        if (clock < 0f) return
        for (r in racers) {
            r.laneDrawn += (r.lane - r.laneDrawn) * min(1f, dt * 12f)
            r.hop = max(0f, r.hop - dt * 3.2f)
            if (r.finished >= 0f) { r.v *= exp(-3f * dt); r.x += r.v * dt; continue }
            if (!r.you) {
                r.nextHop -= dt
                if (r.nextHop <= 0f) {
                    hop(r)
                    r.nextHop = (0.2f + rnd.nextFloat() * 0.12f) / r.pace
                    // Rivals dodge puddles ahead (sometimes).
                    val ahead = pads.firstOrNull { !it.boost && it.lane == r.lane && it.x > r.x && it.x - r.x < 3f }
                    if (ahead != null && rnd.nextFloat() < 0.35f * r.pace) {
                        val to = (r.lane + if (rnd.nextBoolean()) 1 else -1).coerceIn(0, LANES - 1)
                        if (racers.none { it !== r && it.lane == to && abs(it.x - r.x) < 0.8f }) r.lane = to
                    }
                }
            }
            r.stuck = false
            for (p in pads) {
                if (p.lane != r.lane || r.x < p.x || r.x > p.x + p.len) continue
                if (p.boost) {
                    if (p.used.add(r)) { r.v = min(MAX_SPEED * 1.3f, r.v + 5f); r.boosted = 1f; sound(r, Snd.WHOOSH) }
                } else {
                    r.stuck = true
                    if (p.used.add(r)) sound(r, Snd.SQUISH)
                }
            }
            r.boosted = max(0f, r.boosted - dt * 1.5f)
            r.v *= exp(-(if (r.stuck) 4.5f else 1.4f) * dt)
            r.x += r.v * dt
            if (r.x >= cup.length) {
                r.finished = clock
                order += r
                if (r.you) { place = order.size - 1; sound(r, Snd.DONE) }
            }
        }
        if (you.finished >= 0f && !done) {
            // Everyone else still running finishes in the order they're in.
            racers.filter { it.finished < 0f }.sortedByDescending { it.x }.forEach { it.finished = clock + 1f; order += it }
            done = true
        }
    }

    fun changeLane(d: Int) {
        val r = you
        if (r.finished >= 0f || clock < 0f) return
        r.lane = (r.lane + d).coerceIn(0, LANES - 1)
    }
}

@Composable
fun RaceScreen(state: AppState) {
    var cup by remember { mutableStateOf<Cup?>(null) }
    var result by remember { mutableStateOf<Pair<Cup, Int>?>(null) }
    var prize by remember { mutableIntStateOf(0) }
    val c = cup
    Box(Modifier.fillMaxSize()) {
        if (c != null) {
            RaceTrack(c) { place ->
                prize = Game.finishRace(c, place)
                result = c to place
                cup = null
                if (place == 0) state.toasts += Toast(System.nanoTime(), "trophy", "You won the ${c.title}!", "", coins = prize)
            }
            GameBar(c.title, { cup = null }, Modifier.safe(), icon = "race", subtitle = "Tap to hop · swipe up or down to change lane")
        } else {
            Column(Modifier.fillMaxSize().safe()) {
                GameBar("Slime racing", { state.go(Screen.HOME) }, icon = "race", subtitle = "Hop your slime to the finish! Dodge the puddles, hit the boosts") { CoinChip() }
                result?.let { (rc, place) ->
                    Row(
                        Modifier.align(Alignment.CenterHorizontally).padding(8.dp).widthIn(max = 560.dp).glass(RoundedCornerShape(24.dp), alpha = 0.92f).padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Pic(if (place == 0) "trophy" else "flag", 64.dp, Modifier.bob(3.dp))
                        Column(Modifier.weight(1f)) {
                            Text(listOf("1st place!", "2nd place!", "3rd place", "4th place")[place], color = Void.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                            Text(if (place == 0) "Champion of the ${rc.title}!" else "So close! Hop faster and dodge those puddles.", color = Void.dim, fontSize = 13.sp)
                            Coins("+$prize", fontSize = 15.sp)
                        }
                        PillButton("Again!", true) { tapFeedback(); result = null; cup = rc }
                    }
                }
                Row(
                    Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    for (k in Cup.entries) CupCard(k) { tapFeedback(); result = null; cup = k }
                }
            }
        }
    }
}

@Composable
private fun CupCard(c: Cup, onGo: () -> Unit) {
    val open = Game.level >= c.level
    Column(
        Modifier.width(230.dp).glass(RoundedCornerShape(24.dp), if (open) Void.cyan.copy(alpha = 0.6f) else Void.border, 0.9f).padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Pic(c.world.icon, 96.dp, alpha = if (open) 1f else 0.5f)
            if (!open) Pic("lock", 26.dp)
        }
        Text(c.title, color = Void.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
        Text(c.blurb, color = Void.dim, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Pic("trophy", 22.dp)
            Coins(c.prizes[0], fontSize = 13.sp, icon = 15.dp)
            Text("2nd ${c.prizes[1]} · 3rd ${c.prizes[2]}", color = Void.dim, fontSize = 11.sp)
        }
        if (open) PillButton("Race!", true, onClick = onGo) else LevelTag(c.level, fontSize = 14.sp)
    }
}

@Composable
private fun RaceTrack(cup: Cup, onDone: (Int) -> Unit) {
    val rnd = remember { Random(System.nanoTime()) }
    val slime = remember { argbColor(colorOf(Game.current.colors.first()).argb) }
    val run = remember(cup) { RaceRun(cup, slime, rnd) }
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(run) {
        var last = 0L
        var after = 0f
        while (true) {
            val now = withFrameNanos { it }
            val dt = if (last == 0L) 0f else ((now - last) / 1e9f).coerceAtMost(0.05f)
            last = now
            run.step(dt, rnd) { r, s ->
                if (r.you || s == Snd.BELL) {
                    Audio.play(s, if (s == Snd.SQUISH) 0.6f else 0.8f, if (s == Snd.TAP) 1.4f else 1f)
                    if (s == Snd.SQUISH) Haptics.buzz(Haptics.Kind.THUD, 0.5f)
                    if (s == Snd.WHOOSH) Haptics.buzz(Haptics.Kind.TICK, 0.8f)
                }
            }
            frame++
            if (run.done) {
                after += dt
                if (after > 1.6f) { onDone(run.place.coerceAtLeast(0)); break }
            }
        }
    }
    val sky = Brush.verticalGradient(listOf(argbColor(cup.world.skyTop), argbColor(cup.world.skyBottom)))
    Canvas(
        Modifier.fillMaxSize().pointerInput(run) {
            val swipe = 36.dp.toPx()
            awaitEachGesture {
                val down = awaitFirstDown()
                run.hop(run.you)
                if (run.clock >= 0f) { Audio.play(Snd.PLIP, 0.35f, 0.9f + rnd.nextFloat() * 0.3f, 0f, 40); Haptics.buzz(Haptics.Kind.TICK, 0.25f) }
                var moved = false
                while (true) {
                    val e = awaitPointerEvent()
                    val p = e.changes.firstOrNull { it.id == down.id } ?: break
                    if (!p.pressed) break
                    val dy = p.position.y - down.position.y
                    if (!moved && abs(dy) > swipe) {
                        moved = true
                        run.changeLane(if (dy > 0) 1 else -1)
                        Audio.play(Snd.SWISH, 0.4f, 1.3f)
                    }
                }
            }
        },
    ) {
        @Suppress("UNUSED_EXPRESSION") frame
        drawRect(sky)
        drawTrack(run)
    }
    Box(Modifier.fillMaxSize().safe()) {
        val t = run.clock
        if (t < 0.6f) {
            val txt = if (t < 0f) "${(-t).toInt() + 1}" else "GO!"
            Text(txt, color = Color.White, fontSize = 72.sp, fontWeight = FontWeight.Black, modifier = Modifier.align(Alignment.Center))
        }
        val ahead = run.racers.count { it.x > run.you.x && !it.you }
        if (t >= 0f) {
            Row(Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp).glass(RoundedCornerShape(50), alpha = 0.85f).padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(listOf("1st", "2nd", "3rd", "4th")[if (run.place >= 0) run.place else ahead], color = Void.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text("${(run.you.x / cup.length * 100).toInt().coerceIn(0, 100)}%", color = Void.dim, fontSize = 16.sp)
            }
        }
    }
}

private fun DrawScope.drawTrack(run: RaceRun) {
    val top = size.height * 0.24f
    val laneH = (size.height - top - size.height * 0.1f) / LANES
    val unit = laneH
    // The camera keeps your slime a third of the way across.
    val camX = run.you.x * unit - size.width * 0.33f
    fun sx(x: Float) = x * unit - camX
    fun ly(l: Float) = top + (l + 0.5f) * laneH
    for (l in 0 until LANES) {
        drawRoundRect(
            if (l % 2 == 0) Color(0xFFFFF3FB).copy(alpha = 0.55f) else Color(0xFFE9F6FF).copy(alpha = 0.55f),
            Offset(sx(-2f), top + l * laneH + 3f), Size((run.cup.length + 8f) * unit, laneH - 6f), CornerRadius(laneH * 0.4f),
        )
    }
    // Distance marks, start and finish.
    var m = 0f
    while (m < run.cup.length) {
        val x = sx(m)
        if (x > -10f && x < size.width + 10f) drawLine(Color.White.copy(alpha = 0.35f), Offset(x, top), Offset(x, top + LANES * laneH), 2f)
        m += 5f
    }
    val fx = sx(run.cup.length)
    val sq = laneH / 4f
    for (i in 0 until LANES * 4) for (j in 0 until 2) {
        drawRect(if ((i + j) % 2 == 0) Color.White else Color(0xFF222233), Offset(fx + j * sq, top + i * sq), Size(sq, sq))
    }
    for (p in run.pads) {
        val x0 = sx(p.x); val x1 = sx(p.x + p.len)
        if (x1 < 0f || x0 > size.width) continue
        val cy = ly(p.lane.toFloat())
        if (p.boost) {
            drawRoundRect(Color(0xFFFFE066), Offset(x0, cy - laneH * 0.3f), Size(x1 - x0, laneH * 0.6f), CornerRadius(laneH * 0.2f))
            for (k in 0 until 3) {
                val ax = x0 + (x1 - x0) * (0.2f + k * 0.25f)
                drawLine(Color(0xFFFF8A3D), Offset(ax, cy - laneH * 0.18f), Offset(ax + laneH * 0.15f, cy), 6f)
                drawLine(Color(0xFFFF8A3D), Offset(ax + laneH * 0.15f, cy), Offset(ax, cy + laneH * 0.18f), 6f)
            }
        } else {
            drawOval(Color(0xFF8E5BD8).copy(alpha = 0.75f), Offset(x0, cy - laneH * 0.28f), Size(x1 - x0, laneH * 0.56f))
            drawOval(Color.White.copy(alpha = 0.3f), Offset(x0 + (x1 - x0) * 0.2f, cy - laneH * 0.2f), Size((x1 - x0) * 0.3f, laneH * 0.12f))
        }
    }
    // Back to front so the lower lanes overlap the upper ones.
    for (r in run.racers.sortedBy { it.laneDrawn }) {
        val x = sx(r.x)
        if (x < -laneH || x > size.width + laneH) continue
        val base = ly(r.laneDrawn) + laneH * 0.22f
        val air = sin(r.hop * Math.PI.toFloat()) * laneH * 0.35f
        val squash = if (r.hop < 0.15f && r.hop > 0f) 1.25f else if (r.stuck) 1.3f else 1f - r.hop * 0.15f
        val w = laneH * 0.62f * squash
        val h = laneH * 0.5f / squash
        drawOval(Color.Black.copy(alpha = 0.18f), Offset(x - w * 0.5f, base - laneH * 0.05f), Size(w, laneH * 0.1f))
        val y = base - air - h
        drawOval(r.color, Offset(x - w / 2f, y), Size(w, h))
        drawOval(Color.White.copy(alpha = 0.45f), Offset(x - w * 0.28f, y + h * 0.12f), Size(w * 0.25f, h * 0.18f))
        if (r.boosted > 0f) {
            for (k in 1..3) drawLine(Color(0xFFFFE066).copy(alpha = r.boosted), Offset(x - w * 0.5f - k * 12f, y + h * (0.3f + k * 0.15f)), Offset(x - w * 0.5f - k * 12f - 24f, y + h * (0.3f + k * 0.15f)), 5f)
        }
        // Eyes, looking ahead.
        val er = h * 0.09f
        for (s in listOf(0.08f, 0.26f)) {
            drawCircle(Color.White, er * 1.4f, Offset(x + w * s, y + h * 0.42f))
            drawCircle(Color(0xFF1B1530), er, Offset(x + w * s + er * 0.3f, y + h * 0.43f))
        }
        if (r.you) {
            drawCircle(Color.White, laneH * 0.08f, Offset(x, y - laneH * 0.14f))
            drawCircle(Void.cyan, laneH * 0.055f, Offset(x, y - laneH * 0.14f))
        }
    }
}
