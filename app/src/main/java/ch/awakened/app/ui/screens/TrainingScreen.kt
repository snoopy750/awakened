package ch.awakened.app.ui.screens

import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ch.awakened.app.MainActivity
import ch.awakened.app.domain.CardioType
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.integrations.SpotifyLauncher
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.TrainingPhase
import ch.awakened.app.ui.TrainingStep
import ch.awakened.app.ui.TrainingUi
import ch.awakened.app.ui.TrainingViewModel
import ch.awakened.app.ui.components.CountButton
import ch.awakened.app.ui.components.GlowProgressBar
import ch.awakened.app.ui.components.KeyValue
import ch.awakened.app.ui.components.SectionLabel
import ch.awakened.app.ui.components.SegmentedChoice
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemDialog
import ch.awakened.app.ui.components.SystemSwitchRow
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys

@Composable
fun TrainingScreen(vm: MainViewModel, tvm: TrainingViewModel = viewModel()) {
    val ui by tvm.ui.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current

    DisposableEffect(ui.phase) {
        view.keepScreenOn = ui.phase == TrainingPhase.RUNNING
        onDispose { view.keepScreenOn = false }
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when (ui.phase) {
            TrainingPhase.SETUP -> Setup(ui, tvm, hasPlaylist = !settings?.spotifyPlaylist.isNullOrBlank()) {
                tvm.start()
                val link = settings?.spotifyPlaylist.orEmpty()
                if (ui.startSpotify && link.isNotBlank()) {
                    val back = Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_NEW_TASK)
                    SpotifyLauncher.startPlaylist(context, link, back)?.let { vm.toast(it) }
                }
            }
            TrainingPhase.RUNNING -> Running(ui, tvm)
            TrainingPhase.DONE -> Done(ui) { tvm.abort() }
        }
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun Setup(ui: TrainingUi, tvm: TrainingViewModel, hasPlaylist: Boolean, onStart: () -> Unit) {
    SystemWindow(title = "TRAINING") {
        SystemText("Stelle deine Einheit zusammen. Erledigte Wiederholungen zählen automatisch für die Daily Quest.", color = Sys.Muted)
        SectionLabel("Übungen pro Runde")
        ui.plan.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(item.exercise.label, color = Sys.Text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                val step = if (item.exercise.unit == "Sprünge") 10 else 1
                SystemButton("−", { tvm.setReps(item.exercise, item.reps - step) }, Modifier.width(48.dp), minHeight = 44.dp)
                Text(
                    "${item.reps}", color = Sys.Cyan, style = MaterialTheme.typography.titleLarge,
                    textAlign = TextAlign.Center, modifier = Modifier.width(64.dp),
                )
                SystemButton("+", { tvm.setReps(item.exercise, item.reps + step) }, Modifier.width(48.dp), minHeight = 44.dp)
            }
        }
        StepperRow("Runden", "${ui.rounds}", { tvm.setRounds(ui.rounds - 1) }, { tvm.setRounds(ui.rounds + 1) })
        StepperRow(
            "Pause", "${ui.restSeconds} s" + if (ui.restReduction) " ⚡" else "",
            { tvm.setRest(ui.restSeconds - 15) }, { tvm.setRest(ui.restSeconds + 15) },
        )
        SectionLabel("Ausdauer-Block am Schluss")
        SegmentedChoice(CardioType.entries.filter { it != CardioType.OTHER }, ui.cardioType, { it.label }, { tvm.setCardioType(it) })
        Spacer(Modifier.height(6.dp))
        StepperRow("Dauer", if (ui.cardioMinutes == 0) "aus" else "${ui.cardioMinutes} min",
            { tvm.setCardio(ui.cardioMinutes - 5) }, { tvm.setCardio(ui.cardioMinutes + 5) })
        if (hasPlaylist) SystemSwitchRow("Spotify-Playlist starten", "Öffnet Spotify und startet deine Playlist.", ui.startSpotify) { tvm.setSpotify(it) }
        else SystemText("Tipp: In den Einstellungen kannst du eine Spotify-Playlist hinterlegen.", color = Sys.Muted)
        Spacer(Modifier.height(12.dp))
        SystemButton("Training starten", onStart, Modifier.fillMaxWidth(), filled = true, minHeight = 60.dp)
    }
}

@Composable
private fun StepperRow(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Sys.Text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        SystemButton("−", onMinus, Modifier.width(48.dp), minHeight = 44.dp)
        Text(value, color = Sys.Cyan, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.width(90.dp))
        SystemButton("+", onPlus, Modifier.width(48.dp), minHeight = 44.dp)
    }
}

@Composable
private fun Running(ui: TrainingUi, tvm: TrainingViewModel) {
    var confirmEnd by remember { mutableStateOf(false) }
    SystemWindow(title = if (ui.paused) "PAUSIERT" else "TRAINING LÄUFT") {
        Text(
            DateUtil.formatDuration(ui.elapsedMs / 1000), color = Sys.Text,
            style = MaterialTheme.typography.displayLarge.copy(fontSize = 72.sp),
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "Schritt ${ui.workStepsDone + 1} von ${ui.workStepsTotal}", color = Sys.Muted, style = MaterialTheme.typography.titleSmall,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
        GlowProgressBar(ui.workStepsDone / ui.workStepsTotal.coerceAtLeast(1).toFloat())
    }

    when (val step = ui.current) {
        is TrainingStep.Work -> SystemWindow(title = "RUNDE ${step.round}") {
            Text(step.exercise.label.uppercase(), color = Sys.Cyan, style = MaterialTheme.typography.headlineLarge, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("${step.reps} ${step.exercise.unit}", color = Sys.Text, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Text("Zeit: ${DateUtil.formatDuration(ui.stepElapsedMs / 1000)}", color = Sys.Muted, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(14.dp))
            SystemButton("Erledigt", { tvm.completeStep() }, Modifier.fillMaxWidth(), filled = true, minHeight = 64.dp, accent = Sys.Green)
            Spacer(Modifier.height(8.dp))
            SystemButton("Überspringen", { tvm.skipStep() }, Modifier.fillMaxWidth())
        }
        is TrainingStep.Rest -> SystemWindow(title = "PAUSE", accent = Sys.Blue) {
            Text(
                DateUtil.formatDuration(((ui.restRemainingMs + 999) / 1000).coerceAtLeast(0)), color = Sys.Text,
                style = MaterialTheme.typography.displayLarge.copy(fontSize = 80.sp), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Text("Als Nächstes: ${step.next}", color = Sys.Muted, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(6.dp))
            GlowProgressBar(1f - ui.restRemainingMs / (step.seconds * 1000f), color = Sys.Blue)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CountButton("+15 s", { tvm.addRest(15) }, accent = Sys.Blue)
                CountButton("Weiter", { tvm.skipStep() }, accent = Sys.Blue)
            }
        }
        is TrainingStep.Cardio -> SystemWindow(title = step.type.label.uppercase(), accent = Sys.Green) {
            val targetMs = step.minutes * 60_000L
            Text(
                "${DateUtil.formatDuration(ui.stepElapsedMs / 1000)} / ${step.minutes}:00", color = Sys.Text,
                style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            GlowProgressBar(ui.stepElapsedMs / targetMs.toFloat(), color = Sys.Green)
            Spacer(Modifier.height(14.dp))
            SystemButton("Fertig (gemessene Zeit)", { tvm.completeStep() }, Modifier.fillMaxWidth(), filled = true, accent = Sys.Green)
            Spacer(Modifier.height(8.dp))
            SystemButton("Ziel von ${step.minutes} min erreicht", { tvm.completeCardioTarget() }, Modifier.fillMaxWidth(), accent = Sys.Green)
        }
        null -> Unit
    }

    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SystemButton(if (ui.paused) "Fortsetzen" else "Pause", { tvm.togglePause() }, Modifier.weight(1f))
        SystemButton("Beenden", { confirmEnd = true }, Modifier.weight(1f), accent = Sys.Red)
    }

    if (confirmEnd) {
        SystemDialog("TRAINING BEENDEN?", onDismiss = { confirmEnd = false }) {
            SystemText("Bisher erledigte Übungen werden gespeichert und mit XP belohnt.")
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Weiter trainieren", { confirmEnd = false }, Modifier.weight(1f))
                SystemButton("Beenden", { confirmEnd = false; tvm.finish() }, Modifier.weight(1f), accent = Sys.Red, filled = true)
            }
        }
    }
}

@Composable
private fun Done(ui: TrainingUi, onClose: () -> Unit) {
    val s = ui.summary
    SystemWindow(title = "TRAINING ABGESCHLOSSEN") {
        if (s == null) {
            SystemText("Auswertung läuft …")
            return@SystemWindow
        }
        Text("+${s.xp} XP", color = Sys.Gold, style = MaterialTheme.typography.displayMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        Spacer(Modifier.height(8.dp))
        KeyValue("Dauer", DateUtil.formatDuration(s.durationSec))
        KeyValue("Kalorien (geschätzt)", "${s.kcal} kcal")
        s.reps.forEach { (ex, n) -> KeyValue(ex.label, "$n ${ex.unit}") }
        if (s.cardioMinutes > 0) KeyValue("Ausdauer", "${s.cardioMinutes} min")
        if (s.levelsGained > 0) KeyValue("Level-up", "+${s.levelsGained}", valueColor = Sys.Cyan)
        Spacer(Modifier.height(6.dp))
        SystemText("Kalorien = MET × Gewicht × Dauer, je Übung und Pause einzeln berechnet.", color = Sys.Muted)
        Spacer(Modifier.height(14.dp))
        SystemButton("Neues Training", onClose, Modifier.fillMaxWidth(), filled = true)
    }
}
