package ch.awakened.app

import ch.awakened.app.domain.DayOutcome
import ch.awakened.app.domain.EmergencyExemptions
import ch.awakened.app.domain.PenaltyConfig
import ch.awakened.app.domain.PenaltyRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PenaltyRulesTest {
    private val cfg = PenaltyConfig(deadlineMinuteOfDay = 23 * 60 + 59, inactivityDays = 3, lockHours = 12)

    @Test
    fun `deadline check`() {
        assertFalse(PenaltyRules.isPastDeadline(23 * 60 + 58, cfg))
        assertTrue(PenaltyRules.isPastDeadline(23 * 60 + 59, cfg))
        val early = cfg.copy(deadlineMinuteOfDay = 20 * 60)
        assertTrue(PenaltyRules.isPastDeadline(21 * 60, early))
        assertEquals(180, PenaltyRules.minutesUntilDeadline(17 * 60, early))
        assertEquals(0, PenaltyRules.minutesUntilDeadline(22 * 60, early))
    }

    @Test
    fun `days to evaluate before deadline excludes today`() {
        assertEquals(listOf(9L), PenaltyRules.daysToEvaluate(8, 10, pastDeadlineToday = false, firstDay = 1))
        assertEquals(listOf(9L, 10L), PenaltyRules.daysToEvaluate(8, 10, pastDeadlineToday = true, firstDay = 1))
        assertTrue(PenaltyRules.daysToEvaluate(10, 10, pastDeadlineToday = true, firstDay = 1).isEmpty())
    }

    @Test
    fun `days to evaluate respects first day and lookback`() {
        assertTrue(PenaltyRules.daysToEvaluate(null, 10, pastDeadlineToday = false, firstDay = 10).isEmpty())
        assertEquals(listOf(10L), PenaltyRules.daysToEvaluate(null, 10, pastDeadlineToday = true, firstDay = 10))
        val long = PenaltyRules.daysToEvaluate(0, 100, pastDeadlineToday = false, firstDay = 1)
        assertEquals((93L..99L).toList(), long)
    }

    @Test
    fun `day outcome uses joker only when not completed`() {
        assertEquals(DayOutcome.COMPLETED, PenaltyRules.decideDay(completed = true, jokerAvailable = true))
        assertEquals(DayOutcome.JOKER_USED, PenaltyRules.decideDay(completed = false, jokerAvailable = true))
        assertEquals(DayOutcome.FAILED, PenaltyRules.decideDay(completed = false, jokerAvailable = false))
    }

    @Test
    fun `inactivity triggers after configured days`() {
        assertFalse(PenaltyRules.inactivityTriggered(today = 12, lastActiveDay = 10, anchorDay = 0, config = cfg))
        assertTrue(PenaltyRules.inactivityTriggered(today = 13, lastActiveDay = 10, anchorDay = 0, config = cfg))
        // Anker (z. B. nach Entsperren) verschiebt die Referenz
        assertFalse(PenaltyRules.inactivityTriggered(today = 13, lastActiveDay = 10, anchorDay = 12, config = cfg))
        // Nie aktiv gewesen: Start-Tag ist Referenz
        assertTrue(PenaltyRules.inactivityTriggered(today = 5, lastActiveDay = null, anchorDay = 2, config = cfg))
        // Regel deaktiviert
        assertFalse(PenaltyRules.inactivityTriggered(100, 1, 0, cfg.copy(inactivityDays = 0)))
    }

    @Test
    fun `unlock time halves with iron will and shrinks with vitality`() {
        val h = 3_600_000L
        assertEquals(12 * h, PenaltyRules.unlockAt(0, cfg, halved = false))
        assertEquals(6 * h, PenaltyRules.unlockAt(0, cfg, halved = true))
        assertEquals(10.8 * h, PenaltyRules.unlockAt(0, cfg, halved = false, vitality = 20).toDouble(), 1000.0)
        assertEquals(8.4 * h, PenaltyRules.unlockAt(0, cfg, halved = false, vitality = 99).toDouble(), 1000.0)
    }

    @Test
    fun `unlock by quest or by time`() {
        assertFalse(PenaltyRules.canUnlock(nowMillis = 10, unlockAtMillis = 100, questDone = false))
        assertTrue(PenaltyRules.canUnlock(nowMillis = 10, unlockAtMillis = 100, questDone = true))
        assertTrue(PenaltyRules.canUnlock(nowMillis = 100, unlockAtMillis = 100, questDone = false))
        assertTrue(PenaltyRules.penaltyQuestDone(50, 50, 20, 20))
        assertFalse(PenaltyRules.penaltyQuestDone(50, 50, 19, 20))
    }

    @Test
    fun `reminder thresholds fire once each`() {
        assertNull(PenaltyRules.reminderDue(200, emptySet()))
        assertEquals(180, PenaltyRules.reminderDue(180, emptySet()))
        assertNull(PenaltyRules.reminderDue(170, setOf(180)))
        assertEquals(60, PenaltyRules.reminderDue(55, setOf(180)))
        // Verpasste Stufen: nur die engste wird gemeldet
        assertEquals(15, PenaltyRules.reminderDue(10, emptySet()))
    }

    @Test
    fun `emergency apps are never blocked`() {
        val own = "ch.awakened.app"
        assertTrue(EmergencyExemptions.isExempt(own, own))
        assertTrue(EmergencyExemptions.isExempt("com.google.android.dialer", own))
        assertTrue(EmergencyExemptions.isExempt("com.google.android.apps.maps", own))
        assertTrue(EmergencyExemptions.isExempt("ch.twint.payment", own))
        assertTrue(EmergencyExemptions.isExempt("ch.postfinance.android", own))
        assertTrue(EmergencyExemptions.isExempt("com.ubs.Paymit.android", own))
        assertTrue(EmergencyExemptions.isExempt("com.example.sms", own, dynamicExempt = setOf("com.example.sms")))
        assertFalse(EmergencyExemptions.isExempt("com.instagram.android", own))
        assertFalse(EmergencyExemptions.isExempt("com.zhiliaoapp.musically", own))
        assertFalse(EmergencyExemptions.isExempt("com.google.android.youtube", own))
    }
}
