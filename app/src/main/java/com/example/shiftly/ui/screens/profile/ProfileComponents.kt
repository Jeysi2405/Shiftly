package com.example.shiftly.ui.screens.profile

import androidx.annotation.DrawableRes
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
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
import coil.compose.AsyncImage
import com.example.shiftly.R
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.screens.expense.CategoryPromptSheet
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.screens.lock.canUseBiometrics
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.PinSecurity
import com.example.shiftly.utils.ShiftlyFormatters
import java.io.File

private fun filterSingleDecimalInput(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

private fun formatEditableNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
}

@Composable
private fun shiftlySwitchColors(): androidx.compose.material3.SwitchColors {
    val colors = ShiftlyColors.current
    return SwitchDefaults.colors(
        checkedThumbColor = colors.onAccentDark,
        checkedTrackColor = colors.mintBright,
        uncheckedThumbColor = colors.textSecondary,
        uncheckedTrackColor = colors.surface,
        uncheckedBorderColor = colors.border
    )
}

@Composable
private fun shiftlySheetFieldColors(): androidx.compose.material3.TextFieldColors {
    val colors = ShiftlyColors.current
    return OutlinedTextFieldDefaults.colors(
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
}

@Composable
fun ProfileSnapshotGlassCard(
    currencySymbol: String,
    totalShifts: Int,
    totalHoursWorked: Double,
    totalEarnings: Double,
    totalExpenses: Double
) {
    val colors = ShiftlyColors.current
    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "SNAPSHOT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = colors.textSecondary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSnapshotTile(
                    label = "Shifts",
                    value = totalShifts.toString(),
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                GlassSnapshotTile(
                    label = "Hours",
                    value = ShiftlyFormatters.formatHours(totalHoursWorked, compact = true),
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                GlassSnapshotTile(
                    label = "Earnings",
                    value = ShiftlyFormatters.formatCurrency(currencySymbol, totalEarnings),
                    valueColor = colors.mintTextOnSurface,
                    modifier = Modifier.weight(1f)
                )
                GlassSnapshotTile(
                    label = "Expenses",
                    value = ShiftlyFormatters.formatCurrency(currencySymbol, totalExpenses),
                    valueColor = colors.coralTextOnSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
fun ProfileWageRateCard(
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
        mutableStateOf(if (defaultHourlyRate > 0.0) formatEditableNumber(defaultHourlyRate) else "")
    }
    var minWageInput by remember {
        mutableStateOf(if (minimumWagePerDay > 0.0) formatEditableNumber(minimumWagePerDay) else "")
    }

    LaunchedEffect(defaultHourlyRate, minimumWagePerDay) {
        if (!hasInitializedMode && (defaultHourlyRate > 0.0 || minimumWagePerDay > 0.0)) {
            hasInitializedMode = true
            isEditing = defaultHourlyRate <= 0.0
        }
        if (!isEditing) {
            hourlyInput = if (defaultHourlyRate > 0.0) formatEditableNumber(defaultHourlyRate) else ""
            minWageInput = if (minimumWagePerDay > 0.0) formatEditableNumber(minimumWagePerDay) else ""
        }
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
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
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.mintContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_banknote),
                            contentDescription = null,
                            tint = colors.mintTextOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = "WAGE & RATE SETTINGS",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = colors.textSecondary
                    )
                }

                if (!isEditing) {
                    IconButton(
                        onClick = {
                            hourlyInput = if (defaultHourlyRate > 0.0) formatEditableNumber(defaultHourlyRate) else ""
                            minWageInput = if (minimumWagePerDay > 0.0) formatEditableNumber(minimumWagePerDay) else ""
                            isEditing = true
                        },
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .border(1.dp, colors.border, CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_pencil),
                            contentDescription = "Edit wage rates",
                            tint = colors.azureAccent,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                } else if (defaultHourlyRate > 0.0) {
                    TextButton(
                        onClick = {
                            hourlyInput = formatEditableNumber(defaultHourlyRate)
                            minWageInput = if (minimumWagePerDay > 0.0) formatEditableNumber(minimumWagePerDay) else ""
                            isEditing = false
                        },
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (!isEditing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    GlassSnapshotTile(
                        label = "Hourly Rate",
                        value = "${ShiftlyFormatters.formatCurrency(currencySymbol, defaultHourlyRate)}/hr",
                        valueColor = colors.mintTextOnSurface,
                        modifier = Modifier.weight(1f)
                    )
                    GlassSnapshotTile(
                        label = "Min Wage / Day",
                        value = if (minimumWagePerDay > 0.0) {
                            "${ShiftlyFormatters.formatCurrency(currencySymbol, minimumWagePerDay)}/day"
                        } else {
                            "Not set"
                        },
                        valueColor = if (minimumWagePerDay > 0.0) colors.textPrimary else colors.textMuted,
                        modifier = Modifier.weight(1f)
                    )
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = hourlyInput,
                        onValueChange = { hourlyInput = filterSingleDecimalInput(it, hourlyInput) },
                        label = { Text("Hourly ($currencySymbol/hr)", fontFamily = PlusJakartaSans) },
                        placeholder = {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                                color = colors.textMuted
                            )
                        },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        colors = shiftlySheetFieldColors(),
                        modifier = Modifier.weight(1f)
                    )

                    OutlinedTextField(
                        value = minWageInput,
                        onValueChange = { minWageInput = filterSingleDecimalInput(it, minWageInput) },
                        label = { Text("Min Wage/Day", fontFamily = PlusJakartaSans) },
                        placeholder = {
                            Text(
                                text = "0",
                                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                                color = colors.textMuted
                            )
                        },
                        prefix = {
                            Text(
                                text = currencySymbol,
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(16.dp),
                        colors = shiftlySheetFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                }

                val parsedMinWageForHelper = minWageInput.toDoubleOrNull()
                if (parsedMinWageForHelper != null && parsedMinWageForHelper.isFinite() && parsedMinWageForHelper > 0.0) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val derivedHourly = parsedMinWageForHelper / 8.0
                        Text(
                            text = "8h equivalent: ${ShiftlyFormatters.formatCurrency(currencySymbol, derivedHourly)}/hr",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.textSecondary
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.azureContainer,
                            border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    hourlyInput = formatEditableNumber(derivedHourly)
                                }
                        ) {
                            Text(
                                text = "Use ÷ 8h",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureInk,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        val parsedMinWage = (minWageInput.toDoubleOrNull() ?: 0.0)
                            .takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
                        val rawHourly = hourlyInput.toDoubleOrNull()
                        val resolvedHourly = when {
                            rawHourly != null && rawHourly.isFinite() && rawHourly > 0.0 -> rawHourly
                            parsedMinWage > 0.0 -> parsedMinWage / 8.0
                            else -> 0.0
                        }
                        if (!resolvedHourly.isFinite() || resolvedHourly <= 0.0) {
                            onShowMessage("Enter an hourly rate or daily minimum wage above 0")
                            return@Button
                        }
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSaveRates(resolvedHourly, parsedMinWage)
                        hourlyInput = formatEditableNumber(resolvedHourly)
                        minWageInput = if (parsedMinWage > 0.0) formatEditableNumber(parsedMinWage) else ""
                        isEditing = false
                        onShowMessage("Wage settings saved")
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.azurePrimary,
                        contentColor = colors.onAzurePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.onAzurePrimary
                    )
                }
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
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textSecondary
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSans,
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
fun SettingsGroupCard(
    groupTitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ShiftlyColors.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = groupTitle,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.cardBorder),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(content = content)
        }
    }
}

@Composable
fun SettingsChevronRow(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    iconBg: Color = ShiftlyColors.current.azureContainer,
    iconTint: Color = ShiftlyColors.current.azureInk,
    trailingBadge: String? = null
) {
    val colors = ShiftlyColors.current
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
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailingBadge != null) {
            Surface(
                shape = RoundedCornerShape(50),
                color = colors.cardElevated,
                border = BorderStroke(1.dp, colors.cardBorder),
                shadowElevation = 0.dp
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
        Icon(
            painter = painterResource(R.drawable.ic_lucide_chevron_right),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun SettingsSwitchRow(
    @DrawableRes iconRes: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    onRowClick: (() -> Unit)? = null,
    iconBg: Color = ShiftlyColors.current.mintContainer,
    iconTint: Color = ShiftlyColors.current.mintInk
) {
    val colors = ShiftlyColors.current
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
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = shiftlySwitchColors()
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
    showTopClose: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
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
                    color = colors.textPrimary
                )
                if (showTopClose) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_x),
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun ThemeSelectorSheetContent(
    currentThemeMode: String,
    onSave: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    val modes = listOf(
        "SYSTEM" to "System Default",
        "DARK" to "Dark Mode",
        "LIGHT" to "Light Mode"
    )

    Column(
        modifier = Modifier.selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        modes.forEach { (modeKey, modeLabel) ->
            val isSelected = currentThemeMode.equals(modeKey, ignoreCase = true)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (isSelected) colors.azureContainer else colors.card,
                border = BorderStroke(
                    width = if (isSelected) 1.5.dp else 1.dp,
                    color = if (isSelected) colors.azureAccent else colors.cardBorder
                ),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.RadioButton) {
                        onSave(modeKey)
                    }
                    .semantics(mergeDescendants = true) {
                        this.selected = isSelected
                    }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = modeLabel,
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (isSelected) colors.azureInk else colors.textPrimary
                    )
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(colors.azurePrimary),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_check),
                                contentDescription = "Selected",
                                tint = colors.onAzurePrimary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AccountEditSheetContent(
    currentName: String,
    profilePhotoPath: String?,
    hourlyRate: Double,
    currencySymbol: String,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    var nameInput by remember { mutableStateOf(currentName) }
    val hasPhoto = !profilePhotoPath.isNullOrBlank() && File(profilePhotoPath).exists()

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.card)
                .border(1.5.dp, colors.azureAccent.copy(alpha = 0.7f), CircleShape)
                .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            if (hasPhoto && profilePhotoPath != null) {
                AsyncImage(
                    model = File(profilePhotoPath),
                    contentDescription = "Profile photo",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(64.dp)
                )
            } else {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_user),
                    contentDescription = "Profile photo",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = onChangePhoto,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.6f)),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_camera),
                        contentDescription = null,
                        tint = colors.azureAccent,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (hasPhoto) "Change Photo" else "Add Photo",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSans),
                        color = colors.azureAccent
                    )
                }
                if (hasPhoto) {
                    TextButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(
                            text = "Remove",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSans),
                            color = colors.coralTextOnSurface
                        )
                    }
                }
            }
        }
    }

    OutlinedTextField(
        value = nameInput,
        onValueChange = { nameInput = it },
        label = { Text("Display Name", fontFamily = PlusJakartaSans) },
        placeholder = {
            Text(
                text = "What I gonna call you?",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                color = colors.textMuted
            )
        },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        colors = shiftlySheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )
    Text(
        text = "Hourly rate: ${ShiftlyFormatters.formatCurrency(currencySymbol, hourlyRate)}/hr (edit on Wage & Rate card)",
        style = MaterialTheme.typography.bodySmall.tabular(),
        color = colors.textSecondary
    )
    ShiftlyPrimaryPillButton(
        text = "Save Profile",
        enabled = nameInput.isNotBlank(),
        onClick = { onSave(nameInput.trim()) }
    )
}

@Composable
fun CurrencyPickerSheetContent(
    currentCurrency: String,
    onApplyCurrency: (newSymbol: String, conversionRate: Double?) -> Unit
) {
    val colors = ShiftlyColors.current
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
                    color = if (isSelected) colors.azureContainer else colors.card,
                    border = BorderStroke(1.dp, if (isSelected) colors.azureInk else colors.cardBorder),
                    shadowElevation = 0.dp,
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
                            color = if (isSelected) colors.azureInk else colors.textPrimary
                        )
                        if (isSelected) {
                            Text(
                                text = "Active",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.azureInk
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
                color = colors.azureContainer,
                border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.45f)),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Switch $currentCurrency -> $targetSymbol ($targetLabel)",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Choose whether to change only the symbol or convert all saved shifts, expenses, split bills, and budgets by an exchange rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            OutlinedButton(
                onClick = { onApplyCurrency(targetSymbol, null) },
                shape = RoundedCornerShape(50),
                border = BorderStroke(1.dp, colors.border),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
            ) {
                Text(
                    text = "Switch Symbol Only (Keep Numbers)",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            HorizontalDivider(color = colors.border)

            Text(
                text = "Or Convert All Saved Amounts",
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            OutlinedTextField(
                value = rateInput,
                onValueChange = { input -> rateInput = filterSingleDecimalInput(input, rateInput) },
                label = {
                    Text(
                        "Exchange Rate (1 $currentCurrency = ? $targetSymbol)",
                        fontFamily = PlusJakartaSans
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. 0.018 or 56.0",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textMuted
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = shiftlySheetFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            if (rateValid && parsedRate != null) {
                val sample = 100.0 * parsedRate
                Text(
                    text = "Preview: ${ShiftlyFormatters.formatCurrency(currentCurrency, 100.0)} -> ${ShiftlyFormatters.formatCurrency(targetSymbol, sample)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.mintTextOnSurface
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
                Text(
                    text = "Back to Currency List",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun NotificationsSheetContent(
    notificationsEnabled: Boolean,
    billReminderEnabled: Boolean,
    shiftReminderEnabled: Boolean,
    savingsReminderEnabled: Boolean,
    onSave: (
        notificationsEnabled: Boolean,
        billReminderEnabled: Boolean,
        shiftReminderEnabled: Boolean,
        savingsReminderEnabled: Boolean
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    var masterEnabled by remember { mutableStateOf(notificationsEnabled) }
    var billRem by remember { mutableStateOf(billReminderEnabled) }
    var shiftRem by remember { mutableStateOf(shiftReminderEnabled) }
    var savingsRem by remember { mutableStateOf(savingsReminderEnabled) }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp
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
                    Text(
                        text = "Allow Notifications",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Master toggle for all reminders",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = masterEnabled,
                    onCheckedChange = { masterEnabled = it },
                    colors = shiftlySwitchColors()
                )
            }
            HorizontalDivider(color = colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Recurring Bills Due",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Remind less than 1 day before due date",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = masterEnabled && billRem,
                    enabled = masterEnabled,
                    onCheckedChange = { billRem = it },
                    colors = shiftlySwitchColors()
                )
            }
            HorizontalDivider(color = colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Log Shift Reminder",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Daily reminder if no shift logged",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = masterEnabled && shiftRem,
                    enabled = masterEnabled,
                    onCheckedChange = { shiftRem = it },
                    colors = shiftlySwitchColors()
                )
            }
            HorizontalDivider(color = colors.border)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Savings & Deposit",
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Remind to log saving & add deposit",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
                Switch(
                    checked = masterEnabled && savingsRem,
                    enabled = masterEnabled,
                    onCheckedChange = { savingsRem = it },
                    colors = shiftlySwitchColors()
                )
            }
        }
    }

    ShiftlyPrimaryPillButton(
        text = "Save Notifications",
        onClick = { onSave(masterEnabled, billRem, shiftRem, savingsRem) }
    )
}

@Composable
private fun SelectableRulePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.azurePrimary else colors.card)
            .border(1.dp, if (selected) colors.azureAccent else colors.cardBorder, RoundedCornerShape(50))
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = if (selected) colors.onAzurePrimary else colors.textPrimary
        )
    }
}

@Composable
fun BudgetsEditorSheetContent(
    currencySymbol: String,
    currentMonthlyBudget: Double,
    currentCategoryBudgets: List<CategoryBudgetSetting>,
    managedCategories: List<ManagedCategorySetting> = emptyList(),
    onDismiss: () -> Unit = {},
    onSave: (Double, List<CategoryBudgetSetting>) -> Unit
) {
    val colors = ShiftlyColors.current
    var monthlyInput by remember {
        mutableStateOf(if (currentMonthlyBudget > 0.0) formatEditableNumber(currentMonthlyBudget) else "")
    }
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
        label = { Text("Monthly Budget ($currencySymbol)", fontFamily = PlusJakartaSans) },
        placeholder = {
            Text(
                text = "What is your monthly budget?",
                style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                color = colors.textMuted
            )
        },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(16.dp),
        colors = shiftlySheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    Text("Per-Category Limits", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
    editedBudgets.forEachIndexed { idx, (cat, limitStr) ->
        OutlinedTextField(
            value = limitStr,
            onValueChange = { updated ->
                editedBudgets[idx] = cat to filterSingleDecimalInput(updated, limitStr)
            },
            label = { Text("$cat ($currencySymbol)", fontFamily = PlusJakartaSans) },
            placeholder = {
                Text(
                    text = "Optional monthly cap",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textMuted
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = shiftlySheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    ShiftlyPrimaryPillButton(
        text = "Save Budgets",
        onClick = {
            val parsedMonthly = monthlyInput.toDoubleOrNull()
            val monthly = when {
                monthlyInput.isBlank() -> 0.0
                parsedMonthly != null && parsedMonthly.isFinite() && parsedMonthly >= 0.0 -> parsedMonthly
                else -> currentMonthlyBudget
            }
            val list = editedBudgets.mapNotNull { (cat, raw) ->
                val limit = raw.toDoubleOrNull()
                if (limit != null && limit.isFinite() && limit > 0.0) CategoryBudgetSetting(cat, limit) else null
            }
            onSave(monthly, list)
        }
    )

    OutlinedButton(
        onClick = onDismiss,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = "Close",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
            color = colors.textSecondary
        )
    }
}

@Composable
fun ManageCategoriesSheetContent(
    currentCategories: List<ManagedCategorySetting>,
    onSave: (List<ManagedCategorySetting>) -> Unit
) {
    val colors = ShiftlyColors.current
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
            label = { Text("Add Category", fontFamily = PlusJakartaSans) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = shiftlySheetFieldColors(),
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
            colors = ButtonDefaults.buttonColors(containerColor = colors.azurePrimary, contentColor = colors.onAzurePrimary),
            modifier = Modifier.heightIn(min = 52.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_plus),
                contentDescription = "Add",
                modifier = Modifier.size(18.dp)
            )
        }
    }

    list.forEachIndexed { idx, item ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(colors.card)
                .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
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
                    unfocusedBorderColor = colors.border,
                    focusedBorderColor = colors.azureInk,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = item.isVisible,
                onCheckedChange = { vis -> list[idx] = item.copy(isVisible = vis) },
                colors = shiftlySwitchColors()
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
    val colors = ShiftlyColors.current
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
    var showCategoryPrompt by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (entries.isEmpty()) {
            Text(
                text = "No recurring entries",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        entries.forEachIndexed { idx, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.card)
                    .border(1.dp, colors.cardBorder, RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(entry.title, style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
                    val subtitle = if (entry.isIncome) {
                        "${entry.frequency} • ${ShiftlyFormatters.formatCurrency(currencySymbol, entry.amount, showPlus = true)}"
                    } else {
                        "${entry.frequency} • ${entry.category} • ${ShiftlyFormatters.formatCurrency(currencySymbol, entry.amount, showMinus = true)}"
                    }
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = if (entry.isIncome) colors.mintInk else colors.coralInk
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = entry.isEnabled,
                        onCheckedChange = { entries[idx] = entry.copy(isEnabled = it) },
                        colors = shiftlySwitchColors()
                    )
                    IconButton(
                        onClick = { entries.removeAt(idx) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_trash_2),
                            contentDescription = "Remove ${entry.title}",
                            tint = colors.coralInk,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }

    HorizontalDivider(color = colors.border)
    Text("Add Entry", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Label", fontFamily = PlusJakartaSans) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = shiftlySheetFieldColors(),
            modifier = Modifier.weight(1.2f)
        )
        OutlinedTextField(
            value = newAmount,
            onValueChange = { newAmount = filterSingleDecimalInput(it, newAmount) },
            label = { Text(currencySymbol, fontFamily = PlusJakartaSans) },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(14.dp),
            colors = shiftlySheetFieldColors(),
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
            label = "Allowance (+)",
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.cardBorder),
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showCategoryPrompt = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.coralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(categoryIcon(newCategory)),
                            contentDescription = null,
                            tint = colors.coralInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Expense Category",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                        Text(
                            text = newCategory,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_right),
                    contentDescription = "Select Category",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    if (showCategoryPrompt) {
        CategoryPromptSheet(
            categoryOptions = selectableCategories,
            selectedCategory = newCategory,
            onSelectCategory = { newCategory = it },
            onDismiss = { showCategoryPrompt = false }
        )
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
        border = BorderStroke(1.dp, colors.border),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Text(
            text = "Add to List",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
            color = colors.azureInk
        )
    }

    ShiftlyPrimaryPillButton(
        text = "Save Rules",
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
    val colors = ShiftlyColors.current
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
            .background(colors.card)
            .border(1.dp, colors.cardBorder, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Enable App Lock", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
            Text(
                "Lock balances on launch and when backgrounded",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        }
        Switch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                errorMessage = null
            },
            colors = shiftlySwitchColors()
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
            color = colors.coralTextOnSurface
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
                    fontFamily = PlusJakartaSans
                )
            },
            placeholder = {
                Text(
                    text = if (hasSavedPin) "••••" else "Enter 4 digits",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textMuted
                )
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(16.dp),
            colors = shiftlySheetFieldColors(),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (!errorMessage.isNullOrBlank()) {
        Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.coralTextOnSurface
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
    val colors = ShiftlyColors.current
    var restoreJsonInput by remember { mutableStateOf("") }
    var restoreMode by remember { mutableStateOf(RestoreMode.MERGE) }

    OutlinedButton(
        onClick = onExportZip,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.5f)),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lucide_folder_archive),
            contentDescription = null,
            tint = colors.azureAccent,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Export Full Backup (.zip + Receipts)",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans)
        )
    }

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(
            onClick = onExportJson,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_cloud_upload),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "JSON",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans)
            )
        }
        OutlinedButton(
            onClick = onExportCsv,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_file_spreadsheet),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "CSV",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans)
            )
        }
        OutlinedButton(
            onClick = onExportPdf,
            shape = RoundedCornerShape(50),
            border = BorderStroke(1.dp, colors.border),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_file_text),
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "PDF",
                style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans)
            )
        }
    }

    HorizontalDivider(color = colors.border)
    Text("Restore Mode", style = MaterialTheme.typography.titleSmall, color = colors.textPrimary)
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
            color = colors.coralTextOnSurface
        )
    }

    OutlinedButton(
        onClick = { onPickBackupFile(restoreMode) },
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, colors.border),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lucide_rotate_ccw),
            contentDescription = null,
            tint = colors.mintTextOnSurface,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Choose Backup File (.zip or .json)",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans)
        )
    }

    OutlinedTextField(
        value = restoreJsonInput,
        onValueChange = { restoreJsonInput = it },
        label = { Text("Or paste exported JSON backup", fontFamily = PlusJakartaSans) },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(16.dp),
        colors = shiftlySheetFieldColors(),
        modifier = Modifier.fillMaxWidth()
    )

    ShiftlyPrimaryPillButton(
        text = if (restoreMode == RestoreMode.REPLACE) "Replace & Restore JSON" else "Merge & Restore JSON",
        enabled = restoreJsonInput.isNotBlank(),
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_download),
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
        },
        onClick = { onRestoreJson(restoreJsonInput, restoreMode) }
    )
}
