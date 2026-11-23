package ch.awakened.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.BuildConfig
import ch.awakened.app.data.repo.fitness
import ch.awakened.app.data.repo.goalEnum
import ch.awakened.app.domain.CardioUnit
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.FitnessLevel
import ch.awakened.app.domain.Goal
import ch.awakened.app.integrations.SpotifyLauncher
import ch.awakened.app.penalty.PenaltyActivity
import ch.awakened.app.penalty.Permissions
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.KeyValue
import ch.awakened.app.ui.components.SectionLabel
import ch.awakened.app.ui.components.SegmentedChoice
import ch.awakened.app.ui.components.StatusDot
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemDialog
import ch.awakened.app.ui.components.SystemSwitchRow
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemTextField
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(vm: MainViewModel, onBack: () -> Unit, onPermissions: () -> Unit, onBlockedApps: () -> Unit) {
    val s = vm.settings.collectAsStateWithLifecycle().value ?: return
    val profile by vm.profile.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val ready = remember(tick) { Permissions.blockingReady(context) }

    var timeDialog by remember { mutableStateOf(false) }
    var profileDialog by remember { mutableStateOf(false) }
    var restoreUri by remember { mutableStateOf<Uri?>(null) }
    var playlist by remember(s.spotifyPlaylist) { mutableStateOf(s.spotifyPlaylist) }

    var hcGranted by remember { mutableStateOf(false) }
    LaunchedEffect(tick) { hcGranted = runCatching { vm.health.hasAllPermissions() }.getOrDefault(false) }
    val hcLauncher = rememberLauncherForActivityResult(vm.health.permissionContract()) { granted ->
        val ok = granted.containsAll(vm.health.permissions)
        hcGranted = ok
        if (ok) { vm.updateSettings { it.copy(healthConnectEnabled = true) }; vm.syncHealth() }
        else vm.toast("Health Connect: Nicht alle Berechtigungen erteilt.")
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let { vm.exportBackup(it) }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri -> restoreUri = uri }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SystemButton("‹ Zurück", onBack, Modifier.width(120.dp), minHeight = 44.dp)
            Spacer(Modifier.width(12.dp))
            Text("EINSTELLUNGEN", style = MaterialTheme.typography.headlineMedium, color = Sys.Cyan)
        }

        SystemWindow(title = "PENALTY ZONE", accent = Sys.Red) {
            SystemSwitchRow("Penalty Zone aktiv", "Sperrt gewählte Apps bei verpasster Daily Quest oder Inaktivität.", s.penaltyEnabled) { v ->
                vm.updateSettings { it.copy(penaltyEnabled = v) }
            }
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                Column(Modifier.weight(1f)) {
                    Text("Deadline Daily Quest", color = Sys.Text, style = MaterialTheme.typography.titleMedium)
                    Text("Bis dann muss alles erledigt sein.", color = Sys.Muted, style = MaterialTheme.typography.bodyMedium)
                }
                SystemButton(DateUtil.formatMinuteOfDay(s.deadlineMinute), { timeDialog = true }, Modifier.width(110.dp), minHeight = 48.dp)
            }
            SettingStepper(
                "Inaktivität bis Strafe", if (s.inactivityDays == 0) "aus" else "${s.inactivityDays} Tage",
                { vm.updateSettings { it.copy(inactivityDays = (it.inactivityDays - 1).coerceAtLeast(0)) } },
                { vm.updateSettings { it.copy(inactivityDays = (it.inactivityDays + 1).coerceAtMost(14)) } },
            )
            SettingStepper(
                "Max. Sperrzeit", "${s.lockHours} h",
                { vm.updateSettings { it.copy(lockHours = (it.lockHours - 1).coerceAtLeast(1)) } },
                { vm.updateSettings { it.copy(lockHours = (it.lockHours + 1).coerceAtMost(72)) } },
            )
            Spacer(Modifier.height(6.dp))
            KeyValue("Gesperrte Apps", "${s.blockedApps.size}")
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 6.dp)) {
                StatusDot(ready)
                Spacer(Modifier.width(8.dp))
                Text(
                    if (ready) "Sperre technisch einsatzbereit" else "Berechtigungen fehlen – Sperre wirkt nicht",
                    color = if (ready) Sys.Green else Sys.Red, style = MaterialTheme.typography.titleSmall,
                )
            }
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Apps wählen", onBlockedApps, Modifier.weight(1f), accent = Sys.Red)
                SystemButton("Berechtigungen", onPermissions, Modifier.weight(1f), accent = Sys.Red)
            }
            Spacer(Modifier.height(8.dp))
            SystemButton("Sperrbildschirm-Vorschau", {
                context.startActivity(Intent(context, PenaltyActivity::class.java).putExtra(PenaltyActivity.EXTRA_PREVIEW, true))
            }, Modifier.fillMaxWidth(), accent = Sys.Red)
        }

        SystemWindow(title = "TRAINING & MUSIK", icon = "♪") {
            SystemTextField(playlist, { playlist = it }, "Spotify-Playlist-Link")
            Spacer(Modifier.height(4.dp))
            SystemText("In Spotify: Playlist → ⋮ → Teilen → Link kopieren, hier einfügen.", color = Sys.Muted)
            Spacer(Modifier.height(8.dp))
            val valid = playlist.isBlank() || SpotifyLauncher.toUri(playlist) != null
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Speichern", {
                    vm.updateSettings { it.copy(spotifyPlaylist = playlist.trim()) }
                    vm.toast("Playlist gespeichert.")
                }, Modifier.weight(1f), enabled = valid, filled = true)
                SystemButton("Testen", {
                    SpotifyLauncher.startPlaylist(context, playlist, null)?.let { vm.toast(it) }
                }, Modifier.weight(1f), enabled = playlist.isNotBlank() && valid)
            }
            if (!valid) Text("Kein gültiger Spotify-Link.", color = Sys.Red, style = MaterialTheme.typography.bodySmall)
            SystemSwitchRow("Beim Training automatisch starten", null, s.spotifyAutoStart) { v -> vm.updateSettings { it.copy(spotifyAutoStart = v) } }
        }

        SystemWindow(title = "QUESTS", icon = "Q") {
            SectionLabel("Einheit für Ausdauer")
            SegmentedChoice(CardioUnit.entries, s.cardio, { it.label }, { u -> vm.updateSettings { it.copy(cardioUnit = u.name) } })
            Spacer(Modifier.height(4.dp))
            SystemText("Gilt ab der nächsten Daily Quest.", color = Sys.Muted)
            SystemSwitchRow("Erinnerungen", "3 h, 1 h und 15 min vor der Deadline.", s.remindersEnabled) { v -> vm.updateSettings { it.copy(remindersEnabled = v) } }
            SystemSwitchRow("Sound", null, s.soundEnabled) { v -> vm.updateSettings { it.copy(soundEnabled = v) } }
            SystemSwitchRow("Vibration", null, s.vibrationEnabled) { v -> vm.updateSettings { it.copy(vibrationEnabled = v) } }
        }

        SystemWindow(title = "FITNESS-TRACKER", icon = "♥") {
            when {
                vm.health.isAvailable() -> {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        StatusDot(hcGranted && s.healthConnectEnabled)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (hcGranted) "Health Connect verbunden" else "Health Connect nicht verbunden",
                            color = Sys.Text, style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    SystemText(
                        "Liest Schritte, Trainings, Distanz und Kalorien (z. B. von Samsung Health, Google Fit, Garmin). " +
                            "Lauf- und Velo-Einheiten zählen automatisch für die Ausdauer-Quest, Schritte geben XP.",
                        color = Sys.Muted,
                    )
                    Spacer(Modifier.height(8.dp))
                    if (!hcGranted) {
                        SystemButton("Verbinden", { hcLauncher.launch(vm.health.permissions) }, Modifier.fillMaxWidth(), filled = true)
                    } else {
                        SystemSwitchRow("Automatisch synchronisieren", "Beim Öffnen der App und alle 15 min.", s.healthConnectEnabled) { v ->
                            vm.updateSettings { it.copy(healthConnectEnabled = v) }
                        }
                        SystemButton("Jetzt synchronisieren", { vm.syncHealth() }, Modifier.fillMaxWidth())
                    }
                }
                vm.health.needsUpdate() -> {
                    SystemText("Health Connect muss installiert oder aktualisiert werden.", color = Sys.Muted)
                    Spacer(Modifier.height(8.dp))
                    SystemButton("Im Play Store öffnen", {
                        runCatching {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                    }, Modifier.fillMaxWidth())
                }
                else -> SystemText("Health Connect ist auf diesem Gerät nicht verfügbar. Ausdauer kannst du jederzeit manuell eintragen.", color = Sys.Muted)
            }
        }

        SystemWindow(title = "PROFIL", icon = "P") {
            profile?.let { p ->
                KeyValue("Name", p.name)
                KeyValue("Alter", "${p.age}")
                KeyValue("Fitnesslevel", p.fitness().label)
                KeyValue("Ziel", p.goalEnum().label)
            }
            Spacer(Modifier.height(8.dp))
            SystemButton("Profil bearbeiten", { profileDialog = true }, Modifier.fillMaxWidth())
        }

        SystemWindow(title = "DATEN", icon = "⇅") {
            SystemText("Sichere alle Fortschritte als JSON-Datei, z. B. vor einem Handywechsel.", color = Sys.Muted)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Backup", {
                    exportLauncher.launch("awakened-backup-${DateUtil.format(DateUtil.today(), "yyyy-MM-dd")}.json")
                }, Modifier.weight(1f), filled = true)
                SystemButton("Wiederherstellen", { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }, Modifier.weight(1f))
            }
        }

        Text(
            "AWAKENED ${BuildConfig.VERSION_NAME}", color = Sys.Dim, style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
    }

    if (timeDialog) {
        val state = rememberTimePickerState(s.deadlineMinute / 60, s.deadlineMinute % 60, is24Hour = true)
        SystemDialog("DEADLINE", onDismiss = { timeDialog = false }) {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Sys.Navy1, selectorColor = Sys.Cyan, containerColor = Sys.Panel,
                    timeSelectorSelectedContainerColor = Sys.Cyan.copy(alpha = 0.25f), timeSelectorUnselectedContainerColor = Sys.Navy1,
                    timeSelectorSelectedContentColor = Sys.Text, timeSelectorUnselectedContentColor = Sys.Muted,
                    clockDialSelectedContentColor = Sys.Navy0, clockDialUnselectedContentColor = Sys.Text,
                ),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Abbrechen", { timeDialog = false }, Modifier.weight(1f))
                SystemButton("Speichern", {
                    val m = state.hour * 60 + state.minute
                    vm.updateSettings { it.copy(deadlineMinute = m) }
                    timeDialog = false
                }, Modifier.weight(1f), filled = true)
            }
        }
    }

    if (profileDialog) profile?.let { p ->
        var name by remember { mutableStateOf(p.name) }
        var age by remember { mutableStateOf(p.age.toString()) }
        var height by remember { mutableStateOf("%.0f".format(p.heightCm)) }
        var fit by remember { mutableStateOf(p.fitness()) }
        var goal by remember { mutableStateOf(p.goalEnum()) }
        SystemDialog("PROFIL", onDismiss = { profileDialog = false }) {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                SystemTextField(name, { name = it }, "Name")
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemTextField(age, { age = it.filter(Char::isDigit).take(3) }, "Alter", Modifier.weight(1f), numeric = true)
                    SystemTextField(height, { height = it.take(5) }, "Grösse (cm)", Modifier.weight(1f), decimal = true)
                }
                SectionLabel("Fitnesslevel")
                SegmentedChoice(FitnessLevel.entries, fit, { it.label }, { fit = it })
                SectionLabel("Ziel")
                SegmentedChoice(Goal.entries, goal, { it.label }, { goal = it })
                Spacer(Modifier.height(6.dp))
                SystemText("Gewicht trägst du unter Statistik → Gewicht ein. Neue Ziele gelten ab der nächsten Daily Quest.", color = Sys.Muted)
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    SystemButton("Abbrechen", { profileDialog = false }, Modifier.weight(1f))
                    SystemButton("Speichern", {
                        val a = age.toIntOrNull()
                        val h = height.toDoubleLenient()
                        if (name.isNotBlank() && a != null && h != null) vm.updateProfile(name.trim(), a, h, fit, goal)
                        profileDialog = false
                    }, Modifier.weight(1f), filled = true)
                }
            }
        }
    }

    restoreUri?.let { uri ->
        SystemDialog("WIEDERHERSTELLEN", onDismiss = { restoreUri = null }, accent = Sys.Red) {
            SystemText("Alle aktuellen Daten werden durch das Backup ersetzt. Fortfahren?")
            Spacer(Modifier.height(14.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Abbrechen", { restoreUri = null }, Modifier.weight(1f))
                SystemButton("Ersetzen", { scope.launch { vm.importBackup(uri) }; restoreUri = null }, Modifier.weight(1f), accent = Sys.Red, filled = true)
            }
        }
    }
}

@Composable
private fun SettingStepper(label: String, value: String, onMinus: () -> Unit, onPlus: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(label, color = Sys.Text, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        SystemButton("−", onMinus, Modifier.width(48.dp), minHeight = 48.dp)
        Text(value, color = Sys.Cyan, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.width(80.dp))
        SystemButton("+", onPlus, Modifier.width(48.dp), minHeight = 48.dp)
    }
}
