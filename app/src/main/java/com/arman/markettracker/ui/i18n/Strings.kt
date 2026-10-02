package com.arman.markettracker.ui.i18n

/**
 * All user-visible strings — English.
 */
interface Strings {
    val appTitle: String
    val lastUpdated: String
    val todayChange: String
    val high24h: String
    val low24h: String
    val change24h: String
    val navHome: String
    val navMarkets: String
    val navSettings: String
    val settingsTheme: String
    val themeLight: String
    val themeDark: String
    val themeSystem: String
    val settingsPriceUnit: String
    val unitToman: String
    val unitRial: String
    val settingsRefresh: String
    val refreshManual: String
    val refreshEveryMinutes: (String) -> String
    val settingsDataSource: String
    val dataSourceLive: String
    val dataSourceLiveNote: String
    val settingsWidget: String
    val widgetUpdateNow: String
    val settingsNotifications: String
    val alertsEnable: String
    val alertsEmpty: String
    val alertAdd: String
    val alertTarget: String
    val alertAbove: String
    val alertBelow: String
    val alertWhen: (asset: String, direction: String, target: String) -> String
    val delete: String
    val cancel: String
    val save: String
    val retry: String
    val offlineStale: String
    val errorLoading: String
    val noData: String
    val perGram: String
    val gold18k: String
    val gold24k: String
    val updatedAtClock: (String) -> String
}

object EnStrings : Strings {
    override val appTitle = "Market Tracker"
    override val lastUpdated = "Last updated"
    override val todayChange = "Today's change"
    override val high24h = "24h high"
    override val low24h = "24h low"
    override val change24h = "24h change"
    override val navHome = "Home"
    override val navMarkets = "Markets"
    override val navSettings = "Settings"
    override val settingsTheme = "Theme"
    override val themeLight = "Light"
    override val themeDark = "Dark"
    override val themeSystem = "System"
    override val settingsPriceUnit = "Price unit"
    override val unitToman = "Toman"
    override val unitRial = "Rial"
    override val settingsRefresh = "Refresh interval"
    override val refreshManual = "Manual"
    override val refreshEveryMinutes: (String) -> String = { m -> "Every $m minutes" }
    override val settingsDataSource = "Data source"
    override val dataSourceLive = "Live market data"
    override val dataSourceLiveNote = "Live prices: baha24 for dollar, euro, Tether and gold; Nobitex cross-check for Tether with real 24h change."
    override val settingsWidget = "Widget"
    override val widgetUpdateNow = "Update widget now"
    override val settingsNotifications = "Notifications"
    override val alertsEnable = "Price alerts"
    override val alertsEmpty = "No alerts yet."
    override val alertAdd = "Add alert"
    override val alertTarget = "Target price"
    override val alertAbove = "Above"
    override val alertBelow = "Below"
    override val alertWhen: (String, String, String) -> String =
        { asset, direction, target -> "When $asset goes $direction $target" }
    override val delete = "Delete"
    override val cancel = "Cancel"
    override val save = "Save"
    override val retry = "Retry"
    override val offlineStale = "Offline — showing last saved prices"
    override val errorLoading = "Failed to load prices"
    override val noData = "No data available"
    override val perGram = "per gram"
    override val gold18k = "18K"
    override val gold24k = "24K"
    override val updatedAtClock: (String) -> String = { "at $it" }
}
