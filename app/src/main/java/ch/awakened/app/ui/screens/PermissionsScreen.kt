package ch.awakened.app.ui.screens

import android.Manifest
import android.os.Build
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import ch.awakened.app.penalty.Permissions
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.StatusDot
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys

@Composable
fun PermissionsScreen(vm: MainViewModel, introMode: Boolean, onBack: () -> Unit, onContinue: () -> Unit) {
    val context = LocalContext.current
    var tick by remember { mutableIntStateOf(0) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { tick++ }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    // tick erzwingt das Neulesen nach Rückkehr aus den Systemeinstellungen
    val notif = remember(tick) { Permissions.hasNotifications(context) }
    val overlay = remember(tick) { Permissions.canDrawOverlays(context) }
    val usage = remember(tick) { Permissions.hasUsageAccess(context) }
    val battery = remember(tick) { Permissions.ignoresBatteryOptimizations(context) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        SystemWindow(title = "SYSTEM-BERECHTIGUNGEN") {
            SystemText(
                "Damit das System dich erinnern und die PENALTY ZONE durchsetzen kann, braucht AWAKENED einige Berechtigungen. " +
                    "Es werden keine Daten gesammelt oder versendet; alles bleibt auf deinem Gerät.",
            )
        }

        PermissionCard(
            "Benachrichtigungen", notif,
            "Für Erinnerungen wie „Die Daily Quest ist noch nicht abgeschlossen. Verbleibende Zeit: 3h“ und Level-up-Meldungen.",
            "Erlauben",
        ) {
            if (Build.VERSION.SDK_INT >= 33) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            else context.startActivity(Permissions.appDetailsIntent(context))
        }

        PermissionCard(
            "Nutzungszugriff (wichtig)", usage,
            "Erkennt, welche App gerade im Vordergrund ist. Nur so weiss das System während der PENALTY ZONE, " +
                "wann eine gesperrte App geöffnet wird. Es werden keine Inhalte gelesen.\n\n" +
                "So geht's: In der Liste AWAKENED antippen → „Nutzungszugriff erlauben“ einschalten.",
            "Erlauben",
        ) { context.startActivity(Permissions.usageSettingsIntent()) }

        PermissionCard(
            "Über anderen Apps einblenden (wichtig)", overlay,
            "Damit legt sich der rote PENALTY-ZONE-Bildschirm über eine gesperrte App.\n\n" +
                "So geht's: AWAKENED antippen → „Über anderen Apps einblenden“ bzw. „Anzeige über anderen Apps“ erlauben.",
            "Erlauben",
        ) { context.startActivity(Permissions.overlaySettingsIntent(context)) }

        PermissionCard(
            "Akku-Optimierung ausnehmen", battery,
            "Verhindert, dass Android die Tagesprüfung und die Sperre stoppt. Bei Xiaomi, Samsung, Huawei usw. zusätzlich in der App-Info „Autostart“ erlauben und den Akkusparmodus auf „Keine Einschränkungen“ setzen.",
            "Ausnehmen",
        ) { runCatching { context.startActivity(Permissions.batterySettingsIntent(context)) } }

        SystemWindow {
            val ready = usage && overlay
            Row(verticalAlignment = Alignment.CenterVertically) {
                StatusDot(ready)
                Spacer(Modifier.width(10.dp))
                Text(
                    if (ready) "PENALTY ZONE einsatzbereit" else "PENALTY ZONE noch nicht durchsetzbar",
                    style = MaterialTheme.typography.titleMedium, color = Sys.Text,
                )
            }
            Spacer(Modifier.height(6.dp))
            SystemText("Du kannst Berechtigungen jederzeit in den Einstellungen nachholen.", color = Sys.Muted)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!introMode) SystemButton("Zurück", onBack, Modifier.weight(1f))
                SystemButton(if (introMode) "Weiter" else "Fertig", onContinue, Modifier.weight(1f), filled = true)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun PermissionCard(
    title: String,
    granted: Boolean,
    text: String,
    actionLabel: String,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    onAction: () -> Unit,
) {
    SystemWindow(accent = if (granted) Sys.Green else Sys.Cyan, contentPadding = 14.dp) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            StatusDot(granted)
            Spacer(Modifier.width(10.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = Sys.Text, modifier = Modifier.weight(1f))
            Text(if (granted) "AKTIV" else "FEHLT", style = MaterialTheme.typography.labelMedium, color = if (granted) Sys.Green else Sys.Red)
        }
        Spacer(Modifier.height(8.dp))
        SystemText(text, color = Sys.Muted)
        if (!granted) {
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                if (secondaryLabel != null && onSecondary != null) SystemButton(secondaryLabel, onSecondary, Modifier.weight(1f))
                SystemButton(actionLabel, onAction, Modifier.weight(1f), filled = true)
            }
        }
    }
}
