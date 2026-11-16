package com.example.shiftly.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun HomeScreen(
    repository: ShiftlyRepository,
    onNavigateToDuties: () -> Unit = {},
    onNavigateToExpense: () -> Unit = {},
    onNavigateToSplitBill: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    HomeScreenContent(
        state = state,
        onSelectDate = { viewModel.selectDate(it) },
        onResetToToday = { viewModel.resetDateToToday() },
        onQuickTimeIn = {
            if (state.activeTimer.isRunning) {
                onNavigateToDuties()
            } else {
                viewModel.startQuickShift()
                onShowMessage("Shift started at ${state.currencySymbol}${"%.2f".format(state.hourlyRate)}/hr")
            }
        },
        onNotificationClick = {
            if (state.pendingSplitsOwed > 0) {
                onNavigateToSplitBill()
                onShowMessage("${state.currencySymbol}${"%,.0f".format(state.pendingSplitsOwed)} pending from classmates")
            } else {
                onShowMessage("All caught up — no pending alerts")
            }
        },
        onProfileClick = onNavigateToProfile,
        onOpenPendingSplits = onNavigateToSplitBill,
        onViewInsights = onNavigateToAnalytics,
        onTimelineItemClick = { item ->
            when (item) {
                is TimelineActivityItem.ShiftActivity -> onNavigateToDuties()
                is TimelineActivityItem.ExpenseActivity -> onNavigateToExpense()
                is TimelineActivityItem.ExtraIncomeActivity -> onNavigateToProfile()
            }
        }
    )
}

@Composable
fun HomeScreenContent(
    state: HomeUiState,
    onSelectDate: (LocalDate) -> Unit,
    onResetToToday: () -> Unit = {},
    onQuickTimeIn: () -> Unit,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenPendingSplits: () -> Unit,
    onViewInsights: () -> Unit,
    onTimelineItemClick: (TimelineActivityItem) -> Unit = {},
    todayOverride: LocalDate? = null,
    greetingHourOverride: Int? = null
) {
    val currency = state.currencySymbol
    val locale = LocalConfiguration.current.locales[0]
    val dateHeaderFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val today = remember(todayOverride) { todayOverride ?: LocalDate.now() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            if (greetingHourOverride != null) {
                HomeGreetingHeader(
                    userName = state.userName,
                    pendingAlertsCount = state.pendingSplitBillCount,
                    greetingHour = greetingHourOverride,
                    onNotificationClick = onNotificationClick,
                    onProfileClick = onProfileClick
                )
            } else {
                HomeGreetingHeader(
                    userName = state.userName,
                    pendingAlertsCount = state.pendingSplitBillCount,
                    onNotificationClick = onNotificationClick,
                    onProfileClick = onProfileClick
                )
            }
        }

        item {
            HomeHeroPager(state = state)
        }

        item {
            CircularWeekdaySelector(
                slots = state.weekdaySlots,
                selectedDate = state.selectedDate,
                onSelectDate = onSelectDate
            )
        }

        item {
            NextShiftQuickCard(
                hourlyRate = state.hourlyRate,
                currencySymbol = currency,
                isTimerRunning = state.activeTimer.isRunning,
                onQuickTimeIn = onQuickTimeIn
            )
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = DarkSurface,
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .padding(bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 40.dp, height = 4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(DarkBorder)
                        )
                    }

                    val budgetRatio = if (state.monthlyBudget > 0.0) {
                        (state.mtdExpenses / state.monthlyBudget).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val nearLimit = budgetRatio >= 0.80f
                    val budgetPct = (budgetRatio * 100).toInt()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                                color = TextLightPrimary
                            )
                            Text(
                                text = "$currency${"%,.0f".format(state.mtdExpenses)} of $currency${"%,.0f".format(state.monthlyBudget)}",
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = if (nearLimit) CoralInkOnWhite else MintInkOnWhite
                            )
                        }
                        LinearProgressIndicator(
                            progress = { budgetRatio },
                            color = if (nearLimit) CoralBright else MintBright,
                            trackColor = DarkBorder,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .semantics {
                                    contentDescription = "Monthly budget $budgetPct percent used"
                                }
                        )
                    }

                    if (state.pendingSplitsOwed > 0.0) {
                        PendingSplitsBanner(
                            pendingAmount = state.pendingSplitsOwed,
                            billCount = state.pendingSplitBillCount,
                            currencySymbol = currency,
                            onClick = onOpenPendingSplits
                        )
                    }

                    WeeklyEarningsSnapshotCard(
                        weeklyEarnings = state.weeklyEarnings,
                        weeklyHours = state.weeklyHours,
                        bars = state.weeklyBars,
                        currencySymbol = currency,
                        onViewInsights = onViewInsights
                    )

                    val selectedLabel = state.selectedDate.format(dateHeaderFormatter)
                    val isDifferentDay = state.selectedDate != today
                    val timelineTitle = when {
                        isDifferentDay && state.selectedDateHasLogs -> "Activity • $selectedLabel"
                        !isDifferentDay && state.selectedDateHasLogs -> "Today's Activity"
                        else -> "Recent Activity"
                    }
                    val timelineTrailing = when {
                        isDifferentDay && state.selectedDateHasLogs -> "Today"
                        isDifferentDay -> "None on $selectedLabel • Today"
                        !state.selectedDateHasLogs && state.timelineItems.isNotEmpty() -> "Latest logs"
                        else -> null
                    }

                    ShiftlySectionHeader(
                        title = timelineTitle,
                        count = state.timelineItems.size,
                        trailingText = timelineTrailing,
                        onTrailingClick = if (isDifferentDay) onResetToToday else null,
                        onLightSurface = true
                    )

                    if (state.timelineItems.isEmpty()) {
                        Text(
                            text = "No activity logged yet. Tap Time In above to start a shift.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextLightSecondary,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column {
                            state.timelineItems.forEachIndexed { idx, item ->
                                HomeTimelineRow(
                                    item = item,
                                    currencySymbol = currency,
                                ) { onTimelineItemClick(item) }
                                if (idx < state.timelineItems.lastIndex) {
                                    HorizontalDivider(color = DarkBorder)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Home Dashboard")
@Composable
private fun HomeScreenPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val sampleState = HomeUiState(
        userName = "Maya",
        currencySymbol = "₱",
        hourlyRate = 96.38,
        totalBalance = 2781.92,
        totalIncome = 3373.30,
        totalExpenses = 591.38,
        mtdIncome = 3373.30,
        mtdExpenses = 591.38,
        monthlyBudget = 3000.0,
        savingsBuffer = 1000.0,
        pendingSplitsOwed = 720.0,
        pendingSplitBillCount = 1,
        safeToSpend = 1061.92,
        weeklyEarnings = 1493.89,
        weeklyHours = 15.5,
        weeklyBars = listOf(
            WeeklyBarData("M", 385.52),
            WeeklyBarData("T", 0.0),
            WeeklyBarData("W", 385.52),
            WeeklyBarData("T", 337.33),
            WeeklyBarData("F", 385.52),
            WeeklyBarData("S", 0.0),
            WeeklyBarData("S", 0.0)
        ),
        selectedDate = today,
        isShowingSelectedDateOnly = true,
        selectedDateHasLogs = true,
        weekdaySlots = listOf(
            WeekdaySlot(today.minusDays(3), "M", "13", hasShift = true, hasExpense = false),
            WeekdaySlot(today.minusDays(2), "T", "14", hasShift = false, hasExpense = true),
            WeekdaySlot(today.minusDays(1), "W", "15", hasShift = true, hasExpense = true),
            WeekdaySlot(today, "T", "16", hasShift = true, hasExpense = true),
            WeekdaySlot(today.plusDays(1), "F", "17", hasShift = false, hasExpense = false),
            WeekdaySlot(today.plusDays(2), "S", "18", hasShift = false, hasExpense = false),
            WeekdaySlot(today.plusDays(3), "S", "19", hasShift = false, hasExpense = false)
        ),
        timelineItems = listOf(
            TimelineActivityItem.ShiftActivity(
                id = 1,
                title = "Campus Café Shift",
                timeRange = "2:00 PM – 6:00 PM",
                durationLabel = "4h 0m",
                amount = 385.52,
                epochDay = today.toEpochDay(),
                timestamp = 3L
            ),
            TimelineActivityItem.ExpenseActivity(
                id = 2,
                title = "Tapsilog & Iced Coffee",
                category = "Food",
                timeLabel = "Today",
                amount = 145.0,
                epochDay = today.toEpochDay(),
                timestamp = 2L
            ),
            TimelineActivityItem.ExpenseActivity(
                id = 3,
                title = "Jeepney & LRT Fare",
                category = "Transport",
                timeLabel = "Today",
                amount = 58.0,
                epochDay = today.toEpochDay(),
                timestamp = 1L
            )
        )
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                    state = sampleState,
                    onSelectDate = {},
                    onQuickTimeIn = {},
                    onNotificationClick = {},
                    onProfileClick = {},
                    onOpenPendingSplits = {},
                    onViewInsights = {},
                    todayOverride = today,
                    greetingHourOverride = 10
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
