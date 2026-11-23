package com.example.shiftly.ui.screens

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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
import com.example.shiftly.ui.components.ShiftlyGroupLabel
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlyToolSheetContainer
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.icons.isSystemAnimationEnabled
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.screens.loans.InstallmentLoansBottomSheet
import com.example.shiftly.ui.screens.profile.BudgetsEditorSheetContent
import com.example.shiftly.ui.screens.profile.ManageCategoriesSheetContent
import com.example.shiftly.ui.screens.profile.ShiftlySheetActionState
import com.example.shiftly.ui.screens.savings.AddSavingsGoalSheet
import com.example.shiftly.ui.screens.savings.SavingsGoalDetailSheet
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.LocalDate

private enum class ActiveToolTutorial {
    BUDGETS,
    CATEGORIES,
    PAYCHECK,
    IOU,
    INSTALLMENT_LOANS,
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
    var showInstallmentLoansSheet by remember { mutableStateOf(false) }
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
    val onOpenInstallmentLoans = remember { { showInstallmentLoansSheet = true } }
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
        onOpenInstallmentLoans = onOpenInstallmentLoans,
        onOpenActivityCalendar = onOpenActivityCalendar
    )

    if (showBudgetsSheet) {
        val budgetsActionState = remember { ShiftlySheetActionState() }
        ShiftlyToolSheetContainer(
            title = "Monthly Limits",
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
            title = "Personal IOUs",
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

    if (showInstallmentLoansSheet) {
        InstallmentLoansBottomSheet(
            currencySymbol = state.currencySymbol,
            loans = state.installmentLoans,
            onAddOrUpdateLoan = { loan ->
                viewModel.addOrUpdateInstallmentLoan(loan)
                onShowMessage("Loan contract saved")
            },
            onMarkMonthPaid = { loan, amountPaid, datePaidMillis ->
                viewModel.markLoanMonthAsPaid(
                    loanId = loan.id,
                    amountPaid = amountPaid,
                    datePaidMillis = datePaidMillis
                )
                onShowMessage("Payment recorded")
            },
            onUpdateLoanPayment = { payment, newAmount, newTime ->
                viewModel.updateLoanPayment(payment, newAmount, newTime)
                onShowMessage("Payment updated")
            },
            onDeleteLoanPayment = { loan, payment ->
                viewModel.deleteLoanPayment(loan, payment)
                onShowMessage("Payment record deleted")
            },
            onDeleteLoan = { loan ->
                viewModel.deleteInstallmentLoan(loan)
                onShowMessage("Loan contract deleted")
            },
            onHelpClick = { activeTutorial = ActiveToolTutorial.INSTALLMENT_LOANS },
            onDismiss = { showInstallmentLoansSheet = false }
        )
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
                subtitle = null,
                steps = listOf(
                    "Set monthly limit" to "Add an overall monthly spending cap.",
                    "Assign category caps" to "Set optional limits per spending category.",
                    "Manage categories" to "Enable or add custom expense categories."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.CATEGORIES -> {
            ShiftlyScreenTutorialSheet(
                title = "Manage Categories Guide",
                subtitle = null,
                steps = listOf(
                    "Toggle categories" to "Hide categories you rarely use.",
                    "Add custom categories" to "Create categories tailored to your routine.",
                    "Save changes" to "Tap Save to update your category list."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.PAYCHECK -> {
            ShiftlyScreenTutorialSheet(
                title = "Paycheck Estimator Guide",
                subtitle = null,
                steps = listOf(
                    "Gross shift pay" to "Pulls current month shift earnings automatically.",
                    "Adjust deductions" to "Set SSS, PhilHealth, Pag-IBIG, or tax amounts.",
                    "Net take-home" to "Preview estimated net pay after deductions."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.IOU -> {
            ShiftlyScreenTutorialSheet(
                title = "Personal IOUs Guide",
                subtitle = null,
                steps = listOf(
                    "Add borrow or lend" to "Enter person, amount, and who owes whom.",
                    "Mark as settled" to "Settle paid entries to update your net total.",
                    "Stored offline" to "All IOU records stay privately on your device."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.INSTALLMENT_LOANS -> {
            ShiftlyScreenTutorialSheet(
                title = "Installment Loans Guide",
                subtitle = null,
                steps = listOf(
                    "Add contract & rate" to "Enter item price, downpayment, Monthly % or Total % interest, terms, and due day.",
                    "Record Payment" to "Open a loan card to log custom Amount Paid, Date Paid, and Time Paid.",
                    "Due status & balance" to "Remaining balance updates automatically with due soon and overdue badges."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        ActiveToolTutorial.CALENDAR -> {
            ShiftlyScreenTutorialSheet(
                title = "Activity Calendar Guide",
                subtitle = null,
                steps = listOf(
                    "Colored dots" to "Green marks shifts; red marks recorded expenses.",
                    "Tap any date" to "Inspect shifts, hours, and expenses for that day.",
                    "Monthly overview" to "Compare work days and spending days side by side."
                ),
                onDismiss = { activeTutorial = null }
            )
        }

        null -> Unit
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
    onOpenAllowance: () -> Unit = {},
    onOpenPaycheckEstimator: () -> Unit = {},
    onOpenIouTracker: () -> Unit = {},
    onOpenInstallmentLoans: () -> Unit = {},
    onOpenActivityCalendar: () -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val currencySymbol = state.currencySymbol
    val context = LocalContext.current
    val animationsEnabled = remember(context) { isSystemAnimationEnabled(context) }
    var activeSpotlight by remember { mutableStateOf<ToolSpotlightRequest?>(null) }
    val spotlightProgress = remember { Animatable(0f) }
    var containerOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var launchSequence by remember { mutableLongStateOf(0L) }

    fun launchWithSpotlight(
        rowKey: String,
        @DrawableRes iconRes: Int,
        bounds: Rect,
        isBottomSheet: Boolean,
        action: () -> Unit
    ) {
        if (activeSpotlight != null) return
        if (!animationsEnabled || bounds.isEmpty || containerSize.width <= 0) {
            action()
            return
        }
        activeSpotlight = ToolSpotlightRequest(
            id = ++launchSequence,
            rowKey = rowKey,
            iconRes = iconRes,
            sourceBoundsInRoot = bounds,
            isBottomSheet = isBottomSheet,
            onLaunch = action
        )
    }

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

    val p = spotlightProgress.value.coerceIn(0f, 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { coords ->
                containerOriginInRoot = coords.positionInRoot()
                containerSize = coords.size
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    if (p > 0f) {
                        val s = lerp(1f, 0.97f, p)
                        scaleX = s
                        scaleY = s
                    }
                }
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && p > 0.01f) {
                        Modifier.blur(
                            radius = (14f * p).dp,
                            edgeTreatment = BlurredEdgeTreatment.Rectangle
                        )
                    } else {
                        Modifier
                    }
                )
        ) {
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
                        ShiftlyGroupLabel(text = "Trackers")
                        ShiftlyGlassCard(
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ToolGroupedRow(
                                    title = "Duties",
                                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.monthShiftEarnings),
                                    iconRes = R.drawable.ic_lucide_clock,
                                    metricColor = colors.mintInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "duties",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("duties", R.drawable.ic_lucide_clock, bounds, isBottomSheet = false, onOpenDuties)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Expenses",
                                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.monthSpent),
                                    iconRes = R.drawable.ic_lucide_receipt_text,
                                    metricColor = colors.coralInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "expenses",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("expenses", R.drawable.ic_lucide_receipt_text, bounds, isBottomSheet = false, onOpenExpense)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Split Bill",
                                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.pendingSplitsAmount),
                                    iconRes = R.drawable.ic_lucide_split,
                                    metricColor = colors.azureInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "split_bill",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("split_bill", R.drawable.ic_lucide_split, bounds, isBottomSheet = false, onOpenSplitBill)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "IOUs",
                                    metric = state.iouSummaryText,
                                    iconRes = R.drawable.ic_lucide_circle_dollar_sign,
                                    metricColor = if (state.iouSummaryText.contains("you owe", ignoreCase = true)) colors.coralInk else colors.mintInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "ious",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("ious", R.drawable.ic_lucide_circle_dollar_sign, bounds, isBottomSheet = true, onOpenIouTracker)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Loans",
                                    metric = state.installmentLoanSummaryText,
                                    iconRes = R.drawable.ic_lucide_file_text,
                                    metricColor = when {
                                        state.overdueLoanCount > 0 -> colors.coralInk
                                        state.dueSoonLoanCount > 0 -> colors.iosOrange
                                        state.installmentLoans.isNotEmpty() && state.activeInstallmentLoanCount == 0 -> colors.mintInk
                                        state.activeInstallmentLoanCount > 0 -> colors.textPrimary
                                        else -> colors.textSecondary
                                    },
                                    isSpotlightHidden = activeSpotlight?.rowKey == "loans",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("loans", R.drawable.ic_lucide_file_text, bounds, isBottomSheet = true, onOpenInstallmentLoans)
                                    }
                                )
                            }
                        }
                    }
                }

                item(key = "tools_planning_group", contentType = "tool_group") {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShiftlyGroupLabel(text = "Planning")
                        ShiftlyGlassCard(
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                ToolGroupedRow(
                                    title = "Budgets",
                                    metric = if (state.monthlyBudget > 0.0) ShiftlyFormatters.formatCurrency(currencySymbol, state.monthlyBudget) else "Set Budget",
                                    iconRes = R.drawable.ic_lucide_tags,
                                    metricColor = colors.textPrimary,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "budgets",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("budgets", R.drawable.ic_lucide_tags, bounds, isBottomSheet = true, onOpenBudgets)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Goals",
                                    metric = state.activeGoalTitle ?: "Add Goal",
                                    iconRes = R.drawable.ic_lucide_piggy_bank,
                                    metricColor = colors.mintInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "goals",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("goals", R.drawable.ic_lucide_piggy_bank, bounds, isBottomSheet = true, onOpenSavingsGoals)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Recurring",
                                    metric = "${state.enabledRecurringCount} active",
                                    iconRes = R.drawable.ic_lucide_repeat,
                                    metricColor = colors.textPrimary,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "recurring",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("recurring", R.drawable.ic_lucide_repeat, bounds, isBottomSheet = false, onOpenRecurringBills)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Allowance",
                                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.totalAllowanceAmount),
                                    iconRes = R.drawable.ic_lucide_wallet,
                                    metricColor = colors.azureInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "allowance",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("allowance", R.drawable.ic_lucide_wallet, bounds, isBottomSheet = false, onOpenAllowance)
                                    }
                                )
                                ShiftlyHairlineDivider(startInset = 62.dp)
                                ToolGroupedRow(
                                    title = "Paycheck",
                                    metric = ShiftlyFormatters.formatCurrency(currencySymbol, state.netPayTakeHome),
                                    iconRes = R.drawable.ic_lucide_banknote,
                                    metricColor = colors.mintInk,
                                    isSpotlightHidden = activeSpotlight?.rowKey == "paycheck",
                                    onClickWithBounds = { bounds ->
                                        launchWithSpotlight("paycheck", R.drawable.ic_lucide_banknote, bounds, isBottomSheet = true, onOpenPaycheckEstimator)
                                    }
                                )
                            }
                        }
                    }
                }

                item(key = "tools_calendar_group", contentType = "tool_group") {
                    Column(
                        modifier = Modifier.padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ShiftlyGroupLabel(text = "Calendar")
                        ShiftlyGlassCard(
                            cornerRadius = 16.dp,
                            contentPadding = PaddingValues(0.dp)
                        ) {
                            ToolGroupedRow(
                                title = "Calendar",
                                metric = "Monthly View",
                                iconRes = R.drawable.ic_lucide_calendar,
                                metricColor = colors.textSecondary,
                                isSpotlightHidden = activeSpotlight?.rowKey == "calendar",
                                onClickWithBounds = { bounds ->
                                    launchWithSpotlight("calendar", R.drawable.ic_lucide_calendar, bounds, isBottomSheet = true, onOpenActivityCalendar)
                                }
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

        ToolsSpotlightOverlay(
            request = activeSpotlight,
            flightProgress = spotlightProgress,
            containerOriginInRoot = containerOriginInRoot,
            containerSize = containerSize,
            onFinished = { activeSpotlight = null }
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
    onOpenBudgets: () -> Unit = {},
    onOpenSavingsGoals: () -> Unit = {},
    onOpenRecurringBills: () -> Unit = {},
    onOpenAllowance: () -> Unit = {},
    onOpenPaycheckEstimator: () -> Unit = {},
    onOpenIouTracker: () -> Unit = {},
    onOpenInstallmentLoans: () -> Unit = {},
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
        onOpenInstallmentLoans = onOpenInstallmentLoans,
        onOpenActivityCalendar = onOpenActivityCalendar
    )
}

@Composable
private fun ToolGroupedRow(
    title: String,
    metric: String,
    @DrawableRes iconRes: Int,
    metricColor: Color = ShiftlyColors.current.textPrimary,
    isSpotlightHidden: Boolean = false,
    onClickWithBounds: (Rect) -> Unit
) {
    val colors = ShiftlyColors.current
    var iconBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .shiftlyPressClickable(onClick = { onClickWithBounds(iconBoundsInRoot) })
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(colors.iconNeutralContainer),
            contentAlignment = Alignment.Center
        ) {
            ShiftlyAnimatedIcon(
                iconRes = iconRes,
                contentDescription = null,
                tint = colors.iconNeutral,
                modifier = Modifier
                    .size(18.dp)
                    .onGloballyPositioned { iconBoundsInRoot = it.boundsInRoot() }
                    .graphicsLayer { alpha = if (isSpotlightHidden) 0f else 1f }
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = (-0.25).sp
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
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
            ShiftlyAnimatedIcon(
                iconRes = R.drawable.ic_lucide_chevron_right,
                contentDescription = "Open $title",
                tint = colors.textSecondary,
                modifier = Modifier.size(17.dp)
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
                    onOpenSplitBill = {}
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
