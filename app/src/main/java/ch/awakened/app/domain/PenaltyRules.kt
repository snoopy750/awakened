package ch.awakened.app.domain

enum class PenaltyReason(val label: String) {
    DAILY_MISSED("Die Daily Quest wurde nicht rechtzeitig abgeschlossen."),
    INACTIVITY("Zu lange keine Aktivität."),
}

enum class DayOutcome { COMPLETED, JOKER_USED, FAILED }

data class PenaltyConfig(
    /** Deadline in Minuten nach Mitternacht, z. B. 23:59 = 1439. */
    val deadlineMinuteOfDay: Int = 23 * 60 + 59,
    /** 0 = Inaktivitäts-Regel aus. */
    val inactivityDays: Int = 3,
    val lockHours: Int = 12,
)

object PenaltyRules {
    const val MAX_DAYS_LOOKBACK = 7L

    fun isPastDeadline(minuteOfDay: Int, config: PenaltyConfig): Boolean = minuteOfDay >= config.deadlineMinuteOfDay

    fun minutesUntilDeadline(minuteOfDay: Int, config: PenaltyConfig): Int =
        (config.deadlineMinuteOfDay - minuteOfDay).coerceAtLeast(0)

    /**
     * Welche Tage müssen jetzt ausgewertet werden? Alle vergangenen, noch nicht
     * ausgewerteten Tage (max. 7 zurück, nie vor dem Start-Tag) sowie heute,
     * falls die Deadline vorbei ist.
     */
    fun daysToEvaluate(lastEvaluatedDay: Long?, today: Long, pastDeadlineToday: Boolean, firstDay: Long): List<Long> {
        val earliest = maxOf(firstDay, today - MAX_DAYS_LOOKBACK, (lastEvaluatedDay ?: (firstDay - 1)) + 1)
        val last = if (pastDeadlineToday) today else today - 1
        if (earliest > last) return emptyList()
        return (earliest..last).toList()
    }

    fun decideDay(completed: Boolean, jokerAvailable: Boolean): DayOutcome = when {
        completed -> DayOutcome.COMPLETED
        jokerAvailable -> DayOutcome.JOKER_USED
        else -> DayOutcome.FAILED
    }

    /**
     * Inaktivität: ausgelöst, wenn seit dem letzten aktiven Tag (oder dem letzten
     * Entsperren bzw. dem Start) mindestens [PenaltyConfig.inactivityDays] Tage vergangen sind.
     */
    fun inactivityTriggered(today: Long, lastActiveDay: Long?, anchorDay: Long, config: PenaltyConfig): Boolean {
        if (config.inactivityDays <= 0) return false
        val reference = maxOf(lastActiveDay ?: Long.MIN_VALUE, anchorDay)
        return today - reference >= config.inactivityDays
    }

    /** Zeitpunkt, ab dem die Sperre automatisch endet. Skill „Eiserner Wille“ halbiert die Zeit. */
    fun unlockAt(startMillis: Long, config: PenaltyConfig, halved: Boolean, vitality: Int = 10): Long {
        var hours = config.lockHours.toDouble()
        if (halved) hours /= 2.0
        // VIT reduziert um 1 % pro Punkt über 10, max. 30 %.
        hours *= 1.0 - ((vitality - 10).coerceIn(0, 30) / 100.0)
        return startMillis + (hours * 3_600_000L).toLong()
    }

    fun penaltyQuestDone(burpeesDone: Int, burpeesTarget: Int, cardioDone: Int, cardioTarget: Int): Boolean =
        burpeesDone >= burpeesTarget && cardioDone >= cardioTarget

    fun canUnlock(nowMillis: Long, unlockAtMillis: Long, questDone: Boolean): Boolean =
        questDone || nowMillis >= unlockAtMillis

    /** Erinnerungsstufen: 3 h, 1 h und 15 min vor der Deadline. */
    val REMINDER_THRESHOLDS = listOf(180, 60, 15)

    /** Liefert die Schwelle, für die jetzt eine Erinnerung fällig ist, oder null. */
    fun reminderDue(minutesLeft: Int, alreadySent: Set<Int>): Int? =
        REMINDER_THRESHOLDS.filter { minutesLeft <= it && it !in alreadySent }.minOrNull()
}

/** Apps, die nie gesperrt werden dürfen. */
object EmergencyExemptions {
    private val FIXED = setOf(
        "com.android.phone", "com.android.server.telecom", "com.google.android.dialer", "com.android.dialer",
        "com.samsung.android.dialer", "com.android.contacts", "com.google.android.contacts",
        "com.android.mms", "com.google.android.apps.messaging", "com.samsung.android.messaging",
        "com.google.android.apps.maps", "com.waze", "ch.sbb.mobile.android.b2c",
        "com.android.settings", "com.android.systemui", "com.android.emergency", "com.google.android.apps.safetyhub",
        "com.android.vending",
    )
    private val BANK_KEYWORDS = listOf(
        "bank", "twint", "postfinance", "ubs", "raiffeisen", "zkb", "credit_suisse", "creditsuisse",
        "revolut", "neon", "yuh", "wise", "paypal", "cembra", "viseca", "swissquote", "sparkasse", "valiant",
        "migrosbank", "bcv", "bcge", "lukb", "blkb", "sgkb", "tkb", "akb", "glkb", "bekb", "clientis",
    )

    fun isExempt(packageName: String, ownPackage: String, dynamicExempt: Set<String> = emptySet()): Boolean {
        if (packageName == ownPackage) return true
        if (packageName in FIXED || packageName in dynamicExempt) return true
        val lower = packageName.lowercase()
        return BANK_KEYWORDS.any { kw -> lower.split('.', '_', '-').any { part -> part == kw || part.startsWith(kw) } }
    }
}
