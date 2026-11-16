package com.example.shiftly.ui.screens.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary

@Composable
fun SplitBillScreen(repository: ShiftlyRepository) {
    val viewModel: SplitBillViewModel = viewModel(factory = SplitBillViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            Column {
                Text(
                    text = "Split Bill Calculator",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Split bills evenly, track who has paid, and log your share",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
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
                    viewModel.createSplitBill(
                        rawTitle = title,
                        totalAmount = totalAmount,
                        personCount = personCount,
                        participantNames = names,
                        alsoLogMyShareAsExpense = alsoLogAsExpense
                    )
                }
            )
        }

        // 3. Split Bill List Log Header
        item {
            Text(
                text = "Split Bill Log (${state.totalBillsCount})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }

        if (state.billsWithParticipants.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = SlateSurface,
                    border = BorderStroke(1.dp, BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "No split bills logged yet. Enter a bill amount and select how many people are included above.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        modifier = Modifier.padding(20.dp)
                    )
                }
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
                        viewModel.toggleParticipantPaid(participant)
                    },
                    onLogAsExpense = { bill ->
                        viewModel.logBillShareAsExpense(bill)
                    },
                    onDeleteBill = { bill ->
                        viewModel.deleteSplitBill(bill)
                    }
                )
            }
        }
    }
}