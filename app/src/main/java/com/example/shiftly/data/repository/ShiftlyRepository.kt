package com.example.shiftly.data.repository

import android.content.Context
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlyDatabase
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.local.UserPreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first

class ShiftlyRepository(context: Context) {
    private val db = ShiftlyDatabase.getInstance(context)
    private val shiftDao = db.shiftDao()
    private val expenseDao = db.expenseDao()
    private val splitBillDao = db.splitBillDao()
    private val prefs = UserPreferencesManager(context)

    // Preferences & Active Timer
    val defaultHourlyRate: Flow<Double> = prefs.defaultHourlyRate
    val currencySymbol: Flow<String> = prefs.currencySymbol
    val activeTimerState: Flow<ActiveTimerState> = prefs.activeTimerState

    suspend fun updateDefaultHourlyRate(rate: Double) = prefs.setDefaultHourlyRate(rate)
    suspend fun updateCurrencySymbol(symbol: String) = prefs.setCurrencySymbol(symbol)
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
    ): Long {
        val billId = splitBillDao.insertBill(bill)
        val linkedParticipants = participants.map { it.copy(billId = billId) }
        splitBillDao.insertParticipants(linkedParticipants)
        return billId
    }

    suspend fun toggleParticipantPaid(participantId: Long, hasPaid: Boolean) =
        splitBillDao.updateParticipantPaidStatus(participantId, hasPaid)

    suspend fun logSplitShareAsExpense(bill: SplitBillEntity) {
        if (bill.isLoggedToExpense) return
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

    suspend fun deleteSplitBill(bill: SplitBillEntity) = splitBillDao.deleteBill(bill)

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