package com.example.shiftly.ui.screens.expense

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import java.time.LocalDate

@Composable
fun ExpenseScreen(
    repository: ShiftlyRepository,
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
        onOpenAddSheet = { showAddSheet = true },
        onSelectCategory = { viewModel.selectCategory(it) },
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
    onDeleteExpense: (ExpenseEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShiftlyScreenHeader(
                title = "Expenses",
                subtitle = "Track daily spending & savings",
                modifier = Modifier.padding(horizontal = 20.dp),
                trailing = {
                    Button(
                        onClick = onOpenAddSheet,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzurePrimaryButton,
                            contentColor = PureWhite
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = PureWhite
                        )
                    }
                }
            )
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ExpenseSummaryCard(
                    monthlyTotal = state.monthlyTotal,
                    allTimeTotal = state.allTimeTotal,
                    monthlyBudget = state.monthlyBudget,
                    totalReceiptCount = state.totalReceiptCount,
                    categoryBreakdown = state.categoryBreakdown,
                    currencySymbol = state.currencySymbol
                )
            }
        }

        item {
            ExpenseCategoryFilterRow(
                categories = state.filterCategories,
                selectedCategory = state.selectedCategory,
                onSelectCategory = onSelectCategory
            )
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 620.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = DarkSurface,
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
                                .background(DarkBorder)
                        )
                    }

                    val listTitle = if (state.selectedCategory == "All") {
                        "Expense History"
                    } else {
                        "${state.selectedCategory} Expenses"
                    }

                    ShiftlySectionHeader(
                        title = listTitle,
                        count = state.filteredExpenses.size,
                        onLightSurface = true
                    )

                    if (state.filteredExpenses.isEmpty()) {
                        ExpenseEmptyCard(
                            selectedCategory = state.selectedCategory,
                            onAddExpense = onOpenAddSheet,
                            onShowAll = { onSelectCategory("All") }
                        )
                    } else {
                        state.filteredExpenses.forEach { expense ->
                            ExpenseItemCard(
                                expense = expense,
                                currencySymbol = state.currencySymbol,
                                onViewReceipt = onViewReceipt,
                                onDelete = { onDeleteExpense(expense) }
                            )
                        }
                    }
                }
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
                    isSelected = { it == Screen.Expense },
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
        selectedCategory = "All",
        monthlyTotal = 683.0,
        allTimeTotal = 1420.0
    )

    ShiftlyTheme {
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
                        .background(InkBlack.copy(alpha = 0.65f))
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = DarkSurface,
                        shadowElevation = 16.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column(modifier = Modifier.padding(top = 12.dp)) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(width = 36.dp, height = 4.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(DarkBorder)
                                )
                            }
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
    val state = ExpenseUiState(currencySymbol = "₱")

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
                    isSelected = { it == Screen.Expense },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
