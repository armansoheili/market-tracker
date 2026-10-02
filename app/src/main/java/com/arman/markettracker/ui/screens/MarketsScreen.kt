package com.arman.markettracker.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arman.markettracker.MarketViewModel
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.model.TimeRange
import com.arman.markettracker.data.repository.MarketUiState
import com.arman.markettracker.ui.components.ChangeBadge
import com.arman.markettracker.ui.components.MiniSparkline
import com.arman.markettracker.ui.i18n.Strings
import com.arman.markettracker.util.label
import com.arman.markettracker.util.toUnitPrice

/**
 * Detailed asset list with sparklines — one step deeper than Home.
 */
@Composable
fun MarketsScreen(
    vm: MarketViewModel,
    strings: Strings,
    onOpenAsset: (String) -> Unit,
    darkTheme: Boolean = false
) {
    val state by vm.marketState.collectAsState()
    val unit by vm.priceUnit.collectAsState()
    val history by vm.historyCache.collectAsState()

    LaunchedEffect(Unit) {
        Assets.all.forEach { vm.loadHistory(it.id, TimeRange.D1) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        Text(
            text = strings.navMarkets,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
        )
        val snapshots = (state as? MarketUiState.Data)?.snapshots.orEmpty()
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 20.dp, end = 20.dp, bottom = 24.dp
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(Assets.all) { asset ->
                val snap = snapshots.find { it.assetId == asset.id }
                val spark = history["${asset.id}:${TimeRange.D1.name}"].orEmpty()
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenAsset(asset.id) },
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(asset.name, style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = "${snap?.price?.toUnitPrice(unit) ?: "—"} ${unit.label()}",
                                style = MaterialTheme.typography.headlineSmall
                            )
                        }
                        Column(
                            modifier = Modifier.width(110.dp),
                            horizontalAlignment = Alignment.End,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (snap != null) {
                                ChangeBadge(changePct = snap.changePct24h, darkTheme = darkTheme)
                            }
                            MiniSparkline(
                                points = spark,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
