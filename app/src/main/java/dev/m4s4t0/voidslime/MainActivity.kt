package dev.m4s4t0.voidslime

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import dev.m4s4t0.voidslime.ui.SlimeAppUi
import dev.m4s4t0.voidui.VoidTheme

class MainActivity : ComponentActivity() {
    private val askNotify = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (intent?.action == Updater.ACTION_SHOW) Updater.showRequested = true
        setContent { VoidTheme { SlimeAppUi() } }
        // The update notification needs this; asked once, on first start.
        val sp = getSharedPreferences("updates", MODE_PRIVATE)
        if (!sp.getBoolean("askedNotify", false)) {
            sp.edit().putBoolean("askedNotify", true).apply()
            askNotify.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.action == Updater.ACTION_SHOW) Updater.showRequested = true
    }

    override fun onStart() {
        super.onStart()
        Updater.foreground = true
        Updater.check(this, quiet = true)
        UpdateJob.schedule(this)
    }

    override fun onStop() {
        super.onStop()
        Updater.foreground = false
        if (!isChangingConfigurations) Themes.syncLauncherIcon()
    }
}
