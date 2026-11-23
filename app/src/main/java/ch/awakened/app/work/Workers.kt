package ch.awakened.app.work

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import ch.awakened.app.container
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.penalty.PenaltyMonitorService
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Prüft Tageswechsel, Deadline, Inaktivität, Erinnerungen und automatisches Entsperren.
 */
class GameCheckWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val c = applicationContext.container
        if (c.repository.profile.first() == null) return Result.success()
        return try {
            c.repository.ensureDay()
            c.repository.evaluatePenalties()
            if (c.settings.currentPenalty().active || c.settings.current().monitorAlways) {
                PenaltyMonitorService.startIfPossible(applicationContext)
            }
            if (c.settings.current().healthConnectEnabled) runCatching { c.health.syncToday(c.repository) }
            WorkScheduler.scheduleDeadline(applicationContext)
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}

object WorkScheduler {
    private const val PERIODIC = "awakened_periodic_check"
    private const val DEADLINE = "awakened_deadline_check"
    private const val NOW = "awakened_check_now"

    fun scheduleAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.enqueueUniquePeriodicWork(
            PERIODIC, ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<GameCheckWorker>(15, TimeUnit.MINUTES).build(),
        )
        val app = context.applicationContext
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch { scheduleDeadline(app) }
    }

    /** Zusätzliche, möglichst pünktliche Prüfung eine Minute nach der Deadline. */
    suspend fun scheduleDeadline(context: Context) {
        val settings = context.container.settings.current()
        val now = DateUtil.minuteOfDay()
        val target = settings.deadlineMinute + 1
        var delayMin = target - now
        if (delayMin <= 0) delayMin += 24 * 60
        WorkManager.getInstance(context).enqueueUniqueWork(
            DEADLINE, ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<GameCheckWorker>().setInitialDelay(delayMin.toLong(), TimeUnit.MINUTES).build(),
        )
    }

    fun checkNow(context: Context) {
        WorkManager.getInstance(context).enqueueUniqueWork(
            NOW, ExistingWorkPolicy.REPLACE, OneTimeWorkRequestBuilder<GameCheckWorker>().build(),
        )
    }
}

/** Plant nach einem Neustart oder Update alle Prüfungen neu. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        WorkScheduler.scheduleAll(context)
        WorkScheduler.checkNow(context)
        val pending = goAsync()
        Thread {
            try {
                val c = context.container
                val needed = kotlinx.coroutines.runBlocking { c.settings.currentPenalty().active || c.settings.current().monitorAlways }
                if (needed) PenaltyMonitorService.startIfPossible(context)
            } finally {
                pending.finish()
            }
        }.start()
    }
}
