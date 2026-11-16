package com.example.shiftly.ui.screens.expense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ExpenseEntity
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
    val fraction: Float,
    val monthlyLimit: Double? = null
)

data class ExpenseUiState(
    val currencySymbol: String = "₱",
    val selectedCategory: String = "All",
    val availableCategories: List<String> = ExpenseViewModel.EXPENSE_CATEGORIES,
    val filterCategories: List<String> = ExpenseViewModel.FILTER_CATEGORIES,
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
        repository.settingsSnapshot,
        _selectedCategory
    ) { currency, allExpenses, settings, selectedCategory ->
        val currentMonth = YearMonth.now()
        val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
        val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

        val visibleCategories = settings.managedCategories
            .filter { it.isVisible && it.name.isNotBlank() }
            .map { it.name.trim() }
            .distinctBy { it.lowercase() }
            .ifEmpty { EXPENSE_CATEGORIES }

        val filterCategories = listOf("All") + visibleCategories
        val effectiveSelectedCategory = if (
            selectedCategory == "All" ||
            visibleCategories.any { it.equals(selectedCategory, ignoreCase = true) }
        ) {
            selectedCategory
        } else {
            "All"
        }

        val monthlyExpenses = allExpenses.filter {
            it.dateEpochDay in monthStartEpoch..monthEndEpoch
        }
        val monthlyTotal = monthlyExpenses.sumOf { it.amount }
        val allTimeTotal = allExpenses.sumOf { it.amount }

        val budgetByCategory = settings.categoryBudgets.associate {
            it.category.trim().lowercase() to it.monthlyLimit
        }

        // Group spending by category for the visual summary card
        val breakdownBase = if (monthlyTotal > 0.0) monthlyExpenses else allExpenses
        val breakdownDenominator = breakdownBase.sumOf { it.amount }.takeIf { it > 0.0 } ?: 1.0

        val categoryBreakdown = breakdownBase
            .groupBy { it.category }
            .map { (category, items) ->
                val sum = items.sumOf { it.amount }
                val limit = budgetByCategory[category.trim().lowercase()]?.takeIf { it > 0.0 }
                CategorySpendSummary(
                    category = category,
                    totalAmount = sum,
                    fraction = (sum / breakdownDenominator).toFloat().coerceIn(0f, 1f),
                    monthlyLimit = limit
                )
            }
            .sortedByDescending { it.totalAmount }

        val filtered = if (effectiveSelectedCategory == "All") {
            allExpenses
        } else {
            allExpenses.filter { it.category.equals(effectiveSelectedCategory, ignoreCase = true) }
        }

        val receiptCount = allExpenses.count { !it.receiptImagePath.isNullOrBlank() }

        ExpenseUiState(
            currencySymbol = currency,
            selectedCategory = effectiveSelectedCategory,
            availableCategories = visibleCategories,
            filterCategories = filterCategories,
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
        if (title.isBlank() || !amount.isFinite() || amount <= 0.0) return
        viewModelScope.launch {
            repository.insertExpense(
                ExpenseEntity(
                    title = title.trim(),
                    amount = amount,
                    category = category,
                    dateEpochDay = date.toEpochDay(),
                    receiptImagePath = receiptImagePath,
                    createdAtMillis = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
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
