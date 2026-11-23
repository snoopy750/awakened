package ch.awakened.app.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.data.repo.stats
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.FitnessCalc
import ch.awakened.app.domain.HunterTitle
import ch.awakened.app.domain.LevelState
import ch.awakened.app.domain.Leveling
import ch.awakened.app.domain.PenaltyRules
import ch.awakened.app.domain.Rank
import ch.awakened.app.domain.Sex
import ch.awakened.app.domain.Stat
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.GlowProgressBar
import ch.awakened.app.ui.components.KeyValue
import ch.awakened.app.ui.components.SectionLabel
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemDialog
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.components.glowBorder
import ch.awakened.app.ui.theme.Sys

fun rankColor(rank: Rank): Color = when (rank) {
    Rank.E -> Sys.Muted
    Rank.D -> Sys.Green
    Rank.C -> Sys.Cyan
    Rank.B -> Sys.Blue
    Rank.A -> Sys.Violet
    Rank.S -> Sys.Gold
}

@Composable
fun RankBadge(rank: Rank, size: Int = 64) {
    val color = rankColor(rank)
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(size.dp)) {
            val w = this.size.width; val h = this.size.height
            val p = Path().apply {
                moveTo(w * 0.5f, 0f); lineTo(w, h * 0.25f); lineTo(w, h * 0.75f)
                lineTo(w * 0.5f, h); lineTo(0f, h * 0.75f); lineTo(0f, h * 0.25f); close()
            }
            drawPath(p, color.copy(alpha = 0.15f))
            drawPath(p, color.copy(alpha = 0.3f), style = Stroke(8f))
            drawPath(p, color, style = Stroke(3f))
        }
        Text(rank.letter, color = color, style = MaterialTheme.typography.headlineLarge)
    }
}

@Composable
fun StatusScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val today by vm.today.collectAsStateWithLifecycle()
    val streak by vm.streak.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val achievements by vm.achievements.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val p = profile ?: return
    var titleDialog by remember { mutableStateOf(false) }

    val rank = Rank.forLevel(p.level)
    val state = LevelState(p.level, p.xpInLevel)
    val title = runCatching { HunterTitle.valueOf(p.activeTitle) }.getOrDefault(HunterTitle.AWAKENED)
    val stats = p.stats()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SystemWindow(title = "STATUS") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("NAME", color = Sys.Muted, style = MaterialTheme.typography.labelMedium)
                    Text(p.name, color = Sys.Text, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.height(4.dp))
                    Text("TITEL", color = Sys.Muted, style = MaterialTheme.typography.labelMedium)
                    Text(
                        title.displayName, color = Sys.Cyan, style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.clickable { titleDialog = true }.padding(vertical = 4.dp),
                    )
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("LEVEL", color = Sys.Muted, style = MaterialTheme.typography.labelMedium)
                    Text("${p.level}", color = Sys.Text, style = MaterialTheme.typography.displayLarge)
                }
                Spacer(Modifier.width(12.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    RankBadge(rank)
                    Text("RANG", color = Sys.Muted, style = MaterialTheme.typography.labelMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row {
                Text("EXP", color = Sys.Muted, style = MaterialTheme.typography.labelMedium, modifier = Modifier.weight(1f))
                Text("${p.xpInLevel} / ${Leveling.xpForNextLevel(p.level)}", color = Sys.Text, style = MaterialTheme.typography.labelMedium)
            }
            Spacer(Modifier.height(4.dp))
            GlowProgressBar(Leveling.progress(state))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MiniStat("STREAK", "$streak 🔥", Modifier.weight(1f))
                MiniStat("GESAMT-XP", "${p.totalXp}", Modifier.weight(1f))
                MiniStat("BESTE SERIE", "${achievements?.bestStreak ?: 0}", Modifier.weight(1f))
            }
            Rank.next(rank)?.let { nxt ->
                Spacer(Modifier.height(8.dp))
                SystemText("Nächster Rang ${nxt.letter} ab Level ${nxt.minLevel}", color = Sys.Muted)
            }
        }

        // Daily Kurzstatus
        val done = today.count { it.done >= it.target }
        val s = settings
        SystemWindow(title = "QUEST INFO", contentPadding = 14.dp) {
            KeyValue("Daily Quest", "$done / ${today.size} erledigt", valueColor = if (done == today.size && today.isNotEmpty()) Sys.Green else Sys.Text)
            if (s != null && s.penaltyEnabled) {
                val left = PenaltyRules.minutesUntilDeadline(DateUtil.minuteOfDay(), s.penaltyConfig)
                KeyValue("Deadline", "${DateUtil.formatMinuteOfDay(s.deadlineMinute)} (noch ${DateUtil.formatRemaining(left)})")
            }
            if (meta.doubleXpDay == DateUtil.today()) KeyValue("Aktiv", "Doppel-XP-Tag", valueColor = Sys.Gold)
        }

        SystemWindow(title = "STATS") {
            if (p.statPoints > 0) {
                Text(
                    "Verfügbare Punkte: ${p.statPoints}", color = Sys.Gold, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
            }
            Stat.entries.forEach { st ->
                Row(Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("${st.short}  ·  ${st.label}", color = Sys.Text, style = MaterialTheme.typography.titleMedium)
                        Text(st.description, color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                    Text("${stats[st]}", color = Sys.Cyan, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.padding(horizontal = 12.dp))
                    SystemButton("+", { vm.allocate(st) }, Modifier.width(52.dp), enabled = p.statPoints > 0, minHeight = 48.dp)
                }
            }
        }

        SystemWindow(title = "KÖRPER", contentPadding = 14.dp) {
            val bmi = FitnessCalc.bmi(p.weightKg, p.heightCm)
            val sex = runCatching { Sex.valueOf(p.sex) }.getOrDefault(Sex.MALE)
            KeyValue("Grösse", "%.0f cm".format(p.heightCm))
            KeyValue("Gewicht", "%.1f kg".format(p.weightKg))
            KeyValue("BMI", "%.1f · %s".format(bmi, FitnessCalc.bmiCategory(bmi)))
            KeyValue("Grundumsatz", "%.0f kcal/Tag".format(FitnessCalc.bmrMifflinStJeor(p.weightKg, p.heightCm, p.age, sex)))
        }
        Spacer(Modifier.height(8.dp))
    }

    if (titleDialog) {
        val unlocked = achievements?.let { HunterTitle.unlocked(it) } ?: listOf(HunterTitle.AWAKENED)
        SystemDialog("TITEL WÄHLEN", onDismiss = { titleDialog = false }) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                HunterTitle.entries.forEach { t ->
                    val ok = t in unlocked
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .glowBorder(if (t == title) Sys.Cyan else Sys.Dim, 6.dp, 1.dp, if (t == title) 1f else 0.1f)
                            .clickable(enabled = ok) { vm.setTitle(t); titleDialog = false }
                            .padding(12.dp),
                    ) {
                        Column {
                            Text(t.displayName, color = if (ok) Sys.Text else Sys.Dim, style = MaterialTheme.typography.titleMedium)
                            Text(if (ok) "Freigeschaltet" else "Bedingung: ${t.condition}", color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                SectionLabel("Hinweis")
                SystemText("Titel erhältst du für Meilensteine wie Streaks, Wiederholungen und Level.", color = Sys.Muted)
                Spacer(Modifier.height(10.dp))
                SystemButton("Schliessen", { titleDialog = false }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun MiniStat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier.glowBorder(Sys.Cyan, 6.dp, 1.dp, 0.25f).padding(vertical = 8.dp, horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, color = Sys.Text, style = MaterialTheme.typography.titleLarge)
        Text(label, color = Sys.Muted, style = MaterialTheme.typography.labelMedium)
    }
}

