package com.example.shiftly.data.backup

import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.InstallmentLoanEntity
import com.example.shiftly.data.local.IouEntrySetting
import com.example.shiftly.data.local.LoanPaymentEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.PaycheckDeductionsSetting
import com.example.shiftly.data.local.PreferenceJsonCodec
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.local.normalizedOrNull
import com.example.shiftly.data.local.toInstallmentLoanEntity
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.LocalDate
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.math.roundToLong
import org.json.JSONArray
import org.json.JSONObject

enum class RestoreMode {
    MERGE,
    REPLACE
}

data class BackupSplitBill(
    val originalId: Long,
    val bill: SplitBillEntity,
    val participants: List<SplitParticipantEntity>
)

data class BackupSavingsGoal(
    val originalId: Long,
    val goal: SavingsGoalEntity,
    val deposits: List<SavingsDepositEntity>
)

data class BackupInstallmentLoan(
    val originalId: Long,
    val loan: InstallmentLoanEntity,
    val payments: List<LoanPaymentEntity>
)

data class BackupExpenseRecord(
    val expense: ExpenseEntity,
    val receiptFileName: String? = null
)

data class BackupPayload(
    val version: Int = CURRENT_VERSION,
    val exportedAtMillis: Long = System.currentTimeMillis(),
    val userName: String? = null,
    val currencySymbol: String? = null,
    val defaultHourlyRate: Double? = null,
    val minimumWagePerDay: Double? = null,
    val monthlyBudget: Double? = null,
    val savingsBuffer: Double? = null,
    val settings: ShiftlySettingsSnapshot? = null,
    val paycheckDeductions: PaycheckDeductionsSetting? = null,
    val iouEntries: List<IouEntrySetting> = emptyList(),
    val installmentLoans: List<BackupInstallmentLoan> = emptyList(),
    val shifts: List<ShiftEntity> = emptyList(),
    val expenses: List<BackupExpenseRecord> = emptyList(),
    val extraIncomes: List<ExtraIncomeEntity> = emptyList(),
    val splitBills: List<BackupSplitBill> = emptyList(),
    val savingsGoals: List<BackupSavingsGoal> = emptyList(),
    val recurringBills: List<RecurringBillEntity> = emptyList(),
    val allowanceEntries: List<AllowanceEntryEntity> = emptyList(),
    val receiptFiles: Map<String, ByteArray> = emptyMap()
) {
    companion object {
        const val CURRENT_VERSION = 7
    }
}

/**
 * Pure Kotlin/JVM codec for Shiftly JSON, ZIP, and CSV backups/exports.
 */
object BackupCodec {
    private const val BACKUP_JSON_ENTRY = "shiftly_backup.json"
    private const val RECEIPTS_DIR_PREFIX = "receipts/"
    private const val MAX_ZIP_ENTRY_BYTES = 15 * 1024 * 1024 // 15 MB per entry safety cap

    /** JSON key for Room-based installment loans (v7+). The legacy `installmentLoans` key is read-only. */
    private const val LOAN_CONTRACTS_KEY = "loanContracts"
    private const val LEGACY_LOANS_KEY = "installmentLoans"

    fun encodeJson(payload: BackupPayload): String {
        val root = JSONObject().apply {
            put("app", "Shiftly")
            put("version", BackupPayload.CURRENT_VERSION)
            put("exportedAtMillis", payload.exportedAtMillis)
            if (!payload.userName.isNullOrBlank()) put("userName", payload.userName)
            if (!payload.currencySymbol.isNullOrBlank()) put("currencySymbol", payload.currencySymbol)
            payload.defaultHourlyRate?.takeIf { it.isFinite() && it > 0.0 }?.let {
                put("defaultHourlyRate", it)
            }
            payload.minimumWagePerDay?.takeIf { it.isFinite() && it >= 0.0 }?.let {
                put("minimumWagePerDay", it)
            }
            payload.monthlyBudget?.takeIf { it.isFinite() && it >= 0.0 }?.let {
                put("monthlyBudget", it)
            }
            payload.savingsBuffer?.takeIf { it.isFinite() && it >= 0.0 }?.let {
                put("savingsBuffer", it)
            }

            payload.settings?.let { s ->
                put(
                    "settings",
                    JSONObject().apply {
                        put("payCycleSchedule", s.payCycleSchedule)
                        put("cutoffMode", s.cutoffMode)
                        put("cutoffDay1", s.cutoffDay1)
                        put("cutoffDay2", s.cutoffDay2)
                        put("cutoffWeeklyDay", s.cutoffWeeklyDay)
                        put("legacyRecurringMigrated", s.legacyRecurringMigrated)
                        put("paydayReminderEnabled", s.paydayReminderEnabled)
                        put("shiftReminderEnabled", s.shiftReminderEnabled)
                        put("billReminderEnabled", s.billReminderEnabled)
                        put("savingsReminderEnabled", s.savingsReminderEnabled)
                        put("notificationsEnabled", s.notificationsEnabled)
                        put("nightDiffStartHour", s.nightDiffStartHour)
                        put("nightDiffMultiplier", s.nightDiffMultiplier)
                        put("overtimeMultiplier", s.overtimeMultiplier)
                        put("savingsGoalTitle", s.savingsGoalTitle)
                        put("savingsGoalTarget", s.savingsGoalTarget)
                        put("jobTitle", s.jobTitle)
                        put("workplace", s.workplace)
                        put("workType", s.workType)
                        put("targetWeeklyHours", s.targetWeeklyHours)
                        put("categoryBudgets", JSONArray(PreferenceJsonCodec.encodeCategoryBudgets(s.categoryBudgets)))
                        put("managedCategories", JSONArray(PreferenceJsonCodec.encodeManagedCategories(s.managedCategories)))
                        put("recurringEntries", JSONArray(PreferenceJsonCodec.encodeRecurringEntries(s.recurringEntries)))
                        put("shiftTimePresets", JSONArray(PreferenceJsonCodec.encodeShiftTimePresets(s.shiftTimePresets)))
                    }
                )
            }

            payload.paycheckDeductions?.let { d ->
                put(
                    "paycheckDeductions",
                    JSONObject().apply {
                        put("sssEnabled", d.sssEnabled)
                        put("sssAmount", d.sssAmount)
                        put("philhealthEnabled", d.philhealthEnabled)
                        put("philhealthAmount", d.philhealthAmount)
                        put("pagibigEnabled", d.pagibigEnabled)
                        put("pagibigAmount", d.pagibigAmount)
                        put("taxEnabled", d.taxEnabled)
                        put("taxPercent", d.taxPercent)
                        put("customEnabled", d.customEnabled)
                        put("customAmount", d.customAmount)
                    }
                )
            }

            if (payload.iouEntries.isNotEmpty()) {
                put("iouEntries", JSONArray(PreferenceJsonCodec.encodeIouEntries(payload.iouEntries)))
            }

            if (payload.installmentLoans.isNotEmpty()) {
                put(
                    LOAN_CONTRACTS_KEY,
                    JSONArray().apply {
                        payload.installmentLoans.forEach { entry ->
                            val l = entry.loan
                            put(
                                JSONObject().apply {
                                    put("id", entry.originalId)
                                    put("gadgetName", l.gadgetName)
                                    put("cashPrice", l.cashPrice)
                                    put("downpayment", l.downpayment)
                                    put("installmentPerMonth", l.installmentPerMonth)
                                    put("termsInMonths", l.termsInMonths)
                                    put("paidMonths", l.paidMonths)
                                    put("startDateMillis", l.startDateMillis)
                                    put("monthlyDueDay", l.monthlyDueDay)
                                    put("firstDueDateMillis", l.firstDueDateMillis)
                                    put(
                                        "payments",
                                        JSONArray().apply {
                                            entry.payments.forEach { p ->
                                                put(
                                                    JSONObject().apply {
                                                        put("monthNumber", p.monthNumber)
                                                        put("amountPaid", p.amountPaid)
                                                        put("datePaidMillis", p.datePaidMillis)
                                                    }
                                                )
                                            }
                                        }
                                    )
                                }
                            )
                        }
                    }
                )
            }

            put(
                "shifts",
                JSONArray().apply {
                    payload.shifts.forEach { s ->
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
                    payload.expenses.forEach { record ->
                        val e = record.expense
                        put(
                            JSONObject().apply {
                                put("title", e.title)
                                put("amount", e.amount)
                                put("category", e.category)
                                put("dateEpochDay", e.dateEpochDay)
                                put("createdAtMillis", e.createdAtMillis)
                                if (e.linkedSplitBillId != null) {
                                    put("linkedSplitBillId", e.linkedSplitBillId)
                                }
                                if (e.linkedSavingsDepositId != null) {
                                    put("linkedSavingsDepositId", e.linkedSavingsDepositId)
                                }
                                if (e.linkedRecurringBillId != null) {
                                    put("linkedRecurringBillId", e.linkedRecurringBillId)
                                }
                                if (e.recurringOccurrenceEpochDay != null) {
                                    put("recurringOccurrenceEpochDay", e.recurringOccurrenceEpochDay)
                                }
                                val receiptName = sanitizeReceiptFileName(
                                    record.receiptFileName ?: e.receiptImagePath
                                )
                                if (!receiptName.isNullOrBlank()) {
                                    put("receiptFileName", receiptName)
                                }
                            }
                        )
                    }
                }
            )

            put(
                "extraIncomes",
                JSONArray().apply {
                    payload.extraIncomes.forEach { inc ->
                        put(
                            JSONObject().apply {
                                put("title", inc.title)
                                put("amount", inc.amount)
                                put("dateEpochDay", inc.dateEpochDay)
                                if (!inc.sourceRuleId.isNullOrBlank()) {
                                    put("sourceRuleId", inc.sourceRuleId)
                                }
                                put("createdAtMillis", inc.createdAtMillis)
                            }
                        )
                    }
                }
            )

            put(
                "splitBills",
                JSONArray().apply {
                    payload.splitBills.forEach { entry ->
                        val b = entry.bill
                        put(
                            JSONObject().apply {
                                put("id", entry.originalId)
                                put("title", b.title)
                                put("totalAmount", b.totalAmount)
                                put("personCount", b.personCount)
                                put("perPersonAmount", b.perPersonAmount)
                                put("dateEpochDay", b.dateEpochDay)
                                put("isLoggedToExpense", b.isLoggedToExpense)
                                put(
                                    "participants",
                                    JSONArray().apply {
                                        entry.participants.forEach { p ->
                                            put(
                                                JSONObject().apply {
                                                    put("personLabel", p.personLabel)
                                                    put("amountOwed", p.amountOwed)
                                                    put("hasPaid", p.hasPaid)
                                                    put("isCurrentUser", p.isCurrentUser)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )

            put(
                "savingsGoals",
                JSONArray().apply {
                    payload.savingsGoals.forEach { entry ->
                        val g = entry.goal
                        put(
                            JSONObject().apply {
                                put("id", entry.originalId)
                                put("title", g.title)
                                put("targetAmount", g.targetAmount)
                                put("targetDateEpochDay", g.targetDateEpochDay)
                                put("isActive", g.isActive)
                                put("createdAtMillis", g.createdAtMillis)
                                put(
                                    "deposits",
                                    JSONArray().apply {
                                        entry.deposits.forEach { d ->
                                            put(
                                                JSONObject().apply {
                                                    put("id", d.id)
                                                    put("amount", d.amount)
                                                    put("timestampMillis", d.timestampMillis)
                                                    put("note", d.note)
                                                }
                                            )
                                        }
                                    }
                                )
                            }
                        )
                    }
                }
            )

            put(
                "recurringBills",
                JSONArray().apply {
                    payload.recurringBills.forEach { rb ->
                        put(
                            JSONObject().apply {
                                put("id", rb.id)
                                put("title", rb.title)
                                put("amount", rb.amount)
                                put("category", rb.category)
                                put("frequency", rb.frequency)
                                put("dueEpochDay", rb.dueEpochDay)
                                put("dueHour", rb.dueHour)
                                put("dueMinute", rb.dueMinute)
                                put("isEnabled", rb.isEnabled)
                                put("lastExecutedEpochDay", rb.lastExecutedEpochDay)
                                put("lastExecutedAtMillis", rb.lastExecutedAtMillis)
                                put("createdAtMillis", rb.createdAtMillis)
                            }
                        )
                    }
                }
            )

            put(
                "allowanceEntries",
                JSONArray().apply {
                    payload.allowanceEntries.forEach { a ->
                        put(
                            JSONObject().apply {
                                put("title", a.title)
                                put("amount", a.amount)
                                put("source", a.source)
                                put("dateEpochDay", a.dateEpochDay)
                                if (!a.cutoffKey.isNullOrBlank()) {
                                    put("cutoffKey", a.cutoffKey)
                                }
                                put("createdAtMillis", a.createdAtMillis)
                            }
                        )
                    }
                }
            )
        }
        return root.toString(2)
    }

    fun decodeJson(rawJson: String): BackupPayload {
        val root = JSONObject(rawJson.trim())
        val appMarker = root.optString("app", "")
        if (appMarker.isNotBlank() && !appMarker.equals("Shiftly", ignoreCase = true)) {
            throw IllegalArgumentException("File is not a Shiftly backup.")
        }

        val version = root.optInt("version", 2)
        val exportedAt = root.optLong("exportedAtMillis", System.currentTimeMillis())
        val userName = root.optString("userName", "").trim().takeIf { it.isNotEmpty() }
        val currency = root.optString("currencySymbol", "").trim().takeIf { it.isNotEmpty() }
        val rate = root.optDouble("defaultHourlyRate", Double.NaN)
            .takeIf { it.isFinite() && it > 0.0 }
        val minWage = root.optDouble("minimumWagePerDay", Double.NaN)
            .takeIf { it.isFinite() && it >= 0.0 }
        val monthlyBudget = root.optDouble("monthlyBudget", Double.NaN)
            .takeIf { it.isFinite() && it >= 0.0 }
        val savingsBuffer = root.optDouble("savingsBuffer", Double.NaN)
            .takeIf { it.isFinite() && it >= 0.0 }

        val settingsObj = root.optJSONObject("settings")
        val settings = settingsObj?.let { s ->
            val goalTarget = s.optDouble("savingsGoalTarget", 0.0)
                .takeIf { it.isFinite() } ?: 0.0
            val legacyPayday = s.optBoolean("paydayReminderEnabled", true)
            val payCycle = s.optString("payCycleSchedule", "15th & 30th")
            val fallbackCutoff = CutoffScheduleConfig.fromLegacySchedule(payCycle)
            val cutoffConfig = CutoffScheduleConfig(
                mode = s.optString("cutoffMode", fallbackCutoff.mode).ifBlank { fallbackCutoff.mode },
                day1 = s.optInt("cutoffDay1", fallbackCutoff.day1),
                day2 = s.optInt("cutoffDay2", fallbackCutoff.day2),
                weeklyDayOfWeek = s.optInt("cutoffWeeklyDay", fallbackCutoff.weeklyDayOfWeek)
            ).normalized()
            val targetWeeklyHours = s.optDouble("targetWeeklyHours", 40.0)
                .takeIf { it.isFinite() && it >= 0.0 } ?: 40.0
            ShiftlySettingsSnapshot(
                payCycleSchedule = payCycle,
                paydayReminderEnabled = legacyPayday,
                shiftReminderEnabled = s.optBoolean("shiftReminderEnabled", true),
                billReminderEnabled = s.optBoolean("billReminderEnabled", true),
                savingsReminderEnabled = s.optBoolean("savingsReminderEnabled", legacyPayday),
                notificationsEnabled = s.optBoolean("notificationsEnabled", true),
                nightDiffStartHour = 22,
                nightDiffMultiplier = 1.10,
                overtimeMultiplier = 1.25,
                savingsGoalTitle = s.optString("savingsGoalTitle", ""),
                savingsGoalTarget = goalTarget.coerceAtLeast(0.0),
                categoryBudgets = parseCategoryBudgetArray(s.optJSONArray("categoryBudgets")),
                managedCategories = parseManagedCategoryArray(s.optJSONArray("managedCategories")),
                recurringEntries = parseRecurringEntryArray(s.optJSONArray("recurringEntries")),
                shiftTimePresets = PreferenceJsonCodec.decodeShiftTimePresets(s.optJSONArray("shiftTimePresets")?.toString()),
                cutoffMode = cutoffConfig.mode,
                cutoffDay1 = cutoffConfig.day1,
                cutoffDay2 = cutoffConfig.day2,
                cutoffWeeklyDay = cutoffConfig.weeklyDayOfWeek,
                legacyRecurringMigrated = s.optBoolean("legacyRecurringMigrated", false),
                jobTitle = s.optString("jobTitle", "").trim(),
                workplace = s.optString("workplace", "").trim(),
                workType = s.optString("workType", "Full-Time").trim().ifEmpty { "Full-Time" },
                targetWeeklyHours = targetWeeklyHours
            )
        }

        val deductionsObj = root.optJSONObject("paycheckDeductions")
        val paycheckDeductions = deductionsObj?.let { d ->
            PaycheckDeductionsSetting(
                sssEnabled = d.optBoolean("sssEnabled", false),
                sssAmount = d.optDouble("sssAmount", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                philhealthEnabled = d.optBoolean("philhealthEnabled", false),
                philhealthAmount = d.optDouble("philhealthAmount", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                pagibigEnabled = d.optBoolean("pagibigEnabled", false),
                pagibigAmount = d.optDouble("pagibigAmount", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                taxEnabled = d.optBoolean("taxEnabled", false),
                taxPercent = d.optDouble("taxPercent", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0,
                customEnabled = d.optBoolean("customEnabled", false),
                customAmount = d.optDouble("customAmount", 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
            )
        }

        val iouEntries = PreferenceJsonCodec.decodeIouEntries(root.optJSONArray("iouEntries")?.toString())

        val loanContractsArray = root.optJSONArray(LOAN_CONTRACTS_KEY) ?: JSONArray()
        val loanContracts = buildList {
            for (i in 0 until loanContractsArray.length()) {
                val obj = loanContractsArray.optJSONObject(i) ?: continue
                val rawLoan = InstallmentLoanEntity(
                    id = 0L,
                    gadgetName = obj.optString("gadgetName", ""),
                    cashPrice = obj.optDouble("cashPrice", 0.0),
                    downpayment = obj.optDouble("downpayment", 0.0),
                    installmentPerMonth = obj.optDouble("installmentPerMonth", Double.NaN),
                    termsInMonths = obj.optInt("termsInMonths", 0),
                    paidMonths = obj.optInt("paidMonths", 0),
                    startDateMillis = obj.optLong("startDateMillis", 0L),
                    monthlyDueDay = obj.optInt("monthlyDueDay", 1),
                    firstDueDateMillis = obj.optLong("firstDueDateMillis", 0L)
                )
                val loan = rawLoan.normalizedOrNull() ?: continue
                val paymentsArray = obj.optJSONArray("payments") ?: JSONArray()
                val payments = buildList {
                    for (j in 0 until paymentsArray.length()) {
                        val pObj = paymentsArray.optJSONObject(j) ?: continue
                        val amount = pObj.optDouble("amountPaid", Double.NaN)
                        val month = pObj.optInt("monthNumber", 0)
                        if (!amount.isFinite() || amount < 0.0 || month <= 0) continue
                        add(
                            LoanPaymentEntity(
                                id = 0L,
                                loanId = 0L,
                                monthNumber = month,
                                amountPaid = amount,
                                datePaidMillis = pObj.optLong("datePaidMillis", 0L)
                            )
                        )
                    }
                }
                add(
                    BackupInstallmentLoan(
                        originalId = obj.optLong("id", (i + 1).toLong()),
                        loan = loan,
                        payments = payments
                    )
                )
            }
        }

        // Backups made before the Room-based tracker stored loans in the older DataStore shape.
        val legacyLoans = PreferenceJsonCodec
            .decodeInstallmentLoans(root.optJSONArray(LEGACY_LOANS_KEY)?.toString())
            .mapIndexedNotNull { index, legacy ->
                legacy.toInstallmentLoanEntity().normalizedOrNull()?.let { loan ->
                    BackupInstallmentLoan(
                        originalId = -(index + 1).toLong(),
                        loan = loan,
                        payments = emptyList()
                    )
                }
            }
        val installmentLoans = loanContracts + legacyLoans

        val shiftsArray = root.optJSONArray("shifts") ?: JSONArray()
        val shifts = buildList {
            for (i in 0 until shiftsArray.length()) {
                val obj = shiftsArray.optJSONObject(i) ?: continue
                val durationMinutes = obj.getInt("durationMinutes")
                val lockedRate = obj.getDouble("lockedHourlyRate")
                val totalEarned = obj.getDouble("totalEarned")
                if (durationMinutes < 0 ||
                    !lockedRate.isFinite() || lockedRate < 0.0 ||
                    !totalEarned.isFinite() || totalEarned < 0.0
                ) {
                    continue
                }
                add(
                    ShiftEntity(
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        timeInMillis = obj.getLong("timeInMillis"),
                        timeOutMillis = obj.getLong("timeOutMillis"),
                        durationMinutes = durationMinutes,
                        lockedHourlyRate = lockedRate,
                        totalEarned = totalEarned,
                        overtimeNote = obj.optString("overtimeNote", ""),
                        dayType = obj.optString("dayType", "ORDINARY")
                    )
                )
            }
        }

        val expensesArray = root.optJSONArray("expenses") ?: JSONArray()
        val expenses = buildList {
            for (i in 0 until expensesArray.length()) {
                val obj = expensesArray.optJSONObject(i) ?: continue
                val title = obj.getString("title").trim()
                val amount = obj.getDouble("amount")
                if (title.isEmpty() || !amount.isFinite() || amount <= 0.0) continue
                val linkedId = if (obj.has("linkedSplitBillId") && !obj.isNull("linkedSplitBillId")) {
                    obj.getLong("linkedSplitBillId")
                } else {
                    null
                }
                val linkedDepositId = if (obj.has("linkedSavingsDepositId") && !obj.isNull("linkedSavingsDepositId")) {
                    obj.getLong("linkedSavingsDepositId")
                } else {
                    null
                }
                val linkedRecurringId = if (obj.has("linkedRecurringBillId") && !obj.isNull("linkedRecurringBillId")) {
                    obj.getLong("linkedRecurringBillId")
                } else {
                    null
                }
                val recurringOccEpoch = if (obj.has("recurringOccurrenceEpochDay") && !obj.isNull("recurringOccurrenceEpochDay")) {
                    obj.getLong("recurringOccurrenceEpochDay")
                } else {
                    null
                }
                val receiptFileName = sanitizeReceiptFileName(obj.optString("receiptFileName", ""))
                add(
                    BackupExpenseRecord(
                        expense = ExpenseEntity(
                            title = title,
                            amount = amount,
                            category = obj.optString("category", "Other").ifBlank { "Other" },
                            dateEpochDay = obj.getLong("dateEpochDay"),
                            linkedSplitBillId = linkedId,
                            createdAtMillis = obj.optLong("createdAtMillis", 0L),
                            linkedSavingsDepositId = linkedDepositId,
                            linkedRecurringBillId = linkedRecurringId,
                            recurringOccurrenceEpochDay = recurringOccEpoch
                        ),
                        receiptFileName = receiptFileName
                    )
                )
            }
        }

        val extraIncomesArray = root.optJSONArray("extraIncomes") ?: JSONArray()
        val extraIncomes = buildList {
            for (i in 0 until extraIncomesArray.length()) {
                val obj = extraIncomesArray.optJSONObject(i) ?: continue
                val title = obj.getString("title").trim()
                val amount = obj.getDouble("amount")
                if (title.isEmpty() || !amount.isFinite() || amount <= 0.0) continue
                add(
                    ExtraIncomeEntity(
                        title = title,
                        amount = amount,
                        dateEpochDay = obj.getLong("dateEpochDay"),
                        sourceRuleId = obj.optString("sourceRuleId", "").takeIf { it.isNotBlank() },
                        createdAtMillis = obj.optLong("createdAtMillis", 0L)
                    )
                )
            }
        }

        val billsArray = root.optJSONArray("splitBills") ?: JSONArray()
        val splitBills = buildList {
            for (i in 0 until billsArray.length()) {
                val obj = billsArray.optJSONObject(i) ?: continue
                val totalAmount = obj.getDouble("totalAmount")
                val personCount = obj.getInt("personCount")
                val perPersonAmount = obj.getDouble("perPersonAmount")
                if (!totalAmount.isFinite() || totalAmount <= 0.0 ||
                    personCount < 2 ||
                    !perPersonAmount.isFinite() || perPersonAmount < 0.0
                ) {
                    continue
                }
                val origId = obj.optLong("id", (i + 1).toLong())
                val bill = SplitBillEntity(
                    title = obj.getString("title").trim().ifEmpty { "Split Bill" },
                    totalAmount = totalAmount,
                    personCount = personCount,
                    perPersonAmount = perPersonAmount,
                    dateEpochDay = obj.getLong("dateEpochDay"),
                    isLoggedToExpense = obj.optBoolean("isLoggedToExpense", false)
                )
                val pArray = obj.optJSONArray("participants") ?: JSONArray()
                val participants = buildList {
                    for (j in 0 until pArray.length()) {
                        val pObj = pArray.optJSONObject(j) ?: continue
                        val label = pObj.getString("personLabel").trim()
                        val amountOwed = pObj.getDouble("amountOwed")
                        if (label.isEmpty() || !amountOwed.isFinite() || amountOwed < 0.0) continue
                        val isSelf = if (pObj.has("isCurrentUser")) {
                            pObj.optBoolean("isCurrentUser", false)
                        } else {
                            label.equals("You", ignoreCase = true)
                        }
                        add(
                            SplitParticipantEntity(
                                billId = 0L,
                                personLabel = label,
                                amountOwed = amountOwed,
                                hasPaid = pObj.optBoolean("hasPaid", false),
                                isCurrentUser = isSelf
                            )
                        )
                    }
                }
                add(BackupSplitBill(originalId = origId, bill = bill, participants = participants))
            }
        }

        val goalsArray = root.optJSONArray("savingsGoals") ?: JSONArray()
        val savingsGoals = buildList {
            for (i in 0 until goalsArray.length()) {
                val obj = goalsArray.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val targetAmount = obj.optDouble("targetAmount", Double.NaN)
                val targetDateEpochDay = obj.optLong("targetDateEpochDay", 0L)
                if (title.isEmpty() || !targetAmount.isFinite() || targetAmount <= 0.0) continue
                val origId = obj.optLong("id", (i + 1).toLong())
                val goal = SavingsGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    targetDateEpochDay = targetDateEpochDay,
                    isActive = obj.optBoolean("isActive", i == 0),
                    createdAtMillis = obj.optLong("createdAtMillis", 0L)
                )
                val dArray = obj.optJSONArray("deposits") ?: JSONArray()
                val deposits = buildList {
                    for (j in 0 until dArray.length()) {
                        val dObj = dArray.optJSONObject(j) ?: continue
                        val amount = dObj.optDouble("amount", Double.NaN)
                        val timestampMillis = dObj.optLong("timestampMillis", 0L)
                        if (!amount.isFinite() || amount <= 0.0 || timestampMillis <= 0L) continue
                        add(
                            SavingsDepositEntity(
                                id = dObj.optLong("id", 0L),
                                goalId = 0L,
                                amount = amount,
                                timestampMillis = timestampMillis,
                                note = dObj.optString("note", "")
                            )
                        )
                    }
                }
                add(BackupSavingsGoal(originalId = origId, goal = goal, deposits = deposits))
            }
        }

        val recurringBillsArray = root.optJSONArray("recurringBills") ?: JSONArray()
        val recurringBills = buildList {
            for (i in 0 until recurringBillsArray.length()) {
                val obj = recurringBillsArray.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val amount = obj.optDouble("amount", Double.NaN)
                if (title.isEmpty() || !amount.isFinite() || amount <= 0.0) continue
                add(
                    RecurringBillEntity(
                        id = obj.optLong("id", 0L),
                        title = title,
                        amount = amount,
                        category = obj.optString("category", "Bills").ifBlank { "Bills" },
                        frequency = obj.optString("frequency", "Monthly").ifBlank { "Monthly" },
                        dueEpochDay = obj.optLong("dueEpochDay", LocalDate.now().toEpochDay()),
                        dueHour = obj.optInt("dueHour", 9).coerceIn(0, 23),
                        dueMinute = obj.optInt("dueMinute", 0).coerceIn(0, 59),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        lastExecutedEpochDay = obj.optLong("lastExecutedEpochDay", 0L),
                        lastExecutedAtMillis = obj.optLong("lastExecutedAtMillis", 0L),
                        createdAtMillis = obj.optLong("createdAtMillis", 0L)
                    )
                )
            }
        }

        val allowanceArray = root.optJSONArray("allowanceEntries") ?: JSONArray()
        val allowanceEntries = buildList {
            for (i in 0 until allowanceArray.length()) {
                val obj = allowanceArray.optJSONObject(i) ?: continue
                val title = obj.optString("title", "").trim()
                val amount = obj.optDouble("amount", Double.NaN)
                if (title.isEmpty() || !amount.isFinite() || amount <= 0.0) continue
                val rawSource = obj.optString("source", AllowanceSource.OTHERS).trim()
                val safeSource = if (rawSource in AllowanceSource.ALL) rawSource else AllowanceSource.OTHERS
                add(
                    AllowanceEntryEntity(
                        title = title,
                        amount = amount,
                        source = safeSource,
                        dateEpochDay = obj.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
                        cutoffKey = obj.optString("cutoffKey", "").takeIf { it.isNotBlank() },
                        createdAtMillis = obj.optLong("createdAtMillis", 0L)
                    )
                )
            }
        }

        return BackupPayload(
            version = version,
            exportedAtMillis = exportedAt,
            userName = userName,
            currencySymbol = currency,
            defaultHourlyRate = rate,
            minimumWagePerDay = minWage,
            monthlyBudget = monthlyBudget,
            savingsBuffer = savingsBuffer,
            settings = settings,
            paycheckDeductions = paycheckDeductions,
            iouEntries = iouEntries,
            installmentLoans = installmentLoans,
            shifts = shifts,
            expenses = expenses,
            extraIncomes = extraIncomes,
            splitBills = splitBills,
            savingsGoals = savingsGoals,
            recurringBills = recurringBills,
            allowanceEntries = allowanceEntries
        )
    }

    fun encodeZipBytes(payload: BackupPayload): ByteArray {
        val baos = ByteArrayOutputStream()
        ZipOutputStream(baos).use { zos ->
            val jsonBytes = encodeJson(payload).toByteArray(Charsets.UTF_8)
            zos.putNextEntry(ZipEntry(BACKUP_JSON_ENTRY))
            zos.write(jsonBytes)
            zos.closeEntry()

            payload.receiptFiles.forEach { (rawName, bytes) ->
                val safeName = sanitizeReceiptFileName(rawName)
                if (safeName != null && bytes.isNotEmpty()) {
                    zos.putNextEntry(ZipEntry("$RECEIPTS_DIR_PREFIX$safeName"))
                    zos.write(bytes)
                    zos.closeEntry()
                }
            }
        }
        return baos.toByteArray()
    }

    fun decodeZipOrJsonBytes(rawBytes: ByteArray): BackupPayload {
        if (rawBytes.size >= 2 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()) {
            var jsonContent: String? = null
            val receipts = mutableMapOf<String, ByteArray>()
            ZipInputStream(ByteArrayInputStream(rawBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val name = entry.name
                        if (name == BACKUP_JSON_ENTRY) {
                            jsonContent = readBoundedEntryBytes(zis).toString(Charsets.UTF_8)
                        } else if (name.startsWith(RECEIPTS_DIR_PREFIX)) {
                            val relative = name.removePrefix(RECEIPTS_DIR_PREFIX)
                            val safeName = sanitizeReceiptFileName(relative)
                            if (safeName != null) {
                                val bytes = readBoundedEntryBytes(zis)
                                if (bytes.isNotEmpty()) {
                                    receipts[safeName] = bytes
                                }
                            }
                        }
                    }
                    zis.closeEntry()
                    entry = zis.nextEntry
                }
            }
            val parsedJson = jsonContent
                ?: throw IllegalArgumentException("Backup ZIP is missing $BACKUP_JSON_ENTRY.")
            return decodeJson(parsedJson).copy(receiptFiles = receipts)
        }
        val text = rawBytes.toString(Charsets.UTF_8)
        return decodeJson(text)
    }

    fun sanitizeReceiptFileName(rawName: String?): String? {
        if (rawName.isNullOrBlank()) return null
        val fileName = rawName.trim().replace('\\', '/').substringAfterLast('/').trim()
        if (fileName.isEmpty() || fileName == "." || fileName == "..") return null
        if (fileName.contains('\u0000')) return null
        return fileName
    }

    fun escapeCsvField(value: String): String {
        val needsQuotes = value.contains(',') ||
            value.contains('"') ||
            value.contains('\n') ||
            value.contains('\r')
        return if (needsQuotes) {
            "\"" + value.replace("\"", "\"\"") + "\""
        } else {
            value
        }
    }

    fun buildCsvExport(
        shifts: List<ShiftEntity>,
        extraIncomes: List<ExtraIncomeEntity>,
        expenses: List<ExpenseEntity>,
        currencySymbol: String,
        allowanceEntries: List<AllowanceEntryEntity> = emptyList(),
        locale: Locale = Locale.US
    ): String = buildString {
        appendLine("Type,Date,Title_Or_Note,Category_Or_DayType,Duration_Min,${escapeCsvField("Amount ($currencySymbol)")}")
        shifts.forEach { s ->
            val date = LocalDate.ofEpochDay(s.dateEpochDay)
            val note = escapeCsvField(s.overtimeNote)
            val dayType = escapeCsvField(s.dayType)
            val amount = String.format(locale, "%.2f", s.totalEarned)
            appendLine("Shift,$date,$note,$dayType,${s.durationMinutes},$amount")
        }
        allowanceEntries
            .filter { it.cutoffKey == null }
            .forEach { a ->
                val date = LocalDate.ofEpochDay(a.dateEpochDay)
                val title = escapeCsvField(a.title)
                val source = escapeCsvField(a.source)
                val amount = String.format(locale, "%.2f", a.amount)
                appendLine("Allowance,$date,$title,$source,0,$amount")
            }
        extraIncomes.forEach { inc ->
            val date = LocalDate.ofEpochDay(inc.dateEpochDay)
            val title = escapeCsvField(inc.title)
            val amount = String.format(locale, "%.2f", inc.amount)
            appendLine("ExtraIncome,$date,$title,Recurring,0,$amount")
        }
        expenses.forEach { e ->
            val date = LocalDate.ofEpochDay(e.dateEpochDay)
            val title = escapeCsvField(e.title)
            val category = escapeCsvField(e.category)
            val amount = String.format(locale, "%.2f", e.amount)
            appendLine("Expense,$date,$title,$category,0,-$amount")
        }
    }

    fun buildShiftsCsvExport(
        shifts: List<ShiftEntity>,
        currencySymbol: String,
        locale: Locale = Locale.US
    ): String = buildString {
        appendLine("Date,DayType,Duration_Min,Hourly_Rate(${escapeCsvField(currencySymbol)}),${escapeCsvField("Earned ($currencySymbol)")},Note")
        shifts.forEach { s ->
            val date = LocalDate.ofEpochDay(s.dateEpochDay)
            val dayType = escapeCsvField(s.dayType)
            val rate = String.format(locale, "%.2f", s.lockedHourlyRate)
            val earned = String.format(locale, "%.2f", s.totalEarned)
            val note = escapeCsvField(s.overtimeNote)
            appendLine("$date,$dayType,${s.durationMinutes},$rate,$earned,$note")
        }
    }

    fun buildExpensesCsvExport(
        expenses: List<ExpenseEntity>,
        currencySymbol: String,
        locale: Locale = Locale.US
    ): String = buildString {
        appendLine("Date,Title,Category,${escapeCsvField("Amount ($currencySymbol)")}")
        expenses.forEach { e ->
            val date = LocalDate.ofEpochDay(e.dateEpochDay)
            val title = escapeCsvField(e.title)
            val category = escapeCsvField(e.category)
            val amount = String.format(locale, "%.2f", e.amount)
            appendLine("$date,$title,$category,$amount")
        }
    }

    private fun readBoundedEntryBytes(zis: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var totalRead = 0
        while (true) {
            val read = zis.read(buffer)
            if (read <= 0) break
            totalRead += read
            if (totalRead > MAX_ZIP_ENTRY_BYTES) {
                throw IllegalArgumentException("Backup entry exceeds maximum allowed size.")
            }
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    fun shiftSignature(s: ShiftEntity): String =
        "${s.dateEpochDay}|${s.timeInMillis}|${s.timeOutMillis}"

    fun expenseSignature(e: ExpenseEntity): String =
        "${e.dateEpochDay}|${e.title.trim().lowercase()}|${(e.amount * 100.0).roundToLong()}|${e.category.trim().lowercase()}"

    fun extraIncomeSignature(e: ExtraIncomeEntity): String =
        "${e.dateEpochDay}|${e.title.trim().lowercase()}|${(e.amount * 100.0).roundToLong()}|${e.sourceRuleId.orEmpty()}"

    fun splitBillSignature(b: SplitBillEntity): String =
        "${b.dateEpochDay}|${b.title.trim().lowercase()}|${(b.totalAmount * 100.0).roundToLong()}|${b.personCount}"

    fun savingsGoalSignature(g: SavingsGoalEntity): String =
        "${g.title.trim().lowercase()}|${(g.targetAmount * 100.0).roundToLong()}|${g.targetDateEpochDay}"

    fun recurringBillSignature(b: RecurringBillEntity): String =
        "${b.title.trim().lowercase()}|${(b.amount * 100.0).roundToLong()}|${b.category.trim().lowercase()}|${b.frequency.trim().lowercase()}|${b.dueHour}:${b.dueMinute}"

    fun installmentLoanSignature(l: InstallmentLoanEntity): String =
        "${l.gadgetName.trim().lowercase()}|${(l.cashPrice * 100.0).roundToLong()}|${(l.downpayment * 100.0).roundToLong()}|${(l.installmentPerMonth * 100.0).roundToLong()}|${l.termsInMonths}|${l.firstDueDateMillis}"

    fun allowanceEntrySignature(a: AllowanceEntryEntity): String =
        if (!a.cutoffKey.isNullOrBlank()) {
            "CUTOFF|${a.cutoffKey}"
        } else {
            "MANUAL|${a.dateEpochDay}|${a.title.trim().lowercase()}|${(a.amount * 100.0).roundToLong()}|${a.source.trim().lowercase()}"
        }

    private fun parseCategoryBudgetArray(array: JSONArray?): List<CategoryBudgetSetting> {
        if (array == null) return ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS
        return PreferenceJsonCodec.decodeCategoryBudgets(array.toString())
    }

    private fun parseManagedCategoryArray(array: JSONArray?): List<ManagedCategorySetting> {
        if (array == null) return ShiftlySettingsSnapshot.DEFAULT_MANAGED_CATEGORIES
        return PreferenceJsonCodec.decodeManagedCategories(array.toString())
    }

    private fun parseRecurringEntryArray(array: JSONArray?): List<RecurringEntrySetting> {
        if (array == null) return emptyList()
        return PreferenceJsonCodec.decodeRecurringEntries(array.toString())
    }
}
