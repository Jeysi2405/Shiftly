package com.example.shiftly.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.InAppNotificationItem
import com.example.shiftly.data.local.InAppNotificationType
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.shiftlySheetBodyBand
import com.example.shiftly.ui.components.shiftlySheetTopBand
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.CutoffShiftSummary
import com.example.shiftly.utils.CutoffWindow
import com.example.shiftly.utils.SavingsGoalMath
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate
import java.time.ZoneId

@Composable
fun HomeScreen(
    repository: ShiftlyRepository,
    onNavigateToDuties: () -> Unit = {},
    onNavigateToExpense: () -> Unit = {},
    onNavigateToSplitBill: () -> Unit = {},
    onNavigateToRecurringBills: () -> Unit = {},
    onNavigateToAllowance: () -> Unit = {},
    onNavigateToProfile: () -> Unit = {},
    onNavigateToAnalytics: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showAddGoalSheet by remember { mutableStateOf(false) }
    var viewingGoalId by remember { mutableStateOf<Long?>(null) }
    var showGoalDetailSheet by remember { mutableStateOf(false) }
    var isInboxOpen by remember { mutableStateOf(false) }

    BackHandler(enabled = isInboxOpen) {
        isInboxOpen = false
        viewModel.markAllNotificationsRead()
    }

    val handleNotificationItemClick: (InAppNotificationItem) -> Unit = { item ->
        viewModel.markNotificationRead(item.id)
        isInboxOpen = false
        when (item.type) {
            InAppNotificationType.BILL_DUE -> onNavigateToRecurringBills()
            InAppNotificationType.BILL_POSTED -> onNavigateToExpense()
            InAppNotificationType.SHIFT_REMINDER -> onNavigateToDuties()
            InAppNotificationType.SAVINGS_REMINDER -> {
                viewingGoalId = null
                showGoalDetailSheet = true
            }
            InAppNotificationType.PAYDAY_CUTOFF -> onNavigateToAllowance()
            InAppNotificationType.SPLIT_BILL -> onNavigateToSplitBill()
        }
    }

    HomeScreenContent(
        state = state,
        isInboxOpen = isInboxOpen,
        onNotificationClick = {
            if (isInboxOpen) {
                isInboxOpen = false
                viewModel.markAllNotificationsRead()
            } else {
                state.pendingHeadsUpNotification?.let { viewModel.markNotificationToastShown(it.id) }
                isInboxOpen = true
            }
        },
        onDismissInbox = {
            isInboxOpen = false
            viewModel.markAllNotificationsRead()
        },
        onClearAllNotifications = {
            viewModel.clearAllNotifications()
        },
        onNotificationItemClick = handleNotificationItemClick,
        onDismissHeadsUpToast = { id ->
            viewModel.markNotificationToastShown(id)
        },
        onProfileClick = onNavigateToProfile,
        onOpenPendingSplits = onNavigateToSplitBill,
        onViewInsights = onNavigateToAnalytics,
        onOpenGoalDetail = { goalId ->
            viewingGoalId = goalId
            showGoalDetailSheet = true
        },
        onOpenAddGoal = { showAddGoalSheet = true },
        onSelectActiveGoal = { goalId -> viewModel.setActiveSavingsGoal(goalId) },
        onTimelineItemClick = { item ->
            when (item) {
                is TimelineActivityItem.ShiftActivity -> onNavigateToDuties()
                is TimelineActivityItem.ExpenseActivity -> onNavigateToExpense()
                is TimelineActivityItem.ExtraIncomeActivity -> onNavigateToProfile()
            }
        }
    )

    if (showGoalDetailSheet) {
        val detailGoals = remember(state.savingsGoals) {
            state.savingsGoals.map { it.goalWithDeposits }
        }
        SavingsGoalDetailSheet(
            goals = detailGoals,
            initialGoalId = viewingGoalId,
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
fun HomeScreenContent(
    state: HomeUiState,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    onOpenPendingSplits: () -> Unit,
    onViewInsights: () -> Unit,
    isInboxOpen: Boolean = false,
    onDismissInbox: () -> Unit = {},
    onClearAllNotifications: () -> Unit = {},
    onNotificationItemClick: (InAppNotificationItem) -> Unit = {},
    onDismissHeadsUpToast: (String) -> Unit = {},
    onOpenGoalDetail: (Long?) -> Unit = {},
    onOpenAddGoal: () -> Unit = {},
    onSelectActiveGoal: (Long) -> Unit = {},
    onTimelineItemClick: (TimelineActivityItem) -> Unit = {},
    todayOverride: LocalDate? = null,
    greetingHourOverride: Int? = null,
    notificationReferenceTimeMillis: Long? = null
) {
    val colors = ShiftlyColors.current
    val currency = state.currencySymbol
    val budgetRatio = remember(state.mtdExpenses, state.monthlyBudget) {
        if (state.monthlyBudget > 0.0) {
            (state.mtdExpenses / state.monthlyBudget).toFloat().coerceIn(0f, 1f)
        } else {
            0f
        }
    }
    val nearLimit = budgetRatio >= 0.80f
    val budgetPct = (budgetRatio * 100).toInt()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item(key = "greeting_header", contentType = "greeting_header") {
                Box(modifier = Modifier.padding(bottom = 14.dp)) {
                    HomeGreetingHeader(
                        profilePhotoPath = state.profilePhotoPath,
                        unreadCount = state.unreadNotificationCount,
                        isInboxOpen = isInboxOpen,
                        todayOverride = todayOverride,
                        onNotificationClick = onNotificationClick,
                        onProfileClick = onProfileClick
                    )
                }
            }

            item(key = "greeting_banner", contentType = "greeting_banner") {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                    DashboardGreetingBanner(
                        userName = state.userName,
                        greetingHour = greetingHourOverride
                    )
                }
            }

            item(key = "hero_pager", contentType = "hero_pager") {
                Box(modifier = Modifier.padding(bottom = 14.dp)) {
                    HomeHeroPager(
                        state = state,
                        onOpenGoalDetail = onOpenGoalDetail,
                        onOpenAddGoal = onOpenAddGoal,
                        onSelectActiveGoal = onSelectActiveGoal
                    )
                }
            }

            item(key = "sheet_header", contentType = "sheet_header") {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shiftlySheetTopBand(colors.surface, colors.cardBorder)
                        .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShiftlySheetDragHandle()

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Monthly Budget",
                                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (state.monthlyBudget > 0.0) {
                                    "${ShiftlyFormatters.formatCurrency(currency, state.mtdExpenses)} of ${ShiftlyFormatters.formatCurrency(currency, state.monthlyBudget)}"
                                } else {
                                    "${ShiftlyFormatters.formatCurrency(currency, state.mtdExpenses)} spent • No cap set"
                                },
                                style = MaterialTheme.typography.labelMedium.tabular(),
                                color = if (nearLimit && state.monthlyBudget > 0.0) colors.coralInk else colors.mintInk
                            )
                        }
                        LinearProgressIndicator(
                            progress = { budgetRatio },
                            color = if (nearLimit) colors.coralBright else colors.mintBright,
                            trackColor = colors.cardElevated,
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

                    MonthlyTopCategoriesCard(
                        monthName = state.currentMonthName,
                        monthlyTotalSpent = state.mtdExpenses,
                        topCategories = state.monthlyTopCategories,
                        currencySymbol = currency,
                        onViewInsights = onViewInsights
                    )

                    MonthlyHoursIndicatorCard(
                        monthName = state.currentMonthName,
                        summary = state.monthlyHoursSummary
                    )

                    ShiftlySectionHeader(
                        title = "Recent Activity",
                        count = state.timelineItems.size,
                        onLightSurface = true
                    )
                }
            }

            if (state.timelineItems.isEmpty()) {
                item(key = "timeline_empty", contentType = "timeline_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shiftlySheetBodyBand(colors.surface, colors.cardBorder)
                            .padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 112.dp)
                    ) {
                        Text(
                            text = "No activity logged yet. Start a shift in Duties or log an expense.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                itemsIndexed(
                    items = state.timelineItems,
                    key = { _, item ->
                        when (item) {
                            is TimelineActivityItem.ShiftActivity -> "shift_${item.id}"
                            is TimelineActivityItem.ExpenseActivity -> "expense_${item.id}"
                            is TimelineActivityItem.ExtraIncomeActivity ->
                                "extra_${item.subtitleTag}_${item.id}_${item.sortTimestamp}"
                        }
                    },
                    contentType = { _, item ->
                        when (item) {
                            is TimelineActivityItem.ShiftActivity -> "timeline_shift"
                            is TimelineActivityItem.ExpenseActivity -> "timeline_expense"
                            is TimelineActivityItem.ExtraIncomeActivity -> "timeline_extra"
                        }
                    }
                ) { idx, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shiftlySheetBodyBand(colors.surface, colors.cardBorder)
                            .padding(horizontal = 20.dp)
                    ) {
                        HomeTimelineRow(
                            item = item,
                            currencySymbol = currency
                        ) { onTimelineItemClick(item) }
                        if (idx < state.timelineItems.lastIndex) {
                            HorizontalDivider(color = colors.border)
                        }
                    }
                }

                item(key = "sheet_footer", contentType = "sheet_footer") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shiftlySheetBodyBand(colors.surface, colors.cardBorder)
                            .height(104.dp)
                    )
                }
            }
        }

        NotificationInboxDropdown(
            visible = isInboxOpen,
            notifications = state.notifications,
            unreadCount = state.unreadNotificationCount,
            onDismiss = onDismissInbox,
            onClearAll = onClearAllNotifications,
            onItemClick = onNotificationItemClick,
            referenceTimeMillis = notificationReferenceTimeMillis
        )

        NotificationHeadsUpBanner(
            notification = if (isInboxOpen) null else state.pendingHeadsUpNotification,
            onTap = onNotificationItemClick,
            onDismiss = onDismissHeadsUpToast,
            referenceTimeMillis = notificationReferenceTimeMillis
        )
    }
}

private fun sampleHomePreviewState(
    today: LocalDate,
    notifications: List<InAppNotificationItem> = emptyList(),
    pendingHeadsUp: InAppNotificationItem? = null
): HomeUiState {
    val goal = SavingsGoalEntity(
        id = 1L,
        title = "Motor",
        targetAmount = 5000.0,
        targetDateEpochDay = today.plusDays(20).toEpochDay(),
        isActive = true
    )
    return HomeUiState(
        userName = "James",
        currencySymbol = "₱",
        hourlyRate = 96.38,
        totalBalance = 2781.92,
        totalIncome = 3373.30,
        totalExpenses = 591.38,
        mtdIncome = 3373.30,
        mtdExpenses = 591.38,
        monthlyBudget = 3000.0,
        pendingSplitsOwed = 720.0,
        pendingSplitBillCount = 1,
        notifications = notifications,
        unreadNotificationCount = notifications.count { !it.isRead },
        pendingHeadsUpNotification = pendingHeadsUp,
        cutoffSummary = CutoffShiftSummary(
            window = CutoffWindow(
                startDate = LocalDate.of(2026, 11, 16),
                endDate = LocalDate.of(2026, 11, 30),
                isFirstCutoff = false,
                label = "2nd Cutoff • Nov 16–30"
            ),
            totalEarnings = 1493.89,
            shiftDaysCount = 4,
            totalHours = 15.5
        ),
        savingsGoals = listOf(
            SavingsGoalCardItem(
                goalWithDeposits = SavingsGoalWithDeposits(goal = goal, deposits = emptyList()),
                progress = SavingsGoalMath.computeProgress(1500.0, 5000.0, today.plusDays(20), today),
                targetDate = today.plusDays(20)
            )
        ),
        currentMonthName = "November",
        monthlyTopCategories = listOf(
            CategorySpendSummary("Food", 320.0, 0.54f),
            CategorySpendSummary("Transport", 150.0, 0.25f),
            CategorySpendSummary("Supplies", 121.38, 0.21f)
        ),
        monthlyHoursSummary = MonthlyHoursSummary(
            totalHours = 42.5,
            nightDiffHours = 6.0,
            overtimeHours = 3.5
        ),
        timelineItems = listOf(
            TimelineActivityItem.ShiftActivity(
                id = 1,
                title = "Campus Café Shift",
                timeRange = "2:00 PM – 6:00 PM",
                durationLabel = "4h",
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
            )
        )
    )
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Home Dashboard")
@Composable
private fun HomeScreenPreview() {
    val today = LocalDate.of(2026, 11, 17)
    val nowMillis = today.atTime(10, 0).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val sampleNotifications = listOf(
        InAppNotificationItem(
            id = "n1",
            eventKey = "bill_due_1",
            type = InAppNotificationType.BILL_DUE,
            title = "Bill Due Soon: Dorm Wi-Fi",
            message = "₱599.00 is due soon (6:00 PM).",
            timestampMillis = nowMillis - 5 * 60_000L,
            isRead = false,
            hasShownToast = true
        )
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                    state = sampleHomePreviewState(today, notifications = sampleNotifications),
                    onNotificationClick = {},
                    onProfileClick = {},
                    onOpenPendingSplits = {},
                    onViewInsights = {},
                    todayOverride = today,
                    greetingHourOverride = 10,
                    notificationReferenceTimeMillis = nowMillis
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Home Notification Inbox Open")
@Composable
private fun HomeScreenInboxOpenPreview() {
    val today = LocalDate.of(2026, 11, 17)
    val nowMillis = today.atTime(10, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val sampleNotifications = listOf(
        InAppNotificationItem(
            id = "n1",
            eventKey = "bill_due_1",
            type = InAppNotificationType.BILL_DUE,
            title = "Bill Due Soon: Dorm Wi-Fi",
            message = "₱599.00 is due soon (6:00 PM).",
            timestampMillis = nowMillis - 4 * 60_000L,
            isRead = false,
            hasShownToast = true
        ),
        InAppNotificationItem(
            id = "n2",
            eventKey = "split_1",
            type = InAppNotificationType.SPLIT_BILL,
            title = "1 Split Bill Pending",
            message = "₱720.00 awaiting payment from friends.",
            timestampMillis = nowMillis - 42 * 60_000L,
            isRead = false,
            hasShownToast = true
        ),
        InAppNotificationItem(
            id = "n3",
            eventKey = "cutoff_1",
            type = InAppNotificationType.PAYDAY_CUTOFF,
            title = "Payday Cutoff Credited",
            message = "₱1,493.89 from 1st Cutoff added to Allowance.",
            timestampMillis = nowMillis - 26 * 3_600_000L,
            isRead = true,
            hasShownToast = true
        )
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                    state = sampleHomePreviewState(today, notifications = sampleNotifications),
                    isInboxOpen = true,
                    onNotificationClick = {},
                    onProfileClick = {},
                    onOpenPendingSplits = {},
                    onViewInsights = {},
                    todayOverride = today,
                    greetingHourOverride = 10,
                    notificationReferenceTimeMillis = nowMillis
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Home Heads-Up Toast Banner")
@Composable
private fun HomeScreenHeadsUpToastPreview() {
    val today = LocalDate.of(2026, 11, 17)
    val nowMillis = today.atTime(10, 30).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    val toastItem = InAppNotificationItem(
        id = "n_toast",
        eventKey = "bill_due_toast",
        type = InAppNotificationType.BILL_DUE,
        title = "Bill Due Soon: Dorm Wi-Fi",
        message = "₱599.00 is due soon (6:00 PM).",
        timestampMillis = nowMillis - 12_000L,
        isRead = false,
        hasShownToast = false
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                HomeScreenContent(
                    state = sampleHomePreviewState(
                        today = today,
                        notifications = listOf(toastItem),
                        pendingHeadsUp = toastItem
                    ),
                    onNotificationClick = {},
                    onProfileClick = {},
                    onOpenPendingSplits = {},
                    onViewInsights = {},
                    todayOverride = today,
                    greetingHourOverride = 10,
                    notificationReferenceTimeMillis = nowMillis
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
