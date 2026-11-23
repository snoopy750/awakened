package ch.awakened.app.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.awakened.app.container
import ch.awakened.app.data.db.ChallengeEntity
import ch.awakened.app.data.db.ChallengeProgressEntity
import ch.awakened.app.data.db.DailyExerciseEntity
import ch.awakened.app.data.db.DayRecord
import ch.awakened.app.data.db.ProfileEntity
import ch.awakened.app.data.prefs.AppSettings
import ch.awakened.app.data.prefs.GameMeta
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.data.repo.WeeklyQuest
import ch.awakened.app.domain.Achievements
import ch.awakened.app.domain.ChallengeCategory
import ch.awakened.app.domain.ChallengeFrequency
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.FitnessLevel
import ch.awakened.app.domain.Goal
import ch.awakened.app.domain.HunterTitle
import ch.awakened.app.domain.Stat
import ch.awakened.app.work.WorkScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    val repo = c.repository
    val feedback = c.feedback
    val health = c.health

    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready.asStateFlow()

    val settings: StateFlow<AppSettings?> = c.settings.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val profile: StateFlow<ProfileEntity?> = repo.profile.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val penalty: StateFlow<PenaltyState> = c.settings.penalty.stateIn(viewModelScope, SharingStarted.Eagerly, PenaltyState())
    val meta: StateFlow<GameMeta> = c.settings.meta.stateIn(viewModelScope, SharingStarted.Eagerly, GameMeta())
    val today: StateFlow<List<DailyExerciseEntity>> = repo.todayExercises.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val todayRecord: StateFlow<DayRecord?> = repo.todayRecord.stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val challenges: StateFlow<List<ChallengeEntity>> = repo.challenges.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val challengeProgress: StateFlow<List<ChallengeProgressEntity>> =
        repo.challengeProgress.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _weekly = MutableStateFlow<List<WeeklyQuest>>(emptyList())
    val weekly: StateFlow<List<WeeklyQuest>> = _weekly.asStateFlow()
    private val _streak = MutableStateFlow(0)
    val streak: StateFlow<Int> = _streak.asStateFlow()
    private val _achievements = MutableStateFlow<Achievements?>(null)
    val achievements: StateFlow<Achievements?> = _achievements.asStateFlow()

    /** Kurzmeldungen (Snackbar-artig) für Aktionen wie Backup oder Health-Sync. */
    private val _toast = MutableStateFlow<String?>(null)
    val toast: StateFlow<String?> = _toast.asStateFlow()
    fun toast(msg: String) { _toast.value = msg }
    fun consumeToast() { _toast.value = null }

    init {
        viewModelScope.launch {
            c.settings.settings.first()
            repo.profile.first()
            _ready.value = true
        }
        combine(repo.todayExercises, repo.weeklyClaims, repo.todayRecord) { _, _, _ -> }
            .onEach { refreshDerived() }
            .launchIn(viewModelScope)
    }

    private suspend fun refreshDerived() {
        if (repo.profile.first() == null) return
        _weekly.value = repo.weeklyQuests()
        _streak.value = repo.currentStreak()
        _achievements.value = repo.achievements()
    }

    fun onResume() {
        viewModelScope.launch {
            if (repo.profile.first() == null) return@launch
            repo.ensureDay()
            repo.evaluatePenalties()
            refreshDerived()
            if (settings.filterNotNull().first().healthConnectEnabled) {
                runCatching { health.syncToday(repo) }
            }
        }
    }

    // --------------------------------------------------------------- Onboarding / Profil

    fun createProfile(name: String, age: Int, sex: String, height: Double, weight: Double, fitness: FitnessLevel, goal: Goal, onDone: () -> Unit) {
        viewModelScope.launch {
            repo.createProfile(name, age, sex, height, weight, fitness, goal)
            c.settings.update { it.copy(onboardingDone = true) }
            WorkScheduler.scheduleAll(getApplication())
            onDone()
        }
    }

    fun finishPermissionsIntro() = viewModelScope.launch { c.settings.update { it.copy(permissionsIntroDone = true) } }

    fun updateProfile(name: String, age: Int, height: Double, fitness: FitnessLevel, goal: Goal) = viewModelScope.launch {
        repo.updateProfile { it.copy(name = name, age = age, heightCm = height, fitnessLevel = fitness.name, goal = goal.name) }
    }

    fun allocate(stat: Stat) = viewModelScope.launch { repo.allocateStat(stat) }
    fun setTitle(t: HunterTitle) = viewModelScope.launch { repo.setActiveTitle(t) }

    // --------------------------------------------------------------- Quests

    fun addProgress(ex: Exercise, delta: Int) = viewModelScope.launch { repo.addProgress(ex, delta) }
    fun setProgress(ex: Exercise, value: Int) = viewModelScope.launch { repo.setProgress(ex, value) }

    fun createChallenge(name: String, cat: ChallengeCategory, freq: ChallengeFrequency, target: Int, unit: String, xp: Long) =
        viewModelScope.launch { repo.createChallenge(name, cat, freq, target, unit, xp) }

    fun deleteChallenge(id: Long) = viewModelScope.launch { repo.deleteChallenge(id) }
    fun challengeProgress(id: Long, delta: Int) = viewModelScope.launch { repo.addChallengeProgress(id, delta) }
    fun periodKey(c: ChallengeEntity) = repo.periodKey(c, DateUtil.today())

    // --------------------------------------------------------------- Penalty

    fun penaltyProgress(burpees: Int = 0, cardio: Int = 0) = viewModelScope.launch { repo.addPenaltyProgress(burpees, cardio) }

    // --------------------------------------------------------------- Skills

    fun activateDoubleXp() = viewModelScope.launch {
        if (!repo.activateDoubleXp()) toast("Doppel-XP ist diese Woche bereits verbraucht.")
    }

    // --------------------------------------------------------------- Einstellungen

    fun updateSettings(transform: (AppSettings) -> AppSettings) = viewModelScope.launch {
        val before = c.settings.current()
        c.settings.update(transform)
        if (before.deadlineMinute != c.settings.current().deadlineMinute) WorkScheduler.scheduleDeadline(getApplication())
    }

    fun addWeight(kg: Double) = viewModelScope.launch { repo.addWeight(kg) }
    fun deleteWeight(id: Long) = viewModelScope.launch { repo.deleteWeight(id) }

    fun syncHealth() = viewModelScope.launch {
        val r = runCatching { health.syncToday(repo) }
        r.onSuccess { res ->
            toast(
                if (res == null) "Health Connect: keine Berechtigung erteilt."
                else "Health Connect: ${res.steps} Schritte, ${res.importedSessions} neue Trainings übernommen.",
            )
        }.onFailure { toast("Health Connect Fehler: ${it.message}") }
    }

    fun exportBackup(uri: Uri) = viewModelScope.launch {
        runCatching { c.backup.export(uri) }
            .onSuccess { toast("Backup gespeichert.") }
            .onFailure { toast("Backup fehlgeschlagen: ${it.message}") }
    }

    fun importBackup(uri: Uri) = viewModelScope.launch {
        runCatching { c.backup.import(uri) }
            .onSuccess {
                repo.ensureDay()
                refreshDerived()
                toast("Backup wiederhergestellt.")
            }
            .onFailure { toast("Wiederherstellung fehlgeschlagen: ${it.message}") }
    }
}
