package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.model.AddIn
import dev.m4s4t0.voidslime.model.Base
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.Glitter
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.colorOf
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Pills
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun ShelfScreen(state: AppState) {
    var sort by remember { mutableStateOf(0) }
    var open by remember { mutableStateOf<Recipe?>(null) }
    val jars = when (sort) {
        1 -> Game.jars.sortedByDescending { it.plays }
        2 -> Game.jars.sortedBy { it.name.lowercase() }
        else -> Game.jars.toList()
    }
    Column(Modifier.fillMaxSize()) {
        VoidTopBar("Shelf", { state.go(Screen.HOME) }, subtitle = "${plural(Game.jars.size, "slime")} in jars") { CoinChip(Modifier.padding(end = 8.dp)) }
        Column(Modifier.padding(horizontal = 14.dp)) {
            Pills(listOf(0 to "Newest", 1 to "Favourites", 2 to "A–Z"), sort) { tapFeedback(); sort = it }
        }
        LazyVerticalGrid(
            GridCells.Adaptive(104.dp), Modifier.fillMaxSize().navigationBarsPadding(),
            contentPadding = PaddingValues(14.dp), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(jars, key = { it.id }) { r ->
                val current = r.id == Game.current.id
                Column(
                    Modifier.squishy { open = r }.clip(RoundedCornerShape(18.dp))
                        .background(Void.panel.copy(alpha = 0.85f))
                        .border(if (current) 2.dp else 1.dp, if (current) Void.cyan else Void.border, RoundedCornerShape(18.dp)).padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    JarPreview(r, Modifier.fillMaxWidth().height(108.dp))
                    Text(r.name, color = Void.text, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${r.base.emoji} ${r.base.title}", color = Void.dim, fontSize = 11.sp, maxLines = 1)
                }
            }
        }
    }
    open?.let { r ->
        AlertDialog(
            onDismissRequest = { open = null },
            containerColor = Void.panel,
            title = { Text(r.name, color = Void.text, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    JarPreview(r, Modifier.fillMaxWidth().height(150.dp))
                    Text("${r.base.emoji} ${r.base.title} · ${r.colors.joinToString { colorOf(it).title }}", color = Void.dim, fontSize = 13.sp)
                    if (r.addIns.isNotEmpty()) Text(r.addIns.keys.joinToString { "${it.emoji} ${it.title}" }, color = Void.dim, fontSize = 13.sp)
                    Text("Played ${r.plays} times", color = Void.dim, fontSize = 12.sp)
                }
            },
            confirmButton = { PillButton("Play", true) { Game.select(r.id); open = null; state.go(Screen.PLAY) } },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PillButton("🗑️", false) { Game.deleteJar(r.id); open = null }
                    PillButton("Close", false) { open = null }
                }
            },
        )
    }
}

/** A little jar of slime, drawn flat: swirled colours, a few add-ins and glitter, a shine on the glass. */
@Composable
fun JarPreview(r: Recipe, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width; val h = size.height
        val jw = minOf(w * 0.9f, h * 0.8f)
        val left = (w - jw) / 2f
        val top = h * 0.12f
        val jh = h * 0.86f
        val cs = r.colors.map { argbColor(colorOf(it).argb) }
        val alpha = if (r.base.mat.alpha < 0.9f) 0.7f else 1f
        // Lid.
        drawRoundRect(Brush.verticalGradient(listOf(Color(0xFFB9A6FF), Color(0xFF6D55C9))), Offset(left + jw * 0.08f, top - h * 0.1f), Size(jw * 0.84f, h * 0.12f), CornerRadius(8f, 8f))
        // Slime.
        val fillTop = top + jh * 0.28f
        val clip = Path().apply { addRoundRect(androidx.compose.ui.geometry.RoundRect(left, top, left + jw, top + jh, CornerRadius(jw * 0.18f))) }
        drawContext.canvas.save()
        drawContext.canvas.clipPath(clip)
        val base = when (r.base) { Base.CHROME -> listOf(Color.White, cs[0], Color.DarkGray, cs[0]); else -> cs + cs.first() }
        drawRect(Brush.verticalGradient(base.map { it.copy(alpha = alpha) }, startY = fillTop, endY = top + jh), Offset(left, fillTop), Size(jw, jh))
        // Swirls.
        val rnd = Random(r.id.hashCode())
        if (cs.size > 1) for (i in 0 until 5) {
            val c = cs[i % cs.size]
            val path = Path()
            val y0 = fillTop + jh * (0.1f + rnd.nextFloat() * 0.55f)
            path.moveTo(left, y0)
            for (k in 1..8) path.lineTo(left + jw * k / 8f, y0 + sin(k * 1.3f + i) * jh * 0.05f)
            drawPath(path, c.copy(alpha = 0.8f * alpha), style = Stroke(width = jh * 0.07f))
        }
        if (r.base == Base.HOLO) drawRect(Brush.linearGradient(listOf(Color(0x55FF7EC8), Color(0x5522E3FF), Color(0x55FFE45C))), Offset(left, fillTop), Size(jw, jh))
        // Add-ins and glitter.
        for ((a, lvl) in r.addIns) {
            val tints = a.tints.ifEmpty { AddIn.BRIGHT }
            repeat(4 + lvl * 4) {
                val c = argbColor(tints[rnd.nextInt(tints.size)])
                val p = Offset(left + jw * (0.1f + rnd.nextFloat() * 0.8f), fillTop + jh * (0.06f + rnd.nextFloat() * 0.62f))
                val s = jw * a.size * 0.32f
                if (a.shape.ordinal == 3) {
                    val ang = rnd.nextFloat() * 6.28f
                    drawLine(c, p - Offset(cos(ang) * s, sin(ang) * s), p + Offset(cos(ang) * s, sin(ang) * s), strokeWidth = s * 0.7f)
                } else drawCircle(c, s, p)
            }
        }
        if (r.glitter != Glitter.NONE) repeat(26) {
            val c = if (r.glitter.argb == 0L) argbColor(AddIn.BRIGHT[rnd.nextInt(AddIn.BRIGHT.size)]) else argbColor(r.glitter.argb)
            drawCircle(c, jw * 0.012f, Offset(left + jw * rnd.nextFloat(), fillTop + (top + jh - fillTop) * rnd.nextFloat()))
        }
        drawContext.canvas.restore()
        // Glass.
        drawRoundRect(Color.White.copy(alpha = 0.08f), Offset(left, top), Size(jw, jh), CornerRadius(jw * 0.18f))
        drawRoundRect(Color.White.copy(alpha = 0.5f), Offset(left, top), Size(jw, jh), CornerRadius(jw * 0.18f), style = Stroke(2.5f))
        drawRoundRect(Color.White.copy(alpha = 0.35f), Offset(left + jw * 0.1f, top + jh * 0.1f), Size(jw * 0.07f, jh * 0.55f), CornerRadius(20f))
    }
}
