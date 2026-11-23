package ch.awakened.app.penalty

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import ch.awakened.app.MainActivity
import ch.awakened.app.container
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.system.Feedback
import ch.awakened.app.ui.components.SystemBackground
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.screens.PenaltyHeader
import ch.awakened.app.ui.screens.PenaltyQuestCard
import ch.awakened.app.ui.screens.rememberNow
import ch.awakened.app.ui.theme.AwakenedTheme
import ch.awakened.app.ui.theme.Sys
import kotlinx.coroutines.launch

/** Roter Vollbild-Sperrbildschirm, der eine gesperrte App ersetzt. */
class PenaltyActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val preview = intent.getBooleanExtra(EXTRA_PREVIEW, false)
        val c = container
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = goHome()
        })
        lifecycleScope.launch {
            val s = c.settings.current()
            c.feedback.play(Feedback.Kind.ALERT, s.soundEnabled, s.vibrationEnabled)
        }
        setContent {
            AwakenedTheme {
                val real by c.settings.penalty.collectAsState(initial = PenaltyState(active = true))
                val state = if (preview) PenaltyState(
                    active = true, unlockAt = System.currentTimeMillis() + 12 * 3_600_000L,
                    burpeesTarget = 50, cardioTarget = 20,
                ) else real
                LaunchedEffect(state.active) { if (!state.active && !preview) finish() }
                val now = rememberNow()
                SystemBackground(accent = Sys.Red, base = Sys.RedDeep) {
                    Column(
                        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().navigationBarsPadding().padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Spacer(Modifier.height(12.dp))
                        PenaltyHeader(state, now)
                        if (preview) {
                            SystemWindow(accent = Sys.Red, contentPadding = 12.dp) {
                                SystemText("Vorschau: So sieht der Sperrbildschirm aus, wenn du eine gesperrte App öffnest.", color = Sys.Muted)
                            }
                        }
                        PenaltyQuestCard(
                            state,
                            { n -> if (!preview) lifecycleScope.launch { c.repository.addPenaltyProgress(burpees = n) } },
                            { n -> if (!preview) lifecycleScope.launch { c.repository.addPenaltyProgress(cardioMinutes = n) } },
                        )
                        SystemButton("AWAKENED öffnen", {
                            startActivity(
                                Intent(this@PenaltyActivity, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                                    .putExtra(MainActivity.EXTRA_ROUTE, "penalty"),
                            )
                            finish()
                        }, Modifier.fillMaxWidth(), accent = Sys.Red, filled = true)
                        SystemButton(if (preview) "Vorschau schliessen" else "Zum Startbildschirm", {
                            if (preview) finish() else goHome()
                        }, Modifier.fillMaxWidth(), accent = Sys.Red)
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }

    private fun goHome() {
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    companion object {
        const val EXTRA_BLOCKED = "blocked_package"
        const val EXTRA_PREVIEW = "preview"
    }
}
