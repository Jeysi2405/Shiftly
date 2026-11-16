package com.example.shiftly.data.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlyDatabase
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.local.UserPreferencesManager
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class ShiftlyRepository(private val context: Context) {
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
    val settingsSnapshot: Flow<ShiftlySettingsSnapshot> = prefs.settingsSnapshot

    suspend fun updateDefaultHourlyRate(rate: Double) = prefs.setDefaultHourlyRate(rate)
    suspend fun updateCurrencySymbol(symbol: String) = prefs.setCurrencySymbol(symbol)
    suspend fun setOnboardingCompleted(completed: Boolean) = prefs.setOnboardingCompleted(completed)
    suspend fun updateUserName(name: String) = prefs.setUserName(name)
    suspend fun updateMonthlyBudget(budget: Double) = prefs.setMonthlyBudget(budget)
    suspend fun updateSavingsBuffer(buffer: Double) = prefs.setSavingsBuffer(buffer)
    suspend fun updatePayCycleSettings(schedule: String, paydayReminder: Boolean, shiftReminder: Boolean) =
        prefs.setPayCycleSettings(schedule, paydayReminder, shiftReminder)
    suspend fun updateNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)
    suspend fun updateShiftRules(ndStartHour: Int, ndMultiplier: Double, otMultiplier: Double) =
        prefs.setShiftRules(ndStartHour, ndMultiplier, otMultiplier)
    suspend fun updateSavingsGoal(title: String, target: Double) = prefs.setSavingsGoal(title, target)
    suspend fun updateCategoryBudgets(items: List<CategoryBudgetSetting>) = prefs.setCategoryBudgets(items)
    suspend fun updateManagedCategories(items: List<ManagedCategorySetting>) = prefs.setManagedCategories(items)
    suspend fun updateRecurringEntries(items: List<RecurringEntrySetting>) = prefs.setRecurringEntries(items)
    suspend fun updateAppLock(enabled: Boolean, method: String, pin: String) =
        prefs.setAppLock(enabled, method, pin)

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
        val name = userName.first()

        val root = JSONObject().apply {
            put("app", "Shiftly")
            put("version", 2)
            put("userName", name)
            put("currencySymbol", currency)
            put("defaultHourlyRate", rate)
            put(
                "shifts",
                JSONArray().apply {
                    shifts.forEach { s ->
                        put(
                            JSONObject().apply {
                                put("dateEpochDay", s.dateEpochDay)
                                put("timeInMillis", s.timeInMillis)
                                put("timeOutMillis", s.timeOutMillis)
                                put("durationMinutes", s.durationMinutes)
                                put("lockedHourlyRate", s.lockedHourlyRate)
                                put("totalEarned", s.totalEarned)
                                put("overtimeNote", s.overtimeNote)
                                put("dayType", s.dayType)
                            }
                        )
                    }
                }
            )
            put(
                "expenses",
                JSONArray().apply {
                    expenses.forEach { e ->
                        put(
                            JSONObject().apply {
                                put("title", e.title)
                                put("amount", e.amount)
                                put("category", e.category)
                                put("dateEpochDay", e.dateEpochDay)
                            }
                        )
                    }
                }
            )
            put("splitBillCount", bills.size)
        }
        return root.toString(2)
    }

    suspend fun restoreFromBackupJson(rawJson: String): Result<String> = runCatching {
        val root = JSONObject(rawJson)
        val restoredName = root.optString("userName", "").trim()
        val restoredCurrency = root.optString("currencySymbol", "").trim()
        val restoredRate = root.optDouble("defaultHourlyRate", Double.NaN)
        val shiftsArray = root.optJSONArray("shifts") ?: JSONArray()
        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()

        db.withTransaction {
            if (restoredName.isNotEmpty()) prefs.setUserName(restoredName)
            if (restoredCurrency.isNotEmpty()) prefs.setCurrencySymbol(restoredCurrency)
            if (!restoredRate.isNaN() && restoredRate > 0.0) prefs.setDefaultHourlyRate(restoredRate)

            for (i in 0 until shiftsArray.length()) {
                val obj = shiftsArray.getJSONObject(i)
                shiftDao.insertShift(
                    ShiftEntity(
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        timeInMillis = obj.getLong("timeInMillis"),
                        timeOutMillis = obj.getLong("timeOutMillis"),
                        durationMinutes = obj.getInt("durationMinutes"),
                        lockedHourlyRate = obj.getDouble("lockedHourlyRate"),
                        totalEarned = obj.getDouble("totalEarned"),
                        overtimeNote = obj.optString("overtimeNote", ""),
                        dayType = obj.optString("dayType", "ORDINARY")
                    )
                )
            }
            for (i in 0 until expensesArray.length()) {
                val obj = expensesArray.getJSONObject(i)
                expenseDao.insertExpense(
                    ExpenseEntity(
                        title = obj.getString("title"),
                        amount = obj.getDouble("amount"),
                        category = obj.optString("category", "Other"),
                        dateEpochDay = obj.getLong("dateEpochDay")
                    )
                )
            }
        }
        "Restored ${shiftsArray.length()} shift(s) and ${expensesArray.length()} expense(s)"
    }

    suspend fun generateCsvExportFile(): File = withContext(Dispatchers.IO) {
        val shifts = allShifts.first()
        val expenses = allExpenses.first()
        val currency = currencySymbol.first()
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val csvFile = File(exportDir, "shiftly_export.csv")

        val csvContent = buildString {
            appendLine("Type,Date,Title_Or_Note,Category_Or_DayType,Duration_Min,Amount ($currency)")
            shifts.forEach { s ->
                val date = LocalDate.ofEpochDay(s.dateEpochDay)
                val note = s.overtimeNote.replace(",", " ")
                appendLine("Shift,$date,$note,${s.dayType},${s.durationMinutes},${"%.2f".format(s.totalEarned)}")
            }
            expenses.forEach { e ->
                val date = LocalDate.ofEpochDay(e.dateEpochDay)
                val title = e.title.replace(",", " ")
                appendLine("Expense,$date,$title,${e.category},0,-${"%.2f".format(e.amount)}")
            }
        }
        csvFile.writeText(csvContent)
        csvFile
    }

    suspend fun generatePdfSummaryFile(): File = withContext(Dispatchers.IO) {
        val shifts = allShifts.first()
        val expenses = allExpenses.first()
        val currency = currencySymbol.first()
        val name = userName.first()
        val totalIncome = shifts.sumOf { it.totalEarned }
        val totalExpense = expenses.sumOf { it.amount }
        val totalHours = shifts.sumOf { it.durationMinutes } / 60.0

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "shiftly_statement.pdf")

        val doc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas = page.canvas

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }

        var y = 52f
        canvas.drawText("Shiftly Statement — $name", 40f, y, titlePaint)
        y += 24f
        canvas.drawText(
            "Shifts: ${shifts.size} (${"%.1f".format(totalHours)}h)   |   Earnings: $currency${"%,.2f".format(totalIncome)}   |   Expenses: $currency${"%,.2f".format(totalExpense)}",
            40f,
            y,
            bodyPaint
        )
        y += 28f
        canvas.drawText("Recent Shifts", 40f, y, headerPaint)
        y += 18f
        shifts.take(12).forEach { s ->
            val date = LocalDate.ofEpochDay(s.dateEpochDay)
            canvas.drawText(
                "$date  •  ${s.durationMinutes}m  •  +$currency${"%,.2f".format(s.totalEarned)}  ${s.overtimeNote}",
                40f,
                y,
                bodyPaint
            )
            y += 16f
        }
        y += 16f
        canvas.drawText("Recent Expenses", 40f, y, headerPaint)
        y += 18f
        expenses.take(14).forEach { e ->
            val date = LocalDate.ofEpochDay(e.dateEpochDay)
            canvas.drawText(
                "$date  •  ${e.category}  •  -$currency${"%,.2f".format(e.amount)}  ${e.title}",
                40f,
                y,
                bodyPaint
            )
            y += 16f
        }

        doc.finishPage(page)
        FileOutputStream(pdfFile).use { out ->
            doc.writeTo(out)
        }
        doc.close()
        pdfFile
    }
}
