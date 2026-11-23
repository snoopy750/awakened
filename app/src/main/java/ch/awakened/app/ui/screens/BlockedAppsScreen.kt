package ch.awakened.app.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import ch.awakened.app.penalty.AppCatalog
import ch.awakened.app.penalty.InstalledApp
import ch.awakened.app.ui.MainViewModel
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemTextField
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.theme.Sys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class AppRow(val app: InstalledApp, val icon: ImageBitmap?)

@Composable
fun BlockedAppsScreen(vm: MainViewModel, onDone: () -> Unit) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()
    var apps by remember { mutableStateOf<List<AppRow>?>(null) }
    var query by remember { mutableStateOf("") }
    var selected by remember { mutableStateOf<Set<String>?>(null) }

    LaunchedEffect(Unit) {
        apps = withContext(Dispatchers.IO) {
            AppCatalog.load(context) { vm.repo.isExempt(it, emptySet()) }
                .filter { it.packageName != context.packageName }
                .map { a -> AppRow(a, runCatching { a.icon?.toBitmap(96, 96)?.asImageBitmap() }.getOrNull()) }
        }
    }
    LaunchedEffect(settings) { if (selected == null && settings != null) selected = settings!!.blockedApps }
    val sel = selected ?: emptySet()

    Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(16.dp)) {
        SystemWindow(title = "GESPERRTE APPS", accent = Sys.Red) {
            SystemText(
                "Diese Apps werden während der PENALTY ZONE gesperrt. Telefon, SMS, Karten, Banking und Systemapps sind immer erlaubt.",
                color = Sys.Muted,
            )
            Spacer(Modifier.height(10.dp))
            SystemTextField(query, { query = it }, "Suchen")
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                SystemButton("Vorschläge", {
                    selected = sel + (apps ?: emptyList()).filter { it.app.suggested && !it.app.exempt }.map { it.app.packageName }
                }, Modifier.weight(1f), accent = Sys.Red)
                SystemButton("Keine", { selected = emptySet() }, Modifier.weight(1f), accent = Sys.Red)
            }
        }
        Spacer(Modifier.height(10.dp))
        Box(Modifier.weight(1f)) {
            val list = apps
            if (list == null) {
                CircularProgressIndicator(color = Sys.Cyan, modifier = Modifier.align(Alignment.Center))
            } else {
                val filtered = list.filter { query.isBlank() || it.app.label.contains(query, ignoreCase = true) }
                LazyColumn(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    items(filtered, key = { it.app.packageName }) { row ->
                        val a = row.app
                        val checked = a.packageName in sel
                        Row(
                            Modifier
                                .fillMaxWidth()
                                .clickable(enabled = !a.exempt) { selected = if (checked) sel - a.packageName else sel + a.packageName }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (row.icon != null) Image(row.icon, null, Modifier.size(40.dp)) else Spacer(Modifier.size(40.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(a.label, color = if (a.exempt) Sys.Dim else Sys.Text, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    when {
                                        a.exempt -> "Immer erlaubt (Notfall/System)"
                                        a.suggested -> "Vorschlag"
                                        else -> a.packageName
                                    },
                                    color = if (a.suggested && !a.exempt) Sys.Red else Sys.Muted, style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Checkbox(
                                checked = checked && !a.exempt, enabled = !a.exempt,
                                onCheckedChange = { selected = if (it) sel + a.packageName else sel - a.packageName },
                                colors = CheckboxDefaults.colors(checkedColor = Sys.Red, uncheckedColor = Sys.Muted, checkmarkColor = Sys.Text),
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        SystemButton("Speichern (${sel.size})", {
            val exempt = apps?.filter { it.app.exempt }?.map { it.app.packageName }?.toSet() ?: emptySet()
            vm.updateSettings { it.copy(blockedApps = sel - exempt) }
            onDone()
        }, Modifier.fillMaxWidth(), filled = true, accent = Sys.Red)
    }
}
