package ch.awakened.app

import android.content.Context
import ch.awakened.app.data.db.AppDatabase
import ch.awakened.app.data.prefs.SettingsRepository
import ch.awakened.app.data.repo.GameRepository
import ch.awakened.app.integrations.BackupManager
import ch.awakened.app.integrations.HealthConnectManager
import ch.awakened.app.system.Feedback

/** Einfache manuelle Dependency Injection. */
class AppContainer(context: Context) {
    val appContext: Context = context.applicationContext
    val database = AppDatabase.build(appContext)
    val settings = SettingsRepository(appContext)
    val repository = GameRepository(database, settings, appContext.packageName)
    val health = HealthConnectManager(appContext)
    val backup = BackupManager(appContext, database, settings)
    val feedback = Feedback(appContext)
}
