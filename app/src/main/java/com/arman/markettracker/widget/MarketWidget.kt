package com.arman.markettracker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import com.arman.markettracker.MainActivity
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.util.toChangePct
import com.arman.markettracker.util.toPrice

/** State keys — one "price|changePct" string per home asset. */
internal val KeyUpdatedAt = longPreferencesKey("w_updated_at")
internal fun keyFor(assetId: String) = stringPreferencesKey("w_$assetId")
internal val AssetIdParam = ActionParameters.Key<String>("asset_id")

private val CompactSize = DpSize(200.dp, 280.dp)
private val LargeSize = DpSize(320.dp, 320.dp)

// Fixed palette matching the reference design — always dark.
private val CardBg = Color(0xFF1E1E1E)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF9A9A9E)
private val Up = Color(0xFF30D158)
private val Down = Color(0xFFFF453A)

/**
 * Home-screen widget styled after the reference design:
 * a dark rounded card with one row per asset —
 * SYMBOL + name on the left, price + green/red % change on the right.
 * Tapping a row opens that asset's detail screen.
 */
class MarketWidget : GlanceAppWidget() {

    override val sizeMode: SizeMode = SizeMode.Responsive(setOf(CompactSize, LargeSize))
    override val stateDefinition = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent { WidgetContent() }
    }

    @Composable
    private fun WidgetContent() {
        val prefs = currentState<Preferences>()

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(CardBg)
                .cornerRadius(28.dp)
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Assets.home.forEachIndexed { index, asset ->
                val raw = prefs[keyFor(asset.id)]
                val price = raw?.substringBefore("|")?.toDoubleOrNull()
                val change = raw?.substringAfter("|", "")?.toDoubleOrNull()

                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(vertical = 9.dp)
                        .clickable(
                            actionStartActivity<MainActivity>(
                                actionParametersOf(AssetIdParam to asset.id)
                            )
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Left: symbol (bold) over name (gray).
                    Column(modifier = GlanceModifier.defaultWeight()) {
                        Text(
                            text = asset.symbol,
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            ),
                            maxLines = 1
                        )
                        Text(
                            text = asset.name,
                            style = TextStyle(
                                fontSize = 13.sp,
                                color = TextSecondary
                            ),
                            maxLines = 1
                        )
                    }
                    // Right: price (bold) over % change (green/red).
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = price?.toPrice() ?: "—",
                            style = TextStyle(
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp,
                                color = TextPrimary
                            ),
                            maxLines = 1
                        )
                        if (change != null) {
                            Text(
                                text = change.toChangePct(),
                                style = TextStyle(
                                    fontSize = 13.sp,
                                    color = if (change >= 0) Up else Down
                                ),
                                maxLines = 1
                            )
                        }
                    }
                }
                if (index < Assets.home.size - 1) {
                    Spacer(modifier = GlanceModifier.height(2.dp))
                }
            }
        }
    }
}
