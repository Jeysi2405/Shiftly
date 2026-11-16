package com.example.shiftly.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
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
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Restore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralTextOnBlue
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.TextLightMuted
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular

@Composable
private fun darkSwitchColors() = SwitchDefaults.colors(
    checkedThumbColor = InkBlack,
    checkedTrackColor = MintBright,
    uncheckedThumbColor = TextLightSecondary,
    uncheckedTrackColor = DarkSurface,
    uncheckedBorderColor = DarkBorder
)

@Composable
private fun darkSheetFieldColors() = OutlinedTextFieldDefaults.colors(
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

@Composable
fun ProfileSnapshotGlassCard(
    currencySymbol: String,
    totalShifts: Int,
    totalHoursWorked: Double,
    totalEarnings: Double,
    totalExpenses: Double
) {
    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SNAPSHOT",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.8.sp
                    ),
                    color = TextLightSecondary
                )
                Text(
                    text = "All-time summary",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextLightMuted
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSnapshotTile(
                    label = "Shifts",
                    value = totalShifts.toString(),
                    valueColor = TextLightPrimary,
                    modifier = Modifier.weight(1f)
                )
                GlassSnapshotTile(
                    label = "Hours",
                    value = "${"%.1f".format(totalHoursWorked)}h",
                    valueColor = TextLightPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSnapshotTile(
                    label = "Earnings",
                    value = "$currencySymbol${"%,.2f".format(totalEarnings)}",
                    valueColor = MintTextOnBlue,
                    modifier = Modifier.weight(1f)
                )
                GlassSnapshotTile(
                    label = "Expenses",
                    value = "$currencySymbol${"%,.2f".format(totalExpenses)}",
                    valueColor = CoralTextOnBlue,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GlassSnapshotTile(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                color = TextLightSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = ShiftlySans,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp
                ).tabular(),
                color = valueColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun ProfileBackupGlassCard(
    onExportBackup: () -> Unit,
    onOpenRestore: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit
) {
    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "BACKUP & DATA EXPORT",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = TextLightSecondary
                    )
                    Text(
                        text = "Offline JSON snapshot, CSV sheet, or PDF statement",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightMuted
                    )
                }
            }

            Button(
                onClick = onExportBackup,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AzurePrimaryButton,
                    contentColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Export Backup",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = PureWhite
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                GlassActionPill(
                    label = "Restore",
                    icon = Icons.Outlined.Restore,
                    onClick = onOpenRestore,
                    modifier = Modifier.weight(1f)
                )
                GlassActionPill(
                    label = "CSV",
                    icon = Icons.Outlined.TableChart,
                    onClick = onExportCsv,
                    modifier = Modifier.weight(1f)
                )
                GlassActionPill(
                    label = "PDF",
                    icon = Icons.Outlined.PictureAsPdf,
                    onClick = onExportPdf,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun GlassActionPill(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MintTextOnBlue,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextLightPrimary
            )
        }
    }
}

@Composable
fun SettingsGroupCard(
    groupTitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = groupTitle,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = ShiftlySerif,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = TextLightSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = DarkCard,
            border = BorderStroke(1.dp, DarkBorder),
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsChevronRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    iconBg: Color = AzureSoftContainer,
    iconTint: Color = AzureInkOnWhite,
    trailingBadge: String? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextLightPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (trailingBadge != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = DarkSurface,
                border = BorderStroke(1.dp, DarkBorder)
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = TextLightPrimary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
        Icon(
            imageVector = Icons.Outlined.ChevronRight,
            contentDescription = null,
            tint = TextLightSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onRowClick: (() -> Unit)? = null,
    iconBg: Color = MintSoftContainer,
    iconTint: Color = MintInkOnWhite
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 60.dp)
            .let { mod ->
                if (onRowClick != null) mod.clickable(onClick = onRowClick) else mod
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconBg),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                ),
                color = TextLightPrimary
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = darkSwitchColors()
        )
    }
}

// =============================================================================
// Settings Modal Bottom Sheets
// =============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsSheetContainer(
    title: String,
    onDismiss: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = DarkSurface,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 26.dp),
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
                    Text("Close", fontFamily = ShiftlySerif, color = TextLightSecondary)
                }
            }
            content()
        }
    }
}

@Composable
fun AccountEditSheetContent(
    currentName: String,
    hourlyRate: Double,
    currencySymbol: String,
    onSave: (String) -> Unit
) {
    var nameInput by remember { mutableStateOf(currentName) }
    OutlinedTextField(
        value = nameInput,
        onValueChange = { nameInput = it },
        label = { Text("Display Name", fontFamily = ShiftlySerif) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "Locked duty rate: $currencySymbol${"%.2f".format(hourlyRate)}/hr (edit on Duties tab)",
        style = MaterialTheme.typography.bodySmall.tabular(),
        color = TextLightSecondary
    )
    ShiftlyPrimaryPillButton(
        text = "Save Profile",
        enabled = nameInput.isNotBlank(),
        onClick = { onSave(nameInput) }
    )
}

@Composable
fun CurrencyPickerSheetContent(
    currentCurrency: String,
    onSelectCurrency: (String) -> Unit
) {
    val options = listOf(
        "₱" to "Philippine Peso (PHP)",
        "$" to "US Dollar (USD)",
        "€" to "Euro (EUR)",
        "£" to "British Pound (GBP)",
        "¥" to "Japanese Yen (JPY)"
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.forEach { (symbol, label) ->
            val selected = symbol == currentCurrency
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) AzureSoftContainer else DarkCard,
                border = BorderStroke(1.dp, if (selected) AzureInkOnWhite else DarkBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { onSelectCurrency(symbol) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$symbol  $label",
                        style = MaterialTheme.typography.titleSmall,
                        color = if (selected) AzureInkOnWhite else TextLightPrimary
                    )
                    if (selected) {
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = AzureInkOnWhite
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PayCycleSheetContent(
    currentSchedule: String,
    currentPaydayReminder: Boolean,
    currentShiftReminder: Boolean,
    onSave: (String, Boolean, Boolean) -> Unit
) {
    var schedule by remember { mutableStateOf(currentSchedule) }
    var paydayRem by remember { mutableStateOf(currentPaydayReminder) }
    var shiftRem by remember { mutableStateOf(currentShiftReminder) }
    val schedules = listOf("Weekly (Fri)", "15th & 30th", "Monthly")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        schedules.forEach { option ->
            val selected = schedule == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) AzurePrimaryButton else DarkCard)
                    .border(
                        width = 1.dp,
                        color = if (selected) AzureAccent else DarkBorder,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { schedule = option }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (selected) PureWhite else TextLightPrimary
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Payday Reminders", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
                    Text("Notify on $schedule cutoffs", style = MaterialTheme.typography.bodySmall, color = TextLightSecondary)
                }
                Switch(
                    checked = paydayRem,
                    onCheckedChange = { paydayRem = it },
                    colors = darkSwitchColors()
                )
            }
            HorizontalDivider(color = DarkBorder)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Shift Check-In Alerts", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
                    Text("15 min before scheduled duty", style = MaterialTheme.typography.bodySmall, color = TextLightSecondary)
                }
                Switch(
                    checked = shiftRem,
                    onCheckedChange = { shiftRem = it },
                    colors = darkSwitchColors()
                )
            }
        }
    }

    ShiftlyPrimaryPillButton(
        text = "Save Pay Cycle",
        onClick = { onSave(schedule, paydayRem, shiftRem) }
    )
}

@Composable
fun ShiftRulesSheetContent(
    currentNdStartHour: Int,
    currentNdMultiplier: Double,
    currentOtMultiplier: Double,
    onSave: (Int, Double, Double) -> Unit
) {
    var ndStart by remember { mutableIntStateOf(currentNdStartHour) }
    var ndMult by remember { mutableDoubleStateOf(currentNdMultiplier) }
    var otMult by remember { mutableDoubleStateOf(currentOtMultiplier) }

    Text("Night Diff Window Start", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(21 to "9 PM", 22 to "10 PM", 23 to "11 PM").forEach { (hour, label) ->
            val selected = ndStart == hour
            SelectableRulePill(
                label = label,
                selected = selected,
                onClick = { ndStart = hour },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Text("Night Diff Bonus Multiplier", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1.10 to "+10%", 1.15 to "+15%", 1.20 to "+20%").forEach { (m, label) ->
            SelectableRulePill(
                label = label,
                selected = kotlin.math.abs(ndMult - m) < 0.01,
                onClick = { ndMult = m },
                modifier = Modifier.weight(1f)
            )
        }
    }

    Text("Overtime Multiplier (After 8h)", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf(1.25 to "1.25x", 1.30 to "1.30x", 1.50 to "1.50x").forEach { (m, label) ->
            SelectableRulePill(
                label = label,
                selected = kotlin.math.abs(otMult - m) < 0.01,
                onClick = { otMult = m },
                modifier = Modifier.weight(1f)
            )
        }
    }

    ShiftlyPrimaryPillButton(
        text = "Save Shift Rules",
        onClick = { onSave(ndStart, ndMult, otMult) }
    )
}

@Composable
private fun SelectableRulePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) AzurePrimaryButton else DarkCard)
            .border(1.dp, if (selected) AzureAccent else DarkBorder, RoundedCornerShape(50))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = if (selected) PureWhite else TextLightPrimary
        )
    }
}

@Composable
fun BudgetsEditorSheetContent(
    currencySymbol: String,
    currentMonthlyBudget: Double,
    currentCategoryBudgets: List<CategoryBudgetSetting>,
    onSave: (Double, List<CategoryBudgetSetting>) -> Unit
) {
    var monthlyInput by remember { mutableStateOf(currentMonthlyBudget.toInt().toString()) }
    val editedBudgets = remember {
        mutableStateListOf<Pair<String, String>>().apply {
            currentCategoryBudgets.forEach { add(it.category to it.monthlyLimit.toInt().toString()) }
        }
    }

    OutlinedTextField(
        value = monthlyInput,
        onValueChange = { monthlyInput = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Monthly Budget ($currencySymbol)", fontFamily = ShiftlySerif) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    Text("Per-Category Limits", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    editedBudgets.forEachIndexed { idx, (cat, limitStr) ->
        OutlinedTextField(
            value = limitStr,
            onValueChange = { updated ->
                editedBudgets[idx] = cat to updated.filter { c -> c.isDigit() || c == '.' }
            },
            label = { Text("$cat ($currencySymbol)", fontFamily = ShiftlySerif) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    ShiftlyPrimaryPillButton(
        text = "Save Budgets",
        onClick = {
            val monthly = monthlyInput.toDoubleOrNull() ?: currentMonthlyBudget
            val list = editedBudgets.map { (cat, raw) ->
                CategoryBudgetSetting(cat, raw.toDoubleOrNull() ?: 500.0)
            }
            onSave(monthly, list)
        }
    )
}

@Composable
fun SavingsGoalSheetContent(
    currencySymbol: String,
    currentTitle: String,
    currentTarget: Double,
    currentBuffer: Double,
    onSave: (String, Double, Double) -> Unit
) {
    var titleInput by remember { mutableStateOf(currentTitle) }
    var targetInput by remember { mutableStateOf(currentTarget.toInt().toString()) }
    var bufferInput by remember { mutableStateOf(currentBuffer.toInt().toString()) }

    OutlinedTextField(
        value = titleInput,
        onValueChange = { titleInput = it },
        label = { Text("Goal Name (e.g., Tuition, Laptop)", fontFamily = ShiftlySerif) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = targetInput,
        onValueChange = { targetInput = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Target Amount ($currencySymbol)", fontFamily = ShiftlySerif) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = bufferInput,
        onValueChange = { bufferInput = it.filter { c -> c.isDigit() || c == '.' } },
        label = { Text("Safe-to-Spend Savings Buffer ($currencySymbol)", fontFamily = ShiftlySerif) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    ShiftlyPrimaryPillButton(
        text = "Save Goal",
        onClick = {
            onSave(
                titleInput,
                targetInput.toDoubleOrNull() ?: currentTarget,
                bufferInput.toDoubleOrNull() ?: currentBuffer
            )
        }
    )
}

@Composable
fun ManageCategoriesSheetContent(
    currentCategories: List<ManagedCategorySetting>,
    onSave: (List<ManagedCategorySetting>) -> Unit
) {
    val list = remember { mutableStateListOf<ManagedCategorySetting>().apply { addAll(currentCategories) } }
    var newCategoryName by remember { mutableStateOf("") }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = { Text("Add Category", fontFamily = ShiftlySerif) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.weight(1f)
        )
        Button(
            onClick = {
                val clean = newCategoryName.trim()
                if (clean.isNotEmpty() && list.none { it.name.equals(clean, true) }) {
                    list.add(ManagedCategorySetting(clean, true))
                    newCategoryName = ""
                }
            },
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(containerColor = AzurePrimaryButton, contentColor = PureWhite),
            modifier = Modifier.heightIn(min = 52.dp)
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add", modifier = Modifier.size(18.dp))
        }
    }

    list.forEachIndexed { idx, item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(DarkCard)
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = item.name,
                onValueChange = { updated -> list[idx] = item.copy(name = updated) },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedBorderColor = DarkBorder,
                    focusedBorderColor = AzureInkOnWhite,
                    focusedTextColor = TextLightPrimary,
                    unfocusedTextColor = TextLightPrimary,
                    focusedContainerColor = DarkSurface,
                    unfocusedContainerColor = DarkSurface
                ),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = item.isVisible,
                onCheckedChange = { vis -> list[idx] = item.copy(isVisible = vis) },
                colors = darkSwitchColors()
            )
        }
    }

    ShiftlyPrimaryPillButton(
        text = "Save Categories",
        onClick = { onSave(list.filter { it.name.isNotBlank() }) }
    )
}

@Composable
fun RecurringEntriesSheetContent(
    currencySymbol: String,
    currentEntries: List<RecurringEntrySetting>,
    onSave: (List<RecurringEntrySetting>) -> Unit
) {
    val entries = remember { mutableStateListOf<RecurringEntrySetting>().apply { addAll(currentEntries) } }
    var newTitle by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        entries.forEachIndexed { idx, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
                    Text(
                        text = "${entry.frequency} • ${if (entry.isIncome) "+" else "-"}$currencySymbol${"%,.0f".format(entry.amount)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = if (entry.isIncome) MintInkOnWhite else CoralInkOnWhite
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = entry.isEnabled,
                        onCheckedChange = { entries[idx] = entry.copy(isEnabled = it) },
                        colors = darkSwitchColors()
                    )
                    IconButton(
                        onClick = { entries.removeAt(idx) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Remove",
                            tint = CoralInkOnWhite
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider(color = DarkBorder)
    Text("Add Recurring Entry", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Label", fontFamily = ShiftlySerif) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.weight(1.2f)
        )
        OutlinedTextField(
            value = newAmount,
            onValueChange = { newAmount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text(currencySymbol, fontFamily = ShiftlySerif) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.weight(0.8f)
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        SelectableRulePill(
            label = "Weekly Allowance (+)",
            selected = isIncome,
            onClick = { isIncome = true },
            modifier = Modifier.weight(1f)
        )
        SelectableRulePill(
            label = "Subscription (-)",
            selected = !isIncome,
            onClick = { isIncome = false },
            modifier = Modifier.weight(1f)
        )
    }

    OutlinedButton(
        onClick = {
            val amt = newAmount.toDoubleOrNull() ?: 0.0
            if (newTitle.isNotBlank() && amt > 0.0) {
                entries.add(
                    RecurringEntrySetting(
                        id = "r_${System.currentTimeMillis()}",
                        title = newTitle.trim(),
                        amount = amt,
                        frequency = if (isIncome) "Weekly" else "Monthly",
                        isIncome = isIncome,
                        isEnabled = true
                    )
                )
                newTitle = ""
                newAmount = ""
            }
        },
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text("Add to Recurring List", fontFamily = ShiftlySerif, color = AzureInkOnWhite)
    }

    ShiftlyPrimaryPillButton(
        text = "Save Recurring Rules",
        onClick = { onSave(entries.toList()) }
    )
}

@Composable
fun AppLockSheetContent(
    currentEnabled: Boolean,
    currentMethod: String,
    currentPin: String,
    onSave: (Boolean, String, String) -> Unit
) {
    var enabled by remember { mutableStateOf(currentEnabled) }
    var method by remember { mutableStateOf(currentMethod) }
    var pin by remember { mutableStateOf(currentPin) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Enable App Lock", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
            Text("Require authentication on launch", style = MaterialTheme.typography.bodySmall, color = TextLightSecondary)
        }
        Switch(
            checked = enabled,
            onCheckedChange = { enabled = it },
            colors = darkSwitchColors()
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("PIN & Fingerprint", "4-Digit PIN", "Fingerprint Only").forEach { option ->
            SelectableRulePill(
                label = option.replace(" Only", ""),
                selected = method == option,
                onClick = { method = option },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (method != "Fingerprint Only") {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(4) },
            label = { Text("4-Digit PIN", fontFamily = ShiftlySerif) },
            placeholder = { Text("1234", color = TextLightMuted) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    ShiftlyPrimaryPillButton(
        text = "Save Security Lock",
        onClick = { onSave(enabled, method, pin) }
    )
}

@Composable
fun BackupAndExportSheetContent(
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onRestoreJson: (String) -> Unit
) {
    var restoreJsonInput by remember { mutableStateOf("") }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onExportJson,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Outlined.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("JSON", fontFamily = ShiftlySerif)
        }
        OutlinedButton(
            onClick = onExportCsv,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Outlined.TableChart, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("CSV", fontFamily = ShiftlySerif)
        }
        OutlinedButton(
            onClick = onExportPdf,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, DarkBorder),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(Icons.Outlined.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("PDF", fontFamily = ShiftlySerif)
        }
    }

    HorizontalDivider(color = DarkBorder)
    Text("Restore From Backup JSON", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    OutlinedTextField(
        value = restoreJsonInput,
        onValueChange = { restoreJsonInput = it },
        label = { Text("Paste exported JSON backup", fontFamily = ShiftlySerif) },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    ShiftlyPrimaryPillButton(
        text = "Restore Data",
        enabled = restoreJsonInput.isNotBlank(),
        leadingIcon = {
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        onClick = { onRestoreJson(restoreJsonInput) }
    )
}
