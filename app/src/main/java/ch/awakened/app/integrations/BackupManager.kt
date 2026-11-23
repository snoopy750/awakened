package ch.awakened.app.integrations

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import ch.awakened.app.data.db.AppDatabase
import ch.awakened.app.data.db.ChallengeEntity
import ch.awakened.app.data.db.ChallengeProgressEntity
import ch.awakened.app.data.db.DailyExerciseEntity
import ch.awakened.app.data.db.DayRecord
import ch.awakened.app.data.db.ProfileEntity
import ch.awakened.app.data.db.WeeklyClaimEntity
import ch.awakened.app.data.db.WeightEntry
import ch.awakened.app.data.db.WorkoutEntity
import ch.awakened.app.data.db.XpLogEntity
import ch.awakened.app.data.prefs.AppSettings
import ch.awakened.app.data.prefs.GameMeta
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.data.prefs.SettingsRepository
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class BackupData(
    val format: String = "awakened-backup",
    val version: Int = 1,
    val exportedAt: Long,
    val profile: ProfileEntity?,
    val weights: List<WeightEntry>,
    val daily: List<DailyExerciseEntity>,
    val days: List<DayRecord>,
    val challenges: List<ChallengeEntity>,
    val challengeProgress: List<ChallengeProgressEntity>,
    val workouts: List<WorkoutEntity>,
    val weeklyClaims: List<WeeklyClaimEntity>,
    val xpLog: List<XpLogEntity>,
    val settings: AppSettings,
    val penalty: PenaltyState,
    val meta: GameMeta,
)

class BackupManager(
    private val context: Context,
    private val db: AppDatabase,
    private val settings: SettingsRepository,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }

    suspend fun export(uri: Uri) {
        val data = BackupData(
            exportedAt = System.currentTimeMillis(),
            profile = db.profileDao().get(),
            weights = db.weightDao().all(),
            daily = db.dailyDao().all(),
            days = db.dayRecordDao().all(),
            challenges = db.challengeDao().all(),
            challengeProgress = db.challengeDao().allProgress(),
            workouts = db.workoutDao().all(),
            weeklyClaims = db.weeklyDao().all(),
            xpLog = db.xpLogDao().all(),
            settings = settings.current(),
            penalty = settings.currentPenalty(),
            meta = settings.currentMeta(),
        )
        context.contentResolver.openOutputStream(uri, "wt")?.use { out ->
            out.write(json.encodeToString(BackupData.serializer(), data).toByteArray())
        } ?: error("Datei konnte nicht geschrieben werden")
    }

    /** Ersetzt alle Daten durch das Backup. */
    suspend fun import(uri: Uri) {
        val text = context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
            ?: error("Datei konnte nicht gelesen werden")
        val data = json.decodeFromString(BackupData.serializer(), text)
        require(data.format == "awakened-backup") { "Keine AWAKENED-Sicherung" }
        db.withTransaction {
            db.profileDao().clear(); db.weightDao().clear(); db.dailyDao().clear(); db.dayRecordDao().clear()
            db.challengeDao().clear(); db.challengeDao().clearProgress(); db.workoutDao().clear()
            db.weeklyDao().clear(); db.xpLogDao().clear()
            data.profile?.let { db.profileDao().upsert(it) }
            db.weightDao().insertAll(data.weights)
            db.dailyDao().upsertAll(data.daily)
            db.dayRecordDao().upsertAll(data.days)
            db.challengeDao().upsertAll(data.challenges)
            db.challengeDao().upsertAllProgress(data.challengeProgress)
            db.workoutDao().insertAll(data.workouts)
            db.weeklyDao().insertAll(data.weeklyClaims)
            db.xpLogDao().insertAll(data.xpLog)
        }
        settings.restore(data.settings, data.penalty, data.meta)
    }
}
