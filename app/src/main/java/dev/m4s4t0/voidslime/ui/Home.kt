package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.STICKERS
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidslime.model.label
import dev.m4s4t0.voidui.BarIcon
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Void

@Composable
fun HomeScreen(state: AppState) {
    var gift by remember { mutableStateOf<Game.Gift?>(null) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val top = maxHeight * 0.52f
        // Over the slime: the name and the chips.
        Column(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 14.dp, vertical = 8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Void Slime", color = Void.text, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("Poke it! ${Game.current.name}", color = Void.dim, fontSize = 12.sp, maxLines = 1)
                }
                LevelChip()
                CoinChip()
                BarIcon(Icons.Rounded.Settings, "Settings", { tapFeedback(); state.go(Screen.SETTINGS) })
            }
            val streak = Game.stats[Stat.STREAK] ?: 1
            if (streak > 1) Text("🔥 $streak-day streak", color = Void.warn, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
        }
        Column(
            Modifier.padding(top = top).fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Void.bg.copy(alpha = 0.9f), Void.bg), endY = 80f))
                .verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Spacer(Modifier.height(4.dp))
            UpdateBanner { state.go(Screen.SETTINGS) }
            if (Game.giftReady) {
                Row(
                    Modifier.fillMaxWidth().squishy {
                        gift = Game.openGift()
                        Audio.play(Snd.DONE, 0.9f)
                        Haptics.buzz(Haptics.Kind.SUCCESS)
                        state.celebrate()
                    }.clip(RoundedCornerShape(20.dp))
                        .background(Brush.horizontalGradient(listOf(Void.violet.copy(alpha = 0.55f), Void.cyan.copy(alpha = 0.4f))))
                        .border(1.dp, Void.cyan, RoundedCornerShape(20.dp)).padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text("🎁", fontSize = 34.sp)
                    Column(Modifier.weight(1f)) {
                        Text("Today's gift is here!", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Tap to open: coins, a mystery slime, maybe a new supply", color = Color.White.copy(alpha = 0.85f), fontSize = 12.sp)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("🖐️", "Play", "Squish, stretch, pop", Modifier.weight(1f), Void.violet) { state.go(Screen.PLAY) }
                Tile("🧪", "Slime lab", "Make a new slime", Modifier.weight(1f), Void.cyan) { state.lab.startNew(); state.go(Screen.LAB) }
            }
            ChallengesCard { state.go(Screen.PLAY) }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("🛎️", "Slime shop", "${Game.orders.size} customers waiting", Modifier.weight(1f), Void.warn, badge = "${Game.orders.size}") { state.go(Screen.SHOP) }
                Tile("🫧", "Bubble rush", "Best: ${Game.stats[Stat.RUSH_BEST] ?: 0}", Modifier.weight(1f), Void.ok) { state.rush.result = null; state.go(Screen.RUSH) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Tile("🏺", "Shelf", plural(Game.jars.size, "jar"), Modifier.weight(1f), Void.violet) { state.go(Screen.SHELF) }
                Tile("🌟", "Stickers", "${Game.earned.size} of ${STICKERS.size}", Modifier.weight(1f), Void.bad) { state.go(Screen.STICKERS) }
            }
            Spacer(Modifier.height(12.dp))
        }
    }
    gift?.let { g ->
        AlertDialog(
            onDismissRequest = { gift = null },
            containerColor = Void.panel,
            title = { Text("🎁 Your gift", color = Void.text, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("🪙 ${g.coins} coins", color = Void.text, fontSize = 16.sp)
                    Text("🏺 A mystery jar: ${g.jar.name}", color = Void.text, fontSize = 16.sp)
                    g.unlockedKey?.let { Text("✨ Unlocked: ${label(it)}", color = Void.cyan, fontSize = 16.sp) }
                    Text("Come back tomorrow for another one!", color = Void.dim, fontSize = 13.sp, textAlign = TextAlign.Start)
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
    Box(Modifier.size(0.dp))
}
