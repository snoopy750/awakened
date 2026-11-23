package ch.awakened.app.penalty

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import ch.awakened.app.container
import ch.awakened.app.data.prefs.PenaltyState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * Erkennt über Fensterwechsel, welche App im Vordergrund ist, und zeigt während
 * der Penalty Zone den Sperrbildschirm, sobald eine gesperrte App geöffnet wird.
 */
class PenaltyAccessibilityService : AccessibilityService() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Volatile private var penalty = PenaltyState()
    @Volatile private var blocked: Set<String> = emptySet()
    @Volatile private var dynamicExempt: Set<String> = emptySet()
    @Volatile private var lastEvaluation = 0L

    override fun onServiceConnected() {
        super.onServiceConnected()
        val c = applicationContext.container
        dynamicExempt = AppCatalog.dynamicExemptions(this)
        c.settings.penalty.onEach { penalty = it }.launchIn(scope)
        c.settings.settings.onEach { blocked = if (it.penaltyEnabled) it.blockedApps else emptySet() }.launchIn(scope)
        // Der Dienst läuft dauerhaft: guter Ort für eine zusätzliche Deadline-Prüfung.
        PenaltyMonitorService.stop(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName) return

        val now = System.currentTimeMillis()
        if (now - lastEvaluation > 60_000) {
            lastEvaluation = now
            scope.launch { runCatching { applicationContext.container.repository.evaluatePenalties() } }
        }

        if (BlockGuard.shouldBlock(this, pkg, penalty, blocked, dynamicExempt)) {
            performGlobalAction(GLOBAL_ACTION_HOME)
            BlockGuard.showPenaltyScreen(this, pkg)
        }
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
