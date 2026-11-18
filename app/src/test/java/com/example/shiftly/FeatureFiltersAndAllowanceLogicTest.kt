package com.example.shiftly

import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.splitbill.SplitBillViewModel
import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Test

class FeatureFiltersAndAllowanceLogicTest {

    @Test
    fun filterExpensesByMonth_filtersExactMonthBoundariesAndSupportsAllMonths() {
        val may1 = LocalDate.of(2026, 5, 1)
        val may31 = LocalDate.of(2026, 5, 31)
        val apr30 = LocalDate.of(2026, 4, 30)
        val jun1 = LocalDate.of(2026, 6, 1)

        val items = listOf(
            ExpenseEntity(id = 1L, title = "Apr End", amount = 50.0, category = "Food", dateEpochDay = apr30.toEpochDay()),
            ExpenseEntity(id = 2L, title = "May Start", amount = 80.0, category = "Food", dateEpochDay = may1.toEpochDay()),
            ExpenseEntity(id = 3L, title = "May End", amount = 120.0, category = "Bills", dateEpochDay = may31.toEpochDay()),
            ExpenseEntity(id = 4L, title = "Jun Start", amount = 90.0, category = "Food", dateEpochDay = jun1.toEpochDay())
        )

        val mayFiltered = ExpenseViewModel.filterExpensesByMonth(items, YearMonth.of(2026, 5))
        assertEquals(listOf(2L, 3L), mayFiltered.map { it.id })

        val allFiltered = ExpenseViewModel.filterExpensesByMonth(items, null)
        assertEquals(4, allFiltered.size)
    }

    @Test
    fun filterBillsByMonth_filtersExactMonthBoundariesAndSupportsAllMonths() {
        val may15 = LocalDate.of(2026, 5, 15)
        val apr15 = LocalDate.of(2026, 4, 15)

        val bills = listOf(
            SplitBillWithParticipants(
                bill = SplitBillEntity(1L, "Apr Dinner", 400.0, 2, 200.0, apr15.toEpochDay()),
                participants = emptyList()
            ),
            SplitBillWithParticipants(
                bill = SplitBillEntity(2L, "May Lunch", 600.0, 3, 200.0, may15.toEpochDay()),
                participants = emptyList()
            )
        )

        val mayBills = SplitBillViewModel.filterBillsByMonth(bills, YearMonth.of(2026, 5))
        assertEquals(listOf(2L), mayBills.map { it.bill.id })

        val allBills = SplitBillViewModel.filterBillsByMonth(bills, null)
        assertEquals(2, allBills.size)
    }

    @Test
    fun allowanceTotals_excludeAutoCreditedCutoffSalaryFromCombinedIncome() {
        val shifts = listOf(
            ShiftEntity(
                id = 1L,
                dateEpochDay = LocalDate.of(2026, 5, 10).toEpochDay(),
                timeInMillis = 0L,
                timeOutMillis = 0L,
                durationMinutes = 480,
                lockedHourlyRate = 100.0,
                totalEarned = 800.0
            )
        )
        val allowances = listOf(
            AllowanceEntryEntity(
                id = 1L,
                title = "Salary Payout (1st Cutoff)",
                amount = 800.0,
                source = AllowanceSource.SALARY,
                dateEpochDay = LocalDate.of(2026, 5, 15).toEpochDay(),
                cutoffKey = "SEMI_2026-05-15"
            ),
            AllowanceEntryEntity(
                id = 2L,
                title = "Weekend Sales",
                amount = 500.0,
                source = AllowanceSource.BUSINESS,
                dateEpochDay = LocalDate.of(2026, 5, 16).toEpochDay(),
                cutoffKey = null
            )
        )

        val manualAllowances = allowances.filter { it.cutoffKey.isNullOrBlank() }
        val combinedIncome = shifts.sumOf { it.totalEarned } + manualAllowances.sumOf { it.amount }
        val allowanceTabTotal = allowances.sumOf { it.amount }

        assertEquals(1300.0, combinedIncome, 0.001)
        assertEquals(1300.0, allowanceTabTotal, 0.001)
    }
}
