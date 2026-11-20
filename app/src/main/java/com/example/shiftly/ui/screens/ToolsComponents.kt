package com.example.shiftly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.IouEntrySetting
import com.example.shiftly.data.local.PaycheckDeductionsSetting
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyIosSwitch
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.UUID

// =============================================================================
// 1. Paycheck Estimator Sheet Content
// =============================================================================

@Composable
fun PaycheckEstimatorSheetContent(
    currencySymbol: String,
    grossPay: Double,
    deductions: PaycheckDeductionsSetting,
    onSaveDeductions: (PaycheckDeductionsSetting) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    var currentSetting by remember(deductions) { mutableStateOf(deductions) }

    var sssAmountText by remember(deductions) {
        mutableStateOf(if (deductions.sssAmount > 0.0) deductions.sssAmount.toString() else "")
    }
    var philhealthAmountText by remember(deductions) {
        mutableStateOf(if (deductions.philhealthAmount > 0.0) deductions.philhealthAmount.toString() else "")
    }
    var pagibigAmountText by remember(deductions) {
        mutableStateOf(if (deductions.pagibigAmount > 0.0) deductions.pagibigAmount.toString() else "")
    }
    var taxPercentText by remember(deductions) {
        mutableStateOf(if (deductions.taxPercent > 0.0) deductions.taxPercent.toString() else "")
    }
    var customAmountText by remember(deductions) {
        mutableStateOf(if (deductions.customAmount > 0.0) deductions.customAmount.toString() else "")
    }

    val updatedSetting = remember(
        currentSetting.sssEnabled, sssAmountText,
        currentSetting.philhealthEnabled, philhealthAmountText,
        currentSetting.pagibigEnabled, pagibigAmountText,
        currentSetting.taxEnabled, taxPercentText,
        currentSetting.customEnabled, customAmountText
    ) {
        PaycheckDeductionsSetting(
            sssEnabled = currentSetting.sssEnabled,
            sssAmount = sssAmountText.toDoubleOrNull() ?: 0.0,
            philhealthEnabled = currentSetting.philhealthEnabled,
            philhealthAmount = philhealthAmountText.toDoubleOrNull() ?: 0.0,
            pagibigEnabled = currentSetting.pagibigEnabled,
            pagibigAmount = pagibigAmountText.toDoubleOrNull() ?: 0.0,
            taxEnabled = currentSetting.taxEnabled,
            taxPercent = taxPercentText.toDoubleOrNull() ?: 0.0,
            customEnabled = currentSetting.customEnabled,
            customAmount = customAmountText.toDoubleOrNull() ?: 0.0
        )
    }

    val totalDeductions = remember(updatedSetting, grossPay) {
        updatedSetting.calculateTotalDeductions(grossPay)
    }
    val netTakeHome = remember(grossPay, totalDeductions) {
        (grossPay - totalDeductions).coerceAtLeast(0.0)
    }

    fun updateAndNotify(newSetting: PaycheckDeductionsSetting) {
        currentSetting = newSetting
        onSaveDeductions(newSetting)
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Summary Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.mintContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "ESTIMATED NET TAKE-HOME PAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        letterSpacing = 0.5.sp
                    ),
                    color = colors.mintInk
                )
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, netTakeHome),
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ).tabular(),
                    color = colors.mintTextOnSurface
                )
                ShiftlyHairlineDivider(startInset = 0.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Gross Earnings: ${ShiftlyFormatters.formatCurrency(currencySymbol, grossPay)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Deductions: -${ShiftlyFormatters.formatCurrency(currencySymbol, totalDeductions)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.coralInk
                    )
                }
            }
        }

        // Deductions List Card
        Text(
            text = "DEDUCTIONS BREAKDOWN",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                letterSpacing = 0.5.sp
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
            Column(modifier = Modifier.fillMaxWidth()) {
                // SSS
                DeductionConfigRow(
                    title = "SSS Contribution",
                    enabled = updatedSetting.sssEnabled,
                    amountText = sssAmountText,
                    suffix = currencySymbol,
                    onToggle = { active ->
                        updateAndNotify(updatedSetting.copy(sssEnabled = active))
                    },
                    onAmountChange = { input ->
                        sssAmountText = input
                        updateAndNotify(updatedSetting.copy(sssAmount = input.toDoubleOrNull() ?: 0.0))
                    }
                )
                ShiftlyHairlineDivider(startInset = 16.dp)

                // PhilHealth
                DeductionConfigRow(
                    title = "PhilHealth",
                    enabled = updatedSetting.philhealthEnabled,
                    amountText = philhealthAmountText,
                    suffix = currencySymbol,
                    onToggle = { active ->
                        updateAndNotify(updatedSetting.copy(philhealthEnabled = active))
                    },
                    onAmountChange = { input ->
                        philhealthAmountText = input
                        updateAndNotify(updatedSetting.copy(philhealthAmount = input.toDoubleOrNull() ?: 0.0))
                    }
                )
                ShiftlyHairlineDivider(startInset = 16.dp)

                // Pag-IBIG
                DeductionConfigRow(
                    title = "Pag-IBIG Fund",
                    enabled = updatedSetting.pagibigEnabled,
                    amountText = pagibigAmountText,
                    suffix = currencySymbol,
                    onToggle = { active ->
                        updateAndNotify(updatedSetting.copy(pagibigEnabled = active))
                    },
                    onAmountChange = { input ->
                        pagibigAmountText = input
                        updateAndNotify(updatedSetting.copy(pagibigAmount = input.toDoubleOrNull() ?: 0.0))
                    }
                )
                ShiftlyHairlineDivider(startInset = 16.dp)

                // Withholding Tax
                DeductionConfigRow(
                    title = "Withholding Tax",
                    enabled = updatedSetting.taxEnabled,
                    amountText = taxPercentText,
                    suffix = "%",
                    placeholder = "0.0",
                    onToggle = { active ->
                        updateAndNotify(updatedSetting.copy(taxEnabled = active))
                    },
                    onAmountChange = { input ->
                        taxPercentText = input
                        updateAndNotify(updatedSetting.copy(taxPercent = input.toDoubleOrNull() ?: 0.0))
                    }
                )
                ShiftlyHairlineDivider(startInset = 16.dp)

                // Custom Deduction
                DeductionConfigRow(
                    title = "Other / Custom",
                    enabled = updatedSetting.customEnabled,
                    amountText = customAmountText,
                    suffix = currencySymbol,
                    onToggle = { active ->
                        updateAndNotify(updatedSetting.copy(customEnabled = active))
                    },
                    onAmountChange = { input ->
                        customAmountText = input
                        updateAndNotify(updatedSetting.copy(customAmount = input.toDoubleOrNull() ?: 0.0))
                    }
                )
            }
        }

        ShiftlySheetCloseButton(
            label = "Done",
            onClick = onDismiss
        )
    }
}

@Composable
private fun DeductionConfigRow(
    title: String,
    enabled: Boolean,
    amountText: String,
    suffix: String,
    placeholder: String = "0.00",
    onToggle: (Boolean) -> Unit,
    onAmountChange: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f)
        ) {
            ShiftlyIosSwitch(
                checked = enabled,
                onCheckedChange = onToggle
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (enabled) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                BasicTextField(
                    value = amountText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) onAmountChange(filtered)
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
                            if (amountText.isEmpty()) {
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
                    modifier = Modifier.width(64.dp)
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
}

// =============================================================================
// 2. IOU / Debt & Loan Tracker Sheet Content
// =============================================================================

@Composable
fun IouTrackerSheetContent(
    currencySymbol: String,
    entries: List<IouEntrySetting>,
    onSaveEntries: (List<IouEntrySetting>) -> Unit,
    onDismiss: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val itemList = remember(entries) {
        mutableStateListOf<IouEntrySetting>().apply { addAll(entries) }
    }

    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) } // true = Lent, false = Borrowed
    var noteText by remember { mutableStateOf("") }
    var pendingDeleteEntry by remember { mutableStateOf<IouEntrySetting?>(null) }

    val totalLent = remember(itemList.toList()) {
        itemList.filter { !it.isSettled && it.isLent }.sumOf { it.amount }
    }
    val totalBorrowed = remember(itemList.toList()) {
        itemList.filter { !it.isSettled && !it.isLent }.sumOf { it.amount }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Balance Summary Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Owed to You (Lent)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, totalLent),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.mintInk
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = "You Owe (Borrowed)",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, totalBorrowed),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.coralInk
                    )
                }
            }
        }

        // Add Entry Section
        Text(
            text = "ADD NEW ENTRY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                letterSpacing = 0.5.sp
            ),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        // 2-option Segmented Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(9.dp))
                .background(colors.surface)
                .padding(4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (isLent) colors.mintContainer else Color.Transparent)
                    .clickable { isLent = true },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Lent",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = if (isLent) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (isLent) colors.mintInk else colors.textSecondary
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(35.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(if (!isLent) colors.coralContainer else Color.Transparent)
                    .clickable { isLent = false },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Borrowed",
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = if (!isLent) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 13.sp
                    ),
                    color = if (!isLent) colors.coralInk else colors.textSecondary
                )
            }
        }

        // Single clean iOS Inset-Grouped Container
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                IouInsetRow(
                    label = "Person Name",
                    value = personName,
                    onValueChange = { personName = it },
                    placeholder = "Name"
                )
                ShiftlyHairlineDivider(startInset = 16.dp)

                IouInsetRow(
                    label = "Amount ($currencySymbol)",
                    value = amountText,
                    onValueChange = { input ->
                        val filtered = input.filter { it.isDigit() || it == '.' }
                        if (filtered.count { it == '.' } <= 1) amountText = filtered
                    },
                    placeholder = "0.00",
                    prefix = currencySymbol,
                    isNumeric = true
                )
                ShiftlyHairlineDivider(startInset = 16.dp)

                IouInsetRow(
                    label = "Note (optional)",
                    value = noteText,
                    onValueChange = { noteText = it },
                    placeholder = "Note"
                )
            }
        }

        ShiftlySecondaryTextButton(
            text = "Add Entry",
            onClick = {
                val amt = amountText.toDoubleOrNull() ?: 0.0
                if (personName.isNotBlank() && amt > 0.0) {
                    itemList.add(
                        0,
                        IouEntrySetting(
                            id = UUID.randomUUID().toString(),
                            personName = personName.trim(),
                            amount = amt,
                            isLent = isLent,
                            note = noteText.trim(),
                            isSettled = false
                        )
                    )
                    personName = ""
                    amountText = ""
                    noteText = ""
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Active / Settled Entries List
        if (itemList.isNotEmpty()) {
            Text(
                text = "ENTRIES (${itemList.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    letterSpacing = 0.5.sp
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
                Column(modifier = Modifier.fillMaxWidth()) {
                    itemList.forEachIndexed { idx, entry ->
                        if (idx > 0) {
                            ShiftlyHairlineDivider(startInset = 16.dp)
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 54.dp)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = entry.personName,
                                        style = MaterialTheme.typography.titleMedium.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = if (entry.isSettled) colors.textSecondary else colors.textPrimary
                                    )
                                    if (entry.isSettled) {
                                        Text(
                                            text = "• Settled",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.mintInk
                                        )
                                    }
                                }

                                val subText = if (entry.isLent) "Lent • ${entry.note.ifBlank { "No note" }}" else "Borrowed • ${entry.note.ifBlank { "No note" }}"
                                Text(
                                    text = subText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "${if (entry.isLent) "+" else "-"}${ShiftlyFormatters.formatCurrency(currencySymbol, entry.amount)}",
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = when {
                                        entry.isSettled -> colors.textSecondary
                                        entry.isLent -> colors.mintInk
                                        else -> colors.coralInk
                                    }
                                )

                                ShiftlyIosSwitch(
                                    checked = entry.isSettled,
                                    onCheckedChange = { settled ->
                                        itemList[idx] = entry.copy(isSettled = settled)
                                    }
                                )

                                IconButton(
                                    onClick = { pendingDeleteEntry = entry },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_trash_2),
                                        contentDescription = "Delete entry",
                                        tint = colors.coralInk,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        ShiftlySheetBottomButtons(
            onCancel = onDismiss,
            onAction = {
                onSaveEntries(itemList.toList())
                onDismiss()
            },
            cancelLabel = "Cancel",
            actionLabel = "Save"
        )
    }

    pendingDeleteEntry?.let { entryToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Record?",
            message = "This ${if (entryToDelete.isLent) "lent" else "borrowed"} record for ${entryToDelete.personName} will be permanently removed.",
            onDismiss = { pendingDeleteEntry = null },
            onConfirm = {
                itemList.removeAll { it.id == entryToDelete.id }
                pendingDeleteEntry = null
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddIouBottomSheet(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSave: (personName: String, amount: Double, isLent: Boolean, note: String) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) }
    var noteText by remember { mutableStateOf("") }

    val parsedAmount = remember(amountText) { amountText.toDoubleOrNull() ?: 0.0 }
    val isValid = personName.trim().isNotEmpty() && parsedAmount.isFinite() && parsedAmount > 0.0

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
            ShiftlySheetHeader(title = "Add IOU")

            // 2-option Segmented Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(9.dp))
                    .background(colors.cardElevated)
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(35.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (isLent) colors.mintContainer else Color.Transparent)
                        .clickable { isLent = true },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Lent",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = if (isLent) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (isLent) colors.mintInk else colors.textSecondary
                    )
                }

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(35.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(if (!isLent) colors.coralContainer else Color.Transparent)
                        .clickable { isLent = false },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Borrowed",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = if (!isLent) FontWeight.SemiBold else FontWeight.Medium,
                            fontSize = 13.sp
                        ),
                        color = if (!isLent) colors.coralInk else colors.textSecondary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                border = BorderStroke(0.5.dp, colors.separator),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    IouInsetRow(
                        label = "Person Name",
                        value = personName,
                        onValueChange = { personName = it },
                        placeholder = "Name"
                    )
                    ShiftlyHairlineDivider(startInset = 16.dp)

                    IouInsetRow(
                        label = "Amount ($currencySymbol)",
                        value = amountText,
                        onValueChange = { input ->
                            val filtered = input.filter { it.isDigit() || it == '.' }
                            if (filtered.count { it == '.' } <= 1) amountText = filtered
                        },
                        placeholder = "0.00",
                        prefix = currencySymbol,
                        isNumeric = true
                    )
                    ShiftlyHairlineDivider(startInset = 16.dp)

                    IouInsetRow(
                        label = "Note (optional)",
                        value = noteText,
                        onValueChange = { noteText = it },
                        placeholder = "Note"
                    )
                }
            }

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    if (isValid) {
                        onSave(personName.trim(), parsedAmount, isLent, noteText.trim())
                    }
                },
                cancelLabel = "Cancel",
                actionLabel = "Save",
                actionEnabled = isValid
            )
        }
    }
}

@Composable
private fun IouInsetRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String? = null,
    isNumeric: Boolean = false
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
            color = colors.textPrimary,
            maxLines = 1
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (prefix != null && value.isNotEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = if (isNumeric) KeyboardType.Decimal else KeyboardType.Text
                ),
                cursorBrush = SolidColor(colors.azureAccent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End
                ).let { if (isNumeric) it.tabular() else it },
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
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

// =============================================================================
// 3. Unified Shift & Spend Calendar Sheet Content
// =============================================================================

@Composable
fun ActivityCalendarSheetContent(
    currencySymbol: String,
    shifts: List<ShiftEntity>,
    expenses: List<ExpenseEntity>,
    onDismiss: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]

    var displayedMonth by remember { mutableStateOf(YearMonth.now()) }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val fullDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", locale) }

    val shiftsByDay = remember(shifts) {
        shifts.groupBy { it.dateEpochDay }
    }
    val expensesByDay = remember(expenses) {
        expenses.groupBy { it.dateEpochDay }
    }

    val selectedEpoch = selectedDate.toEpochDay()
    val shiftsForSelectedDate = remember(selectedEpoch, shiftsByDay) {
        shiftsByDay[selectedEpoch].orEmpty()
    }
    val expensesForSelectedDate = remember(selectedEpoch, expensesByDay) {
        expensesByDay[selectedEpoch].orEmpty()
    }

    val selectedShiftMinutes = remember(shiftsForSelectedDate) {
        shiftsForSelectedDate.sumOf { it.durationMinutes.toLong() }
    }
    val selectedShiftEarnings = remember(shiftsForSelectedDate) {
        shiftsForSelectedDate.sumOf { it.totalEarned }
    }
    val selectedExpenseTotal = remember(expensesForSelectedDate) {
        expensesForSelectedDate.sumOf { it.amount }
    }

    val sundayFirstDays = remember {
        listOf(
            DayOfWeek.SUNDAY,
            DayOfWeek.MONDAY,
            DayOfWeek.TUESDAY,
            DayOfWeek.WEDNESDAY,
            DayOfWeek.THURSDAY,
            DayOfWeek.FRIDAY,
            DayOfWeek.SATURDAY
        )
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Month Navigation Header
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { displayedMonth = displayedMonth.minusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_chevron_left),
                            contentDescription = "Previous Month",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = displayedMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = { displayedMonth = displayedMonth.plusMonths(1) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_chevron_right),
                            contentDescription = "Next Month",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Weekday Headers (S M T W T F S)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    sundayFirstDays.forEach { dow ->
                        Text(
                            text = dow.getDisplayName(TextStyle.NARROW, locale),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Days Grid
                val firstDayOfMonth = displayedMonth.atDay(1)
                val dayOfWeekOffset = firstDayOfMonth.dayOfWeek.value % 7
                val daysInMonth = displayedMonth.lengthOfMonth()
                val totalCells = ((dayOfWeekOffset + daysInMonth + 6) / 7) * 7

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (week in 0 until (totalCells / 7)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (dayIdx in 0 until 7) {
                                val cellIndex = week * 7 + dayIdx
                                val dayNum = cellIndex - dayOfWeekOffset + 1
                                val isDayInMonth = dayNum in 1..daysInMonth

                                if (isDayInMonth) {
                                    val cellDate = displayedMonth.atDay(dayNum)
                                    val cellEpoch = cellDate.toEpochDay()
                                    val isSelected = cellDate == selectedDate

                                    val hasShift = shiftsByDay.containsKey(cellEpoch)
                                    val hasExpense = expensesByDay.containsKey(cellEpoch)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(if (isSelected) colors.azurePrimary else Color.Transparent)
                                            .clickable { selectedDate = cellDate },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Column(
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.Center
                                        ) {
                                            Text(
                                                text = dayNum.toString(),
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                                                ).tabular(),
                                                color = if (isSelected) PureWhite else colors.textPrimary
                                            )

                                            // Indicator Dots Row (Green = Shift, Red = Expense)
                                            if (hasShift || hasExpense) {
                                                Row(
                                                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    modifier = Modifier.padding(top = 2.dp)
                                                ) {
                                                    if (hasShift) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) PureWhite else Color(0xFF30D158))
                                                        )
                                                    }
                                                    if (hasExpense) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) PureWhite else Color(0xFFFF453A))
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
        }

        // Selected Date Summary & Details Card
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = selectedDate.format(fullDateFormatter),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )

                // Side by Side Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Shift Summary
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        shadowElevation = 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Shift Worked",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = if (shiftsForSelectedDate.isNotEmpty()) {
                                    ShiftlyFormatters.formatCurrency(currencySymbol, selectedShiftEarnings)
                                } else {
                                    "No shift"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = if (shiftsForSelectedDate.isNotEmpty()) colors.mintInk else colors.textSecondary
                            )
                            if (shiftsForSelectedDate.isNotEmpty()) {
                                Text(
                                    text = "${ShiftlyFormatters.formatHours(selectedShiftMinutes / 60.0, compact = true)} worked",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    // Expense Summary
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.surface,
                        shadowElevation = 0.dp,
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = "Expenses Logged",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                            Text(
                                text = if (expensesForSelectedDate.isNotEmpty()) {
                                    ShiftlyFormatters.formatCurrency(currencySymbol, selectedExpenseTotal)
                                } else {
                                    "No expenses"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = if (expensesForSelectedDate.isNotEmpty()) colors.coralInk else colors.textSecondary
                            )
                            if (expensesForSelectedDate.isNotEmpty()) {
                                Text(
                                    text = "${expensesForSelectedDate.size} expense(s)",
                                    style = MaterialTheme.typography.bodySmall.tabular(),
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }

                // Detailed Entries List
                if (shiftsForSelectedDate.isNotEmpty() || expensesForSelectedDate.isNotEmpty()) {
                    ShiftlyHairlineDivider()

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        shiftsForSelectedDate.forEach { shift ->
                            val zone = remember { ZoneId.systemDefault() }
                            val startLocal = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime()
                            val endLocal = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime()
                            val timeFormatter = DateTimeFormatter.ofPattern("h:mm a", locale)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ShiftlyIosIconTile(
                                        iconRes = R.drawable.ic_lucide_clock,
                                        backgroundColor = colors.azurePrimary,
                                        size = 28.dp,
                                        cornerRadius = 7.dp,
                                        iconSize = 22.dp
                                    )
                                    Text(
                                        text = "${startLocal.format(timeFormatter)} - ${endLocal.format(timeFormatter)}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily).tabular(),
                                        color = colors.textPrimary
                                    )
                                }
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currencySymbol, shift.totalEarned),
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = colors.mintInk
                                )
                            }
                        }

                        expensesForSelectedDate.forEach { expense ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ShiftlyIosIconTile(
                                        iconRes = R.drawable.ic_lucide_receipt_text,
                                        backgroundColor = colors.coralBright,
                                        size = 28.dp,
                                        cornerRadius = 7.dp,
                                        iconSize = 22.dp
                                    )
                                    Text(
                                        text = "${expense.category}: ${expense.title}",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "-${ShiftlyFormatters.formatCurrency(currencySymbol, expense.amount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = colors.coralInk
                                )
                            }
                        }
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
