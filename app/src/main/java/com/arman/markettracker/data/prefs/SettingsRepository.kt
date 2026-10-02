package com.arman.markettracker.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.arman.markettracker.data.model.AlertRule
import com.arman.markettracker.data.model.PriceUnit
import com.arman.markettracker.data.model.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

private val Context.settingsStore by preferencesDataStore("settings")

/** User preferences: theme, price unit, refresh cadence, widget + alert settings. */
class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME = stringPreferencesKey("theme")
        val PRICE_UNIT = stringPreferencesKey("price_unit")
        val REFRESH_MINUTES = intPreferencesKey("refresh_minutes") // 0 = manual
        val ALERTS_ENABLED = booleanPreferencesKey("alerts_enabled")
        val ALERTS = stringSetPreferencesKey("alerts")
    }

    val theme: Flow<ThemeMode> = context.settingsStore.data.map { p ->
        runCatching { ThemeMode.valueOf(p[Keys.THEME] ?: "SYSTEM") }.getOrDefault(ThemeMode.SYSTEM)
    }

    val priceUnit: Flow<PriceUnit> = context.settingsStore.data.map { p ->
        runCatching { PriceUnit.valueOf(p[Keys.PRICE_UNIT] ?: "TOMAN") }.getOrDefault(PriceUnit.TOMAN)
    }

    /** 15 / 30 / 60 minutes, or 0 for manual-only refresh. */
    val refreshMinutes: Flow<Int> = context.settingsStore.data.map { p ->
        p[Keys.REFRESH_MINUTES] ?: 30
    }

    val alertsEnabled: Flow<Boolean> = context.settingsStore.data.map { p ->
        p[Keys.ALERTS_ENABLED] ?: false
    }

    val alerts: Flow<List<AlertRule>> = context.settingsStore.data.map { p ->
        (p[Keys.ALERTS] ?: emptySet()).mapNotNull(AlertRule::parse).sortedBy { it.assetId }
    }

    suspend fun setTheme(mode: ThemeMode) {
        context.settingsStore.edit { it[Keys.THEME] = mode.name }
    }

    suspend fun setPriceUnit(unit: PriceUnit) {
        context.settingsStore.edit { it[Keys.PRICE_UNIT] = unit.name }
    }

    suspend fun setRefreshMinutes(minutes: Int) {
        context.settingsStore.edit { it[Keys.REFRESH_MINUTES] = minutes }
    }

    suspend fun setAlertsEnabled(enabled: Boolean) {
        context.settingsStore.edit { it[Keys.ALERTS_ENABLED] = enabled }
    }

    suspend fun addAlert(assetId: String, target: Double, direction: com.arman.markettracker.data.model.Direction) {
        val rule = AlertRule(UUID.randomUUID().toString(), assetId, target, direction)
        context.settingsStore.edit { prefs ->
            prefs[Keys.ALERTS] = (prefs[Keys.ALERTS] ?: emptySet()) + rule.serialize()
        }
    }

    suspend fun removeAlert(id: String) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.ALERTS] = (prefs[Keys.ALERTS] ?: emptySet())
                .mapNotNull(AlertRule::parse)
                .filter { it.id != id }
                .map { it.serialize() }
                .toSet()
        }
    }

    suspend fun markTriggered(id: String) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.ALERTS] = (prefs[Keys.ALERTS] ?: emptySet())
                .mapNotNull(AlertRule::parse)
                .map { if (it.id == id) it.copy(triggered = true) else it }
                .map { it.serialize() }
                .toSet()
        }
    }

    suspend fun resetTrigger(id: String) {
        context.settingsStore.edit { prefs ->
            prefs[Keys.ALERTS] = (prefs[Keys.ALERTS] ?: emptySet())
                .mapNotNull(AlertRule::parse)
                .map { if (it.id == id) it.copy(triggered = false) else it }
                .map { it.serialize() }
                .toSet()
        }
    }
}
