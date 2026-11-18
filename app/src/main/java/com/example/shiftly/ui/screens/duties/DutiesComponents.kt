package com.example.shiftly.ui.screens.duties

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.WorkHistory
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.WageCalculator
import com.example.shiftly.utils.WageDetails
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.Month
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlin.math.abs

private enum class ActiveWheelSection {
    NONE,
    DATE,
    START_TIME,
    END_TIME
}

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

private fun to24Hour(hour12: Int, isPm: Boolean): Int {
    val safe12 = hour12.coerceIn(1, 12)
    return if (isPm) {
        if (safe12 == 12) 12 else safe12 + 12
    } else {
        if (safe12 == 12) 0 else safe12
    }
}

private fun formatDurationCompact(totalMinutes: Long): String {
    val safe = totalMinutes.coerceAtLeast(0L)
    val h = safe / 60L
    val m = safe % 60L
    return when {
        h > 0L && m > 0L -> "${h}h ${m}m"
        h > 0L -> "${h}h"
        else -> "${m}m"
    }
}

private fun formatPillDuration(totalMinutes: Long): String {
    val safe = totalMinutes.coerceAtLeast(0L)
    val h = safe / 60L
    val m = safe % 60L
    return when {
        h > 0L && m > 0L -> if (h == 1L) "${h} hour ${m} mins" else "${h} hours ${m} mins"
        h > 0L -> if (h == 1L) "${h} hour" else "${h} hours"
        else -> "${m} mins"
    }
}

@Composable
fun DayTypeSelectorRow(
    selectedDayType: DayType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Day Type",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                    color = colors.textSecondary
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = selectedDayType.label,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
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
                                    fontFamily = ShiftlySans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.coralInk,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Choose day type",
                tint = colors.textSecondary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayTypePromptSheet(
    selectedDayType: DayType,
    onSelect: (DayType) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Day Type",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = ShiftlySerif),
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            DayType.orderedForSelection.forEach { dayType ->
                val isSelected = dayType == selectedDayType
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) colors.azureContainer else colors.card,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colors.azureAccent else colors.border
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.RadioButton) {
                            onSelect(dayType)
                            onDismiss()
                        }
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = dayType.label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary
                            )
                            dayType.holidayBadge?.let { badge ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.coralContainer,
                                    border = BorderStroke(1.dp, colors.coralBright.copy(alpha = 0.45f)),
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = ShiftlySans,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.coralInk,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(colors.azurePrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = colors.onAzurePrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatHoursLabel(hours: Double): String {
    val numStr = if (hours % 1.0 == 0.0) "${hours.toInt()}" else String.format(java.util.Locale.US, "%.1f", hours)
    return if (hours == 1.0) "$numStr hour" else "$numStr hours"
}

@Composable
fun TimeBreakdownCard(
    monthlyTotalEarnings: Double,
    monthlyTotalHours: Double,
    timeBreakdown: TimeBreakdown,
    currencySymbol: String,
    displayedMonth: YearMonth = YearMonth.now()
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = remember(displayedMonth, locale) {
        displayedMonth.month.getDisplayName(TextStyle.FULL, locale)
    }
    val headerTitle = "$monthLabel Summary"

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalEarnings),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ).tabular(),
                        color = colors.textPrimary
                    )
                }
            }

            // Regular = Primary, Night Diff = Purple, Overtime = Red
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeBreakdownTile(
                    label = "Regular",
                    hours = timeBreakdown.regularHours,
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                TimeBreakdownTile(
                    label = "Night Diff",
                    hours = timeBreakdown.nightDiffHours,
                    valueColor = colors.nightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                TimeBreakdownTile(
                    label = "Overtime",
                    hours = timeBreakdown.overtimeHours,
                    valueColor = colors.coralBright,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun TimeBreakdownTile(
    label: String,
    hours: Double,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(colors.card)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = ShiftlyFormatters.formatHours(hours, compact = true),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = ShiftlySerif,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
    onClearFilter: () -> Unit,
    todayOverride: LocalDate? = null
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember(todayOverride) { todayOverride ?: LocalDate.now() }
    val monthTitle = remember(displayedMonth, locale) {
        "${displayedMonth.month.getDisplayName(TextStyle.FULL, locale)} ${displayedMonth.year}"
    }
    val fullDateFormatter = remember(locale) {
        DateTimeFormatter.ofPattern("EEEE, MMMM d, yyyy", locale)
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(horizontal = 14.dp, vertical = 16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = monthTitle,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedCalendarDate != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.card,
                            border = BorderStroke(1.dp, colors.border),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onClearFilter)
                        ) {
                            Box(
                                modifier = Modifier.padding(horizontal = 12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Reset",
                                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = colors.textPrimary
                        )
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Next Month",
                            tint = colors.textPrimary
                        )
                    }
                }
            }

            val weekHeaders = listOf("M", "T", "W", "T", "F", "S", "S")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                weekHeaders.forEach { day ->
                    Text(
                        text = day,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val firstDayOfMonth = displayedMonth.atDay(1)
            val startOffset = firstDayOfMonth.dayOfWeek.value - 1
            val daysInMonth = displayedMonth.lengthOfMonth()
            val totalCells = startOffset + daysInMonth
            val rowCount = (totalCells + 6) / 7

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayNumber = cellIndex - startOffset + 1

                            if (dayNumber in 1..daysInMonth) {
                                val cellDate = displayedMonth.atDay(dayNumber)
                                val hasLoggedShift = loggedEpochDays.contains(cellDate.toEpochDay())
                                val isSelected = selectedCalendarDate == cellDate
                                val isToday = cellDate == today
                                val cellDescription = buildString {
                                    append(cellDate.format(fullDateFormatter))
                                    if (isToday) append(", today")
                                    if (hasLoggedShift) append(", has logged shift")
                                    if (isSelected) append(", selected")
                                }

                                val isFirstCutoff = dayNumber in 1..15
                                val cutoffBg = if (isFirstCutoff) colors.coralContainer else colors.azureContainer
                                val cutoffBorder = if (isFirstCutoff) colors.coralBright.copy(alpha = 0.55f) else colors.azureAccent.copy(alpha = 0.55f)
                                val cutoffText = if (isFirstCutoff) colors.coralInk else colors.azureInk
                                val cutoffDot = if (isFirstCutoff) colors.coralBright else colors.azureAccent

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isSelected -> colors.azurePrimary
                                                hasLoggedShift -> cutoffBg
                                                else -> colors.card
                                            }
                                        )
                                        .then(
                                            if (isToday && !isSelected) {
                                                Modifier.border(
                                                    width = 1.6.dp,
                                                    color = colors.azureAccent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                            } else if (hasLoggedShift && !isSelected) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = cutoffBorder,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                            } else {
                                                Modifier
                                            }
                                        )
                                        .clickable { onSelectDate(cellDate) }
                                        .semantics(mergeDescendants = true) {
                                            selected = isSelected
                                            contentDescription = cellDescription
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = ShiftlySans,
                                                fontWeight = if (isSelected || hasLoggedShift || isToday) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                }
                                            ).tabular(),
                                            color = when {
                                                isSelected -> colors.onAzurePrimary
                                                hasLoggedShift -> cutoffText
                                                else -> colors.textPrimary
                                            }
                                        )
                                        if (hasLoggedShift) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) colors.onAzurePrimary else cutoffDot
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ShiftHistoryEmptyCard(
    isFiltered: Boolean,
    onLogFirstShift: () -> Unit,
    onClearFilter: () -> Unit
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.mintContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkHistory,
                    contentDescription = null,
                    tint = colors.mintInk,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = if (isFiltered) "No shifts on this day" else "No shifts logged yet",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isFiltered) {
                    "Pick another day on the calendar or reset the filter."
                } else {
                    "Tap below to log your shift hours and auto-compute your pay."
                },
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShiftlyPrimaryPillButton(
                text = if (isFiltered) "Show All Shifts" else "Log your first shift",
                onClick = if (isFiltered) onClearFilter else onLogFirstShift
            )
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
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val zone = remember { ZoneId.systemDefault() }
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }

    val shiftDateText = remember(shift.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(shift.dateEpochDay).format(dateFormatter)
    }
    val timeRangeText = remember(shift.timeInMillis, shift.timeOutMillis, timeFormatter) {
        val start = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime().format(timeFormatter)
        val end = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime().format(timeFormatter)
        "$start – $end"
    }

    val wageDetails = remember(
        shift.timeInMillis,
        shift.timeOutMillis,
        shift.lockedHourlyRate,
        shift.dayType
    ) {
        WageCalculator.calculateTotalEarned(
            startMillis = shift.timeInMillis,
            endMillis = shift.timeOutMillis,
            hourlyRate = shift.lockedHourlyRate,
            dayTypeStr = shift.dayType
        )
    }

    val durationLabel = formatDurationCompact(shift.durationMinutes.toLong())

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(colors.mintContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = colors.mintInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = shiftDateText,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                        Text(
                            text = timeRangeText,
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.textSecondary
                        )
                    }
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, shift.totalEarned)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.mintInk
                    )
                    Text(
                        text = runCatching { DayType.valueOf(shift.dayType).label }.getOrNull() ?: shift.dayType,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
            }

            // Color-coded breakdown chips: Regular = Primary, Night Diff = Purple, Overtime = Red
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (wageDetails.regularBaseMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "Reg ${formatPillDuration(wageDetails.regularBaseMinutes)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                if (wageDetails.nightDiffMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.nightDiffPurpleContainer,
                        border = BorderStroke(1.dp, colors.nightDiffPurple.copy(alpha = 0.45f)),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "ND ${formatPillDuration(wageDetails.nightDiffMinutes)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
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
                            text = "OT ${formatPillDuration(wageDetails.overtimeMinutes)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.coralBright,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (shift.overtimeNote.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = colors.surface,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 0.dp,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = shift.overtimeNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, shift.lockedHourlyRate)}/hr",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = colors.textSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditNote, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "Edit Shift",
                            tint = colors.azureInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Shift",
                            tint = colors.coralInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

// =============================================================================
// Scrollable Wheel / Drum Picker + Presets + Receipt Income Preview
// =============================================================================

@Composable
private fun WheelPickerColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelectedIndexChanged: (Int) -> Unit,
    modifier: Modifier = Modifier,
    width: Dp = 64.dp
) {
    val colors = ShiftlyColors.current
    val itemHeight = 36.dp
    val visibleCount = 5
    val pickerHeight = itemHeight * visibleCount
    val contentVerticalPadding = itemHeight * 2
    val itemHeightPx = with(LocalDensity.current) { itemHeight.toPx() }
    val safeInitial = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = safeInitial)
    val flingBehavior = rememberSnapFlingBehavior(lazyListState = listState)
    var isProgrammaticScroll by remember { mutableStateOf(false) }

    val centeredIndex by remember(items.size, itemHeightPx) {
        derivedStateOf {
            if (items.isEmpty()) 0
            else {
                val offset = listState.firstVisibleItemScrollOffset
                val raw = listState.firstVisibleItemIndex + if (offset > itemHeightPx / 2f) 1 else 0
                raw.coerceIn(0, items.lastIndex)
            }
        }
    }

    LaunchedEffect(selectedIndex, items.size) {
        val target = selectedIndex.coerceIn(0, (items.size - 1).coerceAtLeast(0))
        if (!listState.isScrollInProgress && centeredIndex != target) {
            isProgrammaticScroll = true
            try {
                listState.animateScrollToItem(target)
            } finally {
                isProgrammaticScroll = false
            }
        }
    }

    LaunchedEffect(listState) {
        snapshotFlow { Triple(centeredIndex, listState.isScrollInProgress, isProgrammaticScroll) }
            .collect { (idx, _, programmatic) ->
                if (!programmatic && idx != selectedIndex && idx in items.indices) {
                    onSelectedIndexChanged(idx)
                }
            }
    }

    Box(
        modifier = modifier
            .width(width)
            .height(pickerHeight)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background.copy(alpha = 0.85f),
                        colors.cardElevated.copy(alpha = 0.45f),
                        colors.background.copy(alpha = 0.85f)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            flingBehavior = flingBehavior,
            contentPadding = PaddingValues(vertical = contentVerticalPadding),
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(items) { index, label ->
                val dist = abs(index - centeredIndex)
                val alpha = when (dist) {
                    0 -> 1f
                    1 -> 0.55f
                    else -> 0.25f
                }
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(itemHeight)
                        .clickable {
                            if (index != selectedIndex) {
                                onSelectedIndexChanged(index)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = if (dist == 0) FontWeight.Bold else FontWeight.Medium,
                            fontSize = if (dist == 0) 19.sp else 15.sp
                        ).tabular(),
                        color = if (dist == 0) colors.textPrimary else colors.textSecondary,
                        modifier = Modifier.alpha(alpha),
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun TimeBoxChip(
    text: String,
    highlighted: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) colors.cardElevated else colors.surface)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = colors.textPrimary
        )
    }
}

@Composable
private fun TimeDisplayPill(
    hour24: Int,
    minute: Int,
    expanded: Boolean,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val (hour12, isPm) = to12HourParts(hour24)
    val hourStr = hour12.toString().padStart(2, '0')
    val minStr = minute.toString().padStart(2, '0')
    val amPmStr = if (isPm) "PM" else "AM"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) colors.azureAccent else colors.border,
                shape = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        TimeBoxChip(text = hourStr, highlighted = expanded)
        Text(
            text = ":",
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = colors.textSecondary
        )
        TimeBoxChip(text = minStr, highlighted = expanded)
        TimeBoxChip(text = amPmStr, highlighted = expanded)
    }
}

@Composable
private fun InlineTime12hWheel(
    hour24: Int,
    minute: Int,
    onTimeChanged: (newHour24: Int, newMinute: Int) -> Unit
) {
    val colors = ShiftlyColors.current
    val (hour12, isPm) = to12HourParts(hour24)
    val hours12List = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minutesList = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val amPmList = remember { listOf("AM", "PM") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Centered blue selection frame behind active row
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, colors.azureAccent, RoundedCornerShape(12.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPickerColumn(
                items = hours12List,
                selectedIndex = (hour12 - 1).coerceIn(0, 11),
                onSelectedIndexChanged = { idx ->
                    val newHour24 = to24Hour(hour12 = idx + 1, isPm = isPm)
                    onTimeChanged(newHour24, minute)
                },
                width = 66.dp
            )

            Text(
                text = ":",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            WheelPickerColumn(
                items = minutesList,
                selectedIndex = minute.coerceIn(0, 59),
                onSelectedIndexChanged = { idx ->
                    onTimeChanged(hour24, idx)
                },
                width = 66.dp
            )

            Spacer(modifier = Modifier.width(12.dp))

            WheelPickerColumn(
                items = amPmList,
                selectedIndex = if (isPm) 1 else 0,
                onSelectedIndexChanged = { idx ->
                    val newHour24 = to24Hour(hour12 = hour12, isPm = idx == 1)
                    onTimeChanged(newHour24, minute)
                },
                width = 66.dp
            )
        }
    }
}

@Composable
private fun InlineDateWheel(
    date: LocalDate,
    onDateChanged: (LocalDate) -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val monthsList = remember(locale) {
        Month.entries.map { it.getDisplayName(TextStyle.SHORT, locale) }
    }
    val currentYear = remember { LocalDate.now().year }
    val years = remember(currentYear, date.year) {
        val minY = minOf(currentYear - 2, date.year)
        val maxY = maxOf(currentYear + 2, date.year)
        (minY..maxY).toList()
    }
    val yearsList = remember(years) { years.map { it.toString() } }
    val maxDayInMonth = remember(date.year, date.monthValue) {
        YearMonth.of(date.year, date.monthValue).lengthOfMonth()
    }
    val daysList = remember(maxDayInMonth) {
        (1..maxDayInMonth).map { it.toString().padStart(2, '0') }
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.background)
            .border(1.dp, colors.border, RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, colors.azureAccent, RoundedCornerShape(12.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            WheelPickerColumn(
                items = monthsList,
                selectedIndex = (date.monthValue - 1).coerceIn(0, 11),
                onSelectedIndexChanged = { idx ->
                    val newMonth = idx + 1
                    val clampedDay = date.dayOfMonth.coerceAtMost(YearMonth.of(date.year, newMonth).lengthOfMonth())
                    onDateChanged(LocalDate.of(date.year, newMonth, clampedDay))
                },
                width = 86.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            WheelPickerColumn(
                items = daysList,
                selectedIndex = (date.dayOfMonth - 1).coerceIn(0, daysList.lastIndex),
                onSelectedIndexChanged = { idx ->
                    onDateChanged(LocalDate.of(date.year, date.monthValue, idx + 1))
                },
                width = 64.dp
            )
            Spacer(modifier = Modifier.width(8.dp))
            WheelPickerColumn(
                items = yearsList,
                selectedIndex = years.indexOf(date.year).coerceAtLeast(0),
                onSelectedIndexChanged = { idx ->
                    val newYear = years[idx]
                    val clampedDay = date.dayOfMonth.coerceAtMost(YearMonth.of(newYear, date.monthValue).lengthOfMonth())
                    onDateChanged(LocalDate.of(newYear, date.monthValue, clampedDay))
                },
                width = 82.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BasicDateAndTimeSelectorCard(
    date: LocalDate,
    startHour24: Int,
    startMinute: Int,
    endHour24: Int,
    endMinute: Int,
    onDateChanged: (LocalDate) -> Unit,
    onStartTimeChanged: (Int, Int) -> Unit,
    onEndTimeChanged: (Int, Int) -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val dayOfWeekFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showStartTimePicker by remember { mutableStateOf(false) }
    var showEndTimePicker by remember { mutableStateOf(false) }

    val startLocal = LocalTime.of(startHour24, startMinute)
    val endLocal = LocalTime.of(endHour24, endMinute)
    val endsNextDay = !endLocal.isAfter(startLocal)

    fun format12h(time: LocalTime): String {
        val (h12, isPm) = to12HourParts(time.hour)
        val amPm = if (isPm) "PM" else "AM"
        return String.format(java.util.Locale.US, "%02d:%02d %s", h12, time.minute, amPm)
    }

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // 1. Date Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showDatePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = date.format(dayOfWeekFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.65f))

            // 2. Start Time Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Start time",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Shift begins",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = format12h(startLocal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.65f))

            // 3. End Time Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "End time",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (endsNextDay) "Ends next day" else "Ends same day",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (endsNextDay) colors.azureAccent else colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = format12h(endLocal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }

    // Customized Date Picker Dialog
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
                    Text("Select", color = colors.azureAccent, fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
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

    // Customized Clock Time Picker Dialog (Start Time)
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour24,
            initialMinute = startMinute,
            is24Hour = false
        )
        DatePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartTimeChanged(timePickerState.hour, timePickerState.minute)
                        showStartTimePicker = false
                    }
                ) {
                    Text("OK", color = colors.azureAccent, fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
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

    // Customized Clock Time Picker Dialog (End Time)
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour24,
            initialMinute = endMinute,
            is24Hour = false
        )
        DatePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        onEndTimeChanged(timePickerState.hour, timePickerState.minute)
                        showEndTimePicker = false
                    }
                ) {
                    Text("OK", color = colors.azureAccent, fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
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
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShiftTimePresetsBox(
    presets: List<ShiftTimePreset>,
    currentStartHour24: Int,
    currentStartMinute: Int,
    currentEndHour24: Int,
    currentEndMinute: Int,
    onSelectPreset: (ShiftTimePreset) -> Unit,
    onSaveCurrentAsPreset: () -> Unit,
    onDeletePreset: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    val alreadyExists = presets.any {
        it.startHour24 == currentStartHour24 &&
            it.startMinute == currentStartMinute &&
            it.endHour24 == currentEndHour24 &&
            it.endMinute == currentEndMinute
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PRESETS",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = colors.textSecondary
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (alreadyExists) colors.surface else colors.azureContainer,
                    border = BorderStroke(
                        1.dp,
                        if (alreadyExists) colors.border else colors.azureAccent.copy(alpha = 0.55f)
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .clickable(enabled = !alreadyExists, onClick = onSaveCurrentAsPreset)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (alreadyExists) Icons.Outlined.Check else Icons.Outlined.Add,
                            contentDescription = null,
                            tint = if (alreadyExists) colors.mintTextOnSurface else colors.azureAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (alreadyExists) "Saved" else "Save Time",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (alreadyExists) colors.mintTextOnSurface else colors.azureAccent
                        )
                    }
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    val isActive = preset.startHour24 == currentStartHour24 &&
                        preset.startMinute == currentStartMinute &&
                        preset.endHour24 == currentEndHour24 &&
                        preset.endMinute == currentEndMinute

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = if (isActive) colors.azureContainer else colors.surface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) colors.azureAccent else colors.border
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
                        Row(
                            modifier = Modifier.padding(
                                horizontal = 12.dp,
                                vertical = 7.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                ).tabular(),
                                color = if (isActive) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatNightDiffTimeRange(
    startHour24: Int,
    startMinute: Int,
    endHour24: Int,
    endMinute: Int
): String {
    val startTotalMins = startHour24 * 60 + startMinute
    var endTotalMins = endHour24 * 60 + endMinute
    if (endTotalMins <= startTotalMins) {
        endTotalMins += 24 * 60
    }

    val ndStartCap = 22 * 60
    val ndEndCap = 30 * 60

    val overlapStart = maxOf(startTotalMins, ndStartCap)
    val overlapEnd = minOf(endTotalMins, ndEndCap)

    if (overlapEnd <= overlapStart) {
        return "10 PM - 6 AM"
    }

    val sH24 = (overlapStart / 60) % 24
    val sM = overlapStart % 60
    val eH24 = (overlapEnd / 60) % 24
    val eM = overlapEnd % 60

    fun format12h(h24: Int, m: Int): String {
        val (h12, isPm) = to12HourParts(h24)
        val amPm = if (isPm) "PM" else "AM"
        return if (m == 0) "$h12 $amPm" else String.format(java.util.Locale.US, "%d:%02d %s", h12, m, amPm)
    }

    return "${format12h(sH24, sM)} - ${format12h(eH24, eM)}"
}

@Composable
private fun LogShiftReceiptPreviewCard(
    totalMinutes: Long,
    currencySymbol: String,
    wageDetails: WageDetails,
    startHour24: Int = 14,
    startMinute: Int = 0,
    endHour24: Int = 18,
    endMinute: Int = 0
) {
    val colors = ShiftlyColors.current
    val regularHours = wageDetails.regularBaseMinutes / 60.0
    val nightDiffTimeRange = remember(startHour24, startMinute, endHour24, endMinute) {
        formatNightDiffTimeRange(startHour24, startMinute, endHour24, endMinute)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "INCOME PREVIEW",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = colors.textSecondary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = formatDurationCompact(totalMinutes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Regular row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Regular • ${ShiftlyFormatters.formatHours(regularHours)} × ${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.effectiveRegularHourlyRate)}/hr",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.regularPay),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.textPrimary
                )
            }

            // Night Diff row (Purple) - fixed DOLE 10 PM–6 AM (+10%)
            if (wageDetails.nightDiffMinutes > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Night Diff ($nightDiffTimeRange) • ${formatDurationCompact(wageDetails.nightDiffMinutes)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.nightDiffPurple,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.nightDiffPay)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.nightDiffPurple
                    )
                }
            }

            // Overtime row (Red) - shown below Night Diff when total > 8 hours
            if (wageDetails.overtimeMinutes > 0L) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Overtime (>8h) • ${formatDurationCompact(wageDetails.overtimeMinutes)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.coralBright,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.overtimePay)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.coralBright
                    )
                }
            }

            HorizontalDivider(color = colors.border)

            // Large Receipt Total
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Total Earned",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )
                Text(
                    text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.totalEarned)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ).tabular(),
                    color = colors.mintInk
                )
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
            onSavePreset = onSavePreset,
            onDeletePreset = onDeletePreset,
            onDismiss = onDismiss,
            onSave = onSave
        )
    }
}

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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
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
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", fontFamily = ShiftlySerif, color = colors.textSecondary)
            }
        }

        if (showPresets) {
            ShiftTimePresetsBox(
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
                },
                onDeletePreset = onDeletePreset
            )
        }

        BasicDateAndTimeSelectorCard(
            date = pickedDate,
            startHour24 = startHour24,
            startMinute = startMin,
            endHour24 = endHour24,
            endMinute = endMin,
            onDateChanged = { pickedDate = it },
            onStartTimeChanged = { h, m ->
                startHour24 = h
                startMin = m
            },
            onEndTimeChanged = { h, m ->
                endHour24 = h
                endMin = m
            }
        )

        DayTypeSelectorRow(
            selectedDayType = selectedDayType,
            onClick = { showDayTypeSheet = true }
        )

        OutlinedTextField(
            value = manualNote,
            onValueChange = { manualNote = it },
            label = { Text("Shift Note (optional)", fontFamily = ShiftlySerif) },
            placeholder = { Text("Add shift note (optional)", color = colors.textMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        LogShiftReceiptPreviewCard(
            totalMinutes = totalMinutes,
            currencySymbol = currencySymbol,
            wageDetails = wageDetails,
            startHour24 = startHour24,
            startMinute = startMin,
            endHour24 = endHour24,
            endMinute = endMin
        )

        ShiftlyPrimaryPillButton(
            text = confirmButtonText,
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
fun ManageShiftPresetsSheet(
    presets: List<ShiftTimePreset>,
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
                    text = "Shift Time Presets",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = colors.textSecondary
                    )
                }
            }

            Text(
                text = "Saved Presets",
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
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
                            border = BorderStroke(1.dp, colors.border),
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
                                        fontFamily = ShiftlySans,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = colors.textPrimary
                                )
                                IconButton(
                                    onClick = { onDeletePreset(preset.id) },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Delete,
                                        contentDescription = "Delete preset",
                                        tint = colors.coralBright
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
                style = MaterialTheme.typography.titleMedium,
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

    val startLocal = LocalTime.of(startHour24, startMinute)
    val endLocal = LocalTime.of(endHour24, endMinute)
    val endsNextDay = !endLocal.isAfter(startLocal)

    fun format12h(time: LocalTime): String {
        val (h12, isPm) = to12HourParts(time.hour)
        val amPm = if (isPm) "PM" else "AM"
        return String.format(java.util.Locale.US, "%02d:%02d %s", h12, time.minute, amPm)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Start Time Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStartTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Start time",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Shift begins",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = format12h(startLocal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }
            }

            HorizontalDivider(color = colors.border.copy(alpha = 0.65f))

            // End Time Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showEndTimePicker = true },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "End time",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (endsNextDay) "Ends next day" else "Ends same day",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (endsNextDay) colors.azureAccent else colors.textSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = BorderStroke(1.dp, colors.border),
                    shadowElevation = 0.dp
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.AccessTime,
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = format12h(endLocal),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }
    }

    // Start Time Dialog
    if (showStartTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = startHour24,
            initialMinute = startMinute,
            is24Hour = false
        )
        DatePickerDialog(
            onDismissRequest = { showStartTimePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        onStartTimeChanged(timePickerState.hour, timePickerState.minute)
                        showStartTimePicker = false
                    }
                ) {
                    Text("OK", color = colors.azureAccent, fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
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

    // End Time Dialog
    if (showEndTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = endHour24,
            initialMinute = endMinute,
            is24Hour = false
        )
        DatePickerDialog(
            onDismissRequest = { showEndTimePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        onEndTimeChanged(timePickerState.hour, timePickerState.minute)
                        showEndTimePicker = false
                    }
                ) {
                    Text("OK", color = colors.azureAccent, fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
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
}
