package ch.awakened.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.CountButton
import ch.awakened.app.ui.components.GlowProgressBar
import ch.awakened.app.ui.components.KeyValue
import ch.awakened.app.ui.components.SystemBackground
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys
import kotlinx.coroutines.delay

/** Countdown bis zum automatischen Entsperren. */
@Composable
fun rememberNow(): Long {
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) { while (true) { delay(1000); now = System.currentTimeMillis() } }
    return now
}

@Composable
fun PenaltyHeader(state: PenaltyState, now: Long) {
    SystemWindow(title = "PENALTY ZONE", accent = Sys.Red, icon = "!") {
        Text(
            "WARNUNG", color = Sys.Red, style = MaterialTheme.typography.displayMedium.copy(fontSize = 44.sp),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        SystemText(state.reasonEnum.label, align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(6.dp))
        SystemText(
            "Die gesperrten Apps bleiben blockiert, bis die Straf-Quest erfüllt ist oder die Sperrzeit abläuft.",
            color = Sys.Muted, align = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        val left = ((state.unlockAt - now) / 1000).coerceAtLeast(0)
        Text(
            "Automatisches Ende in", color = Sys.Muted, style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Text(
            DateUtil.formatDuration(left), color = Sys.Text, style = MaterialTheme.typography.displayMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
fun PenaltyQuestCard(state: PenaltyState, onBurpees: (Int) -> Unit, onCardio: (Int) -> Unit) {
    SystemWindow(title = "STRAF-QUEST", accent = Sys.Red, icon = "☠") {
        SystemText("Überlebe die Straf-Quest, um die Sperre sofort aufzuheben.", color = Sys.Muted)
        Spacer(Modifier.height(10.dp))
        KeyValue("Burpees", "${state.burpeesDone} / ${state.burpeesTarget}", valueColor = if (state.burpeesDone >= state.burpeesTarget) Sys.Green else Sys.Text)
        GlowProgressBar(state.burpeesDone / state.burpeesTarget.coerceAtLeast(1).toFloat(), color = Sys.Red)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountButton("−1", { onBurpees(-1) }, accent = Sys.Red, enabled = state.burpeesDone > 0)
            CountButton("+1", { onBurpees(1) }, accent = Sys.Red)
            CountButton("+5", { onBurpees(5) }, accent = Sys.Red)
            CountButton("+10", { onBurpees(10) }, accent = Sys.Red)
        }
        Spacer(Modifier.height(14.dp))
        KeyValue("Ausdauer", "${state.cardioDone} / ${state.cardioTarget} min", valueColor = if (state.cardioDone >= state.cardioTarget) Sys.Green else Sys.Text)
        GlowProgressBar(state.cardioDone / state.cardioTarget.coerceAtLeast(1).toFloat(), color = Sys.Red)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountButton("−1", { onCardio(-1) }, accent = Sys.Red, enabled = state.cardioDone > 0)
            CountButton("+1", { onCardio(1) }, accent = Sys.Red)
            CountButton("+5", { onCardio(5) }, accent = Sys.Red)
            CountButton("+10", { onCardio(10) }, accent = Sys.Red)
        }
    }
}

@Composable
fun PenaltyScreen(vm: MainViewModel, onBack: () -> Unit) {
    val state by vm.penalty.collectAsStateWithLifecycle()
    val now = rememberNow()
    SystemBackground(accent = Sys.Red, base = Sys.RedDeep) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (!state.active) {
                SystemWindow(title = "SYSTEM") {
                    SystemText("Keine aktive PENALTY ZONE. Erfülle weiterhin deine Daily Quest.")
                    Spacer(Modifier.height(12.dp))
                    SystemButton("Zurück", onBack, Modifier.fillMaxWidth())
                }
                return@Column
            }
            PenaltyHeader(state, now)
            PenaltyQuestCard(state, { vm.penaltyProgress(burpees = it) }, { vm.penaltyProgress(cardio = it) })
            SystemWindow(accent = Sys.Red, contentPadding = 12.dp) {
                SystemText("Telefon, SMS, Karten, Banking und AWAKENED selbst sind nie gesperrt.", color = Sys.Muted)
            }
            SystemButton("Zurück", onBack, Modifier.fillMaxWidth(), accent = Sys.Red)
            Spacer(Modifier.height(8.dp))
        }
    }
}
