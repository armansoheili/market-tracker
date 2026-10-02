package com.arman.markettracker.alerts

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.arman.markettracker.MarketApp
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.model.Direction
import com.arman.markettracker.data.repository.MarketUiState
import com.arman.markettracker.widget.WidgetUpdater
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

private const val WORK_NAME = "market_refresh"

/**
 * Background refresh: pulls fresh quotes, evaluates price alerts,
 * and pushes the widget — all in one efficient periodic pass.
 *
 * Alerts are opt-in only (see Settings). Each rule fires once, then
 * re-arms automatically when the price moves back across the target.
 */
class AlertWorker(appContext: Context, params: WorkerParameters) :
    CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as MarketApp).container
        if (!container.settings.alertsEnabled.first()) {
            // Still keep quotes/widget fresh even without alerts.
            container.repository.refresh()
            WidgetUpdater.refreshAll(applicationContext, container.repository)
            return Result.success()
        }

        container.repository.refresh()
        WidgetUpdater.refreshAll(applicationContext, container.repository)

        val data = container.repository.state.value as? MarketUiState.Data
            ?: return Result.success()
        val byId = data.snapshots.associateBy { it.assetId }
        val rules = container.settings.alerts.first()

        for (rule in rules) {
            val price = byId[rule.assetId]?.price ?: continue
            val hit = when (rule.direction) {
                Direction.ABOVE -> price >= rule.target
                Direction.BELOW -> price <= rule.target
            }
            when {
                hit && !rule.triggered -> {
                    val name = Assets.byId(rule.assetId)?.name ?: rule.assetId
                    NotificationHelper.notifyAlert(applicationContext, rule, name, price)
                    container.settings.markTriggered(rule.id)
                }
                !hit && rule.triggered -> {
                    // Price moved back — re-arm for next time.
                    container.settings.resetTrigger(rule.id)
                }
            }
        }
        return Result.success()
    }
}

/**
 * (Re)schedules the background refresh. `minutes <= 0` means manual-only:
 * any existing periodic work is cancelled.
 */
fun scheduleRefreshWork(context: Context, minutes: Int) {
    val wm = WorkManager.getInstance(context)
    if (minutes <= 0) {
        wm.cancelUniqueWork(WORK_NAME)
        return
    }
    val request = PeriodicWorkRequestBuilder<AlertWorker>(minutes.toLong(), TimeUnit.MINUTES)
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        )
        .build()
    wm.enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.UPDATE, request)
}
