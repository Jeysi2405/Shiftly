package com.example.shiftly.ui.screens.profile

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.repository.ShiftlyRepository
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class ProfileUiState(
    val userName: String = "",
    val jobTitle: String = "",
    val workplace: String = "",
    val workType: String = "Full-Time",
    val targetWeeklyHours: Double = 40.0,
    val defaultHourlyRate: Double = 0.0,
    val currencySymbol: String = "₱",
    val monthlyBudget: Double = 0.0,
    val savingsBuffer: Double = 0.0,
    val totalShifts: Int = 0,
    val totalHoursWorked: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val settings: ShiftlySettingsSnapshot = ShiftlySettingsSnapshot()
)

class ProfileViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private data class ProfileCorePrefs(
        val userName: String,
        val defaultHourlyRate: Double,
        val currencySymbol: String,
        val monthlyBudget: Double,
        val savingsBuffer: Double
    )

    private data class ProfileTotals(
        val totalShifts: Int,
        val totalMinutes: Int,
        val totalEarnings: Double,
        val totalExpenses: Double
    )

    private val identityPrefsFlow = combine(
        repository.userName,
        repository.defaultHourlyRate
    ) { userName, rate ->
        userName to rate
    }

    private val financePrefsFlow = combine(
        repository.currencySymbol,
        repository.monthlyBudget,
        repository.savingsBuffer
    ) { currency, budget, buffer ->
        Triple(currency, budget, buffer)
    }

    private val corePrefsFlow = combine(
        identityPrefsFlow,
        financePrefsFlow
    ) { identity, finance ->
        ProfileCorePrefs(
            userName = identity.first,
            defaultHourlyRate = identity.second,
            currencySymbol = finance.first,
            monthlyBudget = finance.second,
            savingsBuffer = finance.third
        )
    }

    private val totalsFlow = combine(
        repository.totalShiftCount,
        repository.totalMinutesWorked,
        repository.totalLifetimeEarnings,
        repository.totalExtraIncomeAmount,
        repository.allAllowanceEntries,
        repository.totalExpenseAmount
    ) { values ->
        val shiftCount = values[0] as Int
        val minutes = values[1] as Int?
        val shiftEarnings = values[2] as Double?
        val extraIncome = values[3] as Double?
        @Suppress("UNCHECKED_CAST")
        val allowanceEntries = values[4] as List<AllowanceEntryEntity>
        val expenses = values[5] as Double?

        val manualAllowancesTotal = allowanceEntries
            .filter { it.cutoffKey.isNullOrBlank() }
            .sumOf { it.amount }

        ProfileTotals(
            totalShifts = shiftCount,
            totalMinutes = minutes ?: 0,
            totalEarnings = (shiftEarnings ?: 0.0) + (extraIncome ?: 0.0) + manualAllowancesTotal,
            totalExpenses = expenses ?: 0.0
        )
    }

    val uiState: StateFlow<ProfileUiState> = combine(
        corePrefsFlow,
        totalsFlow,
        repository.settingsSnapshot
    ) { core, totals, settings ->
        ProfileUiState(
            userName = core.userName,
            jobTitle = settings.jobTitle,
            workplace = settings.workplace,
            workType = settings.workType,
            targetWeeklyHours = settings.targetWeeklyHours,
            defaultHourlyRate = core.defaultHourlyRate,
            currencySymbol = core.currencySymbol,
            monthlyBudget = core.monthlyBudget,
            savingsBuffer = core.savingsBuffer,
            totalShifts = totals.totalShifts,
            totalHoursWorked = totals.totalMinutes / 60.0,
            totalEarnings = totals.totalEarnings,
            totalExpenses = totals.totalExpenses,
            settings = settings
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ProfileUiState()
        )

    fun updateProfileDetails(
        userName: String,
        jobTitle: String,
        workplace: String,
        workType: String,
        targetWeeklyHours: Double,
        defaultHourlyRate: Double? = null
    ) {
        viewModelScope.launch {
            repository.updateProfileDetails(
                userName = userName,
                jobTitle = jobTitle,
                workplace = workplace,
                workType = workType,
                targetWeeklyHours = targetWeeklyHours
            )
            if (defaultHourlyRate != null && defaultHourlyRate.isFinite() && defaultHourlyRate >= 0.0) {
                repository.updateDefaultHourlyRate(defaultHourlyRate)
            }
        }
    }

    fun changeCurrency(symbol: String, conversionRate: Double?) {
        viewModelScope.launch {
            repository.changeCurrency(symbol, conversionRate)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch {
            repository.updateNotificationsEnabled(enabled)
            if (enabled) {
                repository.syncInAppNotifications()
            }
        }
    }

    fun updateNotificationPreferences(
        notificationsEnabled: Boolean,
        billReminderEnabled: Boolean,
        shiftReminderEnabled: Boolean,
        savingsReminderEnabled: Boolean
    ) {
        viewModelScope.launch {
            repository.updateNotificationPreferences(
                notificationsEnabled = notificationsEnabled,
                billReminderEnabled = billReminderEnabled,
                shiftReminderEnabled = shiftReminderEnabled,
                savingsReminderEnabled = savingsReminderEnabled
            )
            if (notificationsEnabled) {
                repository.syncInAppNotifications()
            }
        }
    }

    fun updateBudgets(monthlyBudget: Double, categoryBudgets: List<CategoryBudgetSetting>) {
        viewModelScope.launch {
            repository.updateMonthlyBudget(monthlyBudget)
            repository.updateCategoryBudgets(categoryBudgets)
        }
    }

    fun updateManagedCategories(items: List<ManagedCategorySetting>) {
        viewModelScope.launch { repository.updateManagedCategories(items) }
    }

    fun updateThemeMode(mode: String) {
        viewModelScope.launch { repository.updateThemeMode(mode) }
    }

    fun updateAppLock(
        enabled: Boolean,
        method: String,
        newPin: String?,
        timeoutSeconds: Int? = null
    ) {
        viewModelScope.launch {
            repository.updateAppLock(
                enabled = enabled,
                method = method,
                newPin = newPin,
                timeoutSeconds = timeoutSeconds
            )
        }
    }

    fun exportBackup(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generateOfflineBackupSummary() })
        }
    }

    fun restoreBackupBytes(bytes: ByteArray, mode: RestoreMode, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.restoreFromBackupBytes(bytes, mode))
        }
    }

    fun exportShiftsCsv(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generateShiftsCsvExportFile() })
        }
    }

    fun exportExpensesCsv(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generateExpensesCsvExportFile() })
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}
