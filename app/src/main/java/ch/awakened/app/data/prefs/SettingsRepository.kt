package ch.awakened.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ch.awakened.app.domain.CardioUnit
import ch.awakened.app.domain.PenaltyConfig
import ch.awakened.app.domain.PenaltyReason
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.Serializable

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "awakened_settings")

@Serializable
data class AppSettings(
    val onboardingDone: Boolean = false,
    val permissionsIntroDone: Boolean = false,
    val penaltyEnabled: Boolean = true,
    val deadlineMinute: Int = 23 * 60 + 59,
    val inactivityDays: Int = 3,
    val lockHours: Int = 12,
    val blockedApps: Set<String> = emptySet(),
    val spotifyPlaylist: String = "",
    val spotifyAutoStart: Boolean = true,
    val cardioUnit: String = CardioUnit.MINUTES.name,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val remindersEnabled: Boolean = true,
    val healthConnectEnabled: Boolean = false,
    val monitorAlways: Boolean = true,
) {
    val penaltyConfig: PenaltyConfig get() = PenaltyConfig(deadlineMinute, inactivityDays, lockHours)
    val cardio: CardioUnit get() = runCatching { CardioUnit.valueOf(cardioUnit) }.getOrDefault(CardioUnit.MINUTES)
}

@Serializable
data class PenaltyState(
    val active: Boolean = false,
    val reason: String = PenaltyReason.DAILY_MISSED.name,
    val startedAt: Long = 0,
    val unlockAt: Long = 0,
    val burpeesTarget: Int = 0,
    val burpeesDone: Int = 0,
    val cardioTarget: Int = 0,
    val cardioDone: Int = 0,
) {
    val reasonEnum: PenaltyReason get() = runCatching { PenaltyReason.valueOf(reason) }.getOrDefault(PenaltyReason.DAILY_MISSED)
}

/** Interner Spielzustand, der nicht in Room liegt. */
@Serializable
data class GameMeta(
    val lastEvaluatedDay: Long = -1,
    val inactivityAnchorDay: Long = 0,
    val penaltiesCleared: Int = 0,
    val jokerWeekUsed: Long = -1,
    val doubleXpWeekUsed: Long = -1,
    val doubleXpDay: Long = -1,
    val reminderDay: Long = -1,
    val remindersSent: Set<String> = emptySet(),
)

class SettingsRepository(private val context: Context) {
    private object K {
        val onboardingDone = booleanPreferencesKey("onboarding_done")
        val permissionsIntroDone = booleanPreferencesKey("permissions_intro_done")
        val penaltyEnabled = booleanPreferencesKey("penalty_enabled")
        val deadlineMinute = intPreferencesKey("deadline_minute")
        val inactivityDays = intPreferencesKey("inactivity_days")
        val lockHours = intPreferencesKey("lock_hours")
        val blockedApps = stringSetPreferencesKey("blocked_apps")
        val spotifyPlaylist = stringPreferencesKey("spotify_playlist")
        val spotifyAutoStart = booleanPreferencesKey("spotify_autostart")
        val cardioUnit = stringPreferencesKey("cardio_unit")
        val soundEnabled = booleanPreferencesKey("sound_enabled")
        val vibrationEnabled = booleanPreferencesKey("vibration_enabled")
        val remindersEnabled = booleanPreferencesKey("reminders_enabled")
        val healthConnectEnabled = booleanPreferencesKey("health_connect_enabled")
        val monitorAlways = booleanPreferencesKey("monitor_always")

        val pActive = booleanPreferencesKey("penalty_active")
        val pReason = stringPreferencesKey("penalty_reason")
        val pStartedAt = longPreferencesKey("penalty_started_at")
        val pUnlockAt = longPreferencesKey("penalty_unlock_at")
        val pBurpeesTarget = intPreferencesKey("penalty_burpees_target")
        val pBurpeesDone = intPreferencesKey("penalty_burpees_done")
        val pCardioTarget = intPreferencesKey("penalty_cardio_target")
        val pCardioDone = intPreferencesKey("penalty_cardio_done")

        val lastEvaluatedDay = longPreferencesKey("last_evaluated_day")
        val inactivityAnchorDay = longPreferencesKey("inactivity_anchor_day")
        val penaltiesCleared = intPreferencesKey("penalties_cleared")
        val jokerWeekUsed = longPreferencesKey("joker_week_used")
        val doubleXpWeekUsed = longPreferencesKey("double_xp_week_used")
        val doubleXpDay = longPreferencesKey("double_xp_day")
        val reminderDay = longPreferencesKey("reminder_day")
        val remindersSent = stringSetPreferencesKey("reminders_sent")
    }

    private val store get() = context.dataStore

    val settings: Flow<AppSettings> = store.data.map { p ->
        val d = AppSettings()
        AppSettings(
            onboardingDone = p[K.onboardingDone] ?: d.onboardingDone,
            permissionsIntroDone = p[K.permissionsIntroDone] ?: d.permissionsIntroDone,
            penaltyEnabled = p[K.penaltyEnabled] ?: d.penaltyEnabled,
            deadlineMinute = p[K.deadlineMinute] ?: d.deadlineMinute,
            inactivityDays = p[K.inactivityDays] ?: d.inactivityDays,
            lockHours = p[K.lockHours] ?: d.lockHours,
            blockedApps = p[K.blockedApps] ?: d.blockedApps,
            spotifyPlaylist = p[K.spotifyPlaylist] ?: d.spotifyPlaylist,
            spotifyAutoStart = p[K.spotifyAutoStart] ?: d.spotifyAutoStart,
            cardioUnit = p[K.cardioUnit] ?: d.cardioUnit,
            soundEnabled = p[K.soundEnabled] ?: d.soundEnabled,
            vibrationEnabled = p[K.vibrationEnabled] ?: d.vibrationEnabled,
            remindersEnabled = p[K.remindersEnabled] ?: d.remindersEnabled,
            healthConnectEnabled = p[K.healthConnectEnabled] ?: d.healthConnectEnabled,
            monitorAlways = p[K.monitorAlways] ?: d.monitorAlways,
        )
    }

    val penalty: Flow<PenaltyState> = store.data.map { p ->
        PenaltyState(
            active = p[K.pActive] ?: false,
            reason = p[K.pReason] ?: PenaltyReason.DAILY_MISSED.name,
            startedAt = p[K.pStartedAt] ?: 0,
            unlockAt = p[K.pUnlockAt] ?: 0,
            burpeesTarget = p[K.pBurpeesTarget] ?: 0,
            burpeesDone = p[K.pBurpeesDone] ?: 0,
            cardioTarget = p[K.pCardioTarget] ?: 0,
            cardioDone = p[K.pCardioDone] ?: 0,
        )
    }

    val meta: Flow<GameMeta> = store.data.map { p ->
        GameMeta(
            lastEvaluatedDay = p[K.lastEvaluatedDay] ?: -1,
            inactivityAnchorDay = p[K.inactivityAnchorDay] ?: 0,
            penaltiesCleared = p[K.penaltiesCleared] ?: 0,
            jokerWeekUsed = p[K.jokerWeekUsed] ?: -1,
            doubleXpWeekUsed = p[K.doubleXpWeekUsed] ?: -1,
            doubleXpDay = p[K.doubleXpDay] ?: -1,
            reminderDay = p[K.reminderDay] ?: -1,
            remindersSent = p[K.remindersSent] ?: emptySet(),
        )
    }

    suspend fun current(): AppSettings = settings.first()
    suspend fun currentPenalty(): PenaltyState = penalty.first()
    suspend fun currentMeta(): GameMeta = meta.first()

    suspend fun update(transform: (AppSettings) -> AppSettings) {
        store.edit { p ->
            val s = transform(readSettings(p))
            p[K.onboardingDone] = s.onboardingDone
            p[K.permissionsIntroDone] = s.permissionsIntroDone
            p[K.penaltyEnabled] = s.penaltyEnabled
            p[K.deadlineMinute] = s.deadlineMinute.coerceIn(0, 23 * 60 + 59)
            p[K.inactivityDays] = s.inactivityDays.coerceIn(0, 30)
            p[K.lockHours] = s.lockHours.coerceIn(1, 72)
            p[K.blockedApps] = s.blockedApps
            p[K.spotifyPlaylist] = s.spotifyPlaylist
            p[K.spotifyAutoStart] = s.spotifyAutoStart
            p[K.cardioUnit] = s.cardioUnit
            p[K.soundEnabled] = s.soundEnabled
            p[K.vibrationEnabled] = s.vibrationEnabled
            p[K.remindersEnabled] = s.remindersEnabled
            p[K.healthConnectEnabled] = s.healthConnectEnabled
            p[K.monitorAlways] = s.monitorAlways
        }
    }

    private fun readSettings(p: Preferences): AppSettings {
        val d = AppSettings()
        return AppSettings(
            onboardingDone = p[K.onboardingDone] ?: d.onboardingDone,
            permissionsIntroDone = p[K.permissionsIntroDone] ?: d.permissionsIntroDone,
            penaltyEnabled = p[K.penaltyEnabled] ?: d.penaltyEnabled,
            deadlineMinute = p[K.deadlineMinute] ?: d.deadlineMinute,
            inactivityDays = p[K.inactivityDays] ?: d.inactivityDays,
            lockHours = p[K.lockHours] ?: d.lockHours,
            blockedApps = p[K.blockedApps] ?: d.blockedApps,
            spotifyPlaylist = p[K.spotifyPlaylist] ?: d.spotifyPlaylist,
            spotifyAutoStart = p[K.spotifyAutoStart] ?: d.spotifyAutoStart,
            cardioUnit = p[K.cardioUnit] ?: d.cardioUnit,
            soundEnabled = p[K.soundEnabled] ?: d.soundEnabled,
            vibrationEnabled = p[K.vibrationEnabled] ?: d.vibrationEnabled,
            remindersEnabled = p[K.remindersEnabled] ?: d.remindersEnabled,
            healthConnectEnabled = p[K.healthConnectEnabled] ?: d.healthConnectEnabled,
            monitorAlways = p[K.monitorAlways] ?: d.monitorAlways,
        )
    }

    suspend fun setPenalty(state: PenaltyState) {
        store.edit { p -> writePenalty(p, state) }
    }

    private fun writePenalty(p: MutablePreferences, s: PenaltyState) {
        p[K.pActive] = s.active
        p[K.pReason] = s.reason
        p[K.pStartedAt] = s.startedAt
        p[K.pUnlockAt] = s.unlockAt
        p[K.pBurpeesTarget] = s.burpeesTarget
        p[K.pBurpeesDone] = s.burpeesDone
        p[K.pCardioTarget] = s.cardioTarget
        p[K.pCardioDone] = s.cardioDone
    }

    suspend fun updateMeta(transform: (GameMeta) -> GameMeta) {
        store.edit { p ->
            val cur = GameMeta(
                lastEvaluatedDay = p[K.lastEvaluatedDay] ?: -1,
                inactivityAnchorDay = p[K.inactivityAnchorDay] ?: 0,
                penaltiesCleared = p[K.penaltiesCleared] ?: 0,
                jokerWeekUsed = p[K.jokerWeekUsed] ?: -1,
                doubleXpWeekUsed = p[K.doubleXpWeekUsed] ?: -1,
                doubleXpDay = p[K.doubleXpDay] ?: -1,
                reminderDay = p[K.reminderDay] ?: -1,
                remindersSent = p[K.remindersSent] ?: emptySet(),
            )
            val m = transform(cur)
            p[K.lastEvaluatedDay] = m.lastEvaluatedDay
            p[K.inactivityAnchorDay] = m.inactivityAnchorDay
            p[K.penaltiesCleared] = m.penaltiesCleared
            p[K.jokerWeekUsed] = m.jokerWeekUsed
            p[K.doubleXpWeekUsed] = m.doubleXpWeekUsed
            p[K.doubleXpDay] = m.doubleXpDay
            p[K.reminderDay] = m.reminderDay
            p[K.remindersSent] = m.remindersSent
        }
    }

    /** Für Backup/Restore. */
    suspend fun restore(settings: AppSettings, penalty: PenaltyState, meta: GameMeta) {
        update { settings }
        setPenalty(penalty)
        updateMeta { meta }
    }
}
