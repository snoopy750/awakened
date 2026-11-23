package ch.awakened.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class, WeightEntry::class, DailyExerciseEntity::class, DayRecord::class,
        ChallengeEntity::class, ChallengeProgressEntity::class, WorkoutEntity::class,
        WeeklyClaimEntity::class, XpLogEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao
    abstract fun weightDao(): WeightDao
    abstract fun dailyDao(): DailyDao
    abstract fun dayRecordDao(): DayRecordDao
    abstract fun challengeDao(): ChallengeDao
    abstract fun workoutDao(): WorkoutDao
    abstract fun weeklyDao(): WeeklyDao
    abstract fun xpLogDao(): XpLogDao

    companion object {
        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context, AppDatabase::class.java, "awakened.db").build()
    }
}
