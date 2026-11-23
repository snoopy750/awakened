package ch.awakened.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.data.db.DayRecord
import ch.awakened.app.data.db.WeightEntry
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.Exercise
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.KeyValue
import ch.awakened.app.ui.components.SectionLabel
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemDialog
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemTextField
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys

@Composable
fun StatsScreen(vm: MainViewModel) {
    val today = DateUtil.today()
    val repo = vm.repo
    val xpFlow = remember(today) { repo.dailyXp(today - 13) }
    val dailyXp by xpFlow.collectAsState(initial = emptyList())
    val days by repo.dayRecords.collectAsState(initial = emptyList())
    val weights by repo.weightEntries.collectAsState(initial = emptyList())
    val workouts by repo.workouts.collectAsState(initial = emptyList())
    val pushups by remember { repo.totalOf(Exercise.PUSHUPS) }.collectAsState(initial = 0L)
    val situps by remember { repo.totalOf(Exercise.SITUPS) }.collectAsState(initial = 0L)
    val squats by remember { repo.totalOf(Exercise.SQUATS) }.collectAsState(initial = 0L)
    val jumps by remember { repo.totalOf(Exercise.JUMP_ROPE) }.collectAsState(initial = 0L)
    val achievements by vm.achievements.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    var addWeight by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SystemWindow(title = "XP – LETZTE 14 TAGE", icon = "≡") {
            val values = (0..13).map { i -> dailyXp.firstOrNull { it.epochDay == today - 13 + i }?.total ?: 0L }
            BarChart(values)
            Row(Modifier.fillMaxWidth()) {
                Text(DateUtil.format(today - 13, "dd.MM."), color = Sys.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                Text("heute", color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
            }
        }

        SystemWindow(title = "VERLAUF – 28 TAGE", icon = "▦") {
            CompletionGrid(days, today)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Legend(Sys.Green, "erfüllt"); Legend(Sys.Gold, "Joker"); Legend(Sys.Red, "verpasst")
            }
            Spacer(Modifier.height(6.dp))
            KeyValue("Aktuelle Serie", "$streak Tage")
            KeyValue("Beste Serie", "${achievements?.bestStreak ?: 0} Tage")
        }

        SystemWindow(title = "GESAMT", icon = "Σ") {
            KeyValue("Liegestütze", "$pushups")
            KeyValue("Sit-ups", "$situps")
            KeyValue("Kniebeugen", "$squats")
            KeyValue("Seilsprünge", "$jumps")
            KeyValue("Ausdauer", "${achievements?.totalCardioMinutes ?: 0} min")
            KeyValue("Trainings", "${workouts.size}")
            KeyValue("Kalorien (Trainings)", "${workouts.sumOf { it.kcal }} kcal")
            KeyValue("Gesamt-XP", "${profile?.totalXp ?: 0}")
            KeyValue("Straf-Quests überlebt", "${achievements?.penaltiesCleared ?: 0}")
        }

        SystemWindow(title = "GEWICHT", icon = "⚖") {
            if (weights.size >= 2) WeightChart(weights) else SystemText("Trage dein Gewicht regelmässig ein, um den Verlauf zu sehen.", color = Sys.Muted)
            Spacer(Modifier.height(8.dp))
            weights.takeLast(5).reversed().forEach { w ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(DateUtil.format(w.epochDay), color = Sys.Muted, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                    Text("%.1f kg".format(w.weightKg), color = Sys.Text, style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { vm.deleteWeight(w.id) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Filled.Delete, "Eintrag löschen", tint = Sys.Dim)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            SystemButton("Gewicht eintragen", { addWeight = true }, Modifier.fillMaxWidth(), filled = true)
        }

        if (workouts.isNotEmpty()) {
            SystemWindow(title = "LETZTE TRAININGS", icon = "»") {
                workouts.take(8).forEach { w ->
                    Column(Modifier.padding(vertical = 5.dp)) {
                        Row {
                            Text(w.title, color = Sys.Text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            Text("+${w.xp} XP", color = Sys.Gold, style = MaterialTheme.typography.titleSmall)
                        }
                        Text(
                            "${DateUtil.format(w.epochDay)} · ${DateUtil.formatDuration(w.durationSec)} · ${w.kcal} kcal" +
                                (if (w.source == "HEALTH") " · Health Connect" else "") +
                                (if (w.details.isNotBlank()) "\n${w.details}" else ""),
                            color = Sys.Muted, style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
    }

    if (addWeight) {
        var text by remember { mutableStateOf(profile?.weightKg?.let { "%.1f".format(it) } ?: "") }
        SystemDialog("GEWICHT EINTRAGEN", onDismiss = { addWeight = false }) {
            SystemTextField(text, { text = it.take(6) }, "Gewicht (kg)", decimal = true)
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Abbrechen", { addWeight = false }, Modifier.weight(1f))
                SystemButton("Speichern", {
                    text.toDoubleLenient()?.takeIf { it in 30.0..300.0 }?.let { vm.addWeight(it) }
                    addWeight = false
                }, Modifier.weight(1f), filled = true)
            }
        }
    }
}

@Composable
private fun Legend(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).padding(0.dp)) { Canvas(Modifier.fillMaxSize()) { drawRect(color) } }
        Spacer(Modifier.padding(start = 6.dp))
        Text(label, color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun BarChart(values: List<Long>) {
    val max = (values.maxOrNull() ?: 0L).coerceAtLeast(1L)
    Canvas(Modifier.fillMaxWidth().height(140.dp).padding(vertical = 8.dp)) {
        val n = values.size
        val gap = 6.dp.toPx()
        val bw = (size.width - gap * (n - 1)) / n
        for (i in 1..3) {
            val y = size.height * i / 4f
            drawLine(Sys.Grid, Offset(0f, y), Offset(size.width, y), 1f)
        }
        values.forEachIndexed { i, v ->
            val h = size.height * (v.toFloat() / max)
            val x = i * (bw + gap)
            if (v > 0) {
                drawRoundRect(Sys.Cyan.copy(alpha = 0.18f), Offset(x - 2f, size.height - h - 4f), Size(bw + 4f, h + 4f), CornerRadius(4f, 4f))
                drawRoundRect(
                    Brush.verticalGradient(listOf(Sys.Cyan, Sys.Blue.copy(alpha = 0.6f)), size.height - h, size.height),
                    Offset(x, size.height - h), Size(bw, h), CornerRadius(3f, 3f),
                )
            } else {
                drawRoundRect(Sys.Dim.copy(alpha = 0.4f), Offset(x, size.height - 3f), Size(bw, 3f))
            }
        }
    }
}

@Composable
private fun CompletionGrid(days: List<DayRecord>, today: Long) {
    val byDay = days.associateBy { it.epochDay }
    val start = today - 27
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        for (row in 0 until 4) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (col in 0 until 7) {
                    val d = start + row * 7 + col
                    val r = byDay[d]
                    val color = when {
                        r?.allDone == true -> Sys.Green
                        r?.outcome == "JOKER_USED" -> Sys.Gold
                        r?.outcome == "FAILED" -> Sys.Red
                        d == today -> Sys.Cyan.copy(alpha = 0.35f)
                        else -> Sys.Dim.copy(alpha = 0.25f)
                    }
                    Box(Modifier.weight(1f).aspectRatio(1f)) {
                        Canvas(Modifier.fillMaxSize()) {
                            drawRoundRect(color.copy(alpha = if (color.alpha < 1f) color.alpha else 0.85f), cornerRadius = CornerRadius(6f, 6f))
                            if (d == today) drawRoundRect(Sys.Cyan, cornerRadius = CornerRadius(6f, 6f), style = Stroke(3f))
                        }
                        Text(
                            DateUtil.format(d, "d"), color = Sys.Navy0.takeIf { r?.allDone == true || r?.outcome != null } ?: Sys.Muted,
                            style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>) {
    val pts = entries.takeLast(30)
    val min = pts.minOf { it.weightKg } - 1
    val max = pts.maxOf { it.weightKg } + 1
    Column {
        Row {
            Text("%.1f kg".format(max), color = Sys.Muted, style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
            Text("Δ %+.1f kg".format(pts.last().weightKg - pts.first().weightKg), color = Sys.Cyan, style = MaterialTheme.typography.titleSmall)
        }
        Canvas(Modifier.fillMaxWidth().height(130.dp).padding(vertical = 6.dp)) {
            val span = (max - min).coerceAtLeast(0.1)
            val firstDay = pts.first().epochDay
            val daySpan = (pts.last().epochDay - firstDay).coerceAtLeast(1)
            fun pos(e: WeightEntry, idx: Int): Offset {
                val xFrac = if (pts.last().epochDay == firstDay) idx / (pts.size - 1f) else (e.epochDay - firstDay) / daySpan.toFloat()
                return Offset(size.width * xFrac, size.height * (1f - ((e.weightKg - min) / span).toFloat()))
            }
            val path = Path()
            pts.forEachIndexed { i, e -> val p = pos(e, i); if (i == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y) }
            drawPath(path, Sys.Cyan.copy(alpha = 0.25f), style = Stroke(10f, cap = StrokeCap.Round))
            drawPath(path, Sys.Cyan, style = Stroke(4f, cap = StrokeCap.Round))
            pts.forEachIndexed { i, e -> drawCircle(Sys.Text, 6f, pos(e, i)) }
        }
        Text("%.1f kg".format(min), color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
        SectionLabel("Einträge")
    }
}
