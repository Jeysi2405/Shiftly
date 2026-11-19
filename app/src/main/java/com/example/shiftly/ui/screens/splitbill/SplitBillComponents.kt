package com.example.shiftly.ui.screens.splitbill

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
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
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.SplitBillMath
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

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

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "PENDING",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, totalPendingAmount),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = PlusJakartaSans,
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
                        shape = RoundedCornerShape(50),
                        color = colors.mintContainer,
                        border = BorderStroke(1.dp, colors.mintBright.copy(alpha = 0.45f)),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "Paid: ${ShiftlyFormatters.formatCurrency(currencySymbol, totalCollectedAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
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
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            val animatedPaidFraction by animateFloatAsState(
                targetValue = paidFraction.coerceIn(0f, 1f),
                animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
                label = "paidFractionAnim"
            )

            // Paid vs Unpaid Mint Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(colors.border)
            ) {
                if (animatedPaidFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(animatedPaidFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(colors.mintBright)
                    )
                }
            }
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
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
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

    val sheetFieldColors = OutlinedTextFieldDefaults.colors(
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "New Split",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 44.dp)
            ) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                    color = colors.textSecondary
                )
            }
        }

        // Total + Label Fields
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedTextField(
                value = amountInput,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' }
                    if (filtered.count { it == '.' } <= 1) {
                        amountInput = filtered
                    }
                },
                label = { Text("Total", fontFamily = PlusJakartaSans) },
                placeholder = {
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textMuted
                    )
                },
                prefix = {
                    Text(
                        text = "$currencySymbol ",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.azureAccent
                    )
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = RoundedCornerShape(16.dp),
                colors = sheetFieldColors,
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = titleInput,
                onValueChange = { titleInput = it },
                label = { Text("Label", fontFamily = PlusJakartaSans) },
                placeholder = {
                    Text(
                        text = "Dinner",
                        style = MaterialTheme.typography.bodyLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textMuted
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = sheetFieldColors,
                modifier = Modifier.weight(1.15f)
            )
        }

        // Persons Stepper (- / Count / +) with Dynamic Name & Paid Status Rows
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Persons",
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (personCount > 2) colors.card
                                else colors.surface
                            )
                            .border(
                                width = 1.dp,
                                color = if (personCount > 2) colors.border
                                else colors.border.copy(alpha = 0.60f),
                                shape = CircleShape
                            )
                            .clickable(enabled = personCount > 2) {
                                if (personCount > 2) personCount--
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_minus),
                            contentDescription = "Decrease persons",
                            tint = if (personCount > 2) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Text(
                        text = personCount.toString(),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.mintTextOnSurface,
                        modifier = Modifier.padding(horizontal = 4.dp)
                    )

                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(
                                if (personCount < 20) colors.azurePrimary
                                else colors.disabledContainer
                            )
                            .clickable(enabled = personCount < 20) {
                                if (personCount < 20) {
                                    personCount++
                                    ensureNameCapacity(personCount)
                                    ensurePaidCapacity(personCount)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_plus),
                            contentDescription = "Increase persons",
                            tint = if (personCount < 20) colors.onAzurePrimary else colors.disabledContent,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                for (index in 0 until personCount) {
                    val isSelf = index == 0
                    val nameValue = customNames.getOrElse(index) { if (isSelf) "You" else "Person ${index + 1}" }
                    val isPaid = customPaidStates.getOrElse(index) { isSelf }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = nameValue,
                            onValueChange = { updated ->
                                ensureNameCapacity(index + 1)
                                customNames[index] = updated
                            },
                            label = {
                                Text(
                                    text = if (isSelf) "You" else "Person ${index + 1}",
                                    fontFamily = PlusJakartaSans
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = sheetFieldColors,
                            modifier = Modifier.weight(1f)
                        )

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (isPaid) colors.mintContainer else colors.card,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (isPaid) colors.mintBright else colors.border
                            ),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .heightIn(min = 44.dp)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    ensurePaidCapacity(index + 1)
                                    customPaidStates[index] = !isPaid
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(
                                        if (isPaid) R.drawable.ic_lucide_check else R.drawable.ic_lucide_x
                                    ),
                                    contentDescription = null,
                                    tint = if (isPaid) colors.mintInk else colors.textSecondary,
                                    modifier = Modifier.size(15.dp)
                                )
                                Text(
                                    text = if (isPaid) "Paid" else "Unpaid",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = if (isPaid) colors.mintInk else colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Bold "Per Person" Result Banner
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.mintContainer,
            border = BorderStroke(1.dp, colors.mintBright.copy(alpha = 0.40f)),
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
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                    color = colors.textPrimary
                )
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, perPersonShare),
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp
                    ).tabular(),
                    color = colors.mintTextOnSurface
                )
            }
        }

        // Docked Action Buttons: "Save" vs "Split & Log"
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = {
                    onSaveSplitBill(
                        titleInput,
                        totalAmount,
                        personCount,
                        customNames.take(personCount),
                        customPaidStates.take(personCount),
                        false
                    )
                },
                enabled = isValid,
                shape = RoundedCornerShape(50),
                border = BorderStroke(
                    width = 1.4.dp,
                    color = if (isValid) colors.azureAccent else colors.border
                ),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = colors.textPrimary,
                    disabledContentColor = colors.disabledContent
                ),
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 52.dp)
            ) {
                Text(
                    text = "Save",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Button(
                onClick = {
                    onSaveSplitBill(
                        titleInput,
                        totalAmount,
                        personCount,
                        customNames.take(personCount),
                        customPaidStates.take(personCount),
                        true
                    )
                },
                enabled = isValid,
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.azurePrimary,
                    contentColor = colors.onAzurePrimary,
                    disabledContainerColor = colors.disabledContainer,
                    disabledContentColor = colors.disabledContent
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp,
                    disabledElevation = 0.dp
                ),
                modifier = Modifier
                    .weight(1.3f)
                    .heightIn(min = 52.dp)
            ) {
                Text(
                    text = "Split & Log",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
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
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.azureContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_split),
                    contentDescription = null,
                    tint = colors.azureInk,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = if (showMonthReset) "No split bills in $monthLabel" else "No split bills yet",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (showMonthReset) {
                ShiftlyPrimaryPillButton(
                    text = "Show All Months",
                    onClick = onShowAllMonths
                )
            } else {
                ShiftlyPrimaryPillButton(
                    text = "Try ${currencySymbol}1,200 ÷ 4 dinner",
                    onClick = onTrySampleSplit
                )
            }
        }
    }
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
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(
            width = 1.dp,
            color = if (allPaid) colors.mintBright.copy(alpha = 0.45f) else colors.cardBorder
        ),
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
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (allPaid) colors.mintContainer else colors.azureContainer,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = if (allPaid) "Paid" else "Pending ($paidCount/${summary.trackedCount})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSans,
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

                IconButton(
                    onClick = { onDeleteBill(bill) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_trash_2),
                        contentDescription = "Delete Split Bill ${bill.title}",
                        tint = colors.coralInk,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = colors.border)

            // Participant Rows with Payment Prompt Action for all participants (including yourself)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(if (participant.hasPaid) colors.mintContainer else colors.azureContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_user),
                                    contentDescription = null,
                                    tint = if (participant.hasPaid) colors.mintInk else colors.azureInk,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = displayLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currencySymbol, participant.amountOwed),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Medium
                                    ).tabular(),
                                    color = if (participant.hasPaid) colors.mintInk else colors.textSecondary
                                )
                            }
                        }

                        if (participant.hasPaid) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = colors.mintContainer,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .heightIn(min = 44.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClickLabel = "Change payment status for $displayLabel") {
                                        selectedParticipantForPrompt = participant
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_check),
                                        contentDescription = null,
                                        tint = colors.mintInk,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Paid",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = colors.mintInk
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { selectedParticipantForPrompt = participant },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, colors.azureInk),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier.heightIn(min = 44.dp)
                            ) {
                                Text(
                                    text = "Mark Paid",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.azureInk
                                )
                            }
                        }
                    }
                }
            }

            // One-Tap "Log Share as Expense" Action or Logged Confirmation Banner
            if (bill.isLoggedToExpense) {
                Surface(
                    shape = RoundedCornerShape(50),
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
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_circle_check),
                            contentDescription = null,
                            tint = colors.mintInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logged to Expenses (${ShiftlyFormatters.formatCurrency(currencySymbol, bill.perPersonAmount)})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = colors.mintInk
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onLogAsExpense(bill) },
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
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_receipt_text),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log Share to Expenses (${ShiftlyFormatters.formatCurrency(currencySymbol, bill.perPersonAmount)})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = PlusJakartaSans,
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
        val personName = if (participant.isCurrentUser && participant.personLabel.equals("You", ignoreCase = true)) "You" else participant.personLabel
        val targetStatus = if (participant.hasPaid) "Unpaid" else "Paid"

        AlertDialog(
            onDismissRequest = { selectedParticipantForPrompt = null },
            containerColor = colors.card,
            icon = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (participant.hasPaid) colors.surface else colors.mintContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(
                            if (participant.hasPaid) R.drawable.ic_lucide_x else R.drawable.ic_lucide_check
                        ),
                        contentDescription = null,
                        tint = if (participant.hasPaid) colors.textSecondary else colors.mintInk,
                        modifier = Modifier.size(22.dp)
                    )
                }
            },
            title = {
                Text(
                    text = "Payment Status",
                    style = MaterialTheme.typography.titleMedium.copy(fontFamily = PlusJakartaSans),
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Confirm marking $personName as $targetStatus for this bill?",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onToggleParticipantPaid(participant)
                        selectedParticipantForPrompt = null
                    },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (participant.hasPaid) colors.surface else colors.azurePrimary,
                        contentColor = if (participant.hasPaid) colors.textPrimary else colors.onAzurePrimary
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    )
                ) {
                    Text(
                        text = "Mark $targetStatus",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedParticipantForPrompt = null }) {
                    Text(
                        text = "Cancel",
                        style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
                        color = colors.textSecondary
                    )
                }
            }
        )
    }
}
