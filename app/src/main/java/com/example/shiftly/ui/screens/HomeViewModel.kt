package com.example.shiftly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.utils.CutoffCalculator
import com.example.shiftly.utils.CutoffShiftSummary
import com.example.shiftly.utils.CutoffWindow
import com.example.shiftly.utils.SavingsGoalMath
import com.example.shiftly.utils.SavingsGoalProgress
import com.example.shiftly.utils.SplitBillMath
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

fun greetingForHour(hourOfDay: Int): String {
    val normalized = ((hourOfDay % 24) + 24) % 24
    return when (normalized) {
        in 5..11 -> "Good morning,"
        in 12..17 -> "Good afternoon,"
        else -> "Good evening,"
    }
}

data class SavingsGoalCardItem(
    val goalWithDeposits: SavingsGoalWithDeposits,
    val progress: SavingsGoalProgress,
    val targetDate: LocalDate
)

sealed class TimelineActivityItem(
    val sortEpochDay: Long,
    val sortTimestamp: Long
) {
    class ShiftActivity(
        val id: Long,
        val title: String,
        val timeRange: String,
        val durationLabel: String,
        val amount: Double,
        epochDay: Long,
        timestamp: Long
    ) : TimelineActivityItem(epochDay, timestamp)

    class ExpenseActivity(
        val id: Long,
        val title: String,
        val category: String,
        val timeLabel: String,
        val amount: Double,
        epochDay: Long,
        timestamp: Long
    ) : TimelineActivityItem(epochDay, timestamp)

    class ExtraIncomeActivity(
        val id: Long,
        val title: String,
        val timeLabel: String,
        val amount: Double,
        epochDay: Long,
        timestamp: Long
    ) : TimelineActivityItem(epochDay, timestamp)
}

data class HomeUiState(
    val userName: String = "",
    val profilePhotoPath: String? = null,
    val currencySymbol: String = "₱",
    val hourlyRate: Double = 0.0,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val mtdIncome: Double = 0.0,
    val mtdExpenses: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val pendingSplitsOwed: Double = 0.0,
    val pendingSplitBillCount: Int = 0,
    val cutoffSummary: CutoffShiftSummary = CutoffShiftSummary(
        window = CutoffWindow(
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            isFirstCutoff = true,
            label = ""
        ),
        totalEarnings = 0.0,
        shiftDaysCount = 0,
        totalHours = 0.0
    ),
    val savingsGoals: List<SavingsGoalCardItem> = emptyList(),
    val currentMonthName: String = LocalDate.now().month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
    val monthlyTopCategories: List<CategorySpendSummary> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val isShowingSelectedDateOnly: Boolean = false,
    val selectedDateHasLogs: Boolean = false,
    val shiftEpochDays: Set<Long> = emptySet(),
    val expenseEpochDays: Set<Long> = emptySet(),
    val timelineItems: List<TimelineActivityItem> = emptyList(),
    val activeTimer: ActiveTimerState = ActiveTimerState()
)

class HomeViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    private data class CorePrefs(
        val userName: String,
        val profilePhotoPath: String?,
        val currency: String,
        val rate: Double,
        val budget: Double,
        val activeTimer: ActiveTimerState
    )

    private val corePrefsFlow = combine(
        repository.userName,
        repository.profilePhotoPath,
        repository.currencySymbol,
        repository.defaultHourlyRate,
        repository.monthlyBudget,
        repository.activeTimerState
    ) { values ->
        CorePrefs(
            userName = values[0] as String,
            profilePhotoPath = values[1] as String?,
            currency = values[2] as String,
            rate = values[3] as Double,
            budget = values[4] as Double,
            activeTimer = values[5] as ActiveTimerState
        )
    }

    val uiState: StateFlow<HomeUiState> = combine(
        corePrefsFlow,
        repository.allShifts,
        repository.allExtraIncomes,
        repository.allExpenses,
        repository.allSplitBills,
        repository.allSavingsGoals,
        _selectedDate
    ) { values ->
        val prefs = values[0] as CorePrefs
        @Suppress("UNCHECKED_CAST")
        val shifts = values[1] as List<ShiftEntity>
        @Suppress("UNCHECKED_CAST")
        val extraIncomes = values[2] as List<ExtraIncomeEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[3] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splitBills = values[4] as List<SplitBillWithParticipants>
        @Suppress("UNCHECKED_CAST")
        val rawGoals = values[5] as List<SavingsGoalWithDeposits>
        val selectedDate = values[6] as LocalDate

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val currentMonth = YearMonth.from(today)
        val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
        val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

        val totalIncome = shifts.sumOf { it.totalEarned } + extraIncomes.sumOf { it.amount }
        val totalExpenses = expenses.sumOf { it.amount }
        val totalBalance = totalIncome - totalExpenses

        val mtdIncome = shifts
            .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
            .sumOf { it.totalEarned } +
            extraIncomes
                .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
                .sumOf { it.amount }
        val mtdExpensesList = expenses.filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
        val mtdExpenses = mtdExpensesList.sumOf { it.amount }

        val splitSummary = SplitBillMath.summarizeFriendsOwed(splitBills)
        val pendingSplitsOwed = splitSummary.pendingFriendsAmount
        val pendingSplitBillCount = splitSummary.pendingBillCount

        val cutoffSummary = CutoffCalculator.summarizeCutoffShifts(
            shifts = shifts,
            referenceDate = today
        )

        val savingsGoalItems = rawGoals.map { goalWithDeps ->
            SavingsGoalCardItem(
                goalWithDeposits = goalWithDeps,
                progress = SavingsGoalMath.computeProgress(goalWithDeps, today),
                targetDate = LocalDate.ofEpochDay(goalWithDeps.goal.targetDateEpochDay)
            )
        }

        val mtdDenom = mtdExpenses.takeIf { it > 0.0 } ?: 1.0
        val monthlyTopCategories = mtdExpensesList
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                CategorySpendSummary(
                    category = cat,
                    totalAmount = sum,
                    fraction = (sum / mtdDenom).toFloat().coerceIn(0f, 1f)
                )
            }
            .sortedByDescending { it.totalAmount }

        val shiftsByDay = shifts.groupBy { it.dateEpochDay }
        val extraByDay = extraIncomes.groupBy { it.dateEpochDay }
        val expensesByDay = expenses.groupBy { it.dateEpochDay }

        val shiftEpochDays = shiftsByDay.keys + extraByDay.keys
        val expenseEpochDays = expensesByDay.keys

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val selectedEpoch = selectedDate.toEpochDay()
        val dayShifts = shiftsByDay[selectedEpoch].orEmpty()
        val dayExtra = extraByDay[selectedEpoch].orEmpty()
        val dayExpenses = expensesByDay[selectedEpoch].orEmpty()

        val hasDayLogs = dayShifts.isNotEmpty() || dayExtra.isNotEmpty() || dayExpenses.isNotEmpty()
        val showAllRecent = !hasDayLogs
        val timelineShifts = if (showAllRecent) shifts.take(5) else dayShifts
        val timelineExtra = if (showAllRecent) extraIncomes.take(3) else dayExtra
        val timelineExpenses = if (showAllRecent) expenses.take(5) else dayExpenses

        val shiftItems = timelineShifts.map { shift ->
            val startStr = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime().format(timeFormatter)
            val endStr = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime().format(timeFormatter)
            val hrs = shift.durationMinutes / 60
            val mins = shift.durationMinutes % 60
            val dur = if (hrs > 0 && mins > 0) "${hrs}h ${mins}m" else if (hrs > 0) "${hrs}h" else "${mins}m"
            TimelineActivityItem.ShiftActivity(
                id = shift.id,
                title = shift.overtimeNote.ifBlank { "Duty Shift" },
                timeRange = "$startStr – $endStr",
                durationLabel = dur,
                amount = shift.totalEarned,
                epochDay = shift.dateEpochDay,
                timestamp = shift.timeInMillis
            )
        }

        val extraItems = timelineExtra.map { inc ->
            val dateLabel = LocalDate.ofEpochDay(inc.dateEpochDay).format(monthDayFormatter)
            TimelineActivityItem.ExtraIncomeActivity(
                id = inc.id,
                title = inc.title,
                timeLabel = dateLabel,
                amount = inc.amount,
                epochDay = inc.dateEpochDay,
                timestamp = inc.createdAtMillis.takeIf { it > 0L } ?: (inc.dateEpochDay * 86_400_000L + inc.id)
            )
        }

        val expenseItems = timelineExpenses.map { exp ->
            val dateLabel = LocalDate.ofEpochDay(exp.dateEpochDay).format(monthDayFormatter)
            TimelineActivityItem.ExpenseActivity(
                id = exp.id,
                title = exp.title,
                category = exp.category,
                timeLabel = dateLabel,
                amount = exp.amount,
                epochDay = exp.dateEpochDay,
                timestamp = exp.createdAtMillis.takeIf { it > 0L } ?: (exp.dateEpochDay * 86_400_000L + exp.id)
            )
        }

        val combinedTimeline = (shiftItems + extraItems + expenseItems)
            .sortedWith(compareByDescending<TimelineActivityItem> { it.sortEpochDay }.thenByDescending { it.sortTimestamp })

        HomeUiState(
            userName = prefs.userName,
            profilePhotoPath = prefs.profilePhotoPath,
            currencySymbol = prefs.currency,
            hourlyRate = prefs.rate,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            mtdIncome = mtdIncome,
            mtdExpenses = mtdExpenses,
            monthlyBudget = prefs.budget,
            pendingSplitsOwed = pendingSplitsOwed,
            pendingSplitBillCount = pendingSplitBillCount,
            cutoffSummary = cutoffSummary,
            savingsGoals = savingsGoalItems,
            currentMonthName = today.month.getDisplayName(TextStyle.FULL, Locale.ENGLISH),
            monthlyTopCategories = monthlyTopCategories,
            selectedDate = selectedDate,
            isShowingSelectedDateOnly = hasDayLogs,
            selectedDateHasLogs = hasDayLogs,
            shiftEpochDays = shiftEpochDays,
            expenseEpochDays = expenseEpochDays,
            timelineItems = combinedTimeline,
            activeTimer = prefs.activeTimer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun resetDateToToday() {
        _selectedDate.value = LocalDate.now()
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

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
