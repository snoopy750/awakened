package ch.awakened.app.ui

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ch.awakened.app.container
import ch.awakened.app.data.repo.unlockedSkills
import ch.awakened.app.domain.CardioType
import ch.awakened.app.domain.Exercise
import ch.awakened.app.domain.FitnessCalc
import ch.awakened.app.domain.Skill
import ch.awakened.app.system.Feedback
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.roundToInt

enum class TrainingPhase { SETUP, RUNNING, DONE }

data class PlanItem(val exercise: Exercise, val reps: Int)

sealed interface TrainingStep {
    data class Work(val exercise: Exercise, val reps: Int, val round: Int) : TrainingStep
    data class Rest(val seconds: Int, val next: String) : TrainingStep
    data class Cardio(val minutes: Int, val type: CardioType) : TrainingStep
}

data class TrainingSummary(
    val durationSec: Long,
    val reps: Map<Exercise, Int>,
    val cardioMinutes: Int,
    val kcal: Int,
    val xp: Long,
    val levelsGained: Int,
)

data class TrainingUi(
    val phase: TrainingPhase = TrainingPhase.SETUP,
    val plan: List<PlanItem> = emptyList(),
    val rounds: Int = 3,
    val restSeconds: Int = 60,
    val cardioMinutes: Int = 0,
    val cardioType: CardioType = CardioType.RUNNING,
    val startSpotify: Boolean = true,
    val steps: List<TrainingStep> = emptyList(),
    val index: Int = 0,
    val elapsedMs: Long = 0,
    val stepElapsedMs: Long = 0,
    val restRemainingMs: Long = 0,
    val paused: Boolean = false,
    val completedReps: Map<Exercise, Int> = emptyMap(),
    val cardioDoneMinutes: Int = 0,
    val summary: TrainingSummary? = null,
    val restReduction: Boolean = false,
) {
    val current: TrainingStep? get() = steps.getOrNull(index)
    val workStepsTotal get() = steps.count { it !is TrainingStep.Rest }
    val workStepsDone get() = steps.take(index).count { it !is TrainingStep.Rest }
}

class TrainingViewModel(app: Application) : AndroidViewModel(app) {
    private val c = app.container
    private val _ui = MutableStateFlow(TrainingUi())
    val ui: StateFlow<TrainingUi> = _ui.asStateFlow()

    private var ticker: Job? = null
    private var startedAtWall = 0L
    private val activeMs = mutableMapOf<Exercise, Long>()
    private var restMs = 0L
    private var cardioMs = 0L

    init { resetPlan() }

    /** Startplan aus den noch offenen Daily-Zielen, verteilt auf die Runden. */
    fun resetPlan() = viewModelScope.launch {
        val rounds = 3
        val rows = c.repository.todayExercises.first()
        val profile = c.repository.profile.first()
        val shadow = profile?.let { Skill.SHADOW_STEP in it.unlockedSkills() } ?: false
        val plan = listOf(Exercise.PUSHUPS, Exercise.SITUPS, Exercise.SQUATS, Exercise.JUMP_ROPE).map { ex ->
            val row = rows.firstOrNull { it.exercise == ex.name }
            val remaining = row?.let { (it.target - it.done).coerceAtLeast(0) } ?: 0
            val base = if (remaining > 0) remaining else (row?.target ?: 30)
            PlanItem(ex, ceil(base / rounds.toDouble()).toInt().coerceAtLeast(1))
        }
        val cardioRow = rows.firstOrNull { it.exercise == Exercise.CARDIO.name }
        val cardioRemaining = cardioRow?.let {
            val left = (it.target - it.done).coerceAtLeast(0)
            if (it.unit == "km10") (left * 0.6).roundToInt() else left
        } ?: 0
        val spotify = c.settings.current().let { it.spotifyAutoStart && it.spotifyPlaylist.isNotBlank() }
        _ui.value = TrainingUi(
            plan = plan, rounds = rounds, restSeconds = if (shadow) 48 else 60, restReduction = shadow,
            cardioMinutes = cardioRemaining, startSpotify = spotify,
        )
    }

    fun setReps(ex: Exercise, reps: Int) = _ui.update { s ->
        s.copy(plan = s.plan.map { if (it.exercise == ex) it.copy(reps = reps.coerceIn(0, 500)) else it })
    }

    fun setRounds(n: Int) = _ui.update { it.copy(rounds = n.coerceIn(1, 10)) }
    fun setRest(sec: Int) = _ui.update { it.copy(restSeconds = sec.coerceIn(10, 300)) }
    fun setCardio(min: Int) = _ui.update { it.copy(cardioMinutes = min.coerceIn(0, 180)) }
    fun setCardioType(t: CardioType) = _ui.update { it.copy(cardioType = t) }
    fun setSpotify(b: Boolean) = _ui.update { it.copy(startSpotify = b) }

    fun start() {
        val s = _ui.value
        val steps = mutableListOf<TrainingStep>()
        val items = s.plan.filter { it.reps > 0 }
        for (round in 1..s.rounds) {
            items.forEachIndexed { i, item ->
                steps += TrainingStep.Work(item.exercise, item.reps, round)
                val isLast = round == s.rounds && i == items.lastIndex
                if (!isLast) {
                    val next = items.getOrNull(i + 1)?.exercise?.label ?: items.first().exercise.label
                    steps += TrainingStep.Rest(s.restSeconds, next)
                }
            }
        }
        if (s.cardioMinutes > 0) {
            if (steps.isNotEmpty()) steps += TrainingStep.Rest(s.restSeconds, s.cardioType.label)
            steps += TrainingStep.Cardio(s.cardioMinutes, s.cardioType)
        }
        if (steps.isEmpty()) return
        activeMs.clear(); restMs = 0; cardioMs = 0
        startedAtWall = System.currentTimeMillis()
        _ui.value = s.copy(
            phase = TrainingPhase.RUNNING, steps = steps, index = 0, elapsedMs = 0, stepElapsedMs = 0,
            restRemainingMs = (steps.first() as? TrainingStep.Rest)?.seconds?.times(1000L) ?: 0, paused = false,
            completedReps = emptyMap(), cardioDoneMinutes = 0, summary = null,
        )
        startTicker()
    }

    private fun startTicker() {
        ticker?.cancel()
        ticker = viewModelScope.launch {
            var last = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(200)
                val now = SystemClock.elapsedRealtime()
                val dt = now - last
                last = now
                val s = _ui.value
                if (s.phase != TrainingPhase.RUNNING || s.paused) continue
                when (val step = s.current) {
                    is TrainingStep.Work -> activeMs[step.exercise] = (activeMs[step.exercise] ?: 0) + dt
                    is TrainingStep.Rest -> restMs += dt
                    is TrainingStep.Cardio -> cardioMs += dt
                    null -> Unit
                }
                val restLeft = if (s.current is TrainingStep.Rest) s.restRemainingMs - dt else s.restRemainingMs
                _ui.value = s.copy(elapsedMs = s.elapsedMs + dt, stepElapsedMs = s.stepElapsedMs + dt, restRemainingMs = restLeft)
                if (s.current is TrainingStep.Rest && restLeft <= 0) {
                    feedback(Feedback.Kind.QUEST)
                    next()
                }
            }
        }
    }

    private suspend fun feedback(kind: Feedback.Kind) {
        val st = c.settings.current()
        c.feedback.play(kind, st.soundEnabled, st.vibrationEnabled)
    }

    fun completeStep() {
        val s = _ui.value
        when (val step = s.current) {
            is TrainingStep.Work -> _ui.value = s.copy(
                completedReps = s.completedReps + (step.exercise to ((s.completedReps[step.exercise] ?: 0) + step.reps)),
            )
            is TrainingStep.Cardio -> _ui.value = s.copy(
                cardioDoneMinutes = s.cardioDoneMinutes + (s.stepElapsedMs / 60_000L).toInt().coerceAtLeast(1),
            )
            else -> Unit
        }
        next()
    }

    /** Ausdauer-Block als erreicht markieren (z. B. draussen ohne App gelaufen). */
    fun completeCardioTarget() {
        val s = _ui.value
        val step = s.current as? TrainingStep.Cardio ?: return
        val missingMs = step.minutes * 60_000L - s.stepElapsedMs
        if (missingMs > 0) cardioMs += missingMs
        _ui.value = s.copy(cardioDoneMinutes = s.cardioDoneMinutes + step.minutes)
        next()
    }

    fun skipStep() = next()

    fun addRest(seconds: Int) = _ui.update { it.copy(restRemainingMs = it.restRemainingMs + seconds * 1000L) }

    fun togglePause() = _ui.update { it.copy(paused = !it.paused) }

    private fun next() {
        val s = _ui.value
        val nextIndex = s.index + 1
        if (nextIndex >= s.steps.size) {
            finish()
            return
        }
        val nextStep = s.steps[nextIndex]
        _ui.value = s.copy(
            index = nextIndex, stepElapsedMs = 0,
            restRemainingMs = (nextStep as? TrainingStep.Rest)?.seconds?.times(1000L) ?: 0,
        )
    }

    fun finish() {
        ticker?.cancel()
        val s = _ui.value
        if (s.phase != TrainingPhase.RUNNING) return
        viewModelScope.launch {
            val weight = c.repository.profile.first()?.weightKg ?: 70.0
            var kcal = 0.0
            activeMs.forEach { (ex, ms) -> kcal += FitnessCalc.caloriesMet(ex.met, weight, ms / 60_000.0) }
            kcal += FitnessCalc.caloriesMet(1.8, weight, restMs / 60_000.0)
            kcal += FitnessCalc.caloriesMet(s.cardioType.met, weight, cardioMs / 60_000.0)
            val durationSec = s.elapsedMs / 1000
            val title = when {
                s.completedReps.isEmpty() && s.cardioDoneMinutes > 0 -> s.cardioType.label
                s.cardioDoneMinutes > 0 -> "Kraft + ${s.cardioType.label}"
                else -> "Krafttraining"
            }
            val result = c.repository.finishTraining(
                title = title, startedAt = startedAtWall, durationSec = durationSec,
                reps = s.completedReps, cardioMinutes = s.cardioDoneMinutes, kcal = kcal.roundToInt(),
            )
            _ui.value = s.copy(
                phase = TrainingPhase.DONE,
                summary = TrainingSummary(
                    durationSec, s.completedReps, s.cardioDoneMinutes, result.kcal, result.xp,
                    result.levelResult?.levelsGained ?: 0,
                ),
            )
        }
    }

    fun abort() {
        ticker?.cancel()
        resetPlan()
    }

    override fun onCleared() {
        ticker?.cancel()
        super.onCleared()
    }
}
