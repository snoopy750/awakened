package ch.awakened.app

import ch.awakened.app.domain.LevelState
import ch.awakened.app.domain.Leveling
import ch.awakened.app.domain.Rank
import ch.awakened.app.domain.XpRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelingTest {
    @Test
    fun `xp curve follows 100 times level pow 1_5`() {
        assertEquals(100L, Leveling.xpForNextLevel(1))
        assertEquals(282L, Leveling.xpForNextLevel(2)) // 282.84
        assertEquals(519L, Leveling.xpForNextLevel(3)) // 519.6
        assertEquals(800L, Leveling.xpForNextLevel(4))
        assertEquals(3162L, Leveling.xpForNextLevel(10)) // 3162.27
    }

    @Test
    fun `xp below threshold does not level up`() {
        val r = Leveling.addXp(LevelState(1, 0), 99)
        assertEquals(1, r.after.level)
        assertEquals(99L, r.after.xpInLevel)
        assertEquals(0, r.levelsGained)
        assertEquals(0, r.statPointsGained)
    }

    @Test
    fun `exact threshold levels up with zero remainder`() {
        val r = Leveling.addXp(LevelState(1, 0), 100)
        assertEquals(2, r.after.level)
        assertEquals(0L, r.after.xpInLevel)
        assertEquals(Leveling.STAT_POINTS_PER_LEVEL, r.statPointsGained)
    }

    @Test
    fun `large gain crosses multiple levels and keeps remainder`() {
        // 100 + 282 + 519 = 901 -> Level 4, Rest 99
        val r = Leveling.addXp(LevelState(1, 0), 1000)
        assertEquals(4, r.after.level)
        assertEquals(99L, r.after.xpInLevel)
        assertEquals(3, r.levelsGained)
        assertEquals(9, r.statPointsGained)
    }

    @Test
    fun `total xp to reach is sum of steps`() {
        assertEquals(0L, Leveling.totalXpToReach(1))
        assertEquals(901L, Leveling.totalXpToReach(4))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `negative xp is rejected`() {
        Leveling.addXp(LevelState(1, 0), -5)
    }

    @Test
    fun `progress is clamped`() {
        assertEquals(0.5f, Leveling.progress(LevelState(1, 50)), 0.001f)
        assertEquals(1f, Leveling.progress(LevelState(1, 500)), 0.001f)
    }

    @Test
    fun `ranks map to level ranges`() {
        assertEquals(Rank.E, Rank.forLevel(1))
        assertEquals(Rank.E, Rank.forLevel(9))
        assertEquals(Rank.D, Rank.forLevel(10))
        assertEquals(Rank.C, Rank.forLevel(20))
        assertEquals(Rank.B, Rank.forLevel(35))
        assertEquals(Rank.A, Rank.forLevel(50))
        assertEquals(Rank.S, Rank.forLevel(70))
        assertEquals(Rank.S, Rank.forLevel(400))
    }

    @Test
    fun `xp multipliers`() {
        assertEquals(20L, XpRules.applyMultipliers(20, 10, doubleXp = false))
        assertEquals(40L, XpRules.applyMultipliers(20, 10, doubleXp = true))
        assertEquals(22L, XpRules.applyMultipliers(20, 20, doubleXp = false)) // +10 %
        assertEquals(1.5, XpRules.statMultiplier(999), 0.0001)
        assertTrue(XpRules.allDailiesBonus(10) > XpRules.allDailiesBonus(1))
    }

    @Test
    fun `step xp is capped`() {
        assertEquals(0L, XpRules.stepXp(999))
        assertEquals(25L, XpRules.stepXp(5_500))
        assertEquals(XpRules.MAX_STEP_XP_PER_DAY, XpRules.stepXp(100_000))
    }
}
