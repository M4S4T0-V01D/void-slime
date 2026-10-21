package dev.m4s4t0.voidslime.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.gl.Look
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Levels
import dev.m4s4t0.voidui.Palette
import dev.m4s4t0.voidui.Void

fun Color.rgb() = floatArrayOf(red, green, blue)

fun Palette.look() = Look(bg.rgb(), accent.rgb(), accent2.rgb(), light)

fun argbColor(argb: Long) = Color(argb.toInt())

/** Every tap in the app: a soft tick and a tiny buzz. */
fun tapFeedback() {
    Audio.play(Snd.TAP, 0.6f)
    Haptics.buzz(Haptics.Kind.CLICK, 0.4f)
}

/** Squishes a little while held, springs back on release: buttons that feel like slime. */
@Composable
fun Modifier.squishy(onClick: () -> Unit): Modifier {
    val src = remember { MutableInteractionSource() }
    val pressed by src.collectIsPressedAsState()
    val s by animateFloatAsState(if (pressed) 0.92f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "squish")
    return this.scale(s).clickable(src, null) { tapFeedback(); onClick() }
}

@Composable
fun CoinChip(modifier: Modifier = Modifier) {
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Void.panel.copy(alpha = 0.85f)).border(1.dp, Void.border, RoundedCornerShape(50)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text("🪙", fontSize = 15.sp)
        Text("${Game.coins}", color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LevelChip(modifier: Modifier = Modifier) {
    val (lvl, into, need) = Levels.of(Game.xp)
    Row(
        modifier.clip(RoundedCornerShape(50)).background(Void.panel.copy(alpha = 0.85f)).border(1.dp, Void.border, RoundedCornerShape(50)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("Lv $lvl", color = Void.text, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.size(width = 54.dp, height = 7.dp).clip(RoundedCornerShape(50)).background(Void.panelAlt)) {
            Box(
                Modifier.size(width = (54f * into / need).dp, height = 7.dp).clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Void.violet, Void.cyan))),
            )
        }
    }
}

/** A big friendly tile: emoji, title, a line under it. */
@Composable
fun Tile(emoji: String, title: String, sub: String?, modifier: Modifier = Modifier, accent: Color = Void.violet, badge: String? = null, onClick: () -> Unit) {
    Box(modifier) {
        Column(
            Modifier.fillMaxWidth().heightIn(min = 118.dp).squishy(onClick).clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(accent.copy(alpha = 0.28f), Void.panel.copy(alpha = 0.9f))))
                .border(1.dp, accent.copy(alpha = 0.45f), RoundedCornerShape(22.dp))
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(emoji, fontSize = 30.sp)
            Text(title, color = Void.text, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1)
            if (sub != null) Text(sub, color = Void.dim, fontSize = 12.sp, maxLines = 2)
        }
        if (badge != null) {
            Text(
                badge, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).clip(RoundedCornerShape(50)).background(Void.bad).padding(horizontal = 7.dp, vertical = 2.dp),
            )
        }
    }
}

/** A round swatch / emoji choice for the lab and shop: ring when picked, padlock and price when locked. */
@Composable
fun Choice(
    label: String,
    selected: Boolean,
    locked: Boolean,
    price: Int,
    modifier: Modifier = Modifier,
    size: Dp = 58.dp,
    fill: Color? = null,
    emoji: String? = null,
    onClick: () -> Unit,
) {
    Column(modifier.squishy(onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Box(
            Modifier.size(size).clip(CircleShape)
                .border(if (selected) 3.dp else 1.dp, if (selected) Void.cyan else Void.border, CircleShape)
                .padding(if (selected) 5.dp else 3.dp).clip(CircleShape)
                .background(
                    fill?.let { Brush.radialGradient(listOf(lerpWhite(it, 0.45f), it, darken(it, 0.75f))) }
                        ?: Brush.linearGradient(listOf(Void.panelAlt, Void.panelAlt)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (emoji != null) Text(emoji, fontSize = (size.value * 0.42f).sp)
            if (locked) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.45f)), contentAlignment = Alignment.Center) {
                    Text("🔒", fontSize = 16.sp)
                }
            }
        }
        Text(
            if (locked) "🪙 $price" else label, color = if (locked) Void.warn else Void.dim, fontSize = 11.sp,
            maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 2.dp),
        )
    }
}

fun lerpWhite(c: Color, t: Float) = Color(c.red + (1 - c.red) * t, c.green + (1 - c.green) * t, c.blue + (1 - c.blue) * t, c.alpha)
fun darken(c: Color, t: Float) = Color(c.red * t, c.green * t, c.blue * t, c.alpha)

/** "1 jar", "3 jars". */
fun plural(n: Int, word: String) = if (n == 1) "$n $word" else "$n ${word}s"
