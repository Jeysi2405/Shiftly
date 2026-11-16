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
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.DarkBackground
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkCardElevated
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.NightDiffPurple
import com.example.shiftly.ui.theme.NightDiffPurpleContainer
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.TextLightMuted
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
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

@Composable
fun DayTypeSelectorRow(
    selectedDayType: DayType,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
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
                    color = TextLightSecondary
                )
                Text(
                    text = selectedDayType.label,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextLightPrimary
                )
                Text(
                    text = selectedDayType.multiplierSummary,
                    style = MaterialTheme.typography.labelSmall.tabular(),
                    color = AzureAccent
                )
            }
            Icon(
                imageVector = Icons.Outlined.ChevronRight,
                contentDescription = "Choose day type",
                tint = TextLightSecondary
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
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
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextLightPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Close",
                        tint = TextLightSecondary
                    )
                }
            }

            DayType.entries.forEach { dayType ->
                val isSelected = dayType == selectedDayType
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AzureSoftContainer else DarkCard,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) AzureAccent else DarkBorder
                    ),
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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                text = dayType.label,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = ShiftlySerif,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isSelected) PureWhite else TextLightPrimary
                            )
                            Text(
                                text = dayType.multiplierSummary,
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = if (isSelected) AzureAccent else TextLightSecondary
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(AzurePrimaryButton),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Selected",
                                    tint = PureWhite,
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

@Composable
fun TimeBreakdownCard(
    monthlyTotalEarnings: Double,
    monthlyTotalHours: Double,
    timeBreakdown: TimeBreakdown,
    currencySymbol: String,
    displayedMonth: YearMonth = YearMonth.now()
) {
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = remember(displayedMonth, locale) {
        displayedMonth.month.getDisplayName(TextStyle.FULL, locale)
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Monthly Summary • $monthLabel",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                        color = TextLightSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalEarnings),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ).tabular(),
                        color = TextLightPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = MintSoftContainer,
                    border = BorderStroke(1.dp, MintBright.copy(alpha = 0.45f))
                ) {
                    Text(
                        text = "${ShiftlyFormatters.formatHours(monthlyTotalHours, compact = true)} total",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = MintTextOnBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Regular = White, Night Diff = Purple, Overtime = Red
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeBreakdownTile(
                    label = "Regular",
                    hours = timeBreakdown.regularHours,
                    valueColor = PureWhite,
                    modifier = Modifier.weight(1f)
                )
                TimeBreakdownTile(
                    label = "Night Diff",
                    hours = timeBreakdown.nightDiffHours,
                    valueColor = NightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                TimeBreakdownTile(
                    label = "Overtime",
                    hours = timeBreakdown.overtimeHours,
                    valueColor = CoralBright,
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
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .background(DarkCard)
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
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
            color = TextLightSecondary,
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
                    color = TextLightPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedCalendarDate != null) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = DarkCard,
                            border = BorderStroke(1.dp, DarkBorder),
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
                                    color = TextLightPrimary
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = onPreviousMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronLeft,
                            contentDescription = "Previous Month",
                            tint = TextLightPrimary
                        )
                    }

                    IconButton(
                        onClick = onNextMonth,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(1.dp, DarkBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ChevronRight,
                            contentDescription = "Next Month",
                            tint = TextLightPrimary
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
                        color = TextLightSecondary,
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

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(
                                            when {
                                                isSelected -> AzurePrimaryButton
                                                hasLoggedShift -> MintSoftContainer
                                                else -> DarkCard
                                            }
                                        )
                                        .then(
                                            if (isToday && !isSelected) {
                                                Modifier.border(
                                                    width = 1.6.dp,
                                                    color = AzureAccent,
                                                    shape = RoundedCornerShape(14.dp)
                                                )
                                            } else if (hasLoggedShift && !isSelected) {
                                                Modifier.border(
                                                    width = 1.dp,
                                                    color = MintBright.copy(alpha = 0.45f),
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
                                                isSelected -> PureWhite
                                                hasLoggedShift -> MintTextOnBlue
                                                else -> TextLightPrimary
                                            }
                                        )
                                        if (hasLoggedShift) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(
                                                        if (isSelected) MintTextOnBlue else MintBright
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
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
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
                    .background(MintSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.WorkHistory,
                    contentDescription = null,
                    tint = MintInkOnWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = if (isFiltered) "No shifts on this day" else "No shifts logged yet",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                color = TextLightPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isFiltered) {
                    "Pick another day on the calendar or reset the filter."
                } else {
                    "Tap below to log your shift hours and auto-compute your pay."
                },
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary,
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
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        shadowElevation = 2.dp,
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
                            .background(MintSoftContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null,
                            tint = MintInkOnWhite,
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
                            color = TextLightPrimary
                        )
                        Text(
                            text = "$timeRangeText ($durationLabel)",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = TextLightSecondary
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
                        color = MintInkOnWhite
                    )
                    Text(
                        text = runCatching { DayType.valueOf(shift.dayType).label }.getOrNull() ?: shift.dayType,
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightSecondary
                    )
                }
            }

            // Color-coded breakdown chips: Regular = White, Night Diff = Purple, Overtime = Red
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (wageDetails.regularBaseMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkBorder)
                    ) {
                        Text(
                            text = "Reg ${formatDurationCompact(wageDetails.regularBaseMinutes)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                if (wageDetails.nightDiffMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = NightDiffPurpleContainer,
                        border = BorderStroke(1.dp, NightDiffPurple.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "ND ${formatDurationCompact(wageDetails.nightDiffMinutes)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = NightDiffPurple,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                }
                if (wageDetails.overtimeMinutes > 0L) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = CoralSoftContainer,
                        border = BorderStroke(1.dp, CoralBright.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "OT ${formatDurationCompact(wageDetails.overtimeMinutes)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = CoralBright,
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
                        color = DarkSurface,
                        border = BorderStroke(1.dp, DarkBorder),
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        Text(
                            text = shift.overtimeNote,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLightPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                } else {
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, shift.lockedHourlyRate)}/hr",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = TextLightSecondary
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEditNote, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.EditNote,
                            contentDescription = "Edit Shift",
                            tint = AzureInkOnWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(48.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete Shift",
                            tint = CoralInkOnWhite,
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
                        DarkBackground.copy(alpha = 0.85f),
                        DarkCardElevated.copy(alpha = 0.45f),
                        DarkBackground.copy(alpha = 0.85f)
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
                        color = if (dist == 0) PureWhite else TextLightSecondary,
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
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (highlighted) DarkCardElevated else DarkSurface)
            .padding(horizontal = 10.dp, vertical = 7.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = TextLightPrimary
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
    val (hour12, isPm) = to12HourParts(hour24)
    val hourStr = hour12.toString().padStart(2, '0')
    val minStr = minute.toString().padStart(2, '0')
    val amPmStr = if (isPm) "PM" else "AM"

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = if (expanded) 1.5.dp else 1.dp,
                color = if (expanded) AzureAccent else DarkBorder,
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
            color = TextLightSecondary
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
    val (hour12, isPm) = to12HourParts(hour24)
    val hours12List = remember { (1..12).map { it.toString().padStart(2, '0') } }
    val minutesList = remember { (0..59).map { it.toString().padStart(2, '0') } }
    val amPmList = remember { listOf("AM", "PM") }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(DarkBackground)
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        // Centered blue selection frame behind active row
        Box(
            modifier = Modifier
                .fillMaxWidth(0.86f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, AzureAccent, RoundedCornerShape(12.dp))
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
                color = PureWhite,
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
            .background(DarkBackground)
            .border(1.dp, DarkBorder, RoundedCornerShape(18.dp))
            .padding(vertical = 10.dp, horizontal = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, AzureAccent, RoundedCornerShape(12.dp))
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

@Composable
private fun DateAndTimeWheelCard(
    date: LocalDate,
    startHour24: Int,
    startMinute: Int,
    endHour24: Int,
    endMinute: Int,
    onDateChanged: (LocalDate) -> Unit,
    onStartTimeChanged: (Int, Int) -> Unit,
    onEndTimeChanged: (Int, Int) -> Unit
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val dayOfWeekFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE", locale) }
    var activeSection by remember { mutableStateOf(ActiveWheelSection.NONE) }

    val startLocal = LocalTime.of(startHour24, startMinute)
    val endLocal = LocalTime.of(endHour24, endMinute)
    val endsNextDay = !endLocal.isAfter(startLocal)

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
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
                    .clickable {
                        activeSection = if (activeSection == ActiveWheelSection.DATE) {
                            ActiveWheelSection.NONE
                        } else {
                            ActiveWheelSection.DATE
                        }
                    },
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
                        color = TextLightPrimary
                    )
                    Text(
                        text = date.format(dayOfWeekFormatter),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (activeSection == ActiveWheelSection.DATE) DarkCardElevated else DarkSurface,
                    border = BorderStroke(
                        width = if (activeSection == ActiveWheelSection.DATE) 1.5.dp else 1.dp,
                        color = if (activeSection == ActiveWheelSection.DATE) AzureAccent else DarkBorder
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = AzureAccent,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = date.format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = TextLightPrimary
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = activeSection == ActiveWheelSection.DATE,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InlineDateWheel(
                    date = date,
                    onDateChanged = onDateChanged
                )
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.65f))

            // 2. Start Time Row (12h)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        activeSection = if (activeSection == ActiveWheelSection.START_TIME) {
                            ActiveWheelSection.NONE
                        } else {
                            ActiveWheelSection.START_TIME
                        }
                    },
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
                        color = TextLightPrimary
                    )
                    Text(
                        text = "Shift begins",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
                    )
                }

                TimeDisplayPill(
                    hour24 = startHour24,
                    minute = startMinute,
                    expanded = activeSection == ActiveWheelSection.START_TIME,
                    onClick = {
                        activeSection = if (activeSection == ActiveWheelSection.START_TIME) {
                            ActiveWheelSection.NONE
                        } else {
                            ActiveWheelSection.START_TIME
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = activeSection == ActiveWheelSection.START_TIME,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InlineTime12hWheel(
                    hour24 = startHour24,
                    minute = startMinute,
                    onTimeChanged = onStartTimeChanged
                )
            }

            HorizontalDivider(color = DarkBorder.copy(alpha = 0.65f))

            // 3. End Time Row (12h)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        activeSection = if (activeSection == ActiveWheelSection.END_TIME) {
                            ActiveWheelSection.NONE
                        } else {
                            ActiveWheelSection.END_TIME
                        }
                    },
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
                        color = TextLightPrimary
                    )
                    Text(
                        text = if (endsNextDay) "Ends next day" else "Shift ends",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (endsNextDay) NightDiffPurple else TextLightSecondary
                    )
                }

                TimeDisplayPill(
                    hour24 = endHour24,
                    minute = endMinute,
                    expanded = activeSection == ActiveWheelSection.END_TIME,
                    onClick = {
                        activeSection = if (activeSection == ActiveWheelSection.END_TIME) {
                            ActiveWheelSection.NONE
                        } else {
                            ActiveWheelSection.END_TIME
                        }
                    }
                )
            }

            AnimatedVisibility(
                visible = activeSection == ActiveWheelSection.END_TIME,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                InlineTime12hWheel(
                    hour24 = endHour24,
                    minute = endMinute,
                    onTimeChanged = onEndTimeChanged
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
    val alreadyExists = presets.any {
        it.startHour24 == currentStartHour24 &&
            it.startMinute == currentStartMinute &&
            it.endHour24 == currentEndHour24 &&
            it.endMinute == currentEndMinute
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
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
                    text = "PRESET TIMES",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = TextLightSecondary
                )

                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (alreadyExists) DarkSurface else AzureSoftContainer,
                    border = BorderStroke(
                        1.dp,
                        if (alreadyExists) DarkBorder else AzureAccent.copy(alpha = 0.55f)
                    ),
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
                            tint = if (alreadyExists) MintTextOnBlue else AzureAccent,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (alreadyExists) "Saved" else "Save Time",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (alreadyExists) MintTextOnBlue else AzureAccent
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
                        color = if (isActive) AzureSoftContainer else DarkSurface,
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (isActive) AzureAccent else DarkBorder
                        ),
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
                                start = 12.dp,
                                end = if (presets.size > 1) 6.dp else 12.dp,
                                top = 7.dp,
                                bottom = 7.dp
                            ),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = preset.label,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
                                ).tabular(),
                                color = if (isActive) PureWhite else TextLightSecondary
                            )
                            if (presets.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .clickable { onDeletePreset(preset.id) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Close,
                                        contentDescription = "Remove preset ${preset.label}",
                                        tint = TextLightMuted,
                                        modifier = Modifier.size(13.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LogShiftReceiptPreviewCard(
    totalMinutes: Long,
    currencySymbol: String,
    wageDetails: WageDetails
) {
    val regularHours = wageDetails.regularBaseMinutes / 60.0

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
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
                    color = TextLightSecondary
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = formatDurationCompact(totalMinutes),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = TextLightPrimary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Regular row (White)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Regular • ${ShiftlyFormatters.formatHours(regularHours)} × ${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.effectiveRegularHourlyRate)}/hr",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = PureWhite,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.regularPay),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = PureWhite
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
                        text = "Night Diff (10 PM–6 AM) • ${formatDurationCompact(wageDetails.nightDiffMinutes)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = NightDiffPurple,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.nightDiffPay)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = NightDiffPurple
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
                        color = CoralBright,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.overtimePay)}",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = CoralBright
                    )
                }
            }

            HorizontalDivider(color = DarkBorder)

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
                    color = TextLightPrimary
                )
                Text(
                    text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, wageDetails.totalEarned)}",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 28.sp
                    ).tabular(),
                    color = MintBright
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
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
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
    var pickedDate by remember(selectedDate) { mutableStateOf(selectedDate) }
    var startHour24 by remember(initialStartHour24) { mutableIntStateOf(initialStartHour24.coerceIn(0, 23)) }
    var startMin by remember(initialStartMinute) { mutableIntStateOf(initialStartMinute.coerceIn(0, 59)) }
    var endHour24 by remember(initialEndHour24) { mutableIntStateOf(initialEndHour24.coerceIn(0, 23)) }
    var endMin by remember(initialEndMinute) { mutableIntStateOf(initialEndMinute.coerceIn(0, 59)) }
    var manualNote by remember(initialNote) { mutableStateOf(initialNote) }
    var selectedDayType by remember(initialDayType) { mutableStateOf(initialDayType) }
    var showDayTypeSheet by remember { mutableStateOf(false) }

    val sheetFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AzureAccent,
        unfocusedBorderColor = DarkBorder,
        focusedLabelColor = AzureAccent,
        unfocusedLabelColor = TextLightSecondary,
        focusedTextColor = TextLightPrimary,
        unfocusedTextColor = TextLightPrimary,
        cursorColor = AzureAccent,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard
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
                color = TextLightPrimary
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", fontFamily = ShiftlySerif, color = TextLightSecondary)
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

        DateAndTimeWheelCard(
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
            placeholder = { Text("Add shift note (optional)", color = TextLightMuted) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
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
