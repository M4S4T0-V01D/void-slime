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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
    Column(Modifier.fillMaxSize().safe()) {
        GameBar("Settings", { state.go(Screen.HOME) }, icon = "gear")
        Row(Modifier.fillMaxSize().padding(horizontal = 14.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Sound and music")
            Card {
                Toggle("Music", "A little song for each world", Game.music) { Game.music = it; Game.saveSettings() }
                Text("Music volume", color = Void.text, fontSize = 14.sp)
                Slider(
                    Game.musicVolume, { Game.musicVolume = it }, onValueChangeFinished = { Game.saveSettings() }, enabled = Game.music,
                    colors = SliderDefaults.colors(thumbColor = Void.violet, activeTrackColor = Void.violet, inactiveTrackColor = Void.panelAlt),
                )
                Toggle("Sounds", "Squishes, crunches and pops", Game.sounds) { Game.sounds = it; Game.saveSettings() }
                Text("Sound volume", color = Void.text, fontSize = 14.sp)
                Slider(
                    Game.volume, { Game.volume = it }, onValueChangeFinished = { Game.saveSettings(); Audio.play(Snd.SQUISH, 1f) },
                    colors = SliderDefaults.colors(thumbColor = Void.cyan, activeTrackColor = Void.cyan, inactiveTrackColor = Void.panelAlt),
                )
                Toggle("Haptics", "Little buzzes as you play", Game.haptics) { Game.haptics = it; Game.saveSettings(); tapFeedback() }
            }
            SectionTitle("Theme")
            Card {
                Pills(BUILTIN_THEMES.map { it.id to it.name }, theme) { tapFeedback(); Themes.choose(it) }
                Toggle("App icon matches the theme", "The launcher icon changes colour with the theme", iconFollows) { Themes.setIconFollows(it) }
            }
            Spacer(Modifier.height(12.dp))
        }
        Column(
            Modifier.weight(1f).fillMaxHeight().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SectionTitle("Updates")
            UpdatesCard()
            SectionTitle("Progress")
            Card {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Level ${Game.level}", color = Void.dim, fontSize = 13.sp)
                    Coins(Game.coins, fontSize = 13.sp, icon = 16.dp)
                    Text("${plural(Game.jars.size, "jar")} · ${plural(Game.earned.size, "sticker")}", color = Void.dim, fontSize = 13.sp)
                }
                PillButton("Start over", false) { confirmReset = true }
            }
            Text("Void Slime ${BuildConfig.VERSION_NAME} · part of the VOID family", color = Void.dim, fontSize = 12.sp, modifier = Modifier.padding(vertical = 12.dp))
        }
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
