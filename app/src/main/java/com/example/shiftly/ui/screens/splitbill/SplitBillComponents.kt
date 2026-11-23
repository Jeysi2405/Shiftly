package com.example.shiftly.ui.screens.splitbill

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.ui.components.ShiftlyConfirmDialog
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyIosSwitch
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.SplitBillMath
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val SplitPillShape = RoundedCornerShape(50)
private val SplitPrimaryButtonShape = RoundedCornerShape(14.dp)
private val SplitItemShape = RoundedCornerShape(18.dp)
private val SplitRowShape = RoundedCornerShape(12.dp)
private val SplitSheetTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

private data class SplitParticipantSummary(
    val trackedCount: Int,
    val paidCount: Int,
    val allPaid: Boolean
)

@Composable
fun SplitBillSummaryCard(
    currencySymbol: String,
    totalPendingAmount: Double,
    totalCollectedAmount: Double,
    paidParticipantsCount: Int,
    totalParticipantsCount: Int,
) {
    val colors = ShiftlyColors.current
    val paidFraction = remember(paidParticipantsCount, totalParticipantsCount) {
        if (totalParticipantsCount > 0) {
            (paidParticipantsCount.toFloat() / totalParticipantsCount.toFloat()).coerceIn(0f, 1f)
        } else {
            0f
        }
    }

    ShiftlyGlassCard(
        cornerRadius = 18.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "Pending",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, totalPendingAmount),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ).tabular(),
                        color = colors.textPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = SplitPillShape,
                        color = colors.mintContainer,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "Paid: ${ShiftlyFormatters.formatCurrency(currencySymbol, totalCollectedAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.mintTextOnSurface,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    if (totalParticipantsCount > 0) {
                        Text(
                            text = "$paidParticipantsCount of $totalParticipantsCount friend shares settled",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            ShiftlyProgressBar(
                progress = paidFraction,
                color = colors.mintBright,
                trackColor = colors.cardElevated,
                height = 5.dp
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewSplitBillBottomSheet(
    currencySymbol: String,
    initialAmount: String = "",
    initialTitle: String = "",
    initialPersonCount: Int = 2,
    prefillTrigger: Int = 0,
    onDismiss: () -> Unit,
    onSaveSplitBill: (
        title: String,
        totalAmount: Double,
        personCount: Int,
        participantNames: List<String>,
        participantPaidStates: List<Boolean>,
        alsoLogAsExpense: Boolean
    ) -> Unit
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
        shape = SplitSheetTopShape
    ) {
        NewSplitBillSheetContent(
            currencySymbol = currencySymbol,
            initialAmount = initialAmount,
            initialTitle = initialTitle,
            initialPersonCount = initialPersonCount,
            externalPrefillTrigger = prefillTrigger,
            onDismiss = onDismiss,
            onSaveSplitBill = onSaveSplitBill
        )
    }
}

@Composable
fun NewSplitBillSheetContent(
    currencySymbol: String,
    onDismiss: () -> Unit,
    onSaveSplitBill: (
        title: String,
        totalAmount: Double,
        personCount: Int,
        participantNames: List<String>,
        participantPaidStates: List<Boolean>,
        alsoLogAsExpense: Boolean
    ) -> Unit,
    initialAmount: String = "",
    initialTitle: String = "",
    initialPersonCount: Int = 2,
    externalPrefillTrigger: Int = 0
) {
    val colors = ShiftlyColors.current
    var titleInput by remember(externalPrefillTrigger) { mutableStateOf(initialTitle) }
    var amountInput by remember(externalPrefillTrigger) { mutableStateOf(initialAmount) }
    var personCount by remember(externalPrefillTrigger) {
        mutableIntStateOf(initialPersonCount.coerceIn(2, 20))
    }
    var alsoLogAsExpense by remember(externalPrefillTrigger) { mutableStateOf(true) }

    val customNames = remember(externalPrefillTrigger) {
        mutableStateListOf<String>().apply {
            val target = maxOf(6, initialPersonCount)
            for (i in 0 until target) {
                add(if (i == 0) "You" else "Person ${i + 1}")
            }
        }
    }

    val customPaidStates = remember(externalPrefillTrigger) {
        mutableStateListOf<Boolean>().apply {
            val target = maxOf(6, initialPersonCount)
            for (i in 0 until target) {
                add(i == 0)
            }
        }
    }

    fun ensureNameCapacity(count: Int) {
        while (customNames.size < count) {
            customNames.add("Person ${customNames.size + 1}")
        }
    }

    fun ensurePaidCapacity(count: Int) {
        while (customPaidStates.size < count) {
            customPaidStates.add(false)
        }
    }

    val totalAmount = remember(amountInput) { amountInput.toDoubleOrNull() ?: 0.0 }
    val perPersonShare = remember(totalAmount, personCount) {
        SplitBillMath.distributeEqualShares(totalAmount, personCount).firstOrNull() ?: 0.0
    }
    val isValid = remember(totalAmount, personCount) {
        totalAmount.isFinite() && (totalAmount > 0.0) && (personCount >= 2)
    }

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
        ShiftlySheetHeader(title = "New Split")

        // Total + Label Fields in a single inset-grouped container
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
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total Amount",
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
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.azureAccent
                            )
                        )
                        BasicTextField(
                            value = amountInput,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) amountInput = filtered
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
                                    if (amountInput.isEmpty()) {
                                        Text(
                                            text = "0.00",
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
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Bill Label",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    BasicTextField(
                        value = titleInput,
                        onValueChange = { titleInput = it },
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
                                if (titleInput.isEmpty()) {
                                    Text(
                                        text = "e.g. Dinner",
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

        // Persons Stepper & Clean Flat Inset-Grouped Person Rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PERSONS ($personCount)",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        letterSpacing = 0.5.sp
                    ).tabular(),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .shiftlyPressClickable(enabled = personCount > 2) {
                                if (personCount > 2) personCount--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (personCount > 2) colors.cardElevated
                                    else colors.cardElevated.copy(alpha = 0.4f)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_minus,
                                contentDescription = "Decrease persons",
                                tint = if (personCount > 2) colors.textPrimary else colors.textSecondary,
                                triggerKey = personCount,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .shiftlyPressClickable(enabled = personCount < 20) {
                                if (personCount < 20) {
                                    personCount++
                                    ensureNameCapacity(personCount)
                                    ensurePaidCapacity(personCount)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(
                                    if (personCount < 20) colors.azurePrimary
                                    else colors.disabledContainer
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            ShiftlyAnimatedIcon(
                                iconRes = R.drawable.ic_lucide_plus,
                                contentDescription = "Increase persons",
                                tint = if (personCount < 20) PureWhite else colors.disabledContent,
                                triggerKey = personCount,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    for (index in 0 until personCount) {
                        val isSelf = index == 0
                        val nameValue = customNames.getOrElse(index) { if (isSelf) "You" else "Person ${index + 1}" }
                        val isPaid = customPaidStates.getOrElse(index) { isSelf }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .padding(horizontal = 14.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            BasicTextField(
                                value = nameValue,
                                onValueChange = { updated ->
                                    ensureNameCapacity(index + 1)
                                    customNames[index] = updated
                                },
                                singleLine = true,
                                cursorBrush = SolidColor(colors.azureAccent),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 12.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .shiftlyPressClickable {
                                        ensurePaidCapacity(index + 1)
                                        customPaidStates[index] = !isPaid
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(20.dp),
                                    color = if (isPaid) colors.mintContainer else colors.surface,
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = if (isPaid) R.drawable.ic_lucide_check else R.drawable.ic_lucide_x,
                                            contentDescription = null,
                                            tint = if (isPaid) colors.mintInk else colors.textSecondary,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Text(
                                            text = if (isPaid) "Paid" else "Unpaid",
                                            style = MaterialTheme.typography.labelSmall.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = if (isPaid) colors.mintInk else colors.textSecondary
                                        )
                                    }
                                }
                            }
                        }
                        if (index < personCount - 1) {
                            ShiftlyHairlineDivider(startInset = 14.dp, endInset = 14.dp)
                        }
                    }
                }
            }
        }

        // Bold "Per Person" Result Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.mintContainer,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Per Person",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                    color = colors.textPrimary
                )
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, perPersonShare),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ).tabular(),
                    color = colors.mintTextOnSurface
                )
            }
        }

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
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Also log my share as an expense",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.textPrimary
                )
                ShiftlyIosSwitch(
                    checked = alsoLogAsExpense,
                    onCheckedChange = { alsoLogAsExpense = it }
                )
            }
        }

        ShiftlySheetBottomButtons(
            onCancel = onDismiss,
            onAction = {
                if (isValid) {
                    onSaveSplitBill(
                        titleInput,
                        totalAmount,
                        personCount,
                        customNames.take(personCount),
                        customPaidStates.take(personCount),
                        alsoLogAsExpense
                    )
                }
            },
            cancelLabel = "Cancel",
            actionLabel = "Save",
            actionEnabled = isValid
        )
    }
}

@Composable
fun SplitBillEmptyCard(
    currencySymbol: String = "₱",
    onTrySampleSplit: () -> Unit,
    selectedMonth: YearMonth? = YearMonth.now(),
    hasAnyBillsAtAll: Boolean = false,
    onShowAllMonths: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = remember(selectedMonth, locale) {
        selectedMonth?.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))
    }
    val showMonthReset = hasAnyBillsAtAll && monthLabel != null

    ShiftlyEmptyState(
        iconRes = R.drawable.ic_lucide_split,
        title = if (showMonthReset) "No split bills in $monthLabel" else "No split bills yet",
        subtitle = null,
        actionText = if (showMonthReset) "Show All Months" else "Try ${currencySymbol}1,200 ÷ 4 dinner",
        onAction = if (showMonthReset) onShowAllMonths else onTrySampleSplit,
        iconBackgroundColor = colors.azurePrimary
    )
}

@Composable
fun SplitBillLogItemCard(
    item: SplitBillWithParticipants,
    currencySymbol: String,
    onToggleParticipantPaid: (SplitParticipantEntity) -> Unit,
    onLogAsExpense: (SplitBillEntity) -> Unit,
    onDeleteBill: (SplitBillEntity) -> Unit,
    dateFormatter: DateTimeFormatter? = null
) {
    val colors = ShiftlyColors.current
    val bill = item.bill
    val participants = item.participants
    val summary = remember(participants) {
        val friendParticipants = participants.filter { !it.isCurrentUser }
        val trackedParticipants = friendParticipants.ifEmpty { participants }
        val paidCount = trackedParticipants.count { it.hasPaid }
        SplitParticipantSummary(
            trackedCount = trackedParticipants.size,
            paidCount = paidCount,
            allPaid = trackedParticipants.isNotEmpty() && paidCount == trackedParticipants.size
        )
    }
    val paidCount = summary.paidCount
    val allPaid = summary.allPaid

    val resolvedFormatter = dateFormatter ?: run {
        val locale = LocalConfiguration.current.locales[0]
        remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    }
    val dateLabel = remember(bill.dateEpochDay, resolvedFormatter) {
        LocalDate.ofEpochDay(bill.dateEpochDay).format(resolvedFormatter)
    }

    var selectedParticipantForPrompt by remember { mutableStateOf<SplitParticipantEntity?>(null) }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.card,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top Bill Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = bill.title,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = SplitPillShape,
                            color = if (allPaid) colors.mintContainer else colors.azureContainer,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = if (allPaid) "Paid" else "Pending ($paidCount/${summary.trackedCount})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = if (allPaid) colors.mintInk else colors.azureInk,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$dateLabel  •  ${ShiftlyFormatters.formatCurrency(currencySymbol, bill.totalAmount)} (${bill.personCount} people)",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary
                    )
                }

                var deleteTapCount by remember { mutableIntStateOf(0) }
                IconButton(
                    onClick = {
                        deleteTapCount++
                        onDeleteBill(bill)
                    },
                    modifier = Modifier.size(48.dp)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_trash_2,
                        contentDescription = "Delete Split Bill ${bill.title}",
                        tint = colors.coralInk,
                        triggerKey = deleteTapCount,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Inset-grouped Participant Rows
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SplitRowShape),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                participants.forEach { participant ->
                    val displayLabel = if (
                        participant.isCurrentUser &&
                        !participant.personLabel.equals("You", ignoreCase = true)
                    ) {
                        "${participant.personLabel} (You)"
                    } else if (participant.isCurrentUser) {
                        "You"
                    } else {
                        participant.personLabel
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(colors.surface)
                            .heightIn(min = 52.dp)
                            .padding(horizontal = 12.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            ShiftlyIosIconTile(
                                iconRes = R.drawable.ic_lucide_user,
                                backgroundColor = if (participant.hasPaid) colors.mintBright else colors.azurePrimary,
                                size = 28.dp,
                                cornerRadius = 8.dp,
                                iconSize = 22.dp
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = displayLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currencySymbol, participant.amountOwed),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ).tabular(),
                                    color = if (participant.hasPaid) colors.mintInk else colors.textSecondary
                                )
                            }
                        }

                        if (participant.hasPaid) {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(SplitPillShape)
                                    .clickable(onClickLabel = "Change payment status for $displayLabel") {
                                        selectedParticipantForPrompt = participant
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = SplitPillShape,
                                    color = colors.mintContainer,
                                    shadowElevation = 0.dp
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        ShiftlyAnimatedIcon(
                                            iconRes = R.drawable.ic_lucide_check,
                                            contentDescription = null,
                                            tint = colors.mintInk,
                                            modifier = Modifier.size(15.dp)
                                        )
                                        Text(
                                            text = "Paid",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = colors.mintInk
                                        )
                                    }
                                }
                            }
                        } else {
                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(SplitPillShape)
                                    .clickable(onClickLabel = "Mark $displayLabel as paid") {
                                        selectedParticipantForPrompt = participant
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Surface(
                                    shape = SplitPillShape,
                                    color = colors.azureContainer,
                                    shadowElevation = 0.dp
                                ) {
                                    Box(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = "Mark Paid",
                                            style = MaterialTheme.typography.labelMedium.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold
                                            ),
                                            color = colors.azureInk
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // One-Tap "Log Share as Expense" Action or Logged Confirmation Banner
            if (bill.isLoggedToExpense) {
                Surface(
                    shape = SplitPillShape,
                    color = colors.mintContainer,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_circle_check,
                            contentDescription = null,
                            tint = colors.mintInk,
                            animateOnMount = true,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logged to Expenses (${ShiftlyFormatters.formatCurrency(currencySymbol, bill.perPersonAmount)})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = colors.mintInk
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onLogAsExpense(bill) },
                    shape = SplitPrimaryButtonShape,
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
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_receipt_text,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log Share to Expenses (${ShiftlyFormatters.formatCurrency(currencySymbol, bill.perPersonAmount)})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = colors.onAzurePrimary
                    )
                }
            }
        }
    }

    // Payment Status Confirmation Prompt Dialog
    selectedParticipantForPrompt?.let { participant ->
        val personName = if (participant.isCurrentUser && participant.personLabel.equals("You", ignoreCase = true)) {
            "You"
        } else {
            participant.personLabel
        }
        val targetStatus = if (participant.hasPaid) "Unpaid" else "Paid"

        ShiftlyConfirmDialog(
            title = "Payment Status",
            message = "Mark $personName as $targetStatus?",
            iconRes = if (participant.hasPaid) R.drawable.ic_lucide_x else R.drawable.ic_lucide_check,
            iconBgColor = if (participant.hasPaid) colors.textSecondary else colors.mintBright,
            iconTint = if (participant.hasPaid) colors.textSecondary else colors.mintBright,
            noLabel = "No",
            yesLabel = "Yes",
            isDestructive = false,
            onDismiss = { selectedParticipantForPrompt = null },
            onConfirm = {
                onToggleParticipantPaid(participant)
                selectedParticipantForPrompt = null
            }
        )
    }
}
