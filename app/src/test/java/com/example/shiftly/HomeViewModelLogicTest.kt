package com.example.shiftly

import com.example.shiftly.ui.screens.greetingForHour
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
}
