package com.example.shiftly.ui.screens

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.SavingsGoalMath
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.SplitBillMath
import com.example.shiftly.utils.WageCalculator
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class AnalyticsRange(val shortLabel: String, val fullLabel: String) {
    DAY("D", "Today"),
    WEEK("W", "This Week"),
    MONTH("M", "This Month"),
    YEAR("Y", "This Year")
}

@Immutable
data class IncomeExpenseBarBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

@Immutable
data class CategorySpendingInsight(
    val category: String,
    val amount: Double,
    val percentage: Int,
    val fraction: Float
)

@Immutable
data class HoursCostInsight(
    val category: String,
    val amount: Double,
    val dutyHours: Double
)

@Immutable
data class AnalyticsUiState(
    val selectedRange: AnalyticsRange = AnalyticsRange.WEEK,
    val currencySymbol: String = "₱",
    val hourlyRate: Double = 0.0,
    val monthlyBudget: Double = 0.0,
    val periodIncome: Double = 0.0,
    val periodExpenses: Double = 0.0,
    val chartBuckets: List<IncomeExpenseBarBucket> = emptyList(),
    val categoryInsights: List<CategorySpendingInsight> = emptyList(),
    val regularHours: Double = 0.0,
    val nightDiffHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val totalWorkedHours: Double = 0.0,
    val workHoursCosts: List<HoursCostInsight> = emptyList(),
    val savingsGoals: List<SavingsGoalWithDeposits> = emptyList(),
    val savingsGoalTitle: String = "",
    val savedProgressAmount: Double = 0.0,
    val savingsGoalTarget: Double = 0.0,
    val savingsGoalTargetEpochDay: Long? = null,
    val savingsGoalNeededPerDay: Double = 0.0,
    val savingsGoalDaysRemaining: Long = 0L,
    val underBudgetStreakDays: Int = 0,
    val streakTargetDays: Int = 14,
    val dailyBudgetAllowance: Double = 0.0,
    val plainLanguageTips: List<String> = emptyList(),
    val unsettledSplitsAmount: Double = 0.0,
    val totalSplitsAmount: Double = 0.0,
    val unsettledFriendsCount: Int = 0
)

class AnalyticsViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(AnalyticsRange.WEEK)

    private data class AnalyticsPrefsBundle(
        val range: AnalyticsRange,
        val currency: String,
        val rate: Double,
        val monthlyBudget: Double
    )

    private val prefsFlow = combine(
        _selectedRange,
        repository.currencySymbol,
        repository.defaultHourlyRate,
        repository.monthlyBudget
    ) { range, currency, rate, budget ->
        AnalyticsPrefsBundle(
            range = range,
            currency = currency,
            rate = rate,
            monthlyBudget = budget
        )
    }

    val uiState: StateFlow<AnalyticsUiState> = combine(
        prefsFlow,
        repository.allShifts,
        repository.allExtraIncomes,
        repository.allExpenses,
        repository.allSplitBills,
        repository.allSavingsGoals,
        repository.allAllowanceEntries
    ) { values ->
        val prefs = values[0] as AnalyticsPrefsBundle
        val range = prefs.range
        val currency = prefs.currency
        val rate = prefs.rate
        val monthlyBudget = prefs.monthlyBudget
        @Suppress("UNCHECKED_CAST")
        val shifts = values[1] as List<ShiftEntity>
        @Suppress("UNCHECKED_CAST")
        val extraIncomes = values[2] as List<ExtraIncomeEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[3] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splitBills = values[4] as List<SplitBillWithParticipants>
        @Suppress("UNCHECKED_CAST")
        val savingsGoals = values[5] as List<SavingsGoalWithDeposits>
        @Suppress("UNCHECKED_CAST")
        val allowanceEntries = values[6] as List<AllowanceEntryEntity>

        // Exclude auto-credited Duties cutoff entries (cutoffKey != null) to avoid double counting
        val manualAllowances = allowanceEntries.filter { it.cutoffKey.isNullOrBlank() }

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val (periodStart, periodEnd) = resolvePeriodBounds(range, today)

        val periodShifts = shifts.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }
        val periodExtraIncomes = extraIncomes.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }
        val periodManualAllowances = manualAllowances.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }
        val periodExpenses = expenses.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }

        val totalIncome = periodShifts.sumOf { it.totalEarned } +
            periodExtraIncomes.sumOf { it.amount } +
            periodManualAllowances.sumOf { it.amount }
        val totalExpense = periodExpenses.sumOf { it.amount }

        var regMins = 0L
        var ndMins = 0L
        var otMins = 0L
        periodShifts.forEach { s ->
            val d = WageCalculator.calculateTotalEarned(
                startMillis = s.timeInMillis,
                endMillis = s.timeOutMillis,
                hourlyRate = s.lockedHourlyRate,
                dayTypeStr = s.dayType
            )
            regMins += d.regularMinutes
            ndMins += d.nightDiffMinutes
            otMins += d.overtimeMinutes
        }
        val totalHours = periodShifts.sumOf { it.durationMinutes } / 60.0

        val buckets = buildChartBuckets(
            range = range,
            today = today,
            zone = zone,
            shifts = shifts,
            extraIncomes = extraIncomes,
            manualAllowances = manualAllowances,
            expenses = expenses
        )

        val spendDenom = totalExpense.takeIf { it > 0.0 } ?: 1.0
        val categoryInsights = periodExpenses
            .groupBy { it.category }
            .map { (cat, list) ->
                val sum = list.sumOf { it.amount }
                val frac = (sum / spendDenom).toFloat().coerceIn(0f, 1f)
                CategorySpendingInsight(
                    category = cat,
                    amount = sum,
                    percentage = (frac * 100f).roundToInt(),
                    fraction = frac
                )
            }
            .sortedByDescending { it.amount }

        val hoursCosts = if (rate > 0.0) {
            categoryInsights.map { item ->
                HoursCostInsight(
                    category = item.category,
                    amount = item.amount,
                    dutyHours = item.amount / rate
                )
            }
        } else {
            emptyList()
        }

        // Active savings goal (using actual deposits)
        val activeGoalWithDeposits = savingsGoals.firstOrNull { it.goal.isActive } ?: savingsGoals.firstOrNull()
        val activeProgress = activeGoalWithDeposits?.let { SavingsGoalMath.computeProgress(it, today) }

        // Under-budget days streak (based on Monthly Budget connected from Welcome/Settings)
        val currentMonthYm = YearMonth.from(today)
        val monthStartEp = currentMonthYm.atDay(1).toEpochDay()
        val monthEndEp = currentMonthYm.atEndOfMonth().toEpochDay()
        val monthSpend = expenses.filter { it.dateEpochDay in monthStartEp..monthEndEp }.sumOf { it.amount }

        val dailyCap = if (monthlyBudget > 0.0) {
            monthlyBudget / currentMonthYm.lengthOfMonth().toDouble()
        } else {
            0.0
        }
        val expenseByDay = expenses.groupBy { it.dateEpochDay }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        var streak = 0
        if (dailyCap > 0.0) {
            for (offset in 0..29) {
                val daySpend = expenseByDay[today.minusDays(offset.toLong()).toEpochDay()] ?: 0.0
                if (daySpend <= dailyCap) streak++ else break
            }
        }

        val tips = buildPlainLanguageTips(
            range = range,
            today = today,
            currency = currency,
            rate = rate,
            monthlyBudget = monthlyBudget,
            monthSpend = monthSpend,
            allExpenses = expenses,
            periodExpenses = periodExpenses,
            categoryInsights = categoryInsights
        )

        val splitSummary = SplitBillMath.summarizeFriendsOwed(splitBills)
        val unsettledFriendsCount = splitBills.sumOf { bill ->
            bill.participants.count { !it.isCurrentUser && !it.hasPaid }
        }

        AnalyticsUiState(
            selectedRange = range,
            currencySymbol = currency,
            hourlyRate = rate,
            monthlyBudget = monthlyBudget,
            periodIncome = totalIncome,
            periodExpenses = totalExpense,
            chartBuckets = buckets,
            categoryInsights = categoryInsights,
            regularHours = regMins / 60.0,
            nightDiffHours = ndMins / 60.0,
            overtimeHours = otMins / 60.0,
            totalWorkedHours = totalHours,
            workHoursCosts = hoursCosts,
            savingsGoals = savingsGoals,
            savingsGoalTitle = activeGoalWithDeposits?.goal?.title.orEmpty(),
            savedProgressAmount = activeProgress?.savedAmount ?: 0.0,
            savingsGoalTarget = activeGoalWithDeposits?.goal?.targetAmount ?: 0.0,
            savingsGoalTargetEpochDay = activeGoalWithDeposits?.goal?.targetDateEpochDay,
            savingsGoalNeededPerDay = activeProgress?.neededPerDay ?: 0.0,
            savingsGoalDaysRemaining = activeProgress?.daysRemaining ?: 0L,
            underBudgetStreakDays = streak,
            streakTargetDays = 14,
            dailyBudgetAllowance = dailyCap,
            plainLanguageTips = tips,
            unsettledSplitsAmount = splitSummary.pendingFriendsAmount,
            totalSplitsAmount = splitSummary.pendingFriendsAmount + splitSummary.collectedFriendsAmount,
            unsettledFriendsCount = unsettledFriendsCount
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState()
        )

    fun selectRange(range: AnalyticsRange) {
        _selectedRange.value = range
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

    private fun resolvePeriodBounds(range: AnalyticsRange, today: LocalDate): Pair<LocalDate, LocalDate> {
        return when (range) {
            AnalyticsRange.DAY -> today to today
            AnalyticsRange.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                start to start.plusDays(6)
            }
            AnalyticsRange.MONTH -> {
                val ym = YearMonth.from(today)
                ym.atDay(1) to ym.atEndOfMonth()
            }
            AnalyticsRange.YEAR -> {
                LocalDate.of(today.year, 1, 1) to LocalDate.of(today.year, 12, 31)
            }
        }
    }

    private fun buildChartBuckets(
        range: AnalyticsRange,
        today: LocalDate,
        zone: ZoneId,
        shifts: List<ShiftEntity>,
        extraIncomes: List<ExtraIncomeEntity>,
        manualAllowances: List<AllowanceEntryEntity>,
        expenses: List<ExpenseEntity>
    ): List<IncomeExpenseBarBucket> {
        val shiftsByDay = shifts.groupBy { it.dateEpochDay }
        val extraByDay = extraIncomes.groupBy { it.dateEpochDay }
        val allowanceByDay = manualAllowances.groupBy { it.dateEpochDay }
        val expensesByDay = expenses.groupBy { it.dateEpochDay }

        return when (range) {
            AnalyticsRange.DAY -> {
                val todayEpoch = today.toEpochDay()
                val dayShifts = shiftsByDay[todayEpoch].orEmpty()
                val dayExtra = extraByDay[todayEpoch].orEmpty()
                val dayManualAllowance = allowanceByDay[todayEpoch].orEmpty()
                val dayExpenses = expensesByDay[todayEpoch].orEmpty()
                val slots = listOf(
                    "12am" to (0..5),
                    "6am" to (6..11),
                    "12pm" to (12..17),
                    "6pm" to (18..22),
                    "11pm" to (23..23)
                )
                slots.mapIndexed { index, (label, hours) ->
                    val inc = dayShifts.filter {
                        Instant.ofEpochMilli(it.timeInMillis).atZone(zone).hour in hours
                    }.sumOf { it.totalEarned } + if (index == 0) {
                        dayExtra.sumOf { it.amount } + dayManualAllowance.sumOf { it.amount }
                    } else {
                        0.0
                    }
                    val exp = dayExpenses.filter {
                        Instant.ofEpochMilli(it.dateEpochDay * 86400000L).atZone(zone).hour in hours
                    }.sumOf { it.amount }
                    IncomeExpenseBarBucket(label, inc, exp)
                }
            }

            AnalyticsRange.WEEK -> {
                val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
                (0..6).map { offset ->
                    val d = start.plusDays(offset.toLong())
                    val label = d.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.ENGLISH)
                    val ep = d.toEpochDay()
                    IncomeExpenseBarBucket(
                        label = label,
                        income = shiftsByDay[ep].orEmpty().sumOf { it.totalEarned } +
                            extraByDay[ep].orEmpty().sumOf { it.amount } +
                            allowanceByDay[ep].orEmpty().sumOf { it.amount },
                        expense = expensesByDay[ep].orEmpty().sumOf { it.amount }
                    )
                }
            }

            AnalyticsRange.MONTH -> {
                val ym = YearMonth.from(today)
                val endDay = ym.lengthOfMonth()
                val slots = listOf(
                    "1" to (1..6),
                    "7" to (7..13),
                    "14" to (14..20),
                    "21" to (21 until endDay),
                    "$endDay" to (endDay..endDay)
                )
                slots.map { (label, dayRange) ->
                    val eps = dayRange.map { ym.atDay(it).toEpochDay() }
                    IncomeExpenseBarBucket(
                        label = label,
                        income = eps.sumOf { ep ->
                            shiftsByDay[ep].orEmpty().sumOf { it.totalEarned } +
                                extraByDay[ep].orEmpty().sumOf { it.amount } +
                                allowanceByDay[ep].orEmpty().sumOf { it.amount }
                        },
                        expense = eps.sumOf { ep -> expensesByDay[ep].orEmpty().sumOf { it.amount } }
                    )
                }
            }

            AnalyticsRange.YEAR -> {
                val currentYear = today.year
                (1..12).map { month ->
                    val ym = YearMonth.of(currentYear, month)
                    val startEp = ym.atDay(1).toEpochDay()
                    val endEp = ym.atEndOfMonth().toEpochDay()
                    val label = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH)
                    IncomeExpenseBarBucket(
                        label = label,
                        income = shifts.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.totalEarned } +
                            extraIncomes.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.amount } +
                            manualAllowances.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.amount },
                        expense = expenses.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.amount }
                    )
                }
            }
        }
    }

    private fun buildPlainLanguageTips(
        range: AnalyticsRange,
        today: LocalDate,
        currency: String,
        rate: Double,
        monthlyBudget: Double,
        monthSpend: Double,
        allExpenses: List<ExpenseEntity>,
        periodExpenses: List<ExpenseEntity>,
        categoryInsights: List<CategorySpendingInsight>
    ): List<String> {
        val tips = mutableListOf<String>()
        if (monthlyBudget > 0.0) {
            val pct = ((monthSpend / monthlyBudget) * 100.0).roundToInt()
            val remaining = monthlyBudget - monthSpend
            if (remaining >= 0.0) {
                tips.add(
                    "Monthly budget: ${ShiftlyFormatters.formatCurrency(currency, monthSpend)} of ${ShiftlyFormatters.formatCurrency(currency, monthlyBudget)} used ($pct% • ${ShiftlyFormatters.formatCurrency(currency, remaining)} left)."
                )
            } else {
                tips.add(
                    "Monthly budget exceeded by ${ShiftlyFormatters.formatCurrency(currency, -remaining)} (${pct}% of ${ShiftlyFormatters.formatCurrency(currency, monthlyBudget)})."
                )
            }
        }

        val topCategory = categoryInsights.firstOrNull()
        if (topCategory != null) {
            val prevRangeDays = when (range) {
                AnalyticsRange.DAY -> 1L
                AnalyticsRange.WEEK -> 7L
                AnalyticsRange.MONTH -> 30L
                AnalyticsRange.YEAR -> 365L
            }
            val (curStart, _) = resolvePeriodBounds(range, today)
            val prevStartEp = curStart.minusDays(prevRangeDays).toEpochDay()
            val prevEndEp = curStart.minusDays(1).toEpochDay()
            val prevTopSpend = allExpenses
                .filter { it.dateEpochDay in prevStartEp..prevEndEp && it.category.equals(topCategory.category, true) }
                .sumOf { it.amount }

            if (prevTopSpend > 0.0) {
                val deltaPct = (((topCategory.amount - prevTopSpend) / prevTopSpend) * 100.0).roundToInt()
                if (deltaPct > 0) {
                    tips.add("You spent $deltaPct% more on ${topCategory.category.lowercase()} than the previous period.")
                } else if (deltaPct < 0) {
                    tips.add("Nice! You cut ${topCategory.category.lowercase()} spending by ${-deltaPct}% vs. last period.")
                }
            } else if (rate > 0.0) {
                val hoursEq = topCategory.amount / rate
                tips.add(
                    "${topCategory.category} is ${topCategory.percentage}% of your spending (${ShiftlyFormatters.formatHours(hoursEq)} of shift duty)."
                )
            }
        }

        if (periodExpenses.isEmpty()) {
            tips.add("No expenses recorded in ${range.fullLabel.lowercase()} — log expenses to unlock trend tips.")
        } else {
            val totalPeriodSpend = periodExpenses.sumOf { it.amount }
            val savedIfTrim15 = totalPeriodSpend * 0.15
            tips.add(
                "Trimming 15% of spending frees up ${ShiftlyFormatters.formatCurrency(currency, savedIfTrim15)} toward your goal."
            )
        }
        return tips
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AnalyticsViewModel(repository) as T
        }
    }
}
