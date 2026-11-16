package com.example.shiftly.ui.screens.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.DeepAzureBottom
import com.example.shiftly.ui.theme.HairlineLight
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.InkPrimary
import com.example.shiftly.ui.theme.InkSecondary
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.SoftCloudWhite
import com.example.shiftly.ui.theme.tabular
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SplitBillSummaryCard(
    currencySymbol: String,
    totalPendingAmount: Double,
    totalCollectedAmount: Double,
    paidParticipantsCount: Int,
    totalParticipantsCount: Int,
) {
    val paidFraction = if (totalParticipantsCount > 0) {
        (paidParticipantsCount.toFloat() / totalParticipantsCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
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
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = PureWhite.copy(alpha = 0.88f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%,.2f".format(totalPendingAmount)}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ).tabular(),
                        color = PureWhite
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MintBright.copy(alpha = 0.22f),
                        border = BorderStroke(1.dp, MintBright.copy(alpha = 0.50f))
                    ) {
                        Text(
                            text = "Paid: $currencySymbol${"%,.2f".format(totalCollectedAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = MintTextOnBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    if (totalParticipantsCount > 0) {
                        Text(
                            text = "$paidParticipantsCount of $totalParticipantsCount shares settled",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = PureWhite.copy(alpha = 0.82f)
                        )
                    }
                }
            }

            // Paid vs Unpaid Mint Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(PureWhite.copy(alpha = 0.20f))
            ) {
                if (paidFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(paidFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(50))
                            .background(MintBright)
                    )
                }
            }
        }
    }
}

@Composable
fun SplitBillCalculatorCard(
    currencySymbol: String,
    onSaveSplitBill: (
        title: String,
        totalAmount: Double,
        personCount: Int,
        participantNames: List<String>,
        alsoLogAsExpense: Boolean
    ) -> Unit,
    initialAmount: String = "",
    initialTitle: String = "",
    initialPersonCount: Int = 2,
    externalPrefillTrigger: Int = 0
) {
    var titleInput by remember(externalPrefillTrigger) { mutableStateOf(initialTitle) }
    var amountInput by remember(externalPrefillTrigger) { mutableStateOf(initialAmount) }
    var personCount by remember(externalPrefillTrigger) { mutableIntStateOf(initialPersonCount) }
    var editNamesExpanded by remember { mutableStateOf(false) }

    val customNames = remember {
        mutableStateListOf("You", "Person 2", "Person 3", "Person 4", "Person 5", "Person 6")
    }

    fun ensureNameCapacity(count: Int) {
        while (customNames.size < count) {
            customNames.add("Person ${customNames.size + 1}")
        }
    }

    val totalAmount = amountInput.toDoubleOrNull() ?: 0.0
    val perPersonShare = if (personCount > 0) totalAmount / personCount else 0.0
    val isValid = (totalAmount > 0.0) && (personCount >= 2)
    val quickCounts = listOf(2, 3, 4, 5, 6)

    val glassFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = PureWhite,
        unfocusedBorderColor = PureWhite.copy(alpha = 0.42f),
        focusedLabelColor = PureWhite,
        unfocusedLabelColor = PureWhite.copy(alpha = 0.85f),
        focusedTextColor = PureWhite,
        unfocusedTextColor = PureWhite,
        cursorColor = MintTextOnBlue,
        focusedContainerColor = PureWhite.copy(alpha = 0.10f),
        unfocusedContainerColor = PureWhite.copy(alpha = 0.08f)
    )

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(
                text = "NEW SPLIT",
                style = MaterialTheme.typography.labelMedium.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 0.8.sp
                ),
                color = PureWhite.copy(alpha = 0.90f)
            )

            // Total (₱) + Label Fields
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Total ($currencySymbol)", fontFamily = ShiftlySerif) },
                    placeholder = {
                        Text("0.00", color = PureWhite.copy(alpha = 0.45f))
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = glassFieldColors,
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Label", fontFamily = ShiftlySerif) },
                    placeholder = {
                        Text("Dinner", color = PureWhite.copy(alpha = 0.45f))
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = glassFieldColors,
                    modifier = Modifier.weight(1.15f)
                )
            }

            // Persons Stepper (- / Count / +) with 48dp circular targets
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Persons",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = PureWhite
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(
                                    if (personCount > 2) PureWhite.copy(alpha = 0.18f)
                                    else PureWhite.copy(alpha = 0.08f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (personCount > 2) PureWhite.copy(alpha = 0.35f)
                                    else PureWhite.copy(alpha = 0.14f),
                                    shape = CircleShape
                                )
                                .clickable(enabled = personCount > 2) {
                                    if (personCount > 2) personCount--
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Remove,
                                contentDescription = "Decrease persons",
                                tint = if (personCount > 2) PureWhite else PureWhite.copy(alpha = 0.35f),
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Text(
                            text = personCount.toString(),
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = ShiftlySans,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = MintTextOnBlue,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )

                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(PureWhite)
                                .clickable(enabled = personCount < 20) {
                                    if (personCount < 20) {
                                        personCount++
                                        ensureNameCapacity(personCount)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Add,
                                contentDescription = "Increase persons",
                                tint = DeepAzureBottom,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Quick-Pick Person Chips (2p to 6p) + 48dp Edit Names Icon Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickCounts.forEach { count ->
                        val selected = personCount == count
                        Box(
                            modifier = Modifier
                                .widthIn(min = 48.dp)
                                .heightIn(min = 48.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (selected) PureWhite else PureWhite.copy(alpha = 0.14f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (selected) PureWhite else PureWhite.copy(alpha = 0.28f),
                                    shape = RoundedCornerShape(50)
                                )
                                .clickable {
                                    personCount = count
                                    ensureNameCapacity(count)
                                }
                                .padding(horizontal = 12.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "${count}p",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = if (selected) DeepAzureBottom else PureWhite
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (editNamesExpanded) MintBright.copy(alpha = 0.28f)
                                else PureWhite.copy(alpha = 0.14f)
                            )
                            .border(
                                width = 1.dp,
                                color = if (editNamesExpanded) MintTextOnBlue
                                else PureWhite.copy(alpha = 0.32f),
                                shape = CircleShape
                            )
                            .clickable {
                                ensureNameCapacity(personCount)
                                editNamesExpanded = !editNamesExpanded
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = if (editNamesExpanded) "Hide participant names" else "Edit participant names",
                            tint = MintTextOnBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (editNamesExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (index in 0 until personCount) {
                        OutlinedTextField(
                            value = customNames.getOrElse(index) { "Person ${index + 1}" },
                            onValueChange = { updated ->
                                ensureNameCapacity(index + 1)
                                customNames[index] = updated
                            },
                            label = {
                                Text(
                                    text = if (index == 0) "Your Name" else "Person ${index + 1}",
                                    fontFamily = ShiftlySerif
                                )
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            colors = glassFieldColors,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Large Bold "Per Person" Result Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = MintBright.copy(alpha = 0.18f),
                border = BorderStroke(1.dp, MintBright.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Per Person",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = ShiftlySerif),
                            color = PureWhite
                        )
                        Text(
                            text = "Split evenly among $personCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite.copy(alpha = 0.80f)
                        )
                    }
                    Text(
                        text = "$currencySymbol${"%,.2f".format(perPersonShare)}",
                        style = MaterialTheme.typography.headlineSmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp
                        ).tabular(),
                        color = MintTextOnBlue
                    )
                }
            }

            // Distinct Action Buttons: "Save" (pending) vs "Split & Log" (pending + log share)
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
                            false
                        )
                        titleInput = ""
                        amountInput = ""
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(
                        width = 1.4.dp,
                        color = if (isValid) PureWhite else PureWhite.copy(alpha = 0.28f)
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = PureWhite,
                        disabledContentColor = PureWhite.copy(alpha = 0.45f)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 52.dp)
                ) {
                    Text(
                        text = "Save",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ShiftlySerif,
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
                            true
                        )
                        titleInput = ""
                        amountInput = ""
                    },
                    enabled = isValid,
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkBlack,
                        contentColor = PureWhite,
                        disabledContainerColor = InkBlack.copy(alpha = 0.35f),
                        disabledContentColor = PureWhite.copy(alpha = 0.52f)
                    ),
                    modifier = Modifier
                        .weight(1.3f)
                        .heightIn(min = 52.dp)
                ) {
                    Text(
                        text = "Split & Log",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun SplitBillEmptyCard(
    onTrySampleSplit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SoftCloudWhite,
        border = BorderStroke(1.dp, HairlineLight),
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
                    .background(AzureSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.CallSplit,
                    contentDescription = null,
                    tint = AzureInkOnWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = "No split bills yet",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                color = InkPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Enter a group total above to divide shares and log your portion.",
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShiftlyPrimaryPillButton(
                text = "Try ₱1,200 ÷ 4 dinner",
                onClick = onTrySampleSplit
            )
        }
    }
}

@Composable
fun SplitBillLogItemCard(
    item: SplitBillWithParticipants,
    currencySymbol: String,
    onToggleParticipantPaid: (SplitParticipantEntity) -> Unit,
    onLogAsExpense: (SplitBillEntity) -> Unit,
    onDeleteBill: (SplitBillEntity) -> Unit
) {
    val bill = item.bill
    val participants = item.participants
    val paidCount = participants.count { it.hasPaid }
    val allPaid = participants.isNotEmpty() && paidCount == participants.size

    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val dateLabel = remember(bill.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(bill.dateEpochDay).format(dateFormatter)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(
            width = 1.dp,
            color = if (allPaid) MintInkOnWhite.copy(alpha = 0.45f) else HairlineLight
        ),
        shadowElevation = 2.dp,
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
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = InkPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (allPaid) MintSoftContainer else AzureSoftContainer
                        ) {
                            Text(
                                text = if (allPaid) "Paid" else "Pending ($paidCount/${participants.size})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = if (allPaid) MintInkOnWhite else AzureInkOnWhite,
                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = "$dateLabel  •  $currencySymbol${"%,.2f".format(bill.totalAmount)} (${bill.personCount} people)",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = InkSecondary
                    )
                }

                IconButton(
                    onClick = { onDeleteBill(bill) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Split Bill",
                        tint = CoralInkOnWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = HairlineLight)

            // Participant Rows with 48dp Min Height Paid / Mark Paid Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(SoftCloudWhite)
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
                                    .background(if (participant.hasPaid) MintSoftContainer else AzureSoftContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = if (participant.hasPaid) MintInkOnWhite else AzureInkOnWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = participant.personLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = InkPrimary
                                )
                                Text(
                                    text = "$currencySymbol${"%,.2f".format(participant.amountOwed)}",
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = if (participant.hasPaid) MintInkOnWhite else InkSecondary
                                )
                            }
                        }

                        if (participant.hasPaid) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MintSoftContainer,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable { onToggleParticipantPaid(participant) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MintInkOnWhite,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Paid",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = ShiftlySerif,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MintInkOnWhite
                                    )
                                }
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onToggleParticipantPaid(participant) },
                                shape = RoundedCornerShape(50),
                                border = BorderStroke(1.dp, AzureInkOnWhite),
                                contentPadding = PaddingValues(horizontal = 14.dp),
                                modifier = Modifier.heightIn(min = 48.dp)
                            ) {
                                Text(
                                    text = "Mark Paid",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = ShiftlySerif,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = AzureInkOnWhite
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
                    color = MintSoftContainer,
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
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = MintInkOnWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logged to Expenses ($currencySymbol${"%,.2f".format(bill.perPersonAmount)})",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = MintInkOnWhite
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onLogAsExpense(bill) },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = InkBlack,
                        contentColor = PureWhite
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log Share to Expenses ($currencySymbol${"%,.2f".format(bill.perPersonAmount)})",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold
                        ).tabular(),
                        color = PureWhite
                    )
                }
            }
        }
    }
}
