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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyBlueGradientBackground
import com.example.shiftly.ui.components.ShiftlyConfirmDialog
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyHelpIconButton
import com.example.shiftly.ui.components.ShiftlyScreenTutorialSheet
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySectionHeader
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySubScreenTopBar
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.navigation.FloatingPillBottomBar
import com.example.shiftly.ui.navigation.Screen
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
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
            subtitle = null,
            steps = listOf(
                "Log shift or pick a day" to "Tap Log Shift or any calendar date to set times using Presets or custom hours.",
                "Day types & auto math" to "Choose Regular Day, Rest Day, or holiday acronyms (SNW Day, RH + Rest) for automatic OT & ND pay.",
                "Rates, presets & swipe" to "Tap the gear icon to edit Hourly Rate and Presets, or swipe shift cards right to edit and left to delete."
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
            message = "Edit this shift?",
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
            message = "Delete shift on $formattedDate?",
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
            title = "Duties",
            onBack = onBack,
            showDivider = isScrolled,
            trailing = {
                ShiftlyHelpIconButton(onClick = onOpenHelp)
                val settingsInteraction = remember { MutableInteractionSource() }
                var settingsTapCount by remember { mutableIntStateOf(0) }
                IconButton(
                    onClick = {
                        settingsTapCount++
                        onOpenSettings()
                    },
                    interactionSource = settingsInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .shiftlyPressEffect(settingsInteraction)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_settings,
                        contentDescription = "Duties Settings",
                        tint = colors.textPrimary,
                        triggerKey = settingsTapCount,
                        modifier = Modifier.size(21.dp)
                    )
                }
                ShiftlySecondaryTextButton(
                    text = "Log",
                    onClick = onOpenLogShift,
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
            // 1. Monthly Earnings & Time Breakdown Card
            item(key = "time_breakdown", contentType = "summary") {
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 14.dp)) {
                    TimeBreakdownCard(
                        monthlyTotalEarnings = state.monthlyTotalEarnings,
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
                Box(modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 16.dp)) {
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

            // 3. Monthly Shift History Header
            item(key = "sheet_header", contentType = "sheet_header") {
                ShiftlySectionHeader(
                    title = "Shift History",
                    count = state.displayedShifts.size,
                    onLightSurface = false,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp)
                )
            }

            if (state.displayedShifts.isEmpty()) {
                item(key = "sheet_empty", contentType = "sheet_empty") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
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
                        state.displayedShifts.size == 1 -> ShiftlyShapes.Card
                        idx == 0 -> RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
                        idx == state.displayedShifts.lastIndex -> RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
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
                        ShiftHistoryItemCard(
                            shift = shift,
                            currencySymbol = state.currencySymbol,
                            onEditNote = { onEditShift(shift) },
                            onDelete = { onDeleteShift(shift) },
                            containerColor = Color.Transparent,
                            shape = RectangleShape
                        )
                        if (idx < state.displayedShifts.lastIndex) {
                            ShiftlyHairlineDivider(startInset = 16.dp)
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
                        .background(shiftlySheetScrimColor())
                ) {
                    Surface(
                        shape = ShiftlyShapes.Sheet,
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
