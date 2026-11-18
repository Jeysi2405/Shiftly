package com.example.shiftly

import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.PreferenceJsonCodec
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PreferenceJsonCodecTest {

    @Test
    fun categoryBudgets_withColonsAndSemicolons_roundTripViaJson() {
        val input = listOf(
            CategoryBudgetSetting("Food: Dining; Coffee", 1500.0),
            CategoryBudgetSetting("Transit | Grab", 650.5)
        )
        val encoded = PreferenceJsonCodec.encodeCategoryBudgets(input)
        val decoded = PreferenceJsonCodec.decodeCategoryBudgets(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun categoryBudgets_emptyJsonArray_returnsEmptyListWhileNullUsesDefaults() {
        assertTrue(PreferenceJsonCodec.decodeCategoryBudgets("[]").isEmpty())
        assertEquals(
            ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS,
            PreferenceJsonCodec.decodeCategoryBudgets(null)
        )
        assertEquals(
            ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS,
            PreferenceJsonCodec.decodeCategoryBudgets("malformed-json")
        )
    }

    @Test
    fun managedCategories_withPipesAndSemicolons_roundTripViaJson() {
        val input = listOf(
            ManagedCategorySetting("Books | Print; Lab", true),
            ManagedCategorySetting("Gaming", false)
        )
        val encoded = PreferenceJsonCodec.encodeManagedCategories(input)
        val decoded = PreferenceJsonCodec.decodeManagedCategories(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun recurringEntries_defaultEmptyAndRoundTripViaJson() {
        assertTrue(ShiftlySettingsSnapshot.DEFAULT_RECURRING_ENTRIES.isEmpty())
        assertTrue(PreferenceJsonCodec.decodeRecurringEntries(null).isEmpty())

        val input = listOf(
            RecurringEntrySetting(
                id = "r1",
                title = "Allowance | Part-Time; Bonus",
                amount = 1800.0,
                frequency = "Weekly",
                isIncome = true,
                isEnabled = true,
                category = "Income",
                lastAppliedEpochDay = 20400L
            )
        )
        val encoded = PreferenceJsonCodec.encodeRecurringEntries(input)
        val decoded = PreferenceJsonCodec.decodeRecurringEntries(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun shiftTimePresets_defaultEmptyAndRoundTripViaJson() {
        assertTrue(ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS.isEmpty())
        assertTrue(PreferenceJsonCodec.decodeShiftTimePresets(null).isEmpty())
        assertTrue(PreferenceJsonCodec.decodeShiftTimePresets("[]").isEmpty())

        val input = listOf(
            ShiftTimePreset(
                id = "p_custom",
                label = "9 AM – 5 PM",
                startHour24 = 9,
                startMinute = 0,
                endHour24 = 17,
                endMinute = 0
            )
        )
        val encoded = PreferenceJsonCodec.encodeShiftTimePresets(input)
        val decoded = PreferenceJsonCodec.decodeShiftTimePresets(encoded)
        assertEquals(input, decoded)
    }

    @Test
    fun legacyDelimitedStrings_stillDecodeCleanly() {
        val budgets = PreferenceJsonCodec.decodeCategoryBudgets("Food:1200.0;Transport:500.0")
        assertEquals(2, budgets.size)
        assertEquals(1200.0, budgets[0].monthlyLimit, 0.001)

        val cats = PreferenceJsonCodec.decodeManagedCategories("Food|1;Bills|0")
        assertEquals(listOf(ManagedCategorySetting("Food", true), ManagedCategorySetting("Bills", false)), cats)

        val recurring = PreferenceJsonCodec.decodeRecurringEntries("r1|Wi-Fi|199.0|Monthly|0|1")
        assertEquals(1, recurring.size)
        assertEquals("Wi-Fi", recurring[0].title)
        assertEquals(false, recurring[0].isIncome)
    }
}
