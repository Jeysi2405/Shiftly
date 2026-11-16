package com.example.shiftly.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlyDatabase
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.local.UserPreferencesManager
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShiftlyRepository(context: Context) {
    private val db = ShiftlyDatabase.getInstance(context)
    private val shiftDao = db.shiftDao()
    private val expenseDao = db.expenseDao()
    private val splitBillDao = db.splitBillDao()
    private val prefs = UserPreferencesManager(context.applicationContext)

    // Preferences & Active Timer
    val defaultHourlyRate: Flow<Double> = prefs.defaultHourlyRate
    val currencySymbol: Flow<String> = prefs.currencySymbol
    val activeTimerState: Flow<ActiveTimerState> = prefs.activeTimerState
    val hasCompletedOnboarding: Flow<Boolean> = prefs.hasCompletedOnboarding
    val userName: Flow<String> = prefs.userName
    val monthlyBudget: Flow<Double> = prefs.monthlyBudget
    val savingsBuffer: Flow<Double> = prefs.savingsBuffer

    suspend fun updateDefaultHourlyRate(rate: Double) = prefs.setDefaultHourlyRate(rate)
    suspend fun updateCurrencySymbol(symbol: String) = prefs.setCurrencySymbol(symbol)
    suspend fun setOnboardingCompleted(completed: Boolean) = prefs.setOnboardingCompleted(completed)
    suspend fun updateUserName(name: String) = prefs.setUserName(name)
    suspend fun updateMonthlyBudget(budget: Double) = prefs.setMonthlyBudget(budget)
    suspend fun updateSavingsBuffer(buffer: Double) = prefs.setSavingsBuffer(buffer)
    suspend fun startShiftTimer(startMillis: Long, dateEpochDay: Long, lockedRate: Double) =
        prefs.startActiveTimer(startMillis, dateEpochDay, lockedRate)
    suspend fun stopShiftTimer() = prefs.clearActiveTimer()

    // Shifts
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShifts()
    val totalShiftCount: Flow<Int> = shiftDao.getTotalShiftCount()
    val totalMinutesWorked: Flow<Int?> = shiftDao.getTotalMinutesWorked()
    val totalLifetimeEarnings: Flow<Double?> = shiftDao.getTotalLifetimeEarnings()

    fun getShiftsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<ShiftEntity>> =
        shiftDao.getShiftsBetween(startEpochDay, endEpochDay)

    suspend fun insertShift(shift: ShiftEntity): Long = shiftDao.insertShift(shift)
    suspend fun updateShift(shift: ShiftEntity) = shiftDao.updateShift(shift)
    suspend fun deleteShift(shift: ShiftEntity) = shiftDao.deleteShift(shift)

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalExpenseAmount: Flow<Double?> = expenseDao.getTotalExpenseAmount()

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = expenseDao.deleteExpense(expense)

    // Split Bills
    val allSplitBills: Flow<List<SplitBillWithParticipants>> = splitBillDao.getAllBillsWithParticipants()

    suspend fun createSplitBill(
        bill: SplitBillEntity,
        participants: List<SplitParticipantEntity>
    ): Long = db.withTransaction {
        val billId = splitBillDao.insertBill(bill)
        val linkedParticipants = participants.map { it.copy(billId = billId) }
        splitBillDao.insertParticipants(linkedParticipants)
        billId
    }

    suspend fun toggleParticipantPaid(participantId: Long, hasPaid: Boolean) =
        splitBillDao.updateParticipantPaidStatus(participantId, hasPaid)

    suspend fun logSplitShareAsExpense(bill: SplitBillEntity) {
        if (bill.isLoggedToExpense) return
        db.withTransaction {
            expenseDao.insertExpense(
                ExpenseEntity(
                    title = "Split: ${bill.title}",
                    amount = bill.perPersonAmount,
                    category = "Split Bill",
                    dateEpochDay = bill.dateEpochDay,
                    linkedSplitBillId = bill.id
                )
            )
            splitBillDao.markBillLoggedToExpense(bill.id)
        }
    }

    suspend fun deleteSplitBill(bill: SplitBillEntity) = splitBillDao.deleteBill(bill)

    /**
     * Populates realistic student shift, expense, and split-bill data when the database
     * is empty so first-time users can explore the dashboard immediately.
     */
    suspend fun seedSampleStudentData() {
        val existingShifts = allShifts.first()
        val existingExpenses = allExpenses.first()
        if (existingShifts.isNotEmpty() || existingExpenses.isNotEmpty()) return

        val zone = ZoneId.systemDefault()
        val today = LocalDate.now()
        val rate = defaultHourlyRate.first().takeIf { it > 0.0 } ?: 96.38

        fun millisAt(date: LocalDate, hour: Int, minute: Int): Long =
            date.atTime(LocalTime.of(hour, minute)).atZone(zone).toInstant().toEpochMilli()

        db.withTransaction {
            // Shifts across the week at ₱96.38/hr
            val sampleShifts = listOf(
                Triple(today, 14 to 0, 18 to 0) to "Campus Café Shift",
                Triple(today.minusDays(1), 13 to 0, 16 to 30) to "Library Desk Duty",
                Triple(today.minusDays(2), 15 to 0, 19 to 0) to "Campus Café Shift",
                Triple(today.minusDays(4), 10 to 0, 14 to 0) to "Student Center Desk"
            )
            sampleShifts.forEach { (timing, note) ->
                val (date, startHm, endHm) = timing
                val startMs = millisAt(date, startHm.first, startHm.second)
                val endMs = millisAt(date, endHm.first, endHm.second)
                val mins = ((endMs - startMs) / 60_000L).toInt()
                val earned = (mins / 60.0) * rate
                shiftDao.insertShift(
                    ShiftEntity(
                        dateEpochDay = date.toEpochDay(),
                        timeInMillis = startMs,
                        timeOutMillis = endMs,
                        durationMinutes = mins,
                        lockedHourlyRate = rate,
                        totalEarned = earned,
                        overtimeNote = note
                    )
                )
            }

            // Realistic student expenses (Food, Transport, School)
            val sampleExpenses = listOf(
                ExpenseEntity(
                    title = "Jeepney & LRT Fare",
                    amount = 58.0,
                    category = "Transport",
                    dateEpochDay = today.toEpochDay()
                ),
                ExpenseEntity(
                    title = "Tapsilog & Iced Coffee",
                    amount = 145.0,
                    category = "Food",
                    dateEpochDay = today.toEpochDay()
                ),
                ExpenseEntity(
                    title = "Thesis Binding & Prints",
                    amount = 180.0,
                    category = "Supplies",
                    dateEpochDay = today.toEpochDay()
                ),
                ExpenseEntity(
                    title = "Lab Manual Photocopy",
                    amount = 95.0,
                    category = "Supplies",
                    dateEpochDay = today.minusDays(1).toEpochDay()
                ),
                ExpenseEntity(
                    title = "Tricycle to Campus",
                    amount = 40.0,
                    category = "Transport",
                    dateEpochDay = today.minusDays(2).toEpochDay()
                )
            )
            sampleExpenses.forEach { expenseDao.insertExpense(it) }

            // Pending split bill owed by classmates
            val billId = splitBillDao.insertBill(
                SplitBillEntity(
                    title = "Group Lab Print & Snacks",
                    totalAmount = 1080.0,
                    personCount = 3,
                    perPersonAmount = 360.0,
                    dateEpochDay = today.minusDays(1).toEpochDay(),
                    isLoggedToExpense = true
                )
            )
            splitBillDao.insertParticipants(
                listOf(
                    SplitParticipantEntity(billId = billId, personLabel = "You", amountOwed = 360.0, hasPaid = true),
                    SplitParticipantEntity(billId = billId, personLabel = "Jules", amountOwed = 360.0, hasPaid = false),
                    SplitParticipantEntity(billId = billId, personLabel = "Kian", amountOwed = 360.0, hasPaid = false)
                )
            )
            expenseDao.insertExpense(
                ExpenseEntity(
                    title = "Split: Group Lab Print & Snacks",
                    amount = 360.0,
                    category = "Split Bill",
                    dateEpochDay = today.minusDays(1).toEpochDay(),
                    linkedSplitBillId = billId
                )
            )
        }
    }

    // Offline JSON Snapshot Export
    suspend fun generateOfflineBackupSummary(): String {
        val shifts = allShifts.first()
        val expenses = allExpenses.first()
        val bills = allSplitBills.first()
        val currency = currencySymbol.first()
        val rate = defaultHourlyRate.first()

        return buildString {
            appendLine("=== SHIFTLY OFFLINE BACKUP ===")
            appendLine("Default Rate: $currency${"%.2f".format(rate)}/hr")
            appendLine("Total Shifts: ${shifts.size}")
            appendLine("Total Expenses: ${expenses.size}")
            appendLine("Total Split Bills: ${bills.size}")
            appendLine("\n--- SHIFTS ---")
            shifts.forEach {
                appendLine("EpochDay=${it.dateEpochDay} | ${it.durationMinutes}m | Rate=$currency${it.lockedHourlyRate} | Earned=$currency${"%.2f".format(it.totalEarned)} | Note=${it.overtimeNote}")
            }
            appendLine("\n--- EXPENSES ---")
            expenses.forEach {
                appendLine("EpochDay=${it.dateEpochDay} | ${it.title} [${it.category}] | $currency${"%.2f".format(it.amount)}")
            }
        }
    }
}
