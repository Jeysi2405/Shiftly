package com.example.shiftly.ui.screens.profile

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.material.icons.outlined.FolderZip
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.screens.lock.canUseBiometrics
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
import com.example.shiftly.utils.PinSecurity

private fun filterSingleDecimalInput(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

private fun formatEditableNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
}

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
                        text = "Full ZIP + receipts, JSON snapshot, CSV sheet, or PDF statement",
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
                    text = "Export Full Backup (.zip)",
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
                .imePadding()
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
    onApplyCurrency: (newSymbol: String, conversionRate: Double?) -> Unit
) {
    val options = listOf(
        "₱" to "Philippine Peso (PHP)",
        "$" to "US Dollar (USD)",
        "€" to "Euro (EUR)",
        "£" to "British Pound (GBP)",
        "¥" to "Japanese Yen (JPY)"
    )
    var pendingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var rateInput by remember { mutableStateOf("") }

    val target = pendingTarget
    if (target == null) {
        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            options.forEach { (symbol, label) ->
                val isSelected = symbol == currentCurrency
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) AzureSoftContainer else DarkCard,
                    border = BorderStroke(1.dp, if (isSelected) AzureInkOnWhite else DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab) {
                            if (!isSelected) {
                                pendingTarget = symbol to label
                                rateInput = ""
                            }
                        }
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                            contentDescription = "$label${if (isSelected) ", active" else ""}"
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$symbol  $label",
                            style = MaterialTheme.typography.titleSmall,
                            color = if (isSelected) AzureInkOnWhite else TextLightPrimary
                        )
                        if (isSelected) {
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
    } else {
        val (targetSymbol, targetLabel) = target
        val parsedRate = rateInput.toDoubleOrNull()
        val rateValid = parsedRate != null && parsedRate.isFinite() && parsedRate > 0.0

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = AzureSoftContainer,
                border = BorderStroke(1.dp, AzureAccent.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Switch $currentCurrency -> $targetSymbol ($targetLabel)",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                        color = TextLightPrimary
                    )
                    Text(
                        text = "Choose whether to change only the symbol or convert all saved shifts, expenses, split bills, and budgets by an exchange rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextLightSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = { onApplyCurrency(targetSymbol, null) },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, DarkBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Switch Symbol Only (Keep Numbers)",
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                )
            }

            HorizontalDivider(color = DarkBorder)

            Text(
                text = "Or Convert All Saved Amounts",
                style = MaterialTheme.typography.titleSmall,
                color = TextLightPrimary
            )
            OutlinedTextField(
                value = rateInput,
                onValueChange = { input -> rateInput = filterSingleDecimalInput(input, rateInput) },
                label = {
                    Text(
                        "Exchange Rate (1 $currentCurrency = ? $targetSymbol)",
                        fontFamily = ShiftlySerif
                    )
                },
                placeholder = { Text("e.g. 0.018 or 56.0", color = TextLightMuted) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = darkSheetFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            if (rateValid && parsedRate != null) {
                val sample = 100.0 * parsedRate
                Text(
                    text = "Preview: ${currentCurrency}100.00 -> $targetSymbol${"%,.2f".format(sample)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = MintTextOnBlue
                )
            }

            ShiftlyPrimaryPillButton(
                text = "Convert Amounts by Rate",
                enabled = rateValid,
                onClick = {
                    if (parsedRate != null && parsedRate.isFinite() && parsedRate > 0.0) {
                        onApplyCurrency(targetSymbol, parsedRate)
                    }
                }
            )

            TextButton(
                onClick = { pendingTarget = null },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text("Back to Currency List", fontFamily = ShiftlySerif, color = TextLightSecondary)
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
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        schedules.forEach { option ->
            val isSelected = schedule == option
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) AzurePrimaryButton else DarkCard)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) AzureAccent else DarkBorder,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(role = Role.Tab) { schedule = option }
                    .semantics(mergeDescendants = true) {
                        selected = isSelected
                    }
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (isSelected) PureWhite else TextLightPrimary
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
                    Text("Daily reminder if no shift logged", style = MaterialTheme.typography.bodySmall, color = TextLightSecondary)
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
            },
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
    managedCategories: List<ManagedCategorySetting> = emptyList(),
    onSave: (Double, List<CategoryBudgetSetting>) -> Unit
) {
    var monthlyInput by remember { mutableStateOf(formatEditableNumber(currentMonthlyBudget)) }
    val editedBudgets = remember(currentCategoryBudgets, managedCategories) {
        mutableStateListOf<Pair<String, String>>().apply {
            val seen = mutableSetOf<String>()
            currentCategoryBudgets.forEach { item ->
                val key = item.category.trim().lowercase()
                if (key.isNotEmpty() && seen.add(key)) {
                    add(item.category.trim() to formatEditableNumber(item.monthlyLimit))
                }
            }
            managedCategories
                .filter {
                    it.isVisible &&
                        it.name.isNotBlank() &&
                        !it.name.equals("Split Bill", ignoreCase = true) &&
                        !it.name.equals("All", ignoreCase = true)
                }
                .forEach { cat ->
                    val key = cat.name.trim().lowercase()
                    if (seen.add(key)) {
                        add(cat.name.trim() to "")
                    }
                }
        }
    }

    OutlinedTextField(
        value = monthlyInput,
        onValueChange = { monthlyInput = filterSingleDecimalInput(it, monthlyInput) },
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
                editedBudgets[idx] = cat to filterSingleDecimalInput(updated, limitStr)
            },
            label = { Text("$cat ($currencySymbol)", fontFamily = ShiftlySerif) },
            placeholder = { Text("Optional monthly cap", color = TextLightMuted) },
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
            val parsedMonthly = monthlyInput.toDoubleOrNull()
            val monthly = if (parsedMonthly != null && parsedMonthly.isFinite() && parsedMonthly >= 0.0) {
                parsedMonthly
            } else {
                currentMonthlyBudget
            }
            val list = editedBudgets.mapNotNull { (cat, raw) ->
                val limit = raw.toDoubleOrNull()
                if (limit != null && limit.isFinite() && limit > 0.0) CategoryBudgetSetting(cat, limit) else null
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
    var targetInput by remember { mutableStateOf(formatEditableNumber(currentTarget)) }
    var bufferInput by remember { mutableStateOf(formatEditableNumber(currentBuffer)) }

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
        onValueChange = { targetInput = filterSingleDecimalInput(it, targetInput) },
        label = { Text("Target Amount ($currencySymbol)", fontFamily = ShiftlySerif) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    OutlinedTextField(
        value = bufferInput,
        onValueChange = { bufferInput = filterSingleDecimalInput(it, bufferInput) },
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
            val parsedTarget = targetInput.toDoubleOrNull()
            val validTarget = if (parsedTarget != null && parsedTarget.isFinite() && parsedTarget >= 1.0) {
                parsedTarget
            } else {
                currentTarget
            }
            val parsedBuffer = bufferInput.toDoubleOrNull()
            val validBuffer = if (parsedBuffer != null && parsedBuffer.isFinite() && parsedBuffer >= 0.0) {
                parsedBuffer
            } else {
                currentBuffer
            }
            onSave(titleInput, validTarget, validBuffer)
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
    availableCategories: List<String> = listOf("Food", "Transport", "Bills", "Supplies", "Other"),
    onSave: (List<RecurringEntrySetting>) -> Unit
) {
    val entries = remember { mutableStateListOf<RecurringEntrySetting>().apply { addAll(currentEntries) } }
    val selectableCategories = remember(availableCategories) {
        availableCategories
            .filter { !it.equals("Split Bill", ignoreCase = true) && !it.equals("All", ignoreCase = true) }
            .ifEmpty { listOf("Other") }
    }
    var newTitle by remember { mutableStateOf("") }
    var newAmount by remember { mutableStateOf("") }
    var isIncome by remember { mutableStateOf(true) }
    var newCategory by remember(selectableCategories) { mutableStateOf(selectableCategories.first()) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entries.isEmpty()) {
            Text(
                text = "No recurring rules yet. Add weekly allowances or monthly bills below.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightMuted
            )
        }
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
                    val subtitle = if (entry.isIncome) {
                        "${entry.frequency} • +$currencySymbol${"%,.0f".format(entry.amount)}"
                    } else {
                        "${entry.frequency} • ${entry.category} • -$currencySymbol${"%,.0f".format(entry.amount)}"
                    }
                    Text(
                        text = subtitle,
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
                            contentDescription = "Remove ${entry.title}",
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
            onValueChange = { newAmount = filterSingleDecimalInput(it, newAmount) },
            label = { Text(currencySymbol, fontFamily = ShiftlySerif) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.weight(0.8f)
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
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

    if (!isIncome) {
        Text("Expense Category", style = MaterialTheme.typography.labelMedium, color = TextLightSecondary)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            selectableCategories.forEach { cat ->
                val isSelected = cat.equals(newCategory, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (isSelected) AzurePrimaryButton else DarkCard)
                        .border(1.dp, if (isSelected) AzureAccent else DarkBorder, RoundedCornerShape(50))
                        .clickable(role = Role.Tab) { newCategory = cat }
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                            contentDescription = "$cat expense category"
                        }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isSelected) PureWhite else TextLightPrimary
                    )
                }
            }
        }
    }

    OutlinedButton(
        onClick = {
            val amt = newAmount.toDoubleOrNull() ?: 0.0
            if (newTitle.isNotBlank() && amt.isFinite() && amt > 0.0) {
                entries.add(
                    RecurringEntrySetting(
                        id = "r_${System.currentTimeMillis()}",
                        title = newTitle.trim(),
                        amount = amt,
                        frequency = if (isIncome) "Weekly" else "Monthly",
                        isIncome = isIncome,
                        isEnabled = true,
                        category = if (isIncome) "Income" else newCategory
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
    hasSavedPin: Boolean,
    onSave: (Boolean, String, String?) -> Unit
) {
    val context = LocalContext.current
    val biometricsAvailable = remember(context) { canUseBiometrics(context) }

    var enabled by remember { mutableStateOf(currentEnabled) }
    var method by remember { mutableStateOf(currentMethod) }
    var newPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
            Text(
                "Lock balances on launch and when backgrounded",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                errorMessage = null
            },
            colors = darkSwitchColors()
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        listOf("PIN & Fingerprint", "4-Digit PIN", "Fingerprint Only").forEach { option ->
            SelectableRulePill(
                label = option.replace(" Only", ""),
                selected = method == option,
                onClick = {
                    method = option
                    errorMessage = null
                },
                modifier = Modifier.weight(1f)
            )
        }
    }

    if (method != "4-Digit PIN" && !biometricsAvailable) {
        Text(
            text = "No fingerprint/biometric enrolled on this device. Device lock or 4-Digit PIN will be used.",
            style = MaterialTheme.typography.bodySmall,
            color = CoralTextOnBlue
        )
    }

    if (method != "Fingerprint Only") {
        OutlinedTextField(
            value = newPin,
            onValueChange = {
                newPin = it.filter(Char::isDigit).take(4)
                errorMessage = null
            },
            label = {
                Text(
                    if (hasSavedPin) "New 4-Digit PIN (leave blank to keep current)" else "Set 4-Digit PIN",
                    fontFamily = ShiftlySerif
                )
            },
            placeholder = { Text(if (hasSavedPin) "••••" else "Enter 4 digits", color = TextLightMuted) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp),
            colors = darkSheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (!errorMessage.isNullOrBlank()) {
        Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = CoralTextOnBlue
        )
    }

    ShiftlyPrimaryPillButton(
        text = "Save Security Lock",
        onClick = {
            if (!enabled) {
                onSave(false, method, if (PinSecurity.isValidPinFormat(newPin)) newPin else null)
                return@ShiftlyPrimaryPillButton
            }
            if (method == "Fingerprint Only") {
                onSave(true, method, null)
                return@ShiftlyPrimaryPillButton
            }
            when {
                PinSecurity.isValidPinFormat(newPin) -> {
                    onSave(true, method, newPin)
                }
                newPin.isEmpty() && hasSavedPin -> {
                    onSave(true, method, null)
                }
                else -> {
                    errorMessage = "Please enter a full 4-digit PIN."
                }
            }
        }
    )
}

@Composable
fun BackupAndExportSheetContent(
    onExportZip: () -> Unit,
    onExportJson: () -> Unit,
    onExportCsv: () -> Unit,
    onExportPdf: () -> Unit,
    onPickBackupFile: (RestoreMode) -> Unit,
    onRestoreJson: (String, RestoreMode) -> Unit
) {
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreMode by remember { mutableStateOf(RestoreMode.MERGE) }

    OutlinedButton(
        onClick = onExportZip,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, AzureAccent.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(Icons.Outlined.FolderZip, contentDescription = null, tint = AzureAccent, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Export Full Backup (.zip + Receipts)", fontFamily = ShiftlySerif)
    }

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
    Text("Restore Mode", style = MaterialTheme.typography.titleSmall, color = TextLightPrimary)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SelectableRulePill(
            label = "Merge (No Dupes)",
            selected = restoreMode == RestoreMode.MERGE,
            onClick = { restoreMode = RestoreMode.MERGE },
            modifier = Modifier.weight(1f)
        )
        SelectableRulePill(
            label = "Replace All",
            selected = restoreMode == RestoreMode.REPLACE,
            onClick = { restoreMode = RestoreMode.REPLACE },
            modifier = Modifier.weight(1f)
        )
    }
    if (restoreMode == RestoreMode.REPLACE) {
        Text(
            text = "Replace All erases your current shifts, expenses, split bills, and receipts before restoring.",
            style = MaterialTheme.typography.bodySmall,
            color = CoralTextOnBlue
        )
    }

    OutlinedButton(
        onClick = { onPickBackupFile(restoreMode) },
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, DarkBorder),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextLightPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(Icons.Outlined.Restore, contentDescription = null, tint = MintTextOnBlue, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text("Choose Backup File (.zip or .json)", fontFamily = ShiftlySerif)
    }

    OutlinedTextField(
        value = restoreJsonInput,
        onValueChange = { restoreJsonInput = it },
        label = { Text("Or paste exported JSON backup", fontFamily = ShiftlySerif) },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(16.dp),
        colors = darkSheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    ShiftlyPrimaryPillButton(
        text = if (restoreMode == RestoreMode.REPLACE) "Replace & Restore JSON" else "Merge & Restore JSON",
        enabled = restoreJsonInput.isNotBlank(),
        leadingIcon = {
            Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(18.dp))
        },
        onClick = { onRestoreJson(restoreJsonInput, restoreMode) }
    )
}
