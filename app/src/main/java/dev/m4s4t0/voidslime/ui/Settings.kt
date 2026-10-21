package dev.m4s4t0.voidslime.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.m4s4t0.voidslime.BuildConfig
import dev.m4s4t0.voidslime.Themes
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidui.BUILTIN_THEMES
import dev.m4s4t0.voidui.Card
import dev.m4s4t0.voidui.PillButton
import dev.m4s4t0.voidui.Pills
import dev.m4s4t0.voidui.SectionTitle
import dev.m4s4t0.voidui.Toggle
import dev.m4s4t0.voidui.Void
import dev.m4s4t0.voidui.VoidTopBar

@Composable
fun SettingsScreen(state: AppState) {
    val theme by Themes.choice.collectAsState()
    val iconFollows by Themes.iconFollows.collectAsState()
    var confirmReset by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        VoidTopBar("Settings", { state.go(Screen.HOME) })
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 14.dp).navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Theme")
            Card {
                Pills(BUILTIN_THEMES.map { it.id to it.name }, theme) { tapFeedback(); Themes.choose(it) }
                Toggle("App icon matches the theme", "The launcher icon changes colour with the theme", iconFollows) { Themes.setIconFollows(it) }
            }
            SectionTitle("Feel")
            Card {
                Toggle("Sounds", "Squishes, crunches and pops", Game.sounds) { Game.sounds = it; Game.saveSettings() }
                Toggle("Haptics", "Little buzzes as you play", Game.haptics) { Game.haptics = it; Game.saveSettings(); tapFeedback() }
                Text("Volume", color = Void.text, fontSize = 14.sp)
                Slider(
                    Game.volume, { Game.volume = it }, onValueChangeFinished = { Game.saveSettings(); Audio.play(Snd.SQUISH, 1f) },
                    colors = SliderDefaults.colors(thumbColor = Void.cyan, activeTrackColor = Void.cyan, inactiveTrackColor = Void.panelAlt),
                )
            }
            SectionTitle("Updates")
            UpdatesCard()
            SectionTitle("Progress")
            Card {
                Text("Level ${Game.level} · 🪙 ${Game.coins} · ${plural(Game.jars.size, "jar")} · ${plural(Game.earned.size, "sticker")}", color = Void.dim, fontSize = 13.sp)
                PillButton("Start over", false) { confirmReset = true }
            }
            Text("Void Slime ${BuildConfig.VERSION_NAME} · part of the VOID family", color = Void.dim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        }
    }
    if (confirmReset) {
        AlertDialog(
            onDismissRequest = { confirmReset = false },
            containerColor = Void.panel,
            title = { Text("Start over?", color = Void.text) },
            text = { Text("Coins, jars, stickers and supplies all go back to the beginning. This can't be undone.", color = Void.dim) },
            confirmButton = { PillButton("Start over", true) { Game.reset(); confirmReset = false; state.go(Screen.HOME) } },
            dismissButton = { PillButton("Cancel", false) { confirmReset = false } },
        )
    }
}
