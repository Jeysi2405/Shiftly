package com.example.shiftly

import com.example.shiftly.data.local.InstallmentLoan
import com.example.shiftly.data.local.InstallmentLoanEntity
import com.example.shiftly.data.local.InstallmentLoanWithPayments
import com.example.shiftly.data.local.LoanDueStatus
import com.example.shiftly.data.local.ShiftlyDatabase
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.contractEndDate
import com.example.shiftly.data.local.dueStatus
import com.example.shiftly.data.local.finalPrice
import com.example.shiftly.data.local.interestPercent
import com.example.shiftly.data.local.isCompleted
import com.example.shiftly.data.local.needsDueAttention
import com.example.shiftly.data.local.nextDueDate
import com.example.shiftly.data.local.normalizedOrNull
import com.example.shiftly.data.local.progressFraction
import com.example.shiftly.data.local.remainingBalance
import com.example.shiftly.data.local.remainingMonths
import com.example.shiftly.data.local.toInstallmentLoanEntity
import com.example.shiftly.data.local.toUtcMillis
import com.example.shiftly.data.local.totalInterest
import com.example.shiftly.data.local.totalPaid
import com.example.shiftly.ui.screens.ToolsViewModel
import java.time.LocalDate
import java.time.ZoneOffset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InstallmentLoanTest {

    private fun sampleLoan(
        cashPrice: Double = 30000.0,
        downpayment: Double = 5000.0,
        installmentPerMonth: Double = 2500.0,
        termsInMonths: Int = 12,
        paidMonths: Int = 3,
        startDate: LocalDate = LocalDate.of(2026, 1, 15),
        firstDueDate: LocalDate = LocalDate.of(2026, 2, 15),
        monthlyDueDay: Int = firstDueDate.dayOfMonth
    ) = InstallmentLoanEntity(
        id = 1L,
        gadgetName = "Laptop",
        cashPrice = cashPrice,
        downpayment = downpayment,
        installmentPerMonth = installmentPerMonth,
        termsInMonths = termsInMonths,
        paidMonths = paidMonths,
        startDateMillis = startDate.toUtcMillis(),
        monthlyDueDay = monthlyDueDay,
        firstDueDateMillis = firstDueDate.toUtcMillis()
    )

    @Test
    fun automaticCalculations_computeFinalPriceInterestPaidRemainingAndProgress() {
        val loan = sampleLoan()
        // Final price = 5000 + (2500 * 12) = 35000
        assertEquals(35000.0, loan.finalPrice, 0.001)
        // Interest = 35000 - 30000 = 5000 (16.67% of 30000)
        assertEquals(5000.0, loan.totalInterest, 0.001)
        assertEquals(16.666, loan.interestPercent ?: 0.0, 0.01)
        // Total paid = 5000 + (2500 * 3) = 12500
        assertEquals(12500.0, loan.totalPaid, 0.001)
        // Remaining = 35000 - 12500 = 22500, 9 mos left, 3/12 progress
        assertEquals(22500.0, loan.remainingBalance, 0.001)
        assertEquals(9, loan.remainingMonths)
        assertEquals(0.25f, loan.progressFraction, 0.0001f)
        assertFalse(loan.isCompleted)

        val zeroCashLoan = sampleLoan(cashPrice = 0.0)
        assertNull(zeroCashLoan.interestPercent)
    }

    @Test
    fun dueDateAndContractEndDate_clampMonthEndDaysAcrossFebruaryAndLeapYears() {
        val nonLeap = sampleLoan(
            termsInMonths = 3,
            paidMonths = 1,
            startDate = LocalDate.of(2025, 1, 31),
            firstDueDate = LocalDate.of(2025, 1, 31),
            monthlyDueDay = 31
        )
        assertEquals(LocalDate.of(2025, 2, 28), nonLeap.nextDueDate())
        assertEquals(LocalDate.of(2025, 3, 31), nonLeap.contractEndDate())

        val leap = sampleLoan(
            termsInMonths = 2,
            paidMonths = 1,
            startDate = LocalDate.of(2024, 1, 31),
            firstDueDate = LocalDate.of(2024, 1, 31),
            monthlyDueDay = 31
        )
        assertEquals(LocalDate.of(2024, 2, 29), leap.nextDueDate())
    }

    @Test
    fun dueStatus_detectsOverdueDueTodayDueSoonUpcomingAndCompleted() {
        val loan = sampleLoan(
            paidMonths = 0,
            firstDueDate = LocalDate.of(2026, 5, 15)
        )

        assertEquals(LoanDueStatus.UPCOMING, loan.dueStatus(LocalDate.of(2026, 5, 1)))
        assertFalse(loan.needsDueAttention(LocalDate.of(2026, 5, 1)))

        assertEquals(LoanDueStatus.DUE_SOON, loan.dueStatus(LocalDate.of(2026, 5, 8)))
        assertTrue(loan.needsDueAttention(LocalDate.of(2026, 5, 8)))

        assertEquals(LoanDueStatus.DUE_TODAY, loan.dueStatus(LocalDate.of(2026, 5, 15)))
        assertTrue(loan.needsDueAttention(LocalDate.of(2026, 5, 15)))

        assertEquals(LoanDueStatus.OVERDUE, loan.dueStatus(LocalDate.of(2026, 5, 16)))
        assertTrue(loan.needsDueAttention(LocalDate.of(2026, 5, 16)))

        val completed = loan.copy(paidMonths = loan.termsInMonths)
        assertTrue(completed.isCompleted)
        assertEquals(LoanDueStatus.COMPLETED, completed.dueStatus(LocalDate.of(2027, 1, 1)))
        assertFalse(completed.needsDueAttention(LocalDate.of(2027, 1, 1)))
    }

    @Test
    fun normalizedOrNull_validatesAndClampsFields() {
        val invalidBlank = sampleLoan().copy(gadgetName = "   ")
        assertNull(invalidBlank.normalizedOrNull())

        val invalidInstallment = sampleLoan().copy(installmentPerMonth = 0.0)
        assertNull(invalidInstallment.normalizedOrNull())

        val invalidTerms = sampleLoan().copy(termsInMonths = 0)
        assertNull(invalidTerms.normalizedOrNull())

        val valid = sampleLoan(
            cashPrice = -100.0,
            downpayment = 500.129,
            installmentPerMonth = 1234.567,
            termsInMonths = 12,
            paidMonths = 25,
            monthlyDueDay = 40
        ).copy(gadgetName = "  Phone  ").normalizedOrNull()

        assertNotNull(valid)
        assertEquals("Phone", valid?.gadgetName)
        assertEquals(0.0, valid?.cashPrice ?: -1.0, 0.001)
        assertEquals(500.13, valid?.downpayment ?: 0.0, 0.001)
        assertEquals(1234.57, valid?.installmentPerMonth ?: 0.0, 0.001)
        assertEquals(12, valid?.paidMonths)
        assertEquals(31, valid?.monthlyDueDay)
    }

    @Test
    fun legacyLoanConversion_mapsSemiMonthlyAndMonthlyContractsAccurately() {
        val startUtc = LocalDate.of(2026, 3, 10).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val legacy = InstallmentLoan(
            id = "legacy-1",
            itemName = "Tablet",
            lenderOrStore = "SPayLater",
            installmentAmount = 1500.0,
            totalInstallments = 6,
            paidInstallments = 2,
            downPayment = 1000.0,
            frequency = InstallmentLoan.FREQ_SEMI_MONTHLY,
            dueDayLabel = "15th & 30th",
            startDateMillis = startUtc,
            autoLogExpense = true
        )

        val converted = legacy.toInstallmentLoanEntity(ZoneOffset.UTC)
        assertEquals("Tablet (SPayLater)", converted.gadgetName)
        assertEquals(3, converted.termsInMonths)
        assertEquals(1, converted.paidMonths)
        assertEquals(3000.0, converted.installmentPerMonth, 0.001)
        assertEquals(1000.0, converted.downpayment, 0.001)
        assertEquals(15, converted.monthlyDueDay)
    }

    @Test
    fun toolsUiState_countsOverdueAndDueSoonLoans() {
        val overdue = InstallmentLoanWithPayments(
            loan = sampleLoan(paidMonths = 0, firstDueDate = LocalDate.of(2026, 5, 10)),
            payments = emptyList()
        )
        val dueSoon = InstallmentLoanWithPayments(
            loan = sampleLoan(paidMonths = 0, firstDueDate = LocalDate.of(2026, 5, 18)).copy(id = 2L),
            payments = emptyList()
        )
        val uiState = ToolsViewModel.computeToolsUiState(
            currencySymbol = "₱",
            monthlyBudget = 0.0,
            settings = ShiftlySettingsSnapshot(),
            shifts = emptyList(),
            expenses = emptyList(),
            splitBills = emptyList(),
            savingsGoals = emptyList(),
            recurringBills = emptyList(),
            allowanceEntries = emptyList(),
            installmentLoans = listOf(overdue, dueSoon),
            today = LocalDate.of(2026, 5, 15)
        )

        assertEquals(2, uiState.activeInstallmentLoanCount)
        assertEquals(1, uiState.overdueLoanCount)
        assertEquals(1, uiState.dueSoonLoanCount)
        assertTrue(uiState.installmentLoanSummaryText.contains("left"))
    }

    @Test
    fun migration7To8_metadataIsCorrect() {
        assertEquals(7, ShiftlyDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, ShiftlyDatabase.MIGRATION_7_8.endVersion)
    }
}
