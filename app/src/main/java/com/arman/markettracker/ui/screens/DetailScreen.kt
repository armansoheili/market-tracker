package com.arman.markettracker.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.arman.markettracker.MarketViewModel
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.model.TimeRange
import com.arman.markettracker.data.repository.MarketUiState
import com.arman.markettracker.ui.components.ChangeBadge
import com.arman.markettracker.ui.components.PriceChart
import com.arman.markettracker.ui.i18n.Strings
import com.arman.markettracker.util.label
import com.arman.markettracker.util.toClock
import com.arman.markettracker.util.toUnitPrice

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    vm: MarketViewModel,
    strings: Strings,
    assetId: String,
    onBack: () -> Unit,
    darkTheme: Boolean = false
) {
    val asset = Assets.byId(assetId) ?: Assets.USD
    val state by vm.marketState.collectAsState()
    val unit by vm.priceUnit.collectAsState()
    val history by vm.historyCache.collectAsState()
    var range by remember { mutableStateOf(TimeRange.D1) }

    val key = "${asset.id}:${range.name}"
    LaunchedEffect(key) { vm.loadHistory(asset.id, range) }
    val points = history[key].orEmpty()
    val snapshot = (state as? MarketUiState.Data)?.snapshotFor(asset.id)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(asset.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // Hero price.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = snapshot?.price?.toUnitPrice(unit) ?: "—",
                    style = MaterialTheme.typography.displayLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = unit.label(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (snapshot != null) {
                        ChangeBadge(changePct = snapshot.changePct24h, darkTheme = darkTheme)
                    }
                }
            }

            // Chart card — subtle, never dominant.
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    PriceChart(points = points, darkTheme = darkTheme)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                    ) {
                        TimeRange.entries.forEach { r ->
                            FilterChip(
                                selected = r == range,
                                onClick = { range = r },
                                label = { Text(r.label) }
                            )
                        }
                    }
                }
            }

            // Stats grid.
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    label = strings.high24h,
                    value = "${snapshot?.high24h?.toUnitPrice(unit) ?: "—"} ${unit.label()}",
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = strings.low24h,
                    value = "${snapshot?.low24h?.toUnitPrice(unit) ?: "—"} ${unit.label()}",
                    modifier = Modifier.weight(1f)
                )
            }

            snapshot?.let {
                Text(
                    text = "${strings.lastUpdated}: ${strings.updatedAtClock(it.updatedAt.toClock())}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(text = value, style = MaterialTheme.typography.titleLarge)
        }
    }
}
