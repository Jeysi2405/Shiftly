package com.example.shiftly

import com.example.shiftly.utils.ShiftlyFormatters
import org.junit.Assert.assertEquals
import org.junit.Test

class ShiftlyFormattersTest {

    @Test
    fun formatCurrency_omitsDecimalOnWholeAmounts() {
        assertEquals("₱0", ShiftlyFormatters.formatCurrency("₱", 0.0))
        assertEquals("₱3,000", ShiftlyFormatters.formatCurrency("₱", 3000.0))
        assertEquals("+₱500", ShiftlyFormatters.formatCurrency("₱", 500.0, showPlus = true))
        assertEquals("-₱250", ShiftlyFormatters.formatCurrency("₱", 250.0, showMinus = true))
        assertEquals("₱0", ShiftlyFormatters.formatCurrency("₱", 0.0, showPlus = true))
    }

    @Test
    fun formatCurrency_showsDecimalsWhenCentsPresent() {
        assertEquals("₱96.38", ShiftlyFormatters.formatCurrency("₱", 96.38))
        assertEquals("₱12.50", ShiftlyFormatters.formatCurrency("₱", 12.5))
        assertEquals("₱1,250.05", ShiftlyFormatters.formatCurrency("₱", 1250.05))
        assertEquals("-₱99.99", ShiftlyFormatters.formatCurrency("₱", -99.99))
    }

    @Test
    fun formatHours_omitsDecimalOnWholeHours_andShowsTenthsWhenNeeded() {
        assertEquals("0 hours", ShiftlyFormatters.formatHours(0.0))
        assertEquals("1 hour", ShiftlyFormatters.formatHours(1.0))
        assertEquals("6 hours", ShiftlyFormatters.formatHours(6.0))
        assertEquals("6.5 hours", ShiftlyFormatters.formatHours(6.5))

        assertEquals("0h", ShiftlyFormatters.formatHours(0.0, compact = true))
        assertEquals("6h", ShiftlyFormatters.formatHours(6.0, compact = true))
        assertEquals("6.5h", ShiftlyFormatters.formatHours(6.5, compact = true))
    }

    @Test
    fun formatNumber_handlesWholeAndFractionalValues() {
        assertEquals("0", ShiftlyFormatters.formatNumber(0.0))
        assertEquals("1,500", ShiftlyFormatters.formatNumber(1500.0))
        assertEquals("96.38", ShiftlyFormatters.formatNumber(96.38))
    }
}
