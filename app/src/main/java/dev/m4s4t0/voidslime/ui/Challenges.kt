package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Quests
import dev.m4s4t0.voidui.Void

/** Today's three challenges and how far along each one is. [compact] is the narrow version for Home's side column. */
@Composable
fun ChallengesCard(compact: Boolean = false, onPlay: () -> Unit) {
    val qs = Game.quests
    if (qs.isEmpty()) return
    val done = qs.count { it.done }
    Column(
        Modifier.fillMaxWidth().squishy(onPlay).glass(RoundedCornerShape(20.dp), if (done == qs.size) Void.ok else Void.border, 0.88f)
            .padding(if (compact) 10.dp else 14.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Pic(if (done == qs.size) "trophy" else "target", 24.dp)
            Text(if (compact) "Today" else "Today's challenges", color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            if (done == qs.size) Text("All done!", color = Void.ok, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            else Row(verticalAlignment = Alignment.CenterVertically) {
                Text("$done/${qs.size} · all 3 ", color = Void.dim, fontSize = 11.sp)
                Coins("+${Quests.ALL_BONUS}", fontSize = 11.sp, icon = 14.dp)
            }
        }
        for (q in qs) {
            val p = Quests.progress(q, Game.stats)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Pic(if (q.done) "check" else q.icon, if (compact) 26.dp else 32.dp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        q.text, color = if (q.done) Void.dim else Void.text, fontSize = if (compact) 11.sp else 13.sp,
                        textDecoration = if (q.done) TextDecoration.LineThrough else null, maxLines = if (compact) 1 else 2, overflow = TextOverflow.Ellipsis,
                    )
                    LinearProgressIndicator(
                        progress = { p / q.goal.toFloat() }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                        color = if (q.done) Void.ok else Void.cyan, trackColor = Void.panelAlt,
                    )
                }
                if (!compact) Text("$p/${q.goal}", color = Void.dim, fontSize = 12.sp)
                Coins(q.reward, fontSize = 12.sp, icon = 15.dp)
            }
        }
    }
}
