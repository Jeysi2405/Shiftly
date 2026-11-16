package com.example.shiftly.ui.screens.expense

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.DeepBlueContainer
import com.example.shiftly.ui.theme.ElectricBlue
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SlateSurfaceVariant
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary

@Composable
fun ExpenseScreen(
    repository: ShiftlyRepository,
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: ExpenseViewModel = viewModel(factory = ExpenseViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var showAddSheet by remember { mutableStateOf(false) }
    var viewingReceiptExpense by remember { mutableStateOf<ExpenseEntity?>(null) }
    var pendingDeleteExpense by remember { mutableStateOf<ExpenseEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header + Quick Add Button
        item {
            ShiftlyScreenHeader(
                title = "Expenses",
                subtitle = "Log expenses & attach proof receipts",
                trailing = {
                    Button(
                        onClick = { showAddSheet = true },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        // 1. Visual Spending & Category Breakdown Card
        item {
            ExpenseSummaryCard(
                monthlyTotal = state.monthlyTotal,
                allTimeTotal = state.allTimeTotal,
                totalReceiptCount = state.totalReceiptCount,
                categoryBreakdown = state.categoryBreakdown,
                currencySymbol = state.currencySymbol
            )
        }

        // 2. Category Filter Chips
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseViewModel.FILTER_CATEGORIES.forEach { category ->
                    val isSelected = state.selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.selectCategory(category) },
                        label = {
                            Text(
                                text = category,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurface,
                            labelColor = TextSecondary,
                            selectedContainerColor = DeepBlueContainer,
                            selectedLabelColor = ElectricBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = BorderSubtle,
                            selectedBorderColor = ElectricBlue
                        )
                    )
                }
            }
        }

        // 3. Expense Log List
        if (state.filteredExpenses.isEmpty()) {
            item {
                ShiftlyEmptyState(
                    icon = Icons.AutoMirrored.Rounded.ReceiptLong,
                    title = if (state.selectedCategory == "All") {
                        "No expenses logged yet"
                    } else {
                        "No ${state.selectedCategory} expenses"
                    },
                    subtitle = if (state.selectedCategory == "All") {
                        "Tap + Add above to log an expense with an optional receipt photo."
                    } else {
                        "Try another category filter or add a new expense."
                    }
                )
            }
        } else {
            items(
                items = state.filteredExpenses,
                key = { it.id }
            ) { expense ->
                ExpenseItemCard(
                    expense = expense,
                    currencySymbol = state.currencySymbol,
                    onViewReceipt = { viewingReceiptExpense = it },
                    onDelete = { pendingDeleteExpense = expense }
                )
            }
        }
    }

    if (showAddSheet) {
        AddExpenseBottomSheet(
            currencySymbol = state.currencySymbol,
            onDismiss = { showAddSheet = false },
            onSaveExpense = { title, amount, category, date, receiptPath ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addExpense(title, amount, category, date, receiptPath)
                onShowMessage("Expense logged")
            }
        )
    }

    pendingDeleteExpense?.let { expenseToDelete ->
        ShiftlyConfirmDeleteDialog(
            title = "Delete Expense?",
            message = "This will permanently remove \"${expenseToDelete.title}\" and any attached receipt.",
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