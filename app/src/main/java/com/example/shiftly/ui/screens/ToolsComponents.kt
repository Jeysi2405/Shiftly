package com.example.shiftly.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
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
import com.example.shiftly.ui.components.ShiftlyAnimatedCurrencyText
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosSwitch
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.ShiftlySwipeAction
import com.example.shiftly.ui.components.ShiftlySwipeActionRow
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.expense.categoryIcon
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
import kotlinx.coroutines.delay

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
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = colors.mintContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "ESTIMATED NET TAKE-HOME PAY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        letterSpacing = 0.5.sp
                    ),
                    color = colors.mintInk
                )
                ShiftlyAnimatedCurrencyText(
                    currencySymbol = currencySymbol,
                    amount = netTakeHome,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = colors.mintTextOnSurface
                )
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
                ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)

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
                ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)

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
                ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)

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
                ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)

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
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.surface,
                shadowElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
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
    val haptics = LocalHapticFeedback.current
    val itemList = remember(entries) {
        mutableStateListOf<IouEntrySetting>().apply { addAll(entries) }
    }

    var personName by remember { mutableStateOf("") }
    var amountText by remember { mutableStateOf("") }
    var isLent by remember { mutableStateOf(true) }
    var noteText by remember { mutableStateOf("") }
    var pendingDeleteEntry by remember { mutableStateOf<IouEntrySetting?>(null) }
    var lastDeletedIou by remember { mutableStateOf<Pair<Int, IouEntrySetting>?>(null) }

    LaunchedEffect(lastDeletedIou) {
        if (lastDeletedIou != null) {
            delay(4_000L)
            lastDeletedIou = null
        }
    }

    val totalLent = remember(itemList.toList()) {
        itemList.filter { !it.isSettled && it.isLent }.sumOf { it.amount }
    }
    val totalBorrowed = remember(itemList.toList()) {
        itemList.filter { !it.isSettled && !it.isLent }.sumOf { it.amount }
    }

    val parsedAmount = remember(amountText) { amountText.toDoubleOrNull() ?: 0.0 }
    val hasValidNewEntry = personName.isNotBlank() && parsedAmount.isFinite() && parsedAmount > 0.0
    val hasEmptyNewForm = personName.isBlank() && amountText.isBlank() && noteText.isBlank()
    val canSaveIou = hasValidNewEntry || (hasEmptyNewForm && itemList.isNotEmpty())

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

        Text(
            text = "ADD NEW ENTRY",
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                letterSpacing = 0.5.sp
            ),
            color = colors.textSecondary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        IouDirectionToggle(
            isLent = isLent,
            onSelectLent = { isLent = it },
            containerColor = colors.cardElevated
        )

        IouFormCard(
            currencySymbol = currencySymbol,
            personName = personName,
            onPersonNameChange = { personName = it },
            amountText = amountText,
            onAmountTextChange = { amountText = it },
            noteText = noteText,
            onNoteTextChange = { noteText = it }
        )

        if (itemList.isNotEmpty()) {
            Text(
                text = "ENTRIES (${itemList.size})",
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    letterSpacing = 0.5.sp
                ).tabular(),
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
                        key(entry.id) {
                            val rowShape = when {
                                itemList.size == 1 -> RoundedCornerShape(18.dp)
                                idx == 0 -> RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp)
                                idx == itemList.lastIndex -> RoundedCornerShape(bottomStart = 18.dp, bottomEnd = 18.dp)
                                else -> RoundedCornerShape(0.dp)
                            }
                            ShiftlySwipeActionRow(
                                shape = rowShape,
                                startToEndAction = ShiftlySwipeAction(
                                    iconRes = if (entry.isSettled) R.drawable.ic_lucide_rotate_ccw else R.drawable.ic_lucide_check,
                                    label = if (entry.isSettled) "Reopen" else "Settle",
                                    backgroundColor = if (entry.isSettled) colors.azurePrimary else colors.mintBright,
                                    onTrigger = {
                                        val currentIdx = itemList.indexOfFirst { it.id == entry.id }
                                        if (currentIdx >= 0) {
                                            val currentEntry = itemList[currentIdx]
                                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            itemList[currentIdx] = currentEntry.copy(isSettled = !currentEntry.isSettled)
                                            onSaveEntries(itemList.toList())
                                        }
                                    }
                                ),
                                endToStartAction = ShiftlySwipeAction(
                                    iconRes = R.drawable.ic_lucide_trash_2,
                                    label = "Delete",
                                    backgroundColor = colors.coralBright,
                                    onTrigger = {
                                        val currentIdx = itemList.indexOfFirst { it.id == entry.id }
                                        if (currentIdx >= 0) {
                                            val removed = itemList.removeAt(currentIdx)
                                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                            onSaveEntries(itemList.toList())
                                            lastDeletedIou = currentIdx to removed
                                        }
                                    }
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(colors.cardElevated)
                                        .heightIn(min = 56.dp)
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

                                        val subText = if (entry.isLent) {
                                            "Lent • ${entry.note.ifBlank { "No note" }}"
                                        } else {
                                            "Borrowed • ${entry.note.ifBlank { "No note" }}"
                                        }
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
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
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
                                                val currentIdx = itemList.indexOfFirst { it.id == entry.id }
                                                if (currentIdx >= 0) {
                                                    itemList[currentIdx] = itemList[currentIdx].copy(isSettled = settled)
                                                    onSaveEntries(itemList.toList())
                                                }
                                            }
                                        )

                                        IconButton(
                                            onClick = { pendingDeleteEntry = entry },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            ShiftlyAnimatedIcon(
                                                iconRes = R.drawable.ic_lucide_trash_2,
                                                contentDescription = "Delete entry",
                                                tint = colors.coralInk,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            if (idx < itemList.lastIndex) {
                                ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = lastDeletedIou != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            lastDeletedIou?.let { (deletedIdx, deletedEntry) ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 14.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Removed ${deletedEntry.personName}",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        TextButton(
                            onClick = {
                                val insertAt = deletedIdx.coerceIn(0, itemList.size)
                                itemList.add(insertAt, deletedEntry)
                                onSaveEntries(itemList.toList())
                                lastDeletedIou = null
                            },
                            modifier = Modifier.heightIn(min = 40.dp)
                        ) {
                            Text(
                                text = "Undo",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.azureAccent
                            )
                        }
                    }
                }
            }
        }

        ShiftlySheetBottomButtons(
            onCancel = onDismiss,
            onAction = {
                if (hasValidNewEntry) {
                    itemList.add(
                        0,
                        IouEntrySetting(
                            id = UUID.randomUUID().toString(),
                            personName = personName.trim(),
                            amount = parsedAmount,
                            isLent = isLent,
                            note = noteText.trim(),
                            isSettled = false
                        )
                    )
                    onSaveEntries(itemList.toList())
                    onDismiss()
                } else if (hasEmptyNewForm && itemList.isNotEmpty()) {
                    onSaveEntries(itemList.toList())
                    onDismiss()
                }
            },
            cancelLabel = "Cancel",
            actionLabel = "Save IOU",
            actionEnabled = canSaveIou
        )
    }

    pendingDeleteEntry?.let { entryToDelete ->
        ShiftlyDeleteConfirmDialog(
            title = "Delete Record?",
            message = "Delete IOU for ${entryToDelete.personName}?",
            onDismiss = { pendingDeleteEntry = null },
            onConfirm = {
                itemList.removeAll { it.id == entryToDelete.id }
                onSaveEntries(itemList.toList())
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

            IouDirectionToggle(
                isLent = isLent,
                onSelectLent = { isLent = it },
                containerColor = colors.cardElevated
            )

            IouFormCard(
                currencySymbol = currencySymbol,
                personName = personName,
                onPersonNameChange = { personName = it },
                amountText = amountText,
                onAmountTextChange = { amountText = it },
                noteText = noteText,
                onNoteTextChange = { noteText = it }
            )

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    if (isValid) {
                        onSave(personName.trim(), parsedAmount, isLent, noteText.trim())
                    }
                },
                cancelLabel = "Cancel",
                actionLabel = "Save IOU",
                actionEnabled = isValid
            )
        }
    }
}

@Composable
private fun IouDirectionToggle(
    isLent: Boolean,
    onSelectLent: (Boolean) -> Unit,
    containerColor: Color
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(9.dp))
            .background(containerColor)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (isLent) colors.mintContainer else Color.Transparent)
                .shiftlyPressClickable(role = Role.Tab) { onSelectLent(true) },
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
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(if (!isLent) colors.coralContainer else Color.Transparent)
                .shiftlyPressClickable(role = Role.Tab) { onSelectLent(false) },
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
}

@Composable
private fun IouFormCard(
    currencySymbol: String,
    personName: String,
    onPersonNameChange: (String) -> Unit,
    amountText: String,
    onAmountTextChange: (String) -> Unit,
    noteText: String,
    onNoteTextChange: (String) -> Unit
) {
    val colors = ShiftlyColors.current
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
                onValueChange = onPersonNameChange,
                placeholder = "Name"
            )
            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
            IouInsetRow(
                label = "Amount ($currencySymbol)",
                value = amountText,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) onAmountTextChange(filtered)
                },
                placeholder = "0.00",
                prefix = currencySymbol,
                isNumeric = true
            )
            ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)
            IouInsetRow(
                label = "Note (optional)",
                value = noteText,
                onValueChange = onNoteTextChange,
                placeholder = "Note"
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

    val todayDate = remember { LocalDate.now() }
    var displayedMonth by remember { mutableStateOf(YearMonth.from(todayDate)) }
    var prevMonthTrigger by remember { mutableIntStateOf(0) }
    var nextMonthTrigger by remember { mutableIntStateOf(0) }
    var selectedDate by remember { mutableStateOf(todayDate) }

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
                        onClick = {
                            prevMonthTrigger++
                            displayedMonth = displayedMonth.minusMonths(1)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_chevron_left,
                            contentDescription = "Previous Month",
                            tint = colors.textPrimary,
                            triggerKey = prevMonthTrigger,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Text(
                        text = displayedMonth.format(monthFormatter),
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary
                    )

                    IconButton(
                        onClick = {
                            nextMonthTrigger++
                            displayedMonth = displayedMonth.plusMonths(1)
                        },
                        modifier = Modifier.size(48.dp)
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_chevron_right,
                            contentDescription = "Next Month",
                            tint = colors.textPrimary,
                            triggerKey = nextMonthTrigger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

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
                                    val isToday = cellDate == todayDate

                                    val hasShift = shiftsByDay.containsKey(cellEpoch)
                                    val hasExpense = expensesByDay.containsKey(cellEpoch)

                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(
                                                when {
                                                    isSelected -> colors.azurePrimary
                                                    isToday -> colors.azureContainer.copy(alpha = 0.45f)
                                                    else -> Color.Transparent
                                                }
                                            )
                                            .shiftlyPressClickable { selectedDate = cellDate },
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
                                                    fontWeight = when {
                                                        isSelected -> FontWeight.Bold
                                                        isToday -> FontWeight.SemiBold
                                                        else -> FontWeight.Medium
                                                    }
                                                ).tabular(),
                                                color = when {
                                                    isSelected -> PureWhite
                                                    isToday -> colors.azureInk
                                                    else -> colors.textPrimary
                                                }
                                            )

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
                                                                .background(if (isSelected) PureWhite else colors.mintBright)
                                                        )
                                                    }
                                                    if (hasExpense) {
                                                        Box(
                                                            modifier = Modifier
                                                                .size(5.dp)
                                                                .clip(CircleShape)
                                                                .background(if (isSelected) PureWhite else colors.coralBright)
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
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                color = if (shiftsForSelectedDate.isNotEmpty()) colors.mintInk else colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (shiftsForSelectedDate.isNotEmpty()) {
                                Text(
                                    text = "${ShiftlyFormatters.formatHours(selectedShiftMinutes / 60.0, compact = true)} worked",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

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
                                style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily),
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
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
                                color = if (expensesForSelectedDate.isNotEmpty()) colors.coralInk else colors.textSecondary,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (expensesForSelectedDate.isNotEmpty()) {
                                Text(
                                    text = "${expensesForSelectedDate.size} expense(s)",
                                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                if (shiftsForSelectedDate.isNotEmpty() || expensesForSelectedDate.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surface,
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            shiftsForSelectedDate.forEachIndexed { idx, shift ->
                                val zone = remember { ZoneId.systemDefault() }
                                val startLocal = Instant.ofEpochMilli(shift.timeInMillis).atZone(zone).toLocalTime()
                                val endLocal = Instant.ofEpochMilli(shift.timeOutMillis).atZone(zone).toLocalTime()
                                val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = R.drawable.ic_lucide_clock,
                                            contentDescription = null,
                                            tint = colors.azureInk,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = "${startLocal.format(timeFormatter)} - ${endLocal.format(timeFormatter)}",
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                ).tabular(),
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${ShiftlyFormatters.formatHours(shift.durationMinutes / 60.0, compact = true)} shift",
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily),
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = ShiftlyFormatters.formatCurrency(currencySymbol, shift.totalEarned),
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        color = colors.mintInk,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (idx < shiftsForSelectedDate.lastIndex || expensesForSelectedDate.isNotEmpty()) {
                                    ShiftlyHairlineDivider(startInset = 44.dp)
                                }
                            }

                            expensesForSelectedDate.forEachIndexed { idx, expense ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = categoryIcon(expense.category),
                                            contentDescription = null,
                                            tint = colors.coralBright,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Text(
                                                text = expense.title.ifBlank { expense.category },
                                                style = MaterialTheme.typography.bodyMedium.copy(
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = FontWeight.SemiBold
                                                ),
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = expense.category,
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily),
                                                color = colors.textSecondary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    }
                                    Text(
                                        text = "-${ShiftlyFormatters.formatCurrency(currencySymbol, expense.amount)}",
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        color = colors.coralInk,
                                        maxLines = 1,
                                        softWrap = false
                                    )
                                }
                                if (idx < expensesForSelectedDate.lastIndex) {
                                    ShiftlyHairlineDivider(startInset = 44.dp)
                                }
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
