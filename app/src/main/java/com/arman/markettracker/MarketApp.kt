package com.arman.markettracker

import android.app.Application
import com.arman.markettracker.alerts.NotificationHelper
import com.arman.markettracker.alerts.scheduleRefreshWork
import com.arman.markettracker.data.prefs.SettingsRepository
import com.arman.markettracker.data.repository.MarketRepository
import com.arman.markettracker.data.source.ApiMarketDataSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Manual DI container — deliberately dependency-light (no Hilt) so the app
 * stays small and fast. Prices come from [ApiMarketDataSource] (live).
 */
class AppContainer(val app: Application) {
    val settings = SettingsRepository(app)
    val repository = MarketRepository(app, ApiMarketDataSource())
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}

class MarketApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        // Schedule background refresh with the user's saved cadence.
        container.scope.launch {
            val minutes = container.settings.refreshMinutes.first()
            scheduleRefreshWork(this@MarketApp, minutes)
        }
    }
}
