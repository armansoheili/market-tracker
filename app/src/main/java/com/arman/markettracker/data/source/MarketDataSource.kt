package com.arman.markettracker.data.source

import com.arman.markettracker.data.model.PricePoint
import com.arman.markettracker.data.model.PriceSnapshot
import com.arman.markettracker.data.model.TimeRange

/**
 * Abstraction over where market prices come from.
 *
 * To connect a real market-price API later, implement this interface
 * (e.g. `ApiMarketDataSource`) and swap it in inside [AppContainer] —
 * nothing in the UI, repository, widget or alerts layers needs to change.
 */
interface MarketDataSource {
    /** Latest quote for every tracked asset. Prices in Toman. */
    suspend fun fetchSnapshots(): List<PriceSnapshot>

    /** Price history for one asset over [range], oldest → newest. */
    suspend fun fetchHistory(assetId: String, range: TimeRange): List<PricePoint>
}
