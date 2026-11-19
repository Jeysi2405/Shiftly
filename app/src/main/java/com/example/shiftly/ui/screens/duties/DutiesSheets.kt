package com.example.shiftly.ui.screens.duties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.WageCalculator
import com.example.shiftly.utils.WageDetails
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

private fun to12HourParts(hour24: Int): Pair<Int, Boolean> {
    val normalized = ((hour24 % 24) + 24) % 24
    val isPm = normalized >= 12
    val hour12 = when {
        normalized == 0 -> 12
        normalized > 12 -> normalized - 12
        else -> normalized
    }
    return hour12 to isPm
}

private fun format12HourTime(time: LocalTime): String {
    val (h12, isPm) = to12HourParts(time.hour)
    val amPm = if (isPm) "PM" else "AM"
    return String.format(java.util.Locale.US, "%02d:%02d %s", h12, time.minute, amPm)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CompactShiftScheduleGrid(
    date: LocalDate,
    startHour24: Int,
    startMinute: Int,
    endHour24: Int,
    endMinute: Int,
    selectedDayType: DayType,
    onDateChanged: (LocalDate) -> Unit,
    onStartTimeChanged: (Int, Int) -> Unit,
    onEndTimeChanged: (Int, Int) -> Unit,
    onDayTypeClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startLocal = remember(startHour24, startMinute) { LocalTime.of(startHour24, startMinute) }
    val endLocal = remember(endHour24, endMinute) { LocalTime.of(endHour24, endMinute) }
    val endsNextDay = !endLocal.isAfter(startLocal)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        // Row 1: Date | Day Type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_calendar),
                        contentDescription = null,
                        tint = colors.azureAccent,
                        modifier = Modifier.size(17.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Date",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onDayTypeClick)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(1.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Text(
                                text = "Day Type",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colors.textSecondary,
                                maxLines = 1
                            )
                            selectedDayType.holidayBadge?.let { badge ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.coralContainer,
                                    border = BorderStroke(1.dp, colors.coralBright.copy(alpha = 0.45f)),
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp
                                        ),
                                        color = colors.coralInk,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = selectedDayType.label,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_right),
                        contentDescription = "Choose day type",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Row 2: Start Time | End Time
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showStartTimePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_clock),
                        contentDescription = null,
                        tint = colors.azureAccent,
                        modifier = Modifier.size(17.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = "Start time",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = format12HourTime(startLocal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showEndTimePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_clock),
                        contentDescription = null,
                        tint = colors.azureAccent,
                        modifier = Modifier.size(17.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                        Text(
                            text = if (endsNextDay) "End (+1 day)" else "End time",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ),
                            color = if (endsNextDay) colors.azureAccent else colors.textSecondary,
                            maxLines = 1
                        )
                        Text(
                            text = format12HourTime(endLocal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            onDateChanged(
                                Instant.ofEpochMilli(millis)
                                    .atZone(ZoneOffset.UTC)
                                    .toLocalDate()
                            )
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.azureAccent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textSecondary
                    )
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary,
                    headlineContentColor = colors.textPrimary,
                    weekdayContentColor = colors.textSecondary,
                    subheadContentColor = colors.textSecondary,
                    yearContentColor = colors.textPrimary,
                    currentYearContentColor = colors.azureAccent,
                    selectedYearContentColor = colors.onAzurePrimary,
                    selectedYearContainerColor = colors.azurePrimary,
                    dayContentColor = colors.textPrimary,
                    disabledDayContentColor = colors.textMuted,
                    selectedDayContentColor = colors.onAzurePrimary,
                    selectedDayContainerColor = colors.azurePrimary,
                    todayContentColor = colors.azureAccent,
                    todayDateBorderColor = colors.azureAccent
                )
            )
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour24,
            initialMinute = startMinute,
            is24Hour = false
        )
        ShiftTimePickerDialog(
            timePickerState = timePickerState,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                onStartTimeChanged(timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour24,
            initialMinute = endMinute,
            is24Hour = false
        )
        ShiftTimePickerDialog(
            timePickerState = timePickerState,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                onEndTimeChanged(timePickerState.hour, timePickerState.minute)
                showEndTimePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ShiftTimePickerDialog(
    timePickerState: androidx.compose.material3.TimePickerState,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = ShiftlyColors.current
    DatePickerDialog(
        onDismissRequest = onDismiss,
        tonalElevation = 0.dp,
        colors = DatePickerDefaults.colors(containerColor = colors.surface),
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(
                    text = "OK",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.azureAccent
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textSecondary
                )
            }
        }
    ) {
        Box(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            TimePicker(
                state = timePickerState,
                colors = TimePickerDefaults.colors(
                    clockDialColor = colors.card,
                    clockDialSelectedContentColor = colors.onAzurePrimary,
                    clockDialUnselectedContentColor = colors.textSecondary,
                    selectorColor = colors.azurePrimary,
                    containerColor = colors.surface,
                    periodSelectorBorderColor = colors.border,
                    periodSelectorSelectedContainerColor = colors.azurePrimary,
                    periodSelectorUnselectedContainerColor = colors.surface,
                    periodSelectorSelectedContentColor = colors.onAzurePrimary,
                    periodSelectorUnselectedContentColor = colors.textSecondary,
                    timeSelectorSelectedContainerColor = colors.cardElevated,
                    timeSelectorUnselectedContainerColor = colors.surface,
                    timeSelectorSelectedContentColor = colors.textPrimary,
                    timeSelectorUnselectedContentColor = colors.textSecondary
                )
            )
        }
    }
}

@Composable
private fun ShiftTimePresetsStrip(
    presets: List<ShiftTimePreset>,
    currentStartHour24: Int,
    currentStartMinute: Int,
    currentEndHour24: Int,
    currentEndMinute: Int,
    onSelectPreset: (ShiftTimePreset) -> Unit,
    onSaveCurrentAsPreset: () -> Unit
) {
    val colors = ShiftlyColors.current
    val alreadyExists = remember(
        presets,
        currentStartHour24,
        currentStartMinute,
        currentEndHour24,
        currentEndMinute
    ) {
        presets.any {
            it.startHour24 == currentStartHour24 &&
                it.startMinute == currentStartMinute &&
                it.endHour24 == currentEndHour24 &&
                it.endMinute == currentEndMinute
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Presets",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textSecondary
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = if (alreadyExists) colors.card else colors.azureContainer,
                border = BorderStroke(
                    1.dp,
                    if (alreadyExists) colors.cardBorder else colors.azureAccent.copy(alpha = 0.50f)
                ),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = !alreadyExists, onClick = onSaveCurrentAsPreset)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        painter = painterResource(
                            if (alreadyExists) R.drawable.ic_lucide_check else R.drawable.ic_lucide_plus
                        ),
                        contentDescription = null,
                        tint = if (alreadyExists) colors.mintTextOnSurface else colors.azureAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = if (alreadyExists) "Saved" else "Save Time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (alreadyExists) colors.mintTextOnSurface else colors.azureAccent
                    )
                }
            }
        }

        if (presets.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isActive = preset.startHour24 == currentStartHour24 &&
                        preset.startMinute == currentStartMinute &&
                        preset.endHour24 == currentEndHour24 &&
                        preset.endMinute == currentEndMinute

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive) colors.azureContainer else colors.card,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) colors.azureAccent else colors.cardBorder
                        ),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onSelectPreset(preset) }
                            .semantics(mergeDescendants = true) {
                                selected = isActive
                                contentDescription = "Preset shift ${preset.label}"
                            }
                    ) {
                        Text(
                            text = preset.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                            ).tabular(),
                            color = if (isActive) colors.textPrimary else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LogShiftReceiptPreviewCard(
    totalMinutes: Long,
    currencySymbol: String,
    wageDetails: WageDetails
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 11.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Est. Earnings",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = formatDurationCompact(totalMinutes),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Text(
                    text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.totalEarned)}",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 22.sp
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.cardBorder),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = "Reg ${formatDurationCompact(wageDetails.regularBaseMinutes)} • ${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.regularPay)}",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium
                        ).tabular(),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                    )
                }

                if (wageDetails.nightDiffMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.nightDiffPurpleContainer,
                        border = BorderStroke(1.dp, colors.nightDiffPurple.copy(alpha = 0.45f)),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "ND ${formatDurationCompact(wageDetails.nightDiffMinutes)} • +${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.nightDiffPay)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.nightDiffPurple,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }

                if (wageDetails.overtimeMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.coralContainer,
                        border = BorderStroke(1.dp, colors.coralBright.copy(alpha = 0.45f)),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "OT ${formatDurationCompact(wageDetails.overtimeMinutes)} • +${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.overtimePay)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.coralBright,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualShiftBottomSheet(
    selectedDate: LocalDate,
    hourlyRate: Double,
    currencySymbol: String,
    presets: List<ShiftTimePreset> = ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS,
    title: String = "Log Shift",
    confirmButtonText: String = "Save Shift",
    initialStartHour24: Int = 14,
    initialStartMinute: Int = 0,
    initialEndHour24: Int = 18,
    initialEndMinute: Int = 0,
    initialNote: String = "",
    initialDayType: DayType = DayType.ORDINARY,
    showPresets: Boolean = true,
    blockedEpochDays: Set<Long> = emptySet(),
    onSwitchToEdit: ((LocalDate) -> Unit)? = null,
    onSavePreset: (startHour24: Int, startMinute: Int, endHour24: Int, endMinute: Int) -> Unit = { _, _, _, _ -> },
    onDeletePreset: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        date: LocalDate,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        note: String,
        dayType: DayType
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        ManualShiftSheetContent(
            selectedDate = selectedDate,
            hourlyRate = hourlyRate,
            currencySymbol = currencySymbol,
            presets = presets,
            title = title,
            confirmButtonText = confirmButtonText,
            initialStartHour24 = initialStartHour24,
            initialStartMinute = initialStartMinute,
            initialEndHour24 = initialEndHour24,
            initialEndMinute = initialEndMinute,
            initialNote = initialNote,
            initialDayType = initialDayType,
            showPresets = showPresets,
            blockedEpochDays = blockedEpochDays,
            onSwitchToEdit = onSwitchToEdit,
            onSavePreset = onSavePreset,
            onDeletePreset = onDeletePreset,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun ManualShiftSheetContent(
    selectedDate: LocalDate,
    hourlyRate: Double,
    currencySymbol: String,
    presets: List<ShiftTimePreset> = ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS,
    title: String = "Log Shift",
    confirmButtonText: String = "Save Shift",
    initialStartHour24: Int = 14,
    initialStartMinute: Int = 0,
    initialEndHour24: Int = 18,
    initialEndMinute: Int = 0,
    initialNote: String = "",
    initialDayType: DayType = DayType.ORDINARY,
    showPresets: Boolean = true,
    blockedEpochDays: Set<Long> = emptySet(),
    onSwitchToEdit: ((LocalDate) -> Unit)? = null,
    onSavePreset: (startHour24: Int, startMinute: Int, endHour24: Int, endMinute: Int) -> Unit = { _, _, _, _ -> },
    onDeletePreset: (String) -> Unit = {},
    onDismiss: () -> Unit,
    onSave: (
        date: LocalDate,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        note: String,
        dayType: DayType
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    var pickedDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    var startHour24 by remember(initialStartHour24) { mutableIntStateOf(initialStartHour24.coerceIn(0, 23)) }
    var startMin by remember(initialStartMinute) { mutableIntStateOf(initialStartMinute.coerceIn(0, 59)) }
    var endHour24 by remember(initialEndHour24) { mutableIntStateOf(initialEndHour24.coerceIn(0, 23)) }
    var endMin by remember(initialEndMinute) { mutableIntStateOf(initialEndMinute.coerceIn(0, 59)) }
    var manualNote by remember(initialNote) { mutableStateOf(initialNote) }
    var selectedDayType by remember(initialDayType) { mutableStateOf(initialDayType) }
    var showDayTypeSheet by remember { mutableStateOf(false) }

    val sheetFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.azureAccent,
        unfocusedBorderColor = colors.border,
        focusedLabelColor = colors.azureAccent,
        unfocusedLabelColor = colors.textSecondary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.azureAccent,
        focusedContainerColor = colors.card,
        unfocusedContainerColor = colors.card
    )

    val zone = remember { ZoneId.systemDefault() }
    val computedShiftPreview = remember(
        pickedDate,
        startHour24,
        startMin,
        endHour24,
        endMin,
        hourlyRate,
        selectedDayType,
        zone
    ) {
        val startDateTime = pickedDate.atTime(LocalTime.of(startHour24, startMin))
        var endDateTime = pickedDate.atTime(LocalTime.of(endHour24, endMin))
        if (!endDateTime.isAfter(startDateTime)) {
            endDateTime = endDateTime.plusDays(1)
        }
        val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(zone).toInstant().toEpochMilli()
        val totalMinutes = ((endMillis - startMillis) / 60_000L).coerceAtLeast(0L)
        val wageDetails = WageCalculator.calculateTotalEarned(
            startMillis = startMillis,
            endMillis = endMillis,
            hourlyRate = hourlyRate,
            dayTypeStr = selectedDayType.name
        )
        totalMinutes to wageDetails
    }
    val totalMinutes = computedShiftPreview.first
    val wageDetails = computedShiftPreview.second
    val isDateAlreadyLogged = remember(pickedDate, blockedEpochDays) {
        pickedDate.toEpochDay() in blockedEpochDays
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 22.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textSecondary
                )
            }
        }

        if (showPresets) {
            ShiftTimePresetsStrip(
                presets = presets,
                currentStartHour24 = startHour24,
                currentStartMinute = startMin,
                currentEndHour24 = endHour24,
                currentEndMinute = endMin,
                onSelectPreset = { preset ->
                    startHour24 = preset.startHour24
                    startMin = preset.startMinute
                    endHour24 = preset.endHour24
                    endMin = preset.endMinute
                },
                onSaveCurrentAsPreset = {
                    onSavePreset(startHour24, startMin, endHour24, endMin)
                }
            )
        }

        CompactShiftScheduleGrid(
            date = pickedDate,
            startHour24 = startHour24,
            startMinute = startMin,
            endHour24 = endHour24,
            endMinute = endMin,
            selectedDayType = selectedDayType,
            onDateChanged = { pickedDate = it },
            onStartTimeChanged = { h, m ->
                startHour24 = h
                startMin = m
            },
            onEndTimeChanged = { h, m ->
                endHour24 = h
                endMin = m
            },
            onDayTypeClick = { showDayTypeSheet = true }
        )

        if (isDateAlreadyLogged) {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.coralContainer,
                border = BorderStroke(1.dp, colors.coralBright.copy(alpha = 0.45f)),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "A shift is already logged for this date.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.coralInk,
                        modifier = Modifier.weight(1f)
                    )
                    if (onSwitchToEdit != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.surface,
                            border = BorderStroke(1.dp, colors.coralBright.copy(alpha = 0.45f)),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .padding(start = 8.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable { onSwitchToEdit(pickedDate) }
                        ) {
                            Text(
                                text = "Edit Existing",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.coralInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                            )
                        }
                    }
                }
            }
        }

        OutlinedTextField(
            value = manualNote,
            onValueChange = { manualNote = it },
            label = { Text("Shift Note (optional)", fontFamily = PlusJakartaSans) },
            placeholder = {
                Text(
                    text = "e.g., Campus Café Shift",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textMuted
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = sheetFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        LogShiftReceiptPreviewCard(
            totalMinutes = totalMinutes,
            currencySymbol = currencySymbol,
            wageDetails = wageDetails
        )

        ShiftlyPrimaryPillButton(
            text = confirmButtonText,
            enabled = !isDateAlreadyLogged,
            onClick = {
                onSave(
                    pickedDate,
                    startHour24,
                    startMin,
                    endHour24,
                    endMin,
                    manualNote.trim(),
                    selectedDayType
                )
            }
        )
    }

    if (showDayTypeSheet) {
        DayTypePromptSheet(
            selectedDayType = selectedDayType,
            onSelect = { selectedDayType = it },
            onDismiss = { showDayTypeSheet = false }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DutiesSettingsSheet(
    defaultHourlyRate: Double,
    minimumWagePerDay: Double,
    currencySymbol: String,
    presets: List<ShiftTimePreset>,
    onSaveWageRates: (hourlyRate: Double, minimumWagePerDay: Double) -> Unit,
    onShowMessage: (String) -> Unit,
    onSavePreset: (startHour24: Int, startMinute: Int, endHour24: Int, endMinute: Int) -> Unit,
    onDeletePreset: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    var startHour24 by remember { mutableIntStateOf(14) }
    var startMin by remember { mutableIntStateOf(0) }
    var endHour24 by remember { mutableIntStateOf(18) }
    var endMin by remember { mutableIntStateOf(0) }

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Duties Settings",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = PlusJakartaSans),
                    color = colors.textPrimary
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textSecondary
                    )
                }
            }

            DutiesWageRateCard(
                defaultHourlyRate = defaultHourlyRate,
                minimumWagePerDay = minimumWagePerDay,
                currencySymbol = currencySymbol,
                onSaveRates = onSaveWageRates,
                onShowMessage = onShowMessage
            )

            HorizontalDivider(color = colors.border)

            Text(
                text = "Shift Time Presets",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans),
                color = colors.textPrimary
            )

            Text(
                text = "Saved Presets",
                style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                color = colors.textSecondary
            )

            if (presets.isEmpty()) {
                Text(
                    text = "No saved shift presets.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    presets.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.cardBorder),
                            shadowElevation = 0.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = colors.textPrimary
                                )
                                IconButton(
                                    onClick = { onDeletePreset(preset.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_trash_2),
                                        contentDescription = "Delete preset",
                                        tint = colors.coralBright,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = colors.border)

            Text(
                text = "Add New Preset",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans),
                color = colors.textPrimary
            )

            BasicPresetTimeSelectorCard(
                startHour24 = startHour24,
                startMinute = startMin,
                endHour24 = endHour24,
                endMinute = endMin,
                onStartTimeChanged = { h, m ->
                    startHour24 = h
                    startMin = m
                },
                onEndTimeChanged = { h, m ->
                    endHour24 = h
                    endMin = m
                }
            )

            ShiftlyPrimaryPillButton(
                text = "Save New Preset",
                onClick = {
                    onSavePreset(startHour24, startMin, endHour24, endMin)
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicPresetTimeSelectorCard(
    startHour24: Int,
    startMinute: Int,
    endHour24: Int,
    endMinute: Int,
    onStartTimeChanged: (Int, Int) -> Unit,
    onEndTimeChanged: (Int, Int) -> Unit
) {
    val colors = ShiftlyColors.current
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startLocal = remember(startHour24, startMinute) { LocalTime.of(startHour24, startMinute) }
    val endLocal = remember(endHour24, endMinute) { LocalTime.of(endHour24, endMinute) }
    val endsNextDay = !endLocal.isAfter(startLocal)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.cardBorder),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showStartTimePicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_clock),
                    contentDescription = null,
                    tint = colors.azureAccent,
                    modifier = Modifier.size(17.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "Start time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = format12HourTime(startLocal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.cardBorder),
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showEndTimePicker = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(9.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_clock),
                    contentDescription = null,
                    tint = colors.azureAccent,
                    modifier = Modifier.size(17.dp)
                )
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = if (endsNextDay) "End (+1 day)" else "End time",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Medium
                        ),
                        color = if (endsNextDay) colors.azureAccent else colors.textSecondary,
                        maxLines = 1
                    )
                    Text(
                        text = format12HourTime(endLocal),
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                }
            }
        }
    }

    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour24,
            initialMinute = startMinute,
            is24Hour = false
        )
        ShiftTimePickerDialog(
            timePickerState = timePickerState,
            onDismiss = { showStartTimePicker = false },
            onConfirm = {
                onStartTimeChanged(timePickerState.hour, timePickerState.minute)
                showStartTimePicker = false
            }
        )
    }

    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour24,
            initialMinute = endMinute,
            is24Hour = false
        )
        ShiftTimePickerDialog(
            timePickerState = timePickerState,
            onDismiss = { showEndTimePicker = false },
            onConfirm = {
                onEndTimeChanged(timePickerState.hour, timePickerState.minute)
                showEndTimePicker = false
            }
        )
    }
}
