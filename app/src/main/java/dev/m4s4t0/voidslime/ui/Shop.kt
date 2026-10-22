package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.gl.Tool
import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.COLORS
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Mold
import dev.m4s4t0.voidslime.model.Order
import dev.m4s4t0.voidslime.model.Orders
import dev.m4s4t0.voidslime.model.Plate
import dev.m4s4t0.voidslime.model.Toy
import dev.m4s4t0.voidslime.model.Tray
import dev.m4s4t0.voidslime.model.World
import dev.m4s4t0.voidslime.model.Vessel
import dev.m4s4t0.voidslime.model.OrderKind
import dev.m4s4t0.voidslime.model.levelOf
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidslime.model.iconOf
import dev.m4s4t0.voidslime.model.key
import dev.m4s4t0.voidslime.model.label
import dev.m4s4t0.voidslime.model.priceOf
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.SectionTitle
import dev.m4s4t0.voidui.Void

enum class ShopTab(val title: String, val icon: String) {
    ORDERS("Orders", "bell"), SUPPLIES("Supplies", "basket"), TOYS("Toys", "tool_wand"),
    WORLDS("Worlds", "planet"), DISHES("Dishes", "dish"), MOLDS("Molds", "mold_star"),
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ShopScreen(state: AppState) {
    val tab = state.shopTab
    var handing by remember { mutableStateOf<Order?>(null) }
    var buying by remember { mutableStateOf<String?>(null) }
    /** Use it if it's owned, otherwise offer to buy it. */
    fun pick(k: String, use: () -> Unit) {
        if (Game.isUnlocked(k)) { use(); Audio.play(Snd.CLICK, 0.6f) } else buying = k
    }
    fun serve(o: Order, r: dev.m4s4t0.voidslime.model.Recipe) {
        val s = Game.fulfil(o.id, r)
        handing = null
        Audio.play(if (s.stars == 3) Snd.DONE else Snd.COIN, 0.9f)
        Haptics.buzz(Haptics.Kind.SUCCESS)
        state.toasts += servedToast(o, s)
    }
    Column(Modifier.fillMaxSize().safe()) {
        GameBar("Slime shop", { state.go(Screen.HOME) }, icon = "shop", subtitle = "Make what customers ask for, earn coins, spend them on goodies") { CoinChip() }
        Column(Modifier.fillMaxSize()) {
            // The tabs, along the top.
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 12.dp).padding(bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                for (t in ShopTab.entries) {
                    val on = t == tab
                    Row(
                        Modifier.squishy { state.shopTab = t }.clip(RoundedCornerShape(50))
                            .background(if (on) Void.cyan.copy(alpha = 0.25f) else Void.panel.copy(alpha = 0.75f))
                            .border(if (on) 2.dp else 1.dp, if (on) Void.cyan else Void.border, RoundedCornerShape(50))
                            .padding(start = 6.dp, end = 14.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Pic(t.icon, 34.dp)
                        Text(t.title, color = if (on) Void.text else Void.dim, fontSize = 14.sp, fontWeight = if (on) FontWeight.Bold else FontWeight.Normal)
                        if (t == ShopTab.ORDERS && Game.orders.isNotEmpty()) Badge("${Game.orders.size}")
                    }
                }
            }
            Column(
                Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                val gap = Arrangement.spacedBy(10.dp)
                when (tab) {
                    ShopTab.ORDERS -> FlowRow(horizontalArrangement = gap, verticalArrangement = gap, maxItemsInEachRow = 2) {
                        if (Game.orders.isEmpty()) Text("No customers right now. Check back soon!", color = Void.dim, fontSize = 13.sp)
                        for (o in Game.orders) OrderCard(o, Modifier.weight(1f), onMake = {
                            state.pinnedOrder = o.id
                            state.lab.startNew()
                            state.lab.draft = state.lab.draft.copy(base = o.base?.takeIf { Game.isUnlocked(key(it)) } ?: Base.GLOSSY)
                            state.go(Screen.LAB)
                        }, onHand = { handing = o }, onSkip = { Game.skipOrder(o.id); Audio.play(Snd.WHOOSH, 0.5f) })
                    }
                    ShopTab.SUPPLIES -> {
                        Supply("Bases", Base.entries.map { key(it) to it.icon }) { buying = it }
                        Supply("Colours", COLORS.map { key(it) to null }, colors = true) { buying = it }
                        Supply("Mix-ins", AddIn.entries.map { key(it) to it.icon }) { buying = it }
                        Supply("Glitter", Glitter.entries.drop(1).map { key(it) to it.icon }) { buying = it }
                    }
                    ShopTab.TOYS -> {
                        Text("Extra tools for playing with your slime. They show up in Play's tool rail.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = gap, verticalArrangement = gap, maxItemsInEachRow = 2) {
                            for (t in Toy.entries) ToyCard(t, Modifier.weight(1f), onTry = { state.tool = Tool.valueOf(t.name); state.go(Screen.PLAY) }) { buying = key(t) }
                        }
                    }
                    ShopTab.WORLDS -> {
                        Text("Where your slime lives: a backdrop, floaty sparkles and its own little song.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                            for (w in World.entries) {
                                val k = key(w)
                                ItemCard(w.icon, w.title, w.blurb, Game.world == w, Game.isUnlocked(k), w.price, Modifier.width(150.dp), art = 110.dp, need = w.level) {
                                    pick(k) { Game.chooseWorld(w) }
                                }
                            }
                        }
                    }
                    ShopTab.DISHES -> {
                        SectionTitle("What it sits in")
                        Text("Bowls and jars are deep: the slime slumps into them and climbs the sides. Dish shapes and colours go with the dish.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                            for (v in Vessel.entries) {
                                val k = key(v)
                                ItemCard(v.icon, v.title, v.blurb, Game.vessel == v, Game.isUnlocked(k), v.price, Modifier.width(132.dp), art = 84.dp, need = v.level) {
                                    pick(k) { Game.chooseVessel(v) }
                                }
                            }
                        }
                        SectionTitle("Dish shape")
                        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                            for (p in Plate.entries) {
                                val k = key(p)
                                ItemCard(p.icon, p.title, null, Game.plate == p, Game.isUnlocked(k), p.price, Modifier.width(112.dp), art = 76.dp, need = p.level) {
                                    pick(k) { Game.choosePlate(p) }
                                }
                            }
                        }
                        SectionTitle("Dish colour")
                        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                            for (t in Tray.entries) {
                                val k = key(t)
                                ItemCard(t.icon, t.title, null, Game.tray == t, Game.isUnlocked(k), t.price, Modifier.width(112.dp), art = 76.dp) {
                                    pick(k) { Game.chooseTray(t) }
                                }
                            }
                        }
                    }
                    ShopTab.MOLDS -> {
                        Text("Shapes your slime can set into. Pick one in the lab's Shape step.", color = Void.dim, fontSize = 13.sp)
                        FlowRow(horizontalArrangement = gap, verticalArrangement = gap) {
                            for (m in Mold.entries) {
                                val k = key(m)
                                ItemCard(m.icon, m.title, m.blurb, false, Game.isUnlocked(k), m.price, Modifier.width(132.dp), art = 84.dp, ownedLabel = "Owned", need = m.level) {
                                    if (!Game.isUnlocked(k)) buying = k
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    handing?.let { o ->
        AlertDialog(
            onDismissRequest = { handing = null },
            containerColor = Void.panel,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Pic("cust_" + o.face, 44.dp)
                    Text("Which jar for ${o.customer}?", color = Void.text)
                }
            },
            text = {
                Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (Game.jars.isEmpty()) Text("Your shelf is empty. Make one in the lab!", color = Void.dim)
                    for (r in Game.jars.sortedByDescending { Orders.match(o, it) }.take(12)) {
                        val stars = Orders.stars(o, r)
                        Row(
                            Modifier.fillMaxWidth().squishy { serve(o, r) },
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            JarPreview(r, Modifier.width(44.dp).height(52.dp))
                            Column(Modifier.weight(1f)) {
                                Text(r.name, color = Void.text, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Stars(stars, size = 16.dp)
                                    Coins(Orders.payout(o, stars), fontSize = 12.sp, icon = 14.dp)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { PillButton("Close", false) { handing = null } },
        )
    }
    buying?.let { k ->
        BuyDialog(k) {
            buying = null
            // Something new to look at: put it straight on.
            if (Game.isUnlocked(k)) {
                World.entries.firstOrNull { key(it) == k }?.let(Game::chooseWorld)
                Plate.entries.firstOrNull { key(it) == k }?.let(Game::choosePlate)
                Tray.entries.firstOrNull { key(it) == k }?.let(Game::chooseTray)
                Vessel.entries.firstOrNull { key(it) == k }?.let(Game::chooseVessel)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun OrderCard(o: Order, modifier: Modifier, onMake: () -> Unit, onHand: () -> Unit, onSkip: () -> Unit) {
    val edge = when (o.kind) { OrderKind.VIP -> Void.warn; OrderKind.RUSH -> Void.bad; OrderKind.PICKY -> Void.violet; else -> Void.border }
    Column(modifier.glass(RoundedCornerShape(22.dp), edge, 0.88f).padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Pic("cust_" + o.face, 56.dp, Modifier.bob(2.dp, o.id.hashCode() and 7))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(o.customer, color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f, false))
                    if (o.kind != OrderKind.REGULAR) KindTag(o)
                }
                Text("“${o.line}”", color = Void.dim, fontSize = 12.sp, maxLines = 3, overflow = TextOverflow.Ellipsis)
            }
            Coins(o.reward, fontSize = 16.sp, icon = 22.dp)
        }
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            o.base?.let { Wish(it.icon, it.title) }
            o.color?.let { Swatch(colorOf(it).title, colorOf(it).argb) }
            o.color2?.let { Swatch("& " + colorOf(it).title, colorOf(it).argb) }
            o.addIn?.let { Wish(it.icon, if (o.loads) "Loads of ${it.title.lowercase()}" else it.title) }
            o.mold?.let { Wish(it.icon, it.title + " shape") }
            if (o.glitter) Wish("sparkles", "Glitter")
            if (o.smiley) Wish("face", "Cute face")
        }
        o.prize?.takeIf { !Game.isUnlocked(it) }?.let { k ->
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Pic("gift", 24.dp)
                Text("Make it perfect for a gift: ${label(k)}", color = Void.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            PillButton("Make it", true) { tapFeedback(); onMake() }
            PillButton("From shelf", false) { tapFeedback(); onHand() }
            Spacer(Modifier.weight(1f))
            Pic("trash", 32.dp, Modifier.squishy(onSkip))
        }
    }
}

@Composable
private fun ToyCard(t: Toy, modifier: Modifier, onTry: () -> Unit, onBuy: () -> Unit) {
    val owned = Game.isUnlocked(key(t))
    Row(
        modifier.glass(RoundedCornerShape(22.dp), if (owned) Void.violet.copy(alpha = 0.6f) else Void.border, 0.88f).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Pic(t.icon, 56.dp)
        Column(Modifier.weight(1f)) {
            Text(t.title, color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(t.blurb, color = Void.dim, fontSize = 12.sp)
        }
        if (owned) PillButton("Play", true) { tapFeedback(); onTry() }
        else if (Game.level < t.level) LevelTag(t.level, fontSize = 14.sp)
        else Box(Modifier.squishy(onBuy).glass(RoundedCornerShape(50), Void.warn, 0.6f).padding(horizontal = 12.dp, vertical = 8.dp)) {
            Coins(t.price, fontSize = 14.sp)
        }
    }
}

@Composable
private fun Wish(icon: String, text: String) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Void.panelAlt).padding(start = 4.dp, end = 10.dp, top = 2.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Pic(icon, 22.dp)
        Text(text, color = Void.cyan, fontSize = 12.sp)
    }
}

@Composable
private fun Swatch(text: String, argb: Long) {
    Row(
        Modifier.clip(RoundedCornerShape(50)).background(Void.panelAlt).padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Box(Modifier.size(16.dp).clip(CircleShape).background(argbColor(argb)).border(1.dp, Void.border, CircleShape))
        Text(text, color = Void.cyan, fontSize = 12.sp)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Supply(title: String, keys: List<Pair<String, String?>>, colors: Boolean = false, onBuy: (String) -> Unit) {
    SectionTitle(title)
    FlowRow(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for ((k, icon) in keys) {
            val owned = Game.isUnlocked(k)
            val name = label(k).removeSuffix(" base").removeSuffix(" glitter")
            Choice(
                name, owned, !owned, priceOf(k), size = if (colors) 46.dp else 60.dp,
                fill = if (colors) argbColor(colorOf(k.substringAfter(':')).argb) else null, icon = icon, need = levelOf(k),
            ) { if (!owned) onBuy(k) }
        }
    }
}

@Composable
fun BuyDialog(k: String, onDone: () -> Unit) {
    val price = priceOf(k)
    val need = levelOf(k)
    val early = Game.level < need
    val can = Game.coins >= price && !early
    AlertDialog(
        onDismissRequest = onDone,
        containerColor = Void.panel,
        icon = {
            val ic = iconOf(k)
            if (ic != null) Pic(ic, 84.dp, Modifier.bob(3.dp))
            else Box(Modifier.size(64.dp).clip(CircleShape).background(argbColor(colorOf(k.substringAfter(':')).argb)))
        },
        title = { Text(label(k), color = Void.text) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Coins(price, fontSize = 20.sp, icon = 28.dp)
                when {
                    early -> {
                        LevelTag(need, fontSize = 16.sp)
                        Text("Opens at level $need (you're level ${Game.level}). Play, fill orders and race to level up!", color = Void.dim)
                    }
                    can -> Text("Unlock it?", color = Void.dim)
                    else -> Text("Not enough coins yet. Finish shop orders and play to earn more!", color = Void.dim)
                }
            }
        },
        confirmButton = {
            if (can) PillButton("Buy", true) {
                if (Game.buy(k)) { Audio.play(Snd.BUY, 0.9f); Haptics.buzz(Haptics.Kind.SUCCESS) }
                onDone()
            }
        },
        dismissButton = { PillButton(if (can) "Not now" else "OK", false, onClick = onDone) },
    )
}

/** "VIP", "Rush 4:12", "Picky": what sort of customer this is. */
@Composable
private fun KindTag(o: Order) {
    val color = when (o.kind) { OrderKind.VIP -> Void.warn; OrderKind.RUSH -> Void.bad; else -> Void.violet }
    var text = o.kind.title
    if (o.kind == OrderKind.RUSH && o.expires > 0) {
        var now by remember { mutableStateOf(System.currentTimeMillis()) }
        androidx.compose.runtime.LaunchedEffect(o.id) { while (true) { kotlinx.coroutines.delay(1000); now = System.currentTimeMillis() } }
        val left = ((o.expires - now) / 1000).coerceAtLeast(0)
        text = "Rush ${left / 60}:${(left % 60).toString().padStart(2, '0')}"
    }
    Text(
        text, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = Modifier.clip(RoundedCornerShape(50)).background(color.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

/** The thank-you toast after handing over a slime: stars, coins, a perfect-run combo, a VIP's gift. */
fun servedToast(o: Order, s: Game.Served): Toast {
    val title = when {
        s.prize != null -> "${o.customer} gave you ${label(s.prize)}!"
        s.stars == 3 && s.combo > 1 -> "Perfect x${s.combo}! ${o.customer} loves it"
        s.stars == 3 -> "Perfect! ${o.customer} loves it"
        else -> "${o.customer} says thanks!"
    }
    return Toast(System.nanoTime(), "cust_" + o.face, title, if (s.combo > 1) "Combo bonus!" else "", coins = s.pay, stars = s.stars)
}
