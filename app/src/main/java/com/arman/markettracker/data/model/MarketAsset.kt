package com.arman.markettracker.data.model

/** The tradeable things this app tracks. */
enum class AssetKind { GOLD, CURRENCY }

data class MarketAsset(
    val id: String,
    val kind: AssetKind,
    /** Short ticker shown bold, e.g. "USD", "GOLD". */
    val symbol: String,
    /** Full display name, e.g. "US Dollar". */
    val name: String,
    /** true for the gold card that gets slight visual prominence on Home */
    val prominent: Boolean = false
)

object Assets {
    val GOLD_18K = MarketAsset("gold_18k", AssetKind.GOLD, "GOLD", "Gold 18K · per gram", prominent = true)
    val GOLD_24K = MarketAsset("gold_24k", AssetKind.GOLD, "GOLD", "Gold 24K · per gram")
    val USD = MarketAsset("usd", AssetKind.CURRENCY, "USD", "US Dollar")
    val USDT = MarketAsset("usdt", AssetKind.CURRENCY, "USDT", "Tether")
    val EUR = MarketAsset("eur", AssetKind.CURRENCY, "EUR", "Euro")

    val all: List<MarketAsset> = listOf(GOLD_18K, GOLD_24K, USD, USDT, EUR)

    /** Order shown on the Home overview. */
    val home: List<MarketAsset> = listOf(GOLD_18K, USD, USDT, EUR)

    fun byId(id: String): MarketAsset? = all.find { it.id == id }
}

/** A point-in-time quote for one asset. All prices are in Toman. */
data class PriceSnapshot(
    val assetId: String,
    val price: Double,
    val changePct24h: Double,
    val high24h: Double,
    val low24h: Double,
    val updatedAt: Long
)

/** One point of a price-history series. */
data class PricePoint(val t: Long, val price: Double)

enum class TimeRange(val label: String, val hours: Int) {
    H1("1H", 1),
    H6("6H", 6),
    D1("1D", 24),
    W1("1W", 24 * 7),
    M1("1M", 24 * 30)
}

enum class PriceUnit { TOMAN, RIAL }

enum class ThemeMode { LIGHT, DARK, SYSTEM }

enum class Direction { ABOVE, BELOW }

data class AlertRule(
    val id: String,
    val assetId: String,
    val target: Double,
    val direction: Direction,
    val triggered: Boolean = false
) {
    fun serialize(): String =
        listOf(id, assetId, target.toString(), direction.name, triggered.toString()).joinToString("|")

    companion object {
        fun parse(raw: String): AlertRule? {
            val p = raw.split("|")
            if (p.size != 5) return null
            return AlertRule(
                id = p[0],
                assetId = p[1],
                target = p[2].toDoubleOrNull() ?: return null,
                direction = runCatching { Direction.valueOf(p[3]) }.getOrNull() ?: return null,
                triggered = p[4].toBooleanStrictOrNull() ?: false
            )
        }
    }
}
