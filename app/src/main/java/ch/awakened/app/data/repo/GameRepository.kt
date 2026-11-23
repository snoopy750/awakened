package ch.awakened.app.data.repo

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
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.data.prefs.SettingsRepository
import ch.awakened.app.domain.Achievements
import ch.awakened.app.domain.CardioUnit
import ch.awakened.app.domain.ChallengeCategory
import ch.awakened.app.domain.ChallengeFrequency
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.DayOutcome
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.FitnessLevel
import ch.awakened.app.domain.Goal
import ch.awakened.app.domain.HunterTitle
import ch.awakened.app.domain.LevelState
import ch.awakened.app.domain.Leveling
import ch.awakened.app.domain.PenaltyReason
import ch.awakened.app.domain.PenaltyRules
import ch.awakened.app.domain.QuestScaling
import ch.awakened.app.domain.Skill
import ch.awakened.app.domain.Stat
import ch.awakened.app.domain.StatBlock
import ch.awakened.app.domain.XpGainResult
import ch.awakened.app.domain.XpRules
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Ereignisse, auf die die UI mit Animation, Sound und Vibration reagiert. */
sealed interface GameEvent {
    data class LevelUp(val newLevel: Int, val levelsGained: Int, val statPoints: Int, val newRank: Boolean) : GameEvent
    data class QuestCleared(val title: String, val xp: Long) : GameEvent
    data class XpGained(val amount: Long, val reason: String) : GameEvent
    data class SystemMessage(val title: String, val text: String) : GameEvent
}

data class WeeklyQuest(
    val key: String,
    val title: String,
    val description: String,
    val progress: Int,
    val target: Int,
    val claimed: Boolean,
) {
    val done get() = progress >= target
}

/** Hooks in die Android-Schicht (Benachrichtigungen, Sperr-Überwachung). */
interface PenaltyHooks {
    fun onPenaltyStarted(state: PenaltyState)
    fun onPenaltyCleared(byQuest: Boolean)
    fun onJokerUsed(day: Long)
    fun onReminder(minutesLeft: Int)
}

@OptIn(ExperimentalCoroutinesApi::class)
class GameRepository(
    private val db: AppDatabase,
    val settingsRepo: SettingsRepository,
    private val ownPackage: String,
) {
    var hooks: PenaltyHooks? = null
    private val mutex = Mutex()

    private val _events = MutableSharedFlow<GameEvent>(extraBufferCapacity = 16)
    val events: SharedFlow<GameEvent> = _events.asSharedFlow()

    private val _currentDay = MutableStateFlow(DateUtil.today())
    val currentDay: StateFlow<Long> = _currentDay.asStateFlow()

    val profile: Flow<ProfileEntity?> = db.profileDao().observe()
    val todayExercises: Flow<List<DailyExerciseEntity>> = _currentDay.flatMapLatest { db.dailyDao().observeDay(it) }
    val todayRecord: Flow<DayRecord?> = _currentDay.flatMapLatest { db.dayRecordDao().observe(it) }
    val weightEntries: Flow<List<WeightEntry>> = db.weightDao().observeAll()
    val dayRecords: Flow<List<DayRecord>> = db.dayRecordDao().observeAll()
    val workouts: Flow<List<WorkoutEntity>> = db.workoutDao().observeAll()
    val challenges: Flow<List<ChallengeEntity>> = db.challengeDao().observeActive()
    val challengeProgress: Flow<List<ChallengeProgressEntity>> = db.challengeDao().observeProgress()
    val weeklyClaims: Flow<List<WeeklyClaimEntity>> =
        _currentDay.flatMapLatest { db.weeklyDao().observeWeek(DateUtil.weekStart(it)) }
    val recentXp: Flow<List<XpLogEntity>> = db.xpLogDao().observeRecent(30)
    fun dailyXp(from: Long) = db.xpLogDao().observeDailyXp(from)
    fun exercisesInRange(from: Long, to: Long) = db.dailyDao().observeRange(from, to)
    fun totalOf(exercise: Exercise) = db.dailyDao().observeTotal(exercise.name)

    // ---------------------------------------------------------------- Profil

    suspend fun createProfile(
        name: String, age: Int, sex: String, heightCm: Double, weightKg: Double, fitness: FitnessLevel, goal: Goal,
    ) {
        val today = DateUtil.today()
        db.profileDao().upsert(
            ProfileEntity(
                name = name.trim(), age = age, sex = sex, heightCm = heightCm, weightKg = weightKg,
                fitnessLevel = fitness.name, goal = goal.name, createdDay = today,
                activeTitle = HunterTitle.AWAKENED.name,
            ),
        )
        db.weightDao().insert(WeightEntry(epochDay = today, weightKg = weightKg))
        settingsRepo.updateMeta { it.copy(lastEvaluatedDay = today, inactivityAnchorDay = today) }
        ensureDay(today)
    }

    suspend fun updateProfile(transform: (ProfileEntity) -> ProfileEntity) {
        val p = db.profileDao().get() ?: return
        db.profileDao().upsert(transform(p))
    }

    suspend fun addWeight(kg: Double) {
        val today = DateUtil.today()
        db.weightDao().insert(WeightEntry(epochDay = today, weightKg = kg))
        updateProfile { it.copy(weightKg = kg) }
    }

    suspend fun deleteWeight(id: Long) = db.weightDao().delete(id)

    suspend fun allocateStat(stat: Stat) = mutex.withLock {
        val p = db.profileDao().get() ?: return@withLock
        if (p.statPoints <= 0) return@withLock
        val updated = when (stat) {
            Stat.STR -> p.copy(str = p.str + 1)
            Stat.AGI -> p.copy(agi = p.agi + 1)
            Stat.VIT -> p.copy(vit = p.vit + 1)
            Stat.END -> p.copy(endurance = p.endurance + 1)
            Stat.INT -> p.copy(intel = p.intel + 1)
        }.copy(statPoints = p.statPoints - 1)
        val before = Skill.unlocked(p.level, p.stats())
        db.profileDao().upsert(updated)
        announceNewSkills(before, Skill.unlocked(updated.level, updated.stats()))
    }

    suspend fun setActiveTitle(title: HunterTitle) = updateProfile { it.copy(activeTitle = title.name) }

    // ---------------------------------------------------------------- Tagesquests

    /** Legt die Daily Quest für [day] an, falls sie noch nicht existiert. */
    suspend fun ensureDay(day: Long = DateUtil.today()) {
        _currentDay.value = DateUtil.today()
        val profile = db.profileDao().get() ?: return
        val existing = db.dailyDao().getDay(day)
        if (existing.isEmpty()) {
            val settings = settingsRepo.current()
            val fitness = profile.fitness()
            val goal = profile.goalEnum()
            val rows = Exercise.DAILY.map { ex ->
                if (ex == Exercise.CARDIO) {
                    val unit = settings.cardio
                    DailyExerciseEntity(
                        epochDay = day, exercise = ex.name,
                        target = QuestScaling.cardioTarget(profile.level, fitness, goal, unit),
                        unit = if (unit == CardioUnit.MINUTES) "min" else "km10",
                    )
                } else {
                    DailyExerciseEntity(
                        epochDay = day, exercise = ex.name,
                        target = QuestScaling.dailyTarget(profile.level, ex, fitness, goal), unit = "reps",
                    )
                }
            }
            db.dailyDao().upsertAll(rows)
        }
        if (db.dayRecordDao().get(day) == null) db.dayRecordDao().upsert(DayRecord(epochDay = day))
    }

    suspend fun addProgress(exercise: Exercise, delta: Int) = mutex.withLock {
        val day = DateUtil.today()
        ensureDay(day)
        val row = db.dailyDao().get(day, exercise.name) ?: return@withLock
        setProgressLocked(row, (row.done + delta).coerceAtLeast(0))
    }

    suspend fun setProgress(exercise: Exercise, value: Int) = mutex.withLock {
        val day = DateUtil.today()
        ensureDay(day)
        val row = db.dailyDao().get(day, exercise.name) ?: return@withLock
        setProgressLocked(row, value.coerceAtLeast(0))
    }

    private suspend fun setProgressLocked(row: DailyExerciseEntity, newDone: Int) {
        val day = row.epochDay
        val exercise = Exercise.valueOf(row.exercise)
        var updated = row.copy(done = newDone.coerceAtMost(row.target * 10 + 1000))
        if (newDone > row.done) markActive(day)
        val reachedNow = updated.done >= updated.target && !row.xpAwarded
        if (reachedNow) {
            updated = updated.copy(xpAwarded = true)
            db.dailyDao().upsert(updated)
            val profile = db.profileDao().get() ?: return
            var extra = 1.0
            if (exercise in setOf(Exercise.PUSHUPS, Exercise.SQUATS, Exercise.SITUPS) &&
                Skill.POWER_SURGE in profile.unlockedSkills()
            ) extra *= 1.10
            val xp = xpWithBonuses(XpRules.DAILY_EXERCISE_XP, profile.stats()[exercise.xpStat], extra, profile)
            awardXpLocked(xp, "Daily: ${exercise.label}")
            _events.tryEmit(GameEvent.QuestCleared(exercise.label, xp))
        } else {
            db.dailyDao().upsert(updated)
        }
        checkAllDailiesLocked(day)
        checkWeekliesLocked(day)
    }

    /** Ausdauer in Minuten zur Daily Quest addieren (Einheit wird umgerechnet). */
    suspend fun addCardioMinutes(minutes: Int, km: Double? = null) = mutex.withLock {
        val day = DateUtil.today()
        ensureDay(day)
        val row = db.dailyDao().get(day, Exercise.CARDIO.name) ?: return@withLock
        val profile = db.profileDao().get() ?: return@withLock
        val boost = if (Skill.ENDURANCE_BOOST in profile.unlockedSkills()) 1.25 else 1.0
        val delta = if (row.unit == "km10") {
            val tenthKm = km?.let { it * 10 } ?: (minutes / 6.0 * 10)
            Math.round(tenthKm * boost).toInt()
        } else {
            Math.round(minutes * boost).toInt()
        }
        setProgressLocked(row, row.done + delta)
    }

    private suspend fun checkAllDailiesLocked(day: Long) {
        val rows = db.dailyDao().getDay(day)
        val rec = db.dayRecordDao().get(day) ?: DayRecord(epochDay = day)
        val allDone = rows.isNotEmpty() && rows.all { it.done >= it.target }
        if (allDone && !rec.bonusAwarded) {
            db.dayRecordDao().upsert(rec.copy(allDone = true, bonusAwarded = true, active = true))
            val profile = db.profileDao().get() ?: return
            val bonus = xpWithBonuses(XpRules.allDailiesBonus(profile.level), 10, 1.0, profile)
            awardXpLocked(bonus, "Bonus: Alle Dailies erledigt")
            _events.tryEmit(
                GameEvent.SystemMessage(
                    "[QUEST ABGESCHLOSSEN]",
                    "Die Daily Quest ist erfüllt. Belohnung: +$bonus XP. Die Strafe wurde abgewendet.",
                ),
            )
        } else if (allDone != rec.allDone) {
            db.dayRecordDao().upsert((db.dayRecordDao().get(day) ?: rec).copy(allDone = allDone))
        }
    }

    private suspend fun markActive(day: Long) {
        val rec = db.dayRecordDao().get(day) ?: DayRecord(epochDay = day)
        if (!rec.active) db.dayRecordDao().upsert(rec.copy(active = true))
    }

    // ---------------------------------------------------------------- XP

    private suspend fun xpWithBonuses(base: Long, statValue: Int, extra: Double, profile: ProfileEntity): Long {
        val meta = settingsRepo.currentMeta()
        val doubleXp = meta.doubleXpDay == DateUtil.today()
        var e = extra
        if (Skill.MONARCH_AURA in profile.unlockedSkills()) e *= 1.15
        return XpRules.applyMultipliers(base, statValue, doubleXp, e)
    }

    suspend fun awardXp(amount: Long, reason: String): XpGainResult? = mutex.withLock { awardXpLocked(amount, reason) }

    private suspend fun awardXpLocked(amount: Long, reason: String): XpGainResult? {
        if (amount <= 0) return null
        val p = db.profileDao().get() ?: return null
        val result = Leveling.addXp(LevelState(p.level, p.xpInLevel), amount)
        val before = Skill.unlocked(p.level, p.stats())
        val updated = p.copy(
            level = result.after.level,
            xpInLevel = result.after.xpInLevel,
            totalXp = p.totalXp + amount,
            statPoints = p.statPoints + result.statPointsGained,
        )
        db.profileDao().upsert(updated)
        val day = DateUtil.today()
        db.xpLogDao().insert(XpLogEntity(timestamp = System.currentTimeMillis(), epochDay = day, amount = amount, reason = reason))
        val rec = db.dayRecordDao().get(day) ?: DayRecord(epochDay = day)
        db.dayRecordDao().upsert(rec.copy(xpEarned = rec.xpEarned + amount))
        _events.tryEmit(GameEvent.XpGained(amount, reason))
        if (result.levelsGained > 0) {
            val newRank = ch.awakened.app.domain.Rank.forLevel(result.after.level) != ch.awakened.app.domain.Rank.forLevel(p.level)
            _events.tryEmit(GameEvent.LevelUp(result.after.level, result.levelsGained, result.statPointsGained, newRank))
            announceNewSkills(before, Skill.unlocked(updated.level, updated.stats()))
        }
        return result
    }

    private fun announceNewSkills(before: Set<Skill>, after: Set<Skill>) {
        (after - before).forEach { s ->
            _events.tryEmit(GameEvent.SystemMessage("[NEUER SKILL]", "Skill freigeschaltet: „${s.displayName}“. ${s.description}"))
        }
    }

    // ---------------------------------------------------------------- Weekly Quests

    suspend fun weeklyQuests(day: Long = DateUtil.today()): List<WeeklyQuest> {
        val weekStart = DateUtil.weekStart(day)
        val rows = db.dailyDao().range(weekStart, weekStart + 6)
        val claimed = db.weeklyDao().week(weekStart).map { it.questKey }.toSet()
        val cardioDays = rows.filter { it.exercise == Exercise.CARDIO.name }.count { it.minutesEquivalent() >= 30 }
        val pushups = rows.filter { it.exercise == Exercise.PUSHUPS.name }.sumOf { it.done }
        val todayPushTarget = rows.firstOrNull { it.exercise == Exercise.PUSHUPS.name && it.epochDay == day }?.target
            ?: rows.filter { it.exercise == Exercise.PUSHUPS.name }.maxOfOrNull { it.target } ?: 20
        val streak = currentStreak(day)
        return listOf(
            WeeklyQuest("CARDIO_3X", "Ausdauer-Training", "3× Ausdauer von mindestens 30 Minuten", cardioDays, 3, "CARDIO_3X" in claimed),
            WeeklyQuest("STREAK_7", "Ununterbrochen", "7-Tage-Streak der Daily Quest", streak.coerceAtMost(7), 7, "STREAK_7" in claimed),
            WeeklyQuest(
                "PUSHUPS_TOTAL", "Eiserne Arme", "Liegestütze diese Woche insgesamt",
                pushups, QuestScaling.weeklyPushupTarget(todayPushTarget), "PUSHUPS_TOTAL" in claimed,
            ),
        )
    }

    private suspend fun checkWeekliesLocked(day: Long) {
        val weekStart = DateUtil.weekStart(day)
        val profile = db.profileDao().get() ?: return
        weeklyQuests(day).filter { it.done && !it.claimed }.forEach { q ->
            if (db.weeklyDao().insert(WeeklyClaimEntity(weekStart, q.key)) != -1L) {
                val xp = xpWithBonuses(XpRules.WEEKLY_QUEST_XP, 10, 1.0, profile)
                awardXpLocked(xp, "Weekly: ${q.title}")
                _events.tryEmit(GameEvent.QuestCleared("Weekly: ${q.title}", xp))
            }
        }
    }

    suspend fun currentStreak(today: Long = DateUtil.today()): Int =
        DateUtil.streak(db.dayRecordDao().completedDays().toSet(), today)

    suspend fun bestStreak(): Int = DateUtil.bestStreak(db.dayRecordDao().completedDays().toSet())

    // ---------------------------------------------------------------- Challenges

    suspend fun createChallenge(
        name: String, category: ChallengeCategory, frequency: ChallengeFrequency, target: Int, unit: String, xp: Long,
    ) {
        db.challengeDao().insert(
            ChallengeEntity(
                name = name.trim(), category = category.name, frequency = frequency.name,
                target = target.coerceAtLeast(1), unit = unit.trim().ifBlank { "×" },
                xp = xp.coerceIn(1, 5000), createdDay = DateUtil.today(),
            ),
        )
    }

    suspend fun deleteChallenge(id: Long) {
        db.challengeDao().delete(id)
        db.challengeDao().deleteProgress(id)
    }

    fun periodKey(c: ChallengeEntity, day: Long): Long = when (ChallengeFrequency.valueOf(c.frequency)) {
        ChallengeFrequency.DAILY -> day
        ChallengeFrequency.WEEKLY -> DateUtil.weekStart(day)
        ChallengeFrequency.ONCE -> 0
    }

    suspend fun addChallengeProgress(id: Long, delta: Int) = mutex.withLock {
        val c = db.challengeDao().get(id) ?: return@withLock
        val day = DateUtil.today()
        val period = periodKey(c, day)
        val cur = db.challengeDao().progress(id, period) ?: ChallengeProgressEntity(id, period)
        val newDone = (cur.done + delta).coerceAtLeast(0)
        if (delta > 0) markActive(day)
        if (newDone >= c.target && !cur.completed) {
            db.challengeDao().upsertProgress(cur.copy(done = newDone, completed = true))
            val profile = db.profileDao().get() ?: return@withLock
            val isOther = c.category == ChallengeCategory.OTHER.name
            val stat = if (isOther) profile.intel else profile.str
            var extra = 1.0
            if (isOther && Skill.SHARP_MIND in profile.unlockedSkills()) extra *= 1.2
            val xp = xpWithBonuses(c.xp, stat, extra, profile)
            awardXpLocked(xp, "Challenge: ${c.name}")
            _events.tryEmit(GameEvent.QuestCleared(c.name, xp))
            if (c.frequency == ChallengeFrequency.ONCE.name) db.challengeDao().upsert(c.copy(archived = true))
        } else {
            db.challengeDao().upsertProgress(cur.copy(done = newDone))
        }
    }

    // ---------------------------------------------------------------- Training

    data class TrainingResult(val xp: Long, val kcal: Int, val levelResult: XpGainResult?)

    /**
     * Schliesst ein Training ab: Wiederholungen fliessen in die Daily Quest,
     * Kalorien werden per MET geschätzt und XP gutgeschrieben.
     */
    suspend fun finishTraining(
        title: String, startedAt: Long, durationSec: Long, reps: Map<Exercise, Int>, cardioMinutes: Int, kcal: Int,
    ): TrainingResult {
        reps.filterKeys { it in Exercise.DAILY && it != Exercise.CARDIO }.forEach { (ex, n) -> if (n > 0) addProgress(ex, n) }
        if (cardioMinutes > 0) addCardioMinutes(cardioMinutes)
        return mutex.withLock {
            val profile = db.profileDao().get() ?: return@withLock TrainingResult(0, kcal, null)
            val minutes = (durationSec / 60).toInt()
            var extra = 1.0
            if (Skill.SHADOW_STEP in profile.unlockedSkills()) extra *= 1.10
            val xp = xpWithBonuses(XpRules.trainingXp(minutes, kcal), profile.agi, extra, profile)
            val day = DateUtil.today()
            db.workoutDao().insert(
                WorkoutEntity(
                    epochDay = day, startedAt = startedAt, durationSec = durationSec, title = title, kcal = kcal, xp = xp,
                    cardioMinutes = cardioMinutes,
                    details = reps.entries.filter { it.value > 0 }.joinToString(", ") { "${it.value} ${it.key.label}" },
                ),
            )
            val rec = db.dayRecordDao().get(day) ?: DayRecord(epochDay = day)
            db.dayRecordDao().upsert(rec.copy(kcal = rec.kcal + kcal, active = true))
            val res = awardXpLocked(xp, "Training: $title")
            TrainingResult(xp, kcal, res)
        }
    }

    // ---------------------------------------------------------------- Health Connect Import

    /** Schritte von heute übernehmen; vergibt nur die Differenz an Schritt-XP. */
    suspend fun applySteps(steps: Long) = mutex.withLock {
        val day = DateUtil.today()
        ensureDay(day)
        val rec = db.dayRecordDao().get(day) ?: DayRecord(epochDay = day)
        val stepXp = XpRules.stepXp(steps)
        val diff = stepXp - rec.stepXp
        db.dayRecordDao().upsert(rec.copy(steps = steps, stepXp = maxOf(stepXp, rec.stepXp), active = rec.active || steps >= 1000))
        if (diff > 0) awardXpLocked(diff, "Schritte (Health Connect)")
    }

    /** Importiert eine Trainingseinheit aus Health Connect genau einmal. */
    suspend fun importExternalWorkout(
        externalId: String, title: String, startedAt: Long, durationSec: Long, kcal: Int, cardio: Boolean, km: Double?,
    ): Boolean {
        if (db.workoutDao().countExternal(externalId) > 0) return false
        val minutes = (durationSec / 60).toInt()
        if (cardio && DateUtil.epochDayOfMillis(startedAt) == DateUtil.today()) addCardioMinutes(minutes, km)
        return mutex.withLock {
            val profile = db.profileDao().get() ?: return@withLock false
            val xp = xpWithBonuses(XpRules.trainingXp(minutes, kcal), profile.endurance, 1.0, profile)
            val day = DateUtil.epochDayOfMillis(startedAt)
            val id = db.workoutDao().insert(
                WorkoutEntity(
                    epochDay = day, startedAt = startedAt, durationSec = durationSec, title = title, kcal = kcal, xp = xp,
                    source = "HEALTH", externalId = externalId, cardioMinutes = if (cardio) minutes else 0,
                    details = km?.let { "%.1f km".format(it) } ?: "",
                ),
            )
            if (id == -1L) return@withLock false
            markActive(day)
            awardXpLocked(xp, "Health Connect: $title")
            true
        }
    }

    // ---------------------------------------------------------------- Skills

    suspend fun activateDoubleXp(): Boolean {
        val p = db.profileDao().get() ?: return false
        if (Skill.DOUBLE_XP_DAY !in p.unlockedSkills()) return false
        val today = DateUtil.today()
        val meta = settingsRepo.currentMeta()
        if (meta.doubleXpWeekUsed == DateUtil.weekStart(today)) return false
        settingsRepo.updateMeta { it.copy(doubleXpWeekUsed = DateUtil.weekStart(today), doubleXpDay = today) }
        _events.tryEmit(GameEvent.SystemMessage("[SKILL AKTIVIERT]", "Doppel-XP-Tag: Heute erhältst du doppelte XP."))
        return true
    }

    // ---------------------------------------------------------------- Penalty

    /**
     * Prüft Deadline, verpasste Tage, Inaktivität und automatisches Entsperren.
     * Wird vom Worker, vom AccessibilityService und beim Öffnen der App aufgerufen.
     */
    suspend fun evaluatePenalties(nowMillis: Long = System.currentTimeMillis()) = mutex.withLock {
        val profile = db.profileDao().get() ?: return@withLock
        val settings = settingsRepo.current()
        val today = DateUtil.today()
        ensureDay(today)
        val config = settings.penaltyConfig
        val minute = DateUtil.minuteOfDay()

        // Automatisches Entsperren nach Ablauf der Sperrzeit
        val penalty = settingsRepo.currentPenalty()
        if (penalty.active && PenaltyRules.canUnlock(nowMillis, penalty.unlockAt, questDone = false)) {
            clearPenaltyLocked(byQuest = false)
        }
        if (!settings.penaltyEnabled) {
            settingsRepo.updateMeta { it.copy(lastEvaluatedDay = maxOf(it.lastEvaluatedDay, today - 1)) }
            return@withLock
        }

        val meta = settingsRepo.currentMeta()
        val days = PenaltyRules.daysToEvaluate(
            lastEvaluatedDay = meta.lastEvaluatedDay.takeIf { it >= 0 },
            today = today,
            pastDeadlineToday = PenaltyRules.isPastDeadline(minute, config),
            firstDay = profile.createdDay + 1, // Tag der Profilerstellung ist Schonfrist
        )
        var failed = false
        for (d in days) {
            val rec = db.dayRecordDao().get(d) ?: DayRecord(epochDay = d)
            val curMeta = settingsRepo.currentMeta()
            val jokerAvailable = Skill.SECOND_WIND in profile.unlockedSkills() &&
                curMeta.jokerWeekUsed != DateUtil.weekStart(d)
            val outcome = PenaltyRules.decideDay(rec.allDone, jokerAvailable)
            db.dayRecordDao().upsert(rec.copy(outcome = outcome.name))
            when (outcome) {
                DayOutcome.JOKER_USED -> {
                    settingsRepo.updateMeta { it.copy(jokerWeekUsed = DateUtil.weekStart(d)) }
                    hooks?.onJokerUsed(d)
                }
                DayOutcome.FAILED -> failed = true
                DayOutcome.COMPLETED -> Unit
            }
            settingsRepo.updateMeta { it.copy(lastEvaluatedDay = d) }
        }
        if (failed && !settingsRepo.currentPenalty().active) startPenaltyLocked(PenaltyReason.DAILY_MISSED, profile, nowMillis)

        // Inaktivität
        if (!settingsRepo.currentPenalty().active) {
            val lastActive = db.dayRecordDao().lastActiveDay()
            val anchor = maxOf(profile.createdDay, settingsRepo.currentMeta().inactivityAnchorDay)
            if (PenaltyRules.inactivityTriggered(today, lastActive, anchor, config)) {
                startPenaltyLocked(PenaltyReason.INACTIVITY, profile, nowMillis)
            }
        }

        // Erinnerungen
        val todayRec = db.dayRecordDao().get(today)
        if (settings.remindersEnabled && todayRec?.allDone != true && !PenaltyRules.isPastDeadline(minute, config)) {
            val left = PenaltyRules.minutesUntilDeadline(minute, config)
            val m = settingsRepo.currentMeta()
            val sent = if (m.reminderDay == today) m.remindersSent.mapNotNull { it.toIntOrNull() }.toSet() else emptySet()
            PenaltyRules.reminderDue(left, sent)?.let { threshold ->
                settingsRepo.updateMeta {
                    it.copy(reminderDay = today, remindersSent = (sent + PenaltyRules.REMINDER_THRESHOLDS.filter { t -> t >= threshold }).map { t -> t.toString() }.toSet())
                }
                hooks?.onReminder(left)
            }
        }
    }

    private suspend fun startPenaltyLocked(reason: PenaltyReason, profile: ProfileEntity, nowMillis: Long) {
        val settings = settingsRepo.current()
        val (burpees, cardio) = QuestScaling.penaltyQuest(profile.fitness())
        val state = PenaltyState(
            active = true, reason = reason.name, startedAt = nowMillis,
            unlockAt = PenaltyRules.unlockAt(
                nowMillis, settings.penaltyConfig,
                halved = Skill.IRON_WILL in profile.unlockedSkills(), vitality = profile.vit,
            ),
            burpeesTarget = burpees, cardioTarget = cardio,
        )
        settingsRepo.setPenalty(state)
        hooks?.onPenaltyStarted(state)
    }

    /** Nur zu Testzwecken bzw. manuell aus den Einstellungen. */
    suspend fun triggerPenaltyManually() = mutex.withLock {
        val p = db.profileDao().get() ?: return@withLock
        if (!settingsRepo.currentPenalty().active) startPenaltyLocked(PenaltyReason.DAILY_MISSED, p, System.currentTimeMillis())
    }

    suspend fun addPenaltyProgress(burpees: Int = 0, cardioMinutes: Int = 0) = mutex.withLock {
        val s = settingsRepo.currentPenalty()
        if (!s.active) return@withLock
        val updated = s.copy(
            burpeesDone = (s.burpeesDone + burpees).coerceAtLeast(0),
            cardioDone = (s.cardioDone + cardioMinutes).coerceAtLeast(0),
        )
        settingsRepo.setPenalty(updated)
        if (burpees > 0 || cardioMinutes > 0) markActive(DateUtil.today())
        if (PenaltyRules.penaltyQuestDone(updated.burpeesDone, updated.burpeesTarget, updated.cardioDone, updated.cardioTarget)) {
            clearPenaltyLocked(byQuest = true)
        }
    }

    private suspend fun clearPenaltyLocked(byQuest: Boolean) {
        settingsRepo.setPenalty(PenaltyState(active = false))
        val today = DateUtil.today()
        settingsRepo.updateMeta {
            it.copy(
                inactivityAnchorDay = today,
                penaltiesCleared = it.penaltiesCleared + if (byQuest) 1 else 0,
            )
        }
        if (byQuest) {
            awardXpLocked(XpRules.PENALTY_QUEST_XP, "Straf-Quest überlebt")
            _events.tryEmit(GameEvent.SystemMessage("[PENALTY ZONE VERLASSEN]", "Straf-Quest abgeschlossen. Die Sperre ist aufgehoben."))
        }
        hooks?.onPenaltyCleared(byQuest)
    }

    fun isExempt(pkg: String, dynamic: Set<String>) =
        ch.awakened.app.domain.EmergencyExemptions.isExempt(pkg, ownPackage, dynamic)

    // ---------------------------------------------------------------- Erfolge

    suspend fun achievements(): Achievements {
        val p = db.profileDao().get()
        val cardioMinutes = db.dailyDao().all().filter { it.exercise == Exercise.CARDIO.name }.sumOf { it.minutesEquivalent().toLong() }
        return Achievements(
            level = p?.level ?: 1,
            bestStreak = bestStreak(),
            totalPushups = db.dailyDao().total(Exercise.PUSHUPS.name),
            totalCardioMinutes = cardioMinutes,
            totalWorkouts = db.workoutDao().appWorkoutCount(),
            penaltiesCleared = settingsRepo.currentMeta().penaltiesCleared,
            challengesCompleted = db.challengeDao().completedCount(),
        )
    }

    // ---------------------------------------------------------------- Reset

    suspend fun wipeAll() {
        db.withTransaction {
            db.profileDao().clear(); db.weightDao().clear(); db.dailyDao().clear(); db.dayRecordDao().clear()
            db.challengeDao().clear(); db.challengeDao().clearProgress(); db.workoutDao().clear()
            db.weeklyDao().clear(); db.xpLogDao().clear()
        }
    }
}

// ---------------------------------------------------------------- Erweiterungen

fun ProfileEntity.stats() = StatBlock(str, agi, vit, endurance, intel)
fun ProfileEntity.fitness() = runCatching { FitnessLevel.valueOf(fitnessLevel) }.getOrDefault(FitnessLevel.BEGINNER)
fun ProfileEntity.goalEnum() = runCatching { Goal.valueOf(goal) }.getOrDefault(Goal.STRENGTH)
fun ProfileEntity.unlockedSkills() = Skill.unlocked(level, stats())

/** Ausdauer-Fortschritt in Minuten (km werden mit 6 min/km umgerechnet). */
fun DailyExerciseEntity.minutesEquivalent(): Int = if (unit == "km10") (done * 0.6).toInt() else done
