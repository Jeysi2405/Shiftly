package com.example.shiftly.ui.screens.profile

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.shiftly.R
import com.example.shiftly.data.backup.RestoreMode
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.ui.screens.expense.ExpenseViewModel
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyIosSwitch
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.expense.CategoryPromptSheet
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.screens.lock.canUseBiometrics
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.PinSecurity
import com.example.shiftly.utils.ShiftlyFormatters
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val THEME_MODE_OPTIONS = listOf(
    "SYSTEM" to "System Default",
    "DARK" to "Dark Mode",
    "LIGHT" to "Light Mode"
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

private val DEFAULT_RECURRING_CATEGORIES = listOf("Food", "Transport", "Bills", "Supplies", "Other")

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
            color = colors.cardElevated,
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
    iconBg: Color = ShiftlyColors.current.azurePrimary,
    iconTint: Color = PureWhite,
    trailingBadge: String? = null
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftlyIosIconTile(
            iconRes = iconRes,
            backgroundColor = iconBg,
            size = 36.dp,
            cornerRadius = 9.dp,
            iconSize = 18.dp,
            iconTint = iconTint
        )
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
        Icon(
            painter = painterResource(R.drawable.ic_lucide_chevron_right),
            contentDescription = null,
            tint = colors.textSecondary,
            modifier = Modifier.size(18.dp)
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
    iconBg: Color = ShiftlyColors.current.mintBright,
    iconTint: Color = PureWhite
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 58.dp)
            .let { mod ->
                if (onRowClick != null) mod.clickable(onClick = onRowClick) else mod
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ShiftlyIosIconTile(
            iconRes = iconRes,
            backgroundColor = iconBg,
            size = 36.dp,
            cornerRadius = 9.dp,
            iconSize = 18.dp,
            iconTint = iconTint
        )
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
    onSave: (String) -> Unit
) {
    val colors = ShiftlyColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.selectableGroup()) {
            THEME_MODE_OPTIONS.forEachIndexed { index, (modeKey, modeLabel) ->
                if (index > 0) {
                    ShiftlyHairlineDivider(startInset = 16.dp)
                }
                val isSelected = currentThemeMode.equals(modeKey, ignoreCase = true)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 54.dp)
                        .clickable(role = Role.RadioButton) {
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
                                tint = PureWhite,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private val WORK_TYPE_OPTIONS = listOf("Full-Time", "Part-Time", "Freelance")

@Suppress("UNUSED_PARAMETER")
@Composable
fun AccountEditSheetContent(
    currentName: String,
    jobTitle: String = "",
    workplace: String = "",
    workType: String = "Full-Time",
    targetWeeklyHours: Double = 40.0,
    profilePhotoPath: String?,
    hourlyRate: Double = 0.0,
    currencySymbol: String = "₱",
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (name: String, jobTitle: String, workplace: String, workType: String, targetWeeklyHours: Double) -> Unit,
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val density = LocalDensity.current
    val avatarSizePx = remember(density) { with(density) { 64.dp.roundToPx() } }

    var nameInput by remember { mutableStateOf(currentName) }
    var jobTitleInput by remember { mutableStateOf(jobTitle) }
    var workplaceInput by remember { mutableStateOf(workplace) }
    var selectedWorkType by remember { mutableStateOf(if (workType.isBlank()) "Full-Time" else workType) }
    var hoursInput by remember {
        mutableStateOf(if (targetWeeklyHours > 0.0) formatEditableNumber(targetWeeklyHours) else "40")
    }

    val photoFile by produceState<File?>(initialValue = null, key1 = profilePhotoPath) {
        value = if (profilePhotoPath.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                val file = File(profilePhotoPath)
                if (file.exists()) file else null
            }
        }
    }
    val hasPhoto = photoFile != null
    val avatarRequest = remember(photoFile, avatarSizePx, context) {
        photoFile?.let { file ->
            ImageRequest.Builder(context)
                .data(file)
                .size(avatarSizePx)
                .scale(Scale.FILL)
                .crossfade(true)
                .build()
        }
    }

    val isValid = nameInput.isNotBlank()
    val parsedHours = remember(hoursInput) { hoursInput.toDoubleOrNull() ?: 40.0 }

    LaunchedEffect(isValid, nameInput, jobTitleInput, workplaceInput, selectedWorkType, parsedHours) {
        actionState?.enabled = isValid
        actionState?.onSave = {
            if (nameInput.isNotBlank()) {
                onSave(
                    nameInput.trim(),
                    jobTitleInput.trim(),
                    workplaceInput.trim(),
                    selectedWorkType,
                    parsedHours
                )
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(64.dp)
                .clip(CircleShape)
                .background(colors.cardElevated)
                .clickable(onClick = onChangePhoto),
            contentAlignment = Alignment.Center
        ) {
            if (avatarRequest != null) {
                AsyncImage(
                    model = avatarRequest,
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
                ShiftlySecondaryTextButton(
                    text = if (hasPhoto) "Change Photo" else "Add Photo",
                    onClick = onChangePhoto,
                    leadingIcon = {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_camera),
                            contentDescription = null,
                            tint = colors.azureAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
                if (hasPhoto) {
                    TextButton(
                        onClick = onRemovePhoto,
                        modifier = Modifier.heightIn(min = 44.dp)
                    ) {
                        Text(
                            text = "Remove",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                            color = colors.coralTextOnSurface
                        )
                    }
                }
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
            ShiftlyHairlineDivider(startInset = 16.dp)

            ProfileInsetRow(
                label = "Job Title",
                value = jobTitleInput,
                onValueChange = { jobTitleInput = it },
                placeholder = "Role / Job Title"
            )
            ShiftlyHairlineDivider(startInset = 16.dp)

            ProfileInsetRow(
                label = "Workplace",
                value = workplaceInput,
                onValueChange = { workplaceInput = it },
                placeholder = "Company / Workplace"
            )
            ShiftlyHairlineDivider(startInset = 16.dp)

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
                                .height(35.dp)
                                .clip(RoundedCornerShape(7.dp))
                                .background(if (isSelected) colors.azurePrimary else Color.Transparent)
                                .clickable { selectedWorkType = option },
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
            ShiftlyHairlineDivider(startInset = 16.dp)

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Target Hours",
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
                    BasicTextField(
                        value = hoursInput,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) hoursInput = filtered
                        },
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
                                if (hoursInput.isEmpty()) {
                                    Text(
                                        text = "40",
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
                        modifier = Modifier.width(54.dp)
                    )
                    Text(
                        text = "hrs/wk",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary
                    )
                }
            }
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

@Suppress("UNUSED_PARAMETER")
@Composable
fun AccountEditSheetContent(
    currentName: String,
    profilePhotoPath: String?,
    hourlyRate: Double,
    currencySymbol: String,
    onChangePhoto: () -> Unit,
    onRemovePhoto: () -> Unit,
    onSave: (String) -> Unit,
    actionState: ShiftlySheetActionState? = null
) {
    AccountEditSheetContent(
        currentName = currentName,
        jobTitle = "",
        workplace = "",
        workType = "Full-Time",
        targetWeeklyHours = 40.0,
        profilePhotoPath = profilePhotoPath,
        hourlyRate = hourlyRate,
        currencySymbol = currencySymbol,
        onChangePhoto = onChangePhoto,
        onRemovePhoto = onRemovePhoto,
        onSave = { name, _, _, _, _ -> onSave(name) },
        actionState = actionState
    )
}

@Composable
fun CurrencyPickerSheetContent(
    currentCurrency: String,
    onApplyCurrency: (newSymbol: String, conversionRate: Double?) -> Unit
) {
    val colors = ShiftlyColors.current
    var pendingTarget by remember { mutableStateOf<Pair<String, String>?>(null) }
    var rateInput by remember { mutableStateOf("") }

    val target = pendingTarget
    if (target == null) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.selectableGroup()) {
                CURRENCY_OPTIONS.forEachIndexed { index, (symbol, label) ->
                    if (index > 0) {
                        ShiftlyHairlineDivider(startInset = 16.dp)
                    }
                    val isSelected = symbol == currentCurrency
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
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
                }
            }
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

            ShiftlyHairlineDivider()

            Text(
                text = "Or Convert All Saved Amounts",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontSize = 15.sp
                ),
                color = colors.textPrimary
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

            ShiftlySecondaryTextButton(
                text = "Back to Currency List",
                onClick = { pendingTarget = null },
                modifier = Modifier.fillMaxWidth()
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
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    var masterEnabled by remember { mutableStateOf(notificationsEnabled) }
    var billRem by remember { mutableStateOf(billReminderEnabled) }
    var shiftRem by remember { mutableStateOf(shiftReminderEnabled) }
    var savingsRem by remember { mutableStateOf(savingsReminderEnabled) }

    LaunchedEffect(masterEnabled, billRem, shiftRem, savingsRem) {
        actionState?.enabled = true
        actionState?.onSave = { onSave(masterEnabled, billRem, shiftRem, savingsRem) }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp
    ) {
        Column {
            NotificationToggleRow(
                title = "Allow Notifications",
                checked = masterEnabled,
                enabled = true,
                onCheckedChange = { masterEnabled = it }
            )
            ShiftlyHairlineDivider(startInset = 14.dp)
            NotificationToggleRow(
                title = "Recurring Bills Due",
                checked = masterEnabled && billRem,
                enabled = masterEnabled,
                onCheckedChange = { billRem = it }
            )
            ShiftlyHairlineDivider(startInset = 14.dp)
            NotificationToggleRow(
                title = "Log Shift Reminder",
                checked = masterEnabled && shiftRem,
                enabled = masterEnabled,
                onCheckedChange = { shiftRem = it }
            )
            ShiftlyHairlineDivider(startInset = 14.dp)
            NotificationToggleRow(
                title = "Savings & Deposit",
                checked = masterEnabled && savingsRem,
                enabled = masterEnabled,
                onCheckedChange = { savingsRem = it }
            )
        }
    }
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
            .heightIn(min = 56.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
private fun SelectableRulePill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .heightIn(min = 46.dp)
            .clip(RoundedCornerShape(50))
            .background(if (selected) colors.azurePrimary else colors.cardElevated)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics(mergeDescendants = true) {
                this.selected = selected
            },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ).tabular(),
            color = if (selected) PureWhite else colors.textPrimary
        )
    }
}

@Suppress("UNUSED_PARAMETER")
@Composable
fun BudgetsEditorSheetContent(
    currencySymbol: String,
    currentMonthlyBudget: Double,
    currentCategoryBudgets: List<CategoryBudgetSetting>,
    managedCategories: List<ManagedCategorySetting> = emptyList(),
    onManageCategories: (() -> Unit)? = null,
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
                .clickable { isMonthlyExpanded = !isMonthlyExpanded }
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
                            ),
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
                            .padding(bottom = 14.dp)
                    ) {
                        ShiftlyHairlineDivider(modifier = Modifier.padding(bottom = 12.dp))
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
                    .clickable { isCategoryLimitsExpanded = !isCategoryLimitsExpanded }
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
                                ),
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
                        Column(modifier = Modifier.fillMaxWidth()) {
                            ShiftlyHairlineDivider(startInset = 16.dp)
                            editedBudgets.forEachIndexed { idx, (cat, limitStr) ->
                                if (idx > 0) {
                                    ShiftlyHairlineDivider(startInset = 52.dp)
                                }
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 48.dp)
                                        .padding(horizontal = 12.dp, vertical = 6.dp),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ShiftlyIosIconTile(
                                        iconRes = categoryIcon(cat),
                                        backgroundColor = colors.coralBright,
                                        size = 30.dp,
                                        cornerRadius = 8.dp,
                                        iconSize = 15.dp
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
                .clickable { isManageCategoriesExpanded = !isManageCategoriesExpanded }
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
                            ),
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
                    Column(modifier = Modifier.fillMaxWidth()) {
                        ShiftlyHairlineDivider(startInset = 16.dp)

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
                                    .height(40.dp)
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
                                    .size(width = 44.dp, height = 40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        val clean = newCategoryName.trim()
                                        if (clean.isNotEmpty() && managedCategoriesList.none { it.name.equals(clean, true) }) {
                                            managedCategoriesList.add(ManagedCategorySetting(clean, true))
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

                        // Unified Category Rows with Hairline Dividers
                        managedCategoriesList.forEachIndexed { idx, item ->
                            ShiftlyHairlineDivider(startInset = 52.dp)
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 48.dp)
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ShiftlyIosIconTile(
                                    iconRes = categoryIcon(item.name),
                                    backgroundColor = colors.coralBright,
                                    size = 30.dp,
                                    cornerRadius = 8.dp,
                                    iconSize = 15.dp
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
    }
}

@Composable
fun BudgetsEditorSheetContent(
    currencySymbol: String,
    currentMonthlyBudget: Double,
    currentCategoryBudgets: List<CategoryBudgetSetting>,
    managedCategories: List<ManagedCategorySetting> = emptyList(),
    onManageCategories: (() -> Unit)? = null,
    onDismiss: () -> Unit = {},
    onSave: (Double, List<CategoryBudgetSetting>) -> Unit,
    actionState: ShiftlySheetActionState? = null
) {
    BudgetsEditorSheetContent(
        currencySymbol = currencySymbol,
        currentMonthlyBudget = currentMonthlyBudget,
        currentCategoryBudgets = currentCategoryBudgets,
        managedCategories = managedCategories,
        onManageCategories = onManageCategories,
        onDismiss = onDismiss,
        onSave = { monthly, list, _ -> onSave(monthly, list) },
        actionState = actionState
    )
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
            .heightIn(min = 36.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(colors.surface)
            .padding(horizontal = 10.dp, vertical = 7.dp),
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
    actionState: ShiftlySheetActionState? = null
) {
    val colors = ShiftlyColors.current
    val list = remember { mutableStateListOf<ManagedCategorySetting>().apply { addAll(currentCategories) } }
    var newCategoryName by remember { mutableStateOf("") }

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
            ShiftlyIosSwitch(
                checked = item.isVisible,
                onCheckedChange = { vis -> list[idx] = item.copy(isVisible = vis) }
            )
        }
    }
}

@Composable
fun RecurringEntriesSheetContent(
    currencySymbol: String,
    currentEntries: List<RecurringEntrySetting>,
    availableCategories: List<String> = DEFAULT_RECURRING_CATEGORIES,
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
                color = colors.textSecondary
            )
        }
        entries.forEachIndexed { idx, entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.cardElevated)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        entry.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
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
                    ShiftlyIosSwitch(
                        checked = entry.isEnabled,
                        onCheckedChange = { entries[idx] = entry.copy(isEnabled = it) }
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

    ShiftlyHairlineDivider()
    Text(
        "Add Entry",
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = InterFontFamily,
            fontSize = 15.sp
        ),
        color = colors.textPrimary
    )
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("Label", fontFamily = InterFontFamily) },
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = shiftlySheetFieldColors(),
            modifier = Modifier.weight(1.2f)
        )
        OutlinedTextField(
            value = newAmount,
            onValueChange = { newAmount = filterSingleDecimalInput(it, newAmount) },
            label = { Text(currencySymbol, fontFamily = InterFontFamily) },
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
            color = colors.cardElevated,
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
                    ShiftlyIosIconTile(
                        iconRes = categoryIcon(newCategory),
                        backgroundColor = colors.coralBright,
                        size = 32.dp,
                        cornerRadius = 8.dp,
                        iconSize = 16.dp
                    )
                    Column {
                        Text(
                            text = "Expense Category",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                        Text(
                            text = newCategory,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = colors.textPrimary
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_right),
                    contentDescription = "Select Category",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
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

    ShiftlySecondaryTextButton(
        text = "Add to List",
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
        modifier = Modifier.fillMaxWidth()
    )

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
    onSave: (Boolean, String, String?) -> Unit,
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.cardElevated)
            .selectableGroup()
    ) {
        APP_LOCK_METHOD_OPTIONS.forEachIndexed { index, option ->
            if (index > 0) {
                ShiftlyHairlineDivider(startInset = 16.dp)
            }
            val isSelected = method == option
            val displayLabel = option.replace(" Only", "")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 50.dp)
                    .clickable(role = Role.RadioButton) {
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
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(colors.azurePrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_check),
                            contentDescription = "Selected",
                            tint = PureWhite,
                            modifier = Modifier.size(13.dp)
                        )
                    }
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
        OutlinedTextField(
            value = newPin,
            onValueChange = {
                newPin = it.filter(Char::isDigit).take(4)
                errorMessage = null
            },
            label = {
                Text(
                    if (hasSavedPin) "New 4-Digit PIN (leave blank to keep current)" else "Set 4-Digit PIN",
                    fontFamily = InterFontFamily
                )
            },
            placeholder = {
                Text(
                    text = if (hasSavedPin) "••••" else "Enter 4 digits",
                    style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                    color = colors.textSecondary
                )
            },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            shape = RoundedCornerShape(14.dp),
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

    ShiftlySecondaryTextButton(
        text = "Export Full Backup (.zip + Receipts)",
        onClick = onExportZip,
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_folder_archive),
                contentDescription = null,
                tint = colors.azureAccent,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )

    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        ShiftlySecondaryTextButton(
            text = "JSON",
            onClick = onExportJson,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_cloud_upload),
                    contentDescription = null,
                    tint = colors.azureAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        ShiftlySecondaryTextButton(
            text = "CSV",
            onClick = onExportCsv,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_file_spreadsheet),
                    contentDescription = null,
                    tint = colors.azureAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
        ShiftlySecondaryTextButton(
            text = "PDF",
            onClick = onExportPdf,
            leadingIcon = {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_file_text),
                    contentDescription = null,
                    tint = colors.azureAccent,
                    modifier = Modifier.size(16.dp)
                )
            },
            modifier = Modifier.weight(1f)
        )
    }

    ShiftlyHairlineDivider()
    Text(
        "Restore Mode",
        style = MaterialTheme.typography.titleMedium.copy(
            fontFamily = InterFontFamily,
            fontSize = 15.sp
        ),
        color = colors.textPrimary
    )
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

    ShiftlySecondaryTextButton(
        text = "Choose Backup File (.zip or .json)",
        onClick = { onPickBackupFile(restoreMode) },
        leadingIcon = {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_rotate_ccw),
                contentDescription = null,
                tint = colors.azureAccent,
                modifier = Modifier.size(18.dp)
            )
        },
        modifier = Modifier.fillMaxWidth()
    )

    OutlinedTextField(
        value = restoreJsonInput,
        onValueChange = { restoreJsonInput = it },
        label = { Text("Or paste exported JSON backup", fontFamily = InterFontFamily) },
        minLines = 3,
        maxLines = 5,
        shape = RoundedCornerShape(14.dp),
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
