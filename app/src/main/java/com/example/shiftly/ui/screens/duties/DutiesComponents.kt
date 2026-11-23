package com.example.shiftly.ui.screens.duties

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.WageCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val WEEK_HEADERS = listOf("M", "T", "W", "T", "F", "S", "S")
private val CalendarCellShape = RoundedCornerShape(10.dp)

private data class CalendarMonthLayout(
    val startOffset: Int,
    val rowCount: Int,
    val days: List<LocalDate>,
    val epochDays: LongArray
)

internal fun formatDurationCompact(totalMinutes: Long): String {
    val safe = totalMinutes.coerceAtLeast(0L)
    val h = safe / 60L
    val m = safe % 60L
    return when {
        h > 0L && m > 0L -> "${h}h ${m}m"
        h > 0L -> "${h}h"
        else -> "${m}m"
    }
}

private fun filterWageDecimalInput(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

private fun formatWageEditableNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
}

@Composable
fun DutiesWageRateCard(
    defaultHourlyRate: Double,
    minimumWagePerDay: Double,
    currencySymbol: String,
    onSaveRates: (hourlyRate: Double, minimumWagePerDay: Double) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    var hasInitializedMode by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(defaultHourlyRate <= 0.0) }
    var hourlyInput by remember {
        mutableStateOf(if (defaultHourlyRate > 0.0) formatWageEditableNumber(defaultHourlyRate) else "")
    }

    LaunchedEffect(defaultHourlyRate) {
        if (!hasInitializedMode && defaultHourlyRate > 0.0) {
            hasInitializedMode = true
            isEditing = false
        }
        if (!isEditing) {
            hourlyInput = if (defaultHourlyRate > 0.0) formatWageEditableNumber(defaultHourlyRate) else ""
        }
    }

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.azureAccent,
        unfocusedBorderColor = Color.Transparent,
        focusedLabelColor = colors.azureAccent,
        unfocusedLabelColor = colors.textSecondary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.azureAccent,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface
    )

    ShiftlyGlassCard(
        contentPadding = PaddingValues(18.dp),
        containerColor = colors.cardElevated
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    ShiftlyIosIconTile(
                        iconRes = R.drawable.ic_lucide_banknote,
                        backgroundColor = colors.mintBright,
                        size = 28.dp,
                        cornerRadius = 9.dp,
                        iconSize = 22.dp
                    )
                    Text(
                        text = "Hourly Rate",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary
                    )
                }

                if (!isEditing) {
                    var editTapCount by remember { mutableIntStateOf(0) }
                    IconButton(
                        onClick = {
                            editTapCount++
                            hourlyInput = if (defaultHourlyRate > 0.0) formatWageEditableNumber(defaultHourlyRate) else ""
                            isEditing = true
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_pencil,
                            contentDescription = "Edit hourly rate",
                            tint = colors.azureAccent,
                            triggerKey = editTapCount,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                } else if (defaultHourlyRate > 0.0) {
                    TextButton(
                        onClick = {
                            hourlyInput = formatWageEditableNumber(defaultHourlyRate)
                            isEditing = false
                        },
                        modifier = Modifier.heightIn(min = 48.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily),
                            color = colors.azureAccent
                        )
                    }
                }
            }

            if (!isEditing) {
                Text(
                    text = "${ShiftlyFormatters.formatCurrency(currencySymbol, defaultHourlyRate)}/hr",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintTextOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            } else {
                OutlinedTextField(
                    value = hourlyInput,
                    onValueChange = { hourlyInput = filterWageDecimalInput(it, hourlyInput) },
                    label = { Text("Hourly ($currencySymbol/hr)", fontFamily = InterFontFamily) },
                    placeholder = {
                        Text(
                            text = "0",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                    },
                    prefix = {
                        Text(
                            text = currencySymbol,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                ShiftlyPrimaryPillButton(
                    text = "Save",
                    leadingIcon = {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_check,
                            contentDescription = null,
                            tint = colors.onAzurePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    onClick = {
                        val rawHourly = hourlyInput.toDoubleOrNull()
                        val resolvedHourly = if (rawHourly != null && rawHourly.isFinite() && rawHourly > 0.0) {
                            rawHourly
                        } else {
                            0.0
                        }
                        if (!resolvedHourly.isFinite() || resolvedHourly <= 0.0) {
                            onShowMessage("Enter an hourly rate above 0")
                            return@ShiftlyPrimaryPillButton
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveRates(resolvedHourly, minimumWagePerDay)
                        hourlyInput = formatWageEditableNumber(resolvedHourly)
                        isEditing = false
                        onShowMessage("Hourly rate saved")
                    }
                )
            }
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
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShiftlySheetHeader(
                title = "Select Day Type",
                onCancel = onDismiss,
                actionLabel = "Done",
                onAction = onDismiss
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .selectableGroup(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                val options = DayType.orderedForSelection
                options.forEach { dayType ->
                    val isSelected = dayType == selectedDayType
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .background(if (isSelected) colors.azureContainer else colors.cardElevated)
                            .clickable(role = Role.RadioButton) {
                                onSelect(dayType)
                                onDismiss()
                            }
                            .semantics(mergeDescendants = true) {
                                selected = isSelected
                            }
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
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary
                            )
                            dayType.holidayBadge?.let { badge ->
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = colors.coralContainer,
                                    shadowElevation = 0.dp
                                ) {
                                    Text(
                                        text = badge,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = colors.coralInk,
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                        if (isSelected) {
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_check,
                                contentDescription = "Selected",
                                tint = colors.azurePrimary,
                                animateOnMount = true,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            ShiftlySheetCloseButton(
                label = "Close",
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun TimeBreakdownCard(
    monthlyTotalEarnings: Double,
    timeBreakdown: TimeBreakdown,
    currencySymbol: String,
    displayedMonth: YearMonth = YearMonth.now(),
    defaultHourlyRate: Double = 0.0,
    onOpenSettings: (() -> Unit)? = null
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
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = headerTitle,
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalEarnings),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ).tabular(),
                        color = colors.textPrimary
                    )
                }

                if (defaultHourlyRate > 0.0 || onOpenSettings != null) {
                    val hasRate = defaultHourlyRate > 0.0
                    Box(
                        modifier = if (onOpenSettings != null) {
                            Modifier
                                .heightIn(min = 48.dp)
                                .widthIn(min = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onOpenSettings)
                        } else {
                            Modifier
                        },
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (hasRate) colors.cardElevated else colors.azureContainer,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(5.dp)
                            ) {
                                ShiftlyAnimatedIcon(
                                    iconRes = R.drawable.ic_lucide_banknote,
                                    contentDescription = null,
                                    tint = if (hasRate) colors.mintTextOnSurface else colors.azureInk,
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (hasRate) {
                                        "${ShiftlyFormatters.formatCurrency(currencySymbol, defaultHourlyRate)}/hr"
                                    } else {
                                        "Set rate"
                                    },
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ).tabular(),
                                    color = if (hasRate) colors.textPrimary else colors.azureInk
                                )
                            }
                        }
                    }
                }
            }

            ShiftlyHairlineDivider()

            // Flattened Regular / Night Diff / Overtime metrics with hairline dividers
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TimeBreakdownTile(
                    label = "Regular",
                    hours = timeBreakdown.regularHours,
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                ShiftlyVerticalHairlineDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                )
                TimeBreakdownTile(
                    label = "Night Diff",
                    hours = timeBreakdown.nightDiffHours,
                    valueColor = colors.nightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                ShiftlyVerticalHairlineDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
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
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = ShiftlyFormatters.formatHours(hours, compact = true),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = valueColor
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
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
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onSelectDate: (LocalDate) -> Unit,
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
    val calendarLayout = remember(displayedMonth) {
        val firstDayOfMonth = displayedMonth.atDay(1)
        val startOffset = firstDayOfMonth.dayOfWeek.value - 1
        val daysInMonth = displayedMonth.lengthOfMonth()
        val totalCells = startOffset + daysInMonth
        val rowCount = (totalCells + 6) / 7
        val days = List(daysInMonth) { idx -> displayedMonth.atDay(idx + 1) }
        val epochDays = LongArray(daysInMonth) { idx -> days[idx].toEpochDay() }
        CalendarMonthLayout(
            startOffset = startOffset,
            rowCount = rowCount,
            days = days,
            epochDays = epochDays
        )
    }
    val cellDescriptions = remember(calendarLayout, loggedEpochDays, today, fullDateFormatter) {
        List(calendarLayout.days.size) { idx ->
            val cellDate = calendarLayout.days[idx]
            val hasLoggedShift = loggedEpochDays.contains(calendarLayout.epochDays[idx])
            val isToday = cellDate == today
            buildString {
                append(cellDate.format(fullDateFormatter))
                if (isToday) append(", today")
                if (hasLoggedShift) append(", tap to edit logged shift")
                else append(", tap to log shift")
            }
        }
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
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(start = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    var prevTapCount by remember { mutableIntStateOf(0) }
                    var nextTapCount by remember { mutableIntStateOf(0) }
                    IconButton(
                        onClick = {
                            prevTapCount++
                            onPreviousMonth()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_chevron_left,
                            contentDescription = "Previous Month",
                            tint = colors.textPrimary,
                            triggerKey = prevTapCount,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    IconButton(
                        onClick = {
                            nextTapCount++
                            onNextMonth()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_chevron_right,
                            contentDescription = "Next Month",
                            tint = colors.textPrimary,
                            triggerKey = nextTapCount,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                WEEK_HEADERS.forEach { day ->
                    Text(
                        text = day,
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            val daysInMonth = calendarLayout.days.size
            val startOffset = calendarLayout.startOffset

            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                for (row in 0 until calendarLayout.rowCount) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        for (col in 0..6) {
                            val cellIndex = row * 7 + col
                            val dayIdx = cellIndex - startOffset

                            if (dayIdx in 0 until daysInMonth) {
                                val dayNumber = dayIdx + 1
                                val cellDate = calendarLayout.days[dayIdx]
                                val hasLoggedShift = loggedEpochDays.contains(calendarLayout.epochDays[dayIdx])
                                val isToday = cellDate == today
                                val cellDescription = cellDescriptions[dayIdx]

                                val isFirstCutoff = dayNumber in 1..15
                                val cutoffBg = if (isFirstCutoff) colors.coralContainer else colors.azureContainer
                                val cutoffText = if (isFirstCutoff) colors.coralInk else colors.azureInk
                                val cutoffDot = if (isFirstCutoff) colors.coralBright else colors.azureAccent

                                val cellBg = when {
                                    hasLoggedShift -> cutoffBg
                                    isToday -> colors.azurePrimary
                                    else -> colors.cardElevated
                                }
                                val dayTextColor = when {
                                    hasLoggedShift -> cutoffText
                                    isToday -> PureWhite
                                    else -> colors.textPrimary
                                }

                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(CalendarCellShape)
                                        .background(cellBg)
                                        .clickable { onSelectDate(cellDate) }
                                        .semantics(mergeDescendants = true) {
                                            contentDescription = cellDescription
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = dayNumber.toString(),
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = if (hasLoggedShift || isToday) {
                                                    FontWeight.Bold
                                                } else {
                                                    FontWeight.Medium
                                                }
                                            ).tabular(),
                                            color = dayTextColor
                                        )
                                        if (hasLoggedShift) {
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Box(
                                                modifier = Modifier
                                                    .size(5.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isToday) colors.azureAccent else cutoffDot)
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
    onLogFirstShift: () -> Unit
) {
    val colors = ShiftlyColors.current
    ShiftlyEmptyState(
        iconRes = R.drawable.ic_lucide_briefcase,
        title = "No shifts logged this month",
        subtitle = null,
        actionText = "Log Shift",
        onAction = onLogFirstShift,
        iconBackgroundColor = colors.mintBright
    )
}

@Composable
fun ShiftHistoryItemCard(
    shift: ShiftEntity,
    currencySymbol: String,
    onEditNote: () -> Unit,
    onDelete: () -> Unit,
    containerColor: Color = ShiftlyColors.current.card,
    shape: Shape = ShiftlyShapes.Card
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
    val dayTypeLabel = remember(shift.dayType) {
        runCatching { DayType.valueOf(shift.dayType).shortLabel }.getOrNull() ?: shift.dayType
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
    val totalShiftMinutes = remember(shift.timeInMillis, shift.timeOutMillis) {
        ((shift.timeOutMillis - shift.timeInMillis) / 60_000L).coerceAtLeast(0L)
    }
    val extraTags = remember(wageDetails.nightDiffMinutes, wageDetails.overtimeMinutes) {
        buildList {
            if (wageDetails.nightDiffMinutes > 0L) add("ND ${formatDurationCompact(wageDetails.nightDiffMinutes)}")
            if (wageDetails.overtimeMinutes > 0L) add("OT ${formatDurationCompact(wageDetails.overtimeMinutes)}")
        }.joinToString(" · ")
    }

    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onEditNote)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
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
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    ShiftlyIosIconTile(
                        iconRes = R.drawable.ic_lucide_clock,
                        backgroundColor = colors.mintBright,
                        size = 28.dp,
                        cornerRadius = 9.dp,
                        iconSize = 22.dp
                    )

                    Text(
                        text = shiftDateText,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.textPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = dayTypeLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary
                    )
                    Text(
                        text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, shift.totalEarned)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.mintInk
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = timeRangeText,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                        color = colors.textSecondary
                    )
                    Text(
                        text = formatDurationCompact(totalShiftMinutes),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    if (extraTags.isNotEmpty() || shift.overtimeNote.isNotBlank()) {
                        val detailLine = listOfNotNull(
                            extraTags.takeIf { it.isNotEmpty() },
                            shift.overtimeNote.trim().takeIf { it.isNotEmpty() }
                        ).joinToString(" · ")
                        Text(
                            text = detailLine,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    var editTapCount by remember { mutableIntStateOf(0) }
                    var deleteTapCount by remember { mutableIntStateOf(0) }
                    IconButton(
                        onClick = {
                            editTapCount++
                            onEditNote()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_square_pen,
                            contentDescription = "Edit Shift",
                            tint = colors.azureInk,
                            triggerKey = editTapCount,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                    IconButton(
                        onClick = {
                            deleteTapCount++
                            onDelete()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_trash_2,
                            contentDescription = "Delete Shift",
                            tint = colors.coralInk,
                            triggerKey = deleteTapCount,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
        }
    }
}
