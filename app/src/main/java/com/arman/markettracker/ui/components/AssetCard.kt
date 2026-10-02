package com.arman.markettracker.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.arman.markettracker.data.model.MarketAsset
import com.arman.markettracker.data.model.PriceSnapshot
import com.arman.markettracker.data.model.PriceUnit
import com.arman.markettracker.ui.theme.GoldTintDark
import com.arman.markettracker.ui.theme.GoldTintLight
import com.arman.markettracker.util.label
import com.arman.markettracker.util.toUnitPrice

/**
 * One market card on the Home screen.
 *
 * Price changes crossfade subtly instead of flashing — the card never
 * animates on its own. Gold cards get a whisper of prominence.
 */
@Composable
fun AssetCard(
    asset: MarketAsset,
    snapshot: PriceSnapshot?,
    unit: PriceUnit,
    darkTheme: Boolean,
    subLabel: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val goldTint = if (darkTheme) GoldTintDark else GoldTintLight
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (asset.prominent) {
                    Modifier.drawBehind {
                        // Thin warm accent line on the card's top edge.
                        drawLine(
                            color = goldTint,
                            start = Offset(0f, 0f),
                            end = Offset(size.width, 0f),
                            strokeWidth = 3.dp.toPx()
                        )
                    }
                } else Modifier
            )
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = if (asset.prominent) 20.dp else 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = asset.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (subLabel != null) {
                    Text(
                        text = subLabel,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Subtle crossfade on value change — no flashing, no bounce.
                Crossfade(
                    targetState = snapshot?.price?.toUnitPrice(unit) ?: "—",
                    animationSpec = tween(300),
                    label = "price"
                ) { priceText ->
                    Text(
                        text = priceText,
                        style = if (asset.prominent) MaterialTheme.typography.displayLarge
                        else MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = unit.label(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (snapshot != null) {
                        ChangeBadge(changePct = snapshot.changePct24h, darkTheme = darkTheme)
                    }
                }
            }
        }
    }
}
