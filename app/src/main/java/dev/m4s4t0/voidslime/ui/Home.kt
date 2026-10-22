package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.RACE_LEVEL
import dev.m4s4t0.voidslime.model.STICKERS
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidslime.model.iconOf
import dev.m4s4t0.voidslime.model.label
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void

@Composable
fun HomeScreen(state: AppState) {
    var gift by remember { mutableStateOf<Game.Gift?>(null) }
    Box(Modifier.fillMaxSize().safe()) {
        // Left: the logo and the big three.
        Column(
            Modifier.width(HOME_SIDE.dp).fillMaxHeight().verticalScroll(rememberScrollState()).padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Pic("logo", Dp.Unspecified, Modifier.width(220.dp).height(98.dp).bob(3.dp, periodMs = 2600))
            BigButton("play", "Play", "Squish, stretch, pop", accent = Void.violet, phase = 0) { state.go(Screen.PLAY) }
            BigButton("flask", "Slime lab", "Make a brand new slime", accent = Void.cyan, phase = 1) { state.lab.startNew(); state.go(Screen.LAB) }
            BigButton("bubbles", "Bubble rush", "Best: ${Game.stats[Stat.RUSH_BEST] ?: 0}", accent = Void.ok, phase = 2) {
                state.rush.result = null; state.go(Screen.RUSH)
            }
            val raceOpen = Game.level >= RACE_LEVEL
            BigButton("race", "Slime racing", if (raceOpen) "Wins: ${Game.stats[Stat.RACE_WINS] ?: 0}" else "Opens at level $RACE_LEVEL", accent = Void.warn, phase = 3) {
                if (raceOpen) state.go(Screen.RACE)
                else state.toasts += Toast(System.nanoTime(), "lock", "Slime racing opens at level $RACE_LEVEL", "Play, make slimes and fill orders to level up")
            }
        }

        // Top right: progress and settings.
        Row(
            Modifier.align(Alignment.TopEnd).padding(top = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            LevelChip()
            CoinChip()
            RoundIcon("gear", size = 42.dp) { state.go(Screen.SETTINGS) }
        }

        // Right: everything else.
        Column(
            Modifier.align(Alignment.TopEnd).width(HOME_SIDE.dp).fillMaxHeight().padding(top = 62.dp, end = 12.dp, start = 6.dp, bottom = 10.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            if (Game.giftReady) {
                BigButton("gift", "Daily gift!", "Tap to open", accent = Void.bad, phase = 4, height = 62.dp) {
                    gift = Game.openGift()
                    Audio.play(Snd.DONE, 0.9f)
                    Haptics.buzz(Haptics.Kind.SUCCESS)
                    state.celebrate()
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("shop", "Shop", Modifier.weight(1f), Void.warn, badge = Game.orders.size.takeIf { it > 0 }?.toString(), phase = 1) { state.shopTab = ShopTab.ORDERS; state.go(Screen.SHOP) }
                Tile("shelf", "Shelf", Modifier.weight(1f), Void.violet, phase = 2) { state.go(Screen.SHELF) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile(Game.world.icon, "Worlds", Modifier.weight(1f), Void.cyan, phase = 3) { state.shopTab = ShopTab.WORLDS; state.go(Screen.SHOP) }
                Tile("stickers", "${Game.earned.size}/${STICKERS.size}", Modifier.weight(1f), Void.bad, phase = 4) { state.go(Screen.STICKERS) }
            }
            ChallengesCard(compact = true) { state.go(Screen.PLAY) }
        }

        // Bottom middle: who's in the dish.
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp).widthIn(max = 300.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            UpdateBanner { state.go(Screen.SETTINGS) }
            Row(
                Modifier.bob(2.dp, 5).glass(RoundedCornerShape(50), alpha = 0.8f).padding(horizontal = 14.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Pic(Game.current.base.icon, 26.dp)
                Column {
                    Text(Game.current.name, color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    val streak = Game.stats[Stat.STREAK] ?: 1
                    Text(if (streak > 1) "Poke it! · $streak-day streak" else "Poke it!", color = Void.dim, fontSize = 11.sp)
                }
            }
        }
    }
    gift?.let { g ->
        AlertDialog(
            onDismissRequest = { gift = null },
            containerColor = Void.panel,
            icon = { Pic("gift", 64.dp) },
            title = { Text("Your gift", color = Void.text, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    GiftLine("coins") { Text("${g.coins} coins", color = Void.warn, fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                    GiftLine("jar") { Text("A mystery jar: ${g.jar.name}", color = Void.text, fontSize = 15.sp) }
                    g.unlockedKey?.let { k -> GiftLine(iconOf(k) ?: "sparkles") { Text("Unlocked: ${label(k)}", color = Void.cyan, fontSize = 15.sp) } }
                    Text("Come back tomorrow for another one!", color = Void.dim, fontSize = 13.sp)
                }
            },
            confirmButton = {
                PillButton("Play with it", true) {
                    Game.select(g.jar.id)
                    gift = null
                    state.go(Screen.PLAY)
                }
            },
            dismissButton = { PillButton("Later", false) { gift = null } },
        )
    }
}

@Composable
private fun GiftLine(icon: String, content: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Pic(icon, 34.dp)
        content()
    }
}
