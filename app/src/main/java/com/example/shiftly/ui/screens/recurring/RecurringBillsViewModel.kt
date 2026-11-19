package com.example.shiftly.ui.screens.recurring

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.utils.RecurringEngine
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class RecurringBillsUiState(
    val currencySymbol: String = "₱",
    val bills: List<RecurringBillEntity> = emptyList(),
    val enabledMonthlyEstimate: Double = 0.0,
    val activeCount: Int = 0,
    val dueSoonCount: Int = 0,
    val availableCategories: List<String> = ExpenseViewModel.EXPENSE_CATEGORIES.filter { it != "Split Bill" }
)

class RecurringBillsViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    val uiState: StateFlow<RecurringBillsUiState> = combine(
        repository.currencySymbol,
        repository.allRecurringBills,
        repository.settingsSnapshot
    ) { currency, bills, settings ->
        val visibleCategories = settings.managedCategories
            .filter {
                it.isVisible &&
                    it.name.isNotBlank() &&
                    !it.name.equals("Split Bill", ignoreCase = true)
            }
            .map { it.name.trim() }
            .distinctBy { it.lowercase() }
            .ifEmpty { ExpenseViewModel.EXPENSE_CATEGORIES.filter { it != "Split Bill" } }

        val enabledBills = bills.filter { it.isEnabled }
        val monthlyEstimate = enabledBills.sumOf { bill ->
            if (bill.frequency.equals("Weekly", ignoreCase = true)) {
                bill.amount * 4.33
            } else {
                bill.amount
            }
        }
        val dueSoonCount = RecurringEngine.findBillsDueWithinOneDay(
            bills = bills,
            now = LocalDateTime.now()
        ).size

        RecurringBillsUiState(
            currencySymbol = currency,
            bills = bills,
            enabledMonthlyEstimate = monthlyEstimate,
            activeCount = enabledBills.size,
            dueSoonCount = dueSoonCount,
            availableCategories = visibleCategories
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = RecurringBillsUiState()
        )

    fun saveBill(
        existing: RecurringBillEntity?,
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        dueDate: LocalDate,
        dueHour: Int,
        dueMinute: Int,
        isEnabled: Boolean,
        onResult: (Int) -> Unit = {}
    ) {
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty() || !amount.isFinite() || amount <= 0.0) return

        val normalizedFreq = if (frequency.equals("Weekly", ignoreCase = true)) "Weekly" else "Monthly"
        val cleanCategory = category.ifBlank { "Bills" }
        val safeHour = dueHour.coerceIn(0, 23)
        val safeMinute = dueMinute.coerceIn(0, 59)

        viewModelScope.launch {
            if (existing == null) {
                repository.insertRecurringBill(
                    RecurringBillEntity(
                        title = cleanTitle,
                        amount = amount,
                        category = cleanCategory,
                        frequency = normalizedFreq,
                        dueEpochDay = dueDate.toEpochDay(),
                        dueHour = safeHour,
                        dueMinute = safeMinute,
                        isEnabled = isEnabled,
                        createdAtMillis = System.currentTimeMillis()
                    )
                )
            } else {
                repository.updateRecurringBill(
                    existing.copy(
                        title = cleanTitle,
                        amount = amount,
                        category = cleanCategory,
                        frequency = normalizedFreq,
                        dueEpochDay = dueDate.toEpochDay(),
                        dueHour = safeHour,
                        dueMinute = safeMinute,
                        isEnabled = isEnabled
                    )
                )
            }
            val posted = repository.processDueRecurringBills(LocalDateTime.now())
            onResult(posted)
        }
    }

    fun toggleBillEnabled(
        bill: RecurringBillEntity,
        enabled: Boolean,
        onResult: (Int) -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.toggleRecurringBillEnabled(bill, enabled)
            val posted = if (enabled) {
                repository.processDueRecurringBills(LocalDateTime.now())
            } else {
                0
            }
            onResult(posted)
        }
    }

    fun deleteBill(bill: RecurringBillEntity) {
        viewModelScope.launch {
            repository.deleteRecurringBill(bill)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return RecurringBillsViewModel(repository) as T
        }
    }
}
