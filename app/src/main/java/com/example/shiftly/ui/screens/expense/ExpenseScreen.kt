package com.example.shiftly.ui.screens.expense

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyMonthFilterBar
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.shiftlySheetBodyBand
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.components.shiftlySheetTopBand
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun ExpenseScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var showAddSheet by remember { mutableStateOf(false) }
    var viewingReceiptExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var pendingDeleteExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    ExpenseScreenContent(
        state = state,
        onBack = onBack,
        onOpenAddSheet = { showAddSheet = true },
        onPreviousMonth = { viewModel.shiftMonth(-1L) },
        onNextMonth = { viewModel.shiftMonth(1L) },
        onToggleAllMonths = { viewModel.toggleAllMonths() },
        onSelectCategory = { viewModel.selectCategory(it) },
        onResetFilters = { viewModel.resetFilters() },
        onViewReceipt = { viewingReceiptExpense = it },
        onDeleteExpense = { pendingDeleteExpense = it },
    )

    if (showAddSheet) {
        AddExpenseBottomSheet(
            currencySymbol = state.currencySymbol,
            availableCategories = state.availableCategories,
            onDismiss = { showAddSheet = false },
            onSaveExpense = { title, amount, category, date, receiptPath ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addExpense(title, amount, category, date, receiptPath)
                onShowMessage("Expense saved")
            }
        )
    }

    pendingDeleteExpense?.let { expenseToDelete ->
        ShiftlyConfirmDeleteDialog(
            title = "Delete Expense?",
            message = "Remove \"${expenseToDelete.title}\"?",
            onDismiss = { pendingDeleteExpense = null },
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteExpense(expenseToDelete)
                pendingDeleteExpense = null
                onShowMessage("Expense deleted")
            }
        )
    }

    viewingReceiptExpense?.let { expense ->
        ReceiptViewerDialog(
            expense = expense,
            currencySymbol = state.currencySymbol,
            onDismiss = { viewingReceiptExpense = null }
        )
    }
}

@Composable
fun ExpenseScreenContent(
    state: ExpenseUiState,
    onOpenAddSheet: () -> Unit,
    onSelectCategory: (String) -> Unit,
    onViewReceipt: (ExpenseEntity) -> Unit,
    onDeleteExpense: (ExpenseEntity) -> Unit,
    onBack: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onToggleAllMonths: () -> Unit = {},
    onResetFilters: () -> Unit = { onSelectCategory("All") },
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val itemDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val listTitle = remember(state.selectedCategory) {
        if (state.selectedCategory == "All") {
            "Expense History"
        } else {
            "${state.selectedCategory} Expenses"
        }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.Top
    ) {
        item(key = "expense_header", contentType = "header") {
            ShiftlyScreenHeader(
                title = "Expenses",
                subtitle = null,
                onBackClick = onBack,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                trailing = {
                    ShiftlySecondaryTextButton(
                        text = "Add",
                        onClick = onOpenAddSheet,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_plus),
                                contentDescription = null,
                                tint = colors.azureAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
            )
        }

        item(key = "month_filter", contentType = "month_filter") {
            ShiftlyMonthFilterBar(
                selectedMonth = state.selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToggleAllMonths = onToggleAllMonths,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            )
        }

        item(key = "expense_summary", contentType = "summary") {
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                ExpenseSummaryCard(
                    monthlyTotal = state.monthlyTotal,
                    allTimeTotal = state.allTimeTotal,
                    monthlyBudget = state.monthlyBudget,
                    totalReceiptCount = state.totalReceiptCount,
                    categoryBreakdown = state.categoryBreakdown,
                    currencySymbol = state.currencySymbol,
                    selectedMonth = state.selectedMonth
                )
            }
        }

        item(key = "category_filter", contentType = "category_filter") {
            Box(modifier = Modifier.padding(bottom = 14.dp)) {
                ExpenseCategoryFilterRow(
                    categories = state.filterCategories,
                    selectedCategory = state.selectedCategory,
                    onSelectCategory = onSelectCategory
                )
            }
        }

        item(key = "sheet_header", contentType = "sheet_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .shiftlySheetTopBand(colors.surface)
                    .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                ShiftlySheetDragHandle()

                ShiftlySectionHeader(
                    title = listTitle,
                    count = state.filteredExpenses.size,
                    onLightSurface = true
                )
            }
        }

        if (state.filteredExpenses.isEmpty()) {
            item(key = "sheet_empty", contentType = "sheet_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 530.dp)
                        .shiftlySheetBodyBand(colors.surface)
                        .padding(start = 20.dp, end = 20.dp, bottom = 104.dp)
                ) {
                    ExpenseEmptyCard(
                        selectedCategory = state.selectedCategory,
                        selectedMonth = state.selectedMonth,
                        onAddExpense = onOpenAddSheet,
                        onShowAll = onResetFilters
                    )
                }
            }
        } else {
            itemsIndexed(
                items = state.filteredExpenses,
                key = { _, expense -> "expense_${expense.id}" },
                contentType = { _, _ -> "expense_item" }
            ) { idx, expense ->
                val rowShape = when {
                    state.filteredExpenses.size == 1 -> RoundedCornerShape(18.dp)
                    idx == 0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    idx == state.filteredExpenses.lastIndex -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                    else -> RectangleShape
                }
                Box(
                    modifier = Modifier
                        .animateItem(
                            fadeInSpec = spring(dampingRatio = 0.8f),
                            placementSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
                            fadeOutSpec = spring(dampingRatio = 0.8f)
                        )
                        .fillMaxWidth()
                        .shiftlySheetBodyBand(colors.surface)
                        .padding(horizontal = 20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(colors.cardElevated)
                    ) {
                        ExpenseItemCard(
                            expense = expense,
                            currencySymbol = state.currencySymbol,
                            onViewReceipt = onViewReceipt,
                            onDelete = { onDeleteExpense(expense) },
                            dateFormatter = itemDateFormatter,
                            containerColor = Color.Transparent,
                            shape = RectangleShape
                        )
                        if (idx < state.filteredExpenses.lastIndex) {
                            ShiftlyHairlineDivider(startInset = 64.dp)
                        }
                    }
                }
            }

            item(key = "sheet_footer", contentType = "sheet_footer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shiftlySheetBodyBand(colors.surface)
                        .height(if (state.filteredExpenses.size <= 2) 280.dp else 104.dp)
                )
            }
        }
    }
}

// =============================================================================
// High-Fidelity Previews
// =============================================================================

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Expenses List")
@Composable
private fun ExpenseListPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val sampleExpenses = listOf(
        ExpenseEntity(
            id = 1L,
            title = "Split: Group Dinner",
            amount = 300.0,
            category = "Split Bill",
            dateEpochDay = today.toEpochDay(),
            linkedSplitBillId = 10L
        ),
        ExpenseEntity(
            id = 2L,
            title = "Tapsilog & Iced Coffee",
            amount = 145.0,
            category = "Food",
            dateEpochDay = today.toEpochDay()
        ),
        ExpenseEntity(
            id = 3L,
            title = "Jeepney & LRT Fare",
            amount = 58.0,
            category = "Transport",
            dateEpochDay = today.toEpochDay()
        ),
        ExpenseEntity(
            id = 4L,
            title = "Thesis Binding & Prints",
            amount = 180.0,
            category = "Supplies",
            dateEpochDay = today.minusDays(1).toEpochDay()
        )
    )
    val state = ExpenseUiState(
        currencySymbol = "₱",
        monthlyBudget = 3000.0,
        selectedMonth = YearMonth.of(2026, 5),
        selectedCategory = "All",
        monthlyTotal = 683.0,
        allTimeTotal = 1420.0,
        categoryBreakdown = listOf(
            CategorySpendSummary("Split Bill", 300.0, 0.44f),
            CategorySpendSummary("Supplies", 180.0, 0.26f),
            CategorySpendSummary("Food", 145.0, 0.21f, monthlyLimit = 1200.0),
            CategorySpendSummary("Transport", 58.0, 0.09f, monthlyLimit = 500.0)
        ),
        filteredExpenses = sampleExpenses,
        totalReceiptCount = 1
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ExpenseScreenContent(
                    state = state,
                    onOpenAddSheet = {},
                    onSelectCategory = {},
                    onViewReceipt = {},
                    onDeleteExpense = {}
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Tools },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Add Expense Sheet")
@Composable
private fun AddExpenseSheetPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val emptyState = ExpenseUiState(
        currencySymbol = "₱",
        selectedMonth = YearMonth.of(2026, 5),
        selectedCategory = "All",
        monthlyTotal = 683.0,
        allTimeTotal = 1420.0
    )

    ShiftlyTheme {
        val colors = ShiftlyColors.current
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ExpenseScreenContent(
                    state = emptyState,
                    onOpenAddSheet = {},
                    onSelectCategory = {},
                    onViewReceipt = {},
                    onDeleteExpense = {}
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(shiftlySheetScrimColor())
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = colors.surface,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column {
                            ShiftlySheetDragHandle()
                            Spacer(modifier = Modifier.height(6.dp))
                            AddExpenseSheetContent(
                                currencySymbol = "₱",
                                savedReceiptPath = null,
                                onReceiptChanged = {},
                                onDismiss = {},
                                onSaveExpense = { _, _, _, _, _ -> },
                                initialAmount = "145",
                                initialTitle = "Tapsilog & Iced Coffee",
                                initialCategory = "Food",
                                initialDate = today
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Expenses Empty")
@Composable
private fun ExpenseEmptyPreview() {
    val state = ExpenseUiState(
        currencySymbol = "₱",
        selectedMonth = YearMonth.of(2026, 5)
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                ExpenseScreenContent(
                    state = state,
                    onOpenAddSheet = {},
                    onSelectCategory = {},
                    onViewReceipt = {},
                    onDeleteExpense = {}
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Tools },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
