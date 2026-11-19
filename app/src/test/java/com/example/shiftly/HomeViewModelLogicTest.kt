package com.example.shiftly

import com.example.shiftly.ui.screens.formatNotificationTime
import com.example.shiftly.ui.screens.greetingForHour
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeViewModelLogicTest {

    @Test
    fun greetingForHour_returnsMorningAfternoonAndEveningCorrectly() {
        assertEquals("Good morning,", greetingForHour(5))
        assertEquals("Good morning,", greetingForHour(9))
        assertEquals("Good morning,", greetingForHour(11))

        assertEquals("Good afternoon,", greetingForHour(12))
        assertEquals("Good afternoon,", greetingForHour(15))
        assertEquals("Good afternoon,", greetingForHour(17))

        assertEquals("Good evening,", greetingForHour(18))
        assertEquals("Good evening,", greetingForHour(23))
        assertEquals("Good evening,", greetingForHour(0))
        assertEquals("Good evening,", greetingForHour(4))
    }

    @Test
    fun greetingForHour_normalizesNegativeAndOverflowHours() {
        assertEquals("Good morning,", greetingForHour(29))
        assertEquals("Good afternoon,", greetingForHour(38))
        assertEquals("Good evening,", greetingForHour(-2))
    }

    @Test
    fun formatNotificationTime_formatsRelativeYesterdayAndCalendarDates() {
        val zone = ZoneId.of("UTC")
        val reference = ZonedDateTime.of(2025, 5, 20, 18, 0, 0, 0, zone)
        val refMillis = reference.toInstant().toEpochMilli()

        assertEquals(
            "",
            formatNotificationTime(0L, referenceTimeMillis = refMillis, locale = Locale.ENGLISH, zoneId = zone)
        )
        assertEquals(
            "Just now",
            formatNotificationTime(refMillis - 30_000L, referenceTimeMillis = refMillis, locale = Locale.ENGLISH, zoneId = zone)
        )
        assertEquals(
            "15m ago",
            formatNotificationTime(refMillis - 15 * 60_000L, referenceTimeMillis = refMillis, locale = Locale.ENGLISH, zoneId = zone)
        )
        assertEquals(
            "3h ago",
            formatNotificationTime(refMillis - 3 * 3_600_000L, referenceTimeMillis = refMillis, locale = Locale.ENGLISH, zoneId = zone)
        )
        assertEquals(
            "8:00 AM",
            formatNotificationTime(
                ZonedDateTime.of(2025, 5, 20, 8, 0, 0, 0, zone).toInstant().toEpochMilli(),
                referenceTimeMillis = refMillis,
                locale = Locale.ENGLISH,
                zoneId = zone
            )
        )
        assertEquals(
            "Yesterday",
            formatNotificationTime(
                ZonedDateTime.of(2025, 5, 19, 14, 30, 0, 0, zone).toInstant().toEpochMilli(),
                referenceTimeMillis = refMillis,
                locale = Locale.ENGLISH,
                zoneId = zone
            )
        )
        assertEquals(
            "May 15",
            formatNotificationTime(
                ZonedDateTime.of(2025, 5, 15, 10, 0, 0, 0, zone).toInstant().toEpochMilli(),
                referenceTimeMillis = refMillis,
                locale = Locale.ENGLISH,
                zoneId = zone
            )
        )
    }
}
