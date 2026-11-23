package ch.awakened.app.domain

import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object DateUtil {
    private val zone: ZoneId get() = ZoneId.systemDefault()
    val locale: Locale = Locale.forLanguageTag("de-CH")

    fun today(): Long = LocalDate.now(zone).toEpochDay()
    fun minuteOfDay(): Int = LocalDateTime.now(zone).let { it.hour * 60 + it.minute }

    /** Montag der Woche (epochDay). */
    fun weekStart(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).with(DayOfWeek.MONDAY).toEpochDay()

    fun format(epochDay: Long, pattern: String = "dd.MM.yyyy"): String =
        LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern(pattern, locale))

    fun epochDayOfMillis(millis: Long): Long =
        java.time.Instant.ofEpochMilli(millis).atZone(zone).toLocalDate().toEpochDay()

    fun startOfDayMillis(epochDay: Long): Long =
        LocalDate.ofEpochDay(epochDay).atStartOfDay(zone).toInstant().toEpochMilli()

    fun formatMinuteOfDay(m: Int): String = "%02d:%02d".format(m / 60, m % 60)

    fun formatDuration(totalSeconds: Long): String {
        val h = totalSeconds / 3600
        val m = (totalSeconds % 3600) / 60
        val s = totalSeconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
    }

    fun formatRemaining(minutes: Int): String = when {
        minutes >= 60 -> "${minutes / 60}h ${minutes % 60}min"
        else -> "${minutes}min"
    }

    /** Streak: Anzahl aufeinanderfolgender erfüllter Tage bis gestern bzw. heute. */
    fun streak(completedDays: Set<Long>, today: Long): Int {
        var day = if (today in completedDays) today else today - 1
        var count = 0
        while (day in completedDays) { count++; day-- }
        return count
    }

    fun bestStreak(completedDays: Set<Long>): Int {
        if (completedDays.isEmpty()) return 0
        val sorted = completedDays.sorted()
        var best = 1; var cur = 1
        for (i in 1 until sorted.size) {
            cur = if (sorted[i] == sorted[i - 1] + 1) cur + 1 else 1
            best = maxOf(best, cur)
        }
        return best
    }
}
