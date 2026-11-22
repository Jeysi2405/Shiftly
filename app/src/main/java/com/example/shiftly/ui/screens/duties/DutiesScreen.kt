package com.example.shiftly.ui.screens.duties

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.R
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDialog
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyHelpIconButton
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.components.shiftlySheetBodyBand
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.components.shiftlySheetTopBand
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.InterFontFamily
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
    var showTutorialSheet by remember { mutableStateOf(false) }
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
        onOpenHelp = { showTutorialSheet = true },
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

    if (showTutorialSheet) {
        ShiftlyScreenTutorialSheet(
            title = "Duties & Shifts Guide",
            subtitle = "Log your shifts and let Shiftly calculate your pay.",
            steps = listOf(
                "Tap Log Shift or any calendar day" to "Record your clock-in and clock-out times in seconds using saved presets or custom hours.",
                "Choose the day type" to "Mark ordinary, special, or regular holidays so night differential, overtime, and holiday pay compute automatically.",
                "Set your rates & presets" to "Tap the gear icon anytime to adjust your hourly rate, daily minimum wage, or quick shift presets."
            ),
            onDismiss = { showTutorialSheet = false }
        )
    }

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
        ShiftlyConfirmDialog(
            title = "Edit Shift",
            message = "Are you sure you want to edit the shift for this day?",
            iconRes = R.drawable.ic_lucide_square_pen,
            iconBgColor = colors.azurePrimary,
            noLabel = "No",
            yesLabel = "Yes",
            isDestructive = false,
            onDismiss = { pendingConfirmEditShift = null },
            onConfirm = {
                editingShift = shiftToConfirm
                pendingConfirmEditShift = null
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
        val formattedDate = LocalDate.ofEpochDay(shiftToDelete.dateEpochDay).format(deleteFormatter)
        ShiftlyDeleteConfirmDialog(
            title = "Delete Shift?",
            message = "This logged shift on $formattedDate and its computed earnings will be permanently removed.",
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
    onOpenHelp: () -> Unit = {},
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
        verticalArrangement = Arrangement.Top
    ) {
        // 0. Screen Header with Back, Help & Settings Action
        item(key = "duties_header", contentType = "header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val backInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onBack,
                        interactionSource = backInteraction,
                        modifier = Modifier
                            .size(48.dp)
                            .shiftlyPressEffect(backInteraction)
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
                        style = MaterialTheme.typography.headlineSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textPrimary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    ShiftlyHelpIconButton(onClick = onOpenHelp)
                    val settingsInteraction = remember { MutableInteractionSource() }
                    IconButton(
                        onClick = onOpenSettings,
                        interactionSource = settingsInteraction,
                        modifier = Modifier
                            .size(48.dp)
                            .shiftlyPressEffect(settingsInteraction)
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
        }

        // 1. Monthly Earnings & Time Breakdown Card
        item(key = "time_breakdown", contentType = "summary") {
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
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
        item(key = "shift_calendar", contentType = "calendar") {
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
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

        // 3. Prominent Log Shift Action Button
        item(key = "log_shift_button", contentType = "action_button") {
            Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
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

        // 4. Bottom Sheet Top Cap for Monthly Shift History
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
                    title = "Shift History",
                    count = state.displayedShifts.size,
                    onLightSurface = true
                )
            }
        }

        if (state.displayedShifts.isEmpty()) {
            item(key = "sheet_empty", contentType = "sheet_empty") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 380.dp)
                        .shiftlySheetBodyBand(colors.surface)
                        .padding(start = 20.dp, end = 20.dp, bottom = 104.dp)
                ) {
                    ShiftHistoryEmptyCard(
                        onLogFirstShift = onOpenLogShift
                    )
                }
            }
        } else {
            itemsIndexed(
                items = state.displayedShifts,
                key = { _, shift -> "shift_${shift.id}" },
                contentType = { _, _ -> "shift_item" }
            ) { idx, shift ->
                val rowShape = when {
                    state.displayedShifts.size == 1 -> RoundedCornerShape(18.dp)
                    idx == 0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                    idx == state.displayedShifts.lastIndex -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
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
                            bottom = if (idx < state.displayedShifts.lastIndex) 2.dp else 0.dp
                        )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(colors.cardElevated)
                    ) {
                        ShiftHistoryItemCard(
                            shift = shift,
                            currencySymbol = state.currencySymbol,
                            onEditNote = { onEditShift(shift) },
                            onDelete = { onDeleteShift(shift) },
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
                        .height(if (state.displayedShifts.size <= 1) 220.dp else 104.dp)
                )
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
                        .background(shiftlySheetScrimColor())
                ) {
                    Surface(
                        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                        color = colors.surface,
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
