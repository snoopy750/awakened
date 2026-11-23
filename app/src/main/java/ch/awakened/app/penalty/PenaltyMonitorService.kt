package ch.awakened.app.penalty

import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ch.awakened.app.container
import ch.awakened.app.system.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Fallback, falls die Bedienungshilfe nicht aktiv ist: fragt per UsageStats jede
 * Sekunde die Vordergrund-App ab. Läuft nur während der Penalty Zone.
 */
class PenaltyMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var running = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, Notifier.ID_MONITOR, Notifier.monitorNotification(this), type)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            running = true
            scope.launch { loop() }
        }
        return START_STICKY
    }

    private suspend fun loop() {
        val c = applicationContext.container
        val usm = getSystemService(UsageStatsManager::class.java)
        val dynamic = AppCatalog.dynamicExemptions(this)
        var lastPkg: String? = null
        var tick = 0
        while (scope.isActive) {
            val penalty = c.settings.penalty.first()
            val settings = c.settings.settings.first()
            if (!penalty.active || Permissions.isAccessibilityEnabled(this)) {
                if (penalty.active && System.currentTimeMillis() >= penalty.unlockAt) c.repository.evaluatePenalties()
                stopSelf(); return
            }
            if (tick++ % 60 == 0) runCatching { c.repository.evaluatePenalties() }
            if (Permissions.hasUsageAccess(this)) {
                val now = System.currentTimeMillis()
                val events = usm.queryEvents(now - 10_000, now)
                val e = UsageEvents.Event()
                var fg: String? = lastPkg
                while (events.hasNextEvent()) {
                    events.getNextEvent(e)
                    val resumed = if (Build.VERSION.SDK_INT >= 29) UsageEvents.Event.ACTIVITY_RESUMED
                    else @Suppress("DEPRECATION") UsageEvents.Event.MOVE_TO_FOREGROUND
                    if (e.eventType == resumed) fg = e.packageName
                }
                lastPkg = fg
                if (fg != null && fg != packageName &&
                    BlockGuard.shouldBlock(this, fg, penalty, settings.blockedApps, dynamic)
                ) {
                    BlockGuard.showPenaltyScreen(this, fg)
                }
            }
            delay(1000)
        }
    }

    override fun onDestroy() {
        running = false
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        fun startIfPossible(context: Context) {
            if (!Permissions.hasUsageAccess(context)) return
            runCatching {
                ContextCompat.startForegroundService(context, Intent(context, PenaltyMonitorService::class.java))
            }
        }

        fun stop(context: Context) {
            runCatching { context.stopService(Intent(context, PenaltyMonitorService::class.java)) }
        }
    }
}
