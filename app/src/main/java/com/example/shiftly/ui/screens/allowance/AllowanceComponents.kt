package com.example.shiftly.ui.screens.allowance

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.ui.components.ShiftlyDialogShell
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.CutoffCalculator
import com.example.shiftly.utils.CutoffWindow
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle

private val CUTOFF_MODE_OPTIONS = listOf(
    CutoffScheduleConfig.MODE_SEMI_MONTHLY to "Semi-Monthly",
    CutoffScheduleConfig.MODE_MONTHLY to "Monthly",
    CutoffScheduleConfig.MODE_WEEKLY to "Weekly"
)

private val SEMI_MONTHLY_PRESETS = listOf(
    Triple("15th & End", 15, CutoffScheduleConfig.END_OF_MONTH_DAY),
    Triple("10th & 25th", 10, 25),
    Triple("5th & 20th", 5, 20)
)

private val MONTHLY_PRESETS = listOf(
    "End" to CutoffScheduleConfig.END_OF_MONTH_DAY,
    "15th" to 15,
    "19th" to 19,
    "25th" to 25
)

@Composable
fun AllowanceHeroCard(
    totalBalance: Double,
    salaryTotal: Double,
    businessTotal: Double,
    othersTotal: Double,
    currencySymbol: String
) {
    val colors = ShiftlyColors.current
    ShiftlyGlassCard(
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "Total Allowance Logged",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, totalBalance),
                        style = MaterialTheme.typography.displayMedium.tabular(),
                        color = colors.mintInk
                    )
                }
                ShiftlyIosIconTile(
                    iconRes = R.drawable.ic_lucide_wallet,
                    backgroundColor = colors.mintBright,
                    size = 28.dp,
                    cornerRadius = 10.dp,
                    iconSize = 24.dp
                )
            }

            ShiftlyHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AllowanceSourceStatColumn(
                    label = "Salary",
                    amount = ShiftlyFormatters.formatCurrency(currencySymbol, salaryTotal),
                    accentColor = colors.mintInk,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                )
                ShiftlyVerticalHairlineDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                )
                AllowanceSourceStatColumn(
                    label = "Business",
                    amount = ShiftlyFormatters.formatCurrency(currencySymbol, businessTotal),
                    accentColor = colors.azureInk,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 10.dp)
                )
                ShiftlyVerticalHairlineDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                )
                AllowanceSourceStatColumn(
                    label = "Others",
                    amount = ShiftlyFormatters.formatCurrency(currencySymbol, othersTotal),
                    accentColor = colors.textPrimary,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun AllowanceSourceStatColumn(
    label: String,
    amount: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
            color = colors.textSecondary,
            maxLines = 1
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = accentColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun AllowanceCutoffScheduleCard(
    cutoffConfig: CutoffScheduleConfig,
    activeWindow: CutoffWindow,
    pendingDutiesEarnings: Double,
    pendingShiftCount: Int,
    currencySymbol: String,
    onChangeCutoff: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val cutoffDisplayLabel = remember(cutoffConfig, locale) { cutoffConfig.displayLabel(locale) }
    val formattedEndDate = remember(activeWindow.endDate, dateFormatter) {
        activeWindow.endDate.format(dateFormatter)
    }

    ShiftlyGlassCard(
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Payday Cutoff Schedule",
                        style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Text(
                        text = cutoffDisplayLabel,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                }

                ShiftlySecondaryTextButton(
                    text = "Change Cutoff",
                    onClick = onChangeCutoff
                )
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "Pending Duties Earnings",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (pendingShiftCount > 0) {
                                "$pendingShiftCount shift(s) • credits $formattedEndDate"
                            } else {
                                "Current cycle: ${activeWindow.label}"
                            },
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, pendingDutiesEarnings),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.azureInk
                    )
                }
            }
        }
    }
}

@Composable
fun AllowanceHistoryItemCard(
    entry: AllowanceEntryEntity,
    currencySymbol: String,
    onDelete: () -> Unit,
    dateFormatter: DateTimeFormatter? = null,
    containerColor: Color = ShiftlyColors.current.card,
    shape: Shape = RoundedCornerShape(16.dp)
) {
    val colors = ShiftlyColors.current
    val resolvedFormatter = dateFormatter ?: run {
        val locale = LocalConfiguration.current.locales[0]
        remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    }
    val formattedDate = remember(entry.dateEpochDay, resolvedFormatter) {
        LocalDate.ofEpochDay(entry.dateEpochDay).format(resolvedFormatter)
    }
    val isAutoFromDuties = !entry.cutoffKey.isNullOrBlank()
    val iconRes = allowanceSourceIcon(entry.source)
    val tileBg = when (entry.source) {
        AllowanceSource.SALARY -> colors.mintBright
        AllowanceSource.BUSINESS -> colors.azurePrimary
        else -> colors.textSecondary
    }
    val chipBg = when (entry.source) {
        AllowanceSource.SALARY -> colors.mintContainer
        AllowanceSource.BUSINESS -> colors.azureContainer
        else -> colors.surface
    }
    val chipTint = when (entry.source) {
        AllowanceSource.SALARY -> colors.mintInk
        AllowanceSource.BUSINESS -> colors.azureInk
        else -> colors.textSecondary
    }

    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShiftlyIosIconTile(
                iconRes = iconRes,
                backgroundColor = tileBg,
                size = 28.dp,
                cornerRadius = 9.dp,
                iconSize = 22.dp
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = entry.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = chipBg,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = if (isAutoFromDuties) "Auto from Duties" else shortSourceLabel(entry.source),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = chipTint,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "+${ShiftlyFormatters.formatCurrency(currencySymbol, entry.amount)}",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintInk
                )
                if (!isAutoFromDuties) {
                    var deleteTapCount by remember { mutableIntStateOf(0) }
                    IconButton(
                        onClick = {
                            deleteTapCount++
                            onDelete()
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_trash_2,
                            contentDescription = "Delete allowance",
                            tint = colors.coralInk,
                            triggerKey = deleteTapCount,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                } else {
                    Text(
                        text = "Synced",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun AllowanceEmptyCard(
    onAddAllowance: () -> Unit
) {
    val colors = ShiftlyColors.current
    ShiftlyEmptyState(
        iconRes = R.drawable.ic_lucide_wallet,
        title = "No allowance entries yet",
        subtitle = null,
        actionText = "Add Allowance",
        onAction = onAddAllowance,
        iconBackgroundColor = colors.mintBright
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddAllowanceBottomSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, source: String, date: LocalDate) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", locale) }

    var amountText by remember { mutableStateOf("") }
    var title by remember { mutableStateOf("") }
    var selectedSource by remember { mutableStateOf(AllowanceSource.SALARY) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }

    val amountFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        amountFocusRequester.requestFocus()
    }

    val parsedAmount = remember(amountText) { amountText.toDoubleOrNull() ?: 0.0 }
    val isValid = title.isNotBlank() && parsedAmount.isFinite() && parsedAmount > 0.0
    val formattedSelectedDate = remember(selectedDate, dateFormatter) {
        selectedDate.format(dateFormatter)
    }

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
            ShiftlySheetHeader(title = "Add Allowance")

            var isSourceExpanded by remember { mutableStateOf(false) }

            // Card 1: Amount & Description
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    // Row 1: Amount
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Amount",
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
                            Text(
                                text = "$currencySymbol ",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.mintInk
                                )
                            )
                            BasicTextField(
                                value = amountText,
                                onValueChange = { input ->
                                    val filtered = input.filter { it.isDigit() || it == '.' }
                                    if (filtered.count { it == '.' } <= 1) amountText = filtered
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                cursorBrush = SolidColor(colors.mintInk),
                                textStyle = MaterialTheme.typography.titleLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    textAlign = TextAlign.End
                                ).tabular(),
                                decorationBox = { inner ->
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        if (amountText.isEmpty()) {
                                            Text(
                                                text = "0.00",
                                                style = MaterialTheme.typography.titleLarge.copy(
                                                    fontFamily = InterFontFamily,
                                                    color = colors.textSecondary
                                                ),
                                                textAlign = TextAlign.End
                                            )
                                        }
                                        inner()
                                    }
                                },
                                modifier = Modifier
                                    .width(120.dp)
                                    .focusRequester(amountFocusRequester)
                            )
                        }
                    }

                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                    // Row 2: Description
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Description",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            ),
                            color = colors.textPrimary
                        )
                        BasicTextField(
                            value = title,
                            onValueChange = { title = it },
                            singleLine = true,
                            cursorBrush = SolidColor(colors.azureAccent),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium,
                                color = colors.textPrimary,
                                textAlign = TextAlign.End
                            ),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (title.isEmpty()) {
                                        Text(
                                            text = "e.g., Weekly allowance",
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
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 16.dp)
                        )
                    }
                }
            }

            // Card 2: Source Dropdown & Date
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .shiftlyPressClickable { isSourceExpanded = !isSourceExpanded }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Source",
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
                            ShiftlyIosIconTile(
                                iconRes = allowanceSourceIcon(selectedSource),
                                backgroundColor = colors.mintBright,
                                size = 24.dp,
                                cornerRadius = 6.dp,
                                iconSize = 20.dp
                            )
                            Text(
                                text = selectedSource,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ),
                                color = colors.textPrimary
                            )
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_chevron_right,
                                contentDescription = "Toggle source dropdown",
                                tint = colors.textSecondary,
                                triggerKey = isSourceExpanded,
                                modifier = Modifier
                                    .size(18.dp)
                                    .graphicsLayer { rotationZ = if (isSourceExpanded) 90f else 0f }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = isSourceExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            AllowanceSource.ALL.forEach { source ->
                                val isSelected = selectedSource == source
                                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(min = 50.dp)
                                        .background(if (isSelected) colors.mintContainer else Color.Transparent)
                                        .shiftlyPressClickable {
                                            selectedSource = source
                                            isSourceExpanded = false
                                        }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = allowanceSourceIcon(source),
                                            contentDescription = null,
                                            tint = if (isSelected) colors.mintInk else colors.textSecondary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = source,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = colors.textPrimary
                                        )
                                    }
                                    if (isSelected) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = R.drawable.ic_lucide_check,
                                            contentDescription = "Selected",
                                            tint = colors.mintInk,
                                            animateOnMount = true,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                    // Date Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 50.dp)
                            .shiftlyPressClickable { showDatePicker = true }
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Date",
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
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_calendar,
                                contentDescription = null,
                                tint = colors.azureInk,
                                modifier = Modifier.size(17.dp)
                            )
                            Text(
                                text = formattedSelectedDate,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    if (isValid) {
                        onSave(title.trim(), parsedAmount, selectedSource, selectedDate)
                    }
                },
                actionLabel = "Add",
                actionEnabled = isValid
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        ShiftlyDialogShell(
            title = "Select Date",
            onDismiss = { showDatePicker = false },
            actionLabel = "Done",
            onAction = {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                }
                showDatePicker = false
            },
            horizontalOuterPadding = 12.dp,
            contentHorizontalPadding = 8.dp
        ) {
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
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
                    disabledDayContentColor = colors.textSecondary.copy(alpha = 0.45f),
                    selectedDayContentColor = colors.onAzurePrimary,
                    selectedDayContainerColor = colors.azurePrimary,
                    todayContentColor = colors.azureAccent,
                    todayDateBorderColor = colors.azureAccent
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangeCutoffScheduleSheet(
    currentConfig: CutoffScheduleConfig,
    onDismiss: () -> Unit,
    onSave: (CutoffScheduleConfig) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]
    val today = remember { LocalDate.now() }

    val initial = remember(currentConfig) { currentConfig.normalized() }
    var mode by remember(currentConfig) { mutableStateOf(initial.mode) }
    var day1 by remember(currentConfig) { mutableIntStateOf(initial.day1) }
    var day2 by remember(currentConfig) { mutableIntStateOf(initial.day2) }
    var weeklyDay by remember(currentConfig) { mutableIntStateOf(initial.weeklyDayOfWeek) }

    val previewConfig = remember(mode, day1, day2, weeklyDay) {
        CutoffScheduleConfig(
            mode = mode,
            day1 = day1,
            day2 = day2,
            weeklyDayOfWeek = weeklyDay
        ).normalized()
    }
    val previewWindow = remember(today, previewConfig, locale) {
        CutoffCalculator.resolveCutoffWindow(today, previewConfig, locale)
    }
    val previewDisplayLabel = remember(previewConfig, locale) {
        previewConfig.displayLabel(locale)
    }

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
            ShiftlySheetHeader(title = "Payday Cutoff Schedule")

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CUTOFF_MODE_OPTIONS.forEach { (optionMode, label) ->
                    val isSelected = mode == optionMode
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (isSelected) colors.azurePrimary else colors.cardElevated)
                            .clickable(role = Role.Tab) {
                                mode = optionMode
                                if (optionMode == CutoffScheduleConfig.MODE_SEMI_MONTHLY && day1 >= day2) {
                                    day1 = day1.coerceIn(1, 27)
                                    day2 = (day1 + 15).coerceIn(day1 + 1, CutoffScheduleConfig.END_OF_MONTH_DAY)
                                }
                            }
                            .semantics { selected = isSelected },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isSelected) colors.onAzurePrimary else colors.textPrimary,
                            maxLines = 1
                        )
                    }
                }
            }

            when (mode) {
                CutoffScheduleConfig.MODE_SEMI_MONTHLY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            SEMI_MONTHLY_PRESETS.forEach { (label, p1, p2) ->
                                val isPreset = day1 == p1 && day2 == p2
                                PresetChip(
                                    label = label,
                                    selected = isPreset,
                                    onClick = {
                                        day1 = p1
                                        day2 = p2
                                    },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        DayStepperRow(
                            label = "1st Cutoff Day",
                            valueLabel = CutoffScheduleConfig.formatDayLabel(day1),
                            canDecrease = day1 > 1,
                            canIncrease = day1 < 27 && day1 + 1 < day2,
                            onDecrease = { if (day1 > 1) day1 -= 1 },
                            onIncrease = { if (day1 < 27 && day1 + 1 < day2) day1 += 1 }
                        )

                        DayStepperRow(
                            label = "2nd Cutoff Day",
                            valueLabel = CutoffScheduleConfig.formatDayLabel(day2),
                            canDecrease = day2 > day1 + 1,
                            canIncrease = day2 < CutoffScheduleConfig.END_OF_MONTH_DAY,
                            onDecrease = { if (day2 > day1 + 1) day2 -= 1 },
                            onIncrease = { if (day2 < CutoffScheduleConfig.END_OF_MONTH_DAY) day2 += 1 }
                        )
                    }
                }

                CutoffScheduleConfig.MODE_MONTHLY -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = "Quick Presets",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            MONTHLY_PRESETS.forEach { (label, d) ->
                                PresetChip(
                                    label = label,
                                    selected = day1 == d,
                                    onClick = { day1 = d },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }

                        DayStepperRow(
                            label = "Monthly Payday (1–31)",
                            valueLabel = CutoffScheduleConfig.formatDayLabel(day1),
                            canDecrease = day1 > 1,
                            canIncrease = day1 < CutoffScheduleConfig.END_OF_MONTH_DAY,
                            onDecrease = { if (day1 > 1) day1 -= 1 },
                            onIncrease = { if (day1 < CutoffScheduleConfig.END_OF_MONTH_DAY) day1 += 1 }
                        )
                    }
                }

                else -> {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Weekly Payday",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            DayOfWeek.entries.forEach { dow ->
                                val isSelected = weeklyDay == dow.value
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .heightIn(min = 48.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) colors.azurePrimary else colors.cardElevated)
                                        .clickable(role = Role.Tab) { weeklyDay = dow.value }
                                        .semantics { selected = isSelected },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = dow.getDisplayName(TextStyle.NARROW, locale),
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ),
                                        color = if (isSelected) colors.onAzurePrimary else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "Active Rule: $previewDisplayLabel",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Current cycle: ${previewWindow.label}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary
                    )
                }
            }

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = { onSave(previewConfig) },
                cancelLabel = "Cancel",
                actionLabel = "Save"
            )
        }
    }
}

@Composable
private fun PresetChip(
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
            .background(if (selected) colors.mintContainer else colors.cardElevated)
            .clickable(role = Role.Tab, onClick = onClick)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ).tabular(),
            color = if (selected) colors.mintInk else colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun DayStepperRow(
    label: String,
    valueLabel: String,
    canDecrease: Boolean,
    canIncrease: Boolean,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                    color = colors.textSecondary
                )
                Text(
                    text = valueLabel,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.textPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canDecrease, onClick = onDecrease),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (canDecrease) colors.surface else colors.surface.copy(alpha = 0.45f),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "−",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canDecrease) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .clickable(enabled = canIncrease, onClick = onIncrease),
                    contentAlignment = Alignment.Center
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (canIncrease) colors.surface else colors.surface.copy(alpha = 0.45f),
                        shadowElevation = 0.dp,
                        modifier = Modifier.size(38.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "+",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (canIncrease) colors.textPrimary else colors.textSecondary
                            )
                        }
                    }
                }
            }
        }
    }
}

@DrawableRes
private fun allowanceSourceIcon(source: String): Int {
    return when (source) {
        AllowanceSource.SALARY -> R.drawable.ic_lucide_clock
        AllowanceSource.BUSINESS -> R.drawable.ic_lucide_briefcase
        else -> R.drawable.ic_lucide_wallet
    }
}

private fun shortSourceLabel(source: String): String {
    return when (source) {
        AllowanceSource.SALARY -> "Salary"
        AllowanceSource.BUSINESS -> "Business"
        else -> "Others"
    }
}
