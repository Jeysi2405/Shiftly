package com.example.shiftly.ui.screens.splitbill

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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyMonthFilterBar
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.shiftlySheetBodyBand
import com.example.shiftly.ui.components.shiftlySheetTopBand
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@Composable
fun SplitBillScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {},
) {
    val colors = ShiftlyColors.current
    val viewModel: SplitBillViewModel = viewModel(factory = SplitBillViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val listState = rememberLazyListState()

    var showNewSplitSheet by remember { mutableStateOf(false) }
    var pendingDeleteBill by remember { mutableStateOf<SplitBillEntity?>(null) }
    var prefillTrigger by remember { mutableIntStateOf(0) }
    var prefillAmount by remember { mutableStateOf("") }
    var prefillTitle by remember { mutableStateOf("") }
    var prefillPersonCount by remember { mutableIntStateOf(2) }

    SplitBillScreenContent(
        state = state,
        onBack = onBack,
        listState = listState,
        onPreviousMonth = { viewModel.shiftMonth(-1L) },
        onNextMonth = { viewModel.shiftMonth(1L) },
        onToggleAllMonths = { viewModel.toggleAllMonths() },
        onShowAllMonths = { viewModel.showAllMonths() },
        onOpenNewSplit = {
            prefillAmount = ""
            prefillTitle = ""
            prefillPersonCount = 2
            prefillTrigger++
            showNewSplitSheet = true
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
            showNewSplitSheet = true
        }
    )

    if (showNewSplitSheet) {
        NewSplitBillBottomSheet(
            currencySymbol = state.currencySymbol,
            initialAmount = prefillAmount,
            initialTitle = prefillTitle,
            initialPersonCount = prefillPersonCount,
            prefillTrigger = prefillTrigger,
            onDismiss = { showNewSplitSheet = false },
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
                showNewSplitSheet = false
                onShowMessage(
                    if (alsoLogAsExpense) "Split bill saved & share logged to Expenses"
                    else "Split bill saved as pending"
                )
            }
        )
    }

    pendingDeleteBill?.let { billToDelete ->
        if (billToDelete.isLoggedToExpense) {
            AlertDialog(
                onDismissRequest = { pendingDeleteBill = null },
                containerColor = colors.card,
                icon = {
                    ShiftlyIosIconTile(
                        iconRes = R.drawable.ic_lucide_trash_2,
                        backgroundColor = colors.coralBright,
                        size = 44.dp,
                        cornerRadius = 11.dp,
                        iconSize = 22.dp
                    )
                },
                title = {
                    Text(
                        text = "Delete Split Bill?",
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                },
                text = {
                    Text(
                        text = "\"${billToDelete.title}\" also logged your share to Expenses. Delete both entries or keep the logged expense?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary
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
                                containerColor = colors.coralBright,
                                contentColor = PureWhite
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Delete Both",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.deleteSplitBill(billToDelete, deleteLinkedExpense = false)
                                pendingDeleteBill = null
                                onShowMessage("Split bill deleted; expense kept")
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.cardElevated,
                                contentColor = colors.textPrimary
                            ),
                            elevation = ButtonDefaults.buttonElevation(
                                defaultElevation = 0.dp,
                                pressedElevation = 0.dp
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Keep Expense",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                )
                            )
                        }
                        TextButton(
                            onClick = { pendingDeleteBill = null },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Cancel",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily
                                ),
                                color = colors.textSecondary
                            )
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
    onOpenNewSplit: () -> Unit,
    onToggleParticipantPaid: (SplitParticipantEntity) -> Unit,
    onLogAsExpense: (SplitBillEntity) -> Unit,
    onDeleteBill: (SplitBillEntity) -> Unit,
    onTrySampleSplit: () -> Unit,
    onBack: () -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onToggleAllMonths: () -> Unit = {},
    onShowAllMonths: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val itemDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.Top
    ) {
        // 0. Screen Header
        item(key = "split_header", contentType = "header") {
            ShiftlyScreenHeader(
                title = "Split Bill",
                subtitle = null,
                onBackClick = onBack,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            )
        }

        // 0b. Month / All-time Filter Bar
        item(key = "month_filter", contentType = "month_filter") {
            ShiftlyMonthFilterBar(
                selectedMonth = state.selectedMonth,
                onPreviousMonth = onPreviousMonth,
                onNextMonth = onNextMonth,
                onToggleAllMonths = onToggleAllMonths,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            )
        }

        // 1. Summary Card (Pending + Paid Mint Badge + Progress Bar) + New Split Button
        item(key = "split_summary", contentType = "summary") {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SplitBillSummaryCard(
                    currencySymbol = state.currencySymbol,
                    totalPendingAmount = state.totalPendingAmount,
                    totalCollectedAmount = state.totalCollectedAmount,
                    paidParticipantsCount = state.paidParticipantsCount,
                    totalParticipantsCount = state.totalParticipantsCount
                )

                Button(
                    onClick = onOpenNewSplit,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.azurePrimary,
                        contentColor = colors.onAzurePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp, pressedElevation = 0.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_plus),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "New Split",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.onAzurePrimary
                    )
                }
            }
        }

        // 2. Bottom Sheet Top Cap for Split Bills List
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
                    title = "Split Bills",
                    count = state.totalBillsCount,
                    onLightSurface = true
                )
            }
        }

        if (state.billsWithParticipants.isEmpty()) {
            item(key = "sheet_empty", contentType = "sheet_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 500.dp)
                        .shiftlySheetBodyBand(colors.surface)
                        .padding(start = 20.dp, end = 20.dp, bottom = 104.dp)
                ) {
                    SplitBillEmptyCard(
                        currencySymbol = state.currencySymbol,
                        selectedMonth = state.selectedMonth,
                        hasAnyBillsAtAll = state.hasAnyBillsAtAll,
                        onShowAllMonths = onShowAllMonths,
                        onTrySampleSplit = onTrySampleSplit
                    )
                }
            }
        } else {
            itemsIndexed(
                items = state.billsWithParticipants,
                key = { _, item -> "split_${item.bill.id}" },
                contentType = { _, _ -> "split_bill_item" }
            ) { idx, billWithParticipants ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shiftlySheetBodyBand(colors.surface)
                        .padding(horizontal = 20.dp)
                ) {
                    SplitBillLogItemCard(
                        item = billWithParticipants,
                        currencySymbol = state.currencySymbol,
                        onToggleParticipantPaid = onToggleParticipantPaid,
                        onLogAsExpense = onLogAsExpense,
                        onDeleteBill = onDeleteBill,
                        dateFormatter = itemDateFormatter
                    )
                    if (idx < state.billsWithParticipants.lastIndex) {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            item(key = "sheet_footer", contentType = "sheet_footer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shiftlySheetBodyBand(colors.surface)
                        .height(if (state.billsWithParticipants.size <= 1) 260.dp else 104.dp)
                )
            }
        }
    }
}

// =============================================================================
// High-Fidelity Previews (Empty, New Split Sheet, List Filled)
// =============================================================================

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Split Bill Empty")
@Composable
private fun SplitBillEmptyPreview() {
    val state = SplitBillUiState(
        currencySymbol = "₱",
        selectedMonth = YearMonth.of(2026, 5)
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitBillScreenContent(
                    state = state,
                    onOpenNewSplit = {},
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. New Split Bill Sheet")
@Composable
private fun NewSplitBillSheetPreview() {
    val state = SplitBillUiState(
        currencySymbol = "₱",
        selectedMonth = YearMonth.of(2026, 5),
        totalPendingAmount = 600.0,
        totalCollectedAmount = 600.0,
        paidParticipantsCount = 2,
        totalParticipantsCount = 4
    )

    ShiftlyTheme {
        val colors = ShiftlyColors.current
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                SplitBillScreenContent(
                    state = state,
                    onOpenNewSplit = {},
                    onToggleParticipantPaid = {},
                    onLogAsExpense = {},
                    onDeleteBill = {},
                    onTrySampleSplit = {}
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.65f))
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        color = colors.surface,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column {
                            ShiftlySheetDragHandle()
                            Spacer(modifier = Modifier.height(6.dp))
                            NewSplitBillSheetContent(
                                currencySymbol = "₱",
                                onDismiss = {},
                                onSaveSplitBill = { _, _, _, _, _, _ -> },
                                initialAmount = "1200",
                                initialTitle = "Group Dinner",
                                initialPersonCount = 4
                            )
                        }
                    }
                }
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
        selectedMonth = YearMonth.of(2026, 5),
        billsWithParticipants = bills,
        totalBillsCount = bills.size,
        hasAnyBillsAtAll = true,
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
                    onOpenNewSplit = {},
                    onToggleParticipantPaid = {},
                    onLogAsExpense = {},
                    onDeleteBill = {},
                    onTrySampleSplit = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 0)
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
