package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Quests
import dev.m4s4t0.voidui.Void

/** Home's card with today's three challenges and how far along each one is. */
@Composable
fun ChallengesCard(onPlay: () -> Unit) {
    val qs = Game.quests
    if (qs.isEmpty()) return
    val done = qs.count { it.done }
    Column(
        Modifier.fillMaxWidth().squishy(onPlay).clip(RoundedCornerShape(20.dp)).background(Void.panel.copy(alpha = 0.9f))
            .border(1.dp, if (done == qs.size) Void.ok else Void.border, RoundedCornerShape(20.dp)).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("🎯 Today's challenges", color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            Text(if (done == qs.size) "All done! 🎉" else "$done of ${qs.size} · all 3: +${Quests.ALL_BONUS} 🪙", color = if (done == qs.size) Void.ok else Void.dim, fontSize = 12.sp)
        }
        for (q in qs) {
            val p = Quests.progress(q, Game.stats)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(if (q.done) "✅" else q.emoji, fontSize = 20.sp)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        q.text, color = if (q.done) Void.dim else Void.text, fontSize = 13.sp,
                        textDecoration = if (q.done) TextDecoration.LineThrough else null,
                    )
                    LinearProgressIndicator(
                        progress = { p / q.goal.toFloat() }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(50)),
                        color = if (q.done) Void.ok else Void.cyan, trackColor = Void.panelAlt,
                    )
                }
                Text(if (q.done) "✓" else "$p/${q.goal}", color = Void.dim, fontSize = 12.sp)
                Text("🪙 ${q.reward}", color = Void.warn, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
