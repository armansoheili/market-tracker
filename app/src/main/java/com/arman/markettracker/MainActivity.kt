package com.arman.markettracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.core.view.WindowCompat
import com.arman.markettracker.data.model.ThemeMode
import com.arman.markettracker.ui.i18n.EnStrings
import com.arman.markettracker.ui.navigation.AppNav
import com.arman.markettracker.ui.navigation.DeepLink
import com.arman.markettracker.ui.theme.MarketTrackerTheme

class MainActivity : ComponentActivity() {

    private val vm: MarketViewModel by viewModels {
        MarketViewModel.factory((application as MarketApp).container)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        vm.postDeepLink(intent.deepLinkOrNull())

        setContent {
            val themeMode by vm.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
            }
            MarketTrackerTheme(darkTheme = darkTheme) {
                AppNav(vm = vm, strings = EnStrings)
            }
        }

        vm.refresh()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        vm.postDeepLink(intent.deepLinkOrNull())
    }

    companion object {
        const val EXTRA_ASSET_ID = "asset_id"
    }
}

private fun Intent.deepLinkOrNull(): DeepLink? {
    val assetId = getStringExtra(MainActivity.EXTRA_ASSET_ID) ?: return null
    return DeepLink.AssetDetail(assetId)
}
