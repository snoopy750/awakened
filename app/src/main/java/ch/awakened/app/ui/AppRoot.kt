package ch.awakened.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import ch.awakened.app.data.repo.GameEvent
import ch.awakened.app.domain.Rank
import ch.awakened.app.system.Feedback
import ch.awakened.app.ui.components.SystemBackground
import ch.awakened.app.ui.components.SystemButton
import ch.awakened.app.ui.components.SystemDialog
import ch.awakened.app.ui.components.SystemText
import ch.awakened.app.ui.components.SystemWindow
import ch.awakened.app.ui.components.glowBorder
import ch.awakened.app.ui.screens.BlockedAppsScreen
import ch.awakened.app.ui.screens.OnboardingScreen
import ch.awakened.app.ui.screens.PenaltyScreen
import ch.awakened.app.ui.screens.PermissionsScreen
import ch.awakened.app.ui.screens.QuestsScreen
import ch.awakened.app.ui.screens.SettingsScreen
import ch.awakened.app.ui.screens.SkillsScreen
import ch.awakened.app.ui.screens.StatsScreen
import ch.awakened.app.ui.screens.StatusScreen
import ch.awakened.app.ui.screens.TrainingScreen
import ch.awakened.app.ui.theme.Sys
import kotlinx.coroutines.delay

object Routes {
    const val ONBOARDING = "onboarding"
    const val PERMISSIONS = "permissions"
    const val BLOCKED_APPS = "blocked_apps"
    const val STATUS = "status"
    const val QUESTS = "quests"
    const val TRAINING = "training"
    const val SKILLS = "skills"
    const val STATS = "stats"
    const val SETTINGS = "settings"
    const val PENALTY = "penalty"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.STATUS, "Status", Icons.Filled.Person),
    Tab(Routes.QUESTS, "Quests", Icons.AutoMirrored.Filled.ListAlt),
    Tab(Routes.TRAINING, "Training", Icons.Filled.FitnessCenter),
    Tab(Routes.SKILLS, "Skills", Icons.Filled.AutoAwesome),
    Tab(Routes.STATS, "Statistik", Icons.Filled.BarChart),
)

@Composable
fun AppRoot(vm: MainViewModel, pendingRoute: String?, onRouteConsumed: () -> Unit) {
    val ready by vm.ready.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()
    val profile by vm.profile.collectAsStateWithLifecycle()
    if (!ready || settings == null) {
        SystemBackground {}
        return
    }
    val s = settings!!
    val start = when {
        !s.onboardingDone || profile == null -> Routes.ONBOARDING
        !s.permissionsIntroDone -> Routes.PERMISSIONS
        else -> Routes.STATUS
    }
    val startDest = remember { start }
    val nav = rememberNavController()
    val backStack by nav.currentBackStackEntryAsState()
    val route = backStack?.destination?.route
    val penalty by vm.penalty.collectAsStateWithLifecycle()

    LaunchedEffect(pendingRoute, route) {
        if (pendingRoute != null && route != null && profile != null && route != Routes.ONBOARDING) {
            nav.navigate(pendingRoute) { launchSingleTop = true }
            onRouteConsumed()
        }
    }

    SystemBackground {
        Scaffold(
            containerColor = Color.Transparent,
            contentWindowInsets = WindowInsets(0),
            topBar = {
                if (route in tabs.map { it.route }) {
                    TopBar(
                        level = profile?.level ?: 1,
                        penaltyActive = penalty.active,
                        onSettings = { nav.navigate(Routes.SETTINGS) { launchSingleTop = true } },
                        onPenalty = { nav.navigate(Routes.PENALTY) { launchSingleTop = true } },
                    )
                }
            },
            bottomBar = {
                if (route in tabs.map { it.route }) BottomBar(nav, route)
            },
        ) { padding ->
            NavHost(nav, startDestination = startDest, modifier = Modifier.padding(padding)) {
                composable(Routes.ONBOARDING) {
                    OnboardingScreen(vm) {
                        nav.navigate(Routes.PERMISSIONS) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                    }
                }
                composable(Routes.PERMISSIONS) {
                    val st by vm.settings.collectAsStateWithLifecycle()
                    val intro = st?.permissionsIntroDone == false
                    PermissionsScreen(vm, introMode = intro, onBack = { nav.popBackStack() }) {
                        if (intro) {
                            vm.finishPermissionsIntro()
                            nav.navigate(Routes.BLOCKED_APPS) { popUpTo(Routes.PERMISSIONS) { inclusive = true } }
                        } else nav.popBackStack()
                    }
                }
                composable(Routes.BLOCKED_APPS) {
                    BlockedAppsScreen(vm) {
                        if (nav.previousBackStackEntry != null) nav.popBackStack()
                        else nav.navigate(Routes.STATUS) { popUpTo(Routes.BLOCKED_APPS) { inclusive = true } }
                    }
                }
                composable(Routes.STATUS) { StatusScreen(vm) }
                composable(Routes.QUESTS) { QuestsScreen(vm) }
                composable(Routes.TRAINING) { TrainingScreen(vm) }
                composable(Routes.SKILLS) { SkillsScreen(vm) }
                composable(Routes.STATS) { StatsScreen(vm) }
                composable(Routes.SETTINGS) {
                    SettingsScreen(
                        vm,
                        onBack = { nav.popBackStack() },
                        onPermissions = { nav.navigate(Routes.PERMISSIONS) },
                        onBlockedApps = { nav.navigate(Routes.BLOCKED_APPS) },
                    )
                }
                composable(Routes.PENALTY) { PenaltyScreen(vm, onBack = { nav.popBackStack() }) }
            }
        }
        SystemOverlays(vm)
    }
}

@Composable
private fun TopBar(level: Int, penaltyActive: Boolean, onSettings: () -> Unit, onPenalty: () -> Unit) {
    Column(Modifier.fillMaxWidth().statusBarsPadding()) {
        Row(
            Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("AWAKENED", style = MaterialTheme.typography.headlineMedium, color = Sys.Cyan)
            Spacer(Modifier.width(10.dp))
            Box(Modifier.glowBorder(Sys.Cyan, 4.dp, 1.dp, 0.4f).padding(horizontal = 8.dp, vertical = 2.dp)) {
                Text("LV $level · ${Rank.forLevel(level).letter}", style = MaterialTheme.typography.labelMedium, color = Sys.Text)
            }
            Spacer(Modifier.weight(1f))
            IconButton(onClick = onSettings, modifier = Modifier.size(52.dp)) {
                Icon(Icons.Filled.Settings, contentDescription = "Einstellungen", tint = Sys.Muted)
            }
        }
        if (penaltyActive) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .glowBorder(Sys.Red, 6.dp)
                    .background(Sys.Red.copy(alpha = 0.15f))
                    .clickable(onClick = onPenalty)
                    .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Filled.Warning, null, tint = Sys.Red)
                Spacer(Modifier.width(10.dp))
                Text("PENALTY ZONE AKTIV – Straf-Quest öffnen", color = Sys.Text, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun BottomBar(nav: NavHostController, route: String?) {
    NavigationBar(containerColor = Sys.Navy1.copy(alpha = 0.97f), modifier = Modifier.windowInsetsPadding(WindowInsets(0)), tonalElevation = 0.dp) {
        tabs.forEach { t ->
            NavigationBarItem(
                selected = route == t.route,
                onClick = {
                    nav.navigate(t.route) {
                        popUpTo(Routes.STATUS) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(t.icon, contentDescription = t.label) },
                label = { Text(t.label, style = MaterialTheme.typography.labelMedium) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Sys.Cyan, selectedTextColor = Sys.Cyan, indicatorColor = Sys.Cyan.copy(alpha = 0.15f),
                    unselectedIconColor = Sys.Dim, unselectedTextColor = Sys.Dim,
                ),
            )
        }
    }
}

// ------------------------------------------------------------------- Overlays

@Composable
private fun SystemOverlays(vm: MainViewModel) {
    var levelUp by remember { mutableStateOf<GameEvent.LevelUp?>(null) }
    val toasts = remember { mutableStateListOf<String>() }
    var message by remember { mutableStateOf<GameEvent.SystemMessage?>(null) }
    val appToast by vm.toast.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        vm.repo.events.collect { e ->
            val s = vm.settings.value
            val sound = s?.soundEnabled ?: true
            val vib = s?.vibrationEnabled ?: true
            when (e) {
                is GameEvent.LevelUp -> { levelUp = e; vm.feedback.play(Feedback.Kind.LEVEL_UP, sound, vib) }
                is GameEvent.QuestCleared -> { toasts.add("[QUEST ERFÜLLT] ${e.title}  +${e.xp} XP"); vm.feedback.play(Feedback.Kind.QUEST, sound, vib) }
                is GameEvent.SystemMessage -> { message = e; vm.feedback.play(Feedback.Kind.QUEST, sound, vib) }
                is GameEvent.XpGained -> Unit
            }
        }
    }
    LaunchedEffect(appToast) {
        appToast?.let { toasts.add(it); vm.consumeToast() }
    }

    // Toasts oben
    val current = toasts.firstOrNull()
    LaunchedEffect(current) {
        if (current != null) { delay(2600); toasts.remove(current) }
    }
    Box(Modifier.fillMaxSize().statusBarsPadding().padding(top = 8.dp), contentAlignment = Alignment.TopCenter) {
        AnimatedVisibility(
            visible = current != null,
            enter = slideInVertically { -it } + fadeIn(),
            exit = slideOutVertically { -it } + fadeOut(),
        ) {
            SystemWindow(Modifier.padding(horizontal = 16.dp), contentPadding = 14.dp) {
                Text(current ?: "", color = Sys.Text, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
            }
        }
    }

    message?.let { m ->
        SystemDialog(m.title, onDismiss = { message = null }) {
            SystemText(m.text)
            Spacer(Modifier.height(16.dp))
            SystemButton("Bestätigen", { message = null }, Modifier.fillMaxWidth())
        }
    }

    levelUp?.let { e -> LevelUpOverlay(e) { levelUp = null } }
}

@Composable
fun LevelUpOverlay(e: GameEvent.LevelUp, onDismiss: () -> Unit) {
    val scale = remember { Animatable(0.2f) }
    val alpha = remember { Animatable(0f) }
    LaunchedEffect(e) {
        alpha.animateTo(1f, tween(250))
        scale.animateTo(1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow))
    }
    val t = rememberInfiniteTransition(label = "rays")
    val rot by t.animateFloat(0f, 360f, infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "rot")
    val pulse by t.animateFloat(0.85f, 1.1f, infiniteRepeatable(tween(1100), RepeatMode.Reverse), label = "pulse")

    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { this.alpha = alpha.value }
            .background(Sys.Navy0.copy(alpha = 0.93f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(420.dp).rotate(rot)) {
            val c = Offset(size.width / 2, size.height / 2)
            for (i in 0 until 24) {
                val a = Math.toRadians(i * 15.0)
                val end = Offset(c.x + (size.width / 2) * kotlin.math.cos(a).toFloat(), c.y + (size.width / 2) * kotlin.math.sin(a).toFloat())
                drawLine(
                    Brush.linearGradient(listOf(Sys.Cyan.copy(alpha = 0.45f), Color.Transparent), c, end),
                    c, end, strokeWidth = if (i % 2 == 0) 6f else 2f,
                )
            }
            drawCircle(Brush.radialGradient(listOf(Sys.Cyan.copy(alpha = 0.35f), Color.Transparent), c, size.width / 3), size.width / 3, c)
        }
        Column(
            Modifier.padding(24.dp).scale(scale.value).windowInsetsPadding(WindowInsets.navigationBars),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("[SYSTEM]", color = Sys.Muted, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "LEVEL UP!", color = Sys.Text, style = MaterialTheme.typography.displayLarge.copy(fontSize = 64.sp),
                modifier = Modifier.scale(pulse),
            )
            Spacer(Modifier.height(8.dp))
            Text("Level ${e.newLevel}", color = Sys.Cyan, style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            SystemWindow(Modifier.width(300.dp)) {
                SystemText("Du bist stärker geworden.", align = TextAlign.Center, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(6.dp))
                Text(
                    "+${e.statPoints} Stat-Punkte verfügbar", color = Sys.Gold, style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                )
                if (e.newRank) {
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Neuer Rang: ${Rank.forLevel(e.newLevel).title}", color = Sys.Cyan,
                        style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth(),
                    )
                }
                Spacer(Modifier.height(14.dp))
                SystemButton("Bestätigen", onDismiss, Modifier.fillMaxWidth(), filled = true)
            }
        }
    }
}
