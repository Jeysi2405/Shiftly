package com.example.shiftly.ui.screens.duties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyConfirmDeleteDialog
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.ElectricBlue
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SoftBlue
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary
import java.time.Instant
import java.time.LocalDate
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

    var showDutyDatePicker by remember { mutableStateOf(false) }
    var showCheckOutDialog by remember { mutableStateOf(false) }
    var showManualShiftDialog by remember { mutableStateOf(false) }
    var editingNoteShift by remember { mutableStateOf<ShiftEntity?>(null) }
    var pendingDeleteShift by remember { mutableStateOf<ShiftEntity?>(null) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        item {
            ShiftlyScreenHeader(
                title = "Duties"
            )
        }

        // 1. Live Time-In / Check-Out Timer Card
        item {
            LiveShiftTimerCard(
                activeTimer = state.activeTimer,
                selectedDutyDate = state.selectedDutyDate,
                defaultHourlyRate = state.defaultHourlyRate,
                currencySymbol = state.currencySymbol,
                onPickDutyDate = { showDutyDatePicker = true },
                onTimeIn = { viewModel.startTimer() },
                onRequestCheckOut = { showCheckOutDialog = true },
                onCancelTimer = { viewModel.cancelActiveTimer() },
                onOpenManualEntry = { showManualShiftDialog = true }
            )
        }

        // 2. Weekly Earnings Bar Chart Summary
        item {
            WeeklyEarningsCard(
                weeklyTotalEarnings = state.weeklyTotalEarnings,
                weeklyTotalHours = state.weeklyTotalHours,
                weeklyBars = state.weeklyBars,
                currencySymbol = state.currencySymbol
            )
        }

        // 3. Monthly Calendar for Logged Shift Dates
        item {
            ShiftCalendarCard(
                displayedMonth = state.displayedMonth,
                loggedEpochDays = state.loggedEpochDays,
                selectedCalendarDate = state.selectedCalendarDate,
                onPreviousMonth = { viewModel.changeCalendarMonth(-1) },
                onNextMonth = { viewModel.changeCalendarMonth(1) },
                onSelectDate = { viewModel.toggleCalendarDateFilter(it) },
                onClearFilter = { viewModel.clearCalendarFilter() }
            )
        }

        // 4. Logged Shift Duties List Header
        item {
            val filterDate = state.selectedCalendarDate
            val headerTitle = if (filterDate != null) {
                "Shifts (${filterDate.format(DateTimeFormatter.ofPattern("MMM d"))})"
            } else {
                "Shift History"
            }

            ShiftlySectionHeader(
                title = headerTitle,
                count = state.displayedShifts.size
            )
        }

        if (state.displayedShifts.isEmpty()) {
            item {
                ShiftlyEmptyState(
                    icon = Icons.Outlined.WorkHistory,
                    title = "No shifts"
                )
            }
        } else {
            items(
                items = state.displayedShifts,
                key = { it.id }
            ) { shift ->
                ShiftHistoryItemCard(
                    shift = shift,
                    currencySymbol = state.currencySymbol,
                    onEditNote = { editingNoteShift = shift },
                    onDelete = { pendingDeleteShift = shift }
                )
            }
        }
    }

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
                    Text("Select", color = ElectricBlue, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDutyDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // Check-Out & Overtime Note Dialog
    if (showCheckOutDialog) {
        var noteText by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCheckOutDialog = false },
            containerColor = SlateSurface,
            title = {
                Text("Check Out", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Note") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = SoftBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.checkOutAndSave(noteText)
                        showCheckOutDialog = false
                        onShowMessage("Shift saved")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCheckOutDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Edit Existing Shift Overtime Note Dialog
    editingNoteShift?.let { targetShift ->
        var updatedNote by remember(targetShift.id) { mutableStateOf(targetShift.overtimeNote) }

        AlertDialog(
            onDismissRequest = { editingNoteShift = null },
            containerColor = SlateSurface,
            title = {
                Text("Shift Note", color = TextPrimary, fontWeight = FontWeight.Bold)
            },
            text = {
                OutlinedTextField(
                    value = updatedNote,
                    onValueChange = { updatedNote = it },
                    label = { Text("Note") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = SoftBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateOvertimeNote(targetShift, updatedNote)
                        editingNoteShift = null
                        onShowMessage("Note updated")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Update", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingNoteShift = null }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    // Delete Shift Confirmation Dialog
    pendingDeleteShift?.let { shiftToDelete ->
        ShiftlyConfirmDeleteDialog(
            title = "Delete Shift",
            message = "Remove shift on ${
                LocalDate.ofEpochDay(shiftToDelete.dateEpochDay)
                    .format(DateTimeFormatter.ofPattern("MMM d"))
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

    // Quick Manual Past Shift Dialog
    if (showManualShiftDialog) {
        var startHour by remember { mutableStateOf("9") }
        var startMin by remember { mutableStateOf("00") }
        var endHour by remember { mutableStateOf("17") }
        var endMin by remember { mutableStateOf("00") }
        var manualNote by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showManualShiftDialog = false },
            containerColor = SlateSurface,
            title = {
                Text(
                    text = "Manual Shift (${state.selectedDutyDate.format(DateTimeFormatter.ofPattern("MMM d"))})",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = startHour,
                            onValueChange = { startHour = it.take(2) },
                            label = { Text("In Hr") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = startMin,
                            onValueChange = { startMin = it.take(2) },
                            label = { Text("In Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = endHour,
                            onValueChange = { endHour = it.take(2) },
                            label = { Text("Out Hr") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = endMin,
                            onValueChange = { endMin = it.take(2) },
                            label = { Text("Out Min") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    OutlinedTextField(
                        value = manualNote,
                        onValueChange = { manualNote = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val sH = (startHour.toIntOrNull() ?: 9).coerceIn(0, 23)
                        val sM = (startMin.toIntOrNull() ?: 0).coerceIn(0, 59)
                        val eH = (endHour.toIntOrNull() ?: 17).coerceIn(0, 23)
                        val eM = (endMin.toIntOrNull() ?: 0).coerceIn(0, 59)
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        viewModel.logManualShift(
                            date = state.selectedDutyDate,
                            startHour = sH,
                            startMinute = sM,
                            endHour = eH,
                            endMinute = eM,
                            overtimeNote = manualNote
                        )
                        showManualShiftDialog = false
                        onShowMessage("Shift logged")
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showManualShiftDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }
}
