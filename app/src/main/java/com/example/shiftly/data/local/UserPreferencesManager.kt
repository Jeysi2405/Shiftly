package com.example.shiftly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlin.math.roundToLong
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shiftly_prefs")

data class ActiveTimerState(
    val isRunning: Boolean = false,
    val startTimeMillis: Long = 0L,
    val shiftDateEpochDay: Long = 0L,
    val lockedRate: Double = 0.0
)

data class CategoryBudgetSetting(
    val category: String,
    val monthlyLimit: Double
)

data class ManagedCategorySetting(
    val name: String,
    val isVisible: Boolean
)

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

data class ShiftlySettingsSnapshot(
    val payCycleSchedule: String = "15th & 30th",
    val paydayReminderEnabled: Boolean = true,
    val shiftReminderEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val nightDiffStartHour: Int = 22,
    val nightDiffMultiplier: Double = 1.10,
    val overtimeMultiplier: Double = 1.25,
    val savingsGoalTitle: String = "Tuition & Laptop Fund",
    val savingsGoalTarget: Double = 5000.0,
    val categoryBudgets: List<CategoryBudgetSetting> = DEFAULT_CATEGORY_BUDGETS,
    val managedCategories: List<ManagedCategorySetting> = DEFAULT_MANAGED_CATEGORIES,
    val recurringEntries: List<RecurringEntrySetting> = DEFAULT_RECURRING_ENTRIES,
    val appLockEnabled: Boolean = false,
    val appLockMethod: String = "PIN & Fingerprint",
    val hasAppLockPin: Boolean = false,
    val appLockPinHash: String = ""
) {
    companion object {
        val DEFAULT_CATEGORY_BUDGETS = listOf(
            CategoryBudgetSetting("Food", 1200.0),
            CategoryBudgetSetting("Transport", 500.0),
            CategoryBudgetSetting("Supplies", 600.0),
            CategoryBudgetSetting("Bills", 700.0)
        )

        val DEFAULT_MANAGED_CATEGORIES = listOf(
            ManagedCategorySetting("Food", true),
            ManagedCategorySetting("Transport", true),
            ManagedCategorySetting("Bills", true),
            ManagedCategorySetting("Supplies", true),
            ManagedCategorySetting("Split Bill", true),
            ManagedCategorySetting("Other", true)
        )

        val DEFAULT_RECURRING_ENTRIES: List<RecurringEntrySetting> = emptyList()
    }
}

/**
 * Pure JSON serializer/deserializer for structured user preferences.
 * Supports decoding legacy delimited strings for seamless upgrades while always writing JSON.
 */
object PreferenceJsonCodec {
    fun encodeCategoryBudgets(items: List<CategoryBudgetSetting>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("category", item.category.trim())
                        put("monthlyLimit", item.monthlyLimit.coerceAtLeast(0.0))
                    }
                )
            }
        }.toString()

    fun decodeCategoryBudgets(raw: String?): List<CategoryBudgetSetting> {
        if (raw.isNullOrBlank()) return ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            val parsed = runCatching {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val cat = obj.optString("category", "").trim()
                        val limit = obj.optDouble("monthlyLimit", Double.NaN)
                        if (cat.isNotEmpty() && !limit.isNaN() && limit >= 0.0) {
                            add(CategoryBudgetSetting(cat, limit))
                        }
                    }
                }
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }
        // Legacy fallback: "Food:1200.0;Transport:500.0"
        return trimmed.split(";").mapNotNull { entry ->
            val idx = entry.lastIndexOf(':')
            if (idx <= 0 || idx >= entry.length - 1) return@mapNotNull null
            val cat = entry.substring(0, idx).trim()
            val amount = entry.substring(idx + 1).toDoubleOrNull() ?: return@mapNotNull null
            if (cat.isEmpty()) null else CategoryBudgetSetting(cat, amount.coerceAtLeast(0.0))
        }.ifEmpty { ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS }
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
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            val parsed = runCatching {
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
            }.getOrNull()
            if (!parsed.isNullOrEmpty()) return parsed
        }
        // Legacy fallback: "Food|1;Transport|0"
        return trimmed.split(";").mapNotNull { entry ->
            val idx = entry.lastIndexOf('|')
            if (idx <= 0 || idx >= entry.length - 1) return@mapNotNull null
            val name = entry.substring(0, idx).trim()
            val visible = entry.substring(idx + 1) == "1"
            if (name.isEmpty()) null else ManagedCategorySetting(name, visible)
        }.ifEmpty { ShiftlySettingsSnapshot.DEFAULT_MANAGED_CATEGORIES }
    }

    fun encodeRecurringEntries(items: List<RecurringEntrySetting>): String =
        JSONArray().apply {
            items.forEach { item ->
                put(
                    JSONObject().apply {
                        put("id", item.id)
                        put("title", item.title.trim())
                        put("amount", item.amount.coerceAtLeast(0.0))
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
        val trimmed = raw.trim()
        if (trimmed.startsWith("[")) {
            return runCatching {
                val array = JSONArray(trimmed)
                buildList {
                    for (i in 0 until array.length()) {
                        val obj = array.optJSONObject(i) ?: continue
                        val id = obj.optString("id", "").trim()
                        val title = obj.optString("title", "").trim()
                        val amount = obj.optDouble("amount", Double.NaN)
                        if (id.isNotEmpty() && title.isNotEmpty() && !amount.isNaN() && amount > 0.0) {
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
        }
        // Legacy fallback: "id|title|amount|freq|isIncome|isEnabled;..."
        return trimmed.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 6) {
                val amount = parts[2].toDoubleOrNull() ?: return@mapNotNull null
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
}

class UserPreferencesManager(private val context: Context) {

    companion object {
        private val KEY_DEFAULT_HOURLY_RATE = doublePreferencesKey("default_hourly_rate")
        private val KEY_CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        private val KEY_ACTIVE_TIMER_START = longPreferencesKey("active_timer_start")
        private val KEY_ACTIVE_TIMER_DATE = longPreferencesKey("active_timer_date")
        private val KEY_ACTIVE_TIMER_RATE = doublePreferencesKey("active_timer_rate")

        // Onboarding & Home Overview preferences
        private val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        private val KEY_USER_NAME = stringPreferencesKey("user_name")
        private val KEY_MONTHLY_BUDGET = doublePreferencesKey("monthly_budget")
        private val KEY_SAVINGS_BUFFER = doublePreferencesKey("savings_buffer")

        // Profile & Settings / Analytics preferences
        private val KEY_PAY_CYCLE_SCHEDULE = stringPreferencesKey("pay_cycle_schedule")
        private val KEY_PAYDAY_REMINDER = booleanPreferencesKey("payday_reminder")
        private val KEY_SHIFT_REMINDER = booleanPreferencesKey("shift_reminder")
        private val KEY_NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        private val KEY_ND_START_HOUR = intPreferencesKey("night_diff_start_hour")
        private val KEY_ND_MULTIPLIER = doublePreferencesKey("night_diff_multiplier")
        private val KEY_OT_MULTIPLIER = doublePreferencesKey("overtime_multiplier")
        private val KEY_SAVINGS_GOAL_TITLE = stringPreferencesKey("savings_goal_title")
        private val KEY_SAVINGS_GOAL_TARGET = doublePreferencesKey("savings_goal_target")
        private val KEY_CATEGORY_BUDGETS = stringPreferencesKey("category_budgets_serialized")
        private val KEY_MANAGED_CATEGORIES = stringPreferencesKey("managed_categories_serialized")
        private val KEY_RECURRING_ENTRIES = stringPreferencesKey("recurring_entries_serialized")
        private val KEY_APP_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_APP_LOCK_METHOD = stringPreferencesKey("app_lock_method")
        private val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin") // legacy plaintext read-only fallback
        private val KEY_APP_LOCK_PIN_HASH = stringPreferencesKey("app_lock_pin_hash")
        private val KEY_LAST_PAYDAY_NOTIFIED_DAY = longPreferencesKey("last_payday_notified_day")
        private val KEY_LAST_SHIFT_NOTIFIED_DAY = longPreferencesKey("last_shift_notified_day")
    }

    private fun round2(value: Double): Double = (value * 100.0).roundToLong() / 100.0

    val defaultHourlyRate: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_DEFAULT_HOURLY_RATE] ?: 96.38
    }

    val currencySymbol: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_CURRENCY_SYMBOL] ?: "₱"
    }

    val hasCompletedOnboarding: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_ONBOARDING_COMPLETED] ?: false
    }

    val userName: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_USER_NAME] ?: "Maya"
    }

    val monthlyBudget: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_MONTHLY_BUDGET] ?: 3000.0
    }

    val savingsBuffer: Flow<Double> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAVINGS_BUFFER] ?: 1000.0
    }

    val settingsSnapshot: Flow<ShiftlySettingsSnapshot> = context.dataStore.data.map { prefs ->
        val pinHash = prefs[KEY_APP_LOCK_PIN_HASH].orEmpty().ifBlank {
            prefs[KEY_APP_LOCK_PIN].orEmpty()
        }
        ShiftlySettingsSnapshot(
            payCycleSchedule = prefs[KEY_PAY_CYCLE_SCHEDULE] ?: "15th & 30th",
            paydayReminderEnabled = prefs[KEY_PAYDAY_REMINDER] ?: true,
            shiftReminderEnabled = prefs[KEY_SHIFT_REMINDER] ?: true,
            notificationsEnabled = prefs[KEY_NOTIFICATIONS_ENABLED] ?: true,
            nightDiffStartHour = prefs[KEY_ND_START_HOUR] ?: 22,
            nightDiffMultiplier = prefs[KEY_ND_MULTIPLIER] ?: 1.10,
            overtimeMultiplier = prefs[KEY_OT_MULTIPLIER] ?: 1.25,
            savingsGoalTitle = prefs[KEY_SAVINGS_GOAL_TITLE] ?: "Tuition & Laptop Fund",
            savingsGoalTarget = prefs[KEY_SAVINGS_GOAL_TARGET] ?: 5000.0,
            categoryBudgets = PreferenceJsonCodec.decodeCategoryBudgets(prefs[KEY_CATEGORY_BUDGETS]),
            managedCategories = PreferenceJsonCodec.decodeManagedCategories(prefs[KEY_MANAGED_CATEGORIES]),
            recurringEntries = PreferenceJsonCodec.decodeRecurringEntries(prefs[KEY_RECURRING_ENTRIES]),
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            appLockMethod = prefs[KEY_APP_LOCK_METHOD] ?: "PIN & Fingerprint",
            hasAppLockPin = pinHash.isNotBlank(),
            appLockPinHash = pinHash
        )
    }

    val activeTimerState: Flow<ActiveTimerState> = context.dataStore.data.map { prefs ->
        val start = prefs[KEY_ACTIVE_TIMER_START] ?: 0L
        if (start > 0L) {
            ActiveTimerState(
                isRunning = true,
                startTimeMillis = start,
                shiftDateEpochDay = prefs[KEY_ACTIVE_TIMER_DATE] ?: 0L,
                lockedRate = prefs[KEY_ACTIVE_TIMER_RATE] ?: 96.38
            )
        } else {
            ActiveTimerState(isRunning = false)
        }
    }

    suspend fun getLastNotifiedDays(): Pair<Long, Long> {
        val prefs = context.dataStore.data.first()
        return (prefs[KEY_LAST_PAYDAY_NOTIFIED_DAY] ?: 0L) to
            (prefs[KEY_LAST_SHIFT_NOTIFIED_DAY] ?: 0L)
    }

    suspend fun markPaydayNotified(epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_PAYDAY_NOTIFIED_DAY] = epochDay
        }
    }

    suspend fun markShiftNotified(epochDay: Long) {
        context.dataStore.edit { prefs ->
            prefs[KEY_LAST_SHIFT_NOTIFIED_DAY] = epochDay
        }
    }

    suspend fun applyCurrencyConversion(newSymbol: String, conversionRate: Double?) {
        val cleanSymbol = newSymbol.trim().ifEmpty { "₱" }
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY_SYMBOL] = cleanSymbol
            if (conversionRate != null && conversionRate > 0.0) {
                val baseRate = prefs[KEY_DEFAULT_HOURLY_RATE] ?: 96.38
                prefs[KEY_DEFAULT_HOURLY_RATE] = round2(baseRate * conversionRate).coerceAtLeast(0.01)

                val monthly = prefs[KEY_MONTHLY_BUDGET] ?: 3000.0
                prefs[KEY_MONTHLY_BUDGET] = round2(monthly * conversionRate).coerceAtLeast(0.0)

                val buffer = prefs[KEY_SAVINGS_BUFFER] ?: 1000.0
                prefs[KEY_SAVINGS_BUFFER] = round2(buffer * conversionRate).coerceAtLeast(0.0)

                val goalTarget = prefs[KEY_SAVINGS_GOAL_TARGET] ?: 5000.0
                prefs[KEY_SAVINGS_GOAL_TARGET] = round2(goalTarget * conversionRate).coerceAtLeast(100.0)

                val activeRate = prefs[KEY_ACTIVE_TIMER_RATE]
                if (activeRate != null && activeRate > 0.0) {
                    prefs[KEY_ACTIVE_TIMER_RATE] = round2(activeRate * conversionRate).coerceAtLeast(0.01)
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
            }
        }
    }

    suspend fun setDefaultHourlyRate(rate: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DEFAULT_HOURLY_RATE] = rate.coerceAtLeast(0.0)
        }
    }

    suspend fun setCurrencySymbol(symbol: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_CURRENCY_SYMBOL] = symbol
        }
    }

    suspend fun setOnboardingCompleted(completed: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ONBOARDING_COMPLETED] = completed
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

    suspend fun setMonthlyBudget(budget: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_MONTHLY_BUDGET] = budget.coerceAtLeast(0.0)
        }
    }

    suspend fun setSavingsBuffer(buffer: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVINGS_BUFFER] = buffer.coerceAtLeast(0.0)
        }
    }

    suspend fun setPayCycleSettings(schedule: String, paydayReminder: Boolean, shiftReminder: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_PAY_CYCLE_SCHEDULE] = schedule
            prefs[KEY_PAYDAY_REMINDER] = paydayReminder
            prefs[KEY_SHIFT_REMINDER] = shiftReminder
        }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_NOTIFICATIONS_ENABLED] = enabled
        }
    }

    suspend fun setShiftRules(ndStartHour: Int, ndMultiplier: Double, otMultiplier: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ND_START_HOUR] = ndStartHour.coerceIn(18, 23)
            prefs[KEY_ND_MULTIPLIER] = ndMultiplier.coerceIn(1.0, 2.0)
            prefs[KEY_OT_MULTIPLIER] = otMultiplier.coerceIn(1.0, 3.0)
        }
    }

    suspend fun setSavingsGoal(title: String, target: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_SAVINGS_GOAL_TITLE] = title.trim().ifEmpty { "Tuition & Laptop Fund" }
            prefs[KEY_SAVINGS_GOAL_TARGET] = target.coerceAtLeast(100.0)
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

    suspend fun setAppLock(enabled: Boolean, method: String, pinHash: String?) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            prefs[KEY_APP_LOCK_METHOD] = method
            if (!pinHash.isNullOrBlank()) {
                prefs[KEY_APP_LOCK_PIN_HASH] = pinHash
            }
            prefs.remove(KEY_APP_LOCK_PIN)
        }
    }

    suspend fun applyRestoredSettings(
        userName: String?,
        currencySymbol: String?,
        defaultHourlyRate: Double?,
        monthlyBudget: Double?,
        savingsBuffer: Double?,
        settings: ShiftlySettingsSnapshot?
    ) {
        context.dataStore.edit { prefs ->
            if (!userName.isNullOrBlank()) prefs[KEY_USER_NAME] = userName.trim()
            if (!currencySymbol.isNullOrBlank()) prefs[KEY_CURRENCY_SYMBOL] = currencySymbol.trim()
            if (defaultHourlyRate != null && defaultHourlyRate > 0.0) {
                prefs[KEY_DEFAULT_HOURLY_RATE] = defaultHourlyRate
            }
            if (monthlyBudget != null && monthlyBudget >= 0.0) {
                prefs[KEY_MONTHLY_BUDGET] = monthlyBudget
            }
            if (savingsBuffer != null && savingsBuffer >= 0.0) {
                prefs[KEY_SAVINGS_BUFFER] = savingsBuffer
            }
            if (settings != null) {
                prefs[KEY_PAY_CYCLE_SCHEDULE] = settings.payCycleSchedule
                prefs[KEY_PAYDAY_REMINDER] = settings.paydayReminderEnabled
                prefs[KEY_SHIFT_REMINDER] = settings.shiftReminderEnabled
                prefs[KEY_NOTIFICATIONS_ENABLED] = settings.notificationsEnabled
                prefs[KEY_ND_START_HOUR] = settings.nightDiffStartHour.coerceIn(18, 23)
                prefs[KEY_ND_MULTIPLIER] = settings.nightDiffMultiplier.coerceIn(1.0, 2.0)
                prefs[KEY_OT_MULTIPLIER] = settings.overtimeMultiplier.coerceIn(1.0, 3.0)
                prefs[KEY_SAVINGS_GOAL_TITLE] = settings.savingsGoalTitle
                prefs[KEY_SAVINGS_GOAL_TARGET] = settings.savingsGoalTarget.coerceAtLeast(100.0)
                prefs[KEY_CATEGORY_BUDGETS] = PreferenceJsonCodec.encodeCategoryBudgets(settings.categoryBudgets)
                prefs[KEY_MANAGED_CATEGORIES] = PreferenceJsonCodec.encodeManagedCategories(settings.managedCategories)
                prefs[KEY_RECURRING_ENTRIES] = PreferenceJsonCodec.encodeRecurringEntries(settings.recurringEntries)
            }
        }
    }

    suspend fun startActiveTimer(startTimeMillis: Long, dateEpochDay: Long, lockedRate: Double) {
        context.dataStore.edit { prefs ->
            prefs[KEY_ACTIVE_TIMER_START] = startTimeMillis
            prefs[KEY_ACTIVE_TIMER_DATE] = dateEpochDay
            prefs[KEY_ACTIVE_TIMER_RATE] = lockedRate
        }
    }

    suspend fun clearActiveTimer() {
        context.dataStore.edit { prefs ->
            prefs.remove(KEY_ACTIVE_TIMER_START)
            prefs.remove(KEY_ACTIVE_TIMER_DATE)
            prefs.remove(KEY_ACTIVE_TIMER_RATE)
        }
    }
}
