package com.example.shiftly.ui.screens.recurring

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyHelpIconButton
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.shiftlySheetBodyBand
import com.example.shiftly.ui.components.shiftlySheetTopBand
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.utils.RecurringEngine
import com.example.shiftly.workers.ShiftlyDailyWorker
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun RecurringBillsScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: RecurringBillsViewModel = viewModel(factory = RecurringBillsViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var showEditorSheet by remember { mutableStateOf(false) }
    var showTutorialSheet by remember { mutableStateOf(false) }
    var editingBill by remember { mutableStateOf<RecurringBillEntity?>(null) }
    var pendingDeleteBill by remember { mutableStateOf<RecurringBillEntity?>(null) }

    val earliestNextDue = remember(state.bills) {
        RecurringEngine.earliestNextDueDateTime(state.bills)
    }
    LaunchedEffect(earliestNextDue) {
        withContext(Dispatchers.IO) {
            ShiftlyDailyWorker.scheduleNextRecurringCheck(
                context = context,
                nextDue = earliestNextDue
            )
        }
    }

    RecurringBillsScreenContent(
        state = state,
        onBack = onBack,
        onOpenHelp = { showTutorialSheet = true },
        onOpenAddSheet = {
            editingBill = null
            showEditorSheet = true
        },
        onEditBill = { bill ->
            editingBill = bill
            showEditorSheet = true
        },
        onToggleEnabled = { bill, enabled ->
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            viewModel.toggleBillEnabled(bill, enabled) { posted ->
                if (posted > 0) {
                    onShowMessage("Logged $posted due bill(s) to Expenses")
                }
            }
        },
        onDeleteBill = { pendingDeleteBill = it }
    )

    if (showTutorialSheet) {
        ShiftlyScreenTutorialSheet(
            title = "Recurring Bills Guide",
            subtitle = "Stay ahead of repeating bills without manual entry.",
            steps = listOf(
                "Schedule repeating bills" to "Tap Add Recurring Bill to set the amount, category, frequency, and next due date and time.",
                "Automatic expense logging" to "Active bills automatically post to your Expenses when due so your spending totals stay up to date.",
                "Pause or edit anytime" to "Toggle any bill off temporarily without deleting it, or tap a row to adjust its amount and schedule."
            ),
            onDismiss = { showTutorialSheet = false }
        )
    }

    if (showEditorSheet) {
        AddEditRecurringBillBottomSheet(
            currencySymbol = state.currencySymbol,
            availableCategories = state.availableCategories,
            editingBill = editingBill,
            onDismiss = {
                showEditorSheet = false
                editingBill = null
            },
            onSave = { title, amount, category, frequency, dueDate, dueHour, dueMinute, isEnabled ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                val isNew = editingBill == null
                viewModel.saveBill(
                    existing = editingBill,
                    title = title,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    dueDate = dueDate,
                    dueHour = dueHour,
                    dueMinute = dueMinute,
                    isEnabled = isEnabled
                ) { posted ->
                    showEditorSheet = false
                    editingBill = null
                    onShowMessage(
                        when {
                            posted > 0 -> "Saved & posted $posted due bill(s) to Expenses"
                            isNew -> "Recurring bill added"
                            else -> "Recurring bill updated"
                        }
                    )
                }
            }
        )
    }

    pendingDeleteBill?.let { billToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Recurring Bill?",
            message = "This recurring bill schedule for \"${billToDelete.title}\" will be permanently removed.",
            onDismiss = { pendingDeleteBill = null },
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteBill(billToDelete)
                pendingDeleteBill = null
                onShowMessage("Recurring bill deleted")
            }
        )
    }
}

@Composable
fun RecurringBillsScreenContent(
    state: RecurringBillsUiState,
    onBack: () -> Unit = {},
    onOpenHelp: () -> Unit = {},
    onOpenAddSheet: () -> Unit = {},
    onEditBill: (RecurringBillEntity) -> Unit = {},
    onToggleEnabled: (RecurringBillEntity, Boolean) -> Unit = { _, _ -> },
    onDeleteBill: (RecurringBillEntity) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val itemDateTimeFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", locale)
    }
    val now = remember { LocalDateTime.now() }

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.Top
    ) {
        item(key = "recurring_header", contentType = "header") {
            ShiftlyScreenHeader(
                title = "Recurring Bills",
                subtitle = null,
                onBackClick = onBack,
                trailing = {
                    ShiftlyHelpIconButton(onClick = onOpenHelp)
                },
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
            )
        }

        item(key = "recurring_summary", contentType = "summary") {
            Column(
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                RecurringBillsSummaryCard(
                    activeCount = state.activeCount,
                    totalCount = state.bills.size,
                    monthlyEstimate = state.enabledMonthlyEstimate,
                    dueSoonCount = state.dueSoonCount,
                    currencySymbol = state.currencySymbol
                )

                if (state.bills.isNotEmpty()) {
                    ShiftlyPrimaryPillButton(
                        text = "Add Recurring Bill",
                        onClick = onOpenAddSheet,
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_plus),
                                contentDescription = null,
                                tint = colors.onAzurePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    )
                }
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
                    title = "Scheduled Bills",
                    count = state.bills.size,
                    onLightSurface = true
                )
            }
        }

        if (state.bills.isEmpty()) {
            item(key = "sheet_empty", contentType = "sheet_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 540.dp)
                        .shiftlySheetBodyBand(colors.surface)
                        .padding(start = 20.dp, end = 20.dp, bottom = 104.dp)
                ) {
                    RecurringBillsEmptyCard(onAddBill = onOpenAddSheet)
                }
            }
        } else {
            itemsIndexed(
                items = state.bills,
                key = { _, bill -> "recurring_${bill.id}" },
                contentType = { _, _ -> "recurring_bill_item" }
            ) { idx, bill ->
                val rowShape = when {
                    state.bills.size == 1 -> RoundedCornerShape(18.dp)
                    idx == 0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    idx == state.bills.lastIndex -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
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
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            bottom = if (idx < state.bills.lastIndex) 2.dp else 0.dp
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(colors.cardElevated)
                    ) {
                        RecurringBillItemCard(
                            bill = bill,
                            currencySymbol = state.currencySymbol,
                            onToggleEnabled = { enabled -> onToggleEnabled(bill, enabled) },
                            onEdit = { onEditBill(bill) },
                            onDelete = { onDeleteBill(bill) },
                            dateTimeFormatter = itemDateTimeFormatter,
                            now = now,
                            containerColor = Color.Transparent,
                            shape = RectangleShape
                        )
                    }
                }
            }

            item(key = "sheet_footer", contentType = "sheet_footer") {
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .shiftlySheetBodyBand(colors.surface)
                        .height(if (state.bills.size <= 2) 260.dp else 104.dp)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Recurring Bills Empty")
@Composable
private fun RecurringBillsEmptyPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            RecurringBillsScreenContent(state = RecurringBillsUiState())
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Recurring Bills List")
@Composable
private fun RecurringBillsFilledPreview() {
    val tomorrow = LocalDate.now().plusDays(1)
    val nextWeek = LocalDate.now().plusDays(6)
    val sampleBills = listOf(
        RecurringBillEntity(
            id = 1L,
            title = "Dorm Wi-Fi",
            amount = 999.0,
            category = "Bills",
            frequency = "Monthly",
            dueEpochDay = tomorrow.toEpochDay(),
            dueHour = 9,
            dueMinute = 0,
            isEnabled = true
        ),
        RecurringBillEntity(
            id = 2L,
            title = "Weekly Commute Pass",
            amount = 250.0,
            category = "Transport",
            frequency = "Weekly",
            dueEpochDay = nextWeek.toEpochDay(),
            dueHour = 7,
            dueMinute = 30,
            isEnabled = true
        ),
        RecurringBillEntity(
            id = 3L,
            title = "Cloud Storage",
            amount = 149.0,
            category = "Bills",
            frequency = "Monthly",
            dueEpochDay = nextWeek.plusDays(8).toEpochDay(),
            dueHour = 12,
            dueMinute = 0,
            isEnabled = false
        )
    )
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            RecurringBillsScreenContent(
                state = RecurringBillsUiState(
                    bills = sampleBills,
                    enabledMonthlyEstimate = 999.0 + (250.0 * 4.33),
                    activeCount = 2,
                    dueSoonCount = 1
                )
            )
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Add Recurring Bill Sheet")
@Composable
private fun AddRecurringBillSheetPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            RecurringBillsScreenContent(state = RecurringBillsUiState())
            AddEditRecurringBillBottomSheet(
                currencySymbol = "₱",
                availableCategories = listOf("Bills", "Food", "Transport", "Supplies", "Other"),
                onDismiss = {},
                onSave = { _, _, _, _, _, _, _, _ -> }
            )
        }
    }
}
