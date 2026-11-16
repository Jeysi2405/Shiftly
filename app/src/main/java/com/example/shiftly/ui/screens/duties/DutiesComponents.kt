package com.example.shiftly.ui.screens.duties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.ui.components.ShiftlyStatusBadge
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.DeepBlueContainer
import com.example.shiftly.ui.theme.ElectricBlue
import com.example.shiftly.ui.theme.EmeraldContainer
import com.example.shiftly.ui.theme.EmeraldSuccess
import com.example.shiftly.ui.theme.RoseAlert
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SlateSurfaceVariant
import com.example.shiftly.ui.theme.SoftBlue
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.DayType
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.delay

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun DayTypeSelector(
    selectedDayType: DayType,
    onDayTypeSelected: (DayType) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded }
    ) {
        OutlinedTextField(
            value = selectedDayType.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Day Type") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricBlue,
                unfocusedBorderColor = BorderSubtle,
                focusedLabelColor = SoftBlue,
                unfocusedLabelColor = TextSecondary,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(SlateSurface)
        ) {
            DayType.entries.forEach { dayType ->
                DropdownMenuItem(
                    text = { Text(dayType.label, color = TextPrimary) },
                    onClick = {
                        onDayTypeSelected(dayType)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Composable
fun DutiesRateSettingsCard(
    defaultHourlyRate: Double,
    currencySymbol: String,
    onSaveRate: (String) -> Unit,
    onSelectCurrency: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var rateInput by remember { mutableStateOf("") }
    var savedBanner by remember { mutableStateOf(false) }
    val currencies = listOf("₱", "$", "€", "£", "¥")

    LaunchedEffect(defaultHourlyRate) {
        if (rateInput.isEmpty() && defaultHourlyRate > 0.0) {
            rateInput = if (defaultHourlyRate % 1.0 == 0.0) {
                defaultHourlyRate.toInt().toString()
            } else {
                defaultHourlyRate.toString()
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lock,
                    contentDescription = null,
                    tint = ElectricBlue
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Rate & Currency",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }

            // Currency Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                currencies.forEach { symbol ->
                    val selected = currencySymbol == symbol
                    FilterChip(
                        selected = selected,
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onSelectCurrency(symbol)
                            onShowMessage("Currency set to $symbol")
                        },
                        label = {
                            Text(
                                text = symbol,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = SlateSurfaceVariant,
                            labelColor = TextSecondary,
                            selectedContainerColor = DeepBlueContainer,
                            selectedLabelColor = ElectricBlue
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = selected,
                            borderColor = BorderSubtle,
                            selectedBorderColor = ElectricBlue
                        )
                    )
                }
            }

            // Hourly Rate Input + Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = {
                        rateInput = it
                        savedBanner = false
                    },
                    label = { Text("Rate ($currencySymbol/h)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = SoftBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveRate(rateInput)
                        savedBanner = true
                        onShowMessage("Rate saved")
                    },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Outlined.Check, contentDescription = "Save")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Save", fontWeight = FontWeight.SemiBold)
                }
            }

            if (savedBanner) {
                Text(
                    text = "Saved $currencySymbol${"%.2f".format(defaultHourlyRate)}/h",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldSuccess
                )
            }
        }
    }
}

@Composable
fun LiveShiftTimerCard(
    activeTimer: ActiveTimerState,
    selectedDutyDate: LocalDate,
    defaultHourlyRate: Double,
    currencySymbol: String,
    onPickDutyDate: () -> Unit,
    onTimeIn: () -> Unit,
    onRequestCheckOut: () -> Unit,
    onCancelTimer: () -> Unit,
    onOpenManualEntry: () -> Unit
) {
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val haptics = LocalHapticFeedback.current

    var elapsedSeconds by remember(activeTimer.isRunning, activeTimer.startTimeMillis) {
        mutableLongStateOf(
            if (activeTimer.isRunning) {
                ((System.currentTimeMillis() - activeTimer.startTimeMillis) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
        )
    }

    LaunchedEffect(activeTimer.isRunning, activeTimer.startTimeMillis) {
        while (activeTimer.isRunning) {
            elapsedSeconds = ((System.currentTimeMillis() - activeTimer.startTimeMillis) / 1000L)
                .coerceAtLeast(0L)
            delay(1_000L)
        }
    }

    val activeBorder = if (activeTimer.isRunning) ElectricBlue else BorderSubtle

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(if (activeTimer.isRunning) 1.5.dp else 1.dp, activeBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Top Header Row: Status + Locked Rate Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeTimer.isRunning) {
                    ShiftlyStatusBadge(
                        label = "LIVE",
                        containerColor = EmeraldContainer,
                        contentColor = EmeraldSuccess,
                        leadingContent = {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(EmeraldSuccess)
                            )
                        }
                    )
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(ElectricBlue)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "TIMER",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = SoftBlue
                        )
                    }
                }

                // Locked Hourly Rate Badge
                val displayedRate = if (activeTimer.isRunning) activeTimer.lockedRate else defaultHourlyRate
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = DeepBlueContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = SoftBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "$currencySymbol${"%.2f".format(displayedRate)}/h",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftBlue
                        )
                    }
                }
            }

            if (activeTimer.isRunning) {
                // Live Ticking Stopwatch
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val formattedClock = "%02d:%02d:%02d".format(hours, minutes, seconds)
                val liveAccrued = (elapsedSeconds / 3600.0) * activeTimer.lockedRate

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formattedClock,
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = FontFamily.Monospace
                        ).tabular(),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%.2f".format(liveAccrued)}",
                        style = MaterialTheme.typography.titleMedium.tabular(),
                        fontWeight = FontWeight.SemiBold,
                        color = SoftBlue
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelTimer,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Text("Discard", color = TextSecondary)
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRequestCheckOut()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Check Out", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                // Idle State: Duty Date Selector + Time In + Manual Log
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = SlateSurfaceVariant.copy(alpha = 0.55f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onPickDutyDate() }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.CalendarToday,
                                contentDescription = null,
                                tint = ElectricBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = selectedDutyDate.format(dateFormatter),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )
                        }
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = SoftBlue
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onTimeIn()
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Time In", fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenManualEntry,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderSubtle),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = "Manual Shift",
                            tint = SoftBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Manual", color = TextPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun WeeklyEarningsCard(
    weeklyTotalEarnings: Double,
    weeklyTotalHours: Double,
    weeklyBars: List<DailyEarningsBar>,
    currencySymbol: String
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "WEEKLY",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%.2f".format(weeklyTotalEarnings)}",
                        style = MaterialTheme.typography.headlineMedium.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceVariant
                ) {
                    Text(
                        text = "${"%.1f".format(weeklyTotalHours)}h",
                        style = MaterialTheme.typography.labelMedium.tabular(),
                        color = TextSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }

            // 7-Day Mon-Sun Bar Chart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(104.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                weeklyBars.forEach { bar ->
                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Bar Track
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(74.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(SlateSurfaceVariant.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            val barHeightFraction = if (bar.earned > 0.0) {
                                bar.fillFraction.coerceAtLeast(0.12f)
                            } else {
                                0.04f
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .fillMaxHeight(barHeightFraction)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        when {
                                            bar.earned > 0.0 && bar.isToday -> ElectricBlue
                                            bar.earned > 0.0 -> SoftBlue
                                            else -> BorderSubtle
                                        }
                                    )
                            )
                        }

                        Spacer(modifier = Modifier.height(6.dp))

                        Text(
                            text = bar.dayLabel,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = if (bar.isToday) FontWeight.Bold else FontWeight.Normal,
                            color = if (bar.isToday) ElectricBlue else TextSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftCalendarCard(
    displayedMonth: YearMonth,
    loggedEpochDays: Set<Long>,
    selectedCalendarDate: LocalDate?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
    onClearFilter: () -> Unit
) {
    val today = remember { LocalDate.now() }
    val monthTitle = remember(displayedMonth) {
        "${displayedMonth.month.getDisplayName(TextStyle.FULL, Locale.getDefault())} ${displayedMonth.year}"
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Calendar Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (selectedCalendarDate != null) {
                        TextButton(onClick = onClearFilter) {
                            Text("Reset", color = SoftBlue, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                    IconButton(onClick = onPreviousMonth, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous Month", tint = TextSecondary)
                    }
                    IconButton(onClick = onNextMonth, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next Month", tint = TextSecondary)
                    }
                }
            }

            // Day of Week Headers (Mon - Sun)
            val weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(modifier = Modifier.fillMaxWidth()) {
                weekHeaders.forEach { day ->
                    Text(
                        text = day,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = TextSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Calendar Cells Grid
            val firstDayOfMonth = displayedMonth.atDay(1)
            val startOffset = firstDayOfMonth.dayOfWeek.value - 1 // Monday = 0 .. Sunday = 6
            val daysInMonth = displayedMonth.lengthOfMonth()
            val totalCells = startOffset + daysInMonth
            val rowCount = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                for (row in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - startOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = displayedMonth.atDay(dayNumber)
                                val hasLoggedShift = loggedEpochDays.contains(cellDate.toEpochDay())
                                val isSelected = selectedCalendarDate == cellDate
                                val isToday = cellDate == today

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(
                                            when {
                                                isSelected -> ElectricBlue
                                                hasLoggedShift -> DeepBlueContainer
                                                else -> SlateSurfaceVariant.copy(alpha = 0.25f)
                                            }
                                        )
                                        .then(
                                            if (isToday && !isSelected) {
                                                Modifier.border(1.dp, ElectricBlue, RoundedCornerShape(10.dp))
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onSelectDate(cellDate) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = if (isSelected || hasLoggedShift || isToday) {
                                                FontWeight.Bold
                                            } else {
                                                FontWeight.Normal
                                            },
                                            color = when {
                                                isSelected -> TextPrimary
                                                hasLoggedShift -> SoftBlue
                                                else -> TextSecondary
                                            }
                                        )
                                        if (hasLoggedShift) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(4.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) TextPrimary else ElectricBlue)
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHistoryItemCard(
    shift: ShiftEntity,
    currencySymbol: String,
    onEditNote: () -> Unit,
    onDelete: () -> Unit
) {
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("EEE, MMM d") }
    val timeFormatter = remember { DateTimeFormatter.ofPattern("hh:mm a") }

    val shiftDateText = remember(shift.dateEpochDay) {
        LocalDate.ofEpochDay(shift.dateEpochDay).format(dateFormatter)
    }
    val timeRangeText = remember(shift.timeInMillis, shift.timeOutMillis) {
        val start = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime().format(timeFormatter)
        val end = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime().format(timeFormatter)
        "$start – $end"
    }

    val hours = shift.durationMinutes / 60
    val mins = shift.durationMinutes % 60
    val durationLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = shiftDateText,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$timeRangeText ($durationLabel)",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currencySymbol${"%.2f".format(shift.totalEarned)}",
                        style = MaterialTheme.typography.titleMedium.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = (runCatching { DayType.valueOf(shift.dayType).label }.getOrNull() ?: shift.dayType),
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary
                    )
                }
            }

            // Optional Note & Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (shift.overtimeNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DeepBlueContainer.copy(alpha = 0.7f),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(
                            text = shift.overtimeNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = SoftBlue,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(1f))
                }

                Row {
                    IconButton(onClick = onEditNote, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "Edit Note",
                            tint = SoftBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Shift",
                            tint = RoseAlert.copy(alpha = 0.85f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
