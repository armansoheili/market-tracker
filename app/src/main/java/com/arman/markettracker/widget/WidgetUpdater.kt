package com.arman.markettracker.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.repository.MarketRepository
import com.arman.markettracker.data.repository.MarketUiState

/**
 * Pushes the latest quotes from [MarketRepository] into every placed
 * widget instance. Called after each in-app refresh and from the
 * background [com.arman.markettracker.alerts.AlertWorker].
 *
 * Updates are cheap and event-driven (no polling loop), so battery
 * impact stays minimal.
 */
object WidgetUpdater {

    suspend fun refreshAll(context: Context, repository: MarketRepository) {
        val data = repository.state.value as? MarketUiState.Data ?: return
        val byId = data.snapshots.associateBy { it.assetId }
        // Only home assets are shown on the widget.
        val rows = Assets.home.mapNotNull { asset ->
            byId[asset.id]?.let { asset.id to "${it.price}|${it.changePct24h}" }
        }
        val manager = GlanceAppWidgetManager(context)
        val ids = manager.getGlanceIds(MarketWidget::class.java)
        if (ids.isEmpty()) return
        ids.forEach { id ->
            updateAppWidgetState(context, id) { prefs ->
                rows.forEach { (assetId, raw) -> prefs[keyFor(assetId)] = raw }
                prefs[KeyUpdatedAt] = data.updatedAt
            }
        }
        val widget = MarketWidget()
        ids.forEach { id -> widget.update(context, id) }
    }
}
