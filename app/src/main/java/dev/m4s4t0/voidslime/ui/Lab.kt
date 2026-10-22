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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
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
import dev.m4s4t0.voidslime.model.Mold
import dev.m4s4t0.voidslime.model.Names
import dev.m4s4t0.voidslime.model.Orders
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidslime.model.key
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar

enum class LabStep(val title: String, val icon: String) {
    BASE("Base", "flask"), COLOR("Colour", "palette"), MIXINS("Mix-ins", "sparkles"), SHAPE("Shape", "mold_heart"), STIR("Stir", "spoon"), NAME("Jar it", "jar")
}

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

    // Before stirring the slime is runny; the shape step shows it set, so you can see the mold you picked.
    LaunchedEffect(r.id, lab.step) {
        val f = if (lab.step == LabStep.SHAPE || lab.step == LabStep.NAME) 1f else lab.stir
        state.view.scene { setForm(f) }
    }
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

    BoxWithConstraints(Modifier.fillMaxSize().safe()) {
        val panel = labPanel(maxWidth.value).dp
        Column(Modifier.padding(end = panel), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            GameBar("Slime lab", { state.go(Screen.HOME) }, icon = "flask", subtitle = if (lab.step == LabStep.STIR && lab.activated) "Stir in circles!" else "Step ${lab.step.ordinal + 1} of ${LabStep.entries.size}") {
                CoinChip()
            }
            if (pinned != null) {
                Row(
                    Modifier.padding(horizontal = 12.dp).widthIn(max = 360.dp).glass(RoundedCornerShape(18.dp), alpha = 0.88f).padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pic("cust_" + pinned.face, 44.dp)
                    Column {
                        Text(pinned.customer, color = Void.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Text(pinned.line, color = Void.dim, fontSize = 12.sp, maxLines = 2)
                    }
                }
            }
        }
        Column(
            Modifier.align(Alignment.CenterEnd).width(panel).fillMaxHeight().padding(8.dp)
                .glass(RoundedCornerShape(26.dp), alpha = 0.9f).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                for (s in LabStep.entries) {
                    val on = s == lab.step
                    val done = s.ordinal < lab.step.ordinal
                    Box(
                        Modifier.size(if (on) 44.dp else 36.dp).clip(CircleShape)
                            .background(if (on) Void.cyan.copy(alpha = 0.35f) else Void.panelAlt)
                            .border(if (on) 2.dp else 1.dp, if (on) Void.cyan else if (done) Void.ok else Void.border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) { Pic(if (done) "check" else s.icon, if (on) 32.dp else 24.dp, alpha = if (on || done) 1f else 0.6f) }
                }
            }
            Text(lab.step.title, color = Void.text, fontSize = 17.sp, fontWeight = FontWeight.Bold)
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                when (lab.step) {
                    LabStep.BASE -> {
                        Text("Pick a base. Each one feels different!", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (b in Base.entries) {
                                Choice(b.title, r.base == b, !Game.isUnlocked(key(b)), b.price, icon = b.icon) {
                                    pick(key(b)) {
                                        lab.draft = r.copy(base = b)
                                        Audio.play(Snd.POUR, 0.6f)
                                    }
                                }
                            }
                        }
                        Text("${r.base.title}: ${r.base.blurb}", color = Void.text, fontSize = 13.sp)
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
                                Choice(lvl?.let { ADD_IN_LEVELS[it].first } ?: a.title, lvl != null, !Game.isUnlocked(key(a)), a.price, icon = a.icon) {
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
                                    icon = g.icon,
                                ) { pick(key(g)) { lab.draft = r.copy(glitter = g) } }
                            }
                        }
                    }
                    LabStep.SHAPE -> {
                        Text("Pick a mold. Your slime sets in this shape once it's stirred.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            for (m in Mold.entries) {
                                Choice(m.title, r.mold == m, !Game.isUnlocked(key(m)), m.price, icon = m.icon) {
                                    pick(key(m)) {
                                        lab.draft = r.copy(mold = m, face = if (m.animal && r.mold != m) true else r.face)
                                        Audio.play(Snd.SQUISH, 0.7f)
                                    }
                                }
                            }
                        }
                        Text(r.mold.blurb, color = Void.text, fontSize = 13.sp)
                        Row(
                            Modifier.fillMaxWidth().squishy { lab.draft = r.copy(face = !r.face) }
                                .glass(RoundedCornerShape(18.dp), if (r.face) Void.cyan else Void.border, 0.6f).padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Pic("face", 40.dp)
                            Column(Modifier.weight(1f)) {
                                Text("Cute face", color = Void.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Text("Little eyes and rosy cheeks. It blinks!", color = Void.dim, fontSize = 12.sp)
                            }
                            Pic(if (r.face) "check" else "star_off", 28.dp)
                        }
                    }
                    LabStep.STIR -> {
                        if (!lab.activated) {
                            Text("Your glue is ready. Pour in the activator, then stir in big circles in the dish until it turns into slime!", color = Void.text, fontSize = 14.sp)
                            PillButton("Pour activator", true, Modifier.fillMaxWidth()) {
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
                    LabStep.NAME -> NameStep(state, pinned?.let { o -> "Deliver to ${o.customer}" })
                }
            }
            if (lab.step != LabStep.NAME) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (lab.step == LabStep.BASE) {
                        Pic("dice", 40.dp, Modifier.squishy {
                            val m = Game.mysteryRecipe()
                            lab.draft = m.copy(id = r.id)
                            Audio.play(Snd.WHOOSH, 0.6f)
                            lab.step = LabStep.STIR
                        })
                        Text("Surprise me", color = Void.dim, fontSize = 12.sp)
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
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pic("jar", 48.dp, Modifier.bob(3.dp))
            Text("${r.name} is on your shelf!", color = Void.text, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
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
        Pic("dice", 40.dp, Modifier.squishy { lab.draft = r.copy(name = Names.suggest(r)) })
    }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Pic(r.base.icon, 28.dp)
        for (a in r.addIns.keys) Pic(a.icon, 28.dp)
        if (r.glitter != Glitter.NONE) Pic(r.glitter.icon, 28.dp)
        Pic(r.mold.icon, 28.dp)
        Text(r.colors.joinToString { colorOf(it).title }, color = Void.dim, fontSize = 12.sp, maxLines = 1)
    }
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
    PillButton("Put it in a jar", true, Modifier.fillMaxWidth()) { save() }
    if (deliver != null) {
        PillButton(deliver, false, Modifier.fillMaxWidth()) {
            val done = save()
            val id = state.pinnedOrder ?: return@PillButton
            val face = Game.orders.firstOrNull { it.id == id }?.face ?: "mia"
            val (stars, pay) = Game.fulfil(id, done)
            state.pinnedOrder = null
            state.toasts += Toast(System.nanoTime(), "cust_$face", "Order delivered!", "", coins = pay, stars = stars)
            Audio.play(Snd.COIN, 0.9f)
        }
    }
    Spacer(Modifier.height(4.dp))
    Text(
        "Tip: the shop's customers pay coins for slimes that match their order.", color = Void.dim, fontSize = 12.sp,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).border(1.dp, Void.border, RoundedCornerShape(12.dp)).padding(10.dp),
    )
}
