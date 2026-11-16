package com.example.shiftly.ui.screens.duties

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.WorkHistory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.WageCalculator
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
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
            label = { Text("Day Type", fontFamily = ShiftlySerif) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzureAccent,
                unfocusedBorderColor = DarkBorder,
                focusedLabelColor = AzureAccent,
                unfocusedLabelColor = TextLightSecondary,
                focusedTextColor = TextLightPrimary,
                unfocusedTextColor = TextLightPrimary
            )
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(DarkCard)
        ) {
            DayType.entries.forEach { dayType ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = dayType.label,
                            style = MaterialTheme.typography.bodyMedium,
                            color = TextLightPrimary
                        )
                    },
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
    onShowMessage: (String) -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var rateInput by remember { mutableStateOf("") }
    var savedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(defaultHourlyRate) {
        if (rateInput.isEmpty() && defaultHourlyRate > 0.0) {
            rateInput = if (defaultHourlyRate % 1.0 == 0.0) {
                defaultHourlyRate.toInt().toString()
            } else {
                "%.2f".format(defaultHourlyRate)
            }
        }
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(DarkCard),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = "Rate locked per shift",
                            tint = MintTextOnBlue,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Default Hourly Rate",
                        style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                        color = TextLightPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(50),
                    color = DarkCard,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Text(
                        text = "Locked per shift",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextLightSecondary,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            // Hourly Rate Input + Primary Pill Save Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) {
                            rateInput = filtered
                            savedBanner = false
                        }
                    },
                    label = {
                        Text(
                            text = "Rate ($currencySymbol/hr)",
                            fontFamily = ShiftlySerif
                        )
                    },
                    prefix = {
                        Text(
                            text = currencySymbol,
                            fontWeight = FontWeight.Bold,
                            color = TextLightPrimary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(18.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzureAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AzureAccent,
                        unfocusedLabelColor = TextLightSecondary,
                        focusedTextColor = TextLightPrimary,
                        unfocusedTextColor = TextLightPrimary,
                        cursorColor = MintTextOnBlue,
                        focusedContainerColor = DarkCard,
                        unfocusedContainerColor = DarkCard
                    ),
                    modifier = Modifier.weight(1f)
                )

                Button(
                    onClick = {
                        val parsed = rateInput.toDoubleOrNull()
                        if (parsed == null || !parsed.isFinite() || parsed <= 0.0) {
                            savedBanner = false
                            onShowMessage("Enter an hourly rate above 0")
                            return@Button
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveRate(rateInput)
                        savedBanner = true
                        onShowMessage("Rate saved")
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AzurePrimaryButton,
                        contentColor = PureWhite
                    ),
                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 14.dp),
                    modifier = Modifier.heightIn(min = 52.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PureWhite
                    )
                }
            }

            if (savedBanner) {
                Text(
                    text = "Saved $currencySymbol${"%.2f".format(defaultHourlyRate)}/hr — locked for upcoming shifts",
                    style = MaterialTheme.typography.labelMedium,
                    color = MintTextOnBlue
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
    hasShiftForSelectedDate: Boolean,
    onPickDutyDate: () -> Unit,
    onTimeIn: () -> Unit,
    onRequestCheckOut: () -> Unit,
    onCancelTimer: () -> Unit,
    onOpenManualEntry: () -> Unit,
    onEditExistingShift: () -> Unit,
    previewElapsedSecondsOverride: Long? = null
) {
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }
    val haptics = LocalHapticFeedback.current

    var elapsedSeconds by remember(activeTimer.isRunning, activeTimer.startTimeMillis, previewElapsedSecondsOverride) {
        mutableLongStateOf(
            previewElapsedSecondsOverride ?: if (activeTimer.isRunning) {
                ((System.currentTimeMillis() - activeTimer.startTimeMillis) / 1000L).coerceAtLeast(0L)
            } else {
                0L
            }
        )
    }

    LaunchedEffect(activeTimer.isRunning, activeTimer.startTimeMillis, previewElapsedSecondsOverride) {
        if (previewElapsedSecondsOverride != null) return@LaunchedEffect
        while (activeTimer.isRunning) {
            elapsedSeconds = ((System.currentTimeMillis() - activeTimer.startTimeMillis) / 1000L)
                .coerceAtLeast(0L)
            delay(1_000L)
        }
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            // Top Row: Status + Locked Hourly Rate Pill
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (activeTimer.isRunning) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MintSoftContainer,
                        border = BorderStroke(1.dp, MintBright.copy(alpha = 0.55f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(MintBright)
                            )
                            Text(
                                text = "LIVE SHIFT",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                color = MintTextOnBlue
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(9.dp)
                                .clip(CircleShape)
                                .background(MintBright)
                        )
                        Text(
                            text = "Duty Timer",
                            style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                            color = TextLightPrimary
                        )
                    }
                }

                val displayedRate = if (activeTimer.isRunning) activeTimer.lockedRate else defaultHourlyRate
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DarkCard,
                    border = BorderStroke(1.dp, DarkBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Lock,
                            contentDescription = null,
                            tint = MintTextOnBlue,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "$currencySymbol${"%.2f".format(displayedRate)}/hr",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold).tabular(),
                            color = TextLightPrimary
                        )
                    }
                }
            }

            if (activeTimer.isRunning) {
                val hours = elapsedSeconds / 3600
                val minutes = (elapsedSeconds % 3600) / 60
                val seconds = elapsedSeconds % 60
                val formattedClock = "%02d:%02d:%02d".format(hours, minutes, seconds)
                val liveAccrued = (elapsedSeconds / 3600.0) * activeTimer.lockedRate

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = formattedClock,
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 38.sp
                        ).tabular(),
                        color = TextLightPrimary
                    )

                    // Live Income Counter Pill ("+₱ so far")
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MintSoftContainer,
                        border = BorderStroke(1.dp, MintBright.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "+$currencySymbol${"%,.2f".format(liveAccrued)} so far",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = MintTextOnBlue,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCancelTimer,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, DarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
                        modifier = Modifier.heightIn(min = 52.dp)
                    ) {
                        Text(
                            text = "Discard",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = ShiftlySerif),
                            color = TextLightPrimary
                        )
                    }

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onRequestCheckOut()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AzurePrimaryButton,
                            contentColor = PureWhite
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 52.dp)
                    ) {
                        Icon(Icons.Outlined.Stop, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Check Out",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = PureWhite
                        )
                    }
                }
            } else {
                // Idle State: Selected Duty Date Row + Change Pill
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(DarkCard)
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .clickable { onPickDutyDate() }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.CalendarToday,
                            contentDescription = null,
                            tint = MintTextOnBlue,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = selectedDutyDate.format(dateFormatter),
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextLightPrimary
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AzureSoftContainer
                    ) {
                        Text(
                            text = "Change",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = AzureInkOnWhite,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                // Primary Actions Row: Primary Pill "Time In" + Outlined Pill "Manual"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (hasShiftForSelectedDate) {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onEditExistingShift()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AzurePrimaryButton,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Shift",
                                style = MaterialTheme.typography.labelLarge.copy(fontFamily = ShiftlySerif),
                                color = PureWhite
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onTimeIn()
                            },
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AzurePrimaryButton,
                                contentColor = PureWhite
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 52.dp)
                        ) {
                            Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(19.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Time In",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = ShiftlySerif,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = PureWhite
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = onOpenManualEntry,
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.2.dp, DarkBorder),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
                        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
                        modifier = Modifier.heightIn(min = 52.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Add,
                            contentDescription = null,
                            tint = TextLightPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = "Manual",
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = TextLightPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TimeBreakdownCard(
    weeklyTotalEarnings: Double,
    weeklyTotalHours: Double,
    timeBreakdown: TimeBreakdown,
    currencySymbol: String
) {
    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Weekly Summary",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                        color = TextLightSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%,.2f".format(weeklyTotalEarnings)}",
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
                        text = "${"%.1f".format(weeklyTotalHours)}h total",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = MintTextOnBlue,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            // Three elevated stat tiles (Regular, Night Diff, Overtime in mint)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                TimeBreakdownTile(
                    label = "Regular",
                    hours = timeBreakdown.regularHours,
                    modifier = Modifier.weight(1f)
                )
                TimeBreakdownTile(
                    label = "Night Diff",
                    hours = timeBreakdown.nightDiffHours,
                    modifier = Modifier.weight(1f)
                )
                TimeBreakdownTile(
                    label = "Overtime",
                    hours = timeBreakdown.overtimeHours,
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
            text = "${"%.1f".format(hours)}h",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = MintTextOnBlue
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
            // Calendar Header with 48x48dp arrow buttons
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

            // Day of Week Headers (Mon - Sun)
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

            // Larger, easier-to-tap Calendar Cells Grid (48dp height)
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
                    "Tap below to log your hours and auto-compute your take-home pay."
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

    val hours = shift.durationMinutes / 60
    val mins = shift.durationMinutes % 60
    val durationLabel = if (hours > 0) "${hours}h ${mins}m" else "${mins}m"

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
                        text = "+$currencySymbol${"%,.2f".format(shift.totalEarned)}",
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
                        text = "$currencySymbol${"%.2f".format(shift.lockedHourlyRate)}/hr locked",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualShiftBottomSheet(
    selectedDate: LocalDate,
    hourlyRate: Double,
    currencySymbol: String,
    nightDiffStartHour: Int = 22,
    nightDiffMultiplier: Double = 1.10,
    overtimeMultiplier: Double = 1.25,
    onPickDate: () -> Unit,
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
            nightDiffStartHour = nightDiffStartHour,
            nightDiffMultiplier = nightDiffMultiplier,
            overtimeMultiplier = overtimeMultiplier,
            onPickDate = onPickDate,
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
    nightDiffStartHour: Int = 22,
    nightDiffMultiplier: Double = 1.10,
    overtimeMultiplier: Double = 1.25,
    onPickDate: () -> Unit,
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
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", locale) }

    var startHour by remember { mutableStateOf("14") }
    var startMin by remember { mutableStateOf("00") }
    var endHour by remember { mutableStateOf("18") }
    var endMin by remember { mutableStateOf("00") }
    var manualNote by remember { mutableStateOf("Campus Café Shift") }
    var selectedDayType by remember { mutableStateOf(DayType.ORDINARY) }

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

    // Live Income Preview Calculation (hours × rate + ND/OT multipliers)
    val sH = (startHour.toIntOrNull() ?: 14).coerceIn(0, 23)
    val sM = (startMin.toIntOrNull() ?: 0).coerceIn(0, 59)
    val eH = (endHour.toIntOrNull() ?: 18).coerceIn(0, 23)
    val eM = (endMin.toIntOrNull() ?: 0).coerceIn(0, 59)

    val zone = remember { ZoneId.systemDefault() }
    val startDateTime = selectedDate.atTime(LocalTime.of(sH, sM))
    var endDateTime = selectedDate.atTime(LocalTime.of(eH, eM))
    if (!endDateTime.isAfter(startDateTime)) {
        endDateTime = endDateTime.plusDays(1)
    }
    val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()
    val endMillis = endDateTime.atZone(zone).toInstant().toEpochMilli()
    val totalMinutes = ((endMillis - startMillis) / 60_000L).coerceAtLeast(0L)
    val durationHours = totalMinutes / 60.0
    val wageDetails = WageCalculator.calculateTotalEarned(
        startMillis = startMillis,
        endMillis = endMillis,
        hourlyRate = hourlyRate,
        dayTypeStr = selectedDayType.name,
        nightDiffStartHour = nightDiffStartHour,
        nightDiffMultiplier = nightDiffMultiplier,
        ordinaryOvertimeMultiplier = overtimeMultiplier
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
                text = "Log Manual Shift",
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

        // Date Selector Row
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable(onClick = onPickDate)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = AzureInkOnWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        color = TextLightPrimary
                    )
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                    color = AzureInkOnWhite
                )
            }
        }

        // Quick Student Shift Presets (1-handed speed)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                "2–6 PM" to listOf("14", "00", "18", "00"),
                "9–1 PM" to listOf("9", "00", "13", "00"),
                "6–11 PM" to listOf("18", "00", "23", "00")
            ).forEach { (label, times) ->
                val active = startHour == times[0] && endHour == times[2]
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (active) AzureSoftContainer else DarkCard,
                    border = BorderStroke(1.dp, if (active) AzureInkOnWhite else DarkBorder),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable {
                            startHour = times[0]
                            startMin = times[1]
                            endHour = times[2]
                            endMin = times[3]
                        }
                        .semantics(mergeDescendants = true) {
                            selected = active
                            contentDescription = "Preset shift $label"
                        }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.tabular(),
                            color = if (active) AzureInkOnWhite else TextLightSecondary
                        )
                    }
                }
            }
        }

        // Start & End Time Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = startHour,
                onValueChange = { startHour = it.filter(Char::isDigit).take(2) },
                label = { Text("Start Hr") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = startMin,
                onValueChange = { startMin = it.filter(Char::isDigit).take(2) },
                label = { Text("Min") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endHour,
                onValueChange = { endHour = it.filter(Char::isDigit).take(2) },
                label = { Text("End Hr") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors,
                modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
                value = endMin,
                onValueChange = { endMin = it.filter(Char::isDigit).take(2) },
                label = { Text("Min") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                shape = RoundedCornerShape(14.dp),
                colors = sheetFieldColors,
                modifier = Modifier.weight(1f)
            )
        }

        DayTypeSelector(
            selectedDayType = selectedDayType,
            onDayTypeSelected = { selectedDayType = it }
        )

        OutlinedTextField(
            value = manualNote,
            onValueChange = { manualNote = it },
            label = { Text("Shift Note (optional)", fontFamily = ShiftlySerif) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = sheetFieldColors,
            modifier = Modifier.fillMaxWidth()
        )

        // Income Preview Card (hours × rate = ₱ amount)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MintSoftContainer,
            border = BorderStroke(1.dp, MintBright.copy(alpha = 0.35f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Income Preview",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                        color = MintInkOnWhite
                    )
                    Text(
                        text = "${"%.1f".format(durationHours)} hrs × $currencySymbol${"%.2f".format(hourlyRate)}/hr",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = TextLightSecondary
                    )
                }

                Text(
                    text = "+$currencySymbol${"%,.2f".format(wageDetails.totalEarned)}",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = MintInkOnWhite
                )
            }
        }

        ShiftlyPrimaryPillButton(
            text = "Save Shift",
            onClick = { onSave(selectedDate, sH, sM, eH, eM, manualNote, selectedDayType) }
        )
    }
}
