package ch.awakened.app.domain

import kotlin.math.roundToInt

object QuestScaling {
    /** Startwerte pro Fitnesslevel. Ausdauer in Minuten. */
    fun base(fitness: FitnessLevel, exercise: Exercise): Int = when (fitness) {
        FitnessLevel.BEGINNER -> when (exercise) {
            Exercise.PUSHUPS -> 10; Exercise.SITUPS -> 15; Exercise.SQUATS -> 20
            Exercise.JUMP_ROPE -> 50; Exercise.CARDIO -> 10; Exercise.BURPEES -> 20
        }
        FitnessLevel.INTERMEDIATE -> when (exercise) {
            Exercise.PUSHUPS -> 25; Exercise.SITUPS -> 30; Exercise.SQUATS -> 40
            Exercise.JUMP_ROPE -> 150; Exercise.CARDIO -> 20; Exercise.BURPEES -> 40
        }
        FitnessLevel.ADVANCED -> when (exercise) {
            Exercise.PUSHUPS -> 50; Exercise.SITUPS -> 60; Exercise.SQUATS -> 80
            Exercise.JUMP_ROPE -> 300; Exercise.CARDIO -> 30; Exercise.BURPEES -> 60
        }
    }

    /** Obergrenzen, damit die Ziele realistisch bleiben. */
    fun cap(exercise: Exercise): Int = when (exercise) {
        Exercise.PUSHUPS, Exercise.SITUPS, Exercise.SQUATS -> 100
        Exercise.JUMP_ROPE -> 1000
        Exercise.CARDIO -> 60
        Exercise.BURPEES -> 100
    }

    private fun goalFactor(goal: Goal, exercise: Exercise): Double = when (goal) {
        Goal.STRENGTH -> if (exercise in setOf(Exercise.PUSHUPS, Exercise.SQUATS, Exercise.SITUPS)) 1.2 else 1.0
        Goal.ENDURANCE -> if (exercise in setOf(Exercise.CARDIO, Exercise.JUMP_ROPE)) 1.2 else 1.0
        Goal.WEIGHT_LOSS -> if (exercise in setOf(Exercise.CARDIO, Exercise.JUMP_ROPE)) 1.15 else 1.0
    }

    /** Langsames Wachstum: +4 % pro Level über 1. */
    fun levelFactor(level: Int): Double = 1.0 + 0.04 * (level.coerceAtLeast(1) - 1)

    /**
     * Tagesziel einer Übung. Für [Exercise.CARDIO] wird bei [CardioUnit.KILOMETERS]
     * in km umgerechnet (6 min pro km, gerundet auf 0,5 km, intern ×10 gespeichert).
     */
    fun dailyTarget(level: Int, exercise: Exercise, fitness: FitnessLevel, goal: Goal): Int {
        val raw = base(fitness, exercise) * levelFactor(level) * goalFactor(goal, exercise)
        val capped = raw.coerceAtMost(cap(exercise).toDouble())
        val step = when (exercise) {
            Exercise.CARDIO -> 1
            Exercise.JUMP_ROPE -> 10
            else -> 5
        }
        return ((capped / step).roundToInt() * step).coerceAtLeast(step)
    }

    /** Ausdauer-Ziel in Zehntel-km (z. B. 25 = 2,5 km). */
    fun cardioMinutesToTenthKm(minutes: Int): Int = (((minutes / 6.0) * 2).roundToInt() * 5).coerceAtLeast(5)

    fun cardioTarget(level: Int, fitness: FitnessLevel, goal: Goal, unit: CardioUnit): Int {
        val minutes = dailyTarget(level, Exercise.CARDIO, fitness, goal)
        return if (unit == CardioUnit.MINUTES) minutes else cardioMinutesToTenthKm(minutes)
    }

    /** Weekly-Ziel für die Gesamtzahl Liegestütze: 5 × Tagesziel. */
    fun weeklyPushupTarget(dailyPushups: Int): Int = dailyPushups * 5

    /** Straf-Quest abhängig vom Fitnesslevel: Burpees + Ausdauer-Minuten. */
    fun penaltyQuest(fitness: FitnessLevel): Pair<Int, Int> = when (fitness) {
        FitnessLevel.BEGINNER -> 30 to 15
        FitnessLevel.INTERMEDIATE -> 50 to 20
        FitnessLevel.ADVANCED -> 80 to 30
    }
}
