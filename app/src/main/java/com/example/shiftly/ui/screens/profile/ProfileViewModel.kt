package com.example.shiftly.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.repository.ShiftlyRepository
import java.io.File
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val userName: String = "Maya",
    val defaultHourlyRate: Double = 96.38,
    val currencySymbol: String = "₱",
    val monthlyBudget: Double = 3000.0,
    val savingsBuffer: Double = 1000.0,
    val totalShifts: Int = 0,
    val totalHoursWorked: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val settings: ShiftlySettingsSnapshot = ShiftlySettingsSnapshot()
)

class ProfileViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        repository.userName,
        repository.defaultHourlyRate,
        repository.currencySymbol,
        repository.monthlyBudget,
        repository.savingsBuffer,
        repository.totalShiftCount,
        repository.totalMinutesWorked,
        repository.totalLifetimeEarnings,
        repository.totalExtraIncomeAmount,
        repository.totalExpenseAmount,
        repository.settingsSnapshot
    ) { values ->
        val name = values[0] as String
        val rate = values[1] as Double
        val currency = values[2] as String
        val budget = values[3] as Double
        val buffer = values[4] as Double
        val shiftCount = values[5] as Int
        val minutes = (values[6] as Int?) ?: 0
        val shiftEarnings = (values[7] as Double?) ?: 0.0
        val extraIncome = (values[8] as Double?) ?: 0.0
        val expenses = (values[9] as Double?) ?: 0.0
        val settings = values[10] as ShiftlySettingsSnapshot

        ProfileUiState(
            userName = name,
            defaultHourlyRate = rate,
            currencySymbol = currency,
            monthlyBudget = budget,
            savingsBuffer = buffer,
            totalShifts = shiftCount,
            totalHoursWorked = minutes / 60.0,
            totalEarnings = shiftEarnings + extraIncome,
            totalExpenses = expenses,
            settings = settings
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun updateUserName(name: String) {
        viewModelScope.launch { repository.updateUserName(name) }
    }

    fun changeCurrency(symbol: String, conversionRate: Double?) {
        viewModelScope.launch {
            repository.changeCurrency(symbol, conversionRate)
        }
    }

    fun updateNotificationsEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.updateNotificationsEnabled(enabled) }
    }

    fun updatePayCycle(schedule: String, paydayReminder: Boolean, shiftReminder: Boolean) {
        viewModelScope.launch { repository.updatePayCycleSettings(schedule, paydayReminder, shiftReminder) }
    }

    fun updateShiftRules(ndStartHour: Int, ndMultiplier: Double, otMultiplier: Double) {
        viewModelScope.launch { repository.updateShiftRules(ndStartHour, ndMultiplier, otMultiplier) }
    }

    fun updateBudgets(monthlyBudget: Double, categoryBudgets: List<CategoryBudgetSetting>) {
        viewModelScope.launch {
            repository.updateMonthlyBudget(monthlyBudget)
            repository.updateCategoryBudgets(categoryBudgets)
        }
    }

    fun updateSavingsGoal(title: String, target: Double, buffer: Double) {
        viewModelScope.launch {
            repository.updateSavingsGoal(title, target)
            repository.updateSavingsBuffer(buffer)
        }
    }

    fun updateManagedCategories(items: List<ManagedCategorySetting>) {
        viewModelScope.launch { repository.updateManagedCategories(items) }
    }

    fun updateRecurringEntries(items: List<RecurringEntrySetting>, onProcessed: (Int) -> Unit = {}) {
        viewModelScope.launch {
            repository.updateRecurringEntries(items)
            val posted = repository.processDueRecurringEntries()
            onProcessed(posted)
        }
    }

    fun updateAppLock(enabled: Boolean, method: String, newPin: String?) {
        viewModelScope.launch { repository.updateAppLock(enabled, method, newPin) }
    }

    fun exportBackup(onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generateOfflineBackupSummary() })
        }
    }

    fun exportBackupZip(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generateBackupZipFile() })
        }
    }

    fun restoreBackup(rawJson: String, mode: RestoreMode, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.restoreFromBackupJson(rawJson, mode))
        }
    }

    fun restoreBackupBytes(bytes: ByteArray, mode: RestoreMode, onResult: (Result<String>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.restoreFromBackupBytes(bytes, mode))
        }
    }

    fun exportCsv(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generateCsvExportFile() })
        }
    }

    fun exportPdf(onResult: (Result<File>) -> Unit) {
        viewModelScope.launch {
            onResult(runCatching { repository.generatePdfSummaryFile() })
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}
