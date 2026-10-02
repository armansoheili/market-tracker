package com.arman.markettracker.ui.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.arman.markettracker.MarketViewModel
import com.arman.markettracker.data.model.Assets
import com.arman.markettracker.data.model.Direction
import com.arman.markettracker.data.model.PriceUnit
import com.arman.markettracker.data.model.ThemeMode
import com.arman.markettracker.ui.i18n.Strings
import com.arman.markettracker.util.label
import com.arman.markettracker.util.toPrice

@Composable
fun SettingsScreen(
    vm: MarketViewModel,
    strings: Strings,
    darkTheme: Boolean = false
) {
    val themeMode by vm.themeMode.collectAsState()
    val unit by vm.priceUnit.collectAsState()
    val refreshMinutes by vm.refreshMinutes.collectAsState()
    val alertsEnabled by vm.alertsEnabled.collectAsState()
    val alerts by vm.alerts.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Filled.Add, contentDescription = strings.alertAdd)
            }
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
                .statusBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            Text(strings.navSettings, style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(vertical = 4.dp))

            SettingsSection(title = strings.settingsTheme) {
                ThemeMode.entries.forEach { mode ->
                    val label = when (mode) {
                        ThemeMode.LIGHT -> strings.themeLight
                        ThemeMode.DARK -> strings.themeDark
                        ThemeMode.SYSTEM -> strings.themeSystem
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setTheme(mode) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == mode, onClick = { vm.setTheme(mode) })
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            SettingsSection(title = strings.settingsPriceUnit) {
                PriceUnit.entries.forEach { u ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setPriceUnit(u) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = unit == u, onClick = { vm.setPriceUnit(u) })
                        Text(u.label(), modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            SettingsSection(title = strings.settingsRefresh) {
                listOf(15, 30, 60, 0).forEach { minutes ->
                    val label = if (minutes == 0) strings.refreshManual
                    else strings.refreshEveryMinutes(minutes.toString())
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { vm.setRefreshMinutes(minutes) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = refreshMinutes == minutes,
                            onClick = { vm.setRefreshMinutes(minutes) }
                        )
                        Text(label, modifier = Modifier.padding(start = 8.dp))
                    }
                }
            }

            SettingsSection(title = strings.settingsDataSource) {
                Text(strings.dataSourceLive, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    strings.dataSourceLiveNote,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SettingsSection(title = strings.settingsWidget) {
                TextButton(onClick = {
                    // Push the latest quotes to any placed widgets immediately.
                    vm.refresh()
                }) { Text(strings.widgetUpdateNow) }
            }

            SettingsSection(title = strings.settingsNotifications) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(strings.alertsEnable)
                    Switch(
                        checked = alertsEnabled,
                        onCheckedChange = { vm.setAlertsEnabled(it) }
                    )
                }
                Spacer(Modifier.height(8.dp))
                if (alerts.isEmpty()) {
                    Text(
                        strings.alertsEmpty,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    alerts.forEach { rule ->
                        val asset = Assets.byId(rule.assetId)
                        val dir = if (rule.direction == Direction.ABOVE) strings.alertAbove else strings.alertBelow
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                strings.alertWhen(
                                    asset?.name ?: rule.assetId,
                                    dir,
                                    rule.target.toPrice()
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { vm.removeAlert(rule.id) }) {
                                Icon(Icons.Filled.Delete, contentDescription = strings.delete)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(72.dp))
        }
    }

    if (showAddDialog) {
        AddAlertDialog(strings = strings, onDismiss = { showAddDialog = false }) { assetId, target, dir ->
            vm.addAlert(assetId, target, dir)
            showAddDialog = false
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable () -> Unit
) {
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(
            1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            content()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddAlertDialog(
    strings: Strings,
    onDismiss: () -> Unit,
    onSave: (assetId: String, target: Double, dir: Direction) -> Unit
) {
    var assetId by remember { mutableStateOf(Assets.USD.id) }
    var targetText by remember { mutableStateOf("") }
    var above by remember { mutableStateOf(true) }
    var expanded by remember { mutableStateOf(false) }
    val target = targetText.filter { it.isDigit() }.toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.alertAdd) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = it }) {
                    OutlinedTextField(
                        value = Assets.byId(assetId)?.name ?: "",
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        Assets.home.forEach { a ->
                            DropdownMenuItem(
                                text = { Text(a.name) },
                                onClick = { assetId = a.id; expanded = false }
                            )
                        }
                    }
                }
                OutlinedTextField(
                    value = targetText,
                    onValueChange = { targetText = it.filter { c -> c.isDigit() } },
                    label = { Text("${strings.alertTarget} (Toman)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    RadioButton(selected = above, onClick = { above = true })
                    Text(strings.alertAbove, modifier = Modifier.clickable { above = true })
                    Spacer(Modifier.padding(8.dp))
                    RadioButton(selected = !above, onClick = { above = false })
                    Text(strings.alertBelow, modifier = Modifier.clickable { above = false })
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val t = target ?: return@TextButton
                    onSave(assetId, t, if (above) Direction.ABOVE else Direction.BELOW)
                },
                enabled = target != null
            ) { Text(strings.save) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(strings.cancel) } }
    )
}
