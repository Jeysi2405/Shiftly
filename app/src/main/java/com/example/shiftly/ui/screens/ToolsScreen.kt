package com.example.shiftly.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.profile.BudgetsEditorSheetContent
import com.example.shiftly.ui.screens.profile.ManageCategoriesSheetContent
import com.example.shiftly.ui.screens.profile.ProfileViewModel
import com.example.shiftly.ui.screens.profile.SettingsSheetContainer
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate
import java.time.YearMonth

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
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(repository))
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val shifts by repository.allShifts.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by repository.allExpenses.collectAsStateWithLifecycle(initialValue = emptyList())
    val splitBills by repository.allSplitBills.collectAsStateWithLifecycle(initialValue = emptyList())
    val recurringBills by repository.allRecurringBills.collectAsStateWithLifecycle(initialValue = emptyList())
    val allowanceEntries by repository.allAllowanceEntries.collectAsStateWithLifecycle(initialValue = emptyList())

    var showBudgetsSheet by remember { mutableStateOf(false) }
    var showManageCategoriesSheet by remember { mutableStateOf(false) }
    var showAddSavingsGoalSheet by remember { mutableStateOf(false) }
    var showSavingsGoalDetailSheet by remember { mutableStateOf(false) }

    val cutoffScheduleLabel = remember(profileState.settings) {
        profileState.settings.cutoffScheduleConfig().displayLabel()
    }

    ToolsScreenContent(
        currencySymbol = profileState.currencySymbol,
        shifts = shifts,
        expenses = expenses,
        splitBills = splitBills,
        monthlyBudget = profileState.monthlyBudget,
        savingsGoals = profileState.savingsGoals,
        recurringBills = recurringBills,
        allowanceEntries = allowanceEntries,
        cutoffScheduleLabel = cutoffScheduleLabel,
        onOpenDuties = onOpenDuties,
        onOpenExpense = onOpenExpense,
        onOpenSplitBill = onOpenSplitBill,
        onQuickLogShift = onQuickLogShift,
        onQuickAddExpense = onQuickAddExpense,
        onOpenBudgets = { showBudgetsSheet = true },
        onOpenSavingsGoals = {
            if (profileState.savingsGoals.isEmpty()) {
                showAddSavingsGoalSheet = true
            } else {
                showSavingsGoalDetailSheet = true
            }
        },
        onOpenRecurringBills = onOpenRecurringBills,
        onOpenAllowance = onOpenAllowance
    )

    if (showBudgetsSheet) {
        SettingsSheetContainer(
            title = "Monthly & Category Budgets",
            onDismiss = {
                showManageCategoriesSheet = false
                showBudgetsSheet = false
            }
        ) {
            BudgetsEditorSheetContent(
                currencySymbol = profileState.currencySymbol,
                currentMonthlyBudget = profileState.monthlyBudget,
                currentCategoryBudgets = profileState.settings.categoryBudgets,
                managedCategories = profileState.settings.managedCategories,
                onManageCategories = { showManageCategoriesSheet = true },
                onDismiss = { showBudgetsSheet = false },
                onSave = { monthly, categoryList ->
                    profileViewModel.updateBudgets(monthly, categoryList)
                    showBudgetsSheet = false
                    onShowMessage("Budgets saved")
                }
            )
        }
    }

    if (showManageCategoriesSheet) {
        SettingsSheetContainer(
            title = "Manage Categories",
            onDismiss = { showManageCategoriesSheet = false }
        ) {
            ManageCategoriesSheetContent(
                currentCategories = profileState.settings.managedCategories,
                onSave = { updated ->
                    profileViewModel.updateManagedCategories(updated)
                    showManageCategoriesSheet = false
                    onShowMessage("Categories updated")
                }
            )
        }
    }

    if (showSavingsGoalDetailSheet) {
        val activeGoalId = remember(profileState.savingsGoals) {
            (profileState.savingsGoals.firstOrNull { it.goal.isActive }
                ?: profileState.savingsGoals.firstOrNull())?.goal?.id
        }
        SavingsGoalDetailSheet(
            goals = profileState.savingsGoals,
            initialGoalId = activeGoalId,
            currencySymbol = profileState.currencySymbol,
            onDismiss = { showSavingsGoalDetailSheet = false },
            onSelectActiveGoal = { goalId -> profileViewModel.setActiveSavingsGoal(goalId) },
            onOpenAddNewGoal = {
                showSavingsGoalDetailSheet = false
                showAddSavingsGoalSheet = true
            },
            onAddDeposit = { goal, amount, note ->
                profileViewModel.addSavingsDeposit(goal, amount, note)
                onShowMessage("Saved ${ShiftlyFormatters.formatCurrency(profileState.currencySymbol, amount)}")
            },
            onDeleteDeposit = { dep ->
                profileViewModel.deleteSavingsDeposit(dep)
                onShowMessage("Savings deposit removed")
            },
            onDeleteGoal = { goal ->
                profileViewModel.deleteSavingsGoal(goal)
                onShowMessage("Savings goal deleted")
            }
        )
    }

    if (showAddSavingsGoalSheet) {
        AddSavingsGoalSheet(
            currencySymbol = profileState.currencySymbol,
            onDismiss = { showAddSavingsGoalSheet = false },
            onCreateGoal = { title, targetAmount, targetDateEpochDay ->
                profileViewModel.createSavingsGoal(title, targetAmount, targetDateEpochDay)
                showAddSavingsGoalSheet = false
                onShowMessage("Savings goal \"$title\" added")
            }
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
    onOpenAllowance: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val currentMonth = remember { YearMonth.now() }
    val monthStartEpoch = remember(currentMonth) { currentMonth.atDay(1).toEpochDay() }
    val monthEndEpoch = remember(currentMonth) { currentMonth.atEndOfMonth().toEpochDay() }

    val shiftSummary = remember(shifts, monthStartEpoch, monthEndEpoch) {
        var count = 0
        var minutes = 0L
        var earnings = 0.0
        for (shift in shifts) {
            if (shift.dateEpochDay in monthStartEpoch..monthEndEpoch) {
                count++
                minutes += shift.durationMinutes
                earnings += shift.totalEarned
            }
        }
        Triple(count, minutes / 60.0, earnings)
    }
    val monthShiftCount = shiftSummary.first
    val monthShiftHours = shiftSummary.second
    val monthShiftEarnings = shiftSummary.third

    val expenseSummary = remember(expenses, monthStartEpoch, monthEndEpoch) {
        var count = 0
        var spent = 0.0
        for (expense in expenses) {
            if (expense.dateEpochDay in monthStartEpoch..monthEndEpoch) {
                count++
                spent += expense.amount
            }
        }
        Pair(count, spent)
    }
    val monthExpenseCount = expenseSummary.first
    val monthSpent = expenseSummary.second

    val splitSummary = remember(splitBills) {
        var pendingAmt = 0.0
        var pendingBills = 0
        for (item in splitBills) {
            var hasUnpaidFriend = false
            for (p in item.participants) {
                if (!p.isCurrentUser && !p.hasPaid) {
                    pendingAmt += p.amountOwed
                    hasUnpaidFriend = true
                }
            }
            if (hasUnpaidFriend) pendingBills++
        }
        Pair(pendingAmt, pendingBills)
    }
    val pendingSplitsAmount = splitSummary.first
    val pendingBillsCount = splitSummary.second

    val activeGoal = remember(savingsGoals) {
        savingsGoals.firstOrNull { it.goal.isActive } ?: savingsGoals.firstOrNull()
    }
    val activeGoalSaved = remember(activeGoal) {
        activeGoal?.deposits?.sumOf { it.amount } ?: 0.0
    }
    val enabledRecurringCount = remember(recurringBills) {
        recurringBills.count { it.isEnabled }
    }
    val totalAllowanceAmount = remember(allowanceEntries) {
        allowanceEntries.sumOf { it.amount }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "tools_header", contentType = "header") {
            ShiftlyScreenHeader(
                title = "Tools",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item(key = "tool_duties", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Duties & Shifts",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, monthShiftEarnings),
                    metricSub = "${ShiftlyFormatters.formatHours(monthShiftHours, compact = true)} this month • $monthShiftCount shift(s)",
                    iconRes = R.drawable.ic_lucide_clock,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    metricColor = colors.mintInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenDuties
                )
            }
        }

        item(key = "tool_expense", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Expenses & Receipts",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, monthSpent),
                    metricSub = if (monthlyBudget > 0.0) {
                        "of ${ShiftlyFormatters.formatCurrency(currencySymbol, monthlyBudget)} monthly budget"
                    } else {
                        "$monthExpenseCount expense(s) this month"
                    },
                    iconRes = R.drawable.ic_lucide_receipt_text,
                    iconBg = colors.coralContainer,
                    iconTint = colors.coralInk,
                    metricColor = colors.coralInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenExpense
                )
            }
        }

        item(key = "tool_split_bill", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Split Bill",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, pendingSplitsAmount),
                    metricSub = if (pendingBillsCount > 0) {
                        "$pendingBillsCount active bill(s) awaiting payment"
                    } else {
                        "${splitBills.size} bill(s) • all settled"
                    },
                    iconRes = R.drawable.ic_lucide_split,
                    iconBg = colors.azureContainer,
                    iconTint = colors.azureInk,
                    metricColor = colors.azureInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenSplitBill
                )
            }
        }

        item(key = "tool_budgets", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Monthly & Category Budgets",
                    metric = if (monthlyBudget > 0.0) ShiftlyFormatters.formatCurrency(currencySymbol, monthlyBudget) else "Set Budget",
                    metricSub = "Monthly spend limit & category targets",
                    iconRes = R.drawable.ic_lucide_wallet,
                    iconBg = colors.coralContainer,
                    iconTint = colors.coralInk,
                    metricColor = colors.coralInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenBudgets
                )
            }
        }

        item(key = "tool_savings", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Savings Goals",
                    metric = activeGoal?.goal?.title ?: "Add Goal",
                    metricSub = if (activeGoal != null) {
                        "${ShiftlyFormatters.formatCurrency(currencySymbol, activeGoalSaved)} of ${ShiftlyFormatters.formatCurrency(currencySymbol, activeGoal.goal.targetAmount)}"
                    } else {
                        "Track savings progress"
                    },
                    iconRes = R.drawable.ic_lucide_piggy_bank,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    metricColor = colors.mintInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenSavingsGoals
                )
            }
        }

        item(key = "tool_recurring", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Recurring Bills",
                    metric = "$enabledRecurringCount active",
                    metricSub = "${recurringBills.size} scheduled bill(s)",
                    iconRes = R.drawable.ic_lucide_repeat,
                    iconBg = colors.azureContainer,
                    iconTint = colors.azureInk,
                    metricColor = colors.azureInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenRecurringBills
                )
            }
        }

        item(key = "tool_allowance", contentType = "tool_card") {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Allowance",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, totalAllowanceAmount),
                    metricSub = "$cutoffScheduleLabel • ${allowanceEntries.size} entry(s)",
                    iconRes = R.drawable.ic_lucide_wallet,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    metricColor = colors.mintInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenAllowance
                )
            }
        }
    }
}

@Composable
private fun ToolHubCard(
    title: String,
    metric: String,
    metricSub: String,
    @DrawableRes iconRes: Int,
    iconBg: Color,
    iconTint: Color,
    metricColor: Color,
    quickActionLabel: String?,
    onQuickAction: (() -> Unit)?,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    ShiftlyGlassCard(
        cornerRadius = 24.dp,
        contentPadding = PaddingValues(18.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape)
                        .background(iconBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_right),
                    contentDescription = "Open $title",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = metric,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontSize = 22.sp,
                        lineHeight = 28.sp,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = metricColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 180.dp)
                )

                Text(
                    text = metricSub,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium
                    ).tabular(),
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )

                if (quickActionLabel != null && onQuickAction != null) {
                    Spacer(modifier = Modifier.width(4.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .heightIn(min = 40.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onQuickAction)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_plus),
                                contentDescription = null,
                                tint = colors.azureAccent,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = quickActionLabel,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureAccent
                            )
                        }
                    }
                }
            }
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
