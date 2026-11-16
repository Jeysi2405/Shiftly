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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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
    val isEnabled: Boolean
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
    val appLockPin: String = ""
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

        val DEFAULT_RECURRING_ENTRIES = listOf(
            RecurringEntrySetting("r1", "Weekly Allowance", 1500.0, "Weekly", isIncome = true, isEnabled = true),
            RecurringEntrySetting("r2", "Campus Load & Wi-Fi", 199.0, "Weekly", isIncome = false, isEnabled = true),
            RecurringEntrySetting("r3", "Student Cloud Plan", 99.0, "Monthly", isIncome = false, isEnabled = true)
        )
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
        private val KEY_APP_LOCK_PIN = stringPreferencesKey("app_lock_pin")
    }

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
            categoryBudgets = parseCategoryBudgets(prefs[KEY_CATEGORY_BUDGETS]),
            managedCategories = parseManagedCategories(prefs[KEY_MANAGED_CATEGORIES]),
            recurringEntries = parseRecurringEntries(prefs[KEY_RECURRING_ENTRIES]),
            appLockEnabled = prefs[KEY_APP_LOCK_ENABLED] ?: false,
            appLockMethod = prefs[KEY_APP_LOCK_METHOD] ?: "PIN & Fingerprint",
            appLockPin = prefs[KEY_APP_LOCK_PIN] ?: ""
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
        val serialized = items.joinToString(";") { "${it.category.replace(";", "")}:${it.monthlyLimit}" }
        context.dataStore.edit { prefs ->
            prefs[KEY_CATEGORY_BUDGETS] = serialized
        }
    }

    suspend fun setManagedCategories(items: List<ManagedCategorySetting>) {
        val serialized = items.joinToString(";") {
            "${it.name.replace(";", "").replace("|", "")}|${if (it.isVisible) "1" else "0"}"
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_MANAGED_CATEGORIES] = serialized
        }
    }

    suspend fun setRecurringEntries(items: List<RecurringEntrySetting>) {
        val serialized = items.joinToString(";") {
            val cleanTitle = it.title.replace(";", "").replace("|", "")
            val cleanId = it.id.replace(";", "").replace("|", "")
            "$cleanId|$cleanTitle|${it.amount}|${it.frequency}|${if (it.isIncome) "1" else "0"}|${if (it.isEnabled) "1" else "0"}"
        }
        context.dataStore.edit { prefs ->
            prefs[KEY_RECURRING_ENTRIES] = serialized
        }
    }

    suspend fun setAppLock(enabled: Boolean, method: String, pin: String) {
        context.dataStore.edit { prefs ->
            prefs[KEY_APP_LOCK_ENABLED] = enabled
            prefs[KEY_APP_LOCK_METHOD] = method
            prefs[KEY_APP_LOCK_PIN] = pin
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

    private fun parseCategoryBudgets(raw: String?): List<CategoryBudgetSetting> {
        if (raw.isNullOrBlank()) return ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val amount = parts[1].toDoubleOrNull() ?: return@mapNotNull null
                CategoryBudgetSetting(parts[0], amount)
            } else {
                null
            }
        }.ifEmpty { ShiftlySettingsSnapshot.DEFAULT_CATEGORY_BUDGETS }
    }

    private fun parseManagedCategories(raw: String?): List<ManagedCategorySetting> {
        if (raw.isNullOrBlank()) return ShiftlySettingsSnapshot.DEFAULT_MANAGED_CATEGORIES
        return raw.split(";").mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size == 2 && parts[0].isNotBlank()) {
                ManagedCategorySetting(parts[0], parts[1] == "1")
            } else {
                null
            }
        }.ifEmpty { ShiftlySettingsSnapshot.DEFAULT_MANAGED_CATEGORIES }
    }

    private fun parseRecurringEntries(raw: String?): List<RecurringEntrySetting> {
        if (raw == "") return emptyList()
        if (raw == null) return ShiftlySettingsSnapshot.DEFAULT_RECURRING_ENTRIES
        return raw.split(";").mapNotNull { entry ->
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
