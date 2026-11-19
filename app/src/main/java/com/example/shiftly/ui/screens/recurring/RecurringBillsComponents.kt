package com.example.shiftly.ui.screens.recurring

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.shiftly.R
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.screens.expense.CategoryPromptSheet
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.RecurringEngine
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

private val REPEAT_FREQUENCIES = listOf("Monthly", "Weekly")
private val FALLBACK_RECURRING_CATEGORIES = listOf("Bills", "Food", "Transport", "Supplies", "Other")

@Composable
fun RecurringBillsSummaryCard(
    activeCount: Int,
    totalCount: Int,
    monthlyEstimate: Double,
    dueSoonCount: Int,
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
                        text = "Estimated Monthly Bills",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyEstimate),
                        style = MaterialTheme.typography.displayMedium.tabular(),
                        color = colors.coralInk
                    )
                }

                ShiftlyIosIconTile(
                    iconRes = R.drawable.ic_lucide_repeat,
                    backgroundColor = colors.azurePrimary,
                    size = 40.dp,
                    cornerRadius = 10.dp,
                    iconSize = 20.dp
                )
            }

            ShiftlyHairlineDivider()

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RecurringStatColumn(
                    label = "Active Rules",
                    value = "$activeCount / $totalCount",
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 12.dp)
                )
                ShiftlyVerticalHairlineDivider(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 2.dp)
                )
                RecurringStatColumn(
                    label = "Due < 24h",
                    value = dueSoonCount.toString(),
                    highlight = dueSoonCount > 0,
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp)
                )
            }
        }
    }
}

@Composable
private fun RecurringStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = if (highlight) colors.coralInk else colors.textPrimary
        )
    }
}

@Composable
fun RecurringBillItemCard(
    bill: RecurringBillEntity,
    currencySymbol: String,
    onToggleEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    dateTimeFormatter: DateTimeFormatter? = null,
    now: LocalDateTime? = null,
    containerColor: Color = ShiftlyColors.current.cardElevated,
    shape: Shape = RoundedCornerShape(18.dp)
) {
    val colors = ShiftlyColors.current
    val resolvedFormatter = dateTimeFormatter ?: run {
        val locale = LocalConfiguration.current.locales[0]
        remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", locale) }
    }
    val dueDateTime = remember(bill.dueEpochDay, bill.dueHour, bill.dueMinute) {
        RecurringEngine.dueDateTimeOf(bill)
    }
    val resolvedNow = now ?: remember { LocalDateTime.now() }
    val isDueSoon = remember(bill.isEnabled, dueDateTime, resolvedNow) {
        bill.isEnabled &&
            !dueDateTime.isBefore(resolvedNow) &&
            !dueDateTime.isAfter(resolvedNow.plusHours(24))
    }
    val dueDateLabel = remember(dueDateTime, resolvedFormatter) {
        "Due ${dueDateTime.format(resolvedFormatter)}"
    }

    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .clickable(onClick = onEdit)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShiftlyIosIconTile(
                    iconRes = categoryIcon(bill.category),
                    backgroundColor = if (bill.isEnabled) colors.coralBright else colors.disabledContainer,
                    size = 38.dp,
                    cornerRadius = 9.dp,
                    iconSize = 18.dp,
                    iconTint = if (bill.isEnabled) PureWhite else colors.disabledContent
                )

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = bill.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = if (bill.isEnabled) colors.textPrimary else colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.azureContainer,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = bill.frequency,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = bill.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isDueSoon) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.coralContainer,
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = "Due soon",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.coralInk,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, bill.amount),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = if (bill.isEnabled) colors.coralInk else colors.textSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_calendar),
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = dueDateLabel,
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Switch(
                        checked = bill.isEnabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = colors.onAzurePrimary,
                            checkedTrackColor = colors.azurePrimary,
                            uncheckedThumbColor = colors.textSecondary,
                            uncheckedTrackColor = colors.surface,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_pencil),
                            contentDescription = "Edit bill",
                            tint = colors.azureInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_trash_2),
                            contentDescription = "Delete bill",
                            tint = colors.coralInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RecurringBillsEmptyCard(
    onAddBill: () -> Unit
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShiftlyIosIconTile(
                iconRes = R.drawable.ic_lucide_repeat,
                backgroundColor = colors.azurePrimary,
                size = 48.dp,
                cornerRadius = 12.dp,
                iconSize = 24.dp
            )
            Text(
                text = "No recurring bills yet",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textPrimary
            )
            Text(
                text = "Schedule bills with due dates for automatic logging.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
            ShiftlyPrimaryPillButton(
                text = "Add Recurring Bill",
                onClick = onAddBill
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditRecurringBillBottomSheet(
    currencySymbol: String,
    availableCategories: List<String>,
    editingBill: RecurringBillEntity? = null,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        dueDate: LocalDate,
        dueHour: Int,
        dueMinute: Int,
        isEnabled: Boolean
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d, yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }

    val categoryOptions = remember(availableCategories) {
        availableCategories.ifEmpty { FALLBACK_RECURRING_CATEGORIES }
    }

    var title by remember(editingBill?.id) { mutableStateOf(editingBill?.title.orEmpty()) }
    var amountText by remember(editingBill?.id) {
        mutableStateOf(
            editingBill?.let {
                val r = (it.amount * 100.0).roundToLong()
                if (r % 100L == 0L) (r / 100L).toString() else "%.2f".format(Locale.US, it.amount)
            }.orEmpty()
        )
    }
    var selectedCategory by remember(editingBill?.id, categoryOptions) {
        mutableStateOf(
            categoryOptions.firstOrNull {
                it.equals(editingBill?.category ?: "Bills", ignoreCase = true)
            } ?: categoryOptions.firstOrNull() ?: "Bills"
        )
    }
    var selectedFrequency by remember(editingBill?.id) {
        mutableStateOf(
            if (editingBill?.frequency.equals("Weekly", ignoreCase = true)) "Weekly" else "Monthly"
        )
    }
    var dueDate by remember(editingBill?.id) {
        mutableStateOf(
            editingBill?.let { LocalDate.ofEpochDay(it.dueEpochDay) } ?: LocalDate.now()
        )
    }
    var dueHour by remember(editingBill?.id) { mutableIntStateOf(editingBill?.dueHour ?: 9) }
    var dueMinute by remember(editingBill?.id) { mutableIntStateOf(editingBill?.dueMinute ?: 0) }
    var isEnabled by remember(editingBill?.id) { mutableStateOf(editingBill?.isEnabled ?: true) }

    var showCategoryPrompt by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val amountFocusRequester = remember { FocusRequester() }
    LaunchedEffect(editingBill?.id) {
        if (editingBill == null) {
            amountFocusRequester.requestFocus()
        }
    }

    val parsedAmount = remember(amountText) { amountText.toDoubleOrNull() ?: 0.0 }
    val isValid = title.isNotBlank() && parsedAmount.isFinite() && parsedAmount > 0.0
    val formattedDueDate = remember(dueDate, dateFormatter) { dueDate.format(dateFormatter) }
    val formattedDueTime = remember(dueHour, dueMinute, timeFormatter) {
        LocalTime.of(dueHour, dueMinute).format(timeFormatter)
    }

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
                    text = if (editingBill == null) "New Recurring Bill" else "Edit Recurring Bill",
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.heightIn(min = 44.dp)
                ) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                }
            }

            OutlinedTextField(
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountText = filtered
                    }
                },
                label = { Text("Bill Amount", fontFamily = InterFontFamily) },
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.headlineLarge.tabular(),
                        color = colors.textSecondary
                    )
                },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.coralInk
                    )
                },
                textStyle = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.coralInk,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = colors.coralInk,
                    unfocusedLabelColor = colors.textSecondary,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.cardElevated,
                    unfocusedContainerColor = colors.cardElevated
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(amountFocusRequester)
            )

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Bill Title", fontFamily = InterFontFamily) },
                placeholder = {
                    Text(
                        text = "e.g., Dorm Rent, Fiber Wi-Fi",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.azureAccent,
                    unfocusedBorderColor = Color.Transparent,
                    focusedLabelColor = colors.azureAccent,
                    unfocusedLabelColor = colors.textSecondary,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedContainerColor = colors.cardElevated,
                    unfocusedContainerColor = colors.cardElevated
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 52.dp)
                    .clip(RoundedCornerShape(12.dp))
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
                            iconRes = categoryIcon(selectedCategory),
                            backgroundColor = colors.coralBright,
                            size = 32.dp,
                            cornerRadius = 8.dp,
                            iconSize = 16.dp
                        )
                        Column {
                            Text(
                                text = "Category",
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                                color = colors.textSecondary
                            )
                            Text(
                                text = selectedCategory,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = InterFontFamily,
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

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Repeat Frequency",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    REPEAT_FREQUENCIES.forEach { freq ->
                        val isSelected = selectedFrequency == freq
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(50))
                                .background(if (isSelected) colors.azurePrimary else colors.cardElevated)
                                .clickable(role = Role.Tab) { selectedFrequency = freq }
                                .semantics { selected = isSelected },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = freq,
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (isSelected) colors.onAzurePrimary else colors.textPrimary
                            )
                        }
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Next Due Date & Time",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                    color = colors.textSecondary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.cardElevated,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1.3f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showDatePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_calendar),
                                contentDescription = null,
                                tint = colors.azureInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = formattedDueDate,
                                style = MaterialTheme.typography.titleSmall.tabular(),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.cardElevated,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showTimePicker = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_clock),
                                contentDescription = null,
                                tint = colors.azureInk,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = formattedDueTime,
                                style = MaterialTheme.typography.titleSmall.tabular(),
                                color = colors.textPrimary,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            if (editingBill != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Auto-log bill when due",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                        Switch(
                            checked = isEnabled,
                            onCheckedChange = { isEnabled = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = colors.onAzurePrimary,
                                checkedTrackColor = colors.azurePrimary,
                                uncheckedThumbColor = colors.textSecondary,
                                uncheckedTrackColor = colors.surface,
                                uncheckedBorderColor = Color.Transparent
                            )
                        )
                    }
                }
            }

            ShiftlyPrimaryPillButton(
                text = if (editingBill == null) "Save Recurring Bill" else "Update Recurring Bill",
                enabled = isValid,
                onClick = {
                    onSave(
                        title.trim(),
                        parsedAmount,
                        selectedCategory,
                        selectedFrequency,
                        dueDate,
                        dueHour,
                        dueMinute,
                        isEnabled
                    )
                }
            )
        }
    }

    if (showCategoryPrompt) {
        CategoryPromptSheet(
            categoryOptions = categoryOptions,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            onDismiss = { showCategoryPrompt = false }
        )
    }

    if (showDatePicker) {
        val initialMillis = dueDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            dueDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Select",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.azureAccent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
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
                    disabledDayContentColor = colors.textSecondary.copy(alpha = 0.45f),
                    selectedDayContentColor = colors.onAzurePrimary,
                    selectedDayContainerColor = colors.azurePrimary,
                    todayContentColor = colors.azureAccent,
                    todayDateBorderColor = colors.azureAccent
                )
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = dueHour,
            initialMinute = dueMinute,
            is24Hour = false
        )
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        dueHour = timePickerState.hour
                        dueMinute = timePickerState.minute
                        showTimePicker = false
                    }
                ) {
                    Text(
                        text = "OK",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.azureAccent
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
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
                        clockDialColor = colors.cardElevated,
                        clockDialSelectedContentColor = colors.onAzurePrimary,
                        clockDialUnselectedContentColor = colors.textSecondary,
                        selectorColor = colors.azurePrimary,
                        containerColor = colors.surface,
                        periodSelectorBorderColor = Color.Transparent,
                        periodSelectorSelectedContainerColor = colors.azurePrimary,
                        periodSelectorUnselectedContainerColor = colors.cardElevated,
                        periodSelectorSelectedContentColor = colors.onAzurePrimary,
                        periodSelectorUnselectedContentColor = colors.textSecondary,
                        timeSelectorSelectedContainerColor = colors.cardElevated,
                        timeSelectorUnselectedContainerColor = colors.cardElevated,
                        timeSelectorSelectedContentColor = colors.textPrimary,
                        timeSelectorUnselectedContentColor = colors.textSecondary
                    )
                )
            }
        }
    }
}
