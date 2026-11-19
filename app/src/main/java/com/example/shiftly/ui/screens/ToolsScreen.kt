package com.example.shiftly.ui.screens

import androidx.annotation.DrawableRes
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
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
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.profile.BudgetsEditorSheetContent
import com.example.shiftly.ui.screens.profile.ManageCategoriesSheetContent
import com.example.shiftly.ui.screens.profile.SettingsSheetContainer
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate

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

    ToolsScreenContent(
        state = state,
        onOpenDuties = onOpenDuties,
        onOpenExpense = onOpenExpense,
        onOpenSplitBill = onOpenSplitBill,
        onOpenBudgets = onOpenBudgets,
        onOpenSavingsGoals = onOpenSavingsGoals,
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
                currencySymbol = state.currencySymbol,
                currentMonthlyBudget = state.monthlyBudget,
                currentCategoryBudgets = state.settings.categoryBudgets,
                managedCategories = state.settings.managedCategories,
                onManageCategories = { showManageCategoriesSheet = true },
                onDismiss = { showBudgetsSheet = false },
                onSave = { monthly, categoryList ->
                    viewModel.updateBudgets(monthly, categoryList)
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
                currentCategories = state.settings.managedCategories,
                onSave = { updated ->
                    viewModel.updateManagedCategories(updated)
                    showManageCategoriesSheet = false
                    onShowMessage("Categories updated")
                }
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
}

@Composable
fun ToolsScreenContent(
    state: ToolsUiState,
    onOpenDuties: () -> Unit,
    onOpenExpense: () -> Unit,
    onOpenSplitBill: () -> Unit,
    onOpenBudgets: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenRecurringBills: () -> Unit = {},
    onOpenAllowance: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val currencySymbol = state.currencySymbol

    LazyColumn(
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
                modifier = Modifier.padding(horizontal = 20.dp)
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
                        metricSub = "${ShiftlyFormatters.formatHours(state.monthShiftHours, compact = true)} this month • ${state.monthShiftCount} shift(s)",
                        iconRes = R.drawable.ic_lucide_clock,
                        iconTileColor = colors.mintBright,
                        metricColor = colors.mintInk,
                        onClick = onOpenDuties
                    )
                    ShiftlyHairlineDivider(startInset = 66.dp)
                    ToolGroupedRow(
                        title = "Expenses & Receipts",
                        metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.monthSpent),
                        metricSub = if (state.monthlyBudget > 0.0) {
                            "of ${ShiftlyFormatters.formatCurrency(currencySymbol, state.monthlyBudget)} monthly budget"
                        } else {
                            "${state.monthExpenseCount} expense(s) this month"
                        },
                        iconRes = R.drawable.ic_lucide_receipt_text,
                        iconTileColor = colors.coralBright,
                        metricColor = colors.coralInk,
                        onClick = onOpenExpense
                    )
                    ShiftlyHairlineDivider(startInset = 66.dp)
                    ToolGroupedRow(
                        title = "Split Bill",
                        metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.pendingSplitsAmount),
                        metricSub = if (state.pendingBillsCount > 0) {
                            "${state.pendingBillsCount} active bill(s) awaiting payment"
                        } else {
                            "${state.totalSplitBillsCount} bill(s) • all settled"
                        },
                        iconRes = R.drawable.ic_lucide_split,
                        iconTileColor = colors.azurePrimary,
                        metricColor = colors.azureInk,
                        onClick = onOpenSplitBill
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
                        metricSub = "Monthly spend limit & category targets",
                        iconRes = R.drawable.ic_lucide_wallet,
                        iconTileColor = colors.iosOrange,
                        metricColor = colors.textPrimary,
                        onClick = onOpenBudgets
                    )
                    ShiftlyHairlineDivider(startInset = 66.dp)
                    ToolGroupedRow(
                        title = "Savings Goals",
                        metric = state.activeGoalTitle ?: "Add Goal",
                        metricSub = if (state.activeGoalTitle != null) {
                            "${ShiftlyFormatters.formatCurrency(currencySymbol, state.activeGoalSaved)} of ${ShiftlyFormatters.formatCurrency(currencySymbol, state.activeGoalTarget)}"
                        } else {
                            "Track savings progress"
                        },
                        iconRes = R.drawable.ic_lucide_piggy_bank,
                        iconTileColor = colors.mintBright,
                        metricColor = colors.mintInk,
                        onClick = onOpenSavingsGoals
                    )
                    ShiftlyHairlineDivider(startInset = 66.dp)
                    ToolGroupedRow(
                        title = "Recurring Bills",
                        metric = "${state.enabledRecurringCount} active",
                        metricSub = "${state.totalRecurringCount} scheduled bill(s)",
                        iconRes = R.drawable.ic_lucide_repeat,
                        iconTileColor = colors.nightDiffPurple,
                        metricColor = colors.textPrimary,
                        onClick = onOpenRecurringBills
                    )
                    ShiftlyHairlineDivider(startInset = 66.dp)
                    ToolGroupedRow(
                        title = "Allowance",
                        metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.totalAllowanceAmount),
                        metricSub = "${state.cutoffScheduleLabel} • ${state.allowanceEntryCount} entry(s)",
                        iconRes = R.drawable.ic_lucide_wallet,
                        iconTileColor = colors.azurePrimary,
                        metricColor = colors.azureInk,
                        onClick = onOpenAllowance
                    )
                }
            }
        }
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
        onOpenAllowance = onOpenAllowance
    )
}

@Composable
private fun ToolGroupedRow(
    title: String,
    metric: String,
    metricSub: String,
    @DrawableRes iconRes: Int,
    iconTileColor: Color,
    metricColor: Color,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftlyIosIconTile(
            iconRes = iconRes,
            backgroundColor = iconTileColor,
            size = 38.dp,
            cornerRadius = 9.dp,
            iconSize = 19.dp
        )

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = metricSub,
                style = MaterialTheme.typography.bodySmall.tabular(),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = metric,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
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
