package ch.awakened.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.data.repo.stats
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.HunterTitle
import ch.awakened.app.domain.Skill
import ch.awakened.app.domain.SkillType
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys

@Composable
fun SkillsScreen(vm: MainViewModel) {
    val profile by vm.profile.collectAsStateWithLifecycle()
    val meta by vm.meta.collectAsStateWithLifecycle()
    val achievements by vm.achievements.collectAsStateWithLifecycle()
    val p = profile ?: return
    val stats = p.stats()
    val week = DateUtil.weekStart(DateUtil.today())

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SystemWindow(title = "SKILLS") {
            SystemText("Skills werden durch Level und Stat-Werte freigeschaltet. Verteile deine Punkte im Status gezielt.", color = Sys.Muted)
        }
        Skill.entries.forEach { skill ->
            val unlocked = skill.isUnlocked(p.level, stats)
            val accent = when {
                !unlocked -> Sys.Dim
                skill.type == SkillType.ACTIVE -> Sys.Gold
                else -> Sys.Cyan
            }
            SystemWindow(accent = accent, contentPadding = 14.dp) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(skill.displayName, color = if (unlocked) Sys.Text else Sys.Dim, style = MaterialTheme.typography.titleLarge)
                        Text(
                            "${skill.type.label} · ${skill.requirementText()}",
                            color = if (unlocked) accent else Sys.Dim, style = MaterialTheme.typography.labelMedium,
                        )
                    }
                    Text(if (unlocked) "FREI" else "🔒", color = accent, style = MaterialTheme.typography.labelLarge)
                }
                Spacer(Modifier.height(6.dp))
                SystemText(skill.description, color = if (unlocked) Sys.Muted else Sys.Dim)
                if (unlocked) when (skill) {
                    Skill.SECOND_WIND -> {
                        Spacer(Modifier.height(6.dp))
                        val used = meta.jokerWeekUsed == week
                        Text(
                            if (used) "Joker diese Woche verbraucht" else "Joker diese Woche verfügbar (wird automatisch eingesetzt)",
                            color = if (used) Sys.Muted else Sys.Green, style = MaterialTheme.typography.titleSmall,
                        )
                    }
                    Skill.DOUBLE_XP_DAY -> {
                        Spacer(Modifier.height(10.dp))
                        val activeToday = meta.doubleXpDay == DateUtil.today()
                        val usedWeek = meta.doubleXpWeekUsed == week
                        when {
                            activeToday -> Text("Heute aktiv: doppelte XP!", color = Sys.Gold, style = MaterialTheme.typography.titleMedium)
                            usedWeek -> Text("Diese Woche bereits eingesetzt", color = Sys.Muted, style = MaterialTheme.typography.titleSmall)
                            else -> SystemButton("Heute aktivieren", { vm.activateDoubleXp() }, Modifier.fillMaxWidth(), accent = Sys.Gold, filled = true)
                        }
                    }
                    else -> Unit
                }
            }
        }

        SystemWindow(title = "TITEL", icon = "T") {
            val unlocked = achievements?.let { HunterTitle.unlocked(it) } ?: emptyList()
            HunterTitle.entries.forEach { t ->
                val ok = t in unlocked
                Row(Modifier.fillMaxWidth().padding(vertical = 5.dp)) {
                    Text(if (ok) "◆" else "◇", color = if (ok) Sys.Cyan else Sys.Dim, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.padding(start = 10.dp))
                    Column {
                        Text(t.displayName, color = if (ok) Sys.Text else Sys.Dim, style = MaterialTheme.typography.titleMedium)
                        Text(t.condition, color = Sys.Muted, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            SystemText("Aktiven Titel wählst du im Status durch Antippen.", color = Sys.Muted)
        }
        Spacer(Modifier.height(8.dp))
    }
}
