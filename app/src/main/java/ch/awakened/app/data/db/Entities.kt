package ch.awakened.app.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "profile")
data class ProfileEntity(
    @PrimaryKey val id: Int = 1,
    val name: String,
    val age: Int,
    val sex: String,
    val heightCm: Double,
    val weightKg: Double,
    val fitnessLevel: String,
    val goal: String,
    val level: Int = 1,
    val xpInLevel: Long = 0,
    val totalXp: Long = 0,
    val statPoints: Int = 0,
    val str: Int = 10,
    val agi: Int = 10,
    val vit: Int = 10,
    val endurance: Int = 10,
    val intel: Int = 10,
    val activeTitle: String = "AWAKENED",
    val createdDay: Long,
)

@Serializable
@Entity(tableName = "weight_entries")
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val weightKg: Double,
)

@Serializable
@Entity(tableName = "daily_exercises", primaryKeys = ["epochDay", "exercise"])
data class DailyExerciseEntity(
    val epochDay: Long,
    val exercise: String,
    val target: Int,
    val done: Int = 0,
    /** "min", "km10" (Zehntel-km) oder "reps". */
    val unit: String = "reps",
    val xpAwarded: Boolean = false,
)

@Serializable
@Entity(tableName = "day_records")
data class DayRecord(
    @PrimaryKey val epochDay: Long,
    val allDone: Boolean = false,
    val bonusAwarded: Boolean = false,
    /** COMPLETED, JOKER_USED, FAILED oder null (noch nicht ausgewertet). */
    val outcome: String? = null,
    val xpEarned: Long = 0,
    val steps: Long = 0,
    val stepXp: Long = 0,
    val kcal: Int = 0,
    val active: Boolean = false,
)

@Serializable
@Entity(tableName = "challenges")
data class ChallengeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val category: String,
    val frequency: String,
    val target: Int,
    val unit: String,
    val xp: Long,
    val archived: Boolean = false,
    val createdDay: Long,
)

@Serializable
@Entity(tableName = "challenge_progress", primaryKeys = ["challengeId", "periodKey"])
data class ChallengeProgressEntity(
    val challengeId: Long,
    /** Tag (daily), Wochenstart (weekly) oder 0 (einmalig). */
    val periodKey: Long,
    val done: Int = 0,
    val completed: Boolean = false,
)

@Serializable
@Entity(tableName = "workouts", indices = [Index(value = ["externalId"], unique = true)])
data class WorkoutEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val epochDay: Long,
    val startedAt: Long,
    val durationSec: Long,
    val title: String,
    val kcal: Int,
    val xp: Long,
    /** APP oder HEALTH. */
    val source: String = "APP",
    val externalId: String? = null,
    val cardioMinutes: Int = 0,
    val details: String = "",
)

@Serializable
@Entity(tableName = "weekly_claims", primaryKeys = ["weekStart", "questKey"])
data class WeeklyClaimEntity(val weekStart: Long, val questKey: String)

@Serializable
@Entity(tableName = "xp_log")
data class XpLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Long,
    val epochDay: Long,
    val amount: Long,
    val reason: String,
)
