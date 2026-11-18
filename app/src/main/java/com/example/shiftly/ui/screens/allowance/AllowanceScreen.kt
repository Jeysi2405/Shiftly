package com.example.shiftly.ui.screens.allowance

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import java.time.LocalDate

@Composable
fun AllowanceScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: AllowanceViewModel = viewModel(factory = AllowanceViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current

    var showAddSheet by remember { mutableStateOf(false) }
    var showChangeCutoffSheet by remember { mutableStateOf(false) }
    var pendingDeleteEntry by remember { mutableStateOf<AllowanceEntryEntity?>(null) }

    AllowanceScreenContent(
        state = state,
        onBack = onBack,
        onOpenAddAllowance = { showAddSheet = true },
        onOpenChangeCutoff = { showChangeCutoffSheet = true },
        onDeleteEntry = { pendingDeleteEntry = it }
    )

    if (showAddSheet) {
        AddAllowanceBottomSheet(
            currencySymbol = state.currencySymbol,
            onDismiss = { showAddSheet = false },
            onSave = { title, amount, source, date ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.addManualAllowance(title, amount, source, date)
                showAddSheet = false
                onShowMessage("Allowance added")
            }
        )
    }

    if (showChangeCutoffSheet) {
        ChangeCutoffScheduleSheet(
            currentConfig = state.cutoffConfig,
            onDismiss = { showChangeCutoffSheet = false },
            onSave = { config ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.updateCutoffSchedule(config) {
                    showChangeCutoffSheet = false
                    onShowMessage("Cutoff schedule updated")
                }
            }
        )
    }

    pendingDeleteEntry?.let { entryToDelete ->
        ShiftlyConfirmDeleteDialog(
            title = "Delete Allowance Entry?",
            message = "Remove \"${entryToDelete.title}\"?",
            onDismiss = { pendingDeleteEntry = null },
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteAllowanceEntry(entryToDelete)
                pendingDeleteEntry = null
                onShowMessage("Allowance entry deleted")
            }
        )
    }
}

@Composable
fun AllowanceScreenContent(
    state: AllowanceUiState,
    onBack: () -> Unit = {},
    onOpenAddAllowance: () -> Unit = {},
    onOpenChangeCutoff: () -> Unit = {},
    onDeleteEntry: (AllowanceEntryEntity) -> Unit = {}
) {
    val colors = ShiftlyColors.current
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ShiftlyScreenHeader(
                title = "Allowance",
                subtitle = null,
                onBackClick = onBack,
                modifier = Modifier.padding(horizontal = 20.dp),
                trailing = {
                    Button(
                        onClick = onOpenAddAllowance,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.azurePrimary,
                            contentColor = colors.onAzurePrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp
                        ),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_plus),
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Add",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.onAzurePrimary
                        )
                    }
                }
            )
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                AllowanceHeroCard(
                    totalBalance = state.totalAllowanceBalance,
                    salaryTotal = state.salaryTotal,
                    businessTotal = state.businessTotal,
                    othersTotal = state.othersTotal,
                    currencySymbol = state.currencySymbol
                )
            }
        }

        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                AllowanceCutoffScheduleCard(
                    cutoffConfig = state.cutoffConfig,
                    activeWindow = state.activeCutoffWindow,
                    pendingDutiesEarnings = state.pendingDutiesEarnings,
                    pendingShiftCount = state.pendingShiftCount,
                    currencySymbol = state.currencySymbol,
                    onChangeCutoff = onOpenChangeCutoff
                )
            }
        }

        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 520.dp),
                shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                color = colors.surface,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShiftlySheetDragHandle()

                    ShiftlySectionHeader(
                        title = "Allowance History",
                        count = state.entries.size,
                        onLightSurface = true
                    )

                    if (state.entries.isEmpty()) {
                        AllowanceEmptyCard(onAddAllowance = onOpenAddAllowance)
                    } else {
                        state.entries.forEach { entry ->
                            AllowanceHistoryItemCard(
                                entry = entry,
                                currencySymbol = state.currencySymbol,
                                onDelete = { onDeleteEntry(entry) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Allowance Empty")
@Composable
private fun AllowanceEmptyPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            AllowanceScreenContent(state = AllowanceUiState())
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Allowance Filled")
@Composable
private fun AllowanceFilledPreview() {
    val today = LocalDate.now()
    val entries = listOf(
        AllowanceEntryEntity(
            id = 1L,
            title = "Salary Payout (1st Cutoff)",
            amount = 4250.0,
            source = AllowanceSource.SALARY,
            dateEpochDay = today.minusDays(2).toEpochDay(),
            cutoffKey = "SEMI_${today.minusDays(2)}"
        ),
        AllowanceEntryEntity(
            id = 2L,
            title = "UI Freelance Commission",
            amount = 1800.0,
            source = AllowanceSource.BUSINESS,
            dateEpochDay = today.minusDays(1).toEpochDay(),
            cutoffKey = null
        ),
        AllowanceEntryEntity(
            id = 3L,
            title = "Weekly Family Support",
            amount = 1200.0,
            source = AllowanceSource.OTHERS,
            dateEpochDay = today.toEpochDay(),
            cutoffKey = null
        )
    )
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            AllowanceScreenContent(
                state = AllowanceUiState(
                    entries = entries,
                    totalAllowanceBalance = 7250.0,
                    salaryTotal = 4250.0,
                    businessTotal = 1800.0,
                    othersTotal = 1200.0,
                    cutoffConfig = CutoffScheduleConfig(),
                    pendingDutiesEarnings = 1480.0,
                    pendingShiftCount = 2
                )
            )
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Change Cutoff Sheet")
@Composable
private fun ChangeCutoffSheetPreview() {
    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            AllowanceScreenContent(state = AllowanceUiState())
            ChangeCutoffScheduleSheet(
                currentConfig = CutoffScheduleConfig(),
                onDismiss = {},
                onSave = {}
            )
        }
    }
}
