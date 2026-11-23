package ch.awakened.app.system

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import ch.awakened.app.MainActivity
import ch.awakened.app.R
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.penalty.PenaltyActivity

object Notifier {
    const val CH_QUEST = "quest"
    const val CH_PENALTY = "penalty"
    const val CH_SYSTEM = "system"
    const val CH_MONITOR = "monitor"

    private const val ID_REMINDER = 100
    private const val ID_PENALTY = 101
    private const val ID_SYSTEM = 102
    const val ID_MONITOR = 103

    private val CYAN = Color.rgb(0x3D, 0xE8, 0xFF)
    private val RED = Color.rgb(0xFF, 0x2E, 0x4D)

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)
        nm.createNotificationChannels(
            listOf(
                NotificationChannel(CH_QUEST, "Quest-Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Erinnert dich, wenn die Daily Quest noch offen ist."
                },
                NotificationChannel(CH_PENALTY, "Penalty Zone", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Meldungen über Sperren und Straf-Quests."
                    enableLights(true); lightColor = RED
                },
                NotificationChannel(CH_SYSTEM, "System-Meldungen", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "Level-ups, Skills und Joker."
                },
                NotificationChannel(CH_MONITOR, "Sperr-Überwachung", NotificationManager.IMPORTANCE_LOW).apply {
                    description = "Läuft nur, solange die Penalty Zone aktiv ist."
                },
            ),
        )
    }

    private fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    private fun openApp(context: Context, route: String? = null): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            route?.let { putExtra(MainActivity.EXTRA_ROUTE, it) }
        }
        return PendingIntent.getActivity(
            context, route.hashCode(), intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun post(context: Context, id: Int, builder: NotificationCompat.Builder) {
        if (!canPost(context)) return
        try {
            NotificationManagerCompat.from(context).notify(id, builder.build())
        } catch (_: SecurityException) {
        }
    }

    fun reminder(context: Context, minutesLeft: Int) {
        val text = "Die Daily Quest ist noch nicht abgeschlossen. Verbleibende Zeit: ${DateUtil.formatRemaining(minutesLeft)}"
        post(
            context, ID_REMINDER,
            NotificationCompat.Builder(context, CH_QUEST)
                .setSmallIcon(R.drawable.ic_stat_system)
                .setColor(CYAN)
                .setContentTitle("[QUEST INFO]")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText("$text\n\nBei Nichterfüllung folgt die PENALTY ZONE."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(openApp(context, "quests"))
                .setAutoCancel(true),
        )
    }

    fun penaltyStarted(context: Context, state: PenaltyState) {
        val intent = Intent(context, PenaltyActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val pi = PendingIntent.getActivity(context, 7, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = "${state.reasonEnum.label} Ausgewählte Apps sind gesperrt. " +
            "Straf-Quest: ${state.burpeesTarget} Burpees + ${state.cardioTarget} min Ausdauer."
        post(
            context, ID_PENALTY,
            NotificationCompat.Builder(context, CH_PENALTY)
                .setSmallIcon(R.drawable.ic_stat_system)
                .setColor(RED)
                .setContentTitle("[WARNUNG] PENALTY ZONE")
                .setContentText(text)
                .setStyle(NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setContentIntent(pi)
                .setAutoCancel(true),
        )
    }

    fun penaltyCleared(context: Context, byQuest: Boolean) {
        NotificationManagerCompat.from(context).cancel(ID_PENALTY)
        val text = if (byQuest) "Straf-Quest abgeschlossen. Alle Apps sind wieder freigegeben."
        else "Die Sperrzeit ist abgelaufen. Alle Apps sind wieder freigegeben."
        post(
            context, ID_SYSTEM,
            NotificationCompat.Builder(context, CH_SYSTEM)
                .setSmallIcon(R.drawable.ic_stat_system)
                .setColor(CYAN)
                .setContentTitle("[SYSTEM] Penalty Zone verlassen")
                .setContentText(text)
                .setContentIntent(openApp(context))
                .setAutoCancel(true),
        )
    }

    fun jokerUsed(context: Context, date: String) {
        post(
            context, ID_SYSTEM,
            NotificationCompat.Builder(context, CH_SYSTEM)
                .setSmallIcon(R.drawable.ic_stat_system)
                .setColor(CYAN)
                .setContentTitle("[SKILL] Zweiter Atem")
                .setContentText("Die verpasste Daily Quest vom $date wurde durch deinen Joker gedeckt.")
                .setContentIntent(openApp(context))
                .setAutoCancel(true),
        )
    }

    fun monitorNotification(context: Context) =
        NotificationCompat.Builder(context, CH_MONITOR)
            .setSmallIcon(R.drawable.ic_stat_system)
            .setColor(RED)
            .setContentTitle("PENALTY ZONE aktiv")
            .setContentText("Gesperrte Apps werden überwacht, bis die Straf-Quest erfüllt ist.")
            .setOngoing(true)
            .setContentIntent(openApp(context, "penalty"))
            .build()
}
