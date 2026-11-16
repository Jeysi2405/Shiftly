package com.example.shiftly.data.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.example.shiftly.data.backup.BackupCodec
import com.example.shiftly.data.backup.BackupExpenseRecord
import com.example.shiftly.data.backup.BackupPayload
import com.example.shiftly.data.backup.BackupSplitBill
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlyDatabase
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.local.UserPreferencesManager
import com.example.shiftly.utils.PinSecurity
import com.example.shiftly.utils.RecurringEngine
import com.example.shiftly.utils.SplitBillMath
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ShiftlyRepository(private val context: Context) {
    private val db = ShiftlyDatabase.getInstance(context)
    private val shiftDao = db.shiftDao()
    private val expenseDao = db.expenseDao()
    private val extraIncomeDao = db.extraIncomeDao()
    private val splitBillDao = db.splitBillDao()
    private val prefs = UserPreferencesManager(context.applicationContext)
    private val recurringMutex = Mutex()

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

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

    suspend fun getLastNotifiedDays(): Pair<Long, Long> = prefs.getLastNotifiedDays()
    suspend fun markPaydayNotified(epochDay: Long) = prefs.markPaydayNotified(epochDay)
    suspend fun markShiftNotified(epochDay: Long) = prefs.markShiftNotified(epochDay)

    suspend fun changeCurrency(newSymbol: String, conversionRate: Double?) = withContext(Dispatchers.IO) {
        if (conversionRate != null && conversionRate > 0.0) {
            db.withTransaction {
                shiftDao.getAllShiftsOnce().forEach { s ->
                    shiftDao.updateShift(
                        s.copy(
                            lockedHourlyRate = round2(s.lockedHourlyRate * conversionRate),
                            totalEarned = round2(s.totalEarned * conversionRate)
                        )
                    )
                }
                expenseDao.getAllExpensesOnce().forEach { e ->
                    expenseDao.updateExpense(
                        e.copy(amount = round2(e.amount * conversionRate))
                    )
                }
                extraIncomeDao.getAllExtraIncomesOnce().forEach { inc ->
                    extraIncomeDao.updateExtraIncome(
                        inc.copy(amount = round2(inc.amount * conversionRate))
                    )
                }
                splitBillDao.getAllBillsWithParticipantsOnce().forEach { entry ->
                    val newTotal = round2(entry.bill.totalAmount * conversionRate)
                    val shares = SplitBillMath.distributeEqualShares(
                        totalAmount = newTotal,
                        personCount = entry.participants.size.coerceAtLeast(entry.bill.personCount)
                    )
                    val newPerPerson = shares.firstOrNull() ?: round2(entry.bill.perPersonAmount * conversionRate)
                    splitBillDao.updateBill(
                        entry.bill.copy(
                            totalAmount = newTotal,
                            perPersonAmount = newPerPerson
                        )
                    )
                    entry.participants.forEachIndexed { idx, p ->
                        val scaledOwed = shares.getOrElse(idx) { round2(p.amountOwed * conversionRate) }
                        splitBillDao.updateParticipant(p.copy(amountOwed = scaledOwed))
                    }
                }
            }
        }
        prefs.applyCurrencyConversion(newSymbol, conversionRate)
    }

    suspend fun updateAppLock(enabled: Boolean, method: String, newPin: String? = null) {
        val existingHash = settingsSnapshot.first().appLockPinHash
        val resolvedHash = when {
            !newPin.isNullOrBlank() && PinSecurity.isValidPinFormat(newPin) -> PinSecurity.hashPin(newPin)
            else -> existingHash
        }
        prefs.setAppLock(enabled = enabled, method = method, pinHash = resolvedHash)
    }

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

    // Extra Incomes (Recurring / Bonus income)
    val allExtraIncomes: Flow<List<ExtraIncomeEntity>> = extraIncomeDao.getAllExtraIncomes()
    val totalExtraIncomeAmount: Flow<Double?> = extraIncomeDao.getTotalExtraIncomeAmount()

    suspend fun insertExtraIncome(income: ExtraIncomeEntity): Long = extraIncomeDao.insertExtraIncome(income)
    suspend fun deleteExtraIncome(income: ExtraIncomeEntity) = extraIncomeDao.deleteExtraIncome(income)

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalExpenseAmount: Flow<Double?> = expenseDao.getTotalExpenseAmount()

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)
    suspend fun updateExpense(expense: ExpenseEntity) = expenseDao.updateExpense(expense)
    suspend fun deleteExpense(expense: ExpenseEntity) = db.withTransaction {
        expenseDao.deleteExpense(expense)
        expense.linkedSplitBillId?.let { billId ->
            splitBillDao.setBillLoggedToExpense(billId, false)
        }
    }

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
                    linkedSplitBillId = bill.id,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
            splitBillDao.setBillLoggedToExpense(bill.id, true)
        }
    }

    suspend fun deleteSplitBill(bill: SplitBillEntity, deleteLinkedExpense: Boolean = false) = db.withTransaction {
        if (deleteLinkedExpense) {
            expenseDao.deleteExpensesByLinkedSplitBillId(bill.id)
        } else {
            expenseDao.unlinkExpensesBySplitBillId(bill.id)
        }
        splitBillDao.deleteBill(bill)
    }

    /**
     * Evaluates recurring rules via [RecurringEngine] and atomically inserts due
     * income/expense entries, advancing each rule's `lastAppliedEpochDay` to prevent duplicates.
     */
    suspend fun processDueRecurringEntries(today: LocalDate = LocalDate.now()): Int = recurringMutex.withLock {
        val snapshot = settingsSnapshot.first()
        if (snapshot.recurringEntries.isEmpty()) return@withLock 0

        var postedCount = 0
        val updatedRules = mutableListOf<RecurringEntrySetting>()

        db.withTransaction {
            val nowMillis = System.currentTimeMillis()
            snapshot.recurringEntries.forEach { entry ->
                val dueOccurrences = RecurringEngine.computeDueOccurrences(entry, today)
                if (dueOccurrences.isEmpty()) {
                    updatedRules.add(entry)
                } else {
                    dueOccurrences.forEach { occ ->
                        if (entry.isIncome) {
                            extraIncomeDao.insertExtraIncome(
                                ExtraIncomeEntity(
                                    title = entry.title,
                                    amount = entry.amount,
                                    dateEpochDay = occ.dueEpochDay,
                                    sourceRuleId = entry.id,
                                    createdAtMillis = nowMillis
                                )
                            )
                        } else {
                            expenseDao.insertExpense(
                                ExpenseEntity(
                                    title = entry.title,
                                    amount = entry.amount,
                                    category = entry.category.ifBlank { "Other" },
                                    dateEpochDay = occ.dueEpochDay,
                                    createdAtMillis = nowMillis
                                )
                            )
                        }
                        postedCount++
                    }
                    val maxApplied = dueOccurrences.maxOf { it.dueEpochDay }
                    updatedRules.add(entry.copy(lastAppliedEpochDay = maxApplied))
                }
            }
        }

        if (postedCount > 0) {
            prefs.setRecurringEntries(updatedRules)
        }
        postedCount
    }

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

            val sampleExpenses = listOf(
                ExpenseEntity(
                    title = "Jeepney & LRT Fare",
                    amount = 58.0,
                    category = "Transport",
                    dateEpochDay = today.toEpochDay(),
                    createdAtMillis = millisAt(today, 8, 15)
                ),
                ExpenseEntity(
                    title = "Tapsilog & Iced Coffee",
                    amount = 145.0,
                    category = "Food",
                    dateEpochDay = today.toEpochDay(),
                    createdAtMillis = millisAt(today, 12, 30)
                ),
                ExpenseEntity(
                    title = "Thesis Binding & Prints",
                    amount = 180.0,
                    category = "Supplies",
                    dateEpochDay = today.toEpochDay(),
                    createdAtMillis = millisAt(today, 16, 45)
                ),
                ExpenseEntity(
                    title = "Lab Manual Photocopy",
                    amount = 95.0,
                    category = "Supplies",
                    dateEpochDay = today.minusDays(1).toEpochDay(),
                    createdAtMillis = millisAt(today.minusDays(1), 11, 20)
                ),
                ExpenseEntity(
                    title = "Tricycle to Campus",
                    amount = 40.0,
                    category = "Transport",
                    dateEpochDay = today.minusDays(2).toEpochDay(),
                    createdAtMillis = millisAt(today.minusDays(2), 7, 45)
                )
            )
            sampleExpenses.forEach { expenseDao.insertExpense(it) }

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
                    SplitParticipantEntity(
                        billId = billId,
                        personLabel = "You",
                        amountOwed = 360.0,
                        hasPaid = true,
                        isCurrentUser = true
                    ),
                    SplitParticipantEntity(
                        billId = billId,
                        personLabel = "Jules",
                        amountOwed = 360.0,
                        hasPaid = false,
                        isCurrentUser = false
                    ),
                    SplitParticipantEntity(
                        billId = billId,
                        personLabel = "Kian",
                        amountOwed = 360.0,
                        hasPaid = false,
                        isCurrentUser = false
                    )
                )
            )
            expenseDao.insertExpense(
                ExpenseEntity(
                    title = "Split: Group Lab Print & Snacks",
                    amount = 360.0,
                    category = "Split Bill",
                    dateEpochDay = today.minusDays(1).toEpochDay(),
                    linkedSplitBillId = billId,
                    createdAtMillis = millisAt(today.minusDays(1), 18, 10)
                )
            )
        }
    }

    private suspend fun buildCurrentBackupPayload(includeReceipts: Boolean): BackupPayload = withContext(Dispatchers.IO) {
        val shifts = shiftDao.getAllShiftsOnce()
        val expenses = expenseDao.getAllExpensesOnce()
        val extraIncomes = extraIncomeDao.getAllExtraIncomesOnce()
        val bills = splitBillDao.getAllBillsWithParticipantsOnce()
        val settings = settingsSnapshot.first()
        val currency = currencySymbol.first()
        val rate = defaultHourlyRate.first()
        val name = userName.first()
        val budget = monthlyBudget.first()
        val buffer = savingsBuffer.first()

        val receiptMap = mutableMapOf<String, ByteArray>()
        val backupExpenses = expenses.map { e ->
            val path = e.receiptImagePath
            val file = if (!path.isNullOrBlank()) File(path) else null
            val validName = if (includeReceipts && file != null && file.exists() && file.isFile) {
                runCatching {
                    receiptMap[file.name] = file.readBytes()
                    file.name
                }.getOrNull()
            } else {
                file?.name
            }
            BackupExpenseRecord(expense = e, receiptFileName = validName)
        }

        val backupBills = bills.map { entry ->
            BackupSplitBill(
                originalId = entry.bill.id,
                bill = entry.bill,
                participants = entry.participants
            )
        }

        BackupPayload(
            userName = name,
            currencySymbol = currency,
            defaultHourlyRate = rate,
            monthlyBudget = budget,
            savingsBuffer = buffer,
            settings = settings,
            shifts = shifts,
            expenses = backupExpenses,
            extraIncomes = extraIncomes,
            splitBills = backupBills,
            receiptFiles = receiptMap
        )
    }

    // Complete Offline JSON Snapshot Export
    suspend fun generateOfflineBackupSummary(): String = withContext(Dispatchers.IO) {
        BackupCodec.encodeJson(buildCurrentBackupPayload(includeReceipts = false))
    }

    // Complete Offline ZIP Backup (JSON + Split Bills + Settings + Receipt Photos)
    suspend fun generateBackupZipFile(): File = withContext(Dispatchers.IO) {
        val payload = buildCurrentBackupPayload(includeReceipts = true)
        val zipBytes = BackupCodec.encodeZipBytes(payload)
        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val stamp = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE)
        val zipFile = File(exportDir, "shiftly_backup_$stamp.zip")
        zipFile.writeBytes(zipBytes)
        zipFile
    }

    suspend fun restoreFromBackupJson(
        rawJson: String,
        mode: RestoreMode = RestoreMode.MERGE
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupCodec.decodeJson(rawJson)
            applyBackupPayload(payload, mode)
        }
    }

    suspend fun restoreFromBackupBytes(
        rawBytes: ByteArray,
        mode: RestoreMode = RestoreMode.MERGE
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = BackupCodec.decodeZipOrJsonBytes(rawBytes)
            applyBackupPayload(payload, mode)
        }
    }

    private suspend fun applyBackupPayload(payload: BackupPayload, mode: RestoreMode): String {
        val receiptsDir = File(context.filesDir, "receipts").apply { mkdirs() }
        var restoredShiftsCount = 0
        var restoredExpensesCount = 0
        var restoredExtraIncomesCount = 0
        var restoredBillsCount = 0
        var restoredReceiptsCount = 0

        db.withTransaction {
            if (mode == RestoreMode.REPLACE) {
                expenseDao.deleteAllExpenses()
                extraIncomeDao.deleteAllExtraIncomes()
                shiftDao.deleteAllShifts()
                splitBillDao.deleteAllSplitBills()
                receiptsDir.listFiles()?.forEach { it.delete() }
            }

            // Restore receipt files first so restored expenses point to valid paths
            val restoredReceiptPaths = mutableMapOf<String, String>()
            payload.receiptFiles.forEach { (fileName, bytes) ->
                val safeName = File(fileName).name
                if (safeName.isNotBlank() && bytes.isNotEmpty()) {
                    val target = File(receiptsDir, safeName)
                    target.writeBytes(bytes)
                    restoredReceiptPaths[safeName] = target.absolutePath
                    restoredReceiptsCount++
                }
            }

            val existingShifts = if (mode == RestoreMode.MERGE) {
                shiftDao.getAllShiftsOnce().map { BackupCodec.shiftSignature(it) }.toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.shifts.forEach { shift ->
                val sig = BackupCodec.shiftSignature(shift)
                if (mode == RestoreMode.REPLACE || existingShifts.add(sig)) {
                    shiftDao.insertShift(shift.copy(id = 0L))
                    restoredShiftsCount++
                }
            }

            val existingExtraIncomes = if (mode == RestoreMode.MERGE) {
                extraIncomeDao.getAllExtraIncomesOnce().map { BackupCodec.extraIncomeSignature(it) }.toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.extraIncomes.forEach { inc ->
                val sig = BackupCodec.extraIncomeSignature(inc)
                if (mode == RestoreMode.REPLACE || existingExtraIncomes.add(sig)) {
                    extraIncomeDao.insertExtraIncome(inc.copy(id = 0L))
                    restoredExtraIncomesCount++
                }
            }

            val existingBillsMap = if (mode == RestoreMode.MERGE) {
                splitBillDao.getAllBillsWithParticipantsOnce()
                    .associate { BackupCodec.splitBillSignature(it.bill) to it.bill.id }
                    .toMutableMap()
            } else {
                mutableMapOf()
            }

            val oldToNewBillId = mutableMapOf<Long, Long>()
            payload.splitBills.forEach { entry ->
                val sig = BackupCodec.splitBillSignature(entry.bill)
                val existingId = existingBillsMap[sig]
                if (mode == RestoreMode.MERGE && existingId != null) {
                    oldToNewBillId[entry.originalId] = existingId
                } else {
                    val newId = splitBillDao.insertBill(entry.bill.copy(id = 0L))
                    oldToNewBillId[entry.originalId] = newId
                    existingBillsMap[sig] = newId
                    if (entry.participants.isNotEmpty()) {
                        splitBillDao.insertParticipants(
                            entry.participants.map { p -> p.copy(id = 0L, billId = newId) }
                        )
                    }
                    restoredBillsCount++
                }
            }

            val existingExpenses = if (mode == RestoreMode.MERGE) {
                expenseDao.getAllExpensesOnce().map { BackupCodec.expenseSignature(it) }.toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.expenses.forEach { record ->
                val e = record.expense
                val sig = BackupCodec.expenseSignature(e)
                if (mode == RestoreMode.REPLACE || existingExpenses.add(sig)) {
                    val mappedBillId = e.linkedSplitBillId?.let { oldToNewBillId[it] }
                    val receiptPath = record.receiptFileName?.let { restoredReceiptPaths[it] }
                    expenseDao.insertExpense(
                        e.copy(
                            id = 0L,
                            linkedSplitBillId = mappedBillId,
                            receiptImagePath = receiptPath
                        )
                    )
                    restoredExpensesCount++
                }
            }
        }

        prefs.applyRestoredSettings(
            userName = payload.userName,
            currencySymbol = payload.currencySymbol,
            defaultHourlyRate = payload.defaultHourlyRate,
            monthlyBudget = payload.monthlyBudget,
            savingsBuffer = payload.savingsBuffer,
            settings = payload.settings
        )

        val modeLabel = if (mode == RestoreMode.REPLACE) "Replaced with" else "Merged"
        return "$modeLabel $restoredShiftsCount shift(s), $restoredExpensesCount expense(s), " +
            "$restoredBillsCount split bill(s)" +
            (if (restoredExtraIncomesCount > 0) ", $restoredExtraIncomesCount extra income(s)" else "") +
            if (restoredReceiptsCount > 0) ", $restoredReceiptsCount receipt(s)" else ""
    }

    suspend fun generateCsvExportFile(): File = withContext(Dispatchers.IO) {
        val shifts = allShifts.first()
        val extraIncomes = allExtraIncomes.first()
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
            extraIncomes.forEach { inc ->
                val date = LocalDate.ofEpochDay(inc.dateEpochDay)
                val title = inc.title.replace(",", " ")
                appendLine("ExtraIncome,$date,$title,Recurring,0,${"%.2f".format(inc.amount)}")
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
        val extraIncomes = allExtraIncomes.first()
        val expenses = allExpenses.first()
        val currency = currencySymbol.first()
        val name = userName.first()
        val totalIncome = shifts.sumOf { it.totalEarned } + extraIncomes.sumOf { it.amount }
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
