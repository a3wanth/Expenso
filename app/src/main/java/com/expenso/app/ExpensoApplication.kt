package com.expenso.app

import android.app.Application
import android.content.Context
import com.expenso.app.data.FinanceRepository
import com.expenso.app.data.SettingsRepository
import com.expenso.app.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    val database: AppDatabase = AppDatabase.getInstance(appContext)
    val repository = FinanceRepository(database)
    val settings = SettingsRepository(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun bootstrap() {
        scope.launch {
            val prefs = settings.preferences.first()
            if (!prefs.categoriesSeeded) {
                repository.seedCategoriesIfEmpty()
                settings.markCategoriesSeeded()
            }
        }
    }
}

class ExpensoApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        container.bootstrap()
    }
}
