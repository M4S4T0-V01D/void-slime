package dev.m4s4t0.voidslime.ui

import android.view.View
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import dev.m4s4t0.voidslime.Updater
import dev.m4s4t0.voidslime.audio.Audio
import dev.m4s4t0.voidslime.audio.Haptics
import dev.m4s4t0.voidslime.audio.Snd
import dev.m4s4t0.voidslime.audio.Song
import dev.m4s4t0.voidslime.gl.SceneListener
import dev.m4s4t0.voidslime.gl.SceneMode
import dev.m4s4t0.voidslime.gl.SlimeView
import dev.m4s4t0.voidslime.gl.Tool
import dev.m4s4t0.voidslime.model.Game
import dev.m4s4t0.voidslime.model.GameEvent
import dev.m4s4t0.voidslime.model.Quests
import dev.m4s4t0.voidslime.model.Recipe
import dev.m4s4t0.voidslime.model.Stat
import dev.m4s4t0.voidui.CosmicBackground
import dev.m4s4t0.voidui.Void
import kotlinx.coroutines.delay

enum class Screen { HOME, PLAY, LAB, RUSH, RACE, SHOP, SHELF, STICKERS, SETTINGS }

/** Shared between the screens and the 3D view. */
class AppState(val view: SlimeView) {
    var screen by mutableStateOf(Screen.HOME)
    var tool by mutableStateOf(Tool.POKE)
    var lights by mutableStateOf(true)
    var zen by mutableStateOf(false)
    val lab = LabState()
    val rush = RushState()
    /** A recipe the shop wants made: the lab opens with it pinned. */
    var pinnedOrder by mutableStateOf<String?>(null)
    /** Which shop tab to open on (Home's Worlds button opens straight to worlds). */
    var shopTab by mutableStateOf(ShopTab.ORDERS)
    /** Pops of celebration on top of everything. */
    val toasts = mutableStateListOf<Toast>()

    fun go(s: Screen) {
        if (s == screen) return
        screen = s
    }

    fun celebrate() = view.scene { celebrate() }
}

class RushState {
    var score by mutableStateOf(0)
    var left by mutableStateOf(0)
    var running by mutableStateOf(false)
    var result by mutableStateOf<Int?>(null)
}

/** A celebration card: art, a title, a line, and optionally stars and coins earned. */
data class Toast(val id: Long, val icon: String, val title: String, val sub: String, val coins: Int = 0, val stars: Int = 0)

private val SCENE_SCREENS = setOf(Screen.HOME, Screen.PLAY, Screen.LAB, Screen.RUSH)

@Composable
fun SlimeAppUi() {
    val ctx = LocalContext.current
    val holder = remember { arrayOfNulls<AppState>(1) }
    val state = remember {
        val listener = object : SceneListener {
            override fun onStat(stat: String) = Game.add(stat)
            override fun onStir(progress: Float) { holder[0]?.lab?.stir = progress }
            override fun onRushPop(points: Int) { holder[0]?.rush?.let { if (it.running) it.score += points } }
        }
        AppState(SlimeView(ctx, listener)).also { holder[0] = it }
    }
    val view = state.view
    val screen = state.screen

    // Pause the GL loop and the mixer while the app is in the background.
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e ->
            when (e) {
                Lifecycle.Event.ON_RESUME -> view.onResume()
                Lifecycle.Event.ON_PAUSE -> { view.onPause(); Game.flush() }
                Lifecycle.Event.ON_START -> Audio.start()
                Lifecycle.Event.ON_STOP -> { Audio.stop(); Game.saveAll() }
                else -> Unit
            }
        }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    // Keep the scene in step with the UI.
    val look = Void.palette.look()
    LaunchedEffect(look.bg.toList(), look.accent.toList()) { view.scene { this.look = look } }
    LaunchedEffect(Game.tray) { view.renderer.tray = Game.tray }
    LaunchedEffect(Game.world) { view.renderer.world = Game.world }
    LaunchedEffect(Game.plate) { view.renderer.plate = Game.plate }
    LaunchedEffect(Game.vessel) { view.renderer.vessel = Game.vessel }
    // Each world has its own little song; the rush has a faster one.
    LaunchedEffect(screen, Game.world) { Audio.song = if (screen == Screen.RUSH) Song.RUSH else Song.valueOf(Game.world.name) }
    val recipe: Recipe = if (screen == Screen.LAB) state.lab.draft else Game.current
    LaunchedEffect(recipe) { val r = recipe; view.scene { setRecipe(r) } }
    LaunchedEffect(screen, state.tool, state.lights) {
        val mode = when (screen) {
            Screen.HOME -> SceneMode.HOME
            Screen.LAB -> SceneMode.LAB
            Screen.RUSH -> SceneMode.RUSH
            else -> SceneMode.PLAY
        }
        val tool = if (screen == Screen.LAB) state.lab.tool else state.tool
        val lights = screen != Screen.PLAY || state.lights
        val zoom = when (screen) { Screen.LAB -> 0.92f; Screen.HOME -> 1f; else -> 0.95f }
        view.scene {
            this.mode = mode; this.tool = tool; lightsOn = lights; this.zoom = zoom
            if (mode != SceneMode.LAB) setForm(1f)
        }
    }
    LaunchedEffect(state.lab.tool, screen) { if (screen == Screen.LAB) { val t = state.lab.tool; view.scene { tool = t } } }
    LaunchedEffect(state.zen, screen) { Audio.ambient = state.zen && screen == Screen.PLAY }
    // The update notification was tapped: the update is in Settings.
    LaunchedEffect(Updater.showRequested) {
        if (Updater.showRequested) { Updater.showRequested = false; Updater.cancelNotification(ctx); state.go(Screen.SETTINGS) }
    }

    // Time played, and saving now and then.
    LaunchedEffect(Unit) {
        while (true) {
            delay(10_000)
            if (holder[0]?.screen in SCENE_SCREENS) Game.add(Stat.SECONDS, 10)
            // Rush customers who waited too long leave; new ones come in.
            if (Game.topUpOrders()) holder[0]?.toasts?.add(Toast(System.nanoTime(), "timer", "A rush customer gave up waiting", "New customers are at the shop"))
            Game.flush()
        }
    }

    // Celebrations.
    LaunchedEffect(Unit) {
        Game.events.collect { e ->
            when (e) {
                is GameEvent.StickerEarned -> {
                    Audio.play(Snd.STICKER, 0.9f)
                    Haptics.buzz(Haptics.Kind.SUCCESS)
                    state.toasts += Toast(System.nanoTime(), e.sticker.icon, "Sticker: ${e.sticker.title}", e.sticker.desc, coins = e.sticker.reward)
                    state.celebrate()
                }
                is GameEvent.LevelUp -> {
                    Audio.play(Snd.LEVEL, 0.9f)
                    Haptics.buzz(Haptics.Kind.SUCCESS)
                    state.toasts += Toast(System.nanoTime(), "level", "Level ${e.level}!", "Level-up bonus", coins = 25)
                    state.celebrate()
                }
                is GameEvent.QuestDone -> {
                    Audio.play(Snd.STICKER, 0.8f)
                    Haptics.buzz(Haptics.Kind.SUCCESS)
                    state.toasts += Toast(System.nanoTime(), e.quest.icon, "Challenge done!", e.quest.text, coins = e.quest.reward)
                    if (e.allDone) state.toasts += Toast(System.nanoTime() + 1, "trophy", "All three done today!", "Bonus! See you tomorrow", coins = Quests.ALL_BONUS)
                }
                is GameEvent.Coins -> if (e.amount >= 1) Audio.play(Snd.COIN, if (e.amount > 3) 0.7f else 0.35f, 1f, 0f, 120)
            }
        }
    }

    BackHandler(screen != Screen.HOME) {
        when {
            screen == Screen.PLAY && state.zen -> state.zen = false
            else -> state.go(Screen.HOME)
        }
    }

    BoxWithConstraints(Modifier.fillMaxSize().background(Void.bg)) {
        val inScene = screen in SCENE_SCREENS
        if (!inScene) CosmicBackground(Modifier.fillMaxSize())
        // The slime always fills the screen; the panels float over it, and the camera centres the dish in what's left.
        val w = maxWidth.value; val h = maxHeight.value
        val focus = when (screen) {
            Screen.HOME -> floatArrayOf(HOME_SIDE / w, 0.1f, 1f - HOME_SIDE / w, 0.9f)
            Screen.PLAY -> if (state.zen) floatArrayOf(0f, 0f, 1f, 1f) else floatArrayOf(0f, 60f / h, 1f - PLAY_RAIL / w, 1f)
            Screen.LAB -> floatArrayOf(0f, 60f / h, 1f - labPanel(w) / w, 1f)
            else -> floatArrayOf(0.05f, 0.16f, 0.95f, 1f)
        }
        LaunchedEffect(focus.toList()) { view.scene { this.focus = focus } }
        AndroidView(
            factory = { view },
            modifier = Modifier.fillMaxSize(),
            update = { it.visibility = if (inScene) View.VISIBLE else View.GONE },
        )
        when (screen) {
            Screen.HOME -> HomeScreen(state)
            Screen.PLAY -> PlayScreen(state)
            Screen.LAB -> LabScreen(state)
            Screen.RUSH -> RushScreen(state)
            Screen.RACE -> RaceScreen(state)
            Screen.SHOP -> ShopScreen(state)
            Screen.SHELF -> ShelfScreen(state)
            Screen.STICKERS -> StickersScreen(state)
            Screen.SETTINGS -> SettingsScreen(state)
        }
        Toasts(state, Modifier.align(Alignment.TopCenter))
    }
}

@Composable
private fun Toasts(state: AppState, modifier: Modifier) {
    val t = state.toasts.firstOrNull()
    LaunchedEffect(t?.id) {
        if (t != null) {
            delay(2800)
            state.toasts.remove(t)
        }
    }
    AnimatedVisibility(
        t != null,
        modifier.safe().padding(top = 18.dp),
        enter = slideInVertically { -it } + fadeIn() + scaleIn(initialScale = 0.8f),
        exit = slideOutVertically { -it } + fadeOut(),
    ) {
        val shown = t ?: return@AnimatedVisibility
        Row(
            Modifier.widthIn(max = 460.dp).clip(RoundedCornerShape(26.dp)).background(Void.panel.copy(alpha = 0.96f))
                .border(2.dp, Void.cyan.copy(alpha = 0.7f), RoundedCornerShape(26.dp)).padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Pic(shown.icon, 60.dp, Modifier.bob(3.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (shown.stars > 0) Stars(shown.stars, size = 22.dp)
                Text(shown.title, color = Void.text, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                if (shown.sub.isNotEmpty()) Text(shown.sub, color = Void.dim, fontSize = 13.sp)
                if (shown.coins > 0) Coins("+${shown.coins}", fontSize = 15.sp)
            }
        }
    }
}

/** Widths (dp) of the panels beside the slime, so the camera can centre it in the space between. */
const val HOME_SIDE = 250f
const val PLAY_RAIL = 96f
fun labPanel(w: Float) = (w * 0.46f).coerceIn(330f, 480f)
