package ch.awakened.app

import ch.awakened.app.domain.CardioUnit
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.FitnessLevel
import ch.awakened.app.domain.Goal
import ch.awakened.app.domain.QuestScaling
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class QuestScalingTest {
    @Test
    fun `level 1 targets equal base values for neutral goal`() {
        // Ausdauer-Ziel bei Goal STRENGTH unverändert
        assertEquals(10, QuestScaling.dailyTarget(1, Exercise.CARDIO, FitnessLevel.BEGINNER, Goal.STRENGTH))
        assertEquals(15, QuestScaling.dailyTarget(1, Exercise.SITUPS, FitnessLevel.BEGINNER, Goal.ENDURANCE))
        assertEquals(50, QuestScaling.dailyTarget(1, Exercise.PUSHUPS, FitnessLevel.ADVANCED, Goal.ENDURANCE))
    }

    @Test
    fun `start values depend on fitness level`() {
        val b = QuestScaling.dailyTarget(1, Exercise.PUSHUPS, FitnessLevel.BEGINNER, Goal.ENDURANCE)
        val i = QuestScaling.dailyTarget(1, Exercise.PUSHUPS, FitnessLevel.INTERMEDIATE, Goal.ENDURANCE)
        val a = QuestScaling.dailyTarget(1, Exercise.PUSHUPS, FitnessLevel.ADVANCED, Goal.ENDURANCE)
        assertTrue(b < i && i < a)
    }

    @Test
    fun `targets grow slowly and monotonically with level`() {
        var prev = 0
        for (level in 1..60) {
            val t = QuestScaling.dailyTarget(level, Exercise.SQUATS, FitnessLevel.BEGINNER, Goal.ENDURANCE)
            assertTrue("Level $level: $t < $prev", t >= prev)
            prev = t
        }
        // Level 11: 20 × 1.4 = 28 -> auf 5 gerundet 30
        assertEquals(30, QuestScaling.dailyTarget(11, Exercise.SQUATS, FitnessLevel.BEGINNER, Goal.ENDURANCE))
    }

    @Test
    fun `targets are capped`() {
        assertEquals(100, QuestScaling.dailyTarget(500, Exercise.PUSHUPS, FitnessLevel.ADVANCED, Goal.STRENGTH))
        assertEquals(60, QuestScaling.dailyTarget(500, Exercise.CARDIO, FitnessLevel.ADVANCED, Goal.ENDURANCE))
        assertEquals(1000, QuestScaling.dailyTarget(500, Exercise.JUMP_ROPE, FitnessLevel.ADVANCED, Goal.ENDURANCE))
    }

    @Test
    fun `goal boosts matching exercises`() {
        val strength = QuestScaling.dailyTarget(1, Exercise.PUSHUPS, FitnessLevel.INTERMEDIATE, Goal.STRENGTH)
        val endurance = QuestScaling.dailyTarget(1, Exercise.PUSHUPS, FitnessLevel.INTERMEDIATE, Goal.ENDURANCE)
        assertEquals(30, strength) // 25 × 1.2
        assertEquals(25, endurance)
    }

    @Test
    fun `rounding steps`() {
        val jr = QuestScaling.dailyTarget(3, Exercise.JUMP_ROPE, FitnessLevel.BEGINNER, Goal.STRENGTH)
        assertEquals(0, jr % 10)
        val pu = QuestScaling.dailyTarget(7, Exercise.PUSHUPS, FitnessLevel.BEGINNER, Goal.ENDURANCE)
        assertEquals(0, pu % 5)
    }

    @Test
    fun `cardio km conversion in tenth km`() {
        // 30 min / 6 = 5 km -> 50
        assertEquals(50, QuestScaling.cardioMinutesToTenthKm(30))
        // 10 min -> 1.67 km -> auf 0.5 km gerundet = 1.5 km -> 15
        assertEquals(15, QuestScaling.cardioMinutesToTenthKm(10))
        assertEquals(20, QuestScaling.cardioTarget(1, FitnessLevel.INTERMEDIATE, Goal.STRENGTH, CardioUnit.MINUTES))
        assertEquals(35, QuestScaling.cardioTarget(1, FitnessLevel.INTERMEDIATE, Goal.STRENGTH, CardioUnit.KILOMETERS))
    }

    @Test
    fun `penalty quest scales with fitness`() {
        assertEquals(50 to 20, QuestScaling.penaltyQuest(FitnessLevel.INTERMEDIATE))
        assertTrue(QuestScaling.penaltyQuest(FitnessLevel.BEGINNER).first < QuestScaling.penaltyQuest(FitnessLevel.ADVANCED).first)
    }
}
