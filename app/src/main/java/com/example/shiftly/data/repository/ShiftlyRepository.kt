package com.example.shiftly.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import com.example.shiftly.data.backup.BackupCodec
import com.example.shiftly.data.backup.BackupExpenseRecord
import com.example.shiftly.data.backup.BackupInstallmentLoan
import com.example.shiftly.data.backup.BackupPayload
import com.example.shiftly.data.backup.BackupSavingsGoal
import com.example.shiftly.data.backup.BackupSplitBill
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.InAppNotificationItem
import com.example.shiftly.data.local.InAppNotificationType
import com.example.shiftly.data.local.InstallmentLoanEntity
import com.example.shiftly.data.local.InstallmentLoanWithPayments
import com.example.shiftly.data.local.IouEntrySetting
import com.example.shiftly.data.local.LoanPaymentEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.NotificationLastDays
import com.example.shiftly.data.local.PaycheckDeductionsSetting
import com.example.shiftly.data.local.ReceiptStorageHelper
import com.example.shiftly.data.local.RecurringBillEntity
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
import com.example.shiftly.data.local.normalizedOrNull
import com.example.shiftly.data.local.paidMonthsClamped
import com.example.shiftly.data.local.toInstallmentLoanEntity
import com.example.shiftly.notifications.ShiftlyNotificationHelper
import com.example.shiftly.utils.CutoffCalculator
import com.example.shiftly.utils.CutoffShiftSummary
import com.example.shiftly.utils.PinSecurity
import com.example.shiftly.utils.RecurringEngine
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.SplitBillMath
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.abs
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class ShiftlyRepository(context: Context) {
    private val appContext: Context = context.applicationContext ?: context
    private val db = ShiftlyDatabase.getInstance(appContext)
    private val shiftDao = db.shiftDao()
    private val expenseDao = db.expenseDao()
    private val extraIncomeDao = db.extraIncomeDao()
    private val recurringBillDao = db.recurringBillDao()
    private val allowanceDao = db.allowanceDao()
    private val splitBillDao = db.splitBillDao()
    private val savingsGoalDao = db.savingsGoalDao()
    private val installmentLoanDao = db.installmentLoanDao()
    private val prefs = UserPreferencesManager(appContext)
    private val recurringMutex = Mutex()
    private val allowanceMutex = Mutex()
    private val notificationMutex = Mutex()
    private val installmentLoanMutex = Mutex()

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    // Preferences
    val defaultHourlyRate: Flow<Double> = prefs.defaultHourlyRate
    val minimumWagePerDay: Flow<Double> = prefs.minimumWagePerDay
    val currencySymbol: Flow<String> = prefs.currencySymbol
    val hasCompletedOnboarding: Flow<Boolean> = prefs.hasCompletedOnboarding
    val hasSeenAppTutorial: Flow<Boolean> = prefs.hasSeenAppTutorial
    val userName: Flow<String> = prefs.userName
    val monthlyBudget: Flow<Double> = prefs.monthlyBudget
    val savingsBuffer: Flow<Double> = prefs.savingsBuffer
    val settingsSnapshot: Flow<ShiftlySettingsSnapshot> = prefs.settingsSnapshot
    val notificationInbox: Flow<List<InAppNotificationItem>> = prefs.notificationInbox
    val balancePrivacyHidden: Flow<Boolean> = prefs.balancePrivacyHidden
    val paycheckDeductions: Flow<PaycheckDeductionsSetting> = prefs.paycheckDeductions
    val iouEntries: Flow<List<IouEntrySetting>> = prefs.iouEntries

    suspend fun setBalancePrivacyHidden(hidden: Boolean) = withContext(Dispatchers.IO) {
        prefs.setBalancePrivacyHidden(hidden)
    }

    suspend fun recordInAppNotification(
        eventKey: String,
        type: InAppNotificationType,
        title: String,
        message: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Boolean = withContext(Dispatchers.IO) {
        prefs.recordInAppNotification(
            eventKey = eventKey,
            type = type,
            title = title,
            message = message,
            timestampMillis = timestampMillis
        )
    }

    suspend fun markNotificationRead(id: String) = withContext(Dispatchers.IO) {
        prefs.markNotificationRead(id)
    }

    suspend fun markNotificationToastShown(id: String) = withContext(Dispatchers.IO) {
        prefs.markNotificationToastShown(id)
    }

    suspend fun markAllNotificationsRead() = withContext(Dispatchers.IO) {
        prefs.markAllNotificationsRead()
    }

    suspend fun clearNotificationInbox() = withContext(Dispatchers.IO) {
        prefs.clearNotificationInbox()
    }

    suspend fun syncInAppNotifications(now: LocalDateTime = LocalDateTime.now()) =
        withContext(Dispatchers.IO) {
            notificationMutex.withLock {
                val settings = settingsSnapshot.first()
                if (!settings.notificationsEnabled) return@withLock

                val currency = currencySymbol.first()
                val today = now.toLocalDate()
                val todayEpoch = today.toEpochDay()
                val zone = ZoneId.systemDefault()

                if (settings.billReminderEnabled) {
                    val allBills = recurringBillDao.getAllBillsOnce()
                    val dueSoonBills = RecurringEngine.findBillsDueWithinOneDay(
                        bills = allBills,
                        now = now
                    )
                    if (dueSoonBills.isNotEmpty()) {
                        val copy = ShiftlyNotificationHelper.formatRecurringBillReminderCopy(
                            dueBills = dueSoonBills,
                            currencySymbol = currency
                        )
                        val billKey = dueSoonBills.map { "${it.id}_${it.dueEpochDay}" }
                            .sorted()
                            .joinToString("_")
                        prefs.recordInAppNotification(
                            eventKey = "bill_due_${todayEpoch}_$billKey",
                            type = InAppNotificationType.BILL_DUE,
                            title = copy.title,
                            message = copy.message
                        )
                    }
                }

                val splitBills = splitBillDao.getAllBillsWithParticipantsOnce()
                val splitSummary = SplitBillMath.summarizeFriendsOwed(splitBills)
                if (splitSummary.pendingBillCount > 0 && splitSummary.pendingFriendsAmount > 0.0) {
                    val amountKey = (splitSummary.pendingFriendsAmount * 100.0).roundToLong()
                    val count = splitSummary.pendingBillCount
                    val plural = if (count > 1) "s" else ""
                    prefs.recordInAppNotification(
                        eventKey = "split_pending_${todayEpoch}_${count}_$amountKey",
                        type = InAppNotificationType.SPLIT_BILL,
                        title = "$count Split Bill$plural Pending",
                        message = "${ShiftlyFormatters.formatCurrency(currency, splitSummary.pendingFriendsAmount)} awaiting payment from friends."
                    )
                }

                val shifts = shiftDao.getAllShiftsOnce()
                val hasLoggedToday = shifts.any { it.dateEpochDay == todayEpoch }
                if (RecurringEngine.shouldSendShiftReminder(
                        settings = settings,
                        hasLoggedShiftToday = hasLoggedToday,
                        isTimerRunning = false,
                        hourOfDay = now.hour
                    )
                ) {
                    val copy = ShiftlyNotificationHelper.formatShiftReminderCopy()
                    prefs.recordInAppNotification(
                        eventKey = "shift_reminder_$todayEpoch",
                        type = InAppNotificationType.SHIFT_REMINDER,
                        title = copy.title,
                        message = copy.message
                    )
                }

                val goals = savingsGoalDao.getAllGoalsWithDepositsOnce()
                val activeGoal = goals.firstOrNull { it.goal.isActive } ?: goals.firstOrNull()
                val hasDepositedToday = goals.any { g ->
                    g.deposits.any { dep ->
                        Instant.ofEpochMilli(dep.timestampMillis).atZone(zone).toLocalDate() == today
                    }
                }
                val isPayday = RecurringEngine.isPaydayToday(settings.cutoffScheduleConfig(), today)
                if (RecurringEngine.shouldSendSavingsDepositReminder(
                        settings = settings,
                        isPaydayToday = isPayday,
                        hasDepositedToday = hasDepositedToday,
                        hourOfDay = now.hour
                    )
                ) {
                    val copy = ShiftlyNotificationHelper.formatSavingsDepositReminderCopy(
                        activeGoalTitle = activeGoal?.goal?.title.orEmpty(),
                        isPayday = isPayday
                    )
                    prefs.recordInAppNotification(
                        eventKey = "savings_reminder_$todayEpoch",
                        type = InAppNotificationType.SAVINGS_REMINDER,
                        title = copy.title,
                        message = copy.message
                    )
                }
            }
        }

    suspend fun updateDefaultHourlyRate(rate: Double) = withContext(Dispatchers.IO) {
        prefs.setDefaultHourlyRate(rate)
    }

    suspend fun updateMinimumWagePerDay(amount: Double) = withContext(Dispatchers.IO) {
        prefs.setMinimumWagePerDay(amount)
    }

    suspend fun setOnboardingCompleted(completed: Boolean) = withContext(Dispatchers.IO) {
        prefs.setOnboardingCompleted(completed)
    }

    suspend fun setHasSeenAppTutorial(seen: Boolean) = withContext(Dispatchers.IO) {
        prefs.setHasSeenAppTutorial(seen)
    }

    suspend fun updateUserName(name: String) = withContext(Dispatchers.IO) {
        prefs.setUserName(name)
    }

    suspend fun updateProfileDetails(
        userName: String,
        jobTitle: String,
        workplace: String,
        workType: String,
        targetWeeklyHours: Double
    ) = withContext(Dispatchers.IO) {
        prefs.setProfileDetails(userName, jobTitle, workplace, workType, targetWeeklyHours)
    }

    suspend fun updatePaycheckDeductions(setting: PaycheckDeductionsSetting) = withContext(Dispatchers.IO) {
        prefs.setPaycheckDeductions(setting)
    }

    suspend fun updateIouEntries(items: List<IouEntrySetting>) = withContext(Dispatchers.IO) {
        prefs.setIouEntries(items)
    }

    // Installment Loans (Room: installment_loans + loan_payments)
    val allInstallmentLoans: Flow<List<InstallmentLoanWithPayments>> =
        installmentLoanDao.getAllInstallmentLoansWithPayments()

    /**
     * One-time, non-destructive move of loans saved by the earlier DataStore-based tracker into
     * Room. Legacy rows are only cleared after they have been inserted successfully.
     */
    suspend fun migrateLegacyInstallmentLoans() = withContext(Dispatchers.IO) {
        installmentLoanMutex.withLock {
            val legacy = prefs.installmentLoans.first()
            if (legacy.isEmpty()) return@withLock
            val converted = legacy.mapNotNull { it.toInstallmentLoanEntity().normalizedOrNull() }
            db.withTransaction {
                converted.forEach { installmentLoanDao.insertInstallmentLoan(it.copy(id = 0L)) }
            }
            prefs.setInstallmentLoans(emptyList())
        }
    }

    /** Inserts a new loan (id == 0) or updates an existing one while preserving its paid progress. */
    suspend fun addOrUpdateInstallmentLoan(loan: InstallmentLoanEntity): Long = withContext(Dispatchers.IO) {
        val normalized = loan.normalizedOrNull() ?: return@withContext 0L
        installmentLoanMutex.withLock {
            db.withTransaction {
                if (normalized.id > 0L) {
                    val existing = installmentLoanDao.getLoanByIdOnce(normalized.id)
                    if (existing == null) {
                        installmentLoanDao.insertInstallmentLoan(normalized.copy(id = 0L, paidMonths = 0))
                    } else {
                        val safePaid = existing.paidMonths.coerceIn(0, normalized.termsInMonths)
                        installmentLoanDao.updateInstallmentLoan(normalized.copy(paidMonths = safePaid))
                        normalized.id
                    }
                } else {
                    installmentLoanDao.insertInstallmentLoan(normalized.copy(id = 0L))
                }
            }
        }
    }

    /** Records the next unpaid month as paid and advances `paidMonths`. Returns false when nothing changed. */
    suspend fun markLoanMonthAsPaid(loanId: Long): Boolean = withContext(Dispatchers.IO) {
        installmentLoanMutex.withLock {
            db.withTransaction {
                val loan = installmentLoanDao.getLoanByIdOnce(loanId) ?: return@withTransaction false
                val paid = loan.paidMonthsClamped
                if (paid >= loan.termsInMonths) return@withTransaction false
                installmentLoanDao.insertLoanPayment(
                    LoanPaymentEntity(
                        loanId = loan.id,
                        monthNumber = paid + 1,
                        amountPaid = loan.installmentPerMonth,
                        datePaidMillis = System.currentTimeMillis()
                    )
                )
                installmentLoanDao.updateInstallmentLoan(loan.copy(paidMonths = paid + 1))
                true
            }
        }
    }

    suspend fun updateLoanPayment(payment: LoanPaymentEntity) = withContext(Dispatchers.IO) {
        installmentLoanMutex.withLock {
            installmentLoanDao.updateLoanPayment(payment)
        }
    }

    suspend fun deleteLoanPayment(loan: InstallmentLoanEntity, payment: LoanPaymentEntity) = withContext(Dispatchers.IO) {
        installmentLoanMutex.withLock {
            db.withTransaction {
                installmentLoanDao.deleteLoanPayment(payment)
                val existingLoan = installmentLoanDao.getLoanByIdOnce(loan.id) ?: loan
                val newPaidMonths = (existingLoan.paidMonths - 1).coerceAtLeast(0)
                installmentLoanDao.updateInstallmentLoan(existingLoan.copy(paidMonths = newPaidMonths))
            }
        }
    }

    suspend fun deleteInstallmentLoan(loan: InstallmentLoanEntity) = withContext(Dispatchers.IO) {
        installmentLoanMutex.withLock {
            db.withTransaction {
                installmentLoanDao.deletePaymentsForLoan(loan.id)
                installmentLoanDao.deleteInstallmentLoan(loan)
            }
        }
    }

    suspend fun updateMonthlyBudget(budget: Double) = withContext(Dispatchers.IO) {
        prefs.setMonthlyBudget(budget)
    }

    suspend fun updateCutoffSchedule(config: CutoffScheduleConfig) = withContext(Dispatchers.IO) {
        prefs.updateCutoffSchedule(config)
        processDueCutoffAllowances()
    }

    suspend fun updateNotificationPreferences(
        notificationsEnabled: Boolean,
        billReminderEnabled: Boolean,
        shiftReminderEnabled: Boolean,
        savingsReminderEnabled: Boolean
    ) = withContext(Dispatchers.IO) {
        prefs.setNotificationPreferences(
            notificationsEnabled = notificationsEnabled,
            billReminderEnabled = billReminderEnabled,
            shiftReminderEnabled = shiftReminderEnabled,
            savingsReminderEnabled = savingsReminderEnabled
        )
    }

    suspend fun updateNotificationsEnabled(enabled: Boolean) = withContext(Dispatchers.IO) {
        prefs.setNotificationsEnabled(enabled)
    }

    suspend fun updateCategoryBudgets(items: List<CategoryBudgetSetting>) = withContext(Dispatchers.IO) {
        prefs.setCategoryBudgets(items)
    }

    suspend fun updateManagedCategories(items: List<ManagedCategorySetting>) = withContext(Dispatchers.IO) {
        prefs.setManagedCategories(items)
    }

    suspend fun updateShiftTimePresets(items: List<ShiftTimePreset>) = withContext(Dispatchers.IO) {
        prefs.setShiftTimePresets(items)
    }

    suspend fun getNotificationLastDays(): NotificationLastDays = withContext(Dispatchers.IO) {
        prefs.getNotificationLastDays()
    }

    suspend fun markBillNotified(epochDay: Long) = withContext(Dispatchers.IO) {
        prefs.markBillNotified(epochDay)
    }

    suspend fun markShiftNotified(epochDay: Long) = withContext(Dispatchers.IO) {
        prefs.markShiftNotified(epochDay)
    }

    suspend fun markSavingsNotified(epochDay: Long) = withContext(Dispatchers.IO) {
        prefs.markSavingsNotified(epochDay)
    }

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
                recurringBillDao.getAllBillsOnce().forEach { rb ->
                    recurringBillDao.updateBill(
                        rb.copy(amount = round2(rb.amount * conversionRate).coerceAtLeast(0.01))
                    )
                }
                allowanceDao.getAllEntriesOnce().forEach { a ->
                    allowanceDao.updateEntry(
                        a.copy(amount = round2(a.amount * conversionRate).coerceAtLeast(0.01))
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
                installmentLoanDao.getAllInstallmentLoansWithPaymentsOnce().forEach { entry ->
                    val loan = entry.loan
                    installmentLoanDao.updateInstallmentLoan(
                        loan.copy(
                            cashPrice = round2(loan.cashPrice * conversionRate),
                            downpayment = round2(loan.downpayment * conversionRate),
                            installmentPerMonth = round2(loan.installmentPerMonth * conversionRate)
                                .coerceAtLeast(0.01)
                        )
                    )
                    entry.payments.forEach { p ->
                        installmentLoanDao.updateLoanPayment(
                            p.copy(amountPaid = round2(p.amountPaid * conversionRate))
                        )
                    }
                }
            }
        }
        prefs.applyCurrencyConversion(newSymbol, conversionRate)
    }

    suspend fun updateAppLock(enabled: Boolean, method: String, newPin: String? = null) = withContext(Dispatchers.IO) {
        val existingHash = settingsSnapshot.first().appLockPinHash
        val resolvedHash = when {
            !newPin.isNullOrBlank() && PinSecurity.isValidPinFormat(newPin) -> PinSecurity.hashPin(newPin)
            else -> existingHash
        }
        prefs.setAppLock(enabled = enabled, method = method, pinHash = resolvedHash)
    }

    suspend fun updateThemeMode(mode: String) = withContext(Dispatchers.IO) {
        prefs.updateThemeMode(mode)
    }

    // Shifts (one shift per dateEpochDay enforced at repository + DB levels)
    val allShifts: Flow<List<ShiftEntity>> = shiftDao.getAllShifts()
    val totalShiftCount: Flow<Int> = shiftDao.getTotalShiftCount()
    val totalMinutesWorked: Flow<Int?> = shiftDao.getTotalMinutesWorked()
    val totalLifetimeEarnings: Flow<Double?> = shiftDao.getTotalLifetimeEarnings()

    suspend fun insertShift(shift: ShiftEntity): Long = withContext(Dispatchers.IO) {
        if (shiftDao.getShiftByDateOnce(shift.dateEpochDay) != null) return@withContext -1L
        val id = try {
            shiftDao.insertShift(shift)
        } catch (_: SQLiteConstraintException) {
            -1L
        }
        if (id > 0L) {
            processDueCutoffAllowances()
        }
        id
    }

    suspend fun updateShift(shift: ShiftEntity): Boolean = withContext(Dispatchers.IO) {
        val conflict = shiftDao.getShiftByDateOnce(shift.dateEpochDay)
        if (conflict != null && conflict.id != shift.id) return@withContext false
        val updated = try {
            shiftDao.updateShift(shift)
            true
        } catch (_: SQLiteConstraintException) {
            false
        }
        if (updated) {
            processDueCutoffAllowances()
        }
        updated
    }

    suspend fun deleteShift(shift: ShiftEntity) = withContext(Dispatchers.IO) {
        shiftDao.deleteShift(shift)
        processDueCutoffAllowances()
    }

    // Extra Incomes (Legacy / Bonus income)
    val allExtraIncomes: Flow<List<ExtraIncomeEntity>> = extraIncomeDao.getAllExtraIncomes()
    val totalExtraIncomeAmount: Flow<Double?> = extraIncomeDao.getTotalExtraIncomeAmount()

    // Recurring Bills (Dedicated Room table with exact due date & time execution)
    val allRecurringBills: Flow<List<RecurringBillEntity>> = recurringBillDao.getAllBills()

    suspend fun insertRecurringBill(bill: RecurringBillEntity): Long = withContext(Dispatchers.IO) {
        recurringBillDao.insertBill(bill)
    }

    suspend fun updateRecurringBill(bill: RecurringBillEntity) = withContext(Dispatchers.IO) {
        recurringBillDao.updateBill(bill)
    }

    suspend fun deleteRecurringBill(bill: RecurringBillEntity) = withContext(Dispatchers.IO) {
        recurringBillDao.deleteBill(bill)
    }

    suspend fun toggleRecurringBillEnabled(bill: RecurringBillEntity, enabled: Boolean) = withContext(Dispatchers.IO) {
        recurringBillDao.updateBill(bill.copy(isEnabled = enabled))
    }

    // Allowance & Payday Cutoff Credits
    val allAllowanceEntries: Flow<List<AllowanceEntryEntity>> = allowanceDao.getAllEntries()

    suspend fun addManualAllowanceEntry(
        title: String,
        amount: Double,
        source: String,
        dateEpochDay: Long
    ): Long = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || !amount.isFinite() || amount <= 0.0) return@withContext 0L
        val safeSource = if (source in AllowanceSource.ALL) source else AllowanceSource.OTHERS
        allowanceDao.insertEntry(
            AllowanceEntryEntity(
                title = cleanTitle,
                amount = round2(amount),
                source = safeSource,
                dateEpochDay = dateEpochDay,
                cutoffKey = null,
                createdAtMillis = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteAllowanceEntry(entry: AllowanceEntryEntity) = withContext(Dispatchers.IO) {
        allowanceDao.deleteEntry(entry)
    }

    /**
     * Reconciles reached Duties cutoff windows (`window.endDate <= today`) against the
     * `allowance_entries` table using unique `cutoffKey`. Idempotent and safe to run on
     * launch, resume, shift CRUD, cutoff config changes, and background worker runs.
     */
    suspend fun processDueCutoffAllowances(today: LocalDate = LocalDate.now()): Int =
        withContext(Dispatchers.IO) {
            allowanceMutex.withLock {
                val snapshot = settingsSnapshot.first()
                val config = snapshot.cutoffScheduleConfig()
                var changesCount = 0
                val newlyCredited = mutableListOf<Pair<String, CutoffShiftSummary>>()

                db.withTransaction {
                    val shifts = shiftDao.getAllShiftsOnce()
                    val existingEntries = allowanceDao.getAllEntriesOnce()
                    val reachedSummaries = CutoffCalculator.resolveReachedCutoffWindows(
                        shifts = shifts,
                        today = today,
                        config = config
                    ).filter { it.totalEarnings > 0.0 }

                    val reachedByKey = reachedSummaries.associateBy { it.window.key }
                    val existingAuto = existingEntries
                        .filter { !it.cutoffKey.isNullOrBlank() }
                        .associateBy { it.cutoffKey!! }

                    // Remove stale auto-credited entries whose cutoff key no longer has reached earnings
                    existingAuto.forEach { (key, entry) ->
                        if (!reachedByKey.containsKey(key)) {
                            allowanceDao.deleteEntry(entry)
                        }
                    }

                    val nowMillis = System.currentTimeMillis()
                    reachedByKey.forEach { (key, summary) ->
                        val amount = round2(summary.totalEarnings)
                        val title = "Salary Payout (${summary.window.label})"
                        val dateEpoch = summary.window.endDate.toEpochDay()
                        val existing = existingAuto[key]
                        if (existing == null) {
                            allowanceDao.insertEntry(
                                AllowanceEntryEntity(
                                    title = title,
                                    amount = amount,
                                    source = AllowanceSource.SALARY,
                                    dateEpochDay = dateEpoch,
                                    cutoffKey = key,
                                    createdAtMillis = nowMillis
                                )
                            )
                            newlyCredited.add(key to summary)
                            changesCount++
                        } else if (
                            abs(existing.amount - amount) > 0.005 ||
                            existing.dateEpochDay != dateEpoch ||
                            existing.title != title
                        ) {
                            allowanceDao.updateEntry(
                                existing.copy(
                                    title = title,
                                    amount = amount,
                                    dateEpochDay = dateEpoch
                                )
                            )
                            changesCount++
                        }
                    }
                }
                if (newlyCredited.isNotEmpty() && snapshot.notificationsEnabled) {
                    val currency = currencySymbol.first()
                    newlyCredited.forEach { (key, summary) ->
                        prefs.recordInAppNotification(
                            eventKey = "cutoff_payout_$key",
                            type = InAppNotificationType.PAYDAY_CUTOFF,
                            title = "Payday Cutoff Credited",
                            message = "${ShiftlyFormatters.formatCurrency(currency, round2(summary.totalEarnings))} from ${summary.window.label} added to Allowance."
                        )
                    }
                }
                changesCount
            }
        }

    // Expenses
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()
    val totalExpenseAmount: Flow<Double?> = expenseDao.getTotalExpenseAmount()

    suspend fun insertExpense(expense: ExpenseEntity): Long = withContext(Dispatchers.IO) {
        expenseDao.insertExpense(expense)
    }

    suspend fun updateExpense(expense: ExpenseEntity) = withContext(Dispatchers.IO) {
        val existing = expenseDao.getExpenseByIdOnce(expense.id)
        expenseDao.updateExpense(expense)
        val oldReceiptPath = existing?.receiptImagePath
        if (!oldReceiptPath.isNullOrBlank() && oldReceiptPath != expense.receiptImagePath) {
            ReceiptStorageHelper.deleteReceiptFile(oldReceiptPath)
        }
    }

    suspend fun deleteExpense(
        expense: ExpenseEntity,
        deleteReceiptFile: Boolean = true
    ) = withContext(Dispatchers.IO) {
        db.withTransaction {
            expenseDao.deleteExpense(expense)
            expense.linkedSplitBillId?.let { billId ->
                splitBillDao.setBillLoggedToExpense(billId, isLogged = false)
            }
            expense.linkedSavingsDepositId?.let { depositId ->
                savingsGoalDao.deleteDepositById(depositId)
            }
        }
        if (deleteReceiptFile) {
            ReceiptStorageHelper.deleteReceiptFile(expense.receiptImagePath)
        }
    }

    suspend fun deleteReceiptFile(path: String?) = withContext(Dispatchers.IO) {
        ReceiptStorageHelper.deleteReceiptFile(path)
    }

    // Split Bills
    val allSplitBills: Flow<List<SplitBillWithParticipants>> = splitBillDao.getAllBillsWithParticipants()

    suspend fun createSplitBill(
        bill: SplitBillEntity,
        participants: List<SplitParticipantEntity>
    ): Long = withContext(Dispatchers.IO) {
        val insertedId = db.withTransaction {
            val billId = splitBillDao.insertBill(bill)
            val linkedParticipants = participants.map { it.copy(billId = billId) }
            splitBillDao.insertParticipants(linkedParticipants)
            billId
        }
        if (insertedId > 0L) {
            syncInAppNotifications()
        }
        insertedId
    }

    suspend fun toggleParticipantPaid(participantId: Long, hasPaid: Boolean) = withContext(Dispatchers.IO) {
        splitBillDao.updateParticipantPaidStatus(participantId, hasPaid)
    }

    suspend fun logSplitShareAsExpense(bill: SplitBillEntity) = withContext(Dispatchers.IO) {
        if (bill.isLoggedToExpense) return@withContext
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
    ): Long = withContext(Dispatchers.IO) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || !targetAmount.isFinite() || targetAmount <= 0.0) return@withContext 0L
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
        insertedId
    }

    suspend fun setActiveSavingsGoal(goalId: Long) = withContext(Dispatchers.IO) {
        val goal = db.withTransaction {
            savingsGoalDao.setActiveGoal(goalId)
            savingsGoalDao.getGoalByIdOnce(goalId)
        }
        if (goal != null) {
            prefs.setSavingsGoal(goal.title, goal.targetAmount)
        }
    }

    suspend fun deleteSavingsGoal(goal: SavingsGoalEntity) = withContext(Dispatchers.IO) {
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
    ): Long = withContext(Dispatchers.IO) {
        if (!amount.isFinite() || amount <= 0.0) return@withContext 0L
        val cleanAmount = round2(amount)
        val cleanNote = note.trim()
        val depositDateEpochDay = Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
            .toEpochDay()

        db.withTransaction {
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

    suspend fun deleteSavingsDeposit(deposit: SavingsDepositEntity) = withContext(Dispatchers.IO) {
        db.withTransaction {
            expenseDao.deleteExpensesByLinkedSavingsDepositId(deposit.id)
            savingsGoalDao.deleteDeposit(deposit)
        }
    }

    /**
     * Evaluates Room [RecurringBillEntity] entries via [RecurringEngine] against exact [now]
     * date & time, migrating any legacy DataStore bill rules on first run, and atomically
     * inserts due expenses idempotently while advancing each bill's `dueEpochDay`.
     */
    suspend fun processDueRecurringBills(now: LocalDateTime = LocalDateTime.now()): Int =
        withContext(Dispatchers.IO) {
            recurringMutex.withLock {
                val snapshot = settingsSnapshot.first()
                val today = now.toLocalDate()

                if (!snapshot.legacyRecurringMigrated) {
                    val legacyBills = snapshot.recurringEntries.filter { !it.isIncome }
                    if (legacyBills.isNotEmpty()) {
                        db.withTransaction {
                            val existingSignatures = recurringBillDao.getAllBillsOnce()
                                .map { BackupCodec.recurringBillSignature(it) }
                                .toMutableSet()
                            val createdNow = System.currentTimeMillis()
                            legacyBills.forEach { legacy ->
                                val nextDue = RecurringEngine.nextDueDate(legacy, today) ?: today
                                val entity = RecurringBillEntity(
                                    title = legacy.title.trim(),
                                    amount = round2(legacy.amount),
                                    category = legacy.category.ifBlank { "Bills" },
                                    frequency = legacy.frequency.ifBlank { "Monthly" },
                                    dueEpochDay = nextDue.toEpochDay(),
                                    dueHour = 9,
                                    dueMinute = 0,
                                    isEnabled = legacy.isEnabled,
                                    lastExecutedEpochDay = legacy.lastAppliedEpochDay,
                                    lastExecutedAtMillis = if (legacy.lastAppliedEpochDay > 0L) createdNow else 0L,
                                    createdAtMillis = createdNow
                                )
                                val sig = BackupCodec.recurringBillSignature(entity)
                                if (existingSignatures.add(sig)) {
                                    recurringBillDao.insertBill(entity)
                                }
                            }
                        }
                        prefs.setRecurringEntries(snapshot.recurringEntries.filter { it.isIncome })
                    }
                    prefs.markLegacyRecurringMigrated(true)
                }

                var postedCount = 0
                val newlyPostedBills = mutableListOf<Pair<RecurringBillEntity, Long>>()
                val zone = ZoneId.systemDefault()
                val nowMillis = System.currentTimeMillis()

                db.withTransaction {
                    val bills = recurringBillDao.getAllBillsOnce()
                    bills.forEach { bill ->
                        val dueOccurrences = RecurringEngine.computeDueBillOccurrences(bill, now)
                        if (dueOccurrences.isNotEmpty()) {
                            dueOccurrences.forEach { occ ->
                                val occEpochDay = occ.toLocalDate().toEpochDay()
                                val exists = expenseDao.existsRecurringOccurrenceExpense(bill.id, occEpochDay)
                                if (!exists) {
                                    val occMillis = occ.atZone(zone).toInstant().toEpochMilli()
                                    expenseDao.insertExpense(
                                        ExpenseEntity(
                                            title = bill.title,
                                            amount = bill.amount,
                                            category = bill.category.ifBlank { "Bills" },
                                            dateEpochDay = occEpochDay,
                                            createdAtMillis = occMillis,
                                            linkedRecurringBillId = bill.id,
                                            recurringOccurrenceEpochDay = occEpochDay
                                        )
                                    )
                                    newlyPostedBills.add(bill to occEpochDay)
                                    postedCount++
                                }
                            }
                            val nextDueDate = RecurringEngine.nextScheduledDueDateAfter(bill, now)
                            val maxOccEpochDay = dueOccurrences.maxOf { it.toLocalDate().toEpochDay() }
                            recurringBillDao.updateBill(
                                bill.copy(
                                    dueEpochDay = nextDueDate.toEpochDay(),
                                    lastExecutedEpochDay = maxOccEpochDay,
                                    lastExecutedAtMillis = nowMillis
                                )
                            )
                        }
                    }
                }
                if (newlyPostedBills.isNotEmpty() && snapshot.notificationsEnabled && snapshot.billReminderEnabled) {
                    val currency = currencySymbol.first()
                    newlyPostedBills.forEach { (bill, occEpochDay) ->
                        prefs.recordInAppNotification(
                            eventKey = "bill_posted_${bill.id}_$occEpochDay",
                            type = InAppNotificationType.BILL_POSTED,
                            title = "Recurring Bill Logged: ${bill.title}",
                            message = "${ShiftlyFormatters.formatCurrency(currency, bill.amount)} was automatically added to Expenses."
                        )
                    }
                }
                postedCount
            }
        }

    /**
     * Evaluates legacy recurring income rules if any exist, while delegating recurring bills
     * to [processDueRecurringBills].
     */
    suspend fun processDueRecurringEntries(today: LocalDate = LocalDate.now()): Int = withContext(Dispatchers.IO) {
        val billsPosted = processDueRecurringBills(today.atTime(LocalDateTime.now().toLocalTime()))
        val legacyIncomePosted = recurringMutex.withLock {
            val snapshot = settingsSnapshot.first()
            val incomeRules = snapshot.recurringEntries.filter { it.isIncome }
            if (incomeRules.isEmpty()) return@withLock 0

            var postedCount = 0
            val updatedRules = mutableListOf<RecurringEntrySetting>()

            db.withTransaction {
                val nowMillis = System.currentTimeMillis()
                incomeRules.forEach { entry ->
                    val dueOccurrences = RecurringEngine.computeDueOccurrences(entry, today)
                    if (dueOccurrences.isEmpty()) {
                        updatedRules.add(entry)
                    } else {
                        dueOccurrences.forEach { occ ->
                            extraIncomeDao.insertExtraIncome(
                                ExtraIncomeEntity(
                                    title = entry.title,
                                    amount = entry.amount,
                                    dateEpochDay = occ.dueEpochDay,
                                    sourceRuleId = entry.id,
                                    createdAtMillis = nowMillis
                                )
                            )
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
        billsPosted + legacyIncomePosted
    }

    private suspend fun buildCurrentBackupPayload(includeReceipts: Boolean): BackupPayload = withContext(Dispatchers.IO) {
        // Make sure any loans still living in the legacy DataStore are part of the export.
        migrateLegacyInstallmentLoans()

        val shifts = shiftDao.getAllShiftsOnce()
        val expenses = expenseDao.getAllExpensesOnce()
        val extraIncomes = extraIncomeDao.getAllExtraIncomesOnce()
        val bills = splitBillDao.getAllBillsWithParticipantsOnce()
        val goals = savingsGoalDao.getAllGoalsWithDepositsOnce()
        val recurringBills = recurringBillDao.getAllBillsOnce()
        val allowanceEntries = allowanceDao.getAllEntriesOnce()
        val loans = installmentLoanDao.getAllInstallmentLoansWithPaymentsOnce()
        val settings = settingsSnapshot.first()
        val deductions = paycheckDeductions.first()
        val ious = iouEntries.first()
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

        val backupLoans = loans.map { entry ->
            BackupInstallmentLoan(
                originalId = entry.loan.id,
                loan = entry.loan,
                payments = entry.payments.sortedBy { it.monthNumber }
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
            paycheckDeductions = deductions,
            iouEntries = ious,
            installmentLoans = backupLoans,
            shifts = shifts,
            expenses = backupExpenses,
            extraIncomes = extraIncomes,
            splitBills = backupBills,
            savingsGoals = backupGoals,
            recurringBills = recurringBills,
            allowanceEntries = allowanceEntries,
            receiptFiles = receiptMap
        )
    }

    // Complete Offline JSON Snapshot Export
    suspend fun generateOfflineBackupSummary(): String = withContext(Dispatchers.IO) {
        BackupCodec.encodeJson(buildCurrentBackupPayload(includeReceipts = false))
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
        val receiptsDir = File(appContext.filesDir, "receipts").apply { mkdirs() }
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
        var restoredRecurringBillsCount = 0
        var restoredAllowanceCount = 0
        var restoredLoansCount = 0

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

        // Fold any pending legacy loans into Room first so MERGE de-duplication sees them.
        migrateLegacyInstallmentLoans()

        db.withTransaction {
            if (mode == RestoreMode.REPLACE) {
                expenseDao.deleteAllExpenses()
                extraIncomeDao.deleteAllExtraIncomes()
                shiftDao.deleteAllShifts()
                splitBillDao.deleteAllSplitBills()
                savingsGoalDao.deleteAllSavingsGoals()
                recurringBillDao.deleteAllBills()
                allowanceDao.deleteAllEntries()
                installmentLoanDao.deleteAllLoanPayments()
                installmentLoanDao.deleteAllInstallmentLoans()
            }

            val existingShiftDates = if (mode == RestoreMode.MERGE) {
                shiftDao.getAllShiftsOnce().asSequence()
                    .map { it.dateEpochDay }
                    .toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.shifts.forEach { shift ->
                if (existingShiftDates.add(shift.dateEpochDay)) {
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

            val existingLoanSigs = if (mode == RestoreMode.MERGE) {
                installmentLoanDao.getAllInstallmentLoansOnce().asSequence()
                    .map { BackupCodec.installmentLoanSignature(it) }
                    .toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.installmentLoans.forEach { entry ->
                val sig = BackupCodec.installmentLoanSignature(entry.loan)
                if (mode == RestoreMode.REPLACE || existingLoanSigs.add(sig)) {
                    val newLoanId = installmentLoanDao.insertInstallmentLoan(entry.loan.copy(id = 0L))
                    entry.payments.forEach { payment ->
                        installmentLoanDao.insertLoanPayment(
                            payment.copy(id = 0L, loanId = newLoanId)
                        )
                    }
                    restoredLoansCount++
                }
            }

            val existingRecurringSigs = if (mode == RestoreMode.MERGE) {
                recurringBillDao.getAllBillsOnce().asSequence()
                    .map { BackupCodec.recurringBillSignature(it) }
                    .toMutableSet()
            } else {
                mutableSetOf()
            }

            val oldToNewRecurringBillId = mutableMapOf<Long, Long>()
            payload.recurringBills.forEach { rb ->
                val sig = BackupCodec.recurringBillSignature(rb)
                if (mode == RestoreMode.REPLACE || existingRecurringSigs.add(sig)) {
                    val newRbId = recurringBillDao.insertBill(rb.copy(id = 0L))
                    if (rb.id > 0L) {
                        oldToNewRecurringBillId[rb.id] = newRbId
                    }
                    restoredRecurringBillsCount++
                }
            }

            val existingAllowanceSigs = if (mode == RestoreMode.MERGE) {
                allowanceDao.getAllEntriesOnce().asSequence()
                    .map { BackupCodec.allowanceEntrySignature(it) }
                    .toMutableSet()
            } else {
                mutableSetOf()
            }

            payload.allowanceEntries.forEach { entry ->
                val sig = BackupCodec.allowanceEntrySignature(entry)
                if (mode == RestoreMode.REPLACE || existingAllowanceSigs.add(sig)) {
                    allowanceDao.insertEntry(entry.copy(id = 0L))
                    restoredAllowanceCount++
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
                    val mappedRecurringId = e.linkedRecurringBillId?.let { oldToNewRecurringBillId[it] }
                    val receiptPath = record.receiptFileName?.let { restoredReceiptPaths[it] }
                    expenseDao.insertExpense(
                        e.copy(
                            id = 0L,
                            linkedSplitBillId = mappedBillId,
                            linkedSavingsDepositId = mappedDepositId,
                            linkedRecurringBillId = mappedRecurringId,
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

        val resolvedIous: List<IouEntrySetting>? = when {
            mode == RestoreMode.REPLACE -> payload.iouEntries
            payload.iouEntries.isEmpty() -> null
            else -> {
                val existingIous = iouEntries.first()
                val seenIds = existingIous.map { it.id }.toMutableSet()
                existingIous + payload.iouEntries.filter { seenIds.add(it.id) }
            }
        }

        prefs.applyRestoredSettings(
            userName = payload.userName,
            currencySymbol = payload.currencySymbol,
            defaultHourlyRate = payload.defaultHourlyRate,
            minimumWagePerDay = payload.minimumWagePerDay,
            monthlyBudget = payload.monthlyBudget,
            savingsBuffer = payload.savingsBuffer,
            settings = payload.settings,
            paycheckDeductions = payload.paycheckDeductions,
            iouEntries = resolvedIous,
            installmentLoans = null
        )

        processDueCutoffAllowances()

        val modeLabel = if (mode == RestoreMode.REPLACE) "Replaced with" else "Merged"
        return "$modeLabel $restoredShiftsCount shift(s), $restoredExpensesCount expense(s), " +
            "$restoredBillsCount split bill(s)" +
            (if (restoredGoalsCount > 0) ", $restoredGoalsCount savings goal(s)" else "") +
            (if (restoredLoansCount > 0) ", $restoredLoansCount installment loan(s)" else "") +
            (if (restoredRecurringBillsCount > 0) ", $restoredRecurringBillsCount recurring bill(s)" else "") +
            (if (restoredAllowanceCount > 0) ", $restoredAllowanceCount allowance entry(s)" else "") +
            (if (restoredExtraIncomesCount > 0) ", $restoredExtraIncomesCount extra income(s)" else "") +
            if (restoredReceiptsCount > 0) ", $restoredReceiptsCount receipt(s)" else ""
    }

    private fun prepareExportsDir(): File {
        val exportDir = File(appContext.cacheDir, "exports").apply { mkdirs() }
        exportDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                file.delete()
            }
        }
        return exportDir
    }

    suspend fun pruneOrphanedStorage() = withContext(Dispatchers.IO) {
        val referencedPaths = expenseDao.getAllReferencedReceiptPathsOnce().toSet()
        ReceiptStorageHelper.pruneOrphanedReceipts(appContext, referencedPaths)
        ReceiptStorageHelper.deleteLegacyProfilePhotosDir(appContext)
        val exportDir = File(appContext.cacheDir, "exports")
        if (exportDir.exists() && exportDir.isDirectory) {
            exportDir.listFiles()?.forEach { file ->
                if (file.isFile) {
                    file.delete()
                }
            }
        }
    }

    suspend fun generateShiftsCsvExportFile(): File = withContext(Dispatchers.IO) {
        val shifts = allShifts.first()
        val currency = currencySymbol.first()
        val exportDir = prepareExportsDir()
        val csvFile = File(exportDir, "shiftly_shifts.csv")
        val csvContent = BackupCodec.buildShiftsCsvExport(
            shifts = shifts,
            currencySymbol = currency
        )
        csvFile.writeText(csvContent)
        csvFile
    }

    suspend fun generateExpensesCsvExportFile(): File = withContext(Dispatchers.IO) {
        val expenses = allExpenses.first()
        val currency = currencySymbol.first()
        val exportDir = prepareExportsDir()
        val csvFile = File(exportDir, "shiftly_expenses.csv")
        val csvContent = BackupCodec.buildExpensesCsvExport(
            expenses = expenses,
            currencySymbol = currency
        )
        csvFile.writeText(csvContent)
        csvFile
    }

    companion object {
        @SuppressLint("StaticFieldLeak")
        @Volatile
        private var INSTANCE: ShiftlyRepository? = null

        fun getInstance(context: Context): ShiftlyRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: ShiftlyRepository(context.applicationContext ?: context).also { INSTANCE = it }
            }
        }
    }
}
