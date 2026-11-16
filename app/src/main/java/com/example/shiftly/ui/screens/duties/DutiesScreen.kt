package com.example.shiftly.ui.screens.duties

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.utils.DayType
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutiesScreen(
    repository: ShiftlyRepository,
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: DutiesViewModel = viewModel(factory = DutiesViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val haptics = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]

    var showDutyDatePicker by remember { mutableStateOf(false) }
    var showCheckOutSheet by remember { mutableStateOf(false) }
    var showManualShiftSheet by remember { mutableStateOf(false) }
    var editingShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var pendingDeleteShift by remember { mutableStateOf<ShiftEntity?>(null) }

    DutiesScreenContent(
        state = state,
        onSaveRate = { viewModel.saveDefaultRate(it) },
        onPickDutyDate = { showDutyDatePicker = true },
        onTimeIn = { viewModel.startTimer() },
        onRequestCheckOut = { showCheckOutSheet = true },
        onCancelTimer = { viewModel.cancelActiveTimer() },
        onOpenManualEntry = { showManualShiftSheet = true },
        onEditExistingShift = {
            val existingShift = state.displayedShifts.find {
                it.dateEpochDay == state.selectedDutyDate.toEpochDay()
            } ?: state.displayedShifts.firstOrNull()
            if (existingShift != null) editingShift = existingShift
        },
        onPreviousMonth = { viewModel.changeCalendarMonth(-1) },
        onNextMonth = { viewModel.changeCalendarMonth(1) },
        onSelectCalendarDate = { viewModel.toggleCalendarDateFilter(it) },
        onClearCalendarFilter = { viewModel.clearCalendarFilter() },
        onEditShift = { editingShift = it },
        onDeleteShift = { pendingDeleteShift = it },
        onShowMessage = onShowMessage
    )

    // Duty Date Picker Dialog
    if (showDutyDatePicker) {
        val initialMillis = state.selectedDutyDate
            .atStartOfDay(ZoneOffset.UTC)
            .toInstant()
            .toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDutyDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val picked = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                            viewModel.setDutyDate(picked)
                        }
                        showDutyDatePicker = false
                    }
                ) {
                    Text(
                        text = "Select",
                        color = AzureAccent,
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDutyDatePicker = false }) {
                    Text("Cancel", color = TextLightSecondary, fontFamily = ShiftlySerif)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Check-Out & Overtime Note Bottom Sheet
    if (showCheckOutSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var noteText by remember { mutableStateOf("") }
        var selectedDayType by remember { mutableStateOf(DayType.ORDINARY) }

        ModalBottomSheet(
            onDismissRequest = { showCheckOutSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Check Out Shift",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextLightPrimary
                    )
                    TextButton(
                        onClick = { showCheckOutSheet = false },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Cancel", fontFamily = ShiftlySerif, color = TextLightSecondary)
                    }
                }

                DayTypeSelector(
                    selectedDayType = selectedDayType,
                    onDayTypeSelected = { selectedDayType = it }
                )

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Shift Note (optional)", fontFamily = ShiftlySerif) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzureAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AzureAccent,
                        unfocusedLabelColor = TextLightSecondary,
                        focusedTextColor = TextLightPrimary,
                        unfocusedTextColor = TextLightPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                ShiftlyPrimaryPillButton(
                    text = "Save Shift",
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.checkOutAndSave(noteText, selectedDayType)
                        showCheckOutSheet = false
                        onShowMessage("Shift saved to history")
                    }
                )
            }
        }
    }

    // Manual Past Shift Bottom Sheet
    if (showManualShiftSheet) {
        ManualShiftBottomSheet(
            selectedDate = state.selectedDutyDate,
            hourlyRate = state.defaultHourlyRate,
            currencySymbol = state.currencySymbol,
            nightDiffStartHour = state.nightDiffStartHour,
            nightDiffMultiplier = state.nightDiffMultiplier,
            overtimeMultiplier = state.overtimeMultiplier,
            onPickDate = { showDutyDatePicker = true },
            onDismiss = { showManualShiftSheet = false },
            onSave = { date, sH, sM, eH, eM, note, dayType ->
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                viewModel.logManualShift(
                    date = date,
                    startHour = sH,
                    startMinute = sM,
                    endHour = eH,
                    endMinute = eM,
                    overtimeNote = note,
                    dayType = dayType
                )
                showManualShiftSheet = false
                onShowMessage("Manual shift logged")
            }
        )
    }

    // Edit Existing Shift Bottom Sheet
    editingShift?.let { targetShift ->
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        val zone = remember { ZoneId.systemDefault() }
        val startLocalTime = Instant.ofEpochMilli(targetShift.timeInMillis).atZone(zone).toLocalTime()
        val endLocalTime = Instant.ofEpochMilli(targetShift.timeOutMillis).atZone(zone).toLocalTime()
        val shiftDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }

        var startHour by remember(targetShift.id) { mutableStateOf(startLocalTime.hour.toString()) }
        var startMin by remember(targetShift.id) {
            mutableStateOf(startLocalTime.minute.toString().padStart(2, '0'))
        }
        var endHour by remember(targetShift.id) { mutableStateOf(endLocalTime.hour.toString()) }
        var endMin by remember(targetShift.id) {
            mutableStateOf(endLocalTime.minute.toString().padStart(2, '0'))
        }
        var updatedNote by remember(targetShift.id) { mutableStateOf(targetShift.overtimeNote) }
        var selectedDayType by remember(targetShift.id) {
            mutableStateOf(
                runCatching { DayType.valueOf(targetShift.dayType) }.getOrNull() ?: DayType.ORDINARY
            )
        }

        ModalBottomSheet(
            onDismissRequest = { editingShift = null },
            sheetState = sheetState,
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit Shift (${LocalDate.ofEpochDay(targetShift.dateEpochDay).format(shiftDateFormatter)})",
                        style = MaterialTheme.typography.headlineSmall,
                        color = TextLightPrimary
                    )
                    TextButton(
                        onClick = { editingShift = null },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text("Cancel", fontFamily = ShiftlySerif, color = TextLightSecondary)
                    }
                }

                DayTypeSelector(
                    selectedDayType = selectedDayType,
                    onDayTypeSelected = { selectedDayType = it }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startHour,
                        onValueChange = { startHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("In Hr") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = startMin,
                        onValueChange = { startMin = it.filter(Char::isDigit).take(2) },
                        label = { Text("Min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endHour,
                        onValueChange = { endHour = it.filter(Char::isDigit).take(2) },
                        label = { Text("Out Hr") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = endMin,
                        onValueChange = { endMin = it.filter(Char::isDigit).take(2) },
                        label = { Text("Min") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = updatedNote,
                    onValueChange = { updatedNote = it },
                    label = { Text("Note", fontFamily = ShiftlySerif) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                ShiftlyPrimaryPillButton(
                    text = "Update Shift",
                    onClick = {
                        val sH = (startHour.toIntOrNull() ?: 9).coerceIn(0, 23)
                        val sM = (startMin.toIntOrNull() ?: 0).coerceIn(0, 59)
                        val eH = (endHour.toIntOrNull() ?: 17).coerceIn(0, 23)
                        val eM = (endMin.toIntOrNull() ?: 0).coerceIn(0, 59)

                        viewModel.editShift(
                            shift = targetShift,
                            startHour = sH,
                            startMinute = sM,
                            endHour = eH,
                            endMinute = eM,
                            dayType = selectedDayType,
                            overtimeNote = updatedNote
                        )
                        editingShift = null
                        onShowMessage("Shift updated")
                    }
                )
            }
        }
    }

    // Delete Shift Confirmation Dialog
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
    onSaveRate: (String) -> Unit,
    onPickDutyDate: () -> Unit,
    onTimeIn: () -> Unit,
    onRequestCheckOut: () -> Unit,
    onCancelTimer: () -> Unit,
    onOpenManualEntry: () -> Unit,
    onEditExistingShift: () -> Unit,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectCalendarDate: (LocalDate) -> Unit,
    onClearCalendarFilter: () -> Unit,
    onEditShift: (ShiftEntity) -> Unit,
    onDeleteShift: (ShiftEntity) -> Unit,
    onShowMessage: (String) -> Unit = {},
    listState: LazyListState = rememberLazyListState(),
    previewElapsedSecondsOverride: Long? = null,
    todayOverride: LocalDate? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val filterDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val hasShiftForSelectedDate = remember(state.loggedEpochDays, state.selectedDutyDate) {
        state.loggedEpochDays.contains(state.selectedDutyDate.toEpochDay())
    }

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
                title = "Duties",
                subtitle = "Log shifts & auto-compute pay",
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }

        // 1. Live Time-In / Check-Out Timer Card (reachable primary action)
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                LiveShiftTimerCard(
                    activeTimer = state.activeTimer,
                    selectedDutyDate = state.selectedDutyDate,
                    defaultHourlyRate = state.defaultHourlyRate,
                    currencySymbol = state.currencySymbol,
                    hasShiftForSelectedDate = hasShiftForSelectedDate,
                    onPickDutyDate = onPickDutyDate,
                    onTimeIn = onTimeIn,
                    onRequestCheckOut = onRequestCheckOut,
                    onCancelTimer = onCancelTimer,
                    onOpenManualEntry = onOpenManualEntry,
                    onEditExistingShift = onEditExistingShift,
                    previewElapsedSecondsOverride = previewElapsedSecondsOverride
                )
            }
        }

        // 2. Default Hourly Rate Card
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                DutiesRateSettingsCard(
                    defaultHourlyRate = state.defaultHourlyRate,
                    currencySymbol = state.currencySymbol,
                    onSaveRate = onSaveRate,
                    onShowMessage = onShowMessage
                )
            }
        }

        // 3. Weekly Earnings & Time Breakdown Card
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                TimeBreakdownCard(
                    weeklyTotalEarnings = state.weeklyTotalEarnings,
                    weeklyTotalHours = state.weeklyTotalHours,
                    timeBreakdown = state.weeklyTimeBreakdown,
                    currencySymbol = state.currencySymbol
                )
            }
        }

        // 4. Monthly Shift Calendar
        item {
            Box(modifier = Modifier.padding(horizontal = 20.dp)) {
                ShiftCalendarCard(
                    displayedMonth = state.displayedMonth,
                    loggedEpochDays = state.loggedEpochDays,
                    selectedCalendarDate = state.selectedCalendarDate,
                    onPreviousMonth = onPreviousMonth,
                    onNextMonth = onNextMonth,
                    onSelectDate = onSelectCalendarDate,
                    onClearFilter = onClearCalendarFilter,
                    todayOverride = todayOverride
                )
            }
        }

        // 5. Dark Bottom Sheet Surface for Shift History
        item {
            val filterDate = state.selectedCalendarDate
            val headerTitle = if (filterDate != null) {
                "Shifts (${filterDate.format(filterDateFormatter)})"
            } else {
                "Shift History"
            }

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
                        title = headerTitle,
                        count = state.displayedShifts.size,
                        onLightSurface = true
                    )

                    if (state.displayedShifts.isEmpty()) {
                        ShiftHistoryEmptyCard(
                            isFiltered = filterDate != null,
                            onLogFirstShift = onOpenManualEntry,
                            onClearFilter = onClearCalendarFilter
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
                                    color = DarkBorder.copy(alpha = 0.6f),
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
// High-Fidelity Previews (Default, Timer Running, Manual Sheet, History Filled)
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
        weeklyTotalEarnings = 0.0,
        weeklyTotalHours = 0.0
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onSaveRate = {},
                    onPickDutyDate = {},
                    onTimeIn = {},
                    onRequestCheckOut = {},
                    onCancelTimer = {},
                    onOpenManualEntry = {},
                    onEditExistingShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onClearCalendarFilter = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    todayOverride = today
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Duties },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "2. Duties Timer Running")
@Composable
private fun DutiesTimerRunningPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val state = DutiesUiState(
        activeTimer = ActiveTimerState(
            isRunning = true,
            startTimeMillis = 1_778_900_000_000L,
            shiftDateEpochDay = today.toEpochDay(),
            lockedRate = 96.38
        ),
        defaultHourlyRate = 96.38,
        currencySymbol = "₱",
        selectedDutyDate = today,
        displayedMonth = YearMonth.from(today),
        weeklyTotalEarnings = 1493.89,
        weeklyTotalHours = 15.5,
        weeklyTimeBreakdown = TimeBreakdown(
            regularHours = 12.0,
            nightDiffHours = 2.0,
            overtimeHours = 1.5
        )
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onSaveRate = {},
                    onPickDutyDate = {},
                    onTimeIn = {},
                    onRequestCheckOut = {},
                    onCancelTimer = {},
                    onOpenManualEntry = {},
                    onEditExistingShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onClearCalendarFilter = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    previewElapsedSecondsOverride = 8_130L, // 02:15:30 -> +₱217.66 so far
                    todayOverride = today
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Duties },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "3. Duties Manual Entry Sheet")
@Composable
private fun DutiesManualEntrySheetPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val state = DutiesUiState(
        defaultHourlyRate = 96.38,
        currencySymbol = "₱",
        selectedDutyDate = today,
        displayedMonth = YearMonth.from(today)
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onSaveRate = {},
                    onPickDutyDate = {},
                    onTimeIn = {},
                    onRequestCheckOut = {},
                    onCancelTimer = {},
                    onOpenManualEntry = {},
                    onEditExistingShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onClearCalendarFilter = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    todayOverride = today
                )
                // Scrim + Modal Bottom Sheet content
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
                            ManualShiftSheetContent(
                                selectedDate = today,
                                hourlyRate = 96.38,
                                currencySymbol = "₱",
                                onPickDate = {},
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

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "4. Duties Shift History Filled")
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
        selectedCalendarDate = day1,
        weeklyTotalEarnings = 1821.59,
        weeklyTotalHours = 18.5,
        weeklyTimeBreakdown = TimeBreakdown(
            regularHours = 13.0,
            nightDiffHours = 3.5,
            overtimeHours = 2.0
        ),
        loggedEpochDays = setOf(day1.toEpochDay(), day2.toEpochDay(), day3.toEpochDay()),
        displayedShifts = sampleShifts,
        totalAllShiftsCount = sampleShifts.size
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onSaveRate = {},
                    onPickDutyDate = {},
                    onTimeIn = {},
                    onRequestCheckOut = {},
                    onCancelTimer = {},
                    onOpenManualEntry = {},
                    onEditExistingShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onClearCalendarFilter = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 3),
                    todayOverride = today
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Duties },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}

@Preview(showBackground = false, widthDp = 393, heightDp = 852, name = "5. Duties Empty History Sheet")
@Composable
private fun DutiesEmptyHistoryPreview() {
    val today = LocalDate.of(2026, 5, 16)
    val state = DutiesUiState(
        defaultHourlyRate = 96.38,
        currencySymbol = "₱",
        selectedDutyDate = today,
        displayedMonth = YearMonth.from(today)
    )

    ShiftlyTheme {
        ShiftlyBlueGradientBackground {
            Box(modifier = Modifier.fillMaxSize()) {
                DutiesScreenContent(
                    state = state,
                    onSaveRate = {},
                    onPickDutyDate = {},
                    onTimeIn = {},
                    onRequestCheckOut = {},
                    onCancelTimer = {},
                    onOpenManualEntry = {},
                    onEditExistingShift = {},
                    onPreviousMonth = {},
                    onNextMonth = {},
                    onSelectCalendarDate = {},
                    onClearCalendarFilter = {},
                    onEditShift = {},
                    onDeleteShift = {},
                    listState = rememberLazyListState(initialFirstVisibleItemIndex = 4),
                    todayOverride = today
                )
                FloatingPillBottomBar(
                    items = Screen.bottomNavItems,
                    isSelected = { it == Screen.Duties },
                    onSelect = {},
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    }
}
