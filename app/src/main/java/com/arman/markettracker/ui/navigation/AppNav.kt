package com.arman.markettracker.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.arman.markettracker.MarketViewModel
import com.arman.markettracker.data.model.ThemeMode
import com.arman.markettracker.ui.i18n.Strings
import com.arman.markettracker.ui.screens.DetailScreen
import com.arman.markettracker.ui.screens.HomeScreen
import com.arman.markettracker.ui.screens.MarketsScreen
import com.arman.markettracker.ui.screens.SettingsScreen

sealed class DeepLink {
    data class AssetDetail(val assetId: String) : DeepLink()
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

@Composable
fun AppNav(
    vm: MarketViewModel,
    strings: Strings
) {
    val navController = rememberNavController()
    val tabs = listOf(
        Tab("home", strings.navHome, Icons.Filled.Home),
        Tab("markets", strings.navMarkets, Icons.Filled.ShowChart),
        Tab("settings", strings.navSettings, Icons.Filled.Settings)
    )

    // Widget taps arrive here via the ViewModel (see MainActivity).
    val pendingDeepLink by vm.pendingDeepLink.collectAsState()
    LaunchedEffect(pendingDeepLink) {
        when (val dl = pendingDeepLink) {
            is DeepLink.AssetDetail -> navController.navigate("detail/${dl.assetId}")
            null -> return@LaunchedEffect
        }
        vm.consumeDeepLink()
    }

    val backStack by navController.currentBackStackEntryAsState()
    val currentRoute = backStack?.destination?.route
    val showBottomBar = currentRoute in tabs.map { it.route }
    val themeMode by vm.themeMode.collectAsState()
    val darkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = {
                                navController.navigate(tab.route) {
                                    popUpTo("home")
                                    launchSingleTop = true
                                }
                            },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
                            label = { Text(tab.label) }
                        )
                    }
                }
            }
        }
    ) { inner ->
        NavHost(
            navController = navController,
            startDestination = "home",
            modifier = Modifier.padding(inner)
        ) {
            composable("home") { HomeScreen(vm, strings, darkTheme = darkTheme, onOpenAsset = { id -> navController.navigate("detail/$id") }) }
            composable("markets") { MarketsScreen(vm, strings, darkTheme = darkTheme, onOpenAsset = { id -> navController.navigate("detail/$id") }) }
            composable("settings") { SettingsScreen(vm, strings, darkTheme = darkTheme) }
            composable(
                "detail/{assetId}",
                arguments = listOf(navArgument("assetId") { type = NavType.StringType })
            ) { entry ->
                DetailScreen(
                    vm = vm,
                    strings = strings,
                    assetId = entry.arguments?.getString("assetId").orEmpty(),
                    darkTheme = darkTheme,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
