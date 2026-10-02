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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.arman.markettracker.MarketViewModel
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.repository.MarketUiState
import com.arman.markettracker.ui.components.AssetCard
import com.arman.markettracker.ui.components.SegmentedControl
import com.arman.markettracker.ui.i18n.Strings
import com.arman.markettracker.util.toClock

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    vm: MarketViewModel,
    strings: Strings,
    onOpenAsset: (String) -> Unit,
    darkTheme: Boolean = false // resolved by caller from theme state
) {
    val state by vm.marketState.collectAsState()
    val unit by vm.priceUnit.collectAsState()
    var goldIndex by remember { mutableIntStateOf(0) } // 0 → 18K, 1 → 24K

    // The refresh spinner follows the real loading state — never a fake timer.
    val isRefreshing = state is MarketUiState.Loading

    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { vm.refresh() },
        modifier = Modifier.fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            // Header: title + last-updated + manual refresh.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(strings.appTitle, style = MaterialTheme.typography.titleLarge)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val updatedAt = (state as? MarketUiState.Data)?.updatedAt
                    if (updatedAt != null) {
                        Text(
                            text = strings.updatedAtClock(updatedAt.toClock()),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { vm.refresh() }) {
                        Icon(Icons.Filled.Refresh, contentDescription = strings.lastUpdated)
                    }
                }
            }

            when (val s = state) {
                is MarketUiState.Loading -> {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) { CircularProgressIndicator() }
                }
                is MarketUiState.Error -> {
                    Column(
                        modifier = Modifier.fillMaxSize().padding(32.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(s.message, style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(12.dp))
                        TextButton(onClick = { vm.refresh() }) { Text(strings.retry) }
                    }
                }
                is MarketUiState.Data -> {
                    if (s.isOffline) {
                        Surface(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = strings.offlineStale,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            start = 20.dp, end = 20.dp, bottom = 24.dp
                        ),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Gold card with 18K/24K segmented switch.
                        item {
                            val goldAsset = if (goldIndex == 0) Assets.GOLD_18K else Assets.GOLD_24K
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SegmentedControl(
                                    options = listOf(strings.gold18k, strings.gold24k),
                                    selectedIndex = goldIndex,
                                    onSelect = { goldIndex = it }
                                )
                                AssetCard(
                                    asset = goldAsset,
                                    snapshot = s.snapshotFor(goldAsset.id),
                                    unit = unit,
                                    darkTheme = darkTheme,
                                    subLabel = strings.perGram,
                                    onClick = { onOpenAsset(goldAsset.id) }
                                )
                            }
                        }
                        items(Assets.home.filter { it.id != Assets.GOLD_18K.id }) { asset ->
                            AssetCard(
                                asset = asset,
                                snapshot = s.snapshotFor(asset.id),
                                unit = unit,
                                darkTheme = darkTheme,
                                onClick = { onOpenAsset(asset.id) }
                            )
                        }
                    }
                }
            }
        }
    }
}
