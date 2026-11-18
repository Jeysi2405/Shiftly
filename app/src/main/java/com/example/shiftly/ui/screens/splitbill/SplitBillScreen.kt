package com.example.shiftly.ui.screens.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.RoseAlert
import com.example.shiftly.ui.theme.RoseContainer
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import java.time.LocalDate
import kotlinx.coroutines.launch

@Composable
fun SplitBillScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    val viewModel: SplitBillViewModel = viewModel(factory = SplitBillViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()

    var pendingDeleteBill by remember { mutableStateOf<SplitBillEntity?>(null) }
    var prefillTrigger by remember { mutableIntStateOf(0) }
    var prefillAmount by remember { mutableStateOf("") }
    var prefillTitle by remember { mutableStateOf("") }
    var prefillPersonCount by remember { mutableIntStateOf(2) }

    SplitBillScreenContent(
        state = state,
        onBack = onBack,
        listState = listState,
        initialAmount = prefillAmount,
        initialTitle = prefillTitle,
        initialPersonCount = prefillPersonCount,
        prefillTrigger = prefillTrigger,
        onSaveSplitBill = { title, totalAmount, personCount, names, paidStates, alsoLogAsExpense ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.createSplitBill(
                rawTitle = title,
                totalAmount = totalAmount,
                personCount = personCount,
                participantNames = names,
                participantPaidStates = paidStates,
                alsoLogMyShareAsExpense = alsoLogAsExpense
            )
            onShowMessage(
                if (alsoLogAsExpense) "Split bill saved & share logged to Expenses"
                else "Split bill saved as pending"
            )
        },
        onToggleParticipantPaid = { participant ->
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.toggleParticipantPaid(participant)
        },
        onLogAsExpense = { bill ->
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
            viewModel.logBillShareAsExpense(bill)
            onShowMessage("Share logged to Expenses")
        },
        onDeleteBill = { pendingDeleteBill = it },
        onTrySampleSplit = {
            prefillAmount = "1200"
            prefillTitle = "Group Dinner"
            prefillPersonCount = 4
            prefillTrigger++
            scope.launch { listState.animateScrollToItem(1) }
        }
    )

    pendingDeleteBill?.let { billToDelete ->
        if (billToDelete.isLoggedToExpense) {
            AlertDialog(
                onDismissRequest = { pendingDeleteBill = null },
                containerColor = DarkCard,
                icon = {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(RoseContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = null,
                            tint = RoseAlert,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = "Delete Split Bill?",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextLightPrimary
                    )
                },
                text = {
                    Text(
                        text = "\"${billToDelete.title}\" also logged your share to Expenses. Delete both entries or keep the logged expense?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextLightSecondary
                    )
                },
                confirmButton = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteSplitBill(billToDelete, deleteLinkedExpense = true)
                                pendingDeleteBill = null
                                onShowMessage("Split bill & linked expense deleted")
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = CoralBright,
                                contentColor = InkBlack
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Delete Both", fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteSplitBill(billToDelete, deleteLinkedExpense = false)
                                pendingDeleteBill = null
                                onShowMessage("Split bill deleted; expense kept")
                            },
                            shape = RoundedCornerShape(50),
                            border = BorderStroke(1.dp, DarkBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Keep Expense", fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                        }
                        TextButton(
                            onClick = { pendingDeleteBill = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Cancel", fontFamily = ShiftlySerif, color = TextLightSecondary)
                        }
                    }
                }
            )
        } else {
            ShiftlyConfirmDeleteDialog(
                title = "Delete Split Bill?",
                message = "Remove \"${billToDelete.title}\"?",
                onDismiss = { pendingDeleteBill = null },
                onConfirm = {
                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    viewModel.deleteSplitBill(billToDelete, deleteLinkedExpense = false)
                    pendingDeleteBill = null
                    onShowMessage("Split bill deleted")
                }
            )
        }
    }
}

@Composable
fun SplitBillScreenContent(
    state: SplitBillUiState,
    onSaveSplitBill: (
        title: String,
        totalAmount: Double,
        personCount: Int,
        participantNames: List<String>,
        participantPaidStates: List<Boolean>,
        alsoLogAsExpense: Boolean
    ) -> Unit,
    onToggleParticipantPaid: (SplitParticipantEntity) -> Unit,
    onLogAsExpense: (SplitBillEntity) -> Unit,
    onDeleteBill: (SplitBillEntity) -> Unit,
    onTrySampleSplit: () -> Unit,
    onBack: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    initialAmount: String = "",
    initialTitle: String = "",
    initialPersonCount: Int = 2,
    prefillTrigger: Int = 0
) {
    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Screen Header
        item {
            ShiftlyScreenHeader(
                title = "Split Bill",
                subtitle = null,
                onBackClick = onBack,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 1. Summary Card (Pending + Paid Mint Badge + Progress Bar)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SplitBillSummaryCard(
                    currencySymbol = state.currencySymbol,
                    totalPendingAmount = state.totalPendingAmount,
                    totalCollectedAmount = state.totalCollectedAmount,
                    paidParticipantsCount = state.paidParticipantsCount,
                    totalParticipantsCount = state.totalParticipantsCount
                )
            }
        }

        // 2. Interactive Split Calculator Card
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                SplitBillCalculatorCard(
                    currencySymbol = state.currencySymbol,
                    onSaveSplitBill = onSaveSplitBill,
                    initialAmount = initialAmount,
                    initialTitle = initialTitle,
                    initialPersonCount = initialPersonCount,
                    externalPrefillTrigger = prefillTrigger
                )
            }
        }

        // 3. Dark Bottom Sheet Surface for Split Bills List / Empty State
        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
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

                    ShiftlySectionHeader(
                        title = "Split Bills",
                        count = state.totalBillsCount,
                        onLightSurface = true
                    )

                    if (state.billsWithParticipants.isEmpty()) {
                        SplitBillEmptyCard(
                            currencySymbol = state.currencySymbol,
                            onTrySampleSplit = onTrySampleSplit
                        )
                    } else {
                        state.billsWithParticipants.forEach { billWithParticipants ->
                            SplitBillLogItemCard(
                                item = billWithParticipants,
                                currencySymbol = state.currencySymbol,
                                onToggleParticipantPaid = onToggleParticipantPaid,
                                onLogAsExpense = onLogAsExpense,
                                onDeleteBill = onDeleteBill
                            )
                        }
                    }
                }
            }
        }
    }
}

// =============================================================================
// High-Fidelity Previews (Empty, Empty List Sheet, Filled Calculator, List)
// =============================================================================

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Split Bill Empty")
@Composable
private fun SplitBillEmptyPreview() {
    val state = SplitBillUiState(currencySymbol = "₱")

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitBillScreenContent(
                    state = state,
                    onSaveSplitBill = { _, _, _, _, _, _ -> },
                    onToggleParticipantPaid = {},
                    onLogAsExpense = {},
                    onDeleteBill = {},
                    onTrySampleSplit = {}
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Split Bill Filled (₱1,200 ÷ 4)")
@Composable
private fun SplitBillFilledPreview() {
    val state = SplitBillUiState(
        currencySymbol = "₱",
        totalPendingAmount = 600.0,
        totalCollectedAmount = 600.0,
        paidParticipantsCount = 2,
        totalParticipantsCount = 4
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitBillScreenContent(
                    state = state,
                    onSaveSplitBill = { _, _, _, _, _, _ -> },
                    onToggleParticipantPaid = {},
                    onLogAsExpense = {},
                    onDeleteBill = {},
                    onTrySampleSplit = {},
                    initialAmount = "1200",
                    initialTitle = "Group Dinner",
                    initialPersonCount = 4
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Split Bills List Filled")
@Composable
private fun SplitBillListPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val dinnerBill = SplitBillEntity(
        id = 1L,
        title = "Group Dinner",
        totalAmount = 1200.0,
        personCount = 4,
        perPersonAmount = 300.0,
        dateEpochDay = today.toEpochDay(),
        isLoggedToExpense = true
    )
    val printBill = SplitBillEntity(
        id = 2L,
        title = "Lab Print & Snacks",
        totalAmount = 450.0,
        personCount = 3,
        perPersonAmount = 150.0,
        dateEpochDay = today.minusDays(1).toEpochDay(),
        isLoggedToExpense = false
    )

    val bills = listOf(
        SplitBillWithParticipants(
            bill = dinnerBill,
            participants = listOf(
                SplitParticipantEntity(1L, 1L, "You", 300.0, hasPaid = true, isCurrentUser = true),
                SplitParticipantEntity(2L, 1L, "Jules", 300.0, hasPaid = true, isCurrentUser = false),
                SplitParticipantEntity(3L, 1L, "Kian", 300.0, hasPaid = false, isCurrentUser = false),
                SplitParticipantEntity(4L, 1L, "Bea", 300.0, hasPaid = false, isCurrentUser = false)
            )
        ),
        SplitBillWithParticipants(
            bill = printBill,
            participants = listOf(
                SplitParticipantEntity(5L, 2L, "You", 150.0, hasPaid = true, isCurrentUser = true),
                SplitParticipantEntity(6L, 2L, "Mika", 150.0, hasPaid = true, isCurrentUser = false),
                SplitParticipantEntity(7L, 2L, "Leo", 150.0, hasPaid = true, isCurrentUser = false)
            )
        )
    )

    val state = SplitBillUiState(
        currencySymbol = "₱",
        billsWithParticipants = bills,
        totalBillsCount = bills.size,
        totalCollectedAmount = 600.0,
        totalPendingAmount = 600.0,
        paidParticipantsCount = 3,
        totalParticipantsCount = 5
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitBillScreenContent(
                    state = state,
                    onSaveSplitBill = { _, _, _, _, _, _ -> },
                    onToggleParticipantPaid = {},
                    onLogAsExpense = {},
                    onDeleteBill = {},
                    onTrySampleSplit = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 3)
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "4. Split Bill Empty Sheet")
@Composable
private fun SplitBillEmptySheetPreview() {
    val state = SplitBillUiState(currencySymbol = "₱")

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitBillScreenContent(
                    state = state,
                    onSaveSplitBill = { _, _, _, _, _, _ -> },
                    onToggleParticipantPaid = {},
                    onLogAsExpense = {},
                    onDeleteBill = {},
                    onTrySampleSplit = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 3)
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
