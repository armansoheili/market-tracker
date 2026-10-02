package com.arman.markettracker.widget

import androidx.glance.appwidget.GlanceAppWidgetReceiver

class MarketWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget = MarketWidget()
}
