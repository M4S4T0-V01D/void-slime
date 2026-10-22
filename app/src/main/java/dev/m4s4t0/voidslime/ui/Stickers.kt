package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.Composable
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.STICKERS
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar

@Composable
fun StickersScreen(state: AppState) {
    Column(Modifier.fillMaxSize().safe()) {
        GameBar("Stickers", { state.go(Screen.HOME) }, icon = "stickers", subtitle = "${Game.earned.size} of ${STICKERS.size} collected") { CoinChip() }
        LazyVerticalGrid(
            GridCells.Adaptive(150.dp), Modifier.fillMaxSize(),
            contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(STICKERS, key = { it.id }) { s ->
                val got = s.id in Game.earned
                val have = (Game.stats[s.stat] ?: 0).coerceAtMost(s.goal.toLong())
                Column(
                    Modifier.clip(RoundedCornerShape(18.dp)).background(Void.panel.copy(alpha = 0.85f))
                        .border(if (got) 2.dp else 1.dp, if (got) Void.warn else Void.border, RoundedCornerShape(18.dp)).padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Pic(s.icon, 64.dp, if (got) Modifier.bob(2.dp, s.id.hashCode() and 7) else Modifier, alpha = if (got) 1f else 0.28f)
                    Text(s.title, color = Void.text, fontSize = 14.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Text(s.desc, color = Void.dim, fontSize = 11.sp, textAlign = TextAlign.Center)
                    if (got) Coins("+${s.reward}", fontSize = 12.sp, icon = 15.dp)
                    else {
                        LinearProgressIndicator(
                            progress = { have / s.goal.toFloat() }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(50)),
                            color = Void.cyan, trackColor = Void.panelAlt,
                        )
                        Text("$have / ${s.goal}", color = Void.dim, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
