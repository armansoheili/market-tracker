package com.arman.markettracker.data.source

import com.arman.markettracker.data.model.PricePoint
import com.arman.markettracker.data.model.PriceSnapshot
import com.arman.markettracker.data.model.TimeRange
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.random.Random

/**
 * Live market data — free public endpoints, no API keys, no signup.
 *
 * - `baha24.com/api/v1/price` → USD, EUR, USDT and 18k gold, all in Toman.
 *   Single call, verified live.
 * - `apiv2.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=rls` →
 *   USDT cross-check plus real 24h change/high/low (Rial → Toman).
 * - 24k gold is derived from 18k (× 4/3) — pure math, no source needed.
 *
 * Anything a source doesn't publish (e.g. 24h change for USD/EUR/gold)
 * is reported as 0 rather than invented. Price history is a deterministic
 * walk anchored at the live quote, so charts keep working without a
 * history API. If every source fails, an exception is thrown and the
 * repository falls back to its cache.
 */
class ApiMarketDataSource : MarketDataSource {

    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private fun get(url: String): String? {
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "MarketTracker/1.1")
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.string()
            }
        }.getOrNull()
    }

    /** symbol → price in Toman, from baha24. */
    private fun fetchBaha24(): Map<String, Double>? {
        val body = get("https://baha24.com/api/v1/price") ?: return null
        return runCatching {
            val arr = JSONArray(body)
            buildMap {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val symbol = o.optString("symbol")
                    val sell = o.optString("sell").toDoubleOrNull()
                    if (symbol.isNotEmpty() && sell != null && sell > 0 && sell.isFinite()) {
                        put(symbol, sell)
                    }
                }
            }
        }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private data class NobitexUsdt(
        val priceToman: Double,
        val changePct: Double,
        val highToman: Double,
        val lowToman: Double
    )

    /** USDT stats from Nobitex — prices arrive in Rial, converted to Toman. */
    private fun fetchNobitexUsdt(): NobitexUsdt? {
        val body = get("https://apiv2.nobitex.ir/market/stats?srcCurrency=usdt&dstCurrency=rls")
            ?: return null
        return runCatching {
            val stats = JSONObject(body).getJSONObject("stats").getJSONObject("usdt-rls")
            fun toman(key: String) = stats.optString(key).toDoubleOrNull()?.div(10) ?: 0.0
            NobitexUsdt(
                priceToman = toman("latest"),
                changePct = stats.optString("dayChange").toDoubleOrNull() ?: 0.0,
                highToman = toman("dayHigh"),
                lowToman = toman("dayLow")
            )
        }.getOrNull()?.takeIf { it.priceToman > 0 }
    }

    override suspend fun fetchSnapshots(): List<PriceSnapshot> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val baha = fetchBaha24()
        val nob = fetchNobitexUsdt()

        val usd = baha?.get("USD")
        val eur = baha?.get("EUR")
        val g18 = baha?.get("GOL18")

        val out = mutableListOf<PriceSnapshot>()
        fun snap(
            id: String,
            price: Double?,
            change: Double = 0.0,
            high: Double? = null,
            low: Double? = null
        ) {
            val p = price ?: return
            if (p <= 0 || !p.isFinite()) return
            out += PriceSnapshot(id, p, change, high ?: p, low ?: p, now)
        }

        snap("usd", usd)
        snap("eur", eur)
        if (nob != null) {
            snap("usdt", nob.priceToman, nob.changePct, nob.highToman, nob.lowToman)
        } else {
            snap("usdt", baha?.get("USDT"))
        }
        snap("gold_18k", g18)
        snap("gold_24k", g18?.times(4.0 / 3.0))

        if (out.isEmpty()) throw IllegalStateException("all price sources failed")
        out
    }

    override suspend fun fetchHistory(assetId: String, range: TimeRange): List<PricePoint> =
        withContext(Dispatchers.IO) {
            // Live anchor so the series always ends at the real quote.
            val anchor = liveAnchor(assetId) ?: return@withContext emptyList()
            syntheticHistory(assetId, range, anchor)
        }

    /** Current live price for anchoring history, in Toman. */
    private fun liveAnchor(assetId: String): Double? {
        val baha = fetchBaha24()
        val nob = fetchNobitexUsdt()
        return when (assetId) {
            "usd" -> baha?.get("USD")
            "eur" -> baha?.get("EUR")
            "usdt" -> nob?.priceToman ?: baha?.get("USDT")
            "gold_18k" -> baha?.get("GOL18")
            "gold_24k" -> baha?.get("GOL18")?.times(4.0 / 3.0)
            else -> null
        }
    }

    /**
     * Deterministic random walk ending exactly at [anchor].
     * Seeded by hour-bucket so repeated loads within the hour agree.
     */
    private fun syntheticHistory(assetId: String, range: TimeRange, anchor: Double): List<PricePoint> {
        val now = System.currentTimeMillis()
        val (points, stepMs) = when (range) {
            TimeRange.H1 -> 60 to 60_000L
            TimeRange.H6 -> 72 to 5 * 60_000L
            TimeRange.D1 -> 96 to 15 * 60_000L
            TimeRange.W1 -> 84 to 2 * 60 * 60_000L
            TimeRange.M1 -> 90 to 8 * 60 * 60_000L
        }
        val vol = when (assetId) {
            "usdt" -> 0.0007
            "usd", "eur" -> 0.0009
            else -> 0.0011
        }
        val seed = now / (60 * 60_000L) + assetId.hashCode() + range.ordinal * 7919L
        val rnd = Random(seed)
        val prices = ArrayList<Double>(points)
        var p = anchor
        repeat(points) {
            prices.add(p)
            p /= (1.0 + (rnd.nextDouble() - 0.5) * 2 * vol * 3)
        }
        prices.reverse()
        prices[prices.lastIndex] = anchor
        return prices.mapIndexed { i, price ->
            PricePoint(now - (points - 1 - i) * stepMs, price)
        }
    }
}
