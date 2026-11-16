package com.example.shiftly.data.backup

import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.PreferenceJsonCodec
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitParticipantEntity
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
    val shifts: List<ShiftEntity> = emptyList(),
    val expenses: List<BackupExpenseRecord> = emptyList(),
    val extraIncomes: List<ExtraIncomeEntity> = emptyList(),
    val splitBills: List<BackupSplitBill> = emptyList(),
    val savingsGoals: List<BackupSavingsGoal> = emptyList(),
    val receiptFiles: Map<String, ByteArray> = emptyMap()
) {
    companion object {
        const val CURRENT_VERSION = 5
    }
}

/**
 * Pure Kotlin/JVM codec for Shiftly JSON, ZIP, and CSV backups/exports.
 */
object BackupCodec {
    private const val BACKUP_JSON_ENTRY = "shiftly_backup.json"
    private const val RECEIPTS_DIR_PREFIX = "receipts/"
    private const val MAX_ZIP_ENTRY_BYTES = 15 * 1024 * 1024 // 15 MB per entry safety cap

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
                        put("categoryBudgets", JSONArray(PreferenceJsonCodec.encodeCategoryBudgets(s.categoryBudgets)))
                        put("managedCategories", JSONArray(PreferenceJsonCodec.encodeManagedCategories(s.managedCategories)))
                        put("recurringEntries", JSONArray(PreferenceJsonCodec.encodeRecurringEntries(s.recurringEntries)))
                        put("shiftTimePresets", JSONArray(PreferenceJsonCodec.encodeShiftTimePresets(s.shiftTimePresets)))
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
            ShiftlySettingsSnapshot(
                payCycleSchedule = s.optString("payCycleSchedule", "15th & 30th"),
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
                shiftTimePresets = PreferenceJsonCodec.decodeShiftTimePresets(s.optJSONArray("shiftTimePresets")?.toString())
            )
        }

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
                            linkedSavingsDepositId = linkedDepositId
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
            shifts = shifts,
            expenses = expenses,
            extraIncomes = extraIncomes,
            splitBills = splitBills,
            savingsGoals = savingsGoals
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
