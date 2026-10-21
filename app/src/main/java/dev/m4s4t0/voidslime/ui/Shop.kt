package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.COLORS
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Order
import dev.m4s4t0.voidslime.model.Orders
import dev.m4s4t0.voidslime.model.Tray
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidslime.model.key
import dev.m4s4t0.voidslime.model.label
import dev.m4s4t0.voidslime.model.priceOf
import dev.m4s4t0.voidui.Card
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Pills
import dev.m4s4t0.voidui.SectionTitle
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(state: AppState) {
    var tab by remember { mutableStateOf(0) }
    var handing by remember { mutableStateOf<Order?>(null) }
    var buying by remember { mutableStateOf<String?>(null) }
    Column(Modifier.fillMaxSize()) {
        VoidTopBar("Slime shop", { state.go(Screen.HOME) }, subtitle = "Make what customers ask for, earn coins") { CoinChip(Modifier.padding(end = 8.dp)) }
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Pills(listOf(0 to "🛎️ Orders", 1 to "🧺 Supplies", 2 to "🍽️ Dishes"), tab) { tapFeedback(); tab = it }
            when (tab) {
                0 -> for (o in Game.orders) OrderCard(o, onMake = {
                    state.pinnedOrder = o.id
                    state.lab.startNew()
                    state.lab.draft = state.lab.draft.copy(base = o.base?.takeIf { Game.isUnlocked(key(it)) } ?: Base.GLOSSY)
                    state.go(Screen.LAB)
                }, onHand = { handing = o }, onSkip = { Game.skipOrder(o.id); Audio.play(Snd.WHOOSH, 0.5f) })
                1 -> {
                    Supply("Bases", Base.entries.map { key(it) to it.emoji }) { buying = it }
                    Supply("Colours", COLORS.map { key(it) to "🎨" }, colors = true) { buying = it }
                    Supply("Mix-ins", AddIn.entries.map { key(it) to it.emoji }) { buying = it }
                    Supply("Glitter", Glitter.entries.drop(1).map { key(it) to "✨" }) { buying = it }
                }
                else -> {
                    Text("The dish your slime sits in.", color = Void.dim, fontSize = 13.sp)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        for (t in Tray.entries) {
                            val k = key(t)
                            Choice(t.title, Game.tray == t, !Game.isUnlocked(k), t.price, fill = argbColor(t.argb), emoji = t.emoji) {
                                if (Game.isUnlocked(k)) Game.chooseTray(t) else buying = k
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }

    handing?.let { o ->
        AlertDialog(
            onDismissRequest = { handing = null },
            containerColor = Void.panel,
            title = { Text("Which jar for ${o.customer}?", color = Void.text) },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (r in Game.jars.sortedByDescending { Orders.match(o, it) }.take(12)) {
                        Row(
                            Modifier.squishy {
                                val (stars, pay) = Game.fulfil(o.id, r)
                                handing = null
                                Audio.play(if (stars == 3) Snd.DONE else Snd.COIN, 0.9f)
                                Haptics.buzz(Haptics.Kind.SUCCESS)
                                state.toasts += Toast(System.nanoTime(), "⭐".repeat(stars), if (stars == 3) "Perfect! ${o.customer} loves it" else "${o.customer} says thanks!", "+$pay 🪙")
                            },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            JarPreview(r, Modifier.width(44.dp).height(52.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.name, color = Void.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Text("⭐".repeat(Orders.stars(o, r)) + "  🪙 ${Orders.payout(o, Orders.stars(o, r))}", color = Void.dim, fontSize = 12.sp)
                            }
                        }
                    }
                }
            },
            confirmButton = { PillButton("Close", false) { handing = null } },
        )
    }
    buying?.let { k -> BuyDialog(k) { buying = null } }
}

@Composable
private fun OrderCard(o: Order, onMake: () -> Unit, onHand: () -> Unit, onSkip: () -> Unit) {
    Card {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(o.emoji, fontSize = 36.sp)
            Column(Modifier.weight(1f)) {
                Text(o.customer, color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("“${o.line}”", color = Void.dim, fontSize = 13.sp)
            }
            Text("🪙 ${o.reward}", color = Void.warn, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            o.base?.let { Wish("${it.emoji} ${it.title}") }
            o.color?.let { Wish("🎨 ${colorOf(it).title}") }
            o.addIn?.let { Wish("${it.emoji} ${it.title}") }
            if (o.glitter) Wish("✨ Glitter")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PillButton("🧪 Make it", true) { tapFeedback(); onMake() }
            PillButton("🏺 From shelf", false) { tapFeedback(); onHand() }
            Spacer(Modifier.weight(1f))
            PillButton("Skip", false) { tapFeedback(); onSkip() }
        }
    }
}

@Composable
private fun Wish(text: String) {
    Text(text, color = Void.cyan, fontSize = 12.sp)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Supply(title: String, keys: List<Pair<String, String>>, colors: Boolean = false, onBuy: (String) -> Unit) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((k, emoji) in keys) {
            val owned = Game.isUnlocked(k)
            val name = label(k).substringAfter(' ').removeSuffix(" base").removeSuffix(" glitter")
            Choice(
                if (owned) "✓ $name" else name, false, !owned, priceOf(k), size = if (colors) 46.dp else 56.dp,
                fill = if (colors) argbColor(colorOf(k.substringAfter(':')).argb) else null, emoji = if (colors) null else emoji,
            ) { if (!owned) onBuy(k) }
        }
    }
}

@Composable
fun BuyDialog(k: String, onDone: () -> Unit) {
    val price = priceOf(k)
    val can = Game.coins >= price
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Void.panel,
        title = { Text(label(k), color = Void.text) },
        text = { Text(if (can) "Unlock it for 🪙 $price?" else "You need 🪙 $price. Finish shop orders and play to earn more!", color = Void.dim) },
        confirmButton = {
            if (can) PillButton("Buy", true) {
                if (Game.buy(k)) { Audio.play(Snd.BUY, 0.9f); Haptics.buzz(Haptics.Kind.SUCCESS) }
                onDone()
            }
        },
        dismissButton = { PillButton(if (can) "Not now" else "OK", false, onClick = onDone) },
    )
}
