package com.example.shiftly.ui.screens.splitbill

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.CallSplit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader

@Composable
fun SplitBillScreen(
    repository: ShiftlyRepository,
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: SplitBillViewModel = viewModel(factory = SplitBillViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    var pendingDeleteBill by remember { mutableStateOf<SplitBillEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            ShiftlyScreenHeader(
                title = "Split Bill Calculator",
                subtitle = "Split bills evenly, track who has paid, and log your share"
            )
        }

        // 1. Visual Summary Card (Pending vs. Paid Shares)
        item {
            SplitBillSummaryCard(
                currencySymbol = state.currencySymbol,
                totalPendingAmount = state.totalPendingAmount,
                totalCollectedAmount = state.totalCollectedAmount,
                paidParticipantsCount = state.paidParticipantsCount,
                totalParticipantsCount = state.totalParticipantsCount
            )
        }

        // 2. Interactive Split Bill Calculator Card
        item {
            SplitBillCalculatorCard(
                currencySymbol = state.currencySymbol,
                onSaveSplitBill = { title, totalAmount, personCount, names, alsoLogAsExpense ->
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.createSplitBill(
                        rawTitle = title,
                        totalAmount = totalAmount,
                        personCount = personCount,
                        participantNames = names,
                        alsoLogMyShareAsExpense = alsoLogAsExpense
                    )
                    onShowMessage(
                        if (alsoLogAsExpense) "Split bill saved & share logged to Expenses"
                        else "Split bill saved"
                    )
                }
            )
        }

        // 3. Split Bill List Log Header
        item {
            ShiftlySectionHeader(
                title = "Split Bill Log",
                count = state.totalBillsCount
            )
        }

        if (state.billsWithParticipants.isEmpty()) {
            item {
                ShiftlyEmptyState(
                    icon = Icons.AutoMirrored.Rounded.CallSplit,
                    title = "No split bills logged yet",
                    subtitle = "Enter a bill amount and select how many people are included above."
                )
            }
        } else {
            items(
                items = state.billsWithParticipants,
                key = { it.bill.id }
            ) { billWithParticipants ->
                SplitBillLogItemCard(
                    item = billWithParticipants,
                    currencySymbol = state.currencySymbol,
                    onToggleParticipantPaid = { participant ->
                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        viewModel.toggleParticipantPaid(participant)
                    },
                    onLogAsExpense = { bill ->
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.logBillShareAsExpense(bill)
                        onShowMessage("Your share logged to Expenses")
                    },
                    onDeleteBill = { bill ->
                        pendingDeleteBill = bill
                    }
                )
            }
        }
    }

    pendingDeleteBill?.let { billToDelete ->
        ShiftlyConfirmDeleteDialog(
            title = "Delete Split Bill?",
            message = "This will permanently remove \"${billToDelete.title}\" and its participant payment status.",
            onDismiss = { pendingDeleteBill = null },
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteSplitBill(billToDelete)
                pendingDeleteBill = null
                onShowMessage("Split bill deleted")
            }
        )
    }
}