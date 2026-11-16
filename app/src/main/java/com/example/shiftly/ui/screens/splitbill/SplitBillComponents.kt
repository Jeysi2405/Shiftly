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
import androidx.compose.foundation.selection.selectableGroup
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
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
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkDisabledContainer
import com.example.shiftly.ui.theme.DarkDisabledContent
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.MintTextOnBlue
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.TextLightMuted
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import com.example.shiftly.utils.SplitBillMath
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

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
                        color = TextLightSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, totalPendingAmount),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp
                        ).tabular(),
                        color = TextLightPrimary
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MintSoftContainer,
                        border = BorderStroke(1.dp, MintBright.copy(alpha = 0.45f))
                    ) {
                        Text(
                            text = "Paid: ${ShiftlyFormatters.formatCurrency(currencySymbol, totalCollectedAmount)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = MintTextOnBlue,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    if (totalParticipantsCount > 0) {
                        Text(
                            text = "$paidParticipantsCount of $totalParticipantsCount friend shares settled",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = TextLightSecondary
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
                    .background(DarkBorder)
                    .semantics {
                        contentDescription = if (totalParticipantsCount > 0) {
                            "${(paidFraction * 100f).roundToInt()}% settled, $paidParticipantsCount of $totalParticipantsCount friend shares paid"
                        } else {
                            "No active split shares"
                        }
                    }
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
    val perPersonShare = SplitBillMath.distributeEqualShares(totalAmount, personCount).firstOrNull() ?: 0.0
    val isValid = totalAmount.isFinite() && (totalAmount > 0.0) && (personCount >= 2)
    val quickCounts = listOf(2, 3, 4, 5, 6)

    val glassFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AzureAccent,
        unfocusedBorderColor = DarkBorder,
        focusedLabelColor = AzureAccent,
        unfocusedLabelColor = TextLightSecondary,
        focusedTextColor = TextLightPrimary,
        unfocusedTextColor = TextLightPrimary,
        cursorColor = AzureAccent,
        focusedContainerColor = DarkCard,
        unfocusedContainerColor = DarkCard
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
                color = TextLightSecondary
            )

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
                    label = { Text("Total ($currencySymbol)", fontFamily = ShiftlySerif) },
                    placeholder = {
                        Text("0", color = TextLightMuted)
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
                        Text("Dinner", color = TextLightMuted)
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
                        color = TextLightPrimary
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
                                    if (personCount > 2) DarkCard
                                    else DarkSurface
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (personCount > 2) DarkBorder
                                    else DarkBorder.copy(alpha = 0.60f),
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
                                tint = if (personCount > 2) TextLightPrimary else TextLightMuted,
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
                                .background(
                                    if (personCount < 20) AzurePrimaryButton
                                    else DarkDisabledContainer
                                )
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
                                tint = if (personCount < 20) PureWhite else DarkDisabledContent,
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
                    Row(
                        modifier = Modifier.selectableGroup(),
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        quickCounts.forEach { count ->
                            val isSelected = personCount == count
                            Box(
                                modifier = Modifier
                                    .widthIn(min = 48.dp)
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (isSelected) AzurePrimaryButton else DarkCard
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) AzureAccent else DarkBorder,
                                        shape = RoundedCornerShape(50)
                                    )
                                    .clickable(role = Role.Tab) {
                                        personCount = count
                                        ensureNameCapacity(count)
                                    }
                                    .semantics(mergeDescendants = true) {
                                        selected = isSelected
                                        contentDescription = "$count people"
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
                                    color = if (isSelected) PureWhite else TextLightPrimary
                                )
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(
                                if (editNamesExpanded) MintSoftContainer else DarkCard
                            )
                            .border(
                                width = 1.dp,
                                color = if (editNamesExpanded) MintBright else DarkBorder,
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
                color = MintSoftContainer,
                border = BorderStroke(1.dp, MintBright.copy(alpha = 0.40f)),
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
                            color = TextLightPrimary
                        )
                        Text(
                            text = "Split evenly among $personCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextLightSecondary
                        )
                    }
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, perPersonShare),
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
                        color = if (isValid) AzureAccent else DarkBorder
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextLightPrimary,
                        disabledContentColor = DarkDisabledContent
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
                        containerColor = AzurePrimaryButton,
                        contentColor = PureWhite,
                        disabledContainerColor = DarkDisabledContainer,
                        disabledContentColor = DarkDisabledContent
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
    currencySymbol: String = "₱",
    onTrySampleSplit: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
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
                color = TextLightPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Enter a group total above to divide shares and log your portion.",
                style = MaterialTheme.typography.bodySmall,
                color = TextLightSecondary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShiftlyPrimaryPillButton(
                text = "Try ${currencySymbol}1,200 ÷ 4 dinner",
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
    val friendParticipants = participants.filter { !it.isCurrentUser }
    val trackedParticipants = friendParticipants.ifEmpty { participants }
    val paidCount = trackedParticipants.count { it.hasPaid }
    val allPaid = trackedParticipants.isNotEmpty() && paidCount == trackedParticipants.size

    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val dateLabel = remember(bill.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(bill.dateEpochDay).format(dateFormatter)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = DarkCard,
        border = BorderStroke(
            width = 1.dp,
            color = if (allPaid) MintBright.copy(alpha = 0.45f) else DarkBorder
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
                            color = TextLightPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (allPaid) MintSoftContainer else AzureSoftContainer
                        ) {
                            Text(
                                text = if (allPaid) "Paid" else "Pending ($paidCount/${trackedParticipants.size})",
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
                        text = "$dateLabel  •  ${ShiftlyFormatters.formatCurrency(currencySymbol, bill.totalAmount)} (${bill.personCount} people)",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = TextLightSecondary
                    )
                }

                IconButton(
                    onClick = { onDeleteBill(bill) },
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Split Bill ${bill.title}",
                        tint = CoralInkOnWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = DarkBorder)

            // Participant Rows with 48dp Min Height Paid / Mark Paid Actions
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { participant ->
                    val displayLabel = if (
                        participant.isCurrentUser &&
                        !participant.personLabel.equals("You", ignoreCase = true)
                    ) {
                        "${participant.personLabel} (You)"
                    } else {
                        participant.personLabel
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurface)
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
                                    .background(if (participant.hasPaid || participant.isCurrentUser) MintSoftContainer else AzureSoftContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Person,
                                    contentDescription = null,
                                    tint = if (participant.hasPaid || participant.isCurrentUser) MintInkOnWhite else AzureInkOnWhite,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = displayLabel,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = TextLightPrimary
                                )
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currencySymbol, participant.amountOwed),
                                    style = MaterialTheme.typography.labelSmall.tabular(),
                                    color = if (participant.hasPaid || participant.isCurrentUser) MintInkOnWhite else TextLightSecondary
                                )
                            }
                        }

                        if (participant.isCurrentUser) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MintSoftContainer,
                                modifier = Modifier.heightIn(min = 36.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.Check,
                                        contentDescription = null,
                                        tint = MintInkOnWhite,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Text(
                                        text = "Your share",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = ShiftlySerif,
                                            fontWeight = FontWeight.SemiBold
                                        ),
                                        color = MintInkOnWhite
                                    )
                                }
                            }
                        } else if (participant.hasPaid) {
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = MintSoftContainer,
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .clip(RoundedCornerShape(50))
                                    .clickable(onClickLabel = "Mark $displayLabel as unpaid") {
                                        onToggleParticipantPaid(participant)
                                    }
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "$displayLabel, paid. Tap to mark unpaid"
                                    }
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
                                modifier = Modifier
                                    .heightIn(min = 48.dp)
                                    .semantics(mergeDescendants = true) {
                                        contentDescription = "Mark $displayLabel as paid"
                                    }
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
                            text = "Logged to Expenses (${ShiftlyFormatters.formatCurrency(currencySymbol, bill.perPersonAmount)})",
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
                        containerColor = AzurePrimaryButton,
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
                        text = "Log Share to Expenses (${ShiftlyFormatters.formatCurrency(currencySymbol, bill.perPersonAmount)})",
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
