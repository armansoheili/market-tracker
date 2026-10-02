package com.arman.markettracker.util

import com.arman.markettracker.data.model.PriceUnit
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs

/**
 * Plain integer price with NO grouping separators, e.g. 18950000 → "18950000".
 * Never emits NaN/Infinity — falls back to "—".
 */
fun Double.toPrice(decimals: Int = 0): String {
    if (!isFinite()) return "—"
    val pattern = if (decimals > 0) "0.${"0".repeat(decimals)}" else "0"
    return try {
        DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(this)
    } catch (_: Exception) {
        "—"
    }
}

fun Long.toPrice(): String = toDouble().toPrice()

/** +2.59% / -0.12% — always with an explicit sign. */
fun Double.toChangePct(): String {
    if (!isFinite()) return "—"
    val sign = if (this >= 0) "+" else "-"
    return "$sign${"%.2f".format(Locale.US, abs(this))}%"
}

/** Convert a Toman quote into the user's chosen unit, then format. */
fun Double.toUnitPrice(unit: PriceUnit): String = when (unit) {
    PriceUnit.TOMAN -> toPrice()
    PriceUnit.RIAL -> (this * 10).toPrice()
}

fun PriceUnit.label(): String = when (this) {
    PriceUnit.TOMAN -> "Toman"
    PriceUnit.RIAL -> "Rial"
}

/** "14:32" in 24h clock. */
fun Long.toClock(): String {
    val cal = java.util.Calendar.getInstance().apply { timeInMillis = this@toClock }
    val h = cal.get(java.util.Calendar.HOUR_OF_DAY).toString().padStart(2, '0')
    val m = cal.get(java.util.Calendar.MINUTE).toString().padStart(2, '0')
    return "$h:$m"
}
