package com.example.shiftly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.SplitBillMath
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class WeekdaySlot(
    val date: LocalDate,
    val dayLetter: String,
    val dayNumber: String,
    val hasShift: Boolean,
    val hasExpense: Boolean
)

data class WeeklyBarData(
    val dayLabel: String,
    val earned: Double
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
    val userName: String = "Maya",
    val currencySymbol: String = "₱",
    val hourlyRate: Double = 96.38,
    val totalBalance: Double = 0.0,
    val totalIncome: Double = 0.0,
    val totalExpenses: Double = 0.0,
    val mtdIncome: Double = 0.0,
    val mtdExpenses: Double = 0.0,
    val monthlyBudget: Double = 3000.0,
    val savingsBuffer: Double = 1000.0,
    val pendingSplitsOwed: Double = 0.0,
    val pendingSplitBillCount: Int = 0,
    val safeToSpend: Double = 0.0,
    val weeklyEarnings: Double = 0.0,
    val weeklyHours: Double = 0.0,
    val weeklyBars: List<WeeklyBarData> = emptyList(),
    val selectedDate: LocalDate = LocalDate.now(),
    val weekdaySlots: List<WeekdaySlot> = emptyList(),
    val timelineItems: List<TimelineActivityItem> = emptyList(),
    val activeTimer: ActiveTimerState = ActiveTimerState()
)

class HomeViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val _selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<HomeUiState> = combine(
        repository.userName,
        repository.currencySymbol,
        repository.defaultHourlyRate,
        repository.monthlyBudget,
        repository.savingsBuffer,
        repository.activeTimerState,
        repository.allShifts,
        repository.allExtraIncomes,
        repository.allExpenses,
        repository.allSplitBills,
        _selectedDate
    ) { values ->
        val userName = values[0] as String
        val currency = values[1] as String
        val rate = values[2] as Double
        val budget = values[3] as Double
        val buffer = values[4] as Double
        val activeTimer = values[5] as ActiveTimerState
        @Suppress("UNCHECKED_CAST")
        val shifts = values[6] as List<ShiftEntity>
        @Suppress("UNCHECKED_CAST")
        val extraIncomes = values[7] as List<ExtraIncomeEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[8] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splitBills = values[9] as List<SplitBillWithParticipants>
        val selectedDate = values[10] as LocalDate

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
        val mtdExpenses = expenses
            .filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
            .sumOf { it.amount }

        val splitSummary = SplitBillMath.summarizeFriendsOwed(splitBills)
        val pendingSplitsOwed = splitSummary.pendingFriendsAmount
        val pendingSplitBillCount = splitSummary.pendingBillCount

        val safeToSpend = (totalBalance - pendingSplitsOwed - buffer).coerceAtLeast(0.0)

        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val shiftsByDay = shifts.groupBy { it.dateEpochDay }
        val extraByDay = extraIncomes.groupBy { it.dateEpochDay }
        val expensesByDay = expenses.groupBy { it.dateEpochDay }

        var weeklyEarnings = 0.0
        var weeklyMinutes = 0
        val dayLetters = listOf("M", "T", "W", "T", "F", "S", "S")

        val weekdaySlots = (0..6).map { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            val epoch = date.toEpochDay()
            val dayShifts = shiftsByDay[epoch].orEmpty()
            val dayExtra = extraByDay[epoch].orEmpty()
            val dayExpenses = expensesByDay[epoch].orEmpty()
            weeklyEarnings += dayShifts.sumOf { it.totalEarned } + dayExtra.sumOf { it.amount }
            weeklyMinutes += dayShifts.sumOf { it.durationMinutes }
            WeekdaySlot(
                date = date,
                dayLetter = dayLetters[offset],
                dayNumber = date.dayOfMonth.toString(),
                hasShift = dayShifts.isNotEmpty() || dayExtra.isNotEmpty(),
                hasExpense = dayExpenses.isNotEmpty()
            )
        }

        val weeklyBars = (0..6).map { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            val ep = date.toEpochDay()
            val earned = shiftsByDay[ep].orEmpty().sumOf { it.totalEarned } +
                extraByDay[ep].orEmpty().sumOf { it.amount }
            WeeklyBarData(dayLabel = dayLetters[offset], earned = earned)
        }

        val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", Locale.getDefault())
        val monthDayFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
        val selectedEpoch = selectedDate.toEpochDay()
        val dayShifts = shiftsByDay[selectedEpoch].orEmpty()
        val dayExtra = extraByDay[selectedEpoch].orEmpty()
        val dayExpenses = expensesByDay[selectedEpoch].orEmpty()

        val showAllRecent = dayShifts.isEmpty() && dayExtra.isEmpty() && dayExpenses.isEmpty()
        val timelineShifts = if (showAllRecent) shifts.take(5) else dayShifts
        val timelineExtra = if (showAllRecent) extraIncomes.take(3) else dayExtra
        val timelineExpenses = if (showAllRecent) expenses.take(5) else dayExpenses

        val shiftItems = timelineShifts.map { shift ->
            val startStr = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime().format(timeFormatter)
            val endStr = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime().format(timeFormatter)
            val hrs = shift.durationMinutes / 60
            val mins = shift.durationMinutes % 60
            val dur = if (hrs > 0) "${hrs}h ${mins}m" else "${mins}m"
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
            userName = userName,
            currencySymbol = currency,
            hourlyRate = rate,
            totalBalance = totalBalance,
            totalIncome = totalIncome,
            totalExpenses = totalExpenses,
            mtdIncome = mtdIncome,
            mtdExpenses = mtdExpenses,
            monthlyBudget = budget,
            savingsBuffer = buffer,
            pendingSplitsOwed = pendingSplitsOwed,
            pendingSplitBillCount = pendingSplitBillCount,
            safeToSpend = safeToSpend,
            weeklyEarnings = weeklyEarnings,
            weeklyHours = weeklyMinutes / 60.0,
            weeklyBars = weeklyBars,
            selectedDate = selectedDate,
            weekdaySlots = weekdaySlots,
            timelineItems = combinedTimeline,
            activeTimer = activeTimer
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = HomeUiState()
    )

    fun selectDate(date: LocalDate) {
        _selectedDate.value = date
    }

    fun startQuickShift() {
        val current = uiState.value
        if (current.activeTimer.isRunning) return
        viewModelScope.launch {
            repository.startShiftTimer(
                startMillis = System.currentTimeMillis(),
                dateEpochDay = LocalDate.now().toEpochDay(),
                lockedRate = current.hourlyRate
            )
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(repository) as T
        }
    }
}
