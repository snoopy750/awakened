package ch.awakened.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

data class DaySum(val epochDay: Long, val total: Long)

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = 1")
    suspend fun get(): ProfileEntity?

    @Upsert
    suspend fun upsert(profile: ProfileEntity)

    @Query("DELETE FROM profile")
    suspend fun clear()
}

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY epochDay ASC, id ASC")
    fun observeAll(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries")
    suspend fun all(): List<WeightEntry>

    @Insert
    suspend fun insert(entry: WeightEntry)

    @Query("DELETE FROM weight_entries WHERE id = :id")
    suspend fun delete(id: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<WeightEntry>)

    @Query("DELETE FROM weight_entries")
    suspend fun clear()
}

@Dao
interface DailyDao {
    @Query("SELECT * FROM daily_exercises WHERE epochDay = :day")
    fun observeDay(day: Long): Flow<List<DailyExerciseEntity>>

    @Query("SELECT * FROM daily_exercises WHERE epochDay = :day")
    suspend fun getDay(day: Long): List<DailyExerciseEntity>

    @Query("SELECT * FROM daily_exercises WHERE epochDay = :day AND exercise = :exercise")
    suspend fun get(day: Long, exercise: String): DailyExerciseEntity?

    @Upsert
    suspend fun upsert(e: DailyExerciseEntity)

    @Upsert
    suspend fun upsertAll(list: List<DailyExerciseEntity>)

    @Query("SELECT * FROM daily_exercises WHERE epochDay BETWEEN :from AND :to")
    suspend fun range(from: Long, to: Long): List<DailyExerciseEntity>

    @Query("SELECT * FROM daily_exercises WHERE epochDay BETWEEN :from AND :to")
    fun observeRange(from: Long, to: Long): Flow<List<DailyExerciseEntity>>

    @Query("SELECT COALESCE(SUM(done),0) FROM daily_exercises WHERE exercise = :exercise")
    fun observeTotal(exercise: String): Flow<Long>

    @Query("SELECT COALESCE(SUM(done),0) FROM daily_exercises WHERE exercise = :exercise")
    suspend fun total(exercise: String): Long

    @Query("SELECT * FROM daily_exercises")
    suspend fun all(): List<DailyExerciseEntity>

    @Query("DELETE FROM daily_exercises")
    suspend fun clear()
}

@Dao
interface DayRecordDao {
    @Query("SELECT * FROM day_records WHERE epochDay = :day")
    suspend fun get(day: Long): DayRecord?

    @Query("SELECT * FROM day_records WHERE epochDay = :day")
    fun observe(day: Long): Flow<DayRecord?>

    @Upsert
    suspend fun upsert(r: DayRecord)

    @Upsert
    suspend fun upsertAll(list: List<DayRecord>)

    @Query("SELECT * FROM day_records ORDER BY epochDay ASC")
    fun observeAll(): Flow<List<DayRecord>>

    @Query("SELECT * FROM day_records ORDER BY epochDay ASC")
    suspend fun all(): List<DayRecord>

    @Query("SELECT epochDay FROM day_records WHERE allDone = 1 OR outcome = 'JOKER_USED'")
    suspend fun completedDays(): List<Long>

    @Query("SELECT MAX(epochDay) FROM day_records WHERE active = 1")
    suspend fun lastActiveDay(): Long?

    @Query("DELETE FROM day_records")
    suspend fun clear()
}

@Dao
interface ChallengeDao {
    @Query("SELECT * FROM challenges WHERE archived = 0 ORDER BY id DESC")
    fun observeActive(): Flow<List<ChallengeEntity>>

    @Query("SELECT * FROM challenges")
    suspend fun all(): List<ChallengeEntity>

    @Query("SELECT * FROM challenges WHERE id = :id")
    suspend fun get(id: Long): ChallengeEntity?

    @Insert
    suspend fun insert(c: ChallengeEntity): Long

    @Upsert
    suspend fun upsert(c: ChallengeEntity)

    @Upsert
    suspend fun upsertAll(list: List<ChallengeEntity>)

    @Query("DELETE FROM challenges WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("SELECT * FROM challenge_progress")
    fun observeProgress(): Flow<List<ChallengeProgressEntity>>

    @Query("SELECT * FROM challenge_progress")
    suspend fun allProgress(): List<ChallengeProgressEntity>

    @Query("SELECT * FROM challenge_progress WHERE challengeId = :id AND periodKey = :period")
    suspend fun progress(id: Long, period: Long): ChallengeProgressEntity?

    @Upsert
    suspend fun upsertProgress(p: ChallengeProgressEntity)

    @Upsert
    suspend fun upsertAllProgress(list: List<ChallengeProgressEntity>)

    @Query("SELECT COUNT(*) FROM challenge_progress WHERE completed = 1")
    suspend fun completedCount(): Int

    @Query("DELETE FROM challenge_progress WHERE challengeId = :id")
    suspend fun deleteProgress(id: Long)

    @Query("DELETE FROM challenges")
    suspend fun clear()

    @Query("DELETE FROM challenge_progress")
    suspend fun clearProgress()
}

@Dao
interface WorkoutDao {
    @Query("SELECT * FROM workouts ORDER BY startedAt DESC")
    fun observeAll(): Flow<List<WorkoutEntity>>

    @Query("SELECT * FROM workouts")
    suspend fun all(): List<WorkoutEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(w: WorkoutEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<WorkoutEntity>)

    @Query("SELECT COUNT(*) FROM workouts WHERE externalId = :externalId")
    suspend fun countExternal(externalId: String): Int

    @Query("SELECT COUNT(*) FROM workouts WHERE source = 'APP'")
    suspend fun appWorkoutCount(): Int

    @Query("SELECT * FROM workouts WHERE epochDay BETWEEN :from AND :to")
    suspend fun range(from: Long, to: Long): List<WorkoutEntity>

    @Query("DELETE FROM workouts")
    suspend fun clear()
}

@Dao
interface WeeklyDao {
    @Query("SELECT * FROM weekly_claims WHERE weekStart = :weekStart")
    fun observeWeek(weekStart: Long): Flow<List<WeeklyClaimEntity>>

    @Query("SELECT * FROM weekly_claims WHERE weekStart = :weekStart")
    suspend fun week(weekStart: Long): List<WeeklyClaimEntity>

    @Query("SELECT * FROM weekly_claims")
    suspend fun all(): List<WeeklyClaimEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(c: WeeklyClaimEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<WeeklyClaimEntity>)

    @Query("DELETE FROM weekly_claims")
    suspend fun clear()
}

@Dao
interface XpLogDao {
    @Insert
    suspend fun insert(e: XpLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(list: List<XpLogEntity>)

    @Query("SELECT epochDay, SUM(amount) AS total FROM xp_log WHERE epochDay >= :from GROUP BY epochDay ORDER BY epochDay")
    fun observeDailyXp(from: Long): Flow<List<DaySum>>

    @Query("SELECT * FROM xp_log ORDER BY timestamp DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<XpLogEntity>>

    @Query("SELECT * FROM xp_log")
    suspend fun all(): List<XpLogEntity>

    @Query("DELETE FROM xp_log")
    suspend fun clear()
}
