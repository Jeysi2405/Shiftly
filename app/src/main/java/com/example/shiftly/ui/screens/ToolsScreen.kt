package com.example.shiftly.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Savings
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsGoalWithDeposits
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.profile.BudgetsEditorSheetContent
import com.example.shiftly.ui.screens.profile.ProfileViewModel
import com.example.shiftly.ui.screens.profile.RecurringEntriesSheetContent
import com.example.shiftly.ui.screens.profile.SettingsSheetContainer
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate
import java.time.YearMonth

private enum class ToolsSheet {
    BUDGETS,
    RECURRING_BILLS
}

@Composable
fun ToolsScreen(
    repository: ShiftlyRepository,
    onOpenDuties: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onQuickLogShift: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onShowMessage: (String) -> Unit = {}
) {
    val profileViewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(repository))
    val profileState by profileViewModel.uiState.collectAsStateWithLifecycle()

    val shifts by repository.allShifts.collectAsStateWithLifecycle(initialValue = emptyList())
    val expenses by repository.allExpenses.collectAsStateWithLifecycle(initialValue = emptyList())
    val splitBills by repository.allSplitBills.collectAsStateWithLifecycle(initialValue = emptyList())

    var activeSheet by remember { mutableStateOf<ToolsSheet?>(null) }
    var showAddSavingsGoalSheet by remember { mutableStateOf(false) }
    var showSavingsGoalDetailSheet by remember { mutableStateOf(false) }

    ToolsScreenContent(
        currencySymbol = profileState.currencySymbol,
        shifts = shifts,
        expenses = expenses,
        splitBills = splitBills,
        monthlyBudget = profileState.monthlyBudget,
        savingsGoals = profileState.savingsGoals,
        recurringEntries = profileState.settings.recurringEntries,
        onOpenDuties = onOpenDuties,
        onOpenExpense = onOpenExpense,
        onOpenSplitBill = onOpenSplitBill,
        onQuickLogShift = onQuickLogShift,
        onQuickAddExpense = onQuickAddExpense,
        onOpenBudgets = { activeSheet = ToolsSheet.BUDGETS },
        onOpenSavingsGoals = {
            if (profileState.savingsGoals.isEmpty()) {
                showAddSavingsGoalSheet = true
            } else {
                showSavingsGoalDetailSheet = true
            }
        },
        onOpenRecurringBills = { activeSheet = ToolsSheet.RECURRING_BILLS }
    )

    when (activeSheet) {
        ToolsSheet.BUDGETS -> {
            SettingsSheetContainer(
                title = "Monthly & Category Budgets",
                showTopClose = false,
                onDismiss = { activeSheet = null }
            ) {
                BudgetsEditorSheetContent(
                    currencySymbol = profileState.currencySymbol,
                    currentMonthlyBudget = profileState.monthlyBudget,
                    currentCategoryBudgets = profileState.settings.categoryBudgets,
                    managedCategories = profileState.settings.managedCategories,
                    onDismiss = { activeSheet = null },
                    onSave = { monthly, categoryList ->
                        profileViewModel.updateBudgets(monthly, categoryList)
                        activeSheet = null
                        onShowMessage("Budgets saved")
                    }
                )
            }
        }

        ToolsSheet.RECURRING_BILLS -> {
            val visibleExpenseCategories = profileState.settings.managedCategories
                .filter {
                    it.isVisible &&
                        it.name.isNotBlank() &&
                        !it.name.equals("Split Bill", ignoreCase = true)
                }
                .map { it.name.trim() }
                .ifEmpty { ExpenseViewModel.EXPENSE_CATEGORIES.filter { it != "Split Bill" } }

            SettingsSheetContainer(
                title = "Recurring Bills",
                onDismiss = { activeSheet = null }
            ) {
                RecurringEntriesSheetContent(
                    currencySymbol = profileState.currencySymbol,
                    currentEntries = profileState.settings.recurringEntries,
                    availableCategories = visibleExpenseCategories,
                    onSave = { updated ->
                        profileViewModel.updateRecurringEntries(updated) { posted ->
                            activeSheet = null
                            onShowMessage(
                                if (posted > 0) "Saved & posted $posted recurring bill(s)"
                                else "Recurring bills saved"
                            )
                        }
                    }
                )
            }
        }

        null -> {}
    }

    if (showSavingsGoalDetailSheet) {
        val activeGoalId = (profileState.savingsGoals.firstOrNull { it.goal.isActive }
            ?: profileState.savingsGoals.firstOrNull())?.goal?.id
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
    recurringEntries: List<RecurringEntrySetting> = emptyList(),
    onOpenDuties: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onQuickLogShift: () -> Unit,
    onQuickAddExpense: () -> Unit,
    onOpenBudgets: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenRecurringBills: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val currentMonth = YearMonth.now()
    val monthStartEpoch = currentMonth.atDay(1).toEpochDay()
    val monthEndEpoch = currentMonth.atEndOfMonth().toEpochDay()

    val monthShifts = shifts.filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
    val monthShiftHours = monthShifts.sumOf { it.durationMinutes } / 60.0
    val monthShiftEarnings = monthShifts.sumOf { it.totalEarned }

    val monthExpenses = expenses.filter { it.dateEpochDay in monthStartEpoch..monthEndEpoch }
    val monthSpent = monthExpenses.sumOf { it.amount }

    val pendingSplitsAmount = splitBills.sumOf { item ->
        item.participants
            .filter { !it.isCurrentUser && !it.hasPaid }
            .sumOf { it.amountOwed }
    }
    val pendingBillsCount = splitBills.count { item ->
        item.participants.any { !it.isCurrentUser && !it.hasPaid }
    }

    val activeGoal = savingsGoals.firstOrNull { it.goal.isActive } ?: savingsGoals.firstOrNull()
    val enabledRecurringCount = recurringEntries.count { it.isEnabled }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 12.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShiftlyScreenHeader(
                title = "Tools",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Duties & Shifts",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, monthShiftEarnings),
                    metricSub = "${ShiftlyFormatters.formatHours(monthShiftHours, compact = true)} this month • ${monthShifts.size} shift(s)",
                    icon = Icons.Outlined.Schedule,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    metricColor = colors.mintInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenDuties
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Expenses & Receipts",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, monthSpent),
                    metricSub = if (monthlyBudget > 0.0) {
                        "of ${ShiftlyFormatters.formatCurrency(currencySymbol, monthlyBudget)} monthly budget"
                    } else {
                        "${monthExpenses.size} expense(s) this month"
                    },
                    icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                    iconBg = colors.coralContainer,
                    iconTint = colors.coralInk,
                    metricColor = colors.coralInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenExpense
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Split Bill",
                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, pendingSplitsAmount),
                    metricSub = if (pendingBillsCount > 0) {
                        "$pendingBillsCount active bill(s) awaiting payment"
                    } else {
                        "${splitBills.size} bill(s) • all settled"
                    },
                    icon = Icons.AutoMirrored.Outlined.CallSplit,
                    iconBg = colors.azureContainer,
                    iconTint = colors.azureInk,
                    metricColor = colors.azureInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenSplitBill
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Monthly & Category Budgets",
                    metric = if (monthlyBudget > 0.0) ShiftlyFormatters.formatCurrency(currencySymbol, monthlyBudget) else "Set Budget",
                    metricSub = "Monthly spend limit & category targets",
                    icon = Icons.Outlined.AccountBalanceWallet,
                    iconBg = colors.coralContainer,
                    iconTint = colors.coralInk,
                    metricColor = colors.coralInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenBudgets
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Savings Goals",
                    metric = activeGoal?.goal?.title ?: "Add Goal",
                    metricSub = if (activeGoal != null) {
                        "${ShiftlyFormatters.formatCurrency(currencySymbol, activeGoal.deposits.sumOf { it.amount })} of ${ShiftlyFormatters.formatCurrency(currencySymbol, activeGoal.goal.targetAmount)}"
                    } else {
                        "Track savings progress"
                    },
                    icon = Icons.Outlined.Savings,
                    iconBg = colors.mintContainer,
                    iconTint = colors.mintInk,
                    metricColor = colors.mintInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenSavingsGoals
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ToolHubCard(
                    title = "Recurring Bills",
                    metric = "$enabledRecurringCount active",
                    metricSub = "${recurringEntries.size} total recurring bill(s)",
                    icon = Icons.Outlined.Repeat,
                    iconBg = colors.azureContainer,
                    iconTint = colors.azureInk,
                    metricColor = colors.azureInk,
                    quickActionLabel = null,
                    onQuickAction = null,
                    onClick = onOpenRecurringBills
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
    icon: ImageVector,
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
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconTint,
                        modifier = Modifier.size(23.dp)
                    )
                }

                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )

                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Open $title",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = metric,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = metricColor
                        )
                        Text(
                            text = metricSub,
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (quickActionLabel != null && onQuickAction != null) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.border),
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
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null,
                                    tint = colors.azureAccent,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = quickActionLabel,
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = ShiftlySerif,
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
