package ch.awakened.app

import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.FitnessCalc
import ch.awakened.app.domain.Sex
import ch.awakened.app.domain.Skill
import ch.awakened.app.domain.StatBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FitnessCalcTest {
    @Test
    fun bmi() {
        assertEquals(22.86, FitnessCalc.bmi(70.0, 175.0), 0.01)
        assertEquals("Normalgewicht", FitnessCalc.bmiCategory(22.86))
        assertEquals("Übergewicht", FitnessCalc.bmiCategory(27.0))
    }

    @Test
    fun `mifflin st jeor`() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, FitnessCalc.bmrMifflinStJeor(80.0, 180.0, 30, Sex.MALE), 0.01)
        // 10*60 + 6.25*165 - 5*25 - 161 = 1345.25
        assertEquals(1345.25, FitnessCalc.bmrMifflinStJeor(60.0, 165.0, 25, Sex.FEMALE), 0.01)
    }

    @Test
    fun `met calories`() {
        // 9.8 MET × 70 kg × 0.5 h = 343
        assertEquals(343, FitnessCalc.caloriesMet(9.8, 70.0, 30.0))
        assertEquals(0, FitnessCalc.caloriesForReps(Exercise.PUSHUPS, 0, 70.0))
    }

    @Test
    fun streaks() {
        assertEquals(3, DateUtil.streak(setOf(7, 8, 9), today = 10))
        assertEquals(4, DateUtil.streak(setOf(7, 8, 9, 10), today = 10))
        assertEquals(0, DateUtil.streak(setOf(5, 6), today = 10))
        assertEquals(3, DateUtil.bestStreak(setOf(1, 2, 3, 7, 8)))
    }

    @Test
    fun `skills unlock by level and stats`() {
        val base = StatBlock()
        assertFalse(Skill.SECOND_WIND.isUnlocked(4, base))
        assertTrue(Skill.SECOND_WIND.isUnlocked(5, base))
        assertFalse(Skill.ENDURANCE_BOOST.isUnlocked(20, base))
        assertTrue(Skill.ENDURANCE_BOOST.isUnlocked(8, base.copy(end = 15)))
    }
}
