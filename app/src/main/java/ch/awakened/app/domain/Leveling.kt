package ch.awakened.app.domain

import kotlin.math.floor
import kotlin.math.pow

data class LevelState(val level: Int, val xpInLevel: Long)

data class XpGainResult(val before: LevelState, val after: LevelState, val levelsGained: Int, val statPointsGained: Int)

object Leveling {
    const val STAT_POINTS_PER_LEVEL = 3
    const val MAX_LEVEL = 999

    /** Benötigte XP, um von [level] auf level + 1 aufzusteigen: 100 × Level^1.5 */
    fun xpForNextLevel(level: Int): Long {
        require(level >= 1) { "Level muss >= 1 sein" }
        return floor(100.0 * level.toDouble().pow(1.5)).toLong()
    }

    /** Gesamte XP, die nötig sind, um [level] zu erreichen (ab Level 1 mit 0 XP). */
    fun totalXpToReach(level: Int): Long = (1 until level).sumOf { xpForNextLevel(it) }

    fun addXp(state: LevelState, gained: Long): XpGainResult {
        require(gained >= 0) { "XP dürfen nicht negativ sein" }
        var level = state.level
        var xp = state.xpInLevel + gained
        var gainedLevels = 0
        while (level < MAX_LEVEL && xp >= xpForNextLevel(level)) {
            xp -= xpForNextLevel(level)
            level++
            gainedLevels++
        }
        return XpGainResult(state, LevelState(level, xp), gainedLevels, gainedLevels * STAT_POINTS_PER_LEVEL)
    }

    fun progress(state: LevelState): Float =
        (state.xpInLevel.toDouble() / xpForNextLevel(state.level)).toFloat().coerceIn(0f, 1f)
}

enum class Rank(val letter: String, val minLevel: Int, val title: String) {
    E("E", 1, "E-Rang Hunter"),
    D("D", 10, "D-Rang Hunter"),
    C("C", 20, "C-Rang Hunter"),
    B("B", 35, "B-Rang Hunter"),
    A("A", 50, "A-Rang Hunter"),
    S("S", 70, "S-Rang Hunter");

    companion object {
        fun forLevel(level: Int): Rank = entries.last { level >= it.minLevel }
        fun next(rank: Rank): Rank? = entries.getOrNull(rank.ordinal + 1)
    }
}

/** XP-Werte der verschiedenen Aktionen. */
object XpRules {
    const val DAILY_EXERCISE_XP = 20L
    const val WEEKLY_QUEST_XP = 150L
    const val PENALTY_QUEST_XP = 30L
    const val STEPS_PER_XP_UNIT = 1000L
    const val XP_PER_STEP_UNIT = 5L
    const val MAX_STEP_XP_PER_DAY = 100L

    /** Bonus, wenn alle Dailies erledigt sind. Steigt leicht mit dem Level. */
    fun allDailiesBonus(level: Int): Long = 50L + level * 2L

    /** XP für ein Training: 2 XP pro Minute plus 1 XP je 10 kcal. */
    fun trainingXp(minutes: Int, kcal: Int): Long = minutes * 2L + kcal / 10L

    fun stepXp(steps: Long): Long = ((steps / STEPS_PER_XP_UNIT) * XP_PER_STEP_UNIT).coerceAtMost(MAX_STEP_XP_PER_DAY)

    /** Stat-Bonus: +1 % pro Punkt über 10, max. +50 %. */
    fun statMultiplier(statValue: Int): Double = 1.0 + ((statValue - 10).coerceIn(0, 50) / 100.0)

    fun applyMultipliers(base: Long, statValue: Int, doubleXp: Boolean, extra: Double = 1.0): Long {
        val m = statMultiplier(statValue) * extra * (if (doubleXp) 2.0 else 1.0)
        return Math.round(base * m)
    }
}
