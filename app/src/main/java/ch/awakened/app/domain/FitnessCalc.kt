package ch.awakened.app.domain

import kotlin.math.roundToInt

object FitnessCalc {
    fun bmi(weightKg: Double, heightCm: Double): Double {
        if (heightCm <= 0) return 0.0
        val m = heightCm / 100.0
        return weightKg / (m * m)
    }

    fun bmiCategory(bmi: Double): String = when {
        bmi <= 0 -> "–"
        bmi < 18.5 -> "Untergewicht"
        bmi < 25 -> "Normalgewicht"
        bmi < 30 -> "Übergewicht"
        else -> "Adipositas"
    }

    /** Grundumsatz nach Mifflin-St Jeor in kcal/Tag. */
    fun bmrMifflinStJeor(weightKg: Double, heightCm: Double, age: Int, sex: Sex): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * age
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
            Sex.DIVERSE -> base - 78 // Mittelwert der beiden Konstanten
        }
    }

    /** Kalorienverbrauch: MET × Gewicht (kg) × Dauer (h). */
    fun caloriesMet(met: Double, weightKg: Double, minutes: Double): Int =
        (met * weightKg * (minutes / 60.0)).roundToInt()

    fun caloriesForReps(exercise: Exercise, reps: Int, weightKg: Double): Int =
        caloriesMet(exercise.met, weightKg, reps * exercise.secondsPerRep / 60.0)
}
