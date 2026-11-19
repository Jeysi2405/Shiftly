package com.example.shiftly.ui.screens.allowance

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.CutoffCalculator
import com.example.shiftly.utils.CutoffWindow
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class AllowanceUiState(
    val currencySymbol: String = "₱",
    val entries: List<AllowanceEntryEntity> = emptyList(),
    val totalAllowanceBalance: Double = 0.0,
    val salaryTotal: Double = 0.0,
    val businessTotal: Double = 0.0,
    val othersTotal: Double = 0.0,
    val cutoffConfig: CutoffScheduleConfig = CutoffScheduleConfig(),
    val activeCutoffWindow: CutoffWindow = CutoffCalculator.resolveCutoffWindow(LocalDate.now()),
    val pendingDutiesEarnings: Double = 0.0,
    val pendingShiftCount: Int = 0
)

class AllowanceViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    val uiState: StateFlow<AllowanceUiState> = combine(
        repository.currencySymbol,
        repository.allAllowanceEntries,
        repository.allShifts,
        repository.settingsSnapshot
    ) { currency, entries, shifts, settings ->
        val today = LocalDate.now()
        val cutoffConfig = settings.cutoffScheduleConfig()
        val activeWindow = CutoffCalculator.resolveCutoffWindow(today, cutoffConfig)
        val creditedKeys = entries.mapNotNull { it.cutoffKey?.takeIf(String::isNotBlank) }.toSet()

        val pendingShifts = shifts.filter { shift ->
            val shiftDate = LocalDate.ofEpochDay(shift.dateEpochDay)
            val window = CutoffCalculator.resolveCutoffWindow(shiftDate, cutoffConfig)
            !creditedKeys.contains(window.key)
        }

        AllowanceUiState(
            currencySymbol = currency,
            entries = entries,
            totalAllowanceBalance = entries.sumOf { it.amount },
            salaryTotal = entries.filter { it.source == AllowanceSource.SALARY }.sumOf { it.amount },
            businessTotal = entries.filter { it.source == AllowanceSource.BUSINESS }.sumOf { it.amount },
            othersTotal = entries.filter { it.source == AllowanceSource.OTHERS }.sumOf { it.amount },
            cutoffConfig = cutoffConfig,
            activeCutoffWindow = activeWindow,
            pendingDutiesEarnings = pendingShifts.sumOf { it.totalEarned },
            pendingShiftCount = pendingShifts.size
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AllowanceUiState()
        )

    fun addManualAllowance(
        title: String,
        amount: Double,
        source: String,
        date: LocalDate
    ) {
        viewModelScope.launch {
            repository.addManualAllowanceEntry(
                title = title,
                amount = amount,
                source = source,
                dateEpochDay = date.toEpochDay()
            )
        }
    }

    fun deleteAllowanceEntry(entry: AllowanceEntryEntity) {
        viewModelScope.launch {
            repository.deleteAllowanceEntry(entry)
        }
    }

    fun updateCutoffSchedule(
        config: CutoffScheduleConfig,
        onUpdated: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.updateCutoffSchedule(config)
            onUpdated()
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AllowanceViewModel(repository) as T
        }
    }
}
