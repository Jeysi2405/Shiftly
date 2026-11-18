package com.example.shiftly.ui.screens.duties

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.utils.DayType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun DutiesScreen(
    repository: ShiftlyRepository,
    onBack: () -> Unit = {},
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: DutiesViewModel = viewModel(factory = DutiesViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]

    var showLogShiftSheet by remember { mutableStateOf(false) }
    var showDutiesSettingsSheet by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var pendingConfirmEditShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var pendingDeleteShift by remember { mutableStateOf<ShiftEntity?>(null) }

    DutiesScreenContent(
        state = state,
        onBack = onBack,
        onOpenLogShift = {
            val today = LocalDate.now()
            val todayShift = state.shiftsByEpochDay[today.toEpochDay()]
            if (todayShift != null) {
                editingShift = todayShift
                onShowMessage("Editing existing shift for today")
            } else {
                viewModel.setDutyDate(today)
                showLogShiftSheet = true
            }
        },
        onOpenSettings = { showDutiesSettingsSheet = true },
        onPreviousMonth = { viewModel.changeCalendarMonth(-1) },
        onNextMonth = { viewModel.changeCalendarMonth(1) },
        onSelectCalendarDate = { clickedDate ->
            val existingShift = state.shiftsByEpochDay[clickedDate.toEpochDay()]
            if (existingShift != null) {
                pendingConfirmEditShift = existingShift
            } else {
                viewModel.setDutyDate(clickedDate)
                showLogShiftSheet = true
            }
        },
        onEditShift = { editingShift = it },
        onDeleteShift = { pendingDeleteShift = it }
    )

    if (showDutiesSettingsSheet) {
        DutiesSettingsSheet(
            defaultHourlyRate = state.defaultHourlyRate,
            minimumWagePerDay = state.minimumWagePerDay,
            currencySymbol = state.currencySymbol,
            presets = state.shiftTimePresets,
            onSaveWageRates = { hourlyRate, minWagePerDay ->
                viewModel.updateWageRates(hourlyRate, minWagePerDay)
            },
            onShowMessage = onShowMessage,
            onSavePreset = { sH, sM, eH, eM ->
                viewModel.saveShiftTimePreset(sH, sM, eH, eM)
                onShowMessage("Preset saved")
            },
            onDeletePreset = { presetId ->
                viewModel.deleteShiftTimePreset(presetId)
                onShowMessage("Preset removed")
            },
            onDismiss = { showDutiesSettingsSheet = false }
        )
    }

    if (showLogShiftSheet) {
        ManualShiftBottomSheet(
            selectedDate = state.selectedDutyDate,
            hourlyRate = state.defaultHourlyRate,
            currencySymbol = state.currencySymbol,
            presets = state.shiftTimePresets,
            title = "Log Shift",
            confirmButtonText = "Save Shift",
            blockedEpochDays = state.loggedEpochDays,
            onSwitchToEdit = { duplicateDate ->
                state.shiftsByEpochDay[duplicateDate.toEpochDay()]?.let { existing ->
                    showLogShiftSheet = false
                    editingShift = existing
                }
            },
            onSavePreset = { sH, sM, eH, eM ->
                viewModel.saveShiftTimePreset(sH, sM, eH, eM)
                onShowMessage("Preset saved")
            },
            onDeletePreset = { presetId ->
                viewModel.deleteShiftTimePreset(presetId)
            },
            onDismiss = { showLogShiftSheet = false },
            onSave = { date, sH, sM, eH, eM, note, dayType ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.logManualShift(
                    date = date,
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM,
                    overtimeNote = note,
                    dayType = dayType,
                    onResult = { saved ->
                        if (saved) {
                            showLogShiftSheet = false
                            onShowMessage("Shift logged")
                        } else {
                            onShowMessage("A shift is already logged for this date")
                        }
                    }
                )
            }
        )
    }

    pendingConfirmEditShift?.let { shiftToConfirm ->
        val colors = ShiftlyColors.current
        AlertDialog(
            onDismissRequest = { pendingConfirmEditShift = null },
            containerColor = colors.card,
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(colors.azureContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_square_pen),
                        contentDescription = null,
                        tint = colors.azureInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Edit Shift",
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to edit the shift for this day?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        editingShift = shiftToConfirm
                        pendingConfirmEditShift = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.azurePrimary,
                        contentColor = colors.onAzurePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Edit Shift",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingConfirmEditShift = null }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans
                        ),
                        color = colors.textSecondary
                    )
                }
            }
        )
    }

    editingShift?.let { targetShift ->
        val zone = remember { ZoneId.systemDefault() }
        val startLocalTime = remember(targetShift.id, targetShift.timeInMillis) {
            Instant.ofEpochMilli(targetShift.timeInMillis).atZone(zone).toLocalTime()
        }
        val endLocalTime = remember(targetShift.id, targetShift.timeOutMillis) {
            Instant.ofEpochMilli(targetShift.timeOutMillis).atZone(zone).toLocalTime()
        }
        val initialDayType = remember(targetShift.id, targetShift.dayType) {
            runCatching { DayType.valueOf(targetShift.dayType) }.getOrNull() ?: DayType.ORDINARY
        }

        ManualShiftBottomSheet(
            selectedDate = LocalDate.ofEpochDay(targetShift.dateEpochDay),
            hourlyRate = targetShift.lockedHourlyRate,
            currencySymbol = state.currencySymbol,
            presets = state.shiftTimePresets,
            title = "Edit Shift",
            confirmButtonText = "Update Shift",
            initialStartHour24 = startLocalTime.hour,
            initialStartMinute = startLocalTime.minute,
            initialEndHour24 = endLocalTime.hour,
            initialEndMinute = endLocalTime.minute,
            initialNote = targetShift.overtimeNote,
            initialDayType = initialDayType,
            showPresets = false,
            blockedEpochDays = state.loggedEpochDays - targetShift.dateEpochDay,
            onDismiss = { editingShift = null },
            onSave = { date, sH, sM, eH, eM, note, dayType ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.editShift(
                    shift = targetShift,
                    date = date,
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM,
                    dayType = dayType,
                    overtimeNote = note,
                    onResult = { updated ->
                        if (updated) {
                            editingShift = null
                            onShowMessage("Shift updated")
                        } else {
                            onShowMessage("A shift is already logged for that date")
                        }
                    }
                )
            }
        )
    }

    pendingDeleteShift?.let { shiftToDelete ->
        val deleteFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
        ShiftlyConfirmDeleteDialog(
            title = "Delete Shift",
            message = "Remove shift on ${
                LocalDate.ofEpochDay(shiftToDelete.dateEpochDay).format(deleteFormatter)
            }?",
            onDismiss = { pendingDeleteShift = null },
            onConfirm = {
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.deleteShift(shiftToDelete)
                pendingDeleteShift = null
                onShowMessage("Shift deleted")
            }
        )
    }
}

@Composable
fun DutiesScreenContent(
    state: DutiesUiState,
    onBack: () -> Unit = {},
    onOpenLogShift: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onPreviousMonth: () -> Unit = {},
    onNextMonth: () -> Unit = {},
    onSelectCalendarDate: (LocalDate) -> Unit = {},
    onEditShift: (ShiftEntity) -> Unit = {},
    onDeleteShift: (ShiftEntity) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    todayOverride: LocalDate? = null
) {
    val colors = ShiftlyColors.current

    LazyColumn(
        state = listState,
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 0. Screen Header with Back & Settings Action
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_arrow_left),
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Text(
                        text = "Duties",
                        style = MaterialTheme.typography.headlineSmall,
                        color = colors.textPrimary
                    )
                }

                IconButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_settings),
                        contentDescription = "Duties Settings",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // 1. Monthly Earnings & Time Breakdown Card (Regular=White, ND=Purple, OT=Red)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                TimeBreakdownCard(
                    monthlyTotalEarnings = state.monthlyTotalEarnings,
                    monthlyTotalHours = state.monthlyTotalHours,
                    timeBreakdown = state.monthlyTimeBreakdown,
                    currencySymbol = state.currencySymbol,
                    displayedMonth = state.displayedMonth,
                    defaultHourlyRate = state.defaultHourlyRate,
                    onOpenSettings = onOpenSettings
                )
            }
        }

        // 2. Monthly Shift Calendar
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ShiftCalendarCard(
                    displayedMonth = state.displayedMonth,
                    loggedEpochDays = state.loggedEpochDays,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDate = onSelectCalendarDate,
                    todayOverride = todayOverride
                )
            }
        }

        // 2b. Prominent Log Shift Action Button
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ShiftlyPrimaryPillButton(
                    text = "Log Shift",
                    onClick = onOpenLogShift,
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

        // 3. Bottom Sheet Surface for Monthly Shift History
        item {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 460.dp),
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
                        title = "Shift History",
                        count = state.displayedShifts.size,
                        onLightSurface = true
                    )

                    if (state.displayedShifts.isEmpty()) {
                        ShiftHistoryEmptyCard(
                            onLogFirstShift = onOpenLogShift
                        )
                    } else {
                        state.displayedShifts.forEachIndexed { idx, shift ->
                            ShiftHistoryItemCard(
                                shift = shift,
                                currencySymbol = state.currencySymbol,
                                onEditNote = { onEditShift(shift) },
                                onDelete = { onDeleteShift(shift) }
                            )
                            if (idx < state.displayedShifts.lastIndex) {
                                HorizontalDivider(
                                    color = colors.border.copy(alpha = 0.6f),
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "1. Duties Default")
@Composable
private fun DutiesDefaultPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val state = DutiesUiState(
        defaultHourlyRate = 96.38,
        currencySymbol = "₱",
        selectedDutyDate = today,
        displayedMonth = YearMonth.from(today),
        monthlyTotalEarnings = 0.0,
        monthlyTotalHours = 0.0
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onOpenLogShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    todayOverride = today
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Log Shift Sheet")
@Composable
private fun DutiesLogShiftSheetPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val state = DutiesUiState(
        defaultHourlyRate = 100.0,
        currencySymbol = "₱",
        selectedDutyDate = today,
        displayedMonth = YearMonth.from(today)
    )

    ShiftlyTheme {
        val colors = ShiftlyColors.current
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onOpenLogShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    todayOverride = today
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(InkBlack.copy(alpha = 0.65f))
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                    ) {
                        Column {
                            ShiftlySheetDragHandle()
                            Spacer(modifier = Modifier.height(6.dp))
                            ManualShiftSheetContent(
                                selectedDate = today,
                                hourlyRate = 100.0,
                                currencySymbol = "₱",
                                initialStartHour24 = 22,
                                initialEndHour24 = 8,
                                onDismiss = {},
                                onSave = { _, _, _, _, _, _, _ -> }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Duties History Filled")
@Composable
private fun DutiesHistoryFilledPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val zone = ZoneId.systemDefault()

    fun millisOn(date: LocalDate, hour: Int, min: Int): Long =
        date.atTime(LocalTime.of(hour, min)).atZone(zone).toInstant().toEpochMilli()

    val day1 = today.minusDays(1)
    val day2 = today.minusDays(3)
    val day3 = today.minusDays(5)

    val sampleShifts = listOf(
        ShiftEntity(
            id = 1L,
            dateEpochDay = day1.toEpochDay(),
            timeInMillis = millisOn(day1, 14, 0),
            timeOutMillis = millisOn(day1, 18, 0),
            durationMinutes = 240,
            lockedHourlyRate = 96.38,
            totalEarned = 385.52,
            overtimeNote = "Campus Café Shift",
            dayType = DayType.ORDINARY.name
        ),
        ShiftEntity(
            id = 2L,
            dateEpochDay = day2.toEpochDay(),
            timeInMillis = millisOn(day2, 18, 0),
            timeOutMillis = millisOn(day2, 23, 30),
            durationMinutes = 330,
            lockedHourlyRate = 96.38,
            totalEarned = 544.55,
            overtimeNote = "Library Evening Desk",
            dayType = DayType.ORDINARY.name
        ),
        ShiftEntity(
            id = 3L,
            dateEpochDay = day3.toEpochDay(),
            timeInMillis = millisOn(day3, 9, 0),
            timeOutMillis = millisOn(day3, 18, 0),
            durationMinutes = 540,
            lockedHourlyRate = 96.38,
            totalEarned = 891.52,
            overtimeNote = "Student Center Full Day",
            dayType = DayType.ORDINARY.name
        )
    )

    val state = DutiesUiState(
        defaultHourlyRate = 96.38,
        currencySymbol = "₱",
        selectedDutyDate = today,
        displayedMonth = YearMonth.from(today),
        monthlyTotalEarnings = 1821.59,
        monthlyTotalHours = 18.5,
        monthlyTimeBreakdown = TimeBreakdown(
            regularHours = 13.0,
            nightDiffHours = 3.5,
            overtimeHours = 2.0
        ),
        loggedEpochDays = setOf(day1.toEpochDay(), day2.toEpochDay(), day3.toEpochDay()),
        shiftsByEpochDay = sampleShifts.associateBy { it.dateEpochDay },
        displayedShifts = sampleShifts,
        totalAllShiftsCount = sampleShifts.size
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onOpenLogShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 2),
                    todayOverride = today
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
