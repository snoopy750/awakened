package ch.awakened.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.data.db.ChallengeEntity
import ch.awakened.app.data.db.DailyExerciseEntity
import ch.awakened.app.domain.ChallengeCategory
import ch.awakened.app.domain.ChallengeFrequency
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.PenaltyRules
import ch.awakened.app.domain.XpRules
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.CountButton
import ch.awakened.app.ui.components.GlowProgressBar
import ch.awakened.app.ui.components.SectionLabel
import ch.awakened.app.ui.components.SegmentedChoice
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemDialog
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemTextField
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.components.glowBorder
import ch.awakened.app.ui.theme.Sys

/** Anzeige eines Werts in seiner Einheit (Zehntel-km werden als km mit Komma gezeigt). */
fun formatAmount(value: Int, unit: String): String = when (unit) {
    "km10" -> "%.1f".format(DateUtil.locale, value / 10.0)
    else -> value.toString()
}

fun unitLabel(row: DailyExerciseEntity): String = when (row.unit) {
    "km10" -> "km"
    "min" -> "min"
    else -> Exercise.valueOf(row.exercise).unit
}

@Composable
fun QuestsScreen(vm: MainViewModel) {
    val today by vm.today.collectAsStateWithLifecycle()
    val record by vm.todayRecord.collectAsStateWithLifecycle()
    val weekly by vm.weekly.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    val challenges by vm.challenges.collectAsStateWithLifecycle()
    val progress by vm.challengeProgress.collectAsStateWithLifecycle()

    var editRow by remember { mutableStateOf<DailyExerciseEntity?>(null) }
    var newChallenge by remember { mutableStateOf(false) }
    var deleteChallenge by remember { mutableStateOf<ChallengeEntity?>(null) }

    val ordered = Exercise.DAILY.mapNotNull { ex -> today.firstOrNull { it.exercise == ex.name } }
    val allDone = ordered.isNotEmpty() && ordered.all { it.done >= it.target }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SystemWindow(title = "QUEST INFO") {
            Text(
                "Tägliche Quest: Training des Erwachten", color = Sys.Text, style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                DateUtil.format(DateUtil.today(), "EEEE, d. MMMM"), color = Sys.Muted, style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(10.dp))
            SectionLabel("Ziele")
            ordered.forEach { row ->
                ExerciseRow(row, onDelta = { vm.addProgress(Exercise.valueOf(row.exercise), it) }, onEdit = { editRow = row })
                Spacer(Modifier.height(12.dp))
            }
            val bonus = XpRules.allDailiesBonus(profile?.level ?: 1)
            Text(
                if (record?.bonusAwarded == true || allDone) "✓ Alle Ziele erfüllt · Bonus erhalten"
                else "Belohnung bei Abschluss aller Ziele: +$bonus XP Bonus",
                color = if (allDone) Sys.Green else Sys.Gold, style = MaterialTheme.typography.titleSmall,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
            )
            settings?.let { s ->
                if (s.penaltyEnabled && !allDone) {
                    Spacer(Modifier.height(10.dp))
                    val left = PenaltyRules.minutesUntilDeadline(DateUtil.minuteOfDay(), s.penaltyConfig)
                    Column(Modifier.fillMaxWidth().glowBorder(Sys.Red, 6.dp, 1.dp, 0.4f).padding(10.dp)) {
                        Text("WARNUNG", color = Sys.Red, style = MaterialTheme.typography.labelLarge)
                        SystemText(
                            "Wird die Daily Quest nicht bis ${DateUtil.formatMinuteOfDay(s.deadlineMinute)} abgeschlossen, " +
                                "folgt die PENALTY ZONE. Verbleibende Zeit: ${DateUtil.formatRemaining(left)}",
                        )
                    }
                }
            }
        }

        SystemWindow(title = "WEEKLY QUESTS", icon = "W") {
            weekly.forEach { q ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(q.title, color = Sys.Text, style = MaterialTheme.typography.titleMedium)
                        Text(q.description, color = Sys.Muted, style = MaterialTheme.typography.bodyMedium)
                    }
                    Text(
                        if (q.claimed) "✓ +${XpRules.WEEKLY_QUEST_XP}" else "${q.progress.coerceAtMost(q.target)}/${q.target}",
                        color = if (q.claimed) Sys.Green else Sys.Text, style = MaterialTheme.typography.titleMedium,
                    )
                }
                Spacer(Modifier.height(4.dp))
                GlowProgressBar(q.progress / q.target.toFloat(), color = if (q.claimed) Sys.Green else Sys.Blue, height = 8.dp)
                Spacer(Modifier.height(12.dp))
            }
            SystemText("Weekly Quests werden jeden Montag zurückgesetzt.", color = Sys.Muted)
        }

        SystemWindow(title = "CHALLENGES", icon = "+") {
            if (challenges.isEmpty()) {
                SystemText("Erstelle eigene Challenges, z. B. 20 Seiten lesen, 30 min lernen oder um 6 Uhr aufstehen.", color = Sys.Muted)
                Spacer(Modifier.height(10.dp))
            }
            challenges.forEach { c ->
                val key = vm.periodKey(c)
                val p = progress.firstOrNull { it.challengeId == c.id && it.periodKey == key }
                val done = p?.done ?: 0
                val completed = p?.completed == true
                Column(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(c.name, color = Sys.Text, style = MaterialTheme.typography.titleMedium)
                            Text(
                                "${ChallengeCategory.valueOf(c.category).label} · ${ChallengeFrequency.valueOf(c.frequency).label} · ${c.xp} XP",
                                color = Sys.Muted, style = MaterialTheme.typography.bodySmall,
                            )
                        }
                        Text(
                            if (completed) "✓" else "$done/${c.target} ${c.unit}",
                            color = if (completed) Sys.Green else Sys.Text, style = MaterialTheme.typography.titleMedium,
                        )
                        IconButton(onClick = { deleteChallenge = c }, modifier = Modifier.size(48.dp)) {
                            Icon(Icons.Filled.Delete, "Löschen", tint = Sys.Dim)
                        }
                    }
                    GlowProgressBar(done / c.target.toFloat(), color = if (completed) Sys.Green else Sys.Cyan, height = 8.dp)
                    if (!completed) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            CountButton("−1", { vm.challengeProgress(c.id, -1) }, enabled = done > 0)
                            CountButton("+1", { vm.challengeProgress(c.id, 1) })
                            if (c.target >= 5) CountButton("+5", { vm.challengeProgress(c.id, 5) })
                            CountButton("Erfüllt", { vm.challengeProgress(c.id, c.target - done) }, accent = Sys.Green)
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SystemButton("Neue Challenge", { newChallenge = true }, Modifier.fillMaxWidth(), filled = true)
        }
        Spacer(Modifier.height(8.dp))
    }

    editRow?.let { row ->
        val ex = Exercise.valueOf(row.exercise)
        var text by remember(row) { mutableStateOf(formatAmount(row.done, row.unit)) }
        SystemDialog("${ex.label.uppercase()} EINTRAGEN", onDismiss = { editRow = null }) {
            SystemText("Gib den Gesamtwert für heute ein (${unitLabel(row)}).", color = Sys.Muted)
            Spacer(Modifier.height(10.dp))
            SystemTextField(text, { text = it }, "Wert (${unitLabel(row)})", numeric = true, decimal = row.unit == "km10")
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Abbrechen", { editRow = null }, Modifier.weight(1f))
                SystemButton("Speichern", {
                    val v = text.toDoubleLenient()
                    if (v != null) vm.setProgress(ex, if (row.unit == "km10") Math.round(v * 10).toInt() else v.toInt())
                    editRow = null
                }, Modifier.weight(1f), filled = true)
            }
        }
    }

    if (newChallenge) NewChallengeDialog(onDismiss = { newChallenge = false }) { n, cat, f, t, u, xp ->
        vm.createChallenge(n, cat, f, t, u, xp); newChallenge = false
    }

    deleteChallenge?.let { c ->
        SystemDialog("CHALLENGE LÖSCHEN", onDismiss = { deleteChallenge = null }) {
            SystemText("„${c.name}“ und den Fortschritt wirklich löschen?")
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Abbrechen", { deleteChallenge = null }, Modifier.weight(1f))
                SystemButton("Löschen", { vm.deleteChallenge(c.id); deleteChallenge = null }, Modifier.weight(1f), accent = Sys.Red, filled = true)
            }
        }
    }
}

@Composable
private fun ExerciseRow(row: DailyExerciseEntity, onDelta: (Int) -> Unit, onEdit: () -> Unit) {
    val ex = Exercise.valueOf(row.exercise)
    val complete = row.done >= row.target
    val color = if (complete) Sys.Green else Sys.Cyan
    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                (if (complete) "✓ " else "") + ex.label, color = Sys.Text, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.weight(1f),
            )
            Text(
                "[${formatAmount(row.done, row.unit)} / ${formatAmount(row.target, row.unit)}] ${unitLabel(row)}",
                color = color, style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.clickable(onClick = onEdit).padding(vertical = 6.dp),
            )
            IconButton(onClick = onEdit, modifier = Modifier.size(44.dp)) { Icon(Icons.Filled.Edit, "Manuell eingeben", tint = Sys.Muted) }
        }
        GlowProgressBar(row.done / row.target.toFloat(), color = color)
        Spacer(Modifier.height(8.dp))
        val steps = when (row.unit) {
            "km10" -> listOf(5 to "+0,5", 10 to "+1", 50 to "+5")
            "min" -> listOf(1 to "+1", 5 to "+5", 10 to "+10")
            else -> if (ex == Exercise.JUMP_ROPE) listOf(10 to "+10", 50 to "+50", 100 to "+100") else listOf(1 to "+1", 5 to "+5", 10 to "+10")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            CountButton("−${formatAmount(steps.first().first, row.unit)}", { onDelta(-steps.first().first) }, enabled = row.done > 0)
            steps.forEach { (amount, label) -> CountButton(label, { onDelta(amount) }) }
        }
    }
}

@Composable
private fun NewChallengeDialog(
    onDismiss: () -> Unit,
    onCreate: (String, ChallengeCategory, ChallengeFrequency, Int, String, Long) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var cat by remember { mutableStateOf(ChallengeCategory.OTHER) }
    var freq by remember { mutableStateOf(ChallengeFrequency.DAILY) }
    var target by remember { mutableStateOf("1") }
    var unit by remember { mutableStateOf("×") }
    var xp by remember { mutableStateOf("40") }
    val valid = name.isNotBlank() && (target.toIntOrNull() ?: 0) > 0 && (xp.toLongOrNull() ?: 0) > 0
    SystemDialog("NEUE CHALLENGE", onDismiss = onDismiss) {
        Column(Modifier.verticalScroll(rememberScrollState())) {
            SystemTextField(name, { name = it.take(40) }, "Name (z. B. 20 Seiten lesen)")
            SectionLabel("Kategorie")
            SegmentedChoice(ChallengeCategory.entries, cat, { it.label }, { cat = it })
            SectionLabel("Wiederholung")
            SegmentedChoice(ChallengeFrequency.entries, freq, { it.label }, { freq = it })
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemTextField(target, { target = it.filter(Char::isDigit).take(5) }, "Ziel", Modifier.weight(1f), numeric = true)
                SystemTextField(unit, { unit = it.take(10) }, "Einheit", Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            SystemTextField(xp, { xp = it.filter(Char::isDigit).take(4) }, "XP-Belohnung", numeric = true)
            Spacer(Modifier.height(6.dp))
            SystemText("Tipp: Einfache Gewohnheit 20–50 XP, grosse Ziele 100–300 XP.", color = Sys.Muted)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Abbrechen", onDismiss, Modifier.weight(1f))
                SystemButton(
                    "Erstellen",
                    { onCreate(name, cat, freq, target.toInt(), unit, xp.toLong()) },
                    Modifier.weight(1f), filled = true, enabled = valid,
                )
            }
        }
    }
}
