package com.example.shiftly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.HairlineLight
import com.example.shiftly.ui.theme.InkPrimary
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme

@Composable
fun AnalyticsScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onNavigateToSplitBill: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: AnalyticsViewModel = viewModel(factory = AnalyticsViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    AnalyticsScreenContent(
        state = state,
        onBack = onBack,
        onSelectRange = { viewModel.selectRange(it) },
        onOpenSplitBill = onNavigateToSplitBill,
        onOpenGoalMessage = {
            onShowMessage("Update your savings goal anytime in Profile & Settings")
        }
    )
}

@Composable
fun AnalyticsScreenContent(
    state: AnalyticsUiState,
    onBack: () -> Unit,
    onSelectRange: (AnalyticsRange) -> Unit,
    onOpenSplitBill: () -> Unit,
    onOpenGoalMessage: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    var showAllInsights by remember { mutableStateOf(false) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Back Arrow + Header
        item {
            AnalyticsTopBar(
                rangeLabel = state.selectedRange.fullLabel,
                onBack = onBack
            )
        }

        // 1. Segmented Control (D / W / M / 6M / Y)
        item {
            AnalyticsRangeSegmentedControl(
                selectedRange = state.selectedRange,
                onSelectRange = onSelectRange,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 2. Frosted Glass Bar Chart (Income Mint vs Expenses Coral)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                IncomeVsExpenseGlassChartCard(state = state)
            }
        }

        // 3. White Bottom Sheet with Insight Cards & "Show All"
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 520.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = PureWhite,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .padding(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(HairlineLight)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Insights (${state.selectedRange.fullLabel})",
                            style = MaterialTheme.typography.titleMedium,
                            color = InkPrimary
                        )
                        Box(
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { showAllInsights = !showAllInsights }
                                .padding(horizontal = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (showAllInsights) "Show Compact" else "Show All",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = ShiftlySerif,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = AzureInkOnWhite
                            )
                        }
                    }

                    TopSpendingCategoriesCard(
                        categories = state.categoryInsights,
                        currencySymbol = state.currencySymbol,
                        showAll = showAllInsights,
                        onToggleShowAll = { showAllInsights = !showAllInsights }
                    )

                    EarningsHoursBreakdownCard(
                        periodLabel = state.selectedRange.fullLabel,
                        currencySymbol = state.currencySymbol,
                        earnings = state.periodIncome,
                        totalHours = state.totalWorkedHours,
                        regularHours = state.regularHours,
                        nightDiffHours = state.nightDiffHours,
                        overtimeHours = state.overtimeHours
                    )

                    HoursOfWorkCostCard(
                        periodLabel = state.selectedRange.fullLabel,
                        currencySymbol = state.currencySymbol,
                        hourlyRate = state.hourlyRate,
                        items = state.workHoursCosts,
                        showAll = showAllInsights,
                        onToggleShowAll = { showAllInsights = !showAllInsights }
                    )

                    SavingsGoalProgressCard(
                        goalTitle = state.savingsGoalTitle,
                        savedAmount = state.savedProgressAmount,
                        targetAmount = state.savingsGoalTarget,
                        currencySymbol = state.currencySymbol,
                        onShowAllClick = onOpenGoalMessage
                    )

                    UnderBudgetStreakAndTipsCard(
                        streakDays = state.underBudgetStreakDays,
                        streakTargetDays = state.streakTargetDays,
                        dailyAllowance = state.dailyBudgetAllowance,
                        currencySymbol = state.currencySymbol,
                        tips = state.plainLanguageTips,
                        showAllTips = showAllInsights,
                        onToggleShowAll = { showAllInsights = !showAllInsights }
                    )

                    UnsettledSplitsTrackerCard(
                        unsettledAmount = state.unsettledSplitsAmount,
                        totalSplitAmount = state.totalSplitsAmount,
                        unsettledCount = state.unsettledFriendsCount,
                        currencySymbol = state.currencySymbol,
                        onOpenSplitBill = onOpenSplitBill
                    )
                }
            }
        }
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
        savingsGoalTitle = "Tuition & Laptop Fund",
        savedProgressAmount = 3420.0,
        savingsGoalTarget = 5000.0,
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
                    isSelected = { it == Screen.Home },
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
        savingsGoalTitle = "Tuition & Laptop Fund",
        savedProgressAmount = 4230.0,
        savingsGoalTarget = 5000.0,
        underBudgetStreakDays = 12,
        streakTargetDays = 14,
        dailyBudgetAllowance = 120.0,
        plainLanguageTips = listOf(
            "Nice! You cut transport spending by 14% vs. last month.",
            "Night differential shifts added +12.0 bonus hours this month."
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
                    isSelected = { it == Screen.Home },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
