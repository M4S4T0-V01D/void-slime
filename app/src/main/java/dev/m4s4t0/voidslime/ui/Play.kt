package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.gl.Tool
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidui.BarIcon
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar
import kotlinx.coroutines.delay

private val TOOLS = listOf(
    Triple(Tool.POKE, "👉", "Poke"),
    Triple(Tool.STRETCH, "🙌", "Stretch"),
    Triple(Tool.SQUISH, "🤲", "Squish"),
    Triple(Tool.ROLL, "🥖", "Roll"),
    Triple(Tool.BUBBLE, "🫧", "Bubbles"),
)

@Composable
fun PlayScreen(state: AppState) {
    val r = Game.current
    if (state.zen) {
        Box(Modifier.fillMaxSize()) {
            Box(Modifier.align(Alignment.TopEnd).statusBarsPadding().padding(8.dp).alpha(0.35f)) {
                BarIcon(Icons.Rounded.Close, "Leave zen", { state.zen = false })
            }
        }
        return
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxWidth()) {
            VoidTopBar(r.name, { state.go(Screen.HOME) }, subtitle = "${r.base.emoji} ${r.base.title} · ${r.base.blurb}") { CoinChip(Modifier.padding(end = 8.dp)) }
            Row(Modifier.padding(horizontal = 12.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SmallChip(if (state.lights) "💡 Lights" else "🌙 Lights off") {
                    state.lights = !state.lights
                    Audio.play(Snd.CLICK, 0.6f, 0.7f)
                    if (!state.lights && r.base == Base.GLOW) Game.add(Stat.LIGHTS_OFF)
                }
                SmallChip("🫳 Ball up") { state.view.scene { ballUp() } }
                SmallChip("🧘 Zen") { state.zen = true }
            }
        }
        BottomArea(Modifier.align(Alignment.BottomCenter)) {
            Row(
                Modifier.fillMaxWidth().height(150.dp).background(Void.bg.copy(alpha = 0.92f)).padding(horizontal = 8.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                for ((tool, emoji, name) in TOOLS) {
                    val on = state.tool == tool
                    Column(
                        Modifier.weight(1f).padding(horizontal = 3.dp).squishy { state.tool = tool }
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (on) Void.cyan.copy(alpha = 0.22f) else Void.panel.copy(alpha = 0.8f))
                            .border(if (on) 2.dp else 1.dp, if (on) Void.cyan else Void.border, RoundedCornerShape(20.dp))
                            .padding(vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(emoji, fontSize = 30.sp)
                        Text(name, color = if (on) Void.text else Void.dim, fontSize = 12.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                    }
                }
            }
        }
    }
}

@Composable
fun SmallChip(text: String, onClick: () -> Unit) {
    Text(
        text, color = Void.text, fontSize = 13.sp,
        modifier = Modifier.squishy(onClick).clip(RoundedCornerShape(50)).background(Void.panel.copy(alpha = 0.85f))
            .border(1.dp, Void.border, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 7.dp),
    )
}

private const val RUSH_SECONDS = 45

@Composable
fun RushScreen(state: AppState) {
    val rush = state.rush
    LaunchedEffect(rush.running) {
        if (!rush.running) return@LaunchedEffect
        while (rush.left > 0) {
            delay(1000)
            rush.left--
            if (rush.left in 1..5) Audio.play(Snd.TAP, 0.8f, 1.4f)
        }
        rush.running = false
        val score = rush.score
        rush.result = score
        Game.best(Stat.RUSH_BEST, score.toLong())
        Game.earn((score + 1) / 2, score)
        Audio.play(Snd.DONE, 0.9f)
        Haptics.buzz(Haptics.Kind.SUCCESS)
        state.celebrate()
    }
    Box(Modifier.fillMaxSize()) {
        VoidTopBar("Bubble rush", { rush.running = false; state.go(Screen.HOME) }, subtitle = if (rush.running) "Pop pop pop!" else "Best: ${Game.stats[Stat.RUSH_BEST] ?: 0}") {
            CoinChip(Modifier.padding(end = 8.dp))
        }
        if (rush.running) {
            Row(
                Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 70.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Big("🫧 ${rush.score}")
                Big("⏱️ ${rush.left}")
            }
        } else {
            Column(
                Modifier.align(Alignment.BottomCenter).padding(20.dp).padding(bottom = 30.dp).fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp)).background(Void.panel.copy(alpha = 0.94f)).border(1.dp, Void.border, RoundedCornerShape(24.dp)).padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                val res = rush.result
                if (res != null) {
                    Text("You popped $res!", color = Void.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("+${(res + 1) / 2} 🪙   Best: ${Game.stats[Stat.RUSH_BEST] ?: 0}", color = Void.dim, fontSize = 14.sp)
                } else {
                    Text("🫧 Bubble rush", color = Void.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("Pop as many bubbles as you can in $RUSH_SECONDS seconds. Golden ones are worth 5!", color = Void.dim, fontSize = 14.sp, textAlign = TextAlign.Center)
                }
                PillButton(if (res != null) "Again!" else "Start", true) {
                    tapFeedback()
                    rush.score = 0
                    rush.left = RUSH_SECONDS
                    rush.result = null
                    rush.running = true
                }
            }
        }
    }
}

@Composable
private fun Big(text: String) {
    Text(
        text, color = Void.text, fontSize = 26.sp, fontWeight = FontWeight.Bold,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(Void.panel.copy(alpha = 0.85f)).padding(horizontal = 18.dp, vertical = 8.dp),
    )
}
