package com.example.shiftly.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.repository.ShiftlyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProfileUiState(
    val defaultHourlyRate: Double = 0.0,
    val currencySymbol: String = "₱",
    val totalShifts: Int = 0,
    val totalHoursWorked: Double = 0.0,
    val totalEarnings: Double = 0.0,
    val totalExpenses: Double = 0.0
)

class ProfileViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine<Any?, ProfileUiState>(
        repository.defaultHourlyRate,
        repository.currencySymbol,
        repository.totalShiftCount,
        repository.totalMinutesWorked,
        repository.totalLifetimeEarnings,
        repository.totalExpenseAmount
    ) { values ->
        val rate = values[0] as Double
        val currency = values[1] as String
        val shiftCount = values[2] as Int
        val minutes = (values[3] as Int?) ?: 0
        val earnings = (values[4] as Double?) ?: 0.0
        val expenses = (values[5] as Double?) ?: 0.0

        ProfileUiState(
            defaultHourlyRate = rate,
            currencySymbol = currency,
            totalShifts = shiftCount,
            totalHoursWorked = minutes / 60.0,
            totalEarnings = earnings,
            totalExpenses = expenses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState()
    )

    fun saveDefaultRate(rateText: String) {
        val parsed = rateText.toDoubleOrNull() ?: return
        viewModelScope.launch {
            repository.updateDefaultHourlyRate(parsed)
        }
    }

    fun selectCurrency(symbol: String) {
        viewModelScope.launch {
            repository.updateCurrencySymbol(symbol)
        }
    }

    fun exportBackup(onReady: (String) -> Unit) {
        viewModelScope.launch {
            val report = repository.generateOfflineBackupSummary()
            onReady(report)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProfileViewModel(repository) as T
        }
    }
}