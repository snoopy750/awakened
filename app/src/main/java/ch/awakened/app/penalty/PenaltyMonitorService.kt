package ch.awakened.app.penalty

import android.annotation.SuppressLint
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.IBinder
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import ch.awakened.app.MainActivity
import ch.awakened.app.R
import ch.awakened.app.container
import ch.awakened.app.data.prefs.AppSettings
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.system.Notifier
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Überwachungsdienst des Systems.
 *
 * - Läuft dauerhaft (Einstellung „Dauerhafte Überwachung“) oder nur während der Penalty Zone.
 * - Prüft regelmässig Deadline, Inaktivität und automatisches Entsperren.
 * - Erkennt während der Penalty Zone per Nutzungszugriff die Vordergrund-App und legt
 *   über gesperrte Apps ein rotes Vollbild-Overlay („Über anderen Apps einblenden“).
 */
class PenaltyMonitorService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var running = false

    private lateinit var settings: StateFlow<AppSettings?>
    private lateinit var penalty: StateFlow<PenaltyState?>

    private var overlay: View? = null
    private var countdownView: TextView? = null
    private var lastNotificationActive: Boolean? = null
    @Volatile private var suppressUntil = 0L

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val type = if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
        try {
            ServiceCompat.startForeground(this, Notifier.ID_MONITOR, Notifier.monitorNotification(this, penaltyActive = false), type)
        } catch (e: Exception) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (!running) {
            running = true
            val c = applicationContext.container
            settings = c.settings.settings.stateIn(scope, SharingStarted.Eagerly, null)
            penalty = c.settings.penalty.stateIn(scope, SharingStarted.Eagerly, null)
            scope.launch { loop() }
        }
        return START_STICKY
    }

    private suspend fun loop() {
        val c = applicationContext.container
        val usm = getSystemService(UsageStatsManager::class.java)
        val dynamic = AppCatalog.dynamicExemptions(this)
        var foreground: String? = null
        var lastEventTime = System.currentTimeMillis() - 3_600_000L
        var lastEvaluation = 0L

        while (scope.isActive) {
            val s = settings.value
            val p = penalty.value
            if (s == null || p == null) { delay(300); continue }

            if (!p.active && !s.monitorAlways) {
                withContext(Dispatchers.Main) { hideOverlay() }
                stopSelf()
                return
            }

            val now = System.currentTimeMillis()
            val evalInterval = if (p.active) 30_000L else 60_000L
            if (now - lastEvaluation > evalInterval) {
                lastEvaluation = now
                runCatching { c.repository.evaluatePenalties(now) }
            }
            updateNotification(p.active)

            if (p.active && Permissions.hasUsageAccess(this)) {
                // Neue Vordergrund-Ereignisse seit der letzten Abfrage einlesen
                val events = usm.queryEvents(lastEventTime, now + 1000)
                val e = UsageEvents.Event()
                val resumed = if (Build.VERSION.SDK_INT >= 29) UsageEvents.Event.ACTIVITY_RESUMED
                else @Suppress("DEPRECATION") UsageEvents.Event.MOVE_TO_FOREGROUND
                while (events.hasNextEvent()) {
                    events.getNextEvent(e)
                    if (e.eventType == resumed) {
                        foreground = e.packageName
                        lastEventTime = maxOf(lastEventTime, e.timeStamp)
                    }
                }
                val fg = foreground
                val block = System.currentTimeMillis() > suppressUntil && fg != null && fg != packageName && BlockGuard.shouldBlock(this, fg, p, s.blockedApps, dynamic)
                withContext(Dispatchers.Main) {
                    if (block) showOverlay(p) else hideOverlay()
                    countdownView?.text = countdownText(p)
                }
                delay(700)
            } else {
                withContext(Dispatchers.Main) { hideOverlay() }
                lastEventTime = now - 5_000
                delay(2_000)
            }
        }
    }

    private fun updateNotification(active: Boolean) {
        if (lastNotificationActive == active) return
        lastNotificationActive = active
        runCatching {
            NotificationManagerCompat.from(this).notify(Notifier.ID_MONITOR, Notifier.monitorNotification(this, active))
        }
    }

    private fun countdownText(p: PenaltyState): String {
        val left = ((p.unlockAt - System.currentTimeMillis()) / 1000).coerceAtLeast(0)
        return "Automatisches Ende in ${DateUtil.formatDuration(left)}\n" +
            "Straf-Quest: ${p.burpeesDone}/${p.burpeesTarget} Burpees · ${p.cardioDone}/${p.cardioTarget} min Ausdauer"
    }

    // ------------------------------------------------------------------ Overlay

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    @SuppressLint("SetTextI18n")
    private fun showOverlay(p: PenaltyState) {
        if (overlay != null || !Permissions.canDrawOverlays(this)) return
        val wm = getSystemService(WindowManager::class.java)
        val red = Color.rgb(0xFF, 0x2E, 0x4D)
        val bold = ResourcesCompat.getFont(this, R.font.rajdhani_bold)
        val semi = ResourcesCompat.getFont(this, R.font.rajdhani_semibold)

        fun text(t: String, sizeSp: Float, color: Int, font: android.graphics.Typeface?) = TextView(this).apply {
            text = t; textSize = sizeSp; setTextColor(color); typeface = font; gravity = Gravity.CENTER
            letterSpacing = 0.08f
        }

        fun button(label: String, filled: Boolean, onClick: () -> Unit) = TextView(this).apply {
            text = label.uppercase(); textSize = 18f; typeface = bold; gravity = Gravity.CENTER
            setTextColor(Color.WHITE); letterSpacing = 0.1f
            minHeight = dp(56)
            background = GradientDrawable().apply {
                cornerRadius = dp(6).toFloat()
                setStroke(dp(2), red)
                setColor(if (filled) Color.argb(70, 0xFF, 0x2E, 0x4D) else Color.argb(20, 0xFF, 0x2E, 0x4D))
            }
            setOnClickListener { onClick() }
        }

        val window = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(22), dp(22), dp(22), dp(22))
            background = GradientDrawable().apply {
                cornerRadius = dp(8).toFloat()
                setStroke(dp(2), red)
                setColor(Color.argb(235, 0x2A, 0x02, 0x0A))
            }
            addView(text("[ WARNUNG ]", 16f, Color.argb(200, 255, 255, 255), semi))
            addView(text("PENALTY ZONE", 40f, red, bold))
            addView(text(p.reasonEnum.label, 17f, Color.WHITE, null).apply { setPadding(0, dp(10), 0, dp(4)) })
            addView(text("Diese App ist gesperrt, bis die Straf-Quest erfüllt ist.", 15f, Color.argb(190, 255, 255, 255), null))
            countdownView = text(countdownText(p), 15f, Color.WHITE, semi).apply { setPadding(0, dp(16), 0, dp(18)) }
            addView(countdownView)
            val lp = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
                .apply { topMargin = dp(10) }
            addView(button("Straf-Quest öffnen", true) {
                suppressUntil = System.currentTimeMillis() + 3_000
                hideOverlay()
                startActivity(
                    Intent(this@PenaltyMonitorService, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        .putExtra(MainActivity.EXTRA_ROUTE, "penalty"),
                )
            }, lp)
            addView(button("Zum Startbildschirm", false) { goHome() }, LinearLayout.LayoutParams(lp))
            addView(text("Telefon, SMS, Karten und Banking bleiben erlaubt.", 13f, Color.argb(150, 255, 255, 255), null).apply {
                setPadding(0, dp(14), 0, 0)
            })
        }

        val root = object : FrameLayout(this) {
            override fun dispatchKeyEvent(event: KeyEvent): Boolean {
                if (event.keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_UP) { goHome(); return true }
                return super.dispatchKeyEvent(event)
            }
        }.apply {
            setBackgroundColor(Color.argb(250, 0x14, 0x00, 0x05))
            isFocusableInTouchMode = true
            addView(window, FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT, Gravity.CENTER).apply {
                leftMargin = dp(20); rightMargin = dp(20)
            })
        }

        val type = if (Build.VERSION.SDK_INT >= 26) WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT, type,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED,
            PixelFormat.TRANSLUCENT,
        )
        try {
            wm.addView(root, params)
            root.requestFocus()
            overlay = root
            val s = settings.value
            if (s != null) applicationContext.container.feedback.play(ch.awakened.app.system.Feedback.Kind.ALERT, s.soundEnabled, s.vibrationEnabled)
        } catch (_: Exception) {
            overlay = null
        }
    }

    private fun hideOverlay() {
        val v = overlay ?: return
        overlay = null
        countdownView = null
        runCatching { getSystemService(WindowManager::class.java).removeView(v) }
    }

    private fun goHome() {
        suppressUntil = System.currentTimeMillis() + 1_500
        hideOverlay()
        startActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    override fun onDestroy() {
        running = false
        hideOverlay()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        /** Startet den Dienst, sofern der Nutzungszugriff erteilt ist. Scheitert im Hintergrund still. */
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
