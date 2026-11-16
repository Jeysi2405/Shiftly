package com.example.shiftly.data.repository

import android.content.Context
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.room.withTransaction
import com.example.shiftly.data.backup.BackupCodec
import com.example.shiftly.data.backup.BackupExpenseRecord
import com.example.shiftly.data.backup.BackupPayload
import com.example.shiftly.data.backup.BackupSavingsGoal
import com.example.shiftly.data.backup.BackupSplitBill
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.NotificationLastDays
import com.example.shiftly.data.local.ProfilePhotoHelper
import com.example.shiftly.data.local.ReceiptStorageHelper
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftTimePreset
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
import java.time.Instant
import java.time.LocalDate
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
    private val savingsGoalDao = db.savingsGoalDao()
    private val prefs = UserPreferencesManager(context.applicationContext)
    private val recurringMutex = Mutex()

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    // Preferences & Active Timer
    val defaultHourlyRate: Flow<Double> = prefs.defaultHourlyRate
    val minimumWagePerDay: Flow<Double> = prefs.minimumWagePerDay
    val currencySymbol: Flow<String> = prefs.currencySymbol
    val activeTimerState: Flow<ActiveTimerState> = prefs.activeTimerState
    val hasCompletedOnboarding: Flow<Boolean> = prefs.hasCompletedOnboarding
    val userName: Flow<String> = prefs.userName
    val profilePhotoPath: Flow<String?> = prefs.profilePhotoPath
    val monthlyBudget: Flow<Double> = prefs.monthlyBudget
    val savingsBuffer: Flow<Double> = prefs.savingsBuffer
    val settingsSnapshot: Flow<ShiftlySettingsSnapshot> = prefs.settingsSnapshot

    suspend fun updateDefaultHourlyRate(rate: Double) = prefs.setDefaultHourlyRate(rate)
    suspend fun updateMinimumWagePerDay(amount: Double) = prefs.setMinimumWagePerDay(amount)
    suspend fun setOnboardingCompleted(completed: Boolean) = prefs.setOnboardingCompleted(completed)
    suspend fun updateUserName(name: String) = prefs.setUserName(name)
    suspend fun updateProfilePhotoPath(path: String?) {
        val previous = prefs.profilePhotoPath.first()
        prefs.setProfilePhotoPath(path)
        if (!previous.isNullOrBlank() && previous != path) {
            ProfilePhotoHelper.deleteProfilePhoto(previous)
        }
    }

    suspend fun updateMonthlyBudget(budget: Double) = prefs.setMonthlyBudget(budget)
    suspend fun updateSavingsBuffer(buffer: Double) = prefs.setSavingsBuffer(buffer)
    suspend fun updatePayCycleSchedule(schedule: String) = prefs.setPayCycleSchedule(schedule)
    suspend fun updateNotificationPreferences(
        notificationsEnabled: Boolean,
        billReminderEnabled: Boolean,
        shiftReminderEnabled: Boolean,
        savingsReminderEnabled: Boolean
    ) = prefs.setNotificationPreferences(
        notificationsEnabled = notificationsEnabled,
        billReminderEnabled = billReminderEnabled,
        shiftReminderEnabled = shiftReminderEnabled,
        savingsReminderEnabled = savingsReminderEnabled
    )
    suspend fun updateNotificationsEnabled(enabled: Boolean) = prefs.setNotificationsEnabled(enabled)
    suspend fun updateSavingsGoal(title: String, target: Double) = prefs.setSavingsGoal(title, target)
    suspend fun updateCategoryBudgets(items: List<CategoryBudgetSetting>) = prefs.setCategoryBudgets(items)
    suspend fun updateManagedCategories(items: List<ManagedCategorySetting>) = prefs.setManagedCategories(items)
    suspend fun updateRecurringEntries(items: List<RecurringEntrySetting>) = prefs.setRecurringEntries(items)
    suspend fun updateShiftTimePresets(items: List<ShiftTimePreset>) = prefs.setShiftTimePresets(items)

    suspend fun getNotificationLastDays(): NotificationLastDays = prefs.getNotificationLastDays()
    suspend fun markBillNotified(epochDay: Long) = prefs.markBillNotified(epochDay)
    suspend fun markShiftNotified(epochDay: Long) = prefs.markShiftNotified(epochDay)
    suspend fun markSavingsNotified(epochDay: Long) = prefs.markSavingsNotified(epochDay)

    suspend fun changeCurrency(newSymbol: String, conversionRate: Double?) = withContext(Dispatchers.IO) {
        if ((conversionRate != null) && conversionRate.isFinite() && (conversionRate > 0.0)) {
            db.withTransaction {
                shiftDao.getAllShiftsOnce().forEach { s ->
                    shiftDao.updateShift(
                        s.copy(
                            lockedHourlyRate = round2(s.lockedHourlyRate * conversionRate),
                            totalEarned = round2(s.totalEarned * conversionRate),
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
                splitBillDao.getAllBillsWithParticipantsOnce().forEach { (bill, participants) ->
                    val newTotal = round2(bill.totalAmount * conversionRate)
                    val shares = SplitBillMath.distributeEqualShares(
                        totalAmount = newTotal,
                        personCount = participants.size.coerceAtLeast(bill.personCount)
                    )
                    val newPerPerson = shares.firstOrNull() ?: round2(bill.perPersonAmount * conversionRate)
                    splitBillDao.updateBill(
                        bill.copy(
                            totalAmount = newTotal,
                            perPersonAmount = newPerPerson
                        )
                    )
                    participants.forEachIndexed { idx, p ->
                        val scaledOwed = shares.getOrElse(idx) { round2(p.amountOwed * conversionRate) }
                        splitBillDao.updateParticipant(p.copy(amountOwed = scaledOwed))
                    }
                }
                savingsGoalDao.getAllGoalsWithDepositsOnce().forEach { (goal, deposits) ->
                    savingsGoalDao.updateGoal(
                        goal.copy(targetAmount = round2(goal.targetAmount * conversionRate).coerceAtLeast(1.0))
                    )
                    deposits.forEach { d ->
                        savingsGoalDao.updateDeposit(
                            d.copy(amount = round2(d.amount * conversionRate).coerceAtLeast(0.01))
                        )
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

    suspend fun insertShift(shift: ShiftEntity): Long = shiftDao.insertShift(shift)
    suspend fun updateShift(shift: ShiftEntity) = shiftDao.updateShift(shift)
    suspend fun deleteShift(shift: ShiftEntity) = shiftDao.deleteShift(shift)

    // Extra Incomes (Recurring / Bonus income)
    val allExtraIncomes: Flow<List<ExtraIncomeEntity>> = extraIncomeDao.getAllExtraIncomes()
    val totalExtraIncomeAmount: Flow<Double?> = extraIncomeDao.getTotalExtraIncomeAmount()

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalExpenseAmount: Flow<Double?> = expenseDao.getTotalExpenseAmount()

    suspend fun insertExpense(expense: ExpenseEntity): Long = expenseDao.insertExpense(expense)

    suspend fun deleteExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            expenseDao.deleteExpense(expense)
            expense.linkedSplitBillId?.let { billId ->
                splitBillDao.setBillLoggedToExpense(billId, isLogged = false)
            }
            expense.linkedSavingsDepositId?.let { depositId ->
                savingsGoalDao.deleteDepositById(depositId)
            }
        }
        ReceiptStorageHelper.deleteReceiptFile(expense.receiptImagePath)
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
            splitBillDao.setBillLoggedToExpense(bill.id, isLogged = true)
        }
    }

    suspend fun deleteSplitBill(bill: SplitBillEntity, deleteLinkedExpense: Boolean = false) =
        withContext(Dispatchers.IO) {
            val linkedReceiptPaths = if (deleteLinkedExpense) {
                expenseDao.getExpensesByLinkedSplitBillIdOnce(bill.id)
                    .mapNotNull { it.receiptImagePath }
            } else {
                emptyList()
            }
            db.withTransaction {
                if (deleteLinkedExpense) {
                    expenseDao.deleteExpensesByLinkedSplitBillId(bill.id)
                } else {
                    expenseDao.unlinkExpensesBySplitBillId(bill.id)
                }
                splitBillDao.deleteBill(bill)
            }
            linkedReceiptPaths.forEach { ReceiptStorageHelper.deleteReceiptFile(it) }
        }

    // Savings Goals & Deposits (each deposit is automatically recorded as an Expense)
    val allSavingsGoals: Flow<List<SavingsGoalWithDeposits>> = savingsGoalDao.getAllGoalsWithDeposits()

    suspend fun createSavingsGoal(
        title: String,
        targetAmount: Double,
        targetDateEpochDay: Long
    ): Long {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || !targetAmount.isFinite() || targetAmount <= 0.0) return 0L
        val insertedId = db.withTransaction {
            savingsGoalDao.clearAllActiveFlags()
            savingsGoalDao.insertGoal(
                SavingsGoalEntity(
                    title = cleanTitle,
                    targetAmount = round2(targetAmount),
                    targetDateEpochDay = targetDateEpochDay,
                    isActive = true,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
        prefs.setSavingsGoal(cleanTitle, round2(targetAmount))
        return insertedId
    }

    suspend fun setActiveSavingsGoal(goalId: Long) {
        val goal = db.withTransaction {
            savingsGoalDao.setActiveGoal(goalId)
            savingsGoalDao.getGoalByIdOnce(goalId)
        }
        if (goal != null) {
            prefs.setSavingsGoal(goal.title, goal.targetAmount)
        }
    }

    suspend fun updateSavingsGoalItem(goal: SavingsGoalEntity) {
        db.withTransaction {
            savingsGoalDao.updateGoal(goal)
        }
        if (goal.isActive) {
            prefs.setSavingsGoal(goal.title, goal.targetAmount)
        }
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        val nextActive = db.withTransaction {
            val deposits = savingsGoalDao.getDepositsForGoalOnce(goal.id)
            val depositIds = deposits.map { it.id }
            if (depositIds.isNotEmpty()) {
                expenseDao.deleteExpensesByLinkedSavingsDepositIds(depositIds)
            }
            savingsGoalDao.deleteGoal(goal)
            val remaining = savingsGoalDao.getAllGoalsWithDepositsOnce()
            val candidate = remaining.firstOrNull()?.goal
            if (candidate != null && remaining.none { it.goal.isActive }) {
                savingsGoalDao.setActiveGoal(candidate.id)
            }
            remaining.firstOrNull { it.goal.isActive }?.goal ?: candidate
        }
        if (nextActive != null) {
            prefs.setSavingsGoal(nextActive.title, nextActive.targetAmount)
        } else {
            prefs.setSavingsGoal("", 0.0)
        }
    }

    /**
     * Records a deposit ("hulog") for the given savings goal and atomically inserts
     * a corresponding Expense ("Savings") so Total Balance, Expenses, Recent Activity,
     * and Analytics stay completely in sync.
     */
    suspend fun addSavingsDeposit(
        goal: SavingsGoalEntity,
        amount: Double,
        timestampMillis: Long = System.currentTimeMillis(),
        note: String = ""
    ): Long {
        if (!amount.isFinite() || amount <= 0.0) return 0L
        val cleanAmount = round2(amount)
        val cleanNote = note.trim()
        val depositDateEpochDay = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()

        return db.withTransaction {
            val depositId = savingsGoalDao.insertDeposit(
                SavingsDepositEntity(
                    goalId = goal.id,
                    amount = cleanAmount,
                    timestampMillis = timestampMillis,
                    note = cleanNote
                )
            )
            val expenseTitle = if (cleanNote.isNotEmpty()) {
                "Savings: ${goal.title} ($cleanNote)"
            } else {
                "Savings: ${goal.title}"
            }
            expenseDao.insertExpense(
                ExpenseEntity(
                    title = expenseTitle,
                    amount = cleanAmount,
                    category = "Savings",
                    dateEpochDay = depositDateEpochDay,
                    createdAtMillis = timestampMillis,
                    linkedSavingsDepositId = depositId
                )
            )
            depositId
        }
    }

    suspend fun deleteSavingsDeposit(deposit: SavingsDepositEntity) {
        db.withTransaction {
            expenseDao.deleteExpensesByLinkedSavingsDepositId(deposit.id)
            savingsGoalDao.deleteDeposit(deposit)
        }
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

    private suspend fun buildCurrentBackupPayload(includeReceipts: Boolean): BackupPayload = withContext(Dispatchers.IO) {
        val shifts = shiftDao.getAllShiftsOnce()
        val expenses = expenseDao.getAllExpensesOnce()
        val extraIncomes = extraIncomeDao.getAllExtraIncomesOnce()
        val bills = splitBillDao.getAllBillsWithParticipantsOnce()
        val goals = savingsGoalDao.getAllGoalsWithDepositsOnce()
        val settings = settingsSnapshot.first()
        val currency = currencySymbol.first()
        val rate = defaultHourlyRate.first()
        val minWage = minimumWagePerDay.first()
        val name = userName.first()
        val budget = monthlyBudget.first()
        val buffer = savingsBuffer.first()

        val receiptMap = mutableMapOf<String, ByteArray>()
        val backupExpenses = expenses.map { e ->
            val path = e.receiptImagePath
            val file = if (!path.isNullOrBlank()) File(path) else null
            val validName = if (includeReceipts && file != null && file.exists() && file.isFile) {
                runCatching {
                    val safeName = BackupCodec.sanitizeReceiptFileName(file.name)
                    if (safeName != null) {
                        receiptMap[safeName] = file.readBytes()
                    }
                    safeName
                }.getOrNull()
            } else {
                BackupCodec.sanitizeReceiptFileName(file?.name)
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

        val backupGoals = goals.map { entry ->
            BackupSavingsGoal(
                originalId = entry.goal.id,
                goal = entry.goal,
                deposits = entry.deposits
            )
        }

        BackupPayload(
            userName = name,
            currencySymbol = currency,
            defaultHourlyRate = rate,
            minimumWagePerDay = minWage,
            monthlyBudget = budget,
            savingsBuffer = buffer,
            settings = settings,
            shifts = shifts,
            expenses = backupExpenses,
            extraIncomes = extraIncomes,
            splitBills = backupBills,
            savingsGoals = backupGoals,
            receiptFiles = receiptMap
        )
    }

    // Complete Offline JSON Snapshot Export
    suspend fun generateOfflineBackupSummary(): String = withContext(Dispatchers.IO) {
        BackupCodec.encodeJson(buildCurrentBackupPayload(includeReceipts = false))
    }

    // Complete Offline ZIP Backup (JSON + Split Bills + Savings Goals + Settings + Receipt Photos)
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
        val existingReceiptFiles = if (mode == RestoreMode.REPLACE) {
            receiptsDir.listFiles()?.toList().orEmpty()
        } else {
            emptyList()
        }

        var restoredShiftsCount = 0
        var restoredExpensesCount = 0
        var restoredExtraIncomesCount = 0
        var restoredBillsCount = 0
        var restoredGoalsCount = 0

        val receiptsToWrite = mutableListOf<Pair<File, ByteArray>>()
        val restoredReceiptPaths = mutableMapOf<String, String>()
        payload.receiptFiles.forEach { (fileName, bytes) ->
            val safeName = BackupCodec.sanitizeReceiptFileName(fileName)
            if (safeName != null && bytes.isNotEmpty()) {
                val target = File(receiptsDir, safeName)
                restoredReceiptPaths[safeName] = target.absolutePath
                receiptsToWrite.add(target to bytes)
            }
        }

        db.withTransaction {
            if (mode == RestoreMode.REPLACE) {
                expenseDao.deleteAllExpenses()
                extraIncomeDao.deleteAllExtraIncomes()
                shiftDao.deleteAllShifts()
                splitBillDao.deleteAllSplitBills()
                savingsGoalDao.deleteAllSavingsGoals()
            }

            val existingShifts = if (mode == RestoreMode.MERGE) {
                shiftDao.getAllShiftsOnce().asSequence()
                    .map { BackupCodec.shiftSignature(it) }
                    .toMutableSet()
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
                extraIncomeDao.getAllExtraIncomesOnce().asSequence()
                    .map { BackupCodec.extraIncomeSignature(it) }
                    .toMutableSet()
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
                    .associateBy(
                        keySelector = { BackupCodec.splitBillSignature(it.bill) },
                        valueTransform = { it.bill.id }
                    )
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

            val existingGoalsMap = if (mode == RestoreMode.MERGE) {
                savingsGoalDao.getAllGoalsWithDepositsOnce()
                    .associateBy(
                        keySelector = { BackupCodec.savingsGoalSignature(it.goal) },
                        valueTransform = { it.goal.id }
                    )
                    .toMutableMap()
            } else {
                mutableMapOf()
            }

            val oldToNewDepositId = mutableMapOf<Long, Long>()
            payload.savingsGoals.forEach { entry ->
                val sig = BackupCodec.savingsGoalSignature(entry.goal)
                val existingId = existingGoalsMap[sig]
                if (mode == RestoreMode.MERGE && existingId != null) {
                    // Existing goal kept; do not duplicate deposits
                } else {
                    val newGoalId = savingsGoalDao.insertGoal(entry.goal.copy(id = 0L))
                    existingGoalsMap[sig] = newGoalId
                    entry.deposits.forEach { dep ->
                        val newDepId = savingsGoalDao.insertDeposit(
                            dep.copy(id = 0L, goalId = newGoalId)
                        )
                        if (dep.id > 0L) {
                            oldToNewDepositId[dep.id] = newDepId
                        }
                    }
                    restoredGoalsCount++
                }
            }

            val existingExpenses = if (mode == RestoreMode.MERGE) {
                expenseDao.getAllExpensesOnce().asSequence()
                    .map { BackupCodec.expenseSignature(it) }
                    .toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.expenses.forEach { record ->
                val e = record.expense
                val sig = BackupCodec.expenseSignature(e)
                if (mode == RestoreMode.REPLACE || existingExpenses.add(sig)) {
                    val mappedBillId = e.linkedSplitBillId?.let { oldToNewBillId[it] }
                    val mappedDepositId = e.linkedSavingsDepositId?.let { oldToNewDepositId[it] }
                    val receiptPath = record.receiptFileName?.let { restoredReceiptPaths[it] }
                    expenseDao.insertExpense(
                        e.copy(
                            id = 0L,
                            linkedSplitBillId = mappedBillId,
                            linkedSavingsDepositId = mappedDepositId,
                            receiptImagePath = receiptPath
                        )
                    )
                    restoredExpensesCount++
                }
            }
        }

        val keptPaths = restoredReceiptPaths.values.toSet()
        if (mode == RestoreMode.REPLACE) {
            existingReceiptFiles.forEach { oldFile ->
                if (oldFile.absolutePath !in keptPaths) {
                    oldFile.delete()
                }
            }
        }
        receiptsToWrite.forEach { (target, bytes) ->
            target.writeBytes(bytes)
        }
        val restoredReceiptsCount = receiptsToWrite.size

        prefs.applyRestoredSettings(
            userName = payload.userName,
            currencySymbol = payload.currencySymbol,
            defaultHourlyRate = payload.defaultHourlyRate,
            minimumWagePerDay = payload.minimumWagePerDay,
            monthlyBudget = payload.monthlyBudget,
            savingsBuffer = payload.savingsBuffer,
            settings = payload.settings
        )

        val modeLabel = if (mode == RestoreMode.REPLACE) "Replaced with" else "Merged"
        return "$modeLabel $restoredShiftsCount shift(s), $restoredExpensesCount expense(s), " +
            "$restoredBillsCount split bill(s)" +
            (if (restoredGoalsCount > 0) ", $restoredGoalsCount savings goal(s)" else "") +
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
        val csvContent = BackupCodec.buildCsvExport(
            shifts = shifts,
            extraIncomes = extraIncomes,
            expenses = expenses,
            currencySymbol = currency
        )
        csvFile.writeText(csvContent)
        csvFile
    }

    suspend fun generatePdfSummaryFile(): File = withContext(Dispatchers.IO) {
        val shifts = allShifts.first()
        val extraIncomes = allExtraIncomes.first()
        val expenses = allExpenses.first()
        val currency = currencySymbol.first()
        val name = userName.first().ifBlank { "Shiftly User" }
        val totalIncome = shifts.sumOf { it.totalEarned } + extraIncomes.sumOf { it.amount }
        val totalExpense = expenses.sumOf { it.amount }
        val netBalance = totalIncome - totalExpense
        val totalHours = shifts.sumOf { it.durationMinutes } / 60.0

        val exportDir = File(context.cacheDir, "exports").apply { mkdirs() }
        val pdfFile = File(exportDir, "shiftly_statement.pdf")

        val pageWidth = 595
        val pageHeight = 842
        val marginX = 40f
        val usableWidth = pageWidth - marginX * 2f
        val maxY = pageHeight - 48f

        val titlePaint = Paint().apply { textSize = 20f; isFakeBoldText = true }
        val headerPaint = Paint().apply { textSize = 13f; isFakeBoldText = true }
        val bodyPaint = Paint().apply { textSize = 11f }

        fun fitText(raw: String, paint: Paint): String {
            val cleaned = raw.replace('\n', ' ').replace('\r', ' ').trim()
            if (paint.measureText(cleaned) <= usableWidth) return cleaned
            val ellipsis = "..."
            val allowed = (usableWidth - paint.measureText(ellipsis)).coerceAtLeast(24f)
            val count = paint.breakText(cleaned, true, allowed, null)
            return if (count <= 0) ellipsis else cleaned.substring(0, count).trimEnd() + ellipsis
        }

        val doc = PdfDocument()
        try {
            var pageNumber = 1
            var page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas
            var y = 52f

            fun ensureSpace(neededHeight: Float) {
                if (y + neededHeight > maxY) {
                    doc.finishPage(page)
                    pageNumber++
                    page = doc.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                    canvas = page.canvas
                    y = 52f
                    canvas.drawText(
                        fitText("Shiftly Statement — $name (Page $pageNumber)", headerPaint),
                        marginX,
                        y,
                        headerPaint
                    )
                    y += 24f
                }
            }

            fun drawLine(text: String, paint: Paint, lineHeight: Float) {
                ensureSpace(lineHeight)
                canvas.drawText(fitText(text, paint), marginX, y, paint)
                y += lineHeight
            }

            drawLine("Shiftly Statement — $name", titlePaint, 24f)
            drawLine(
                "Shifts: ${shifts.size} (${"%.1f".format(totalHours)}h)   |   " +
                    "Income: $currency${"%,.2f".format(totalIncome)}   |   " +
                    "Expenses: $currency${"%,.2f".format(totalExpense)}",
                bodyPaint,
                16f
            )
            drawLine(
                "Net Balance: $currency${"%,.2f".format(netBalance)}",
                bodyPaint,
                26f
            )

            drawLine("Logged Shifts (${shifts.size})", headerPaint, 18f)
            if (shifts.isEmpty()) {
                drawLine("No shifts logged yet.", bodyPaint, 16f)
            } else {
                shifts.forEach { s ->
                    val date = LocalDate.ofEpochDay(s.dateEpochDay)
                    val noteSuffix = if (s.overtimeNote.isNotBlank()) "  ${s.overtimeNote}" else ""
                    drawLine(
                        "$date  •  ${s.durationMinutes}m  •  +$currency${"%,.2f".format(s.totalEarned)}$noteSuffix",
                        bodyPaint,
                        16f
                    )
                }
            }

            if (extraIncomes.isNotEmpty()) {
                y += 10f
                drawLine("Extra Incomes (${extraIncomes.size})", headerPaint, 18f)
                extraIncomes.forEach { inc ->
                    val date = LocalDate.ofEpochDay(inc.dateEpochDay)
                    drawLine(
                        "$date  •  +$currency${"%,.2f".format(inc.amount)}  •  ${inc.title}",
                        bodyPaint,
                        16f
                    )
                }
            }

            y += 10f
            drawLine("Logged Expenses (${expenses.size})", headerPaint, 18f)
            if (expenses.isEmpty()) {
                drawLine("No expenses logged yet.", bodyPaint, 16f)
            } else {
                expenses.forEach { e ->
                    val date = LocalDate.ofEpochDay(e.dateEpochDay)
                    drawLine(
                        "$date  •  ${e.category}  •  -$currency${"%,.2f".format(e.amount)}  ${e.title}",
                        bodyPaint,
                        16f
                    )
                }
            }

            doc.finishPage(page)
            FileOutputStream(pdfFile).use { out ->
                doc.writeTo(out)
            }
        } finally {
            doc.close()
        }
        pdfFile
    }
}
