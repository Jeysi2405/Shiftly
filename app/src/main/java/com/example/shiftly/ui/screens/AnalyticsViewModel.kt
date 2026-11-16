package com.example.shiftly.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

enum class AnalyticsRange(val shortLabel: String, val fullLabel: String) {
    DAY("D", "Today"),
    WEEK("W", "This Week"),
    MONTH("M", "This Month"),
    SIX_MONTHS("6M", "Last 6 Months"),
    YEAR("Y", "This Year")
}

data class IncomeExpenseBarBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategorySpendingInsight(
    val category: String,
    val amount: Double,
    val percentage: Int,
    val fraction: Float
)

data class HoursCostInsight(
    val category: String,
    val amount: Double,
    val dutyHours: Double
)

data class AnalyticsUiState(
    val selectedRange: AnalyticsRange = AnalyticsRange.WEEK,
    val currencySymbol: String = "₱",
    val hourlyRate: Double = 96.38,
    val periodIncome: Double = 0.0,
    val periodExpenses: Double = 0.0,
    val chartBuckets: List<IncomeExpenseBarBucket> = emptyList(),
    val categoryInsights: List<CategorySpendingInsight> = emptyList(),
    val regularHours: Double = 0.0,
    val nightDiffHours: Double = 0.0,
    val overtimeHours: Double = 0.0,
    val totalWorkedHours: Double = 0.0,
    val workHoursCosts: List<HoursCostInsight> = emptyList(),
    val savingsGoalTitle: String = "Tuition & Laptop Fund",
    val savedProgressAmount: Double = 0.0,
    val savingsGoalTarget: Double = 5000.0,
    val underBudgetStreakDays: Int = 0,
    val streakTargetDays: Int = 14,
    val dailyBudgetAllowance: Double = 100.0,
    val plainLanguageTips: List<String> = emptyList(),
    val unsettledSplitsAmount: Double = 0.0,
    val totalSplitsAmount: Double = 0.0,
    val unsettledFriendsCount: Int = 0
)

class AnalyticsViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(AnalyticsRange.WEEK)

    val uiState: StateFlow<AnalyticsUiState> = combine(
        _selectedRange,
        repository.currencySymbol,
        repository.defaultHourlyRate,
        repository.monthlyBudget,
        repository.savingsBuffer,
        repository.settingsSnapshot,
        repository.allShifts,
        repository.allExtraIncomes,
        repository.allExpenses,
        repository.allSplitBills
    ) { values ->
        val range = values[0] as AnalyticsRange
        val currency = values[1] as String
        val rate = (values[2] as Double).takeIf { it > 0.0 } ?: 96.38
        val monthlyBudget = values[3] as Double
        val savingsBuffer = values[4] as Double
        val settings = values[5] as ShiftlySettingsSnapshot
        @Suppress("UNCHECKED_CAST")
        val shifts = values[6] as List<ShiftEntity>
        @Suppress("UNCHECKED_CAST")
        val extraIncomes = values[7] as List<ExtraIncomeEntity>
        @Suppress("UNCHECKED_CAST")
        val expenses = values[8] as List<ExpenseEntity>
        @Suppress("UNCHECKED_CAST")
        val splitBills = values[9] as List<SplitBillWithParticipants>

        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()
        val (periodStart, periodEnd) = resolvePeriodBounds(range, today)

        val periodShifts = shifts.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }
        val periodExtraIncomes = extraIncomes.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }
        val periodExpenses = expenses.filter {
            it.dateEpochDay in periodStart.toEpochDay()..periodEnd.toEpochDay()
        }

        val totalIncome = periodShifts.sumOf { it.totalEarned } + periodExtraIncomes.sumOf { it.amount }
        val totalExpense = periodExpenses.sumOf { it.amount }

        // Regular, Night Diff, Overtime hours split
        var regMins = 0L
        var ndMins = 0L
        var otMins = 0L
        periodShifts.forEach { s ->
            val d = WageCalculator.calculateTotalEarned(
                startMillis = s.timeInMillis,
                endMillis = s.timeOutMillis,
                hourlyRate = s.lockedHourlyRate,
                dayTypeStr = s.dayType,
                nightDiffStartHour = settings.nightDiffStartHour,
                nightDiffMultiplier = settings.nightDiffMultiplier,
                ordinaryOvertimeMultiplier = settings.overtimeMultiplier
            )
            regMins += d.regularMinutes
            ndMins += d.nightDiffMinutes
            otMins += d.overtimeMinutes
        }
        val totalHours = periodShifts.sumOf { it.durationMinutes } / 60.0

        // Chart Buckets
        val buckets = buildChartBuckets(range, today, zone, shifts, extraIncomes, expenses)

        // Category breakdown
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

        // Hours of work this cost
        val hoursCosts = categoryInsights.map { item ->
            HoursCostInsight(
                category = item.category,
                amount = item.amount,
                dutyHours = item.amount / rate
            )
        }

        // Savings goal progress (net lifetime + buffer capped cleanly against target)
        val lifetimeIncome = shifts.sumOf { it.totalEarned } + extraIncomes.sumOf { it.amount }
        val lifetimeNet = (lifetimeIncome - expenses.sumOf { it.amount })
            .coerceAtLeast(0.0)
        val savedSoFar = (lifetimeNet - savingsBuffer).coerceAtLeast(lifetimeNet * 0.55)
            .coerceAtLeast(0.0)

        // Under-budget days streak (last 30 days)
        val dailyCap = (monthlyBudget / 30.0).coerceAtLeast(50.0)
        val expenseByDay = expenses.groupBy { it.dateEpochDay }
            .mapValues { (_, list) -> list.sumOf { it.amount } }
        var streak = 0
        for (offset in 0..29) {
            val daySpend = expenseByDay[today.minusDays(offset.toLong()).toEpochDay()] ?: 0.0
            if (daySpend <= dailyCap) streak++ else break
        }

        // Plain-language tips comparing current vs previous window
        val tips = buildPlainLanguageTips(
            range = range,
            today = today,
            currency = currency,
            rate = rate,
            allExpenses = expenses,
            periodExpenses = periodExpenses,
            categoryInsights = categoryInsights
        )

        // Unsettled split bills tracker (strictly excludes current user)
        val splitSummary = SplitBillMath.summarizeFriendsOwed(splitBills)
        val unsettledFriendsCount = splitBills.sumOf { bill ->
            bill.participants.count { !it.isCurrentUser && !it.hasPaid }
        }

        AnalyticsUiState(
            selectedRange = range,
            currencySymbol = currency,
            hourlyRate = rate,
            periodIncome = totalIncome,
            periodExpenses = totalExpense,
            chartBuckets = buckets,
            categoryInsights = categoryInsights,
            regularHours = regMins / 60.0,
            nightDiffHours = ndMins / 60.0,
            overtimeHours = otMins / 60.0,
            totalWorkedHours = totalHours,
            workHoursCosts = hoursCosts,
            savingsGoalTitle = settings.savingsGoalTitle,
            savedProgressAmount = savedSoFar,
            savingsGoalTarget = settings.savingsGoalTarget,
            underBudgetStreakDays = streak,
            streakTargetDays = 14,
            dailyBudgetAllowance = dailyCap,
            plainLanguageTips = tips,
            unsettledSplitsAmount = splitSummary.pendingFriendsAmount,
            totalSplitsAmount = splitSummary.pendingFriendsAmount + splitSummary.collectedFriendsAmount,
            unsettledFriendsCount = unsettledFriendsCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AnalyticsUiState()
    )

    fun selectRange(range: AnalyticsRange) {
        _selectedRange.value = range
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
            AnalyticsRange.SIX_MONTHS -> {
                val start = YearMonth.from(today).minusMonths(5).atDay(1)
                start to YearMonth.from(today).atEndOfMonth()
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
        expenses: List<ExpenseEntity>
    ): List<IncomeExpenseBarBucket> {
        val shiftsByDay = shifts.groupBy { it.dateEpochDay }
        val extraByDay = extraIncomes.groupBy { it.dateEpochDay }
        val expensesByDay = expenses.groupBy { it.dateEpochDay }

        return when (range) {
            AnalyticsRange.DAY -> {
                val todayEpoch = today.toEpochDay()
                val dayShifts = shiftsByDay[todayEpoch].orEmpty()
                val dayExtra = extraByDay[todayEpoch].orEmpty()
                val dayExpenses = expensesByDay[todayEpoch].orEmpty()
                val slots = listOf("AM" to (0..11), "PM" to (12..17), "Eve" to (18..23))
                slots.mapIndexed { index, (label, hours) ->
                    val inc = dayShifts.filter {
                        Instant.ofEpochMilli(it.timeInMillis).atZone(zone).hour in hours
                    }.sumOf { it.totalEarned } + if (index == 0) dayExtra.sumOf { it.amount } else 0.0
                    val exp = if (index == 1) dayExpenses.sumOf { it.amount } else 0.0
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
                            extraByDay[ep].orEmpty().sumOf { it.amount },
                        expense = expensesByDay[ep].orEmpty().sumOf { it.amount }
                    )
                }
            }

            AnalyticsRange.MONTH -> {
                val ym = YearMonth.from(today)
                val endDay = ym.lengthOfMonth()
                val ranges = listOf(1..7, 8..14, 15..21, 22..endDay)
                ranges.mapIndexed { idx, dayRange ->
                    val eps = dayRange.map { ym.atDay(it).toEpochDay() }
                    IncomeExpenseBarBucket(
                        label = "W${idx + 1}",
                        income = eps.sumOf { ep ->
                            shiftsByDay[ep].orEmpty().sumOf { it.totalEarned } +
                                extraByDay[ep].orEmpty().sumOf { it.amount }
                        },
                        expense = eps.sumOf { ep -> expensesByDay[ep].orEmpty().sumOf { it.amount } }
                    )
                }
            }

            AnalyticsRange.SIX_MONTHS -> {
                val currentMonth = YearMonth.from(today)
                (5 downTo 0).map { offset ->
                    val ym = currentMonth.minusMonths(offset.toLong())
                    val startEp = ym.atDay(1).toEpochDay()
                    val endEp = ym.atEndOfMonth().toEpochDay()
                    IncomeExpenseBarBucket(
                        label = ym.month.getDisplayName(TextStyle.SHORT, Locale.ENGLISH),
                        income = shifts.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.totalEarned } +
                            extraIncomes.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.amount },
                        expense = expenses.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.amount }
                    )
                }
            }

            AnalyticsRange.YEAR -> {
                val currentYear = today.year
                listOf(1..3, 4..6, 7..9, 10..12).mapIndexed { idx, qMonths ->
                    val startEp = LocalDate.of(currentYear, qMonths.first, 1).toEpochDay()
                    val endYm = YearMonth.of(currentYear, qMonths.last)
                    val endEp = endYm.atEndOfMonth().toEpochDay()
                    IncomeExpenseBarBucket(
                        label = "Q${idx + 1}",
                        income = shifts.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.totalEarned } +
                            extraIncomes.filter { it.dateEpochDay in startEp..endEp }.sumOf { it.amount },
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
        allExpenses: List<ExpenseEntity>,
        periodExpenses: List<ExpenseEntity>,
        categoryInsights: List<CategorySpendingInsight>
    ): List<String> {
        val tips = mutableListOf<String>()
        val topCategory = categoryInsights.firstOrNull()
        if (topCategory != null) {
            val prevRangeDays = when (range) {
                AnalyticsRange.DAY -> 1L
                AnalyticsRange.WEEK -> 7L
                AnalyticsRange.MONTH -> 30L
                AnalyticsRange.SIX_MONTHS -> 180L
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
            } else {
                val hoursEq = topCategory.amount / rate
                tips.add(
                    "${topCategory.category} is ${topCategory.percentage}% of your spending (${ "%.1f".format(hoursEq) }h of shift duty)."
                )
            }
        }

        if (periodExpenses.isEmpty()) {
            tips.add("No expenses recorded in ${range.fullLabel.lowercase()} — log purchases to unlock trend tips.")
        } else {
            val totalPeriodSpend = periodExpenses.sumOf { it.amount }
            val savedIfTrim15 = totalPeriodSpend * 0.15
            tips.add(
                "Trimming 15% of variable spend frees up $currency${"%,.0f".format(savedIfTrim15)} toward your goal."
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
