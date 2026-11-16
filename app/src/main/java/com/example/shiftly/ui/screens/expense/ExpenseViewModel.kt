package com.example.shiftly.ui.screens.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ReceiptStorageHelper
import com.example.shiftly.data.repository.ShiftlyRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategorySpendSummary(
    val category: String,
    val totalAmount: Double,
    val fraction: Float
)

data class ExpenseUiState(
    val currencySymbol: String = "₱",
    val selectedCategory: String = "All",
    val monthlyTotal: Double = 0.0,
    val allTimeTotal: Double = 0.0,
    val categoryBreakdown: List<CategorySpendSummary> = emptyList(),
    val filteredExpenses: List<ExpenseEntity> = emptyList(),
    val totalReceiptCount: Int = 0
)

class ExpenseViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    companion object {
        val EXPENSE_CATEGORIES = listOf(
            "Food",
            "Transport",
            "Bills",
            "Supplies",
            "Split Bill",
            "Other"
        )
        val FILTER_CATEGORIES = listOf("All") + EXPENSE_CATEGORIES
    }

    private val _selectedCategory = MutableStateFlow("All")

    val uiState: StateFlow<ExpenseUiState> = combine(
        repository.currencySymbol,
        repository.allExpenses,
        _selectedCategory
    ) { currency, allExpenses, selectedCategory ->
        val currentMonth = YearMonth.now()
        val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
        val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

        val monthlyExpenses = allExpenses.filter {
            it.dateEpochDay in monthStartEpoch..monthEndEpoch
        }
        val monthlyTotal = monthlyExpenses.sumOf { it.amount }
        val allTimeTotal = allExpenses.sumOf { it.amount }

        // Group spending by category for the visual summary card
        val breakdownBase = if (monthlyTotal > 0.0) monthlyExpenses else allExpenses
        val breakdownDenominator = breakdownBase.sumOf { it.amount }.takeIf { it > 0.0 } ?: 1.0

        val categoryBreakdown = breakdownBase
            .groupBy { it.category }
            .map { (category, items) ->
                val sum = items.sumOf { it.amount }
                CategorySpendSummary(
                    category = category,
                    totalAmount = sum,
                    fraction = (sum / breakdownDenominator).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.totalAmount }

        val filtered = if (selectedCategory == "All") {
            allExpenses
        } else {
            allExpenses.filter { it.category.equals(selectedCategory, ignoreCase = true) }
        }

        val receiptCount = allExpenses.count { !it.receiptImagePath.isNullOrBlank() }

        ExpenseUiState(
            currencySymbol = currency,
            selectedCategory = selectedCategory,
            monthlyTotal = monthlyTotal,
            allTimeTotal = allTimeTotal,
            categoryBreakdown = categoryBreakdown,
            filteredExpenses = filtered,
            totalReceiptCount = receiptCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ExpenseUiState()
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun addExpense(
        title: String,
        amount: Double,
        category: String,
        date: LocalDate,
        receiptImagePath: String?
    ) {
        if (title.isBlank() || amount <= 0.0) return
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    dateEpochDay = date.toEpochDay(),
                    receiptImagePath = receiptImagePath
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            ReceiptStorageHelper.deleteReceiptFile(expense.receiptImagePath)
            repository.deleteExpense(expense)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExpenseViewModel(repository) as T
        }
    }
}