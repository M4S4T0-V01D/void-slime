package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.gl.Tool
import dev.m4s4t0.voidslime.model.ADD_IN_LEVELS
import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.COLORS
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Names
import dev.m4s4t0.voidslime.model.Orders
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidslime.model.key
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar

enum class LabStep(val title: String) { BASE("Base"), COLOR("Colour"), MIXINS("Mix-ins"), STIR("Activate"), NAME("Jar it") }

class LabState {
    var draft by mutableStateOf(blank())
    var step by mutableStateOf(LabStep.BASE)
    var stir by mutableStateOf(0f)
    var activated by mutableStateOf(false)
    var saved by mutableStateOf(false)

    val tool get() = if (step == LabStep.STIR && activated) Tool.STIR else Tool.POKE

    fun startNew() {
        draft = blank()
        step = LabStep.BASE
        stir = 0f
        activated = false
        saved = false
    }

    private fun blank() = Recipe(id = Recipe.newId(), name = "", base = Base.GLOSSY, colors = listOf(COLORS.first().id), created = System.currentTimeMillis())
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun LabScreen(state: AppState) {
    val lab = state.lab
    val r = lab.draft
    var buying by remember { mutableStateOf<String?>(null) }
    val pinned = Game.orders.firstOrNull { it.id == state.pinnedOrder }

    LaunchedEffect(r.id) { val f = lab.stir; state.view.scene { setForm(f) } }
    LaunchedEffect(lab.stir >= 1f) {
        if (lab.stir >= 1f && lab.step == LabStep.STIR) {
            Audio.play(Snd.DONE, 0.9f)
            Haptics.buzz(Haptics.Kind.SUCCESS)
            state.celebrate()
            if (r.name.isEmpty()) lab.draft = r.copy(name = Names.suggest(r))
            lab.step = LabStep.NAME
        }
    }

    /** Tapping something: use it if unlocked, otherwise offer to buy it. */
    fun pick(k: String, use: () -> Unit) {
        if (Game.isUnlocked(k)) use() else buying = k
    }

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val top = maxHeight * 0.44f
        Column(Modifier.fillMaxWidth()) {
            VoidTopBar("Slime lab", { state.go(Screen.HOME) }, subtitle = if (lab.step == LabStep.STIR && lab.activated) "Stir in circles!" else "Step ${lab.step.ordinal + 1} of 5") {
                CoinChip(Modifier.padding(end = 8.dp))
            }
            if (pinned != null) {
                Text(
                    "${pinned.emoji} ${pinned.customer}: ${pinned.line}", color = Void.text, fontSize = 12.sp,
                    modifier = Modifier.padding(horizontal = 12.dp).clip(RoundedCornerShape(12.dp)).background(Void.panel.copy(alpha = 0.85f)).padding(8.dp),
                )
            }
        }
        Column(
            Modifier.padding(top = top).fillMaxSize().background(Void.bg).navigationBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (s in LabStep.entries) {
                    val on = s == lab.step
                    val done = s.ordinal < lab.step.ordinal
                    Text(
                        (if (done) "✓ " else "") + s.title, fontSize = 12.sp, color = if (on) Void.bg else if (done) Void.cyan else Void.dim,
                        fontWeight = if (on) FontWeight.Bold else FontWeight.Normal,
                        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (on) Void.cyan else Void.panelAlt).padding(horizontal = 9.dp, vertical = 5.dp),
                    )
                }
            }
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (lab.step) {
                    LabStep.BASE -> {
                        Text("Pick a base. Each one feels different!", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (b in Base.entries) {
                                Choice(b.title, r.base == b, !Game.isUnlocked(key(b)), b.price, emoji = b.emoji) {
                                    pick(key(b)) {
                                        lab.draft = r.copy(base = b)
                                        Audio.play(Snd.POUR, 0.6f)
                                    }
                                }
                            }
                        }
                        Text("${r.base.emoji} ${r.base.title}: ${r.base.blurb}", color = Void.text, fontSize = 13.sp)
                    }
                    LabStep.COLOR -> {
                        Text("Drop in up to 3 colours. They swirl together as you play.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            for (c in COLORS) {
                                Choice(c.title, c.id in r.colors, !Game.isUnlocked(key(c)), c.price, size = 48.dp, fill = argbColor(c.argb)) {
                                    pick(key(c)) {
                                        val cs = r.colors.toMutableList()
                                        when {
                                            c.id in cs && cs.size > 1 -> cs.remove(c.id)
                                            c.id in cs -> Unit
                                            cs.size >= 3 -> cs[2] = c.id
                                            else -> cs += c.id
                                        }
                                        lab.draft = r.copy(colors = cs)
                                    }
                                }
                            }
                        }
                    }
                    LabStep.MIXINS -> {
                        Text("Tap a mix-in to add more: a pinch, some, loads. Up to 3 kinds.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (a in AddIn.entries) {
                                val lvl = r.addIns[a]
                                Choice(lvl?.let { ADD_IN_LEVELS[it].first } ?: a.title, lvl != null, !Game.isUnlocked(key(a)), a.price, emoji = a.emoji) {
                                    pick(key(a)) {
                                        val m = r.addIns.toMutableMap()
                                        when {
                                            lvl == null && m.size >= 3 -> return@pick
                                            lvl == null -> m[a] = 0
                                            lvl < ADD_IN_LEVELS.lastIndex -> m[a] = lvl + 1
                                            else -> m.remove(a)
                                        }
                                        lab.draft = r.copy(addIns = m)
                                    }
                                }
                            }
                        }
                        Text("Glitter", color = Void.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (g in Glitter.entries) {
                                Choice(
                                    g.title, r.glitter == g, !Game.isUnlocked(key(g)), g.price, size = 46.dp,
                                    fill = if (g == Glitter.NONE || g.argb == 0L) null else argbColor(g.argb),
                                    emoji = when (g) { Glitter.NONE -> "🚫"; Glitter.RAINBOW -> "🌈"; else -> "✨" },
                                ) { pick(key(g)) { lab.draft = r.copy(glitter = g) } }
                            }
                        }
                    }
                    LabStep.STIR -> {
                        if (!lab.activated) {
                            Text("Your glue is ready. Pour in the activator, then stir in big circles in the dish until it turns into slime!", color = Void.text, fontSize = 14.sp)
                            PillButton("🧴 Pour activator", true, Modifier.fillMaxWidth()) {
                                lab.activated = true
                                Audio.play(Snd.POUR, 0.8f)
                                Haptics.buzz(Haptics.Kind.THUD, 0.5f)
                            }
                        } else {
                            Text("Stir! Drag your finger round and round in the dish.", color = Void.text, fontSize = 14.sp)
                            LinearProgressIndicator(
                                progress = { lab.stir }, modifier = Modifier.fillMaxWidth().height(12.dp).clip(RoundedCornerShape(50)),
                                color = Void.cyan, trackColor = Void.panelAlt,
                            )
                            Text(
                                when {
                                    lab.stir < 0.3f -> "Still gloopy…"
                                    lab.stir < 0.7f -> "It's coming together!"
                                    else -> "Almost slime!"
                                },
                                color = Void.dim, fontSize = 13.sp,
                            )
                        }
                    }
                    LabStep.NAME -> NameStep(state, pinned?.let { o -> "${o.emoji} Deliver to ${o.customer} (${Orders.stars(o, r)}★)" })
                }
            }
            if (lab.step != LabStep.NAME) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (lab.step == LabStep.BASE) {
                        PillButton("🎲 Surprise me", false) {
                            val m = Game.mysteryRecipe()
                            lab.draft = m.copy(id = r.id)
                            Audio.play(Snd.WHOOSH, 0.6f)
                            lab.step = LabStep.STIR
                        }
                    } else {
                        PillButton("Back", false) { lab.step = LabStep.entries[lab.step.ordinal - 1] }
                    }
                    Spacer(Modifier.weight(1f))
                    if (lab.step != LabStep.STIR) PillButton("Next", true) { lab.step = LabStep.entries[lab.step.ordinal + 1] }
                }
            }
        }
    }

    buying?.let { k -> BuyDialog(k) { buying = null } }
}

@Composable
private fun NameStep(state: AppState, deliver: String?) {
    val lab = state.lab
    val r = lab.draft
    if (lab.saved) {
        Text("🏺 ${r.name} is on your shelf!", color = Void.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton("Play with it", true) { Game.select(r.id); state.go(Screen.PLAY) }
            PillButton("Make another", false) { lab.startNew(); state.view.scene { setForm(0f) } }
        }
        return
    }
    Text("It's slime! Give it a name and pop it in a jar.", color = Void.dim, fontSize = 13.sp)
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            r.name, { lab.draft = r.copy(name = it.take(28)) }, Modifier.weight(1f), singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(focusedTextColor = Void.text, unfocusedTextColor = Void.text, focusedBorderColor = Void.cyan, unfocusedBorderColor = Void.border, cursorColor = Void.cyan),
        )
        Text("🎲", fontSize = 28.sp, modifier = Modifier.squishy { lab.draft = r.copy(name = Names.suggest(r)) })
    }
    Text(
        "${r.base.emoji} ${r.base.title} · " + r.colors.joinToString { colorOf(it).title } +
            (if (r.addIns.isNotEmpty()) " · " + r.addIns.keys.joinToString { it.emoji } else "") +
            (if (r.glitter != Glitter.NONE) " · ✨" else ""),
        color = Void.dim, fontSize = 12.sp,
    )
    fun save(): Recipe {
        val done = r.copy(name = r.name.ifBlank { Names.suggest(r) }, created = System.currentTimeMillis())
        lab.draft = done
        Game.saveJar(done)
        Game.add(Stat.MADE)
        Game.earn(5, 20)
        lab.saved = true
        Audio.play(Snd.BELL, 0.8f)
        return done
    }
    PillButton("🏺 Put it in a jar", true, Modifier.fillMaxWidth()) { save() }
    if (deliver != null) {
        PillButton(deliver, false, Modifier.fillMaxWidth()) {
            val done = save()
            val id = state.pinnedOrder ?: return@PillButton
            val (stars, pay) = Game.fulfil(id, done)
            state.pinnedOrder = null
            state.toasts += Toast(System.nanoTime(), "⭐".repeat(stars), "Order delivered!", "+$pay 🪙")
            Audio.play(Snd.COIN, 0.9f)
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Tip: the shop's customers pay coins for slimes that match their order.", color = Void.dim, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Void.border, RoundedCornerShape(12.dp)).padding(10.dp),
    )
    Spacer(Modifier.statusBarsPadding().height(0.dp))
}
