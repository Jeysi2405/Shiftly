package com.example.shiftly.ui.screens.splitbill

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.DeepBlueContainer
import com.example.shiftly.ui.theme.ElectricBlue
import com.example.shiftly.ui.theme.EmeraldSuccess
import com.example.shiftly.ui.theme.RoseAlert
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SlateSurfaceVariant
import com.example.shiftly.ui.theme.SoftBlue
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary
import com.example.shiftly.ui.theme.tabular
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun SplitBillSummaryCard(
    currencySymbol: String,
    totalPendingAmount: Double,
    totalCollectedAmount: Double,
    paidParticipantsCount: Int,
    totalParticipantsCount: Int
) {
    val paidFraction = if (totalParticipantsCount > 0) {
        (paidParticipantsCount.toFloat() / totalParticipantsCount.toFloat()).coerceIn(0f, 1f)
    } else {
        0f
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "PENDING",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftBlue
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%.2f".format(totalPendingAmount)}",
                        style = MaterialTheme.typography.headlineMedium.tabular(),
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SlateSurfaceVariant
                ) {
                    Text(
                        text = "Paid: $currencySymbol${"%.2f".format(totalCollectedAmount)}",
                        style = MaterialTheme.typography.labelSmall.tabular(),
                        color = EmeraldSuccess,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                    )
                }
            }

            // Paid vs. Unpaid Progress Bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(SlateSurfaceVariant)
            ) {
                if (paidFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(paidFraction)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(6.dp))
                            .background(EmeraldSuccess)
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
    ) -> Unit
) {
    var titleInput by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var personCount by remember { mutableIntStateOf(2) }
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
    val isValid = totalAmount > 0.0 && personCount >= 2
    val quickCounts = listOf(2, 3, 4, 5, 6)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "NEW SPLIT",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = SoftBlue
            )

            // Total Bill Amount + Title Inputs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = amountInput,
                    onValueChange = { amountInput = it },
                    label = { Text("Total ($currencySymbol)") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = SoftBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )

                OutlinedTextField(
                    value = titleInput,
                    onValueChange = { titleInput = it },
                    label = { Text("Label") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricBlue,
                        unfocusedBorderColor = BorderSubtle,
                        focusedLabelColor = SoftBlue,
                        unfocusedLabelColor = TextSecondary,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.weight(1.15f)
                )
            }

            // Number of Persons Included Selector
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Persons",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )

                    // - / Count / + Stepper
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SlateSurfaceVariant,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(enabled = personCount > 2) {
                                    if (personCount > 2) personCount--
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Remove,
                                    contentDescription = "Decrease",
                                    tint = if (personCount > 2) TextPrimary else TextSecondary.copy(alpha = 0.4f),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Text(
                            text = "$personCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = ElectricBlue,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )

                        Surface(
                            shape = CircleShape,
                            color = DeepBlueContainer,
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .clickable(enabled = personCount < 20) {
                                    if (personCount < 20) {
                                        personCount++
                                        ensureNameCapacity(personCount)
                                    }
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = "Increase",
                                    tint = ElectricBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                // Quick-Pick Person Count Chips (2..6)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    quickCounts.forEach { count ->
                        val selected = personCount == count
                        FilterChip(
                            selected = selected,
                            onClick = {
                                personCount = count
                                ensureNameCapacity(count)
                            },
                            label = {
                                Text(
                                    text = "${count}p",
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                containerColor = SlateSurfaceVariant,
                                labelColor = TextSecondary,
                                selectedContainerColor = DeepBlueContainer,
                                selectedLabelColor = ElectricBlue
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = selected,
                                borderColor = BorderSubtle,
                                selectedBorderColor = ElectricBlue
                            )
                        )
                    }

                    TextButton(
                        onClick = {
                            ensureNameCapacity(personCount)
                            editNamesExpanded = !editNamesExpanded
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = null,
                            tint = SoftBlue,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (editNamesExpanded) "Hide" else "Names",
                            style = MaterialTheme.typography.labelMedium,
                            color = SoftBlue
                        )
                    }
                }
            }

            // Optional Inline Participant Name Editor
            if (editNamesExpanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    for (index in 0 until personCount) {
                        OutlinedTextField(
                            value = customNames.getOrElse(index) { "Person ${index + 1}" },
                            onValueChange = { updated ->
                                ensureNameCapacity(index + 1)
                                customNames[index] = updated
                            },
                            label = { Text(if (index == 0) "Your Name" else "Person ${index + 1}") },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = ElectricBlue,
                                unfocusedBorderColor = BorderSubtle,
                                focusedLabelColor = SoftBlue,
                                unfocusedLabelColor = TextSecondary,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Live Per-Person Calculation Banner
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = DeepBlueContainer.copy(alpha = 0.7f),
                border = BorderStroke(1.dp, ElectricBlue.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Per Person",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = SoftBlue
                    )
                    Text(
                        text = "$currencySymbol${"%.2f".format(perPersonShare)}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }
            }

            // Action Buttons: Save OR Split & Log
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
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (isValid) ElectricBlue else BorderSubtle),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Save",
                        color = if (isValid) ElectricBlue else TextSecondary,
                        fontWeight = FontWeight.SemiBold
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
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        disabledContainerColor = SlateSurfaceVariant
                    ),
                    modifier = Modifier
                        .weight(1.25f)
                        .height(48.dp)
                ) {
                    Text(
                        text = "Split & Log",
                        fontWeight = FontWeight.Bold
                    )
                }
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
    onDeleteBill: (SplitBillEntity) -> Unit
) {
    val bill = item.bill
    val participants = item.participants
    val paidCount = participants.count { it.hasPaid }
    val allPaid = participants.isNotEmpty() && paidCount == participants.size

    val dateFormatter = remember { DateTimeFormatter.ofPattern("MMM d") }
    val dateLabel = remember(bill.dateEpochDay) {
        LocalDate.ofEpochDay(bill.dateEpochDay).format(dateFormatter)
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SlateSurface,
        border = BorderStroke(1.dp, if (allPaid) EmeraldSuccess.copy(alpha = 0.5f) else BorderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Top Bill Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bill.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (allPaid) {
                                EmeraldSuccess.copy(alpha = 0.16f)
                            } else {
                                DeepBlueContainer
                            }
                        ) {
                            Text(
                                text = if (allPaid) "Paid" else "$paidCount/${participants.size}",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (allPaid) EmeraldSuccess else SoftBlue,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "$dateLabel  •  $currencySymbol${"%.2f".format(bill.totalAmount)}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = TextSecondary
                    )
                }

                IconButton(
                    onClick = { onDeleteBill(bill) },
                    modifier = Modifier.size(30.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete",
                        tint = RoseAlert.copy(alpha = 0.85f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            HorizontalDivider(thickness = 1.dp, color = BorderSubtle)

            // Participant Rows with Individual "Paid" Toggle Buttons
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                participants.forEach { participant ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(SlateSurfaceVariant.copy(alpha = 0.4f))
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Person,
                                contentDescription = null,
                                tint = if (participant.hasPaid) EmeraldSuccess else SoftBlue,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = participant.personLabel,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = "$currencySymbol${"%.2f".format(participant.amountOwed)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (participant.hasPaid) EmeraldSuccess else TextSecondary
                                )
                            }
                        }

                        // Person Paid Toggle Button
                        if (participant.hasPaid) {
                            Button(
                                onClick = { onToggleParticipantPaid(participant) },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = EmeraldSuccess.copy(alpha = 0.2f),
                                    contentColor = EmeraldSuccess
                                ),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Paid",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { onToggleParticipantPaid(participant) },
                                shape = RoundedCornerShape(8.dp),
                                border = BorderStroke(1.dp, ElectricBlue),
                                modifier = Modifier.height(34.dp)
                            ) {
                                Text(
                                    text = "Mark Paid",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = ElectricBlue
                                )
                            }
                        }
                    }
                }
            }

            // One-Tap "Log Share as Expense" Action Button
            if (bill.isLoggedToExpense) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = EmeraldSuccess.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            tint = EmeraldSuccess,
                            modifier = Modifier.size(17.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Logged to Expenses ($currencySymbol${"%.2f".format(bill.perPersonAmount)})",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = EmeraldSuccess
                        )
                    }
                }
            } else {
                Button(
                    onClick = { onLogAsExpense(bill) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = DeepBlueContainer),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                        contentDescription = null,
                        tint = ElectricBlue,
                        modifier = Modifier.size(17.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Log Share ($currencySymbol${"%.2f".format(bill.perPersonAmount)})",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = ElectricBlue
                    )
                }
            }
        }
    }
}
