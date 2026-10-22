package dev.m4s4t0.voidslime.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.R
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

/** Keeps content clear of the camera cutout and any bars that are showing. */
@Composable
fun Modifier.safe() = windowInsetsPadding(WindowInsets.safeDrawing)

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
    val s by animateFloatAsState(if (pressed) 0.9f else 1f, spring(dampingRatio = 0.35f, stiffness = 500f), label = "squish")
    return this.scale(s).clickable(src, null) { tapFeedback(); onClick() }
}

/** A gentle up-and-down float, so things on screen feel alive. [phase] staggers neighbours. */
@Composable
fun Modifier.bob(amount: Dp = 3.dp, phase: Int = 0, periodMs: Int = 1800): Modifier {
    val t = rememberInfiniteTransition(label = "bob")
    val y by t.animateFloat(
        -1f, 1f, infiniteRepeatable(tween(periodMs, delayMillis = 0, easing = FastOutSlowInEasing), RepeatMode.Reverse, initialStartOffset = androidx.compose.animation.core.StartOffset(phase * 260)),
        label = "bobY",
    )
    return graphicsLayer { translationY = y * amount.toPx() }
}

/** A frosted panel over the 3D scene. */
fun Modifier.glass(shape: Shape = RoundedCornerShape(22.dp), border: Color = Void.border, alpha: Float = 0.82f) =
    clip(shape).background(Void.panel.copy(alpha = alpha)).border(1.dp, border, shape)

/** A piece of the game's art by name (see [Art]). */
@Composable
fun Pic(name: String, size: Dp, modifier: Modifier = Modifier, alpha: Float = 1f) {
    val m = if (size == Dp.Unspecified) modifier else modifier.size(size)
    Image(painterResource(Art.ids[name] ?: R.drawable.ic_star), null, m.alpha(alpha))
}

/** A coin and an amount. */
@Composable
fun Coins(amount: Any, modifier: Modifier = Modifier, fontSize: TextUnit = 14.sp, color: Color = Void.warn, icon: Dp = 18.dp) {
    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Pic("coin", icon)
        Text("$amount", color = color, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1)
    }
}

/** A row of [n] stars out of [of]. */
@Composable
fun Stars(n: Int, of: Int = 3, size: Dp = 20.dp) {
    Row(horizontalArrangement = Arrangement.spacedBy(1.dp)) {
        for (i in 0 until of) Pic(if (i < n) "star" else "star_off", size)
    }
}

@Composable
fun CoinChip(modifier: Modifier = Modifier) {
    Row(
        modifier.glass(RoundedCornerShape(50), alpha = 0.85f).padding(start = 6.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Pic("coins", 24.dp)
        Text("${Game.coins}", color = Void.text, fontSize = 15.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun LevelChip(modifier: Modifier = Modifier) {
    val (lvl, into, need) = Levels.of(Game.xp)
    Row(
        modifier.glass(RoundedCornerShape(50), alpha = 0.85f).padding(start = 6.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Pic("level", 24.dp)
        Text("$lvl", color = Void.text, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Box(Modifier.size(width = 50.dp, height = 7.dp).clip(RoundedCornerShape(50)).background(Void.panelAlt)) {
            Box(
                Modifier.size(width = (50f * into / need).dp, height = 7.dp).clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Void.violet, Void.cyan))),
            )
        }
    }
}

/** A round glass button with a picture: settings, lights, zen and the like. */
@Composable
fun RoundIcon(icon: String, modifier: Modifier = Modifier, size: Dp = 48.dp, on: Boolean = false, onClick: () -> Unit) {
    Box(
        modifier.size(size).squishy(onClick).glass(CircleShape, if (on) Void.cyan else Void.border, 0.8f),
        contentAlignment = Alignment.Center,
    ) { Pic(icon, size * 0.68f) }
}

/** The bar along the top of each screen in landscape: back, title, then [actions] on the right. */
@Composable
fun GameBar(title: String, onBack: () -> Unit, modifier: Modifier = Modifier, icon: String? = null, subtitle: String? = null, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(44.dp).squishy(onBack).glass(CircleShape, alpha = 0.85f), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = Void.text)
        }
        // The title sits on glass so it reads over any world.
        Row(
            Modifier.weight(1f, fill = false).glass(RoundedCornerShape(50), alpha = 0.78f).padding(start = if (icon != null) 6.dp else 16.dp, end = 18.dp, top = 3.dp, bottom = 3.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (icon != null) Pic(icon, 36.dp)
            Column {
                Text(title, color = Void.text, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (subtitle != null) Text(subtitle, color = Void.dim, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Spacer(Modifier.weight(0.001f))
        actions()
    }
}

/** A big, bouncy menu button: art on the left, a title and a line, and a little badge if something's waiting. */
@Composable
fun BigButton(
    icon: String, title: String, sub: String?, modifier: Modifier = Modifier, accent: Color = Void.violet,
    badge: String? = null, phase: Int = 0, height: Dp = 74.dp, onClick: () -> Unit,
) {
    Box(modifier.bob(2.5.dp, phase)) {
        Row(
            Modifier.fillMaxWidth().height(height).squishy(onClick).clip(RoundedCornerShape(26.dp))
                .background(Brush.linearGradient(listOf(lerpWhite(accent, 0.15f).copy(alpha = 0.9f), darken(accent, 0.45f).copy(alpha = 0.92f))))
                .border(2.dp, lerpWhite(accent, 0.45f).copy(alpha = 0.8f), RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Pic(icon, height * 0.74f, Modifier.bob(2.dp, phase + 3, 1400))
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 19.sp, fontWeight = FontWeight.ExtraBold, maxLines = 1)
                if (sub != null) Text(sub, color = Color.White.copy(alpha = 0.82f), fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        if (badge != null) Badge(badge, Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 4.dp))
    }
}

/** A square menu tile: art over a label. */
@Composable
fun Tile(icon: String, title: String, modifier: Modifier = Modifier, accent: Color = Void.violet, badge: String? = null, phase: Int = 0, onClick: () -> Unit) {
    Box(modifier.bob(2.dp, phase)) {
        Column(
            Modifier.fillMaxWidth().squishy(onClick).clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(accent.copy(alpha = 0.55f), Void.panel.copy(alpha = 0.92f))))
                .border(1.5.dp, accent.copy(alpha = 0.7f), RoundedCornerShape(22.dp))
                .padding(vertical = 8.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Pic(icon, 46.dp)
            Text(title, color = Void.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
        if (badge != null) Badge(badge, Modifier.align(Alignment.TopEnd).padding(4.dp))
    }
}

@Composable
fun Badge(text: String, modifier: Modifier = Modifier) {
    Text(
        text, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold,
        modifier = modifier.clip(RoundedCornerShape(50)).background(Void.bad).border(1.5.dp, Color.White, RoundedCornerShape(50)).padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

/** A round swatch or picture choice for the lab and shop: ring when picked, padlock and price when locked. */
@Composable
fun Choice(
    label: String,
    selected: Boolean,
    locked: Boolean,
    price: Int,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    fill: Color? = null,
    icon: String? = null,
    /** The level it opens at (shown instead of the price until then). */
    need: Int = 1,
    onClick: () -> Unit,
) {
    Column(modifier.squishy(onClick), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(
            Modifier.size(size).clip(CircleShape)
                .border(if (selected) 3.dp else 1.dp, if (selected) Void.cyan else Void.border, CircleShape)
                .padding(if (selected) 4.dp else 2.dp).clip(CircleShape)
                .background(
                    fill?.let { Brush.radialGradient(listOf(lerpWhite(it, 0.45f), it, darken(it, 0.75f))) }
                        ?: Brush.radialGradient(listOf(Void.panelAlt, Void.panel)),
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (icon != null) Pic(icon, size * 0.8f, alpha = if (locked) 0.55f else 1f)
            if (locked) {
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.32f)), contentAlignment = Alignment.BottomEnd) {
                    Pic("lock", size * 0.36f, Modifier.padding(2.dp))
                }
            }
        }
        if (locked && Game.level < need) LevelTag(need)
        else if (locked) Coins(price, fontSize = 11.sp, icon = 13.dp)
        else Text(label, color = if (selected) Void.text else Void.dim, fontSize = 11.sp, maxLines = 1, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 2.dp))
    }
}

/** A bold picture card for the shop's worlds, dishes and molds: big art, name, and owned / price / in use. */
@Composable
fun ItemCard(icon: String, title: String, sub: String?, selected: Boolean, owned: Boolean, price: Int, modifier: Modifier = Modifier, art: Dp = 84.dp, ownedLabel: String = "Use", need: Int = 1, onClick: () -> Unit) {
    Column(
        modifier.squishy(onClick).glass(RoundedCornerShape(20.dp), if (selected) Void.cyan else if (owned) Void.violet.copy(alpha = 0.6f) else Void.border, 0.86f)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Pic(icon, art, alpha = if (owned) 1f else 0.7f)
            if (!owned) Pic("lock", 24.dp)
            else if (selected) Pic("check", 24.dp)
        }
        Text(title, color = Void.text, fontSize = 13.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (sub != null) Text(sub, color = Void.dim, fontSize = 10.sp, maxLines = 2, textAlign = TextAlign.Center, lineHeight = 12.sp)
        when {
            selected -> Text("In use", color = Void.cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            owned -> Text(ownedLabel, color = Void.text, fontSize = 11.sp)
            Game.level < need -> LevelTag(need)
            else -> Coins(price, fontSize = 12.sp, icon = 15.dp)
        }
    }
}

/** "Lv 4": what you're still too low a level for. */
@Composable
fun LevelTag(level: Int, modifier: Modifier = Modifier, fontSize: TextUnit = 11.sp) {
    Text(
        "Lv $level", color = Void.violet, fontSize = fontSize, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = modifier.clip(RoundedCornerShape(50)).background(Void.violet.copy(alpha = 0.18f)).padding(horizontal = 7.dp, vertical = 1.dp),
    )
}

fun lerpWhite(c: Color, t: Float) = Color(c.red + (1 - c.red) * t, c.green + (1 - c.green) * t, c.blue + (1 - c.blue) * t, c.alpha)
fun darken(c: Color, t: Float) = Color(c.red * t, c.green * t, c.blue * t, c.alpha)

/** "1 jar", "3 jars". */
fun plural(n: Int, word: String) = if (n == 1) "$n $word" else "$n ${word}s"
