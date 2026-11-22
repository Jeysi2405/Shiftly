package com.example.shiftly.ui.screens.profile

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyIosSegmentedControl
import com.example.shiftly.ui.components.ShiftlyIosSwitch
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.screens.lock.canUseBiometrics
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.PinSecurity
import com.example.shiftly.utils.ShiftlyFormatters

private val THEME_MODE_OPTIONS = listOf(
    "SYSTEM" to "System Default",
    "DARK" to "Dark Mode",
    "LIGHT" to "Light Mode",
)

private val CURRENCY_OPTIONS = listOf(
    "₱" to "Philippine Peso (PHP)",
    "$" to "US Dollar (USD)",
    "€" to "Euro (EUR)",
    "£" to "British Pound (GBP)",
    "¥" to "Japanese Yen (JPY)"
)

private val APP_LOCK_METHOD_OPTIONS = listOf(
    "PIN & Fingerprint",
    "4-Digit PIN",
    "Fingerprint Only"
)

class ShiftlySheetActionState {
    var onSave: (() -> Unit)? = null
    var enabled by mutableStateOf(true)
}

private fun filterSingleDecimalInput(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

private fun formatEditableNumber(value: Double): String {
    if (!value.isFinite()) return "0"
    return if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)
}

@Composable
private fun shiftlySheetFieldColors(): androidx.compose.material3.TextFieldColors {
    val colors = ShiftlyColors.current
    return OutlinedTextFieldDefaults.colors(
        focusedBorderColor = colors.azureAccent,
        unfocusedBorderColor = Color.Transparent,
        focusedLabelColor = colors.azureAccent,
        unfocusedLabelColor = colors.textSecondary,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.azureAccent,
        focusedContainerColor = colors.cardElevated,
        unfocusedContainerColor = colors.cardElevated
    )
}

/**
 * Tappable Apple-ID-style profile identity card surfacing name, role/workplace,
 * schedule type, weekly target hours, and default hourly rate.
 */
@Composable
fun ProfileIdentityCard(
    userName: String,
    jobTitle: String,
    workplace: String,
    workType: String,
    targetWeeklyHours: Double,
    currencySymbol: String,
    defaultHourlyRate: Double,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val trimmedName = userName.trim()
    val initials = remember(trimmedName) {
        trimmedName.split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
            .take(2)
            .mapNotNull { it.firstOrNull()?.uppercaseChar() }
            .joinToString("")
    }
    val roleSummary = remember(jobTitle, workplace) {
        listOf(jobTitle.trim(), workplace.trim())
            .filter { it.isNotEmpty() }
            .joinToString(" · ")
    }
    val workMetadata = remember(workType, targetWeeklyHours, currencySymbol, defaultHourlyRate) {
        buildList {
            workType.trim().takeIf { it.isNotEmpty() }?.let(::add)
            if (targetWeeklyHours > 0.0) {
                add("${ShiftlyFormatters.formatHours(targetWeeklyHours, compact = true)}/wk")
            }
            if (defaultHourlyRate > 0.0) {
                add("${ShiftlyFormatters.formatCurrency(currencySymbol, defaultHourlyRate)}/hr")
            }
        }.joinToString(" · ")
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.card,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .shiftlyPressClickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.azureContainer),
                contentAlignment = Alignment.Center
            ) {
                if (initials.isNotEmpty()) {
                    Text(
                        text = initials,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = colors.azureAccent
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_user),
                        contentDescription = null,
                        tint = colors.azureAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = trimmedName.ifEmpty { "Set Up Your Profile" },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = roleSummary.ifEmpty { "Tap to add job title, workplace & hourly rate" },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (workMetadata.isNotEmpty()) {
                    Text(
                        text = workMetadata,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ).tabular(),
                        color = colors.azureAccent,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Icon(
                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                contentDescription = "Edit profile",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
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
    ShiftlyGlassCard(cornerRadius = 18.dp, contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "SNAPSHOT",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.7.sp
                ),
                color = colors.textSecondary
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                SnapshotStatColumn(
                    label = "Shifts",
                    value = totalShifts.toString(),
                    valueColor = colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                ShiftlyVerticalHairlineDivider(modifier = Modifier.fillMaxHeight())
                SnapshotStatColumn(
                    label = "Hours",
                    value = ShiftlyFormatters.formatHours(totalHoursWorked, compact = true),
                    valueColor = colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }

            ShiftlyHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min)
            ) {
                SnapshotStatColumn(
                    label = "Earnings",
                    value = ShiftlyFormatters.formatCurrency(currencySymbol, totalEarnings),
                    valueColor = colors.mintTextOnSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                ShiftlyVerticalHairlineDivider(modifier = Modifier.fillMaxHeight())
                SnapshotStatColumn(
                    label = "Expenses",
                    value = ShiftlyFormatters.formatCurrency(currencySymbol, totalExpenses),
                    valueColor = colors.coralTextOnSurface,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun SnapshotStatColumn(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier.padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 19.sp
            ).tabular(),
            color = valueColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.6.sp
            ),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.card,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                content = content
            )
        }
    }
}

@Composable
private fun SettingsRowIconTile(
    @DrawableRes iconRes: Int,
    iconTint: Color
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(iconTint.copy(alpha = if (colors.isDark) 0.18f else 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = iconTint,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsChevronRow(
    @DrawableRes iconRes: Int,
    title: String,
    onClick: () -> Unit,
    subtitle: String? = null,
    iconBg: Color = ShiftlyColors.current.azurePrimary,
    iconTint: Color = iconBg,
    trailingBadge: String? = null,
    trailingText: String? = null
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .shiftlyPressClickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIconTile(iconRes = iconRes, iconTint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
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
                color = colors.surface,
                shadowElevation = 0.dp
            ) {
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.textPrimary,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                )
            }
        }
        if (!trailingText.isNullOrBlank()) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 15.sp
                ),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_lucide_chevron_right),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsSwitchRow(
    @DrawableRes iconRes: Int,
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    onRowClick: (() -> Unit)? = null,
    iconBg: Color = ShiftlyColors.current.mintBright,
    iconTint: Color = iconBg
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .let { mod ->
                if (onRowClick != null) mod.shiftlyPressClickable(onClick = onRowClick) else mod
            }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SettingsRowIconTile(iconRes = iconRes, iconTint = iconTint)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
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
        ShiftlyIosSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
fun SettingsStaticRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            ),
            color = colors.textPrimary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Normal,
                fontSize = 15.sp
            ).tabular(),
            color = Color(0xFF8E8E93)
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
    actionLabel: String? = "Done",
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = onDismiss,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(
                title = title,
                onCancel = onDismiss,
                actionLabel = actionLabel,
                actionEnabled = actionEnabled,
                onAction = onAction
            )
            content()
        }
    }
}

@Composable
fun ThemeSelectorSheetContent(
    currentThemeMode: String,
    onSave: (String) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val colors = ShiftlyColors.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.selectableGroup()
            ) {
                THEME_MODE_OPTIONS.forEachIndexed { index, (modeKey, modeLabel) ->
                    val isSelected = currentThemeMode.equals(modeKey, ignoreCase = true)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 54.dp)
                            .shiftlyPressClickable(role = Role.RadioButton) {
                                onSave(modeKey)
                            }
                            .semantics(mergeDescendants = true) {
                                this.selected = isSelected
                            }
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = modeLabel,
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            ),
                            color = if (isSelected) colors.azureAccent else colors.textPrimary
                        )
                        if (isSelected) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_check),
                                contentDescription = "Selected",
                                tint = colors.azurePrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                    if (index < THEME_MODE_OPTIONS.lastIndex) {
                        ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
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

private val WORK_TYPE_OPTIONS = listOf("Full-Time", "Part-Time", "Freelance")

@Composable
fun AccountEditSheetContent(
    currentName: String,
    jobTitle: String = "",
    workplace: String = "",
    workType: String = "Full-Time",
    targetWeeklyHours: Double = 40.0,
    currencySymbol: String = "₱",
    currentHourlyRate: Double = 0.0,
    onSave: (
        name: String,
        jobTitle: String,
        workplace: String,
        workType: String,
        targetWeeklyHours: Double,
        defaultHourlyRate: Double
    ) -> Unit,
    onDismiss: () -> Unit = {},
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current

    var nameInput by remember { mutableStateOf(currentName) }
    var jobTitleInput by remember { mutableStateOf(jobTitle) }
    var workplaceInput by remember { mutableStateOf(workplace) }
    var selectedWorkType by remember { mutableStateOf(workType.ifBlank { "Full-Time" }) }
    var hoursInput by remember {
        mutableStateOf(if (targetWeeklyHours > 0.0) formatEditableNumber(targetWeeklyHours) else "40")
    }
    var hourlyRateInput by remember {
        mutableStateOf(if (currentHourlyRate > 0.0) formatEditableNumber(currentHourlyRate) else "")
    }

    val isValid = nameInput.isNotBlank()
    val parsedHours = remember(hoursInput) { hoursInput.toDoubleOrNull() ?: 40.0 }
    val parsedHourlyRate = remember(hourlyRateInput) {
        (hourlyRateInput.toDoubleOrNull() ?: 0.0).coerceAtLeast(0.0)
    }

    LaunchedEffect(
        isValid,
        nameInput,
        jobTitleInput,
        workplaceInput,
        selectedWorkType,
        parsedHours,
        parsedHourlyRate
    ) {
        actionState?.enabled = isValid
        actionState?.onSave = {
            if (nameInput.isNotBlank()) {
                onSave(
                    nameInput.trim(),
                    jobTitleInput.trim(),
                    workplaceInput.trim(),
                    selectedWorkType,
                    parsedHours,
                    parsedHourlyRate
                )
            }
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ProfileInsetRow(
                label = "Display Name",
                value = nameInput,
                onValueChange = { nameInput = it },
                placeholder = "Your name"
            )

            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

            ProfileInsetRow(
                label = "Job Title",
                value = jobTitleInput,
                onValueChange = { jobTitleInput = it },
                placeholder = "Role / Job Title"
            )

            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

            ProfileInsetRow(
                label = "Workplace",
                value = workplaceInput,
                onValueChange = { workplaceInput = it },
                placeholder = "Company / Workplace"
            )

            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

            ProfileNumericInsetRow(
                label = "Default Hourly Rate",
                value = hourlyRateInput,
                onValueChange = { hourlyRateInput = filterSingleDecimalInput(it, hourlyRateInput) },
                placeholder = "0.00",
                prefix = currencySymbol,
                suffix = "/hr"
            )

            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Work Type",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(9.dp))
                        .background(colors.surface)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    WORK_TYPE_OPTIONS.forEach { option ->
                        val isSelected = selectedWorkType.equals(option, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSelected) colors.azurePrimary else Color.Transparent)
                                .shiftlyPressClickable(role = Role.Tab) { selectedWorkType = option },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) PureWhite else colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }

            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

            ProfileNumericInsetRow(
                label = "Target Hours",
                value = hoursInput,
                onValueChange = { hoursInput = filterSingleDecimalInput(it, hoursInput) },
                placeholder = "40",
                suffix = "hrs/wk"
            )
        }
    }

    ShiftlySheetBottomButtons(
        onCancel = onDismiss,
        onAction = {
            if (isValid) {
                onSave(
                    nameInput.trim(),
                    jobTitleInput.trim(),
                    workplaceInput.trim(),
                    selectedWorkType,
                    parsedHours,
                    parsedHourlyRate
                )
            }
        },
        cancelLabel = "Cancel",
        actionLabel = "Save",
        actionEnabled = isValid,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ProfileNumericInsetRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    suffix: String,
    prefix: String? = null
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (!prefix.isNullOrEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.textSecondary
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(colors.azureAccent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End
                ).tabular(),
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    color = colors.textSecondary
                                ),
                                textAlign = TextAlign.End
                            )
                        }
                        inner()
                    }
                },
                modifier = Modifier.width(68.dp)
            )
            Text(
                text = suffix,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textSecondary
            )
        }
    }
}

@Composable
private fun ProfileInsetRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary
        )
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            cursorBrush = SolidColor(colors.azureAccent),
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium,
                color = colors.textPrimary,
                textAlign = TextAlign.End
            ),
            decorationBox = { innerTextField ->
                Box(contentAlignment = Alignment.CenterEnd) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                color = colors.textSecondary
                            ),
                            textAlign = TextAlign.End
                        )
                    }
                    innerTextField()
                }
            },
            modifier = Modifier
                .weight(1f)
                .padding(start = 16.dp)
        )
    }
}

@Composable
fun CurrencyPickerSheetContent(
    currentCurrency: String,
    onApplyCurrency: (newSymbol: String, conversionRate: Double?) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    var pendingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var rateInput by remember { mutableStateOf("") }

    val target = pendingTarget
    if (target == null) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.selectableGroup()
                ) {
                    CURRENCY_OPTIONS.forEachIndexed { index, (symbol, label) ->
                        val isSelected = symbol == currentCurrency
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .shiftlyPressClickable(role = Role.Tab) {
                                    if (!isSelected) {
                                        pendingTarget = symbol to label
                                        rateInput = ""
                                    }
                                }
                                .semantics(mergeDescendants = true) {
                                    selected = isSelected
                                    contentDescription = "$label${if (isSelected) ", active" else ""}"
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "$symbol  $label",
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                ),
                                color = if (isSelected) colors.azureAccent else colors.textPrimary
                            )
                            if (isSelected) {
                                Text(
                                    text = "Active",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = colors.azureAccent
                                )
                            }
                        }
                        if (index < CURRENCY_OPTIONS.lastIndex) {
                            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
                        }
                    }
                }
            }

            ShiftlySheetCloseButton(
                label = "Close",
                onClick = onDismiss
            )
        }
    } else {
        val (targetSymbol, targetLabel) = target
        val parsedRate = remember(rateInput) { rateInput.toDoubleOrNull() }
        val rateValid = parsedRate != null && parsedRate.isFinite() && parsedRate > 0.0

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.azureContainer,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Switch $currentCurrency -> $targetSymbol ($targetLabel)",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Choose whether to change only the symbol or convert all saved shifts, expenses, split bills, and budgets by an exchange rate.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }

            ShiftlySecondaryTextButton(
                text = "Switch Symbol Only (Keep Numbers)",
                onClick = { onApplyCurrency(targetSymbol, null) },
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "Or Convert All Saved Amounts",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp
                ),
                color = colors.textPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            OutlinedTextField(
                value = rateInput,
                onValueChange = { input -> rateInput = filterSingleDecimalInput(input, rateInput) },
                label = {
                    Text(
                        "Exchange Rate (1 $currentCurrency = ? $targetSymbol)",
                        fontFamily = InterFontFamily
                    )
                },
                placeholder = {
                    Text(
                        text = "e.g. 0.018 or 56.0",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = shiftlySheetFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )
            if (parsedRate != null && parsedRate.isFinite() && parsedRate > 0.0) {
                val sample = 100.0 * parsedRate
                Text(
                    text = "Preview: ${ShiftlyFormatters.formatCurrency(currentCurrency, 100.0)} -> ${ShiftlyFormatters.formatCurrency(targetSymbol, sample)}",
                    style = MaterialTheme.typography.bodySmall.tabular(),
                    color = colors.mintTextOnSurface
                )
            }

            ShiftlySheetBottomButtons(
                onCancel = { pendingTarget = null },
                onAction = {
                    if (parsedRate != null && parsedRate.isFinite() && parsedRate > 0.0) {
                        onApplyCurrency(targetSymbol, parsedRate)
                    }
                },
                cancelLabel = "Back",
                actionLabel = "Convert",
                actionEnabled = rateValid
            )
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
    ) -> Unit,
    onDismiss: () -> Unit = {},
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    var masterEnabled by remember { mutableStateOf(notificationsEnabled) }
    var billsEnabled by remember { mutableStateOf(billReminderEnabled) }
    var shiftsEnabled by remember { mutableStateOf(shiftReminderEnabled) }
    var savingsEnabled by remember { mutableStateOf(savingsReminderEnabled) }

    LaunchedEffect(masterEnabled, billsEnabled, shiftsEnabled, savingsEnabled) {
        actionState?.enabled = true
        actionState?.onSave = {
            onSave(masterEnabled, billsEnabled, shiftsEnabled, savingsEnabled)
        }
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Enable Notifications",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                ShiftlyIosSwitch(
                    checked = masterEnabled,
                    onCheckedChange = { masterEnabled = it }
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                NotificationToggleRow(
                    title = "Recurring Bill Due Reminders",
                    checked = billsEnabled,
                    enabled = masterEnabled,
                    onCheckedChange = { billsEnabled = it }
                )
                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
                NotificationToggleRow(
                    title = "Scheduled Shift Reminders",
                    checked = shiftsEnabled,
                    enabled = masterEnabled,
                    onCheckedChange = { shiftsEnabled = it }
                )
                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
                NotificationToggleRow(
                    title = "Savings Goal Check-Ins",
                    checked = savingsEnabled,
                    enabled = masterEnabled,
                    onCheckedChange = { savingsEnabled = it }
                )
            }
        }
    }

    ShiftlySheetBottomButtons(
        onCancel = onDismiss,
        onAction = { onSave(masterEnabled, billsEnabled, shiftsEnabled, savingsEnabled) },
        cancelLabel = "Cancel",
        actionLabel = "Save",
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun NotificationToggleRow(
    title: String,
    checked: Boolean,
    enabled: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        ShiftlyIosSwitch(
            checked = checked,
            enabled = enabled,
            onCheckedChange = onCheckedChange
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
    onSave: (monthlyBudget: Double, categoryBudgets: List<CategoryBudgetSetting>, managedCategories: List<ManagedCategorySetting>) -> Unit,
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    var monthlyInput by remember {
        mutableStateOf(if (currentMonthlyBudget > 0.0) formatEditableNumber(currentMonthlyBudget) else "")
    }
    var isMonthlyExpanded by remember { mutableStateOf(false) }
    var isCategoryLimitsExpanded by remember { mutableStateOf(false) }
    var isManageCategoriesExpanded by remember { mutableStateOf(false) }

    val monthlyChevronRotation by animateFloatAsState(
        targetValue = if (isMonthlyExpanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "monthlyChevron"
    )
    val categoryChevronRotation by animateFloatAsState(
        targetValue = if (isCategoryLimitsExpanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "categoryChevron"
    )
    val manageChevronRotation by animateFloatAsState(
        targetValue = if (isManageCategoriesExpanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "manageChevron"
    )

    val editedBudgets = remember(currentCategoryBudgets) {
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

    val managedCategoriesList = remember(managedCategories) {
        mutableStateListOf<ManagedCategorySetting>().apply {
            val initialList = managedCategories.ifEmpty {
                ExpenseViewModel.EXPENSE_CATEGORIES.map { ManagedCategorySetting(it, true) }
            }
            addAll(initialList)
        }
    }
    var newCategoryName by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<ManagedCategorySetting?>(null) }

    LaunchedEffect(managedCategories) {
        val visibleNames = managedCategories
            .filter {
                it.isVisible &&
                    it.name.isNotBlank() &&
                    !it.name.equals("Split Bill", ignoreCase = true) &&
                    !it.name.equals("All", ignoreCase = true)
            }
            .map { it.name.trim() }
        val visibleKeys = visibleNames.map { it.lowercase() }.toSet()
        val existingByKey = editedBudgets.associate { it.first.lowercase() to it.second }
        val budgetKeys = currentCategoryBudgets.map { it.category.trim().lowercase() }.toSet()
        val rebuilt = mutableListOf<Pair<String, String>>()
        val seen = mutableSetOf<String>()
        editedBudgets.forEach { (name, value) ->
            val key = name.lowercase()
            if ((key in visibleKeys || (key in budgetKeys && value.isNotBlank())) && seen.add(key)) {
                rebuilt.add(name to value)
            }
        }
        visibleNames.forEach { name ->
            val key = name.lowercase()
            if (seen.add(key)) {
                rebuilt.add(name to (existingByKey[key] ?: ""))
            }
        }
        if (rebuilt != editedBudgets.toList()) {
            editedBudgets.clear()
            editedBudgets.addAll(rebuilt)
        }
    }

    val performSave = {
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
        val cleanManaged = managedCategoriesList.filter { it.name.isNotBlank() }
        onSave(monthly, list, cleanManaged)
    }

    LaunchedEffect(monthlyInput, editedBudgets.toList(), managedCategoriesList.toList()) {
        actionState?.enabled = true
        actionState?.onSave = performSave
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Collapsible Card 1: Monthly Budget
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .shiftlyPressClickable { isMonthlyExpanded = !isMonthlyExpanded }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Monthly Budget",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val parsedMonthly = monthlyInput.toDoubleOrNull()
                        val displaySummary = if (parsedMonthly != null && parsedMonthly > 0.0) {
                            ShiftlyFormatters.formatCurrency(currencySymbol, parsedMonthly)
                        } else {
                            "Optional"
                        }
                        Text(
                            text = displaySummary,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = if (parsedMonthly != null && parsedMonthly > 0.0) colors.azureAccent else colors.textSecondary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_chevron_right),
                            contentDescription = if (isMonthlyExpanded) "Collapse monthly budget" else "Expand monthly budget",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = monthlyChevronRotation }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isMonthlyExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 4.dp, bottom = 14.dp)
                    ) {
                        CompactBudgetAmountField(
                            currencySymbol = currencySymbol,
                            value = monthlyInput,
                            onValueChange = { monthlyInput = filterSingleDecimalInput(it, monthlyInput) },
                            placeholder = "Set overall monthly limit",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Collapsible Card 2: Per-Category Limits
        if (editedBudgets.isNotEmpty()) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .shiftlyPressClickable { isCategoryLimitsExpanded = !isCategoryLimitsExpanded }
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Per-Category Limits",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = colors.textPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            val activeCapsCount = editedBudgets.count { it.second.isNotBlank() }
                            Text(
                                text = if (activeCapsCount > 0) "$activeCapsCount set" else "Optional",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = if (activeCapsCount > 0) colors.azureAccent else colors.textSecondary
                            )
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                                contentDescription = if (isCategoryLimitsExpanded) "Collapse category limits" else "Expand category limits",
                                tint = colors.textSecondary,
                                modifier = Modifier
                                    .size(20.dp)
                                    .graphicsLayer { rotationZ = categoryChevronRotation }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isCategoryLimitsExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 6.dp)
                        ) {
                            editedBudgets.forEachIndexed { idx, (cat, limitStr) ->
                                if (idx > 0) {
                                    ShiftlyHairlineDivider(startInset = 50.dp, endInset = 12.dp)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 52.dp)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShiftlyIosIconTile(
                                        iconRes = categoryIcon(cat),
                                        backgroundColor = colors.coralBright,
                                        size = 28.dp,
                                        cornerRadius = 8.dp,
                                        iconSize = 20.dp
                                    )
                                    Text(
                                        text = cat,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f)
                                    )
                                    CompactBudgetAmountField(
                                        currencySymbol = currencySymbol,
                                        value = limitStr,
                                        onValueChange = { updated ->
                                            editedBudgets[idx] = cat to filterSingleDecimalInput(updated, limitStr)
                                        },
                                        placeholder = "No cap",
                                        modifier = Modifier.width(114.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Collapsible Card 3: Manage Categories
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .shiftlyPressClickable { isManageCategoriesExpanded = !isManageCategoriesExpanded }
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Manage Categories",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val activeCatCount = managedCategoriesList.count { it.isVisible }
                        Text(
                            text = "$activeCatCount active",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.azureAccent
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_chevron_right),
                            contentDescription = if (isManageCategoriesExpanded) "Collapse manage categories" else "Expand manage categories",
                            tint = colors.textSecondary,
                            modifier = Modifier
                                .size(20.dp)
                                .graphicsLayer { rotationZ = manageChevronRotation }
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isManageCategoriesExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 6.dp)
                    ) {
                        // Compact Add Category Row
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.surface)
                                    .padding(horizontal = 12.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                if (newCategoryName.isEmpty()) {
                                    Text(
                                        text = "Add Category",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                                        color = colors.textSecondary
                                    )
                                }
                                BasicTextField(
                                    value = newCategoryName,
                                    onValueChange = { newCategoryName = it },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    ),
                                    cursorBrush = SolidColor(colors.azureAccent),
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colors.azurePrimary,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .shiftlyPressClickable {
                                        val clean = newCategoryName.trim()
                                        if (clean.isNotEmpty() && managedCategoriesList.none { it.name.equals(clean, true) }) {
                                            managedCategoriesList.add(ManagedCategorySetting(clean, true))
                                            if (
                                                !clean.equals("Split Bill", ignoreCase = true) &&
                                                !clean.equals("All", ignoreCase = true) &&
                                                editedBudgets.none { it.first.equals(clean, ignoreCase = true) }
                                            ) {
                                                editedBudgets.add(clean to "")
                                            }
                                            newCategoryName = ""
                                        }
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_plus),
                                        contentDescription = "Add category",
                                        tint = PureWhite,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }

                        if (managedCategoriesList.isNotEmpty()) {
                            ShiftlyHairlineDivider(startInset = 12.dp, endInset = 12.dp)
                        }

                        managedCategoriesList.forEachIndexed { idx, item ->
                            if (idx > 0) {
                                ShiftlyHairlineDivider(startInset = 50.dp, endInset = 12.dp)
                            }
                            val isCustomCategory = ExpenseViewModel.EXPENSE_CATEGORIES.none {
                                it.equals(item.name.trim(), ignoreCase = true)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 52.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShiftlyIosIconTile(
                                    iconRes = categoryIcon(item.name),
                                    backgroundColor = colors.coralBright,
                                    size = 28.dp,
                                    cornerRadius = 8.dp,
                                    iconSize = 20.dp
                                )
                                BasicTextField(
                                    value = item.name,
                                    onValueChange = { updated -> managedCategoriesList[idx] = item.copy(name = updated) },
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        color = colors.textPrimary
                                    ),
                                    cursorBrush = SolidColor(colors.azureAccent),
                                    modifier = Modifier.weight(1f)
                                )
                                if (isCustomCategory) {
                                    IconButton(
                                        onClick = { pendingDeleteCategory = item },
                                        modifier = Modifier.size(48.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_lucide_trash_2),
                                            contentDescription = "Delete ${item.name}",
                                            tint = colors.coralInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                ShiftlyIosSwitch(
                                    checked = item.isVisible,
                                    onCheckedChange = { vis -> managedCategoriesList[idx] = item.copy(isVisible = vis) }
                                )
                            }
                        }
                    }
                }
            }
        }

        ShiftlySheetBottomButtons(
            onCancel = onDismiss,
            onAction = performSave,
            cancelLabel = "Cancel",
            actionLabel = "Save",
            modifier = Modifier.padding(top = 4.dp)
        )
    }

    pendingDeleteCategory?.let { categoryToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Category?",
            message = "This custom category \"${categoryToDelete.name}\" and its budget limit will be removed.",
            onDismiss = { pendingDeleteCategory = null },
            onConfirm = {
                managedCategoriesList.removeAll { it.name.equals(categoryToDelete.name, ignoreCase = true) }
                editedBudgets.removeAll { it.first.equals(categoryToDelete.name.trim(), ignoreCase = true) }
                pendingDeleteCategory = null
            }
        )
    }
}

@Composable
private fun CompactBudgetAmountField(
    currencySymbol: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = modifier
            .heightIn(min = 48.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = currencySymbol,
            style = MaterialTheme.typography.bodySmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = colors.textSecondary
        )
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
        ) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.textSecondary,
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                cursorBrush = SolidColor(colors.azureAccent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End
                ).tabular(),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun ManageCategoriesSheetContent(
    currentCategories: List<ManagedCategorySetting>,
    onSave: (List<ManagedCategorySetting>) -> Unit,
    onDismiss: () -> Unit = {},
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    val list = remember { mutableStateListOf<ManagedCategorySetting>().apply { addAll(currentCategories) } }
    var newCategoryName by remember { mutableStateOf("") }
    var pendingDeleteCategory by remember { mutableStateOf<ManagedCategorySetting?>(null) }

    LaunchedEffect(list.toList()) {
        actionState?.enabled = true
        actionState?.onSave = { onSave(list.filter { it.name.isNotBlank() }) }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedTextField(
            value = newCategoryName,
            onValueChange = { newCategoryName = it },
            label = { Text("Add Category", fontFamily = InterFontFamily) },
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
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = colors.azurePrimary, contentColor = PureWhite),
            elevation = ButtonDefaults.buttonElevation(0.dp, 0.dp),
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
        val isCustomCategory = ExpenseViewModel.EXPENSE_CATEGORIES.none {
            it.equals(item.name.trim(), ignoreCase = true)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(colors.cardElevated)
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
                    unfocusedBorderColor = Color.Transparent,
                    focusedBorderColor = colors.azureAccent,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface
                ),
                modifier = Modifier.weight(1f)
            )
            if (isCustomCategory) {
                IconButton(
                    onClick = { pendingDeleteCategory = item },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_trash_2),
                        contentDescription = "Delete ${item.name}",
                        tint = colors.coralInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            ShiftlyIosSwitch(
                checked = item.isVisible,
                onCheckedChange = { vis -> list[idx] = item.copy(isVisible = vis) }
            )
        }
    }

    ShiftlySheetBottomButtons(
        onCancel = onDismiss,
        onAction = { onSave(list.filter { it.name.isNotBlank() }) },
        cancelLabel = "Cancel",
        actionLabel = "Save"
    )

    pendingDeleteCategory?.let { categoryToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Category?",
            message = "This custom category \"${categoryToDelete.name}\" will be removed.",
            onDismiss = { pendingDeleteCategory = null },
            onConfirm = {
                list.removeAll { it.name.equals(categoryToDelete.name, ignoreCase = true) }
                pendingDeleteCategory = null
            }
        )
    }
}

@Composable
fun AppLockSheetContent(
    currentEnabled: Boolean,
    currentMethod: String,
    hasSavedPin: Boolean,
    onSave: (Boolean, String, String?) -> Unit,
    onDismiss: () -> Unit = {},
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val biometricsAvailable = remember(context) { canUseBiometrics(context) }

    var enabled by remember { mutableStateOf(currentEnabled) }
    var method by remember { mutableStateOf(currentMethod) }
    var newPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val submit = {
        when {
            !enabled -> onSave(false, method, if (PinSecurity.isValidPinFormat(newPin)) newPin else null)
            method == "Fingerprint Only" -> onSave(true, method, null)
            PinSecurity.isValidPinFormat(newPin) -> onSave(true, method, newPin)
            newPin.isEmpty() && hasSavedPin -> onSave(true, method, null)
            else -> errorMessage = "Please enter a full 4-digit PIN."
        }
    }

    LaunchedEffect(enabled, method, newPin, hasSavedPin) {
        actionState?.enabled = true
        actionState?.onSave = submit
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.cardElevated)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Enable App Lock",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary,
            modifier = Modifier.weight(1f)
        )
        ShiftlyIosSwitch(
            checked = enabled,
            onCheckedChange = {
                enabled = it
                errorMessage = null
            }
        )
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            APP_LOCK_METHOD_OPTIONS.forEachIndexed { index, option ->
                val isSelected = method == option
                val displayLabel = option.replace(" Only", "")
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .shiftlyPressClickable(role = Role.RadioButton) {
                            method = option
                            errorMessage = null
                        }
                        .semantics(mergeDescendants = true) {
                            selected = isSelected
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayLabel,
                        style = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                        ),
                        color = if (isSelected) colors.azureAccent else colors.textPrimary
                    )
                    if (isSelected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_check),
                            contentDescription = "Selected",
                            tint = colors.azurePrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                if (index < APP_LOCK_METHOD_OPTIONS.lastIndex) {
                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
                }
            }
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
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "4-Digit PIN",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )
                BasicTextField(
                    value = newPin,
                    onValueChange = {
                        newPin = it.filter(Char::isDigit).take(4)
                        errorMessage = null
                    },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    cursorBrush = SolidColor(colors.azureAccent),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        textAlign = TextAlign.End,
                        letterSpacing = 4.sp
                    ).tabular(),
                    decorationBox = { inner ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (newPin.isEmpty()) {
                                Text(
                                    text = if (hasSavedPin) "Leave blank to keep" else "••••",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        color = colors.textSecondary,
                                        letterSpacing = 0.sp
                                    ),
                                    textAlign = TextAlign.End
                                )
                            }
                            inner()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 16.dp)
                )
            }
        }
    }

    if (!errorMessage.isNullOrBlank()) {
        Text(
            text = errorMessage.orEmpty(),
            style = MaterialTheme.typography.bodySmall,
            color = colors.coralTextOnSurface
        )
    }

    ShiftlySheetBottomButtons(
        onCancel = onDismiss,
        onAction = submit,
        cancelLabel = "Cancel",
        actionLabel = "Save",
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
fun BackupAndExportSheetContent(
    onExportJson: () -> Unit,
    onRestoreBackup: () -> Unit,
    onExportShiftsCsv: () -> Unit,
    onExportExpensesCsv: () -> Unit,
    onDismiss: () -> Unit = {}
) {
    val colors = ShiftlyColors.current

    Text(
        text = "BACKUP & RESTORE",
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = InterFontFamily,
            letterSpacing = 0.5.sp
        ),
        color = colors.textSecondary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExportOptionInsetRow(
                iconRes = R.drawable.ic_lucide_cloud_upload,
                label = "Export JSON Backup",
                iconTint = colors.azureAccent,
                onClick = onExportJson
            )

            ShiftlyHairlineDivider(startInset = 42.dp, endInset = 14.dp)

            ExportOptionInsetRow(
                iconRes = R.drawable.ic_lucide_rotate_ccw,
                label = "Restore from Backup",
                iconTint = colors.azureAccent,
                onClick = onRestoreBackup
            )
        }
    }

    Text(
        text = "SPREADSHEETS",
        style = MaterialTheme.typography.labelSmall.copy(
            fontFamily = InterFontFamily,
            letterSpacing = 0.5.sp
        ),
        color = colors.textSecondary,
        modifier = Modifier.padding(horizontal = 4.dp)
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            ExportOptionInsetRow(
                iconRes = R.drawable.ic_lucide_file_spreadsheet,
                label = "Export Shifts (CSV)",
                iconTint = colors.mintInk,
                onClick = onExportShiftsCsv
            )

            ShiftlyHairlineDivider(startInset = 42.dp, endInset = 14.dp)

            ExportOptionInsetRow(
                iconRes = R.drawable.ic_lucide_file_spreadsheet,
                label = "Export Expenses (CSV)",
                iconTint = colors.coralInk,
                onClick = onExportExpensesCsv
            )
        }
    }

    ShiftlySheetCloseButton(
        label = "Close",
        onClick = onDismiss,
        modifier = Modifier.padding(top = 4.dp)
    )
}

@Composable
private fun ExportOptionInsetRow(
    @DrawableRes iconRes: Int,
    label: String,
    iconTint: Color = ShiftlyColors.current.azureAccent,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .shiftlyPressClickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textPrimary
            )
        }
        Icon(
            painter = painterResource(R.drawable.ic_lucide_chevron_right),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(16.dp)
        )
    }
}

enum class LegalTopic(val sheetTitle: String) {
    TERMS("Terms & Conditions"),
    PRIVACY("Privacy Policy"),
    DISCLAIMER("Payroll & Tax Disclaimer"),
    TERMS_AND_PRIVACY("Terms & Privacy Policy")
}

private data class LegalClause(
    val heading: String,
    val body: String
)

private data class LegalSection(
    val sectionLabel: String? = null,
    val clauses: List<LegalClause>
)

private fun termsClauses(): List<LegalClause> = listOf(
    LegalClause(
        heading = "Personal Tracker (\"As Is\")",
        body = "Shiftly is a personal shift, allowance, and expense tracker provided \"as is\" without warranties of any kind. You remain responsible for reviewing and verifying your own logged hours, entries, and totals."
    ),
    LegalClause(
        heading = "Local Data & Backup Responsibility",
        body = "Because Shiftly keeps all records locally on your device, you are responsible for exporting or backing up your data (JSON backup or CSV spreadsheets) before uninstalling the app, clearing app data, or switching devices."
    )
)

private fun privacyClauses(): List<LegalClause> = listOf(
    LegalClause(
        heading = "100% Offline & Local-First",
        body = "Shiftly runs completely offline with no accounts, analytics trackers, or cloud sync. Every shift, allowance, budget, recurring bill, split bill, and expense entry stays strictly on your device and is never uploaded to external servers."
    ),
    LegalClause(
        heading = "Receipts & Biometric Lock",
        body = "Attached receipt photos and App Lock credentials (hashed PINs and biometric lock state) are stored privately on your device and are never shared with third parties."
    )
)

private fun disclaimerClauses(): List<LegalClause> = listOf(
    LegalClause(
        heading = "Reference Estimates Only",
        body = "Shift pay calculations, night differential, overtime and holiday multipliers, and Philippine statutory deduction estimates (SSS, PhilHealth, Pag-IBIG, and Withholding Tax) are provided strictly for personal reference."
    ),
    LegalClause(
        heading = "Not Official Payroll or Tax Advice",
        body = "Calculations in Shiftly do not constitute official employer payroll statements, accounting, or tax advice. Always rely on official employer payslips and current government contribution tables for formal filings."
    )
)

private fun legalSectionsFor(topic: LegalTopic): List<LegalSection> = when (topic) {
    LegalTopic.TERMS -> listOf(LegalSection(clauses = termsClauses()))
    LegalTopic.PRIVACY -> listOf(LegalSection(clauses = privacyClauses()))
    LegalTopic.DISCLAIMER -> listOf(LegalSection(clauses = disclaimerClauses()))
    LegalTopic.TERMS_AND_PRIVACY -> listOf(
        LegalSection(sectionLabel = "TERMS & CONDITIONS", clauses = termsClauses()),
        LegalSection(sectionLabel = "PRIVACY POLICY", clauses = privacyClauses())
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LegalInfoBottomSheet(
    topic: LegalTopic,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val sections = remember(topic) { legalSectionsFor(topic) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(title = topic.sheetTitle)

            sections.forEach { section ->
                if (!section.sectionLabel.isNullOrBlank()) {
                    Text(
                        text = section.sectionLabel,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.6.sp
                        ),
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        section.clauses.forEachIndexed { index, clause ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = clause.heading,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = clause.body,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        lineHeight = 19.sp
                                    ),
                                    color = colors.textSecondary
                                )
                            }
                            if (index < section.clauses.lastIndex) {
                                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
                            }
                        }
                    }
                }
            }

            ShiftlySheetCloseButton(
                label = "Close",
                onClick = onDismiss,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

private val FEEDBACK_TYPE_OPTIONS = listOf("Suggestion", "Bug Report")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackBottomSheet(
    defaultName: String,
    onDismiss: () -> Unit,
    onSendFeedback: (
        feedbackType: String,
        subject: String,
        senderName: String,
        details: String,
        includeDeviceInfo: Boolean
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(FEEDBACK_TYPE_OPTIONS.first()) }
    var subjectInput by remember { mutableStateOf("") }
    var nameInput by remember(defaultName) { mutableStateOf(defaultName.trim()) }
    var detailsInput by remember { mutableStateOf("") }
    var includeDeviceInfo by remember { mutableStateOf(true) }

    val canSend = detailsInput.isNotBlank()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(title = "Send Feedback")

            ShiftlyIosSegmentedControl(
                options = FEEDBACK_TYPE_OPTIONS,
                selectedOption = selectedType,
                onOptionSelected = { selectedType = it }
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    FeedbackInsetTextField(
                        label = "Subject",
                        value = subjectInput,
                        onValueChange = { subjectInput = it },
                        placeholder = "Short summary (e.g., Feature idea or UI bug)",
                        singleLine = true,
                        capitalization = KeyboardCapitalization.Sentences
                    )

                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                    FeedbackInsetTextField(
                        label = "Name (Optional)",
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        placeholder = "Your name (optional)",
                        singleLine = true,
                        capitalization = KeyboardCapitalization.Words
                    )

                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                    FeedbackInsetTextField(
                        label = "Details",
                        value = detailsInput,
                        onValueChange = { detailsInput = it },
                        placeholder = "Describe your suggestion or the steps to reproduce the bug...",
                        singleLine = false,
                        minHeight = 110.dp,
                        capitalization = KeyboardCapitalization.Sentences
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .shiftlyPressClickable { includeDeviceInfo = !includeDeviceInfo }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Include app & device info",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary,
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 12.dp)
                    )
                    ShiftlyIosSwitch(
                        checked = includeDeviceInfo,
                        onCheckedChange = { includeDeviceInfo = it }
                    )
                }
            }

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    if (canSend) {
                        onSendFeedback(
                            selectedType,
                            subjectInput.trim(),
                            nameInput.trim(),
                            detailsInput.trim(),
                            includeDeviceInfo
                        )
                    }
                },
                cancelLabel = "Cancel",
                actionLabel = "Send",
                actionEnabled = canSend,
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

@Composable
private fun FeedbackInsetTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    singleLine: Boolean,
    minHeight: Dp = 48.dp,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = minHeight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = colors.textSecondary
        )
        Box(modifier = Modifier.fillMaxWidth()) {
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        lineHeight = 20.sp
                    ),
                    color = colors.textSecondary.copy(alpha = 0.72f)
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = singleLine,
                keyboardOptions = KeyboardOptions(capitalization = capitalization),
                cursorBrush = SolidColor(colors.azureAccent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal,
                    color = colors.textPrimary,
                    lineHeight = 20.sp
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .let { mod ->
                        if (!singleLine) mod.heightIn(min = minHeight - 36.dp) else mod
                    }
            )
        }
    }
}
