package ch.awakened.app.domain

enum class Sex(val label: String) { MALE("Männlich"), FEMALE("Weiblich"), DIVERSE("Divers") }

enum class FitnessLevel(val label: String) {
    BEGINNER("Anfänger"), INTERMEDIATE("Mittel"), ADVANCED("Fortgeschritten")
}

enum class Goal(val label: String) { STRENGTH("Kraft"), ENDURANCE("Ausdauer"), WEIGHT_LOSS("Abnehmen") }

enum class CardioUnit(val label: String, val short: String) { MINUTES("Minuten", "min"), KILOMETERS("Kilometer", "km") }

enum class CardioType(val label: String, val met: Double) {
    RUNNING("Laufen", 9.8), CYCLING("Velo", 7.5), WALKING("Gehen", 3.8), OTHER("Anderes", 6.0)
}

enum class Stat(val short: String, val label: String, val description: String) {
    STR("STR", "Stärke", "Mehr XP für Kraftübungen (Liegestütze, Kniebeugen)."),
    AGI("AGI", "Agilität", "Mehr XP für Seilspringen und Training."),
    VIT("VIT", "Vitalität", "Verkürzt die Sperrzeit in der Penalty Zone."),
    END("END", "Ausdauer", "Mehr XP für Ausdauer-Einheiten."),
    INT("INT", "Intelligenz", "Mehr XP für Challenges der Kategorie „Anderes“."),
}

enum class Exercise(
    val label: String,
    val unit: String,
    /** MET-Wert für die Kalorienschätzung. */
    val met: Double,
    /** Geschätzte Sekunden pro Wiederholung (für Kalorien aus Wiederholungen). */
    val secondsPerRep: Double,
    val xpStat: Stat,
) {
    PUSHUPS("Liegestütze", "Wdh.", 8.0, 2.0, Stat.STR),
    SITUPS("Sit-ups", "Wdh.", 3.8, 2.5, Stat.STR),
    SQUATS("Kniebeugen", "Wdh.", 5.0, 2.5, Stat.STR),
    JUMP_ROPE("Seilspringen", "Sprünge", 11.8, 0.5, Stat.AGI),
    CARDIO("Ausdauer", "min", 8.0, 60.0, Stat.END),
    BURPEES("Burpees", "Wdh.", 8.0, 4.0, Stat.AGI);

    companion object {
        val DAILY = listOf(PUSHUPS, SITUPS, SQUATS, JUMP_ROPE, CARDIO)
    }
}

enum class ChallengeCategory(val label: String) { FITNESS("Fitness"), OTHER("Anderes") }

enum class ChallengeFrequency(val label: String) { DAILY("Täglich"), WEEKLY("Wöchentlich"), ONCE("Einmalig") }
