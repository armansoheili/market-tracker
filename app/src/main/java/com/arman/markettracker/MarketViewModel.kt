package com.arman.markettracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arman.markettracker.data.model.AlertRule
import com.arman.markettracker.data.model.Direction
import com.arman.markettracker.data.model.PricePoint
import com.arman.markettracker.data.model.PriceUnit
import com.arman.markettracker.data.model.ThemeMode
import com.arman.markettracker.data.model.TimeRange
import com.arman.markettracker.data.repository.MarketUiState
import com.arman.markettracker.widget.WidgetUpdater
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarketViewModel(private val container: AppContainer) : ViewModel() {

    val marketState: StateFlow<MarketUiState> = container.repository.state

    val themeMode: StateFlow<ThemeMode> =
        container.settings.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    val priceUnit: StateFlow<PriceUnit> =
        container.settings.priceUnit.stateIn(viewModelScope, SharingStarted.Eagerly, PriceUnit.TOMAN)

    val refreshMinutes: StateFlow<Int> =
        container.settings.refreshMinutes.stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    val alertsEnabled: StateFlow<Boolean> =
        container.settings.alertsEnabled.stateIn(viewModelScope, SharingStarted.Eagerly, false)

    val alerts: StateFlow<List<AlertRule>> =
        container.settings.alerts.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private val _historyCache = MutableStateFlow<Map<String, List<PricePoint>>>(emptyMap())
    val historyCache: StateFlow<Map<String, List<PricePoint>>> = _historyCache.asStateFlow()

    private val _pendingDeepLink =
        MutableStateFlow<com.arman.markettracker.ui.navigation.DeepLink?>(null)
    val pendingDeepLink: StateFlow<com.arman.markettracker.ui.navigation.DeepLink?> =
        _pendingDeepLink.asStateFlow()

    /** Called from MainActivity when launched with an asset deep link (e.g. from the widget). */
    fun postDeepLink(dl: com.arman.markettracker.ui.navigation.DeepLink?) {
        if (dl != null) _pendingDeepLink.value = dl
    }

    fun consumeDeepLink() {
        _pendingDeepLink.value = null
    }

    fun refresh() {
        viewModelScope.launch {
            container.repository.refresh()
            // Keep the home-screen widget in sync with fresh quotes.
            WidgetUpdater.refreshAll(container.app, container.repository)
        }
    }

    fun loadHistory(assetId: String, range: TimeRange) {
        viewModelScope.launch {
            val key = "$assetId:${range.name}"
            val points = container.repository.history(assetId, range)
            _historyCache.value = _historyCache.value + (key to points)
        }
    }

    fun setTheme(mode: ThemeMode) = viewModelScope.launch { container.settings.setTheme(mode) }
    fun setPriceUnit(unit: PriceUnit) = viewModelScope.launch { container.settings.setPriceUnit(unit) }

    fun setRefreshMinutes(minutes: Int) = viewModelScope.launch {
        container.settings.setRefreshMinutes(minutes)
        com.arman.markettracker.alerts.scheduleRefreshWork(container.app, minutes)
    }

    fun setAlertsEnabled(enabled: Boolean) = viewModelScope.launch {
        container.settings.setAlertsEnabled(enabled)
    }

    fun addAlert(assetId: String, target: Double, direction: Direction) =
        viewModelScope.launch { container.settings.addAlert(assetId, target, direction) }

    fun removeAlert(id: String) = viewModelScope.launch { container.settings.removeAlert(id) }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T =
                    MarketViewModel(container) as T
            }
    }
}
