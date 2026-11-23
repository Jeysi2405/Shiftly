package com.example.shiftly.data.local

import android.content.Context
import androidx.compose.runtime.Immutable
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(
    name = "shiftly_prefs",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() }
)

enum class InAppNotificationType {
    BILL_DUE,
    BILL_POSTED,
    SHIFT_REMINDER,
    SAVINGS_REMINDER,
    PAYDAY_CUTOFF,
    SPLIT_BILL
}

@Immutable
data class InAppNotificationItem(
    val id: String,
    val eventKey: String,
    val type: InAppNotificationType,
    val title: String,
    val message: String,
    val timestampMillis: Long,
    val isRead: Boolean = false,
    val hasShownToast: Boolean = false
)

@Immutable
data class CategoryBudgetSetting(
    val category: String,
    val monthlyLimit: Double
)

@Immutable
data class ManagedCategorySetting(
    val name: String,
    val isVisible: Boolean
)

@Immutable
data class RecurringEntrySetting(
    val id: String,
    val title: String,
    val amount: Double,
    val frequency: String,
    val isIncome: Boolean,
    val isEnabled: Boolean,
    val category: String = "Other",
    val lastAppliedEpochDay: Long = 0L
)

@Immutable
data class ShiftTimePreset(
    val id: String,
    val label: String,
    val startHour24: Int,
    val startMinute: Int,
    val endHour24: Int,
    val endMinute: Int
)

@Immutable
data class PaycheckDeductionsSetting(
    val sssEnabled: Boolean = false,
    val sssAmount: Double = 0.0,
    val philhealthEnabled: Boolean = false,
    val philhealthAmount: Double = 0.0,
    val pagibigEnabled: Boolean = false,
    val pagibigAmount: Double = 0.0,
    val taxEnabled: Boolean = false,
    val taxPercent: Double = 0.0,
    val customEnabled: Boolean = false,
    val customAmount: Double = 0.0
) {
    fun calculateTotalDeductions(grossPay: Double): Double {
        var total = 0.0
        if (sssEnabled && sssAmount.isFinite() && sssAmount > 0.0) total += sssAmount
        if (philhealthEnabled && philhealthAmount.isFinite() && philhealthAmount > 0.0) total += philhealthAmount
        if (pagibigEnabled && pagibigAmount.isFinite() && pagibigAmount > 0.0) total += pagibigAmount
        if (taxEnabled && taxPercent.isFinite() && taxPercent > 0.0 && grossPay > 0.0) total += (grossPay * (taxPercent / 100.0))
        if (customEnabled && customAmount.isFinite() && customAmount > 0.0) total += customAmount
        return total.coerceAtLeast(0.0)
    }
}

@Immutable
data class IouEntrySetting(
    val id: String = UUID.randomUUID().toString(),
    val personName: String,
    val amount: Double,
    val isLent: Boolean,
    val dateEpochDay: Long = LocalDate.now().toEpochDay(),
    val note: String = "",
    val isSettled: Boolean = false
)

@Immutable
data class InstallmentLoan(
    val id: String = UUID.randomUUID().toString(),
    val itemName: String,
    val lenderOrStore: String = "",
    val installmentAmount: Double,
    val totalInstallments: Int,
    val paidInstallments: Int = 0,
    val downPayment: Double = 0.0,
    val frequency: String = FREQ_MONTHLY,
    val dueDayLabel: String = "",
    val startDateMillis: Long = System.currentTimeMillis(),
    val autoLogExpense: Boolean = true
) {
    val totalFinancedAmount: Double
        get() = (installmentAmount.takeIf { it.isFinite() } ?: 0.0).coerceAtLeast(0.0) *
            totalInstallments.coerceAtLeast(0)

    val totalContractAmount: Double
        get() = totalFinancedAmount +
            (downPayment.takeIf { it.isFinite() } ?: 0.0).coerceAtLeast(0.0)

    val paidAmount: Double
        get() {
            val safeTotal = totalInstallments.coerceAtLeast(0)
            val safePaid = paidInstallments.coerceIn(0, safeTotal)
            val safeInstallment = (installmentAmount.takeIf { it.isFinite() } ?: 0.0).coerceAtLeast(0.0)
            val safeDown = (downPayment.takeIf { it.isFinite() } ?: 0.0).coerceAtLeast(0.0)
            return (safeInstallment * safePaid) + safeDown
        }

    val remainingInstallments: Int
        get() = (totalInstallments.coerceAtLeast(0) - paidInstallments.coerceAtLeast(0)).coerceAtLeast(0)

    val remainingBalance: Double
        get() = ((installmentAmount.takeIf { it.isFinite() } ?: 0.0).coerceAtLeast(0.0) * remainingInstallments)
            .coerceAtLeast(0.0)

    val progressFraction: Float
        get() = (paidInstallments.coerceAtLeast(0).toFloat() / totalInstallments.coerceAtLeast(1))
            .coerceIn(0f, 1f)

    val isCompleted: Boolean
        get() = totalInstallments > 0 && remainingInstallments == 0

    fun frequencyUnitSuffix(): String = when (frequency) {
        FREQ_SEMI_MONTHLY -> "cutoff"
        FREQ_WEEKLY -> "wk"
        FREQ_EVERYDAY -> "day"
        else -> "mo"
    }

    fun remainingTimeLabel(): String {
        val rem = remainingInstallments
        if (rem <= 0) return "Completed"
        return when (frequency) {
            FREQ_SEMI_MONTHLY -> "$rem ${if (rem == 1) "cutoff" else "cutoffs"} left"
            FREQ_WEEKLY -> "$rem ${if (rem == 1) "wk" else "wks"} left"
            FREQ_EVERYDAY -> "$rem ${if (rem == 1) "day" else "days"} left"
            else -> "$rem ${if (rem == 1) "mo" else "mos"} left"
        }
    }

    fun estimatedEndDateLabel(referenceDate: LocalDate = LocalDate.now()): String {
        val rem = remainingInstallments
        if (rem <= 0) return "Completed"
        val endDate = when (frequency) {
            FREQ_SEMI_MONTHLY -> referenceDate.plusMonths(rem.toLong() / 2L).plusDays((rem % 2) * 15L)
            FREQ_WEEKLY -> referenceDate.plusWeeks(rem.toLong())
            FREQ_EVERYDAY -> referenceDate.plusDays(rem.toLong())
            else -> referenceDate.plusMonths(rem.toLong())
        }
        val formatter = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH)
        return "Ends ${endDate.format(formatter)}"
    }

    companion object {
        const val FREQ_MONTHLY = "Monthly"
        const val FREQ_SEMI_MONTHLY = "Semi-Monthly"
        const val FREQ_WEEKLY = "Weekly"
        const val FREQ_EVERYDAY = "Everyday"

        fun normalizeFrequency(raw: String): String = when {
            raw.equals(FREQ_SEMI_MONTHLY, ignoreCase = true) -> FREQ_SEMI_MONTHLY
            raw.equals(FREQ_WEEKLY, ignoreCase = true) -> FREQ_WEEKLY
            raw.equals(FREQ_EVERYDAY, ignoreCase = true) -> FREQ_EVERYDAY
            else -> FREQ_MONTHLY
        }
    }
}

@Immutable
data class NotificationLastDays(
    val lastBillDay: Long = 0L,
    val lastShiftDay: Long = 0L,
    val lastSavingsDay: Long = 0L
)

@Immutable
data class CutoffScheduleConfig(
    val mode: String = MODE_SEMI_MONTHLY,
    val day1: Int = 15,
    val day2: Int = END_OF_MONTH_DAY,
    val weeklyDayOfWeek: Int = 5
) {
    companion object {
        const val MODE_SEMI_MONTHLY = "SEMI_MONTHLY"
        const val MODE_MONTHLY = "MONTHLY"
        const val MODE_WEEKLY = "WEEKLY"
        const val END_OF_MONTH_DAY = 31

        fun formatDayLabel(day: Int): String =
            if (day >= END_OF_MONTH_DAY) "End of month" else ordinalDay(day)

        fun ordinalDay(day: Int): String {
            val d = day.coerceIn(1, 31)
            val mod100 = d % 100
            val suffix = if (mod100 in 11..13) {
                "th"
            } else {
                when (d % 10) {
                    1 -> "st"
                    2 -> "nd"
                    3 -> "rd"
                    else -> "th"
                }
            }
            return "$d$suffix"
        }

        fun fromLegacySchedule(schedule: String): CutoffScheduleConfig {
            return when {
                schedule.equals("Monthly", ignoreCase = true) ->
                    CutoffScheduleConfig(mode = MODE_MONTHLY, day1 = END_OF_MONTH_DAY)
                schedule.startsWith("Weekly", ignoreCase = true) ->
                    CutoffScheduleConfig(mode = MODE_WEEKLY, weeklyDayOfWeek = 5)
                else -> CutoffScheduleConfig(
                    mode = MODE_SEMI_MONTHLY,
                    day1 = 15,
                    day2 = END_OF_MONTH_DAY
                )
            }
        }
    }

    fun normalized(): CutoffScheduleConfig {
        return when (mode) {
            MODE_MONTHLY -> copy(day1 = day1.coerceIn(1, END_OF_MONTH_DAY))
            MODE_WEEKLY -> copy(weeklyDayOfWeek = weeklyDayOfWeek.coerceIn(1, 7))
            else -> {
                val safeD1 = day1.coerceIn(1, 27)
                val safeD2 = day2.coerceIn(safeD1 + 1, END_OF_MONTH_DAY)
                copy(mode = MODE_SEMI_MONTHLY, day1 = safeD1, day2 = safeD2)
            }
        }
    }

    fun displayLabel(locale: Locale = Locale.ENGLISH): String {
        val norm = normalized()
        return when (norm.mode) {
            MODE_MONTHLY -> {
                if (norm.day1 >= END_OF_MONTH_DAY) {
                    "Monthly (End of month)"
                } else {
                    "Monthly (${ordinalDay(norm.day1)})"
                }
            }
            MODE_WEEKLY -> {
                val dow = DayOfWeek.of(norm.weeklyDayOfWeek)
                    .getDisplayName(TextStyle.SHORT, locale)
                "Weekly ($dow)"
            }
            else -> {
                val first = ordinalDay(norm.day1)
                val second = if (norm.day2 >= END_OF_MONTH_DAY) "30th / End" else ordinalDay(norm.day2)
                "$first & $second"
            }
        }
    }

    fun toPayCycleScheduleString(): String {
        val norm = normalized()
        return when (norm.mode) {
            MODE_MONTHLY -> "Monthly"
            MODE_WEEKLY -> {
                val dow = DayOfWeek.of(norm.weeklyDayOfWeek)
                    .getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                "Weekly ($dow)"
            }
            else -> if (norm.day1 == 15 && norm.day2 >= 30) "15th & 30th" else displayLabel()
        }
    }
}

data class ShiftlySettingsSnapshot(
    val payCycleSchedule: String = "15th & 30th",
    val paydayReminderEnabled: Boolean = true,
    val shiftReminderEnabled: Boolean = true,
    val billReminderEnabled: Boolean = true,
    val savingsReminderEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val nightDiffStartHour: Int = 22,
    val nightDiffMultiplier: Double = 1.10,
    val overtimeMultiplier: Double = 1.25,
    val savingsGoalTitle: String = "",
    val savingsGoalTarget: Double = 0.0,
    val categoryBudgets: List<CategoryBudgetSetting> = DEFAULT_CATEGORY_BUDGETS,
    val managedCategories: List<ManagedCategorySetting> = DEFAULT_MANAGED_CATEGORIES,
    val recurringEntries: List<RecurringEntrySetting> = DEFAULT_RECURRING_ENTRIES,
    val shiftTimePresets: List<ShiftTimePreset> = DEFAULT_SHIFT_TIME_PRESETS,
    val appLockEnabled: Boolean = false,
    val appLockMethod: String = "PIN & Fingerprint",
    val hasAppLockPin: Boolean = false,
    val appLockPinHash: String = "",
    val appLockTimeoutSeconds: Int = 0,
    val themeMode: String = "SYSTEM",
    val cutoffMode: String = CutoffScheduleConfig.MODE_SEMI_MONTHLY,
    val cutoffDay1: Int = 15,
    val cutoffDay2: Int = CutoffScheduleConfig.END_OF_MONTH_DAY,
    val cutoffWeeklyDay: Int = 5,
    val legacyRecurringMigrated: Boolean = false,
    val jobTitle: String = "",
    val workplace: String = "",
    val workType: String = "Full-Time",
    val targetWeeklyHours: Double = 40.0
) {
    fun cutoffScheduleConfig(): CutoffScheduleConfig =
        CutoffScheduleConfig(
            mode = cutoffMode,
            day1 = cutoffDay1,
            day2 = cutoffDay2,
            weeklyDayOfWeek = cutoffWeeklyDay
        ).normalized()

    companion object {
        val DEFAULT_CATEGORY_BUDGETS: List<CategoryBudgetSetting> = emptyList()

        val DEFAULT_MANAGED_CATEGORIES = listOf(
            ManagedCategorySetting("Food", true),
            ManagedCategorySetting("Transport", true),
            ManagedCategorySetting("Bills", true),
            ManagedCategorySetting("Supplies", true),
            ManagedCategorySetting("Savings", true),
            ManagedCategorySetting("Split Bill", true),
            ManagedCategorySetting("Other", true)
        )

        val DEFAULT_RECURRING_ENTRIES: List<RecurringEntrySetting> = emptyList()

        val DEFAULT_SHIFT_TIME_PRESETS: List<ShiftTimePreset> = emptyList()
    }
}

/**
 * Pure JSON serializer/deserializer for structured user preferences.
 * Supports decoding legacy delimited strings for seamless upgrades while always writing JSON.
 * Uses single-entry raw-string caches so unrelated preference writes preserve List references.
 */
object PreferenceJsonCodec {
    private class CachedJsonDecode<T>(
        val raw: String,
        val value: List<T>
    )

    @Volatile
    private var cachedCategoryBudgets: CachedJsonDecode<CategoryBudgetSetting>? = null

    @Volatile
    private var cachedManagedCategories: CachedJsonDecode<ManagedCategorySetting>? = null

    @Volatile
    private var cachedRecurringEntries: CachedJsonDecode<RecurringEntrySetting>? = null

    @Volatile
    private var cachedShiftTimePresets: CachedJsonDecode<ShiftTimePreset>? = null

    @Volatile
    private var cachedInAppNotifications: CachedJsonDecode<InAppNotificationItem>? = null

    @Volatile
    private var cachedInstallmentLoans: CachedJsonDecode<InstallmentLoan>? = null

    fun encodeCategoryBudgets(items: List<CategoryBudgetSetting>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("category", item.category.trim())
                        put(
                            "monthlyLimit",
                            item.monthlyLimit.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                        )
                    }
                )
            }
        }.toString()

    fun decodeCategoryBudgets(raw: String?): List<CategoryBudgetSetting> {
        if (raw.isNullOrBlank()) return ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS
        cachedCategoryBudgets?.takeIf { it.raw == raw }?.let { return it.value }

        val trimmed = raw.trim()
        val parsed = if (trimmed.startsWith("[")) {
            runCatching {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val cat = obj.optString("category", "").trim()
                        val limit = obj.optDouble("monthlyLimit", Double.NaN)
                        if (cat.isNotEmpty() && limit.isFinite() && limit >= 0.0) {
                            add(CategoryBudgetSetting(cat, limit))
                        }
                    }
                }
            }.getOrNull()
        } else {
            null
        }
        val result = parsed ?: trimmed.split(";").mapNotNull { entry ->
            val idx = entry.lastIndexOf(':')
            if (idx <= 0 || idx >= entry.length - 1) return@mapNotNull null
            val cat = entry.substring(0, idx).trim()
            val amount = entry.substring(idx + 1).toDoubleOrNull()?.takeIf { it.isFinite() }
                ?: return@mapNotNull null
            if (cat.isEmpty()) null else CategoryBudgetSetting(cat, amount.coerceAtLeast(0.0))
        }.ifEmpty { ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS }

        cachedCategoryBudgets = CachedJsonDecode(raw, result)
        return result
    }

    fun encodeManagedCategories(items: List<ManagedCategorySetting>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("name", item.name.trim())
                        put("isVisible", item.isVisible)
                    }
                )
            }
        }.toString()

    fun decodeManagedCategories(raw: String?): List<ManagedCategorySetting> {
        if (raw.isNullOrBlank()) return ShiftlySettingsSnapshot.DEFAULT_MANAGED_CATEGORIES
        cachedManagedCategories?.takeIf { it.raw == raw }?.let { return it.value }

        val trimmed = raw.trim()
        val parsed = if (trimmed.startsWith("[")) {
            runCatching {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val name = obj.optString("name", "").trim()
                        if (name.isNotEmpty()) {
                            add(ManagedCategorySetting(name, obj.optBoolean("isVisible", true)))
                        }
                    }
                }
            }.getOrNull()?.takeIf { it.isNotEmpty() }
        } else {
            null
        }
        val result = parsed ?: trimmed.split(";").mapNotNull { entry ->
            val idx = entry.lastIndexOf('|')
            if (idx <= 0 || idx >= entry.length - 1) return@mapNotNull null
            val name = entry.substring(0, idx).trim()
            val visible = entry.substring(idx + 1) == "1"
            if (name.isEmpty()) null else ManagedCategorySetting(name, visible)
        }.ifEmpty { ShiftlySettingsSnapshot.DEFAULT_MANAGED_CATEGORIES }

        cachedManagedCategories = CachedJsonDecode(raw, result)
        return result
    }

    fun encodeRecurringEntries(items: List<RecurringEntrySetting>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title.trim())
                        put(
                            "amount",
                            item.amount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                        )
                        put("frequency", item.frequency)
                        put("isIncome", item.isIncome)
                        put("isEnabled", item.isEnabled)
                        put("category", item.category)
                        put("lastAppliedEpochDay", item.lastAppliedEpochDay)
                    }
                )
            }
        }.toString()

    fun decodeRecurringEntries(raw: String?): List<RecurringEntrySetting> {
        if (raw.isNullOrBlank()) return emptyList()
        cachedRecurringEntries?.takeIf { it.raw == raw }?.let { return it.value }

        val trimmed = raw.trim()
        val result = if (trimmed.startsWith("[")) {
            runCatching {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val id = obj.optString("id", "").trim()
                        val title = obj.optString("title", "").trim()
                        val amount = obj.optDouble("amount", Double.NaN)
                        if (id.isNotEmpty() && title.isNotEmpty() && amount.isFinite() && amount > 0.0) {
                            add(
                                RecurringEntrySetting(
                                    id = id,
                                    title = title,
                                    amount = amount,
                                    frequency = obj.optString("frequency", "Monthly"),
                                    isIncome = obj.optBoolean("isIncome", false),
                                    isEnabled = obj.optBoolean("isEnabled", true),
                                    category = obj.optString("category", "Other"),
                                    lastAppliedEpochDay = obj.optLong("lastAppliedEpochDay", 0L)
                                )
                            )
                        }
                    }
                }
            }.getOrDefault(emptyList())
        } else {
            trimmed.split(";").mapNotNull { entry ->
                val parts = entry.split("|")
                if (parts.size == 6) {
                    val amount = parts[2].toDoubleOrNull()?.takeIf { it.isFinite() && it > 0.0 }
                        ?: return@mapNotNull null
                    RecurringEntrySetting(
                        id = parts[0],
                        title = parts[1],
                        amount = amount,
                        frequency = parts[3],
                        isIncome = parts[4] == "1",
                        isEnabled = parts[5] == "1"
                    )
                } else {
                    null
                }
            }
        }

        cachedRecurringEntries = CachedJsonDecode(raw, result)
        return result
    }

    fun encodeShiftTimePresets(items: List<ShiftTimePreset>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("label", item.label.trim())
                        put("startHour24", item.startHour24.coerceIn(0, 23))
                        put("startMinute", item.startMinute.coerceIn(0, 59))
                        put("endHour24", item.endHour24.coerceIn(0, 23))
                        put("endMinute", item.endMinute.coerceIn(0, 59))
                    }
                )
            }
        }.toString()

    fun decodeShiftTimePresets(raw: String?): List<ShiftTimePreset> {
        if (raw.isNullOrBlank()) return ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS
        cachedShiftTimePresets?.takeIf { it.raw == raw }?.let { return it.value }

        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS
        val parsed = runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    val label = obj.optString("label", "").trim()
                    if (id.isNotEmpty() && label.isNotEmpty()) {
                        add(
                            ShiftTimePreset(
                                id = id,
                                label = label,
                                startHour24 = obj.optInt("startHour24", 0).coerceIn(0, 23),
                                startMinute = obj.optInt("startMinute", 0).coerceIn(0, 59),
                                endHour24 = obj.optInt("endHour24", 6).coerceIn(0, 23),
                                endMinute = obj.optInt("endMinute", 0).coerceIn(0, 59)
                            )
                        )
                    }
                }
            }
        }.getOrNull()
        val result = parsed ?: ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS
        cachedShiftTimePresets = CachedJsonDecode(raw, result)
        return result
    }

    fun encodeInAppNotifications(items: List<InAppNotificationItem>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("eventKey", item.eventKey)
                        put("type", item.type.name)
                        put("title", item.title.trim())
                        put("message", item.message.trim())
                        put("timestampMillis", item.timestampMillis)
                        put("isRead", item.isRead)
                        put("hasShownToast", item.hasShownToast)
                    }
                )
            }
        }.toString()

    fun encodeIouEntries(items: List<IouEntrySetting>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("personName", item.personName.trim())
                        put("amount", item.amount)
                        put("isLent", item.isLent)
                        put("dateEpochDay", item.dateEpochDay)
                        put("note", item.note.trim())
                        put("isSettled", item.isSettled)
                    }
                )
            }
        }.toString()

    fun decodeIouEntries(raw: String?): List<IouEntrySetting> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val personName = obj.optString("personName", "").trim()
                    if (personName.isNotEmpty()) {
                        add(
                            IouEntrySetting(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                personName = personName,
                                amount = obj.optDouble("amount", 0.0),
                                isLent = obj.optBoolean("isLent", true),
                                dateEpochDay = obj.optLong("dateEpochDay", LocalDate.now().toEpochDay()),
                                note = obj.optString("note", "").trim(),
                                isSettled = obj.optBoolean("isSettled", false)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    fun encodeInstallmentLoans(items: List<InstallmentLoan>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("itemName", item.itemName.trim())
                        put("lenderOrStore", item.lenderOrStore.trim())
                        put(
                            "installmentAmount",
                            item.installmentAmount.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                        )
                        put("totalInstallments", item.totalInstallments.coerceAtLeast(1))
                        put("paidInstallments", item.paidInstallments.coerceIn(0, item.totalInstallments.coerceAtLeast(1)))
                        put(
                            "downPayment",
                            item.downPayment.takeIf { it.isFinite() }?.coerceAtLeast(0.0) ?: 0.0
                        )
                        put("frequency", InstallmentLoan.normalizeFrequency(item.frequency))
                        put("dueDayLabel", item.dueDayLabel.trim())
                        put("startDateMillis", item.startDateMillis)
                        put("autoLogExpense", item.autoLogExpense)
                    }
                )
            }
        }.toString()

    fun decodeInstallmentLoans(raw: String?): List<InstallmentLoan> {
        if (raw.isNullOrBlank()) return emptyList()
        cachedInstallmentLoans?.takeIf { it.raw == raw }?.let { return it.value }

        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val parsed = runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val itemName = obj.optString("itemName", "").trim()
                    val installmentAmount = obj.optDouble("installmentAmount", Double.NaN)
                    val totalInstallments = obj.optInt("totalInstallments", 0)
                    if (itemName.isNotEmpty() &&
                        installmentAmount.isFinite() && installmentAmount > 0.0 &&
                        totalInstallments > 0
                    ) {
                        val downPayment = obj.optDouble("downPayment", 0.0)
                            .takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                        add(
                            InstallmentLoan(
                                id = obj.optString("id", "").ifBlank { UUID.randomUUID().toString() },
                                itemName = itemName,
                                lenderOrStore = obj.optString("lenderOrStore", "").trim(),
                                installmentAmount = installmentAmount,
                                totalInstallments = totalInstallments,
                                paidInstallments = obj.optInt("paidInstallments", 0)
                                    .coerceIn(0, totalInstallments),
                                downPayment = downPayment,
                                frequency = InstallmentLoan.normalizeFrequency(
                                    obj.optString("frequency", InstallmentLoan.FREQ_MONTHLY)
                                ),
                                dueDayLabel = obj.optString("dueDayLabel", "").trim(),
                                startDateMillis = obj.optLong("startDateMillis", System.currentTimeMillis()),
                                autoLogExpense = obj.optBoolean("autoLogExpense", true)
                            )
                        )
                    }
                }
            }
        }.getOrDefault(emptyList())

        cachedInstallmentLoans = CachedJsonDecode(raw, parsed)
        return parsed
    }

    fun decodeInAppNotifications(raw: String?): List<InAppNotificationItem> {
        if (raw.isNullOrBlank()) return emptyList()
        cachedInAppNotifications?.takeIf { it.raw == raw }?.let { return it.value }

        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val parsed = runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    val id = obj.optString("id", "").trim()
                    val title = obj.optString("title", "").trim()
                    val message = obj.optString("message", "").trim()
                    val typeName = obj.optString("type", InAppNotificationType.BILL_DUE.name)
                    val type = runCatching { InAppNotificationType.valueOf(typeName) }
                        .getOrDefault(InAppNotificationType.BILL_DUE)
                    val timestamp = obj.optLong("timestampMillis", 0L)
                    if (id.isNotEmpty() && title.isNotEmpty()) {
                        add(
                            InAppNotificationItem(
                                id = id,
                                eventKey = obj.optString("eventKey", id),
                                type = type,
                                title = title,
                                message = message,
                                timestampMillis = timestamp,
                                isRead = obj.optBoolean("isRead", false),
                                hasShownToast = obj.optBoolean("hasShownToast", false)
                            )
                        )
                    }
                }
            }.sortedByDescending { it.timestampMillis }
        }.getOrDefault(emptyList())

        cachedInAppNotifications = CachedJsonDecode(raw, parsed)
        return parsed
    }

    fun encodeStringList(items: List<String>): String =
        JSONArray().apply {
            items.forEach { put(it) }
        }.toString()

    fun decodeStringList(raw: String?): List<String> {
        if (raw.isNullOrBlank()) return emptyList()
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        return runCatching {
            val array = JSONArray(trimmed)
            buildList {
                for (i in 0 until array.length()) {
                    val value = array.optString(i, "")
                    if (value.isNotBlank()) add(value)
                }
            }
        }.getOrDefault(emptyList())
    }
}

class UserPreferencesManager(private val context: Context) {

    companion object {
        private const val MAX_INBOX_ITEMS = 30
        private const val MAX_RECORDED_EVENT_KEYS = 80

        private val KEY_DEFAULT_HOURLY_RATE = doublePreferencesKey("default_hourly_rate")
        private val KEY_MINIMUM_WAGE_PER_DAY = doublePreferencesKey("minimum_wage_per_day")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")

        // Onboarding & Home Overview preferences
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_HAS_SEEN_APP_TUTORIAL = booleanPreferencesKey("has_seen_app_tutorial")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        private val KEY_SAVINGS_BUFFER = doublePreferencesKey("savings_buffer")

        // Profile & Settings / Analytics preferences
        private val KEY_JOB_TITLE = stringPreferencesKey("job_title")
        private val KEY_WORKPLACE = stringPreferencesKey("workplace")
        private val KEY_WORK_TYPE = stringPreferencesKey("work_type")
        private val KEY_TARGET_WEEKLY_HOURS = doublePreferencesKey("target_weekly_hours")
        private val KEY_PAY_CYCLE_SCHEDULE = stringPreferencesKey("pay_cycle_schedule")
        private val KEY_CUTOFF_MODE = stringPreferencesKey("cutoff_mode")
        private val KEY_CUTOFF_DAY_1 = intPreferencesKey("cutoff_day_1")
        private val KEY_CUTOFF_DAY_2 = intPreferencesKey("cutoff_day_2")
        private val KEY_CUTOFF_WEEKLY_DAY = intPreferencesKey("cutoff_weekly_day")
        private val KEY_LEGACY_RECURRING_MIGRATED = booleanPreferencesKey("legacy_recurring_migrated")
        private val KEY_PAYDAY_REMINDER = booleanPreferencesKey("payday_reminder")
        private val KEY_SHIFT_REMINDER = booleanPreferencesKey("shift_reminder")
        private val KEY_BILL_REMINDER = booleanPreferencesKey("bill_reminder")
        private val KEY_SAVINGS_REMINDER = booleanPreferencesKey("savings_reminder")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_SAVINGS_GOAL_TITLE = stringPreferencesKey("savings_goal_title")
        private val KEY_SAVINGS_GOAL_TARGET = doublePreferencesKey("savings_goal_target")
        private val KEY_CATEGORY_BUDGETS = stringPreferencesKey("category_budgets_serialized")
        private val KEY_MANAGED_CATEGORIES = stringPreferencesKey("managed_categories_serialized")
        private val KEY_RECURRING_ENTRIES = stringPreferencesKey("recurring_entries_serialized")
        private val KEY_SHIFT_TIME_PRESETS = stringPreferencesKey("shift_time_presets_serialized")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_APP_LOCK_METHOD = stringPreferencesKey("app_lock_method")
        private val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
        private val KEY_APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        private val KEY_APP_LOCK_TIMEOUT_SECONDS = intPreferencesKey("app_lock_timeout_seconds")
        private val KEY_THEME_MODE = stringPreferencesKey("theme_mode")
        private val KEY_LAST_BILL_NOTIFIED_DAY = longPreferencesKey("last_bill_notified_day")
        private val KEY_LAST_PAYDAY_NOTIFIED_DAY = longPreferencesKey("last_payday_notified_day")
        private val KEY_LAST_SHIFT_NOTIFIED_DAY = longPreferencesKey("last_shift_notified_day")
        private val KEY_LAST_SAVINGS_NOTIFIED_DAY = longPreferencesKey("last_savings_notified_day")
        private val KEY_NOTIFICATION_INBOX = stringPreferencesKey("notification_inbox_serialized")
        private val KEY_RECORDED_NOTIFICATION_KEYS = stringPreferencesKey("recorded_notification_keys_serialized")
        private val KEY_BALANCE_PRIVACY_HIDDEN = booleanPreferencesKey("balance_privacy_hidden")

        private val KEY_DEDUCTION_SSS_ENABLED = booleanPreferencesKey("deduction_sss_enabled")
        private val KEY_DEDUCTION_SSS_AMOUNT = doublePreferencesKey("deduction_sss_amount")
        private val KEY_DEDUCTION_PHILHEALTH_ENABLED = booleanPreferencesKey("deduction_philhealth_enabled")
        private val KEY_DEDUCTION_PHILHEALTH_AMOUNT = doublePreferencesKey("deduction_philhealth_amount")
        private val KEY_DEDUCTION_PAGIBIG_ENABLED = booleanPreferencesKey("deduction_pagibig_enabled")
        private val KEY_DEDUCTION_PAGIBIG_AMOUNT = doublePreferencesKey("deduction_pagibig_amount")
        private val KEY_DEDUCTION_TAX_ENABLED = booleanPreferencesKey("deduction_tax_enabled")
        private val KEY_DEDUCTION_TAX_PERCENT = doublePreferencesKey("deduction_tax_percent")
        private val KEY_DEDUCTION_CUSTOM_ENABLED = booleanPreferencesKey("deduction_custom_enabled")
        private val KEY_DEDUCTION_CUSTOM_AMOUNT = doublePreferencesKey("deduction_custom_amount")
        private val KEY_IOU_ENTRIES = stringPreferencesKey("iou_entries_serialized")
        private val KEY_INSTALLMENT_LOANS = stringPreferencesKey("installment_loans_serialized")
    }

    private val safeData: Flow<Preferences> = context.dataStore.data.catch { exception ->
        if (exception is IOException) {
            emit(emptyPreferences())
        } else {
            throw exception
        }
    }

    private fun round2(value: Double): Double =
        if (value.isFinite()) (value * 100.0).roundToLong() / 100.0 else 0.0

    val defaultHourlyRate: Flow<Double> = safeData.map { prefs ->
        prefs[KEY_DEFAULT_HOURLY_RATE] ?: 0.0
    }.distinctUntilChanged()

    val minimumWagePerDay: Flow<Double> = safeData.map { prefs ->
        prefs[KEY_MINIMUM_WAGE_PER_DAY] ?: 0.0
    }.distinctUntilChanged()

    val currencySymbol: Flow<String> = safeData.map { prefs ->
        prefs[KEY_CURRENCY_SYMBOL] ?: "₱"
    }.distinctUntilChanged()

    val hasCompletedOnboarding: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }.distinctUntilChanged()

    val hasSeenAppTutorial: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_HAS_SEEN_APP_TUTORIAL] ?: false
    }.distinctUntilChanged()

    val userName: Flow<String> = safeData.map { prefs ->
        prefs[KEY_USER_NAME] ?: ""
    }.distinctUntilChanged()

    val monthlyBudget: Flow<Double> = safeData.map { prefs ->
        prefs[KEY_MONTHLY_BUDGET] ?: 0.0
    }.distinctUntilChanged()

    val savingsBuffer: Flow<Double> = safeData.map { prefs ->
        prefs[KEY_SAVINGS_BUFFER] ?: 0.0
    }.distinctUntilChanged()

    val notificationInbox: Flow<List<InAppNotificationItem>> = safeData.map { prefs ->
        PreferenceJsonCodec.decodeInAppNotifications(prefs[KEY_NOTIFICATION_INBOX])
    }.distinctUntilChanged()

    val balancePrivacyHidden: Flow<Boolean> = safeData.map { prefs ->
        prefs[KEY_BALANCE_PRIVACY_HIDDEN] ?: false
    }.distinctUntilChanged()

    val paycheckDeductions: Flow<PaycheckDeductionsSetting> = safeData.map { prefs ->
        PaycheckDeductionsSetting(
            sssEnabled = prefs[KEY_DEDUCTION_SSS_ENABLED] ?: false,
            sssAmount = prefs[KEY_DEDUCTION_SSS_AMOUNT] ?: 0.0,
            philhealthEnabled = prefs[KEY_DEDUCTION_PHILHEALTH_ENABLED] ?: false,
            philhealthAmount = prefs[KEY_DEDUCTION_PHILHEALTH_AMOUNT] ?: 0.0,
            pagibigEnabled = prefs[KEY_DEDUCTION_PAGIBIG_ENABLED] ?: false,
            pagibigAmount = prefs[KEY_DEDUCTION_PAGIBIG_AMOUNT] ?: 0.0,
            taxEnabled = prefs[KEY_DEDUCTION_TAX_ENABLED] ?: false,
            taxPercent = prefs[KEY_DEDUCTION_TAX_PERCENT] ?: 0.0,
            customEnabled = prefs[KEY_DEDUCTION_CUSTOM_ENABLED] ?: false,
            customAmount = prefs[KEY_DEDUCTION_CUSTOM_AMOUNT] ?: 0.0
        )
    }.distinctUntilChanged()

    suspend fun setPaycheckDeductions(setting: PaycheckDeductionsSetting) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEDUCTION_SSS_ENABLED] = setting.sssEnabled
            prefs[KEY_DEDUCTION_SSS_AMOUNT] = setting.sssAmount
            prefs[KEY_DEDUCTION_PHILHEALTH_ENABLED] = setting.philhealthEnabled
            prefs[KEY_DEDUCTION_PHILHEALTH_AMOUNT] = setting.philhealthAmount
            prefs[KEY_DEDUCTION_PAGIBIG_ENABLED] = setting.pagibigEnabled
            prefs[KEY_DEDUCTION_PAGIBIG_AMOUNT] = setting.pagibigAmount
            prefs[KEY_DEDUCTION_TAX_ENABLED] = setting.taxEnabled
            prefs[KEY_DEDUCTION_TAX_PERCENT] = setting.taxPercent
            prefs[KEY_DEDUCTION_CUSTOM_ENABLED] = setting.customEnabled
            prefs[KEY_DEDUCTION_CUSTOM_AMOUNT] = setting.customAmount
        }
    }

    val iouEntries: Flow<List<IouEntrySetting>> = safeData.map { prefs ->
        PreferenceJsonCodec.decodeIouEntries(prefs[KEY_IOU_ENTRIES])
    }.distinctUntilChanged()

    suspend fun setIouEntries(items: List<IouEntrySetting>) {
        val serialized = PreferenceJsonCodec.encodeIouEntries(items)
        context.dataStore.edit { prefs ->
            prefs[KEY_IOU_ENTRIES] = serialized
        }
    }

    val installmentLoans: Flow<List<InstallmentLoan>> = safeData.map { prefs ->
        PreferenceJsonCodec.decodeInstallmentLoans(prefs[KEY_INSTALLMENT_LOANS])
    }.distinctUntilChanged()

    suspend fun setInstallmentLoans(items: List<InstallmentLoan>) {
        val serialized = PreferenceJsonCodec.encodeInstallmentLoans(items)
        context.dataStore.edit { prefs ->
            prefs[KEY_INSTALLMENT_LOANS] = serialized
        }
    }

    val settingsSnapshot: Flow<ShiftlySettingsSnapshot> = safeData.map { prefs ->
        val pinHash = prefs[KEY_APP_LOCK_PIN_HASH].orEmpty().ifBlank {
            prefs[KEY_APP_LOCK_PIN].orEmpty()
        }
        val legacyPayday = prefs[KEY_PAYDAY_REMINDER] ?: true
        val payCycleSchedule = prefs[KEY_PAY_CYCLE_SCHEDULE] ?: "15th & 30th"
        val fallbackCutoff = CutoffScheduleConfig.fromLegacySchedule(payCycleSchedule)
        val cutoffMode = prefs[KEY_CUTOFF_MODE] ?: fallbackCutoff.mode
        val cutoffDay1 = prefs[KEY_CUTOFF_DAY_1] ?: fallbackCutoff.day1
        val cutoffDay2 = prefs[KEY_CUTOFF_DAY_2] ?: fallbackCutoff.day2
        val cutoffWeeklyDay = prefs[KEY_CUTOFF_WEEKLY_DAY] ?: fallbackCutoff.weeklyDayOfWeek
        val normalizedCutoff = CutoffScheduleConfig(
            mode = cutoffMode,
            day1 = cutoffDay1,
            day2 = cutoffDay2,
            weeklyDayOfWeek = cutoffWeeklyDay
        ).normalized()

        ShiftlySettingsSnapshot(
            payCycleSchedule = payCycleSchedule,
            paydayReminderEnabled = legacyPayday,
            shiftReminderEnabled = prefs[KEY_SHIFT_REMINDER] ?: true,
            billReminderEnabled = prefs[KEY_BILL_REMINDER] ?: true,
            savingsReminderEnabled = prefs[KEY_SAVINGS_REMINDER] ?: legacyPayday,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            nightDiffStartHour = 22,
            nightDiffMultiplier = 1.10,
            overtimeMultiplier = 1.25,
            savingsGoalTitle = prefs[KEY_SAVINGS_GOAL_TITLE] ?: "",
            savingsGoalTarget = prefs[KEY_SAVINGS_GOAL_TARGET] ?: 0.0,
            categoryBudgets = PreferenceJsonCodec.decodeCategoryBudgets(prefs[KEY_CATEGORY_BUDGETS]),
            managedCategories = PreferenceJsonCodec.decodeManagedCategories(prefs[KEY_MANAGED_CATEGORIES]),
            recurringEntries = PreferenceJsonCodec.decodeRecurringEntries(prefs[KEY_RECURRING_ENTRIES]),
            shiftTimePresets = PreferenceJsonCodec.decodeShiftTimePresets(prefs[KEY_SHIFT_TIME_PRESETS]),
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            appLockMethod = prefs[KEY_APP_LOCK_METHOD] ?: "PIN & Fingerprint",
            hasAppLockPin = pinHash.isNotBlank(),
            appLockPinHash = pinHash,
            appLockTimeoutSeconds = (prefs[KEY_APP_LOCK_TIMEOUT_SECONDS] ?: 0).coerceAtLeast(0),
            themeMode = prefs[KEY_THEME_MODE] ?: "SYSTEM",
            cutoffMode = normalizedCutoff.mode,
            cutoffDay1 = normalizedCutoff.day1,
            cutoffDay2 = normalizedCutoff.day2,
            cutoffWeeklyDay = normalizedCutoff.weeklyDayOfWeek,
            legacyRecurringMigrated = prefs[KEY_LEGACY_RECURRING_MIGRATED] ?: false,
            jobTitle = prefs[KEY_JOB_TITLE] ?: "",
            workplace = prefs[KEY_WORKPLACE] ?: "",
            workType = prefs[KEY_WORK_TYPE] ?: "Full-Time",
            targetWeeklyHours = prefs[KEY_TARGET_WEEKLY_HOURS] ?: 40.0
        )
    }.distinctUntilChanged()

    suspend fun recordInAppNotification(
        eventKey: String,
        type: InAppNotificationType,
        title: String,
        message: String,
        timestampMillis: Long = System.currentTimeMillis()
    ): Boolean {
        val cleanTitle = title.trim()
        val cleanMessage = message.trim()
        val cleanKey = eventKey.trim()
        if (cleanKey.isEmpty() || cleanTitle.isEmpty()) return false

        var inserted = false
        context.dataStore.edit { prefs ->
            val recordedKeys = PreferenceJsonCodec.decodeStringList(prefs[KEY_RECORDED_NOTIFICATION_KEYS])
            val currentItems = PreferenceJsonCodec.decodeInAppNotifications(prefs[KEY_NOTIFICATION_INBOX])
            if (cleanKey in recordedKeys || currentItems.any { it.eventKey == cleanKey }) {
                return@edit
            }
            val newItem = InAppNotificationItem(
                id = UUID.randomUUID().toString(),
                eventKey = cleanKey,
                type = type,
                title = cleanTitle,
                message = cleanMessage,
                timestampMillis = timestampMillis,
                isRead = false,
                hasShownToast = false
            )
            val updatedItems = (listOf(newItem) + currentItems)
                .sortedByDescending { it.timestampMillis }
                .take(MAX_INBOX_ITEMS)
            val updatedKeys = (recordedKeys + cleanKey).takeLast(MAX_RECORDED_EVENT_KEYS)
            prefs[KEY_NOTIFICATION_INBOX] = PreferenceJsonCodec.encodeInAppNotifications(updatedItems)
            prefs[KEY_RECORDED_NOTIFICATION_KEYS] = PreferenceJsonCodec.encodeStringList(updatedKeys)
            inserted = true
        }
        return inserted
    }

    suspend fun markNotificationRead(id: String) {
        context.dataStore.edit { prefs ->
            val current = PreferenceJsonCodec.decodeInAppNotifications(prefs[KEY_NOTIFICATION_INBOX])
            var changed = false
            val updated = current.map { item ->
                if (item.id == id && (!item.isRead || !item.hasShownToast)) {
                    changed = true
                    item.copy(isRead = true, hasShownToast = true)
                } else {
                    item
                }
            }
            if (changed) {
                prefs[KEY_NOTIFICATION_INBOX] = PreferenceJsonCodec.encodeInAppNotifications(updated)
            }
        }
    }

    suspend fun markNotificationToastShown(id: String) {
        context.dataStore.edit { prefs ->
            val current = PreferenceJsonCodec.decodeInAppNotifications(prefs[KEY_NOTIFICATION_INBOX])
            var changed = false
            val updated = current.map { item ->
                if (item.id == id && !item.hasShownToast) {
                    changed = true
                    item.copy(hasShownToast = true)
                } else {
                    item
                }
            }
            if (changed) {
                prefs[KEY_NOTIFICATION_INBOX] = PreferenceJsonCodec.encodeInAppNotifications(updated)
            }
        }
    }

    suspend fun markAllNotificationsRead() {
        context.dataStore.edit { prefs ->
            val current = PreferenceJsonCodec.decodeInAppNotifications(prefs[KEY_NOTIFICATION_INBOX])
            if (current.any { !it.isRead || !it.hasShownToast }) {
                val updated = current.map { it.copy(isRead = true, hasShownToast = true) }
                prefs[KEY_NOTIFICATION_INBOX] = PreferenceJsonCodec.encodeInAppNotifications(updated)
            }
        }
    }

    suspend fun clearNotificationInbox() {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATION_INBOX] = "[]"
        }
    }

    suspend fun updateThemeMode(mode: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode
        }
    }

    suspend fun getNotificationLastDays(): NotificationLastDays {
        val prefs = safeData.first()
        val legacyPayday = prefs[KEY_LAST_PAYDAY_NOTIFIED_DAY] ?: 0L
        val savedSavings = prefs[KEY_LAST_SAVINGS_NOTIFIED_DAY] ?: 0L
        return NotificationLastDays(
            lastBillDay = prefs[KEY_LAST_BILL_NOTIFIED_DAY] ?: 0L,
            lastShiftDay = prefs[KEY_LAST_SHIFT_NOTIFIED_DAY] ?: 0L,
            lastSavingsDay = maxOf(legacyPayday, savedSavings)
        )
    }

    suspend fun markBillNotified(epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_BILL_NOTIFIED_DAY] = epochDay
        }
    }

    suspend fun markShiftNotified(epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SHIFT_NOTIFIED_DAY] = epochDay
        }
    }

    suspend fun markSavingsNotified(epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SAVINGS_NOTIFIED_DAY] = epochDay
            prefs[KEY_LAST_PAYDAY_NOTIFIED_DAY] = epochDay
        }
    }

    suspend fun applyCurrencyConversion(newSymbol: String, conversionRate: Double?) {
        val cleanSymbol = newSymbol.trim().ifEmpty { "₱" }
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY_SYMBOL] = cleanSymbol
            if (conversionRate != null && conversionRate.isFinite() && conversionRate > 0.0) {
                val baseRate = prefs[KEY_DEFAULT_HOURLY_RATE] ?: 0.0
                if (baseRate > 0.0) {
                    prefs[KEY_DEFAULT_HOURLY_RATE] = round2(baseRate * conversionRate).coerceAtLeast(0.01)
                }

                val minWage = prefs[KEY_MINIMUM_WAGE_PER_DAY] ?: 0.0
                if (minWage > 0.0) {
                    prefs[KEY_MINIMUM_WAGE_PER_DAY] = round2(minWage * conversionRate).coerceAtLeast(0.01)
                }

                val monthly = prefs[KEY_MONTHLY_BUDGET] ?: 0.0
                if (monthly > 0.0) {
                    prefs[KEY_MONTHLY_BUDGET] = round2(monthly * conversionRate).coerceAtLeast(0.0)
                }

                val buffer = prefs[KEY_SAVINGS_BUFFER] ?: 0.0
                if (buffer > 0.0) {
                    prefs[KEY_SAVINGS_BUFFER] = round2(buffer * conversionRate).coerceAtLeast(0.0)
                }

                val goalTarget = prefs[KEY_SAVINGS_GOAL_TARGET] ?: 0.0
                if (goalTarget > 0.0) {
                    prefs[KEY_SAVINGS_GOAL_TARGET] = round2(goalTarget * conversionRate).coerceAtLeast(1.0)
                }

                val currentBudgets = PreferenceJsonCodec.decodeCategoryBudgets(prefs[KEY_CATEGORY_BUDGETS])
                prefs[KEY_CATEGORY_BUDGETS] = PreferenceJsonCodec.encodeCategoryBudgets(
                    currentBudgets.map { it.copy(monthlyLimit = round2(it.monthlyLimit * conversionRate)) }
                )

                val currentRecurring = PreferenceJsonCodec.decodeRecurringEntries(prefs[KEY_RECURRING_ENTRIES])
                prefs[KEY_RECURRING_ENTRIES] = PreferenceJsonCodec.encodeRecurringEntries(
                    currentRecurring.map {
                        it.copy(amount = round2(it.amount * conversionRate).coerceAtLeast(0.01))
                    }
                )

                val currentLoans = PreferenceJsonCodec.decodeInstallmentLoans(prefs[KEY_INSTALLMENT_LOANS])
                if (currentLoans.isNotEmpty()) {
                    prefs[KEY_INSTALLMENT_LOANS] = PreferenceJsonCodec.encodeInstallmentLoans(
                        currentLoans.map {
                            it.copy(
                                installmentAmount = round2(it.installmentAmount * conversionRate).coerceAtLeast(0.01),
                                downPayment = round2(it.downPayment * conversionRate).coerceAtLeast(0.0)
                            )
                        }
                    )
                }
            }
        }
    }

    suspend fun setDefaultHourlyRate(rate: Double) {
        if (!rate.isFinite()) return
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_HOURLY_RATE] = rate.coerceAtLeast(0.0)
        }
    }

    suspend fun setMinimumWagePerDay(amount: Double) {
        if (!amount.isFinite()) return
        context.dataStore.edit { prefs ->
            prefs[KEY_MINIMUM_WAGE_PER_DAY] = amount.coerceAtLeast(0.0)
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    suspend fun setHasSeenAppTutorial(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_HAS_SEEN_APP_TUTORIAL] = seen
        }
    }

    suspend fun setUserName(name: String) {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) {
            context.dataStore.edit { prefs ->
                prefs[KEY_USER_NAME] = trimmed
            }
        }
    }

    suspend fun setProfileDetails(
        userName: String,
        jobTitle: String,
        workplace: String,
        workType: String,
        targetWeeklyHours: Double
    ) {
        context.dataStore.edit { prefs ->
            if (userName.trim().isNotEmpty()) {
                prefs[KEY_USER_NAME] = userName.trim()
            }
            prefs[KEY_JOB_TITLE] = jobTitle.trim()
            prefs[KEY_WORKPLACE] = workplace.trim()
            prefs[KEY_WORK_TYPE] = workType.trim().ifEmpty { "Full-Time" }
            prefs[KEY_TARGET_WEEKLY_HOURS] = if (targetWeeklyHours.isFinite() && targetWeeklyHours >= 0.0) targetWeeklyHours else 0.0
        }
    }

    suspend fun setMonthlyBudget(budget: Double) {
        if (!budget.isFinite()) return
        context.dataStore.edit { prefs ->
            prefs[KEY_MONTHLY_BUDGET] = budget.coerceAtLeast(0.0)
        }
    }

    suspend fun updateCutoffSchedule(config: CutoffScheduleConfig) {
        val norm = config.normalized()
        context.dataStore.edit { prefs ->
            prefs[KEY_CUTOFF_MODE] = norm.mode
            prefs[KEY_CUTOFF_DAY_1] = norm.day1
            prefs[KEY_CUTOFF_DAY_2] = norm.day2
            prefs[KEY_CUTOFF_WEEKLY_DAY] = norm.weeklyDayOfWeek
            prefs[KEY_PAY_CYCLE_SCHEDULE] = norm.toPayCycleScheduleString()
        }
    }

    suspend fun markLegacyRecurringMigrated(migrated: Boolean = true) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LEGACY_RECURRING_MIGRATED] = migrated
        }
    }

    suspend fun setNotificationPreferences(
        notificationsEnabled: Boolean,
        billReminderEnabled: Boolean,
        shiftReminderEnabled: Boolean,
        savingsReminderEnabled: Boolean
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = notificationsEnabled
            prefs[KEY_BILL_REMINDER] = billReminderEnabled
            prefs[KEY_SHIFT_REMINDER] = shiftReminderEnabled
            prefs[KEY_SAVINGS_REMINDER] = savingsReminderEnabled
            prefs[KEY_PAYDAY_REMINDER] = savingsReminderEnabled
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setSavingsGoal(title: String, target: Double) {
        val safeTarget = (target.takeIf { it.isFinite() } ?: 0.0).coerceAtLeast(0.0)
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVINGS_GOAL_TITLE] = title.trim()
            prefs[KEY_SAVINGS_GOAL_TARGET] = safeTarget
        }
    }

    suspend fun setCategoryBudgets(items: List<CategoryBudgetSetting>) {
        val serialized = PreferenceJsonCodec.encodeCategoryBudgets(items)
        context.dataStore.edit { prefs ->
            prefs[KEY_CATEGORY_BUDGETS] = serialized
        }
    }

    suspend fun setManagedCategories(items: List<ManagedCategorySetting>) {
        val serialized = PreferenceJsonCodec.encodeManagedCategories(items)
        context.dataStore.edit { prefs ->
            prefs[KEY_MANAGED_CATEGORIES] = serialized
        }
    }

    suspend fun setRecurringEntries(items: List<RecurringEntrySetting>) {
        val serialized = PreferenceJsonCodec.encodeRecurringEntries(items)
        context.dataStore.edit { prefs ->
            prefs[KEY_RECURRING_ENTRIES] = serialized
        }
    }

    suspend fun setShiftTimePresets(items: List<ShiftTimePreset>) {
        val serialized = PreferenceJsonCodec.encodeShiftTimePresets(items)
        context.dataStore.edit { prefs ->
            prefs[KEY_SHIFT_TIME_PRESETS] = serialized
        }
    }

    suspend fun setAppLock(
        enabled: Boolean,
        method: String,
        pinHash: String?,
        timeoutSeconds: Int? = null
    ) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            prefs[KEY_APP_LOCK_METHOD] = method
            if (!pinHash.isNullOrBlank()) {
                prefs[KEY_APP_LOCK_PIN_HASH] = pinHash
            }
            if (timeoutSeconds != null) {
                prefs[KEY_APP_LOCK_TIMEOUT_SECONDS] = timeoutSeconds.coerceAtLeast(0)
            }
            prefs.remove(KEY_APP_LOCK_PIN)
        }
    }

    suspend fun applyRestoredSettings(
        userName: String?,
        currencySymbol: String?,
        defaultHourlyRate: Double?,
        minimumWagePerDay: Double? = null,
        monthlyBudget: Double?,
        savingsBuffer: Double?,
        settings: ShiftlySettingsSnapshot?,
        paycheckDeductions: PaycheckDeductionsSetting? = null,
        iouEntries: List<IouEntrySetting>? = null,
        installmentLoans: List<InstallmentLoan>? = null
    ) {
        context.dataStore.edit { prefs ->
            if (!userName.isNullOrBlank()) prefs[KEY_USER_NAME] = userName.trim()
            if (!currencySymbol.isNullOrBlank()) prefs[KEY_CURRENCY_SYMBOL] = currencySymbol.trim()
            if (defaultHourlyRate != null && defaultHourlyRate.isFinite() && defaultHourlyRate > 0.0) {
                prefs[KEY_DEFAULT_HOURLY_RATE] = defaultHourlyRate
            }
            if (minimumWagePerDay != null && minimumWagePerDay.isFinite() && minimumWagePerDay >= 0.0) {
                prefs[KEY_MINIMUM_WAGE_PER_DAY] = minimumWagePerDay
            }
            if (monthlyBudget != null && monthlyBudget.isFinite() && monthlyBudget >= 0.0) {
                prefs[KEY_MONTHLY_BUDGET] = monthlyBudget
            }
            if (savingsBuffer != null && savingsBuffer.isFinite() && savingsBuffer >= 0.0) {
                prefs[KEY_SAVINGS_BUFFER] = savingsBuffer
            }
            if (settings != null) {
                val normCutoff = settings.cutoffScheduleConfig()
                prefs[KEY_PAY_CYCLE_SCHEDULE] = settings.payCycleSchedule
                prefs[KEY_CUTOFF_MODE] = normCutoff.mode
                prefs[KEY_CUTOFF_DAY_1] = normCutoff.day1
                prefs[KEY_CUTOFF_DAY_2] = normCutoff.day2
                prefs[KEY_CUTOFF_WEEKLY_DAY] = normCutoff.weeklyDayOfWeek
                prefs[KEY_LEGACY_RECURRING_MIGRATED] = settings.legacyRecurringMigrated
                prefs[KEY_PAYDAY_REMINDER] = settings.paydayReminderEnabled
                prefs[KEY_SHIFT_REMINDER] = settings.shiftReminderEnabled
                prefs[KEY_BILL_REMINDER] = settings.billReminderEnabled
                prefs[KEY_SAVINGS_REMINDER] = settings.savingsReminderEnabled
                prefs[KEY_NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
                prefs[KEY_SAVINGS_GOAL_TITLE] = settings.savingsGoalTitle
                prefs[KEY_SAVINGS_GOAL_TARGET] = settings.savingsGoalTarget.coerceAtLeast(0.0)
                prefs[KEY_JOB_TITLE] = settings.jobTitle.trim()
                prefs[KEY_WORKPLACE] = settings.workplace.trim()
                prefs[KEY_WORK_TYPE] = settings.workType.trim().ifEmpty { "Full-Time" }
                prefs[KEY_TARGET_WEEKLY_HOURS] =
                    if (settings.targetWeeklyHours.isFinite() && settings.targetWeeklyHours >= 0.0) {
                        settings.targetWeeklyHours
                    } else {
                        40.0
                    }
                prefs[KEY_CATEGORY_BUDGETS] = PreferenceJsonCodec.encodeCategoryBudgets(settings.categoryBudgets)
                prefs[KEY_MANAGED_CATEGORIES] = PreferenceJsonCodec.encodeManagedCategories(settings.managedCategories)
                prefs[KEY_RECURRING_ENTRIES] = PreferenceJsonCodec.encodeRecurringEntries(settings.recurringEntries)
                prefs[KEY_SHIFT_TIME_PRESETS] = PreferenceJsonCodec.encodeShiftTimePresets(settings.shiftTimePresets)
            }
            if (paycheckDeductions != null) {
                prefs[KEY_DEDUCTION_SSS_ENABLED] = paycheckDeductions.sssEnabled
                prefs[KEY_DEDUCTION_SSS_AMOUNT] = paycheckDeductions.sssAmount
                prefs[KEY_DEDUCTION_PHILHEALTH_ENABLED] = paycheckDeductions.philhealthEnabled
                prefs[KEY_DEDUCTION_PHILHEALTH_AMOUNT] = paycheckDeductions.philhealthAmount
                prefs[KEY_DEDUCTION_PAGIBIG_ENABLED] = paycheckDeductions.pagibigEnabled
                prefs[KEY_DEDUCTION_PAGIBIG_AMOUNT] = paycheckDeductions.pagibigAmount
                prefs[KEY_DEDUCTION_TAX_ENABLED] = paycheckDeductions.taxEnabled
                prefs[KEY_DEDUCTION_TAX_PERCENT] = paycheckDeductions.taxPercent
                prefs[KEY_DEDUCTION_CUSTOM_ENABLED] = paycheckDeductions.customEnabled
                prefs[KEY_DEDUCTION_CUSTOM_AMOUNT] = paycheckDeductions.customAmount
            }
            if (iouEntries != null) {
                prefs[KEY_IOU_ENTRIES] = PreferenceJsonCodec.encodeIouEntries(iouEntries)
            }
            if (installmentLoans != null) {
                prefs[KEY_INSTALLMENT_LOANS] = PreferenceJsonCodec.encodeInstallmentLoans(installmentLoans)
            }
        }
    }

    suspend fun setBalancePrivacyHidden(hidden: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_BALANCE_PRIVACY_HIDDEN] = hidden
        }
    }
}
