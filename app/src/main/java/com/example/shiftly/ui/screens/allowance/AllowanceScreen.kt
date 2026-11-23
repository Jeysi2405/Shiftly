package com.example.shiftly.ui.screens.allowance

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
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
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyHelpIconButton
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySubScreenTopBar
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.ShiftlyTheme
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    var showTutorialSheet by remember { mutableStateOf(false) }
    var pendingDeleteEntry by remember { mutableStateOf<AllowanceEntryEntity?>(null) }

    AllowanceScreenContent(
        state = state,
        onBack = onBack,
        onOpenHelp = { showTutorialSheet = true },
        onOpenAddAllowance = { showAddSheet = true },
        onOpenChangeCutoff = { showChangeCutoffSheet = true },
        onDeleteEntry = { pendingDeleteEntry = it }
    )

    if (showTutorialSheet) {
        ShiftlyScreenTutorialSheet(
            title = "Allowance & Cutoff Guide",
            subtitle = null,
            steps = listOf(
                "Add allowance or income" to "Record allowances, stipends, or side income tagged by source.",
                "Adjust payday cutoff" to "Set semi-monthly, weekly, or custom cutoff schedules anytime.",
                "Track cutoff payout" to "Monitor pending shift earnings for the active payday cutoff period."
            ),
            onDismiss = { showTutorialSheet = false }
        )
    }

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
        ShiftlyDeleteConfirmDialog(
            title = "Delete Allowance?",
            message = "Delete \"${entryToDelete.title}\"?",
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
    onOpenHelp: () -> Unit = {},
    onOpenAddAllowance: () -> Unit = {},
    onOpenChangeCutoff: () -> Unit = {},
    onDeleteEntry: (AllowanceEntryEntity) -> Unit = {},
    listState: LazyListState = rememberLazyListState()
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val itemDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val isScrolled by remember(listState) {
        derivedStateOf {
            listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 8
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        ShiftlySubScreenTopBar(
            title = "Allowance",
            onBack = onBack,
            showDivider = isScrolled,
            trailing = {
                ShiftlyHelpIconButton(onClick = onOpenHelp)
                ShiftlySecondaryTextButton(
                    text = "Add",
                    onClick = onOpenAddAllowance,
                    leadingIcon = {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_plus,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
            }
        )

        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(top = 12.dp, bottom = 32.dp),
            verticalArrangement = Arrangement.Top
        ) {
            item(key = "allowance_hero", contentType = "hero") {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                    AllowanceHeroCard(
                        totalBalance = state.totalAllowanceBalance,
                        salaryTotal = state.salaryTotal,
                        businessTotal = state.businessTotal,
                        othersTotal = state.othersTotal,
                        currencySymbol = state.currencySymbol
                    )
                }
            }

            item(key = "allowance_cutoff", contentType = "cutoff") {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
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

            item(key = "sheet_header", contentType = "sheet_header") {
                ShiftlySectionHeader(
                    title = "Allowance History",
                    count = state.entries.size,
                    onLightSurface = false,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                )
            }

            if (state.entries.isEmpty()) {
                item(key = "sheet_empty", contentType = "sheet_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                    ) {
                        AllowanceEmptyCard(onAddAllowance = onOpenAddAllowance)
                    }
                }
            } else {
                itemsIndexed(
                    items = state.entries,
                    key = { _, entry -> "allowance_${entry.id}" },
                    contentType = { _, _ -> "allowance_item" }
                ) { idx, entry ->
                    val rowShape = when {
                        state.entries.size == 1 -> ShiftlyShapes.Card
                        idx == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        idx == state.entries.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        else -> RectangleShape
                    }
                    Column(
                        modifier = Modifier
                            .animateItem(
                                fadeInSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
                                placementSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                                fadeOutSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)
                            )
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .clip(rowShape)
                            .background(colors.card)
                    ) {
                        AllowanceHistoryItemCard(
                            entry = entry,
                            currencySymbol = state.currencySymbol,
                            onDelete = { onDeleteEntry(entry) },
                            dateFormatter = itemDateFormatter,
                            containerColor = Color.Transparent,
                            shape = RectangleShape
                        )
                        if (idx < state.entries.lastIndex) {
                            ShiftlyHairlineDivider(startInset = 54.dp)
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
