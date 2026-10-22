package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import dev.m4s4t0.voidslime.model.Toy
import dev.m4s4t0.voidslime.model.key
import dev.m4s4t0.voidui.BarIcon
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar
import kotlinx.coroutines.delay

/** The tool rail: the free tools, then the toys from the shop (locked ones show their price). */
private class ToolSlot(val tool: Tool, val name: String, val toy: Toy? = null) {
    val icon get() = "tool_" + tool.name.lowercase()
}

private val TOOLS = listOf(
    ToolSlot(Tool.POKE, "Poke"),
    ToolSlot(Tool.STRETCH, "Stretch"),
    ToolSlot(Tool.SQUISH, "Squish"),
    ToolSlot(Tool.ROLL, "Roll"),
    ToolSlot(Tool.BUBBLE, "Bubbles"),
) + Toy.entries.map { ToolSlot(Tool.valueOf(it.name), it.title.substringAfterLast(' ').replaceFirstChar { c -> c.uppercase() }, it) }

@Composable
fun PlayScreen(state: AppState) {
    val r = Game.current
    var buying by remember { mutableStateOf<String?>(null) }
    buying?.let { k ->
        BuyDialog(k) {
            buying = null
            if (Game.isUnlocked(k)) TOOLS.firstOrNull { it.toy?.let(::key) == k }?.let { state.tool = it.tool }
        }
    }
    if (state.zen) {
        Box(Modifier.fillMaxSize().safe()) {
            RoundIcon("zen", Modifier.align(Alignment.TopEnd).padding(10.dp).alpha(0.4f), size = 40.dp) { state.zen = false }
        }
        return
    }
    Box(Modifier.fillMaxSize().safe()) {
        GameBar(r.name, { state.go(Screen.HOME) }, Modifier.padding(end = PLAY_RAIL.dp), icon = r.base.icon, subtitle = "${r.base.title} · ${r.base.blurb}") {
            RoundIcon(if (state.lights) "bulb" else "moon", size = 44.dp, on = !state.lights) {
                state.lights = !state.lights
                Audio.play(Snd.CLICK, 0.6f, 0.7f)
                if (!state.lights && r.base == Base.GLOW) Game.add(Stat.LIGHTS_OFF)
            }
            RoundIcon("ballup", size = 44.dp) { state.view.scene { ballUp() } }
            RoundIcon("zen", size = 44.dp) { state.zen = true }
            CoinChip()
        }
        Column(
            Modifier.align(Alignment.CenterEnd).width(PLAY_RAIL.dp).fillMaxHeight().padding(vertical = 8.dp, horizontal = 8.dp)
                .glass(RoundedCornerShape(26.dp), alpha = 0.78f).verticalScroll(rememberScrollState()).padding(vertical = 8.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            for (slot in TOOLS) {
                val on = state.tool == slot.tool
                val locked = slot.toy != null && !Game.isUnlocked(key(slot.toy))
                Column(
                    Modifier.fillMaxWidth().squishy { if (locked) buying = key(slot.toy!!) else state.tool = slot.tool }
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (on) Void.cyan.copy(alpha = 0.25f) else Void.panelAlt.copy(alpha = 0.5f))
                        .border(if (on) 2.dp else 1.dp, if (on) Void.cyan else if (slot.toy != null && !locked) Void.violet else Void.border, RoundedCornerShape(18.dp))
                        .padding(vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(contentAlignment = Alignment.BottomEnd) {
                        Pic(slot.icon, 46.dp, if (on) Modifier.bob(2.dp) else Modifier, alpha = if (locked) 0.5f else 1f)
                        if (locked) Pic("lock", 20.dp)
                    }
                    if (locked) Coins(slot.toy!!.price, fontSize = 11.sp, icon = 13.dp)
                    else Text(slot.name, color = if (on) Void.text else Void.dim, fontSize = 11.sp, maxLines = 1, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }
    }
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
        Game.add(Stat.RUSHES)
        Game.earn((score + 1) / 2, score)
        Audio.play(Snd.DONE, 0.9f)
        Haptics.buzz(Haptics.Kind.SUCCESS)
        state.celebrate()
    }
    Box(Modifier.fillMaxSize().safe()) {
        GameBar("Bubble rush", { rush.running = false; state.go(Screen.HOME) }, icon = "bubbles", subtitle = if (rush.running) "Pop pop pop!" else "Best: ${Game.stats[Stat.RUSH_BEST] ?: 0}") {
            CoinChip()
        }
        if (rush.running) {
            Row(
                Modifier.align(Alignment.TopCenter).padding(top = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                Big("bubbles", "${rush.score}")
                Big("timer", "${rush.left}")
            }
        } else {
            Row(
                Modifier.align(Alignment.BottomCenter).padding(bottom = 14.dp).widthIn(max = 520.dp)
                    .glass(RoundedCornerShape(26.dp), alpha = 0.94f).padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                val res = rush.result
                Pic(if (res != null) "trophy" else "bubbles", 72.dp, Modifier.bob(3.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (res != null) {
                        Text("You popped $res!", color = Void.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Coins("+${(res + 1) / 2}", fontSize = 15.sp)
                            Text("Best: ${Game.stats[Stat.RUSH_BEST] ?: 0}", color = Void.dim, fontSize = 14.sp)
                        }
                    } else {
                        Text("Bubble rush", color = Void.text, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                        Text("Pop as many bubbles as you can in $RUSH_SECONDS seconds. Golden ones are worth 5!", color = Void.dim, fontSize = 13.sp)
                    }
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
private fun Big(icon: String, text: String) {
    Row(
        Modifier.glass(RoundedCornerShape(50), alpha = 0.88f).padding(start = 8.dp, end = 18.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Pic(icon, 40.dp)
        Text(text, color = Void.text, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    }
}
