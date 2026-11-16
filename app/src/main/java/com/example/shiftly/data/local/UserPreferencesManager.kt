package com.example.shiftly.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
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
