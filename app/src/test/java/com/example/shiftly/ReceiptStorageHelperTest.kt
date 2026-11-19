package com.example.shiftly

import com.example.shiftly.data.local.ReceiptStorageHelper
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class ReceiptStorageHelperTest {

    @Test
    fun formatWatermarkTimestamp_matchesExpectedPattern() {
        val originalDefault = TimeZone.getDefault()
        try {
            val utc = TimeZone.getTimeZone("UTC")
            TimeZone.setDefault(utc)
            val calendar = Calendar.getInstance(utc, Locale.US).apply {
                set(2025, Calendar.MAY, 20, 8, 0, 15)
                set(Calendar.MILLISECOND, 0)
            }
            val formatted = ReceiptStorageHelper.formatWatermarkTimestamp(
                timestampMillis = calendar.timeInMillis,
                locale = Locale.US
            )
            assertEquals("2025-05-20 08:00:15 AM", formatted)

            calendar.set(Calendar.HOUR_OF_DAY, 20)
            calendar.set(Calendar.MINUTE, 5)
            calendar.set(Calendar.SECOND, 9)
            val formattedPm = ReceiptStorageHelper.formatWatermarkTimestamp(
                timestampMillis = calendar.timeInMillis,
                locale = Locale.US
            )
            assertEquals("2025-05-20 08:05:09 PM", formattedPm)
        } finally {
            TimeZone.setDefault(originalDefault)
        }
    }
}
