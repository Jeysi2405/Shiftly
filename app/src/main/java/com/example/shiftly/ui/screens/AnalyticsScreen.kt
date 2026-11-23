package com.example.shiftly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyCollapsedTopBar
import com.example.shiftly.ui.components.ShiftlyGroupLabel
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlyToolSheetContainer
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.profile.BudgetsEditorSheetContent
import com.example.shiftly.ui.screens.profile.ShiftlySheetActionState
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
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
    var showBudgetsSheet by remember { mutableStateOf(false) }
    var showBudgetsTutorial by remember { mutableStateOf(false) }

    AnalyticsScreenContent(
        state = state,
        onBack = onBack,
        onSelectRange = { viewModel.selectRange(it) },
        onOpenSplitBill = onNavigateToSplitBill,
        onOpenBudgetSettings = { showBudgetsSheet = true },
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

    if (showBudgetsSheet) {
        val budgetsActionState = remember { ShiftlySheetActionState() }
        ShiftlyToolSheetContainer(
            title = "Monthly Limits",
            onDismiss = { showBudgetsSheet = false },
            onHelpClick = { showBudgetsTutorial = true }
        ) {
            BudgetsEditorSheetContent(
                currencySymbol = state.currencySymbol,
                currentMonthlyBudget = state.monthlyBudget,
                currentCategoryBudgets = state.categoryBudgets,
                managedCategories = state.managedCategories,
                onDismiss = { showBudgetsSheet = false },
                onSave = { monthly, categoryList, managedList ->
                    viewModel.updateBudgets(monthly, categoryList)
                    viewModel.updateManagedCategories(managedList)
                    showBudgetsSheet = false
                    onShowMessage("Budgets saved")
                },
                actionState = budgetsActionState
            )
        }
    }

    if (showBudgetsTutorial) {
        ShiftlyScreenTutorialSheet(
            title = "Budgets Guide",
            subtitle = null,
            steps = listOf(
                "Set monthly limit" to "Add an overall monthly spending cap.",
                "Assign category caps" to "Set optional limits per spending category.",
                "Manage categories" to "Enable or add custom expense categories."
            ),
            onDismiss = { showBudgetsTutorial = false }
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun AnalyticsScreenContent(
    state: AnalyticsUiState,
    onBack: () -> Unit,
    onSelectRange: (AnalyticsRange) -> Unit,
    onOpenSplitBill: () -> Unit = {},
    onOpenBudgetSettings: () -> Unit = {},
    onOpenGoalSettings: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    var showMoreInsights by rememberSaveable { mutableStateOf(false) }
    val moreChevronRotation by animateFloatAsState(
        targetValue = if (showMoreInsights) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "moreInsightsChevron"
    )
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
            contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item(key = "analytics_top_bar", contentType = "top_bar") {
                Box(modifier = Modifier.graphicsLayer { alpha = headerAlpha }) {
                    AnalyticsTopBar()
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

            if (state.plainLanguageTips.isNotEmpty()) {
                item(key = "analytics_smart_tips", contentType = "smart_tips") {
                    Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                        SmartTakeawaysCard(tips = state.plainLanguageTips)
                    }
                }
            }

            item(key = "analytics_budget_savings_section", contentType = "analytics_section") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlyGroupLabel(text = "Budget")

                    BudgetAndSavingsSummaryCard(
                        streakDays = state.underBudgetStreakDays,
                        streakTargetDays = state.streakTargetDays,
                        dailyAllowance = state.dailyBudgetAllowance,
                        ratePercentage = state.netSavingsRate,
                        netAmount = state.netSavingsAmount,
                        progressFraction = state.netSavingsFraction,
                        projectedSpend = state.projectedMonthlySpend,
                        dailyPace = state.dailyAveragePace,
                        isWithinBudget = state.isProjectedWithinBudget,
                        currencySymbol = state.currencySymbol,
                        onOpenBudgetSettings = onOpenBudgetSettings
                    )
                }
            }

            item(key = "analytics_spending_section", contentType = "analytics_section") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlyGroupLabel(text = "Spending")

                    TopSpendingCategoriesCard(
                        categories = state.categoryInsights,
                        currencySymbol = state.currencySymbol
                    )
                }
            }

            item(key = "analytics_work_section", contentType = "analytics_section") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlyGroupLabel(text = "Work")

                    EarningsHoursBreakdownCard(
                        currencySymbol = state.currencySymbol,
                        earnings = state.periodIncome,
                        totalHours = state.totalWorkedHours,
                        regularHours = state.regularHours,
                        nightDiffHours = state.nightDiffHours,
                        overtimeHours = state.overtimeHours
                    )
                }
            }

            item(key = "analytics_more_insights", contentType = "analytics_more") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = ShiftlyShapes.Card,
                        color = colors.card,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(ShiftlyShapes.Card)
                            .shiftlyPressClickable { showMoreInsights = !showMoreInsights }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "More insights",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_chevron_right,
                                contentDescription = if (showMoreInsights) "Collapse more insights" else "Expand more insights",
                                tint = colors.textSecondary,
                                triggerKey = showMoreInsights,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = moreChevronRotation }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = showMoreInsights,
                        enter = expandVertically(spring(dampingRatio = 0.85f)) + fadeIn(),
                        exit = shrinkVertically(spring(dampingRatio = 0.85f)) + fadeOut()
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            PeakShiftSpendPatternCard(
                                topEarningDayLabel = state.topEarningDayLabel,
                                highestSpendDayLabel = state.highestSpendDayLabel
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
