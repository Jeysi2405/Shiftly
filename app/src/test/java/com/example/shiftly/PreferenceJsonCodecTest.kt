package com.example.shiftly

import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.InAppNotificationItem
import com.example.shiftly.data.local.InAppNotificationType
import com.example.shiftly.data.local.InstallmentLoan
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.PreferenceJsonCodec
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun inAppNotifications_roundTripSortsNewestFirstAndHandlesInvalidInput() {
        assertTrue(PreferenceJsonCodec.decodeInAppNotifications(null).isEmpty())
        assertTrue(PreferenceJsonCodec.decodeInAppNotifications("").isEmpty())
        assertTrue(PreferenceJsonCodec.decodeInAppNotifications("not-json").isEmpty())

        val older = InAppNotificationItem(
            id = "n1",
            eventKey = "bill_due_1",
            type = InAppNotificationType.BILL_DUE,
            title = "Wi-Fi Bill Due",
            message = "₱1,499 due tomorrow",
            timestampMillis = 1_700_000_000_000L,
            isRead = true,
            hasShownToast = true
        )
        val newer = InAppNotificationItem(
            id = "n2",
            eventKey = "payday_2025_05_15",
            type = InAppNotificationType.PAYDAY_CUTOFF,
            title = "Payday Credited",
            message = "Added ₱5,200 to your balance",
            timestampMillis = 1_700_005_000_000L,
            isRead = false,
            hasShownToast = false
        )

        val encoded = PreferenceJsonCodec.encodeInAppNotifications(listOf(older, newer))
        val decoded = PreferenceJsonCodec.decodeInAppNotifications(encoded)
        assertEquals(listOf(newer, older), decoded)
    }

    @Test
    fun inAppNotifications_unknownTypeFallsBackGracefully() {
        val raw = """
            [
              {
                "id": "n_unknown",
                "eventKey": "k1",
                "type": "FUTURE_TYPE",
                "title": "Heads Up",
                "message": "Check schedule",
                "timestampMillis": 1700000000000,
                "isRead": false,
                "hasShownToast": true
              },
              {
                "id": "",
                "title": "Should Skip"
              }
            ]
        """.trimIndent()

        val decoded = PreferenceJsonCodec.decodeInAppNotifications(raw)
        assertEquals(1, decoded.size)
        assertEquals(InAppNotificationType.BILL_DUE, decoded[0].type)
        assertEquals("Heads Up", decoded[0].title)
        assertEquals(true, decoded[0].hasShownToast)
    }

    @Test
    fun stringList_roundTripAndHandlesInvalidInput() {
        assertTrue(PreferenceJsonCodec.decodeStringList(null).isEmpty())
        assertTrue(PreferenceJsonCodec.decodeStringList("malformed").isEmpty())

        val keys = listOf("bill_due_10", "shift_2025_05_20", "split_99")
        val encoded = PreferenceJsonCodec.encodeStringList(keys)
        val decoded = PreferenceJsonCodec.decodeStringList(encoded)
        assertEquals(keys, decoded)
    }

    @Test
    fun installmentLoans_roundTripViaJsonAndHandlesMalformed() {
        assertTrue(PreferenceJsonCodec.decodeInstallmentLoans(null).isEmpty())
        assertTrue(PreferenceJsonCodec.decodeInstallmentLoans("bad-json").isEmpty())

        val loans = listOf(
            InstallmentLoan(
                id = "loan-1",
                itemName = "MacBook Air",
                lenderOrStore = "Home Credit",
                installmentAmount = 2500.0,
                totalInstallments = 12,
                paidInstallments = 3,
                downPayment = 5000.0,
                frequency = InstallmentLoan.FREQ_MONTHLY,
                dueDayLabel = "Every 15th",
                startDateMillis = 1_760_000_000_000L,
                autoLogExpense = true
            ),
            InstallmentLoan(
                id = "loan-2",
                itemName = "Phone Plan",
                lenderOrStore = "SPayLater",
                installmentAmount = 950.0,
                totalInstallments = 6,
                paidInstallments = 6,
                downPayment = 0.0,
                frequency = InstallmentLoan.FREQ_SEMI_MONTHLY,
                dueDayLabel = "15th & 30th",
                startDateMillis = 1_750_000_000_000L,
                autoLogExpense = false
            )
        )

        val encoded = PreferenceJsonCodec.encodeInstallmentLoans(loans)
        val decoded = PreferenceJsonCodec.decodeInstallmentLoans(encoded)
        assertEquals(loans, decoded)
    }

    @Test
    fun installmentLoan_computedHelpersAndFrequenciesWorkAccurately() {
        val ref = LocalDate.of(2026, 5, 1)

        val monthly = InstallmentLoan(
            itemName = "Laptop",
            installmentAmount = 2500.0,
            totalInstallments = 12,
            paidInstallments = 3,
            downPayment = 5000.0,
            frequency = InstallmentLoan.FREQ_MONTHLY
        )
        assertEquals(30000.0, monthly.totalFinancedAmount, 0.001)
        assertEquals(35000.0, monthly.totalContractAmount, 0.001)
        assertEquals(12500.0, monthly.paidAmount, 0.001)
        assertEquals(9, monthly.remainingInstallments)
        assertEquals(22500.0, monthly.remainingBalance, 0.001)
        assertEquals(0.25f, monthly.progressFraction, 0.001f)
        assertFalse(monthly.isCompleted)
        assertEquals("mo", monthly.frequencyUnitSuffix())
        assertEquals("9 mos left", monthly.remainingTimeLabel())
        assertEquals("Ends Feb 2027", monthly.estimatedEndDateLabel(ref))

        val semiMonthly = monthly.copy(frequency = InstallmentLoan.FREQ_SEMI_MONTHLY, totalInstallments = 5, paidInstallments = 2)
        assertEquals("cutoff", semiMonthly.frequencyUnitSuffix())
        assertEquals("3 cutoffs left", semiMonthly.remainingTimeLabel())
        assertEquals("Ends Jun 2026", semiMonthly.estimatedEndDateLabel(ref))

        val weekly = monthly.copy(frequency = InstallmentLoan.FREQ_WEEKLY, totalInstallments = 6, paidInstallments = 5)
        assertEquals("wk", weekly.frequencyUnitSuffix())
        assertEquals("1 wk left", weekly.remainingTimeLabel())

        val everyday = monthly.copy(frequency = InstallmentLoan.FREQ_EVERYDAY, totalInstallments = 10, paidInstallments = 8)
        assertEquals("day", everyday.frequencyUnitSuffix())
        assertEquals("2 days left", everyday.remainingTimeLabel())

        val completed = monthly.copy(paidInstallments = 12)
        assertTrue(completed.isCompleted)
        assertEquals(0.0, completed.remainingBalance, 0.001)
        assertEquals("Completed", completed.remainingTimeLabel())
        assertEquals("Completed", completed.estimatedEndDateLabel(ref))
    }
}
