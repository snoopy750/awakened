package ch.awakened.app.integrations

import android.content.Context
import androidx.activity.result.contract.ActivityResultContract
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ActiveCaloriesBurnedRecord
import androidx.health.connect.client.records.DistanceRecord
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.records.TotalCaloriesBurnedRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import ch.awakened.app.data.repo.GameRepository
import ch.awakened.app.domain.CardioType
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.FitnessCalc
import kotlinx.coroutines.flow.first
import java.time.Duration
import java.time.Instant

data class HealthSyncResult(val steps: Long, val importedSessions: Int)

class HealthConnectManager(private val context: Context) {
    val permissions = setOf(
        HealthPermission.getReadPermission(StepsRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(ActiveCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(TotalCaloriesBurnedRecord::class),
        HealthPermission.getReadPermission(DistanceRecord::class),
    )

    fun sdkStatus(): Int = HealthConnectClient.getSdkStatus(context)
    fun isAvailable(): Boolean = sdkStatus() == HealthConnectClient.SDK_AVAILABLE
    fun needsUpdate(): Boolean = sdkStatus() == HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED

    private val client: HealthConnectClient? by lazy {
        if (isAvailable()) HealthConnectClient.getOrCreate(context) else null
    }

    fun permissionContract(): ActivityResultContract<Set<String>, Set<String>> =
        PermissionController.createRequestPermissionResultContract()

    suspend fun hasAllPermissions(): Boolean {
        val c = client ?: return false
        return c.permissionController.getGrantedPermissions().containsAll(permissions)
    }

    private val cardioTypes = mapOf(
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING to CardioType.RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_RUNNING_TREADMILL to CardioType.RUNNING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING to CardioType.CYCLING,
        ExerciseSessionRecord.EXERCISE_TYPE_BIKING_STATIONARY to CardioType.CYCLING,
        ExerciseSessionRecord.EXERCISE_TYPE_WALKING to CardioType.WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_HIKING to CardioType.WALKING,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_POOL to CardioType.OTHER,
        ExerciseSessionRecord.EXERCISE_TYPE_SWIMMING_OPEN_WATER to CardioType.OTHER,
        ExerciseSessionRecord.EXERCISE_TYPE_ROWING_MACHINE to CardioType.OTHER,
        ExerciseSessionRecord.EXERCISE_TYPE_ELLIPTICAL to CardioType.OTHER,
        ExerciseSessionRecord.EXERCISE_TYPE_STAIR_CLIMBING to CardioType.OTHER,
    )

    /** Liest Schritte von heute und Trainings der letzten 2 Tage und verbucht sie. */
    suspend fun syncToday(repo: GameRepository): HealthSyncResult? {
        val c = client ?: return null
        if (!hasAllPermissions()) return null
        val start = Instant.ofEpochMilli(DateUtil.startOfDayMillis(DateUtil.today()))
        val now = Instant.now()

        val steps = c.aggregate(
            AggregateRequest(setOf(StepsRecord.COUNT_TOTAL), TimeRangeFilter.between(start, now)),
        )[StepsRecord.COUNT_TOTAL] ?: 0L
        repo.applySteps(steps)

        val weight = repo.profile.first()?.weightKg ?: 70.0
        val sessions = c.readRecords(
            ReadRecordsRequest(
                ExerciseSessionRecord::class,
                TimeRangeFilter.between(start.minus(Duration.ofDays(1)), now),
            ),
        ).records
        var imported = 0
        for (s in sessions) {
            val range = TimeRangeFilter.between(s.startTime, s.endTime)
            val agg = c.aggregate(
                AggregateRequest(
                    setOf(ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL, DistanceRecord.DISTANCE_TOTAL), range,
                ),
            )
            val type = cardioTypes[s.exerciseType]
            val durationSec = Duration.between(s.startTime, s.endTime).seconds
            val kcal = agg[ActiveCaloriesBurnedRecord.ACTIVE_CALORIES_TOTAL]?.inKilocalories?.toInt()
                ?: FitnessCalc.caloriesMet(type?.met ?: 6.0, weight, durationSec / 60.0)
            val km = agg[DistanceRecord.DISTANCE_TOTAL]?.inKilometers
            val title = s.title?.takeIf { it.isNotBlank() } ?: type?.label ?: "Training"
            if (repo.importExternalWorkout(
                    externalId = "hc:${s.metadata.id}", title = title,
                    startedAt = s.startTime.toEpochMilli(), durationSec = durationSec, kcal = kcal,
                    cardio = type != null, km = km,
                )
            ) imported++
        }
        return HealthSyncResult(steps, imported)
    }
}
