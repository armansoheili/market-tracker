package com.arman.markettracker.data.repository

import android.content.Context
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.model.PricePoint
import com.arman.markettracker.data.model.PriceSnapshot
import com.arman.markettracker.data.model.TimeRange
import com.arman.markettracker.data.source.MarketDataSource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.cacheStore by preferencesDataStore("price_cache")

sealed interface MarketUiState {
    data object Loading : MarketUiState
    data class Data(
        val snapshots: List<PriceSnapshot>,
        val updatedAt: Long,
        /** true when these are the last good quotes and the network failed */
        val isStale: Boolean = false,
        val isOffline: Boolean = false
    ) : MarketUiState {
        fun snapshotFor(assetId: String): PriceSnapshot? =
            snapshots.find { it.assetId == assetId }
    }
    data class Error(val message: String) : MarketUiState
}

/**
 * Single source of truth for market data.
 *
 * - Pulls quotes from [MarketDataSource].
 * - Caches the last good quotes in DataStore so the app stays useful offline.
 * - Never exposes NaN / broken values: snapshots with non-finite prices are dropped.
 */
class MarketRepository(
    private val context: Context,
    private val source: MarketDataSource
) {
    private val _state = MutableStateFlow<MarketUiState>(MarketUiState.Loading)
    val state: StateFlow<MarketUiState> = _state.asStateFlow()

    suspend fun refresh() {
        val previous = (_state.value as? MarketUiState.Data)
        try {
            val snapshots = source.fetchSnapshots()
                .filter { it.price.isFinite() && it.price > 0 }
            if (snapshots.isEmpty()) throw IllegalStateException("empty feed")
            cacheSnapshots(snapshots)
            _state.value = MarketUiState.Data(
                snapshots = snapshots,
                updatedAt = snapshots.maxOf { it.updatedAt }
            )
        } catch (e: Exception) {
            val cached = readCached()
            _state.value = when {
                cached != null -> MarketUiState.Data(
                    snapshots = cached,
                    updatedAt = cached.maxOf { it.updatedAt },
                    isStale = true,
                    isOffline = true
                )
                previous != null -> previous.copy(isStale = true, isOffline = true)
                else -> MarketUiState.Error("Failed to load prices")
            }
        }
    }

    suspend fun history(assetId: String, range: TimeRange): List<PricePoint> =
        runCatching { source.fetchHistory(assetId, range) }
            .getOrElse { emptyList() }
            .filter { it.price.isFinite() && it.price > 0 }

    private suspend fun cacheSnapshots(snapshots: List<PriceSnapshot>) {
        context.cacheStore.edit { prefs ->
            for (s in snapshots) {
                prefs[doublePreferencesKey("price_${s.assetId}")] = s.price
                prefs[doublePreferencesKey("chg_${s.assetId}")] = s.changePct24h
                prefs[doublePreferencesKey("high_${s.assetId}")] = s.high24h
                prefs[doublePreferencesKey("low_${s.assetId}")] = s.low24h
                prefs[longPreferencesKey("ts_${s.assetId}")] = s.updatedAt
            }
        }
    }

    private suspend fun readCached(): List<PriceSnapshot>? {
        val prefs = context.cacheStore.data.first()
        val list = Assets.all.mapNotNull { asset ->
            val price = prefs[doublePreferencesKey("price_${asset.id}")] ?: return@mapNotNull null
            PriceSnapshot(
                assetId = asset.id,
                price = price,
                changePct24h = prefs[doublePreferencesKey("chg_${asset.id}")] ?: 0.0,
                high24h = prefs[doublePreferencesKey("high_${asset.id}")] ?: price,
                low24h = prefs[doublePreferencesKey("low_${asset.id}")] ?: price,
                updatedAt = prefs[longPreferencesKey("ts_${asset.id}")] ?: 0L
            )
        }
        return list.ifEmpty { null }
    }
}
