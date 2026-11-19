package com.example.shiftly.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyCollapsedTopBar
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate

@Composable
fun AnalyticsScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onNavigateToSplitBill: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showGoalDetailSheet by remember { mutableStateOf(false) }
    var showAddGoalSheet by remember { mutableStateOf(false) }

    AnalyticsScreenContent(
        state = state,
        onBack = onBack,
        onSelectRange = { viewModel.selectRange(it) },
        onOpenSplitBill = onNavigateToSplitBill,
        onOpenGoalSettings = {
            if (state.savingsGoals.isEmpty()) {
                showAddGoalSheet = true
            } else {
                showGoalDetailSheet = true
            }
        }
    )

    if (showGoalDetailSheet) {
        val activeGoalId = remember(state.savingsGoals) {
            (state.savingsGoals.firstOrNull { it.goal.isActive }
                ?: state.savingsGoals.firstOrNull())?.goal?.id
        }
        SavingsGoalDetailSheet(
            goals = state.savingsGoals,
            initialGoalId = activeGoalId,
            currencySymbol = state.currencySymbol,
            onDismiss = { showGoalDetailSheet = false },
            onSelectActiveGoal = { goalId -> viewModel.setActiveSavingsGoal(goalId) },
            onOpenAddNewGoal = {
                showGoalDetailSheet = false
                showAddGoalSheet = true
            },
            onAddDeposit = { goal, amount, note ->
                viewModel.addSavingsDeposit(goal, amount, note)
                onShowMessage("Saved ${ShiftlyFormatters.formatCurrency(state.currencySymbol, amount)} & logged expense")
            },
            onDeleteDeposit = { dep ->
                viewModel.deleteSavingsDeposit(dep)
                onShowMessage("Savings deposit removed")
            },
            onDeleteGoal = { goal ->
                viewModel.deleteSavingsGoal(goal)
                onShowMessage("Savings goal deleted")
            }
        )
    }

    if (showAddGoalSheet) {
        AddSavingsGoalSheet(
            currencySymbol = state.currencySymbol,
            onDismiss = { showAddGoalSheet = false },
            onCreateGoal = { title, targetAmount, targetDateEpochDay ->
                viewModel.createSavingsGoal(title, targetAmount, targetDateEpochDay)
                showAddGoalSheet = false
                onShowMessage("Savings goal \"$title\" added")
            }
        )
    }
}

@Composable
fun AnalyticsScreenContent(
    state: AnalyticsUiState,
    onBack: () -> Unit,
    onSelectRange: (AnalyticsRange) -> Unit,
    onOpenSplitBill: () -> Unit = {},
    onOpenGoalSettings: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val isTitleCollapsed by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 70
        }
    }
    val headerAlpha by remember(listState) {
        derivedStateOf {
            if (listState.firstVisibleItemIndex > 0) {
                0f
            } else {
                (1f - listState.firstVisibleItemScrollOffset / 140f).coerceIn(0f, 1f)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item(key = "analytics_top_bar", contentType = "top_bar") {
                Box(modifier = Modifier.graphicsLayer { alpha = headerAlpha }) {
                    AnalyticsTopBar(
                        rangeLabel = state.selectedRange.fullLabel
                    )
                }
            }

            item(key = "analytics_range_control", contentType = "range_control") {
                AnalyticsRangeSegmentedControl(
                    selectedRange = state.selectedRange,
                    onSelectRange = onSelectRange,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }

            item(key = "analytics_chart_card", contentType = "chart_card") {
                Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                    IncomeVsExpenseGlassChartCard(state = state)
                }
            }

            item(key = "analytics_insights_sheet", contentType = "insights_sheet") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 520.dp),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    color = colors.surface,
                    shadowElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, bottom = 104.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        ShiftlySheetDragHandle()

                        Text(
                            text = "Insights",
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                            modifier = Modifier.semantics { heading() }
                        )

                        OnBudgetStreakCard(
                            streakDays = state.underBudgetStreakDays,
                            streakTargetDays = state.streakTargetDays,
                            dailyAllowance = state.dailyBudgetAllowance,
                            currencySymbol = state.currencySymbol
                        )

                        NetSavingsRateCard(
                            ratePercentage = state.netSavingsRate,
                            netAmount = state.netSavingsAmount,
                            progressFraction = state.netSavingsFraction,
                            currencySymbol = state.currencySymbol
                        )

                        PeakShiftSpendPatternCard(
                            topEarningDayLabel = state.topEarningDayLabel,
                            highestSpendDayLabel = state.highestSpendDayLabel
                        )

                        ProjectedMonthlySpendCard(
                            projectedSpend = state.projectedMonthlySpend,
                            dailyPace = state.dailyAveragePace,
                            isWithinBudget = state.isProjectedWithinBudget,
                            currencySymbol = state.currencySymbol
                        )

                        TopSpendingCategoriesCard(
                            categories = state.categoryInsights,
                            currencySymbol = state.currencySymbol
                        )

                        EarningsHoursBreakdownCard(
                            currencySymbol = state.currencySymbol,
                            earnings = state.periodIncome,
                            totalHours = state.totalWorkedHours,
                            regularHours = state.regularHours,
                            nightDiffHours = state.nightDiffHours,
                            overtimeHours = state.overtimeHours
                        )

                        HoursOfWorkCostCard(
                            currencySymbol = state.currencySymbol,
                            hourlyRate = state.hourlyRate,
                            items = state.workHoursCosts
                        )
                    }
                }
            }
        }

        ShiftlyCollapsedTopBar(
            title = "Analytics",
            isCollapsed = isTitleCollapsed,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

// =============================================================================
// High-Fidelity Previews (Weekly View & Monthly View)
// =============================================================================

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Analytics Weekly View")
@Composable
private fun AnalyticsWeeklyPreview() {
    val weeklyState = AnalyticsUiState(
        selectedRange = AnalyticsRange.WEEK,
        currencySymbol = "₱",
        hourlyRate = 96.38,
        monthlyBudget = 3000.0,
        periodIncome = 2480.0,
        periodExpenses = 1098.0,
        chartBuckets = listOf(
            IncomeExpenseBarBucket("M", 385.52, 145.0),
            IncomeExpenseBarBucket("T", 0.0, 98.0),
            IncomeExpenseBarBucket("W", 481.90, 180.0),
            IncomeExpenseBarBucket("T", 424.07, 130.0),
            IncomeExpenseBarBucket("F", 578.28, 300.0),
            IncomeExpenseBarBucket("S", 610.23, 185.0),
            IncomeExpenseBarBucket("S", 0.0, 60.0)
        ),
        categoryInsights = listOf(
            CategorySpendingInsight("Food", 626.47, 57, 0.57f),
            CategorySpendingInsight("Split Bill", 300.0, 27, 0.27f),
            CategorySpendingInsight("Supplies", 113.53, 10, 0.10f),
            CategorySpendingInsight("Transport", 58.0, 6, 0.06f)
        ),
        regularHours = 18.0,
        nightDiffHours = 4.0,
        overtimeHours = 2.0,
        totalWorkedHours = 24.0,
        workHoursCosts = listOf(
            HoursCostInsight("Food", 626.47, 6.5),
            HoursCostInsight("Split Bill", 300.0, 3.1),
            HoursCostInsight("Supplies", 113.53, 1.2)
        ),
        savingsGoalTitle = "Laptop Fund",
        savedProgressAmount = 3420.0,
        savingsGoalTarget = 5000.0,
        savingsGoalTargetEpochDay = LocalDate.of(2026, 12, 15).toEpochDay(),
        savingsGoalNeededPerDay = 52.67,
        underBudgetStreakDays = 11,
        streakTargetDays = 14,
        dailyBudgetAllowance = 120.0,
        plainLanguageTips = listOf(
            "You spent 20% more on food than last week — packing 2 campus lunches saves ~₱240.",
            "Trimming 15% of variable spend frees up ₱165 toward your tuition goal."
        ),
        unsettledSplitsAmount = 720.0,
        totalSplitsAmount = 1080.0,
        unsettledFriendsCount = 2
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                AnalyticsScreenContent(
                    state = weeklyState,
                    onBack = {},
                    onSelectRange = {},
                    onOpenSplitBill = {}
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Analytics },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Analytics Monthly View")
@Composable
private fun AnalyticsMonthlyPreview() {
    val monthlyState = AnalyticsUiState(
        selectedRange = AnalyticsRange.MONTH,
        currencySymbol = "₱",
        hourlyRate = 96.38,
        monthlyBudget = 3000.0,
        periodIncome = 8420.0,
        periodExpenses = 3190.0,
        chartBuckets = listOf(
            IncomeExpenseBarBucket("W1", 1920.0, 740.0),
            IncomeExpenseBarBucket("W2", 2140.0, 820.0),
            IncomeExpenseBarBucket("W3", 2480.0, 1098.0),
            IncomeExpenseBarBucket("W4", 1880.0, 532.0)
        ),
        categoryInsights = listOf(
            CategorySpendingInsight("Food", 1340.0, 42, 0.42f),
            CategorySpendingInsight("Supplies", 860.0, 27, 0.27f),
            CategorySpendingInsight("Transport", 540.0, 17, 0.17f),
            CategorySpendingInsight("Bills", 450.0, 14, 0.14f)
        ),
        regularHours = 64.0,
        nightDiffHours = 12.0,
        overtimeHours = 6.5,
        totalWorkedHours = 82.5,
        workHoursCosts = listOf(
            HoursCostInsight("Food", 1340.0, 13.9),
            HoursCostInsight("Supplies", 860.0, 8.9),
            HoursCostInsight("Transport", 540.0, 5.6)
        ),
        savingsGoalTitle = "Laptop Fund",
        savedProgressAmount = 4230.0,
        savingsGoalTarget = 5000.0,
        savingsGoalTargetEpochDay = LocalDate.of(2026, 12, 15).toEpochDay(),
        savingsGoalNeededPerDay = 25.67,
        underBudgetStreakDays = 12,
        streakTargetDays = 14,
        dailyBudgetAllowance = 120.0,
        plainLanguageTips = listOf(
            "Nice! You cut transport spending by 14% vs. last month.",
            "Night differential shifts added +12 bonus hours this month."
        ),
        unsettledSplitsAmount = 600.0,
        totalSplitsAmount = 1200.0,
        unsettledFriendsCount = 2
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                AnalyticsScreenContent(
                    state = monthlyState,
                    onBack = {},
                    onSelectRange = {},
                    onOpenSplitBill = {}
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Analytics },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
