package com.example.shiftly.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyCollapsedTopBar
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlyToolSheetContainer
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.profile.BudgetsEditorSheetContent
import com.example.shiftly.ui.screens.profile.ManageCategoriesSheetContent
import com.example.shiftly.ui.screens.profile.ShiftlySheetActionState
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate

private enum class ActiveToolTutorial {
    BUDGETS,
    CATEGORIES,
    PAYCHECK,
    IOU,
    CALENDAR
}

@Composable
fun ToolsScreen(
    repository: ShiftlyRepository,
    onOpenDuties: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onOpenRecurringBills: () -> Unit = {},
    onOpenAllowance: () -> Unit = {},
    onQuickLogShift: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: ToolsViewModel = viewModel(factory = ToolsViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    var showBudgetsSheet by remember { mutableStateOf(false) }
    var showManageCategoriesSheet by remember { mutableStateOf(false) }
    var showAddSavingsGoalSheet by remember { mutableStateOf(false) }
    var showSavingsGoalDetailSheet by remember { mutableStateOf(false) }
    var showPaycheckEstimatorSheet by remember { mutableStateOf(false) }
    var showIouSheet by remember { mutableStateOf(false) }
    var showActivityCalendarSheet by remember { mutableStateOf(false) }
    var activeTutorial by remember { mutableStateOf<ActiveToolTutorial?>(null) }

    val onOpenBudgets = remember { { showBudgetsSheet = true } }
    val onOpenSavingsGoals = remember(state.savingsGoals.isEmpty()) {
        {
            if (state.savingsGoals.isEmpty()) {
                showAddSavingsGoalSheet = true
            } else {
                showSavingsGoalDetailSheet = true
            }
        }
    }
    val onOpenPaycheckEstimator = remember { { showPaycheckEstimatorSheet = true } }
    val onOpenIouTracker = remember { { showIouSheet = true } }
    val onOpenActivityCalendar = remember { { showActivityCalendarSheet = true } }

    ToolsScreenContent(
        state = state,
        onOpenDuties = onOpenDuties,
        onOpenExpense = onOpenExpense,
        onOpenSplitBill = onOpenSplitBill,
        onOpenBudgets = onOpenBudgets,
        onOpenSavingsGoals = onOpenSavingsGoals,
        onOpenRecurringBills = onOpenRecurringBills,
        onOpenAllowance = onOpenAllowance,
        onOpenPaycheckEstimator = onOpenPaycheckEstimator,
        onOpenIouTracker = onOpenIouTracker,
        onOpenActivityCalendar = onOpenActivityCalendar
    )

    if (showBudgetsSheet) {
        val budgetsActionState = remember { ShiftlySheetActionState() }
        ShiftlyToolSheetContainer(
            title = "Monthly & Category Budgets",
            onDismiss = {
                showManageCategoriesSheet = false
                showBudgetsSheet = false
            },
            onHelpClick = { activeTutorial = ActiveToolTutorial.BUDGETS }
        ) {
            BudgetsEditorSheetContent(
                currencySymbol = state.currencySymbol,
                currentMonthlyBudget = state.monthlyBudget,
                currentCategoryBudgets = state.settings.categoryBudgets,
                managedCategories = state.settings.managedCategories,
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

    if (showManageCategoriesSheet) {
        val categoriesActionState = remember { ShiftlySheetActionState() }
        ShiftlyToolSheetContainer(
            title = "Manage Categories",
            onDismiss = { showManageCategoriesSheet = false },
            onHelpClick = { activeTutorial = ActiveToolTutorial.CATEGORIES }
        ) {
            ManageCategoriesSheetContent(
                currentCategories = state.settings.managedCategories,
                onDismiss = { showManageCategoriesSheet = false },
                onSave = { updated ->
                    viewModel.updateManagedCategories(updated)
                    showManageCategoriesSheet = false
                    onShowMessage("Categories updated")
                },
                actionState = categoriesActionState
            )
        }
    }

    if (showSavingsGoalDetailSheet) {
        val activeGoalId = remember(state.savingsGoals) {
            (state.savingsGoals.firstOrNull { it.goal.isActive }
                ?: state.savingsGoals.firstOrNull())?.goal?.id
        }
        SavingsGoalDetailSheet(
            goals = state.savingsGoals,
            initialGoalId = activeGoalId,
            currencySymbol = state.currencySymbol,
            onDismiss = { showSavingsGoalDetailSheet = false },
            onSelectActiveGoal = { goalId -> viewModel.setActiveSavingsGoal(goalId) },
            onOpenAddNewGoal = {
                showSavingsGoalDetailSheet = false
                showAddSavingsGoalSheet = true
            },
            onAddDeposit = { goal, amount, note ->
                viewModel.addSavingsDeposit(goal, amount, note)
                onShowMessage("Saved ${ShiftlyFormatters.formatCurrency(state.currencySymbol, amount)}")
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

    if (showAddSavingsGoalSheet) {
        AddSavingsGoalSheet(
            currencySymbol = state.currencySymbol,
            onDismiss = { showAddSavingsGoalSheet = false },
            onCreateGoal = { title, targetAmount, targetDateEpochDay ->
                viewModel.createSavingsGoal(title, targetAmount, targetDateEpochDay)
                showAddSavingsGoalSheet = false
                onShowMessage("Savings goal \"$title\" added")
            }
        )
    }

    if (showPaycheckEstimatorSheet) {
        ShiftlyToolSheetContainer(
            title = "Paycheck Estimator",
            onDismiss = { showPaycheckEstimatorSheet = false },
            onHelpClick = { activeTutorial = ActiveToolTutorial.PAYCHECK }
        ) {
            PaycheckEstimatorSheetContent(
                currencySymbol = state.currencySymbol,
                grossPay = state.monthShiftEarnings,
                deductions = state.paycheckDeductions,
                onSaveDeductions = { updated ->
                    viewModel.updatePaycheckDeductions(updated)
                },
                onDismiss = { showPaycheckEstimatorSheet = false }
            )
        }
    }

    if (showIouSheet) {
        ShiftlyToolSheetContainer(
            title = "IOU & Loans",
            onDismiss = { showIouSheet = false },
            onHelpClick = { activeTutorial = ActiveToolTutorial.IOU }
        ) {
            IouTrackerSheetContent(
                currencySymbol = state.currencySymbol,
                entries = state.iouEntries,
                onSaveEntries = { updated ->
                    viewModel.updateIouEntries(updated)
                    onShowMessage("IOU entries saved")
                },
                onDismiss = { showIouSheet = false }
            )
        }
    }

    if (showActivityCalendarSheet) {
        ShiftlyToolSheetContainer(
            title = "Activity Calendar",
            onDismiss = { showActivityCalendarSheet = false },
            onHelpClick = { activeTutorial = ActiveToolTutorial.CALENDAR }
        ) {
            ActivityCalendarSheetContent(
                currencySymbol = state.currencySymbol,
                shifts = state.shifts,
                expenses = state.expenses,
                onDismiss = { showActivityCalendarSheet = false }
            )
        }
    }

    when (activeTutorial) {
        ActiveToolTutorial.BUDGETS -> {
            ShiftlyScreenTutorialSheet(
                title = "Budgets Guide",
                subtitle = "Set comfortable limits for your month and top categories.",
                steps = listOf(
                    "Set your monthly limit" to "Enter an overall monthly spending cap to unlock daily safe-to-spend guidance.",
                    "Assign category caps" to "Add optional limits for Food, Transport, or Bills to see individual progress bars.",
                    "Customize your categories" to "Enable, add, or organize the categories shown whenever you log an expense."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.CATEGORIES -> {
            ShiftlyScreenTutorialSheet(
                title = "Manage Categories Guide",
                subtitle = "Keep your expense categories organized around your routine.",
                steps = listOf(
                    "Toggle built-in categories" to "Hide categories you rarely use so logging expenses stays fast and uncluttered.",
                    "Add custom categories" to "Create your own spending categories that fit your school, work, or daily lifestyle.",
                    "Save your changes" to "Tap Save at the bottom to update your category list across the app."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.PAYCHECK -> {
            ShiftlyScreenTutorialSheet(
                title = "Paycheck Estimator Guide",
                subtitle = "Preview your estimated net take-home pay after deductions.",
                steps = listOf(
                    "Review your gross shift pay" to "Shiftly automatically pulls your current month's shift earnings as your starting gross pay.",
                    "Adjust deduction presets" to "Customize SSS, PhilHealth, Pag-IBIG, or tax amounts to match your actual payslip setup.",
                    "Check your net take-home" to "See your estimated take-home amount so you can plan budgets and savings realistically."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.IOU -> {
            ShiftlyScreenTutorialSheet(
                title = "IOU & Loans Guide",
                subtitle = "Remember who owes you and who you need to pay back.",
                steps = listOf(
                    "Add a borrow or lend entry" to "Enter the person's name, amount, and whether they owe you or you owe them.",
                    "Mark balances as settled" to "Mark an entry settled once paid back so your net IOU summary updates right away.",
                    "Keep records private" to "All IOU entries stay safely offline on your device for stress-free personal tracking."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.CALENDAR -> {
            ShiftlyScreenTutorialSheet(
                title = "Activity Calendar Guide",
                subtitle = "See your shifts and expenses together on one monthly view.",
                steps = listOf(
                    "Browse months & colored dots" to "Green dots mark days with logged shifts, while red dots highlight days with recorded expenses.",
                    "Tap any date" to "Select a day on the calendar to inspect its exact shifts, hours worked, and expense items below.",
                    "Spot busy weeks fast" to "Compare work days and spending days side by side to plan upcoming weeks."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        null -> Unit
    }
}

private val ToolGreen = Color(0xFF30D158)
private val ToolRed = Color(0xFFFF453A)
private val ToolBlue = Color(0xFF0A84FF)
private val ToolOrange = Color(0xFFFF9F0A)
private val ToolPurple = Color(0xFFBF5AF2)

@Composable
fun ToolsScreenContent(
    state: ToolsUiState,
    onOpenDuties: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onOpenBudgets: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenRecurringBills: () -> Unit = {},
    onOpenAllowance: () -> Unit = {},
    onOpenPaycheckEstimator: () -> Unit = {},
    onOpenIouTracker: () -> Unit = {},
    onOpenActivityCalendar: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val currencySymbol = state.currencySymbol

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
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            item(key = "tools_header", contentType = "header") {
                ShiftlyScreenHeader(
                    title = "Tools",
                    largeTitle = true,
                    modifier = Modifier
                        .padding(horizontal = 20.dp)
                        .graphicsLayer { alpha = headerAlpha }
                )
            }

            item(key = "tools_trackers_group", contentType = "tool_group") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "TRACKERS",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ShiftlyGlassCard(
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        ToolGroupedRow(
                            title = "Duties & Shifts",
                            metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.monthShiftEarnings),
                            iconRes = R.drawable.ic_lucide_clock,
                            iconTint = ToolGreen,
                            metricColor = colors.mintInk,
                            onClick = onOpenDuties
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "Expenses & Receipts",
                            metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.monthSpent),
                            iconRes = R.drawable.ic_lucide_receipt_text,
                            iconTint = ToolRed,
                            metricColor = colors.coralInk,
                            onClick = onOpenExpense
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "Split Bill",
                            metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.pendingSplitsAmount),
                            iconRes = R.drawable.ic_lucide_split,
                            iconTint = ToolBlue,
                            metricColor = colors.azureInk,
                            onClick = onOpenSplitBill
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "IOU & Loans",
                            metric = state.iouSummaryText,
                            iconRes = R.drawable.ic_lucide_circle_dollar_sign,
                            iconTint = ToolRed,
                            metricColor = if (state.iouSummaryText.contains("you owe", ignoreCase = true)) colors.coralInk else colors.mintInk,
                            onClick = onOpenIouTracker
                        )
                    }
                }
            }

            item(key = "tools_planning_group", contentType = "tool_group") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "PLANNING & INCOME",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ShiftlyGlassCard(
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        ToolGroupedRow(
                            title = "Monthly & Category Budgets",
                            metric = if (state.monthlyBudget > 0.0) ShiftlyFormatters.formatCurrency(currencySymbol, state.monthlyBudget) else "Set Budget",
                            iconRes = R.drawable.ic_lucide_wallet,
                            iconTint = ToolOrange,
                            metricColor = colors.textPrimary,
                            onClick = onOpenBudgets
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "Savings Goals",
                            metric = state.activeGoalTitle ?: "Add Goal",
                            iconRes = R.drawable.ic_lucide_piggy_bank,
                            iconTint = ToolGreen,
                            metricColor = colors.mintInk,
                            onClick = onOpenSavingsGoals
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "Recurring Bills",
                            metric = "${state.enabledRecurringCount} active",
                            iconRes = R.drawable.ic_lucide_repeat,
                            iconTint = ToolPurple,
                            metricColor = colors.textPrimary,
                            onClick = onOpenRecurringBills
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "Allowance",
                            metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.totalAllowanceAmount),
                            iconRes = R.drawable.ic_lucide_wallet,
                            iconTint = ToolBlue,
                            metricColor = colors.azureInk,
                            onClick = onOpenAllowance
                        )
                        ShiftlyHairlineDivider(startInset = 48.dp)
                        ToolGroupedRow(
                            title = "Paycheck Estimator",
                            metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.netPayTakeHome),
                            iconRes = R.drawable.ic_lucide_receipt_text,
                            iconTint = ToolGreen,
                            metricColor = colors.mintInk,
                            onClick = onOpenPaycheckEstimator
                        )
                    }
                }
            }

            item(key = "tools_calendar_group", contentType = "tool_group") {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "CALENDAR & UTILITIES",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                    ShiftlyGlassCard(
                        cornerRadius = 18.dp,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        ToolGroupedRow(
                            title = "Activity Calendar",
                            metric = "Monthly View",
                            iconRes = R.drawable.ic_lucide_calendar,
                            iconTint = ToolBlue,
                            metricColor = colors.textPrimary,
                            onClick = onOpenActivityCalendar
                        )
                    }
                }
            }
        }

        ShiftlyCollapsedTopBar(
            title = "Tools",
            isCollapsed = isTitleCollapsed,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun ToolsScreenContent(
    currencySymbol: String,
    shifts: List<ShiftEntity>,
    expenses: List<ExpenseEntity>,
    splitBills: List<SplitBillWithParticipants>,
    monthlyBudget: Double,
    savingsGoals: List<SavingsGoalWithDeposits> = emptyList(),
    recurringBills: List<RecurringBillEntity> = emptyList(),
    allowanceEntries: List<AllowanceEntryEntity> = emptyList(),
    cutoffScheduleLabel: String = CutoffScheduleConfig().displayLabel(),
    onOpenDuties: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onQuickLogShift: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onOpenBudgets: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenRecurringBills: () -> Unit = {},
    onOpenAllowance: () -> Unit = {},
    onOpenPaycheckEstimator: () -> Unit = {},
    onOpenIouTracker: () -> Unit = {},
    onOpenActivityCalendar: () -> Unit = {}
) {
    val state = remember(
        currencySymbol,
        shifts,
        expenses,
        splitBills,
        monthlyBudget,
        savingsGoals,
        recurringBills,
        allowanceEntries,
        cutoffScheduleLabel
    ) {
        ToolsViewModel.computeToolsUiState(
            currencySymbol = currencySymbol,
            monthlyBudget = monthlyBudget,
            settings = ShiftlySettingsSnapshot(),
            shifts = shifts,
            expenses = expenses,
            splitBills = splitBills,
            savingsGoals = savingsGoals,
            recurringBills = recurringBills,
            allowanceEntries = allowanceEntries
        ).copy(cutoffScheduleLabel = cutoffScheduleLabel)
    }

    ToolsScreenContent(
        state = state,
        onOpenDuties = onOpenDuties,
        onOpenExpense = onOpenExpense,
        onOpenSplitBill = onOpenSplitBill,
        onOpenBudgets = onOpenBudgets,
        onOpenSavingsGoals = onOpenSavingsGoals,
        onOpenRecurringBills = onOpenRecurringBills,
        onOpenAllowance = onOpenAllowance,
        onOpenPaycheckEstimator = onOpenPaycheckEstimator,
        onOpenIouTracker = onOpenIouTracker,
        onOpenActivityCalendar = onOpenActivityCalendar
    )
}

@Composable
private fun ToolGroupedRow(
    title: String,
    metric: String,
    @DrawableRes iconRes: Int,
    iconTint: Color,
    metricColor: Color = ShiftlyColors.current.textPrimary,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .shiftlyPressClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(22.dp)
        )

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = (-0.25).sp
            ),
            color = colors.textPrimary,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.weight(1f)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = metric,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.5.sp
                ).tabular(),
                color = metricColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(
                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                contentDescription = "Open $title",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "Tools Hub")
@Composable
private fun ToolsScreenPreview() {
    val today = LocalDate.now()
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ToolsScreenContent(
                    currencySymbol = "₱",
                    shifts = listOf(
                        ShiftEntity(
                            id = 1L,
                            dateEpochDay = today.toEpochDay(),
                            timeInMillis = 0L,
                            timeOutMillis = 0L,
                            durationMinutes = 360,
                            lockedHourlyRate = 100.0,
                            totalEarned = 660.0
                        )
                    ),
                    expenses = listOf(
                        ExpenseEntity(
                            id = 1L,
                            title = "Coffee",
                            amount = 145.0,
                            category = "Food",
                            dateEpochDay = today.toEpochDay()
                        )
                    ),
                    splitBills = emptyList(),
                    monthlyBudget = 3000.0,
                    onOpenDuties = {},
                    onOpenExpense = {},
                    onOpenSplitBill = {},
                    onQuickLogShift = {},
                    onQuickAddExpense = {}
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Tools },
                    onSelect = {},
                    onFabClick = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
