package com.example.shiftly.ui.screens.expense

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class CategorySpendSummary(
    val category: String,
    val totalAmount: Double,
    val fraction: Float,
    val monthlyLimit: Double? = null
)

@Immutable
data class ExpenseUiState(
    val currencySymbol: String = "₱",
    val monthlyBudget: Double = 0.0,
    val selectedMonth: YearMonth? = YearMonth.now(),
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
            "Savings",
            "Split Bill",
            "Other"
        )
        val FILTER_CATEGORIES = listOf("All") + EXPENSE_CATEGORIES

        fun filterExpensesByMonth(
            expenses: List<ExpenseEntity>,
            selectedMonth: YearMonth?
        ): List<ExpenseEntity> {
            if (selectedMonth == null) return expenses
            val startEpoch = selectedMonth.atDay(1).toEpochDay()
            val endEpoch = selectedMonth.atEndOfMonth().toEpochDay()
            return expenses.filter { it.dateEpochDay in startEpoch..endEpoch }
        }
    }

    private val _selectedCategory = MutableStateFlow("All")
    private val _selectedMonth = MutableStateFlow<YearMonth?>(YearMonth.now())

    val uiState: StateFlow<ExpenseUiState> = combine(
        combine(
            repository.currencySymbol,
            repository.monthlyBudget,
            repository.allExpenses,
            repository.settingsSnapshot
        ) { currency, monthlyBudget, allExpenses, settings ->
            ExpenseInputs(currency, monthlyBudget, allExpenses, settings)
        },
        _selectedCategory,
        _selectedMonth
    ) { inputs, selectedCategory, selectedMonth ->
        val (currency, monthlyBudget, allExpenses, settings) = inputs

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

        val periodExpenses = filterExpensesByMonth(allExpenses, selectedMonth)
        val periodTotal = periodExpenses.sumOf { it.amount }
        val allTimeTotal = allExpenses.sumOf { it.amount }

        val budgetByCategory = settings.categoryBudgets.associate {
            it.category.trim().lowercase() to it.monthlyLimit
        }

        val breakdownDenominator = periodTotal.takeIf { it > 0.0 } ?: 1.0
        val categoryBreakdown = periodExpenses
            .groupBy { it.category }
            .map { (category, items) ->
                val sum = items.sumOf { it.amount }
                val limit = if (selectedMonth != null) {
                    budgetByCategory[category.trim().lowercase()]?.takeIf { it > 0.0 }
                } else {
                    null
                }
                CategorySpendSummary(
                    category = category,
                    totalAmount = sum,
                    fraction = (sum / breakdownDenominator).toFloat().coerceIn(0f, 1f),
                    monthlyLimit = limit
                )
            }
            .sortedByDescending { it.totalAmount }

        val filtered = if (effectiveSelectedCategory == "All") {
            periodExpenses
        } else {
            periodExpenses.filter { it.category.equals(effectiveSelectedCategory, ignoreCase = true) }
        }

        val receiptCount = periodExpenses.count { !it.receiptImagePath.isNullOrBlank() }

        ExpenseUiState(
            currencySymbol = currency,
            monthlyBudget = monthlyBudget,
            selectedMonth = selectedMonth,
            selectedCategory = effectiveSelectedCategory,
            availableCategories = visibleCategories,
            filterCategories = filterCategories,
            monthlyTotal = periodTotal,
            allTimeTotal = allTimeTotal,
            categoryBreakdown = categoryBreakdown,
            filteredExpenses = filtered,
            totalReceiptCount = receiptCount
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ExpenseUiState()
        )

    private data class ExpenseInputs(
        val currency: String,
        val monthlyBudget: Double,
        val allExpenses: List<ExpenseEntity>,
        val settings: com.example.shiftly.data.local.ShiftlySettingsSnapshot
    )

    fun selectCategory(category: String) {
        _selectedCategory.value = category
    }

    fun selectMonth(month: YearMonth?) {
        _selectedMonth.value = month
    }

    fun shiftMonth(delta: Long) {
        val base = _selectedMonth.value ?: YearMonth.now()
        _selectedMonth.value = base.plusMonths(delta)
    }

    fun toggleAllMonths() {
        _selectedMonth.value = if (_selectedMonth.value == null) YearMonth.now() else null
    }

    fun resetFilters() {
        if (_selectedCategory.value != "All") {
            _selectedCategory.value = "All"
        } else {
            _selectedMonth.value = null
        }
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
