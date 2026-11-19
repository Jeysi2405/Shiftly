package com.example.shiftly.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

object ShiftlyFormatters {

    private fun decimalFormat(pattern: String): DecimalFormat {
        val symbols = DecimalFormatSymbols(Locale.US)
        return DecimalFormat(pattern, symbols)
    }

    /**
     * Formats currency cleanly: omits `.00` on whole amounts (e.g. `₱0`, `₱3,000`)
     * and shows 2 decimals only when cents are `01..99` (e.g. `₱96.38`, `₱12.50`).
     */
    fun formatCurrency(
        symbol: String,
        amount: Double,
        showPlus: Boolean = false,
        showMinus: Boolean = false
    ): String {
        val safe = if (amount.isFinite()) amount else 0.0
        val totalCents = (abs(safe) * 100.0).roundToLong()
        val whole = totalCents / 100L
        val cents = totalCents % 100L
        val prefix = when {
            safe < -0.004 -> "-"
            showMinus && totalCents > 0L -> "-"
            showPlus && totalCents > 0L -> "+"
            else -> ""
        }
        val formattedNumber = if (cents == 0L) {
            decimalFormat("#,##0").format(whole)
        } else {
            decimalFormat("#,##0.00").format(totalCents / 100.0)
        }
        return "$prefix$symbol$formattedNumber"
    }

    /**
     * Formats plain numbers without currency symbol, omitting `.00` on whole numbers.
     */
    fun formatNumber(value: Double): String {
        val safe = if (value.isFinite()) value else 0.0
        val totalCents = (abs(safe) * 100.0).roundToLong()
        val whole = totalCents / 100L
        val cents = totalCents % 100L
        val sign = if (safe < -0.004) "-" else ""
        val number = if (cents == 0L) {
            decimalFormat("#,##0").format(whole)
        } else {
            decimalFormat("#,##0.00").format(totalCents / 100.0)
        }
        return "$sign$number"
    }

    /**
     * Formats hours cleanly: omits `.0` on whole hours (e.g. `0 hours`, `6 hours`, or `6h`),
     * and shows 1 decimal only when tenths are `1..9` (e.g. `6.5 hours`, `1.5h`).
     */
    fun formatHours(hours: Double, compact: Boolean = false): String {
        val safe = if (hours.isFinite() && hours >= 0.0) hours else 0.0
        val totalTenths = (safe * 10.0).roundToLong()
        val whole = totalTenths / 10L
        val tenths = totalTenths % 10L
        val numStr = if (tenths == 0L) {
            whole.toString()
        } else {
            "$whole.$tenths"
        }
        return if (compact) {
            "${numStr}h"
        } else {
            val unit = if (tenths == 0L && whole == 1L) "hour" else "hours"
            "$numStr $unit"
        }
    }
}
