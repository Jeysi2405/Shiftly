package com.example.shiftly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.IouEntrySetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.PaycheckDeductionsSetting
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.YearMonth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ToolsUiState(
    val currencySymbol: String = "₱",
    val monthlyBudget: Double = 0.0,
    val monthShiftCount: Int = 0,
    val monthShiftHours: Double = 0.0,
    val monthShiftEarnings: Double = 0.0,
    val monthExpenseCount: Int = 0,
    val monthSpent: Double = 0.0,
    val pendingSplitsAmount: Double = 0.0,
    val pendingBillsCount: Int = 0,
    val totalSplitBillsCount: Int = 0,
    val activeGoalTitle: String? = null,
    val activeGoalSaved: Double = 0.0,
    val activeGoalTarget: Double = 0.0,
    val enabledRecurringCount: Int = 0,
    val totalRecurringCount: Int = 0,
    val totalAllowanceAmount: Double = 0.0,
    val allowanceEntryCount: Int = 0,
    val cutoffScheduleLabel: String = CutoffScheduleConfig().displayLabel(),
    val paycheckDeductions: PaycheckDeductionsSetting = PaycheckDeductionsSetting(),
    val iouEntries: List<IouEntrySetting> = emptyList(),
    val netPayTakeHome: Double = 0.0,
    val totalDeductions: Double = 0.0,
    val iouSummaryText: String = "All settled",
    val shifts: List<ShiftEntity> = emptyList(),
    val expenses: List<ExpenseEntity> = emptyList(),
    val savingsGoals: List<SavingsGoalWithDeposits> = emptyList(),
    val settings: ShiftlySettingsSnapshot = ShiftlySettingsSnapshot()
)

class ToolsViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val corePrefsFlow = combine(
        repository.currencySymbol,
        repository.monthlyBudget,
        repository.settingsSnapshot
    ) { currency, monthlyBudget, settings ->
        Triple(currency, monthlyBudget, settings)
    }

    val uiState: StateFlow<ToolsUiState> = combine(
        corePrefsFlow,
        repository.allShifts,
        repository.allExpenses,
        repository.allSplitBills,
        repository.allSavingsGoals,
        repository.allRecurringBills,
        repository.allAllowanceEntries,
        repository.paycheckDeductions,
        repository.iouEntries
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val prefs = values[0] as Triple<String, Double, ShiftlySettingsSnapshot>
        @Suppress("UNCHECKED_CAST")
        val shifts = values[1] as List<ShiftEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[2] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splitBills = values[3] as List<SplitBillWithParticipants>
        @Suppress("UNCHECKED_CAST")
        val savingsGoals = values[4] as List<SavingsGoalWithDeposits>
        @Suppress("UNCHECKED_CAST")
        val recurringBills = values[5] as List<RecurringBillEntity>
        @Suppress("UNCHECKED_CAST")
        val allowanceEntries = values[6] as List<AllowanceEntryEntity>
        val deductions = values[7] as PaycheckDeductionsSetting
        @Suppress("UNCHECKED_CAST")
        val iouItems = values[8] as List<IouEntrySetting>

        computeToolsUiState(
            currencySymbol = prefs.first,
            monthlyBudget = prefs.second,
            settings = prefs.third,
            shifts = shifts,
            expenses = expenses,
            splitBills = splitBills,
            savingsGoals = savingsGoals,
            recurringBills = recurringBills,
            allowanceEntries = allowanceEntries,
            paycheckDeductions = deductions,
            iouEntries = iouItems
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = ToolsUiState()
        )

    fun updateBudgets(monthlyBudget: Double, categoryBudgets: List<CategoryBudgetSetting>) {
        viewModelScope.launch {
            repository.updateMonthlyBudget(monthlyBudget)
            repository.updateCategoryBudgets(categoryBudgets)
        }
    }

    fun updateManagedCategories(categories: List<ManagedCategorySetting>) {
        viewModelScope.launch {
            repository.updateManagedCategories(categories)
        }
    }

    fun updatePaycheckDeductions(setting: PaycheckDeductionsSetting) {
        viewModelScope.launch {
            repository.updatePaycheckDeductions(setting)
        }
    }

    fun updateIouEntries(items: List<IouEntrySetting>) {
        viewModelScope.launch {
            repository.updateIouEntries(items)
        }
    }

    fun createSavingsGoal(title: String, targetAmount: Double, targetDateEpochDay: Long) {
        viewModelScope.launch {
            repository.createSavingsGoal(title, targetAmount, targetDateEpochDay)
        }
    }

    fun setActiveSavingsGoal(goalId: Long) {
        viewModelScope.launch {
            repository.setActiveSavingsGoal(goalId)
        }
    }

    fun addSavingsDeposit(goal: SavingsGoalEntity, amount: Double, note: String) {
        viewModelScope.launch {
            repository.addSavingsDeposit(goal = goal, amount = amount, note = note)
        }
    }

    fun deleteSavingsDeposit(deposit: SavingsDepositEntity) {
        viewModelScope.launch {
            repository.deleteSavingsDeposit(deposit)
        }
    }

    fun deleteSavingsGoal(goal: SavingsGoalEntity) {
        viewModelScope.launch {
            repository.deleteSavingsGoal(goal)
        }
    }

    companion object {
        fun computeToolsUiState(
            currencySymbol: String,
            monthlyBudget: Double,
            settings: ShiftlySettingsSnapshot,
            shifts: List<ShiftEntity>,
            expenses: List<ExpenseEntity>,
            splitBills: List<SplitBillWithParticipants>,
            savingsGoals: List<SavingsGoalWithDeposits>,
            recurringBills: List<RecurringBillEntity>,
            allowanceEntries: List<AllowanceEntryEntity>,
            paycheckDeductions: PaycheckDeductionsSetting = PaycheckDeductionsSetting(),
            iouEntries: List<IouEntrySetting> = emptyList(),
            currentMonth: YearMonth = YearMonth.now()
        ): ToolsUiState {
            val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
            val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

            var monthShiftCount = 0
            var monthShiftMinutes = 0L
            var monthShiftEarnings = 0.0
            for (shift in shifts) {
                if (shift.dateEpochDay in monthStartEpoch..monthEndEpoch) {
                    monthShiftCount++
                    monthShiftMinutes += shift.durationMinutes
                    monthShiftEarnings += shift.totalEarned
                }
            }

            var monthExpenseCount = 0
            var monthSpent = 0.0
            for (expense in expenses) {
                if (expense.dateEpochDay in monthStartEpoch..monthEndEpoch) {
                    monthExpenseCount++
                    monthSpent += expense.amount
                }
            }

            var pendingSplitsAmount = 0.0
            var pendingBillsCount = 0
            for (item in splitBills) {
                var hasUnpaidFriend = false
                for (p in item.participants) {
                    if (!p.isCurrentUser && !p.hasPaid) {
                        pendingSplitsAmount += p.amountOwed
                        hasUnpaidFriend = true
                    }
                }
                if (hasUnpaidFriend) pendingBillsCount++
            }

            val activeGoal = savingsGoals.firstOrNull { it.goal.isActive } ?: savingsGoals.firstOrNull()
            val activeGoalSaved = activeGoal?.deposits?.sumOf { it.amount } ?: 0.0
            val enabledRecurringCount = recurringBills.count { it.isEnabled }
            val totalAllowanceAmount = allowanceEntries.sumOf { it.amount }

            val totalDeductions = paycheckDeductions.calculateTotalDeductions(monthShiftEarnings)
            val netPayTakeHome = (monthShiftEarnings - totalDeductions).coerceAtLeast(0.0)

            val unsettledIou = iouEntries.filter { !it.isSettled }
            val netIou = unsettledIou.sumOf { if (it.isLent) it.amount else -it.amount }
            val iouSummaryText = when {
                unsettledIou.isEmpty() -> "All settled"
                netIou > 0.0 -> "${ShiftlyFormatters.formatCurrency(currencySymbol, netIou)} owed to you"
                netIou < 0.0 -> "${ShiftlyFormatters.formatCurrency(currencySymbol, -netIou)} you owe"
                else -> "Balanced debts"
            }

            return ToolsUiState(
                currencySymbol = currencySymbol,
                monthlyBudget = monthlyBudget,
                monthShiftCount = monthShiftCount,
                monthShiftHours = monthShiftMinutes / 60.0,
                monthShiftEarnings = monthShiftEarnings,
                monthExpenseCount = monthExpenseCount,
                monthSpent = monthSpent,
                pendingSplitsAmount = pendingSplitsAmount,
                pendingBillsCount = pendingBillsCount,
                totalSplitBillsCount = splitBills.size,
                activeGoalTitle = activeGoal?.goal?.title,
                activeGoalSaved = activeGoalSaved,
                activeGoalTarget = activeGoal?.goal?.targetAmount ?: 0.0,
                enabledRecurringCount = enabledRecurringCount,
                totalRecurringCount = recurringBills.size,
                totalAllowanceAmount = totalAllowanceAmount,
                allowanceEntryCount = allowanceEntries.size,
                cutoffScheduleLabel = settings.cutoffScheduleConfig().displayLabel(),
                paycheckDeductions = paycheckDeductions,
                iouEntries = iouEntries,
                netPayTakeHome = netPayTakeHome,
                totalDeductions = totalDeductions,
                iouSummaryText = iouSummaryText,
                shifts = shifts,
                expenses = expenses,
                savingsGoals = savingsGoals,
                settings = settings
            )
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ToolsViewModel(repository) as T
        }
    }
}
