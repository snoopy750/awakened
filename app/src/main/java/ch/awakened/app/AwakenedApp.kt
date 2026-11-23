package ch.awakened.app

import android.app.Application
import android.content.Context
import ch.awakened.app.data.prefs.PenaltyState
import ch.awakened.app.data.repo.PenaltyHooks
import ch.awakened.app.domain.DateUtil
import ch.awakened.app.penalty.PenaltyMonitorService
import ch.awakened.app.system.Notifier
import ch.awakened.app.work.WorkScheduler

class AwakenedApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        Notifier.createChannels(this)
        container.repository.hooks = object : PenaltyHooks {
            override fun onPenaltyStarted(state: PenaltyState) {
                Notifier.penaltyStarted(this@AwakenedApp, state)
                PenaltyMonitorService.startIfPossible(this@AwakenedApp)
            }

            override fun onPenaltyCleared(byQuest: Boolean) {
                Notifier.penaltyCleared(this@AwakenedApp, byQuest)
            }

            override fun onJokerUsed(day: Long) = Notifier.jokerUsed(this@AwakenedApp, DateUtil.format(day))

            override fun onReminder(minutesLeft: Int) = Notifier.reminder(this@AwakenedApp, minutesLeft)
        }
        WorkScheduler.scheduleAll(this)
    }
}

val Context.container: AppContainer get() = (applicationContext as AwakenedApp).container
