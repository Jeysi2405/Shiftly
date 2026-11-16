package com.example.shiftly.ui.screens.profile

import android.content.Intent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.ui.components.ShiftlyCard
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.theme.BorderSubtle
import com.example.shiftly.ui.theme.DeepBlueContainer
import com.example.shiftly.ui.theme.ElectricBlue
import com.example.shiftly.ui.theme.EmeraldSuccess
import com.example.shiftly.ui.theme.SlateSurface
import com.example.shiftly.ui.theme.SlateSurfaceVariant
import com.example.shiftly.ui.theme.SoftBlue
import com.example.shiftly.ui.theme.TextPrimary
import com.example.shiftly.ui.theme.TextSecondary
import com.example.shiftly.ui.theme.tabular

@Composable
fun ProfileScreen(
    repository: ShiftlyRepository,
    onShowMessage: (String) -> Unit = {}
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(repository))
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val haptics = LocalHapticFeedback.current

    var rateInput by remember { mutableStateOf("") }
    var savedBanner by remember { mutableStateOf(false) }

    LaunchedEffect(state.defaultHourlyRate) {
        if (rateInput.isEmpty() && state.defaultHourlyRate > 0.0) {
            rateInput = if (state.defaultHourlyRate % 1.0 == 0.0) {
                state.defaultHourlyRate.toInt().toString()
            } else {
                state.defaultHourlyRate.toString()
            }
        }
    }

    val currencies = listOf("₱", "$", "€", "£", "¥")

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        ShiftlyScreenHeader(
            title = "Profile & Settings",
            subtitle = "100% private offline storage on this device"
        )

        // 1. All-Time Offline Snapshot Card
        ShiftlyCard {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "ALL-TIME SNAPSHOT",
                    style = MaterialTheme.typography.labelMedium,
                    color = SoftBlue,
                    fontWeight = FontWeight.SemiBold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SnapshotStatBox(
                        label = "Shifts Logged",
                        value = state.totalShifts.toString(),
                        modifier = Modifier.weight(1f)
                    )
                    SnapshotStatBox(
                        label = "Total Hours",
                        value = "${"%.1f".format(state.totalHoursWorked)}h",
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    SnapshotStatBox(
                        label = "Shift Earnings",
                        value = "${state.currencySymbol}${"%.2f".format(state.totalEarnings)}",
                        modifier = Modifier.weight(1f)
                    )
                    SnapshotStatBox(
                        label = "Tracked Expenses",
                        value = "${state.currencySymbol}${"%.2f".format(state.totalExpenses)}",
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // 2. Shift Rate & Currency Configuration Card
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = null,
                        tint = ElectricBlue
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Shift Rate & Currency",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = TextPrimary
                    )
                }

                Text(
                    text = "Your active rate locks into each shift when you Time-In. Updating it later will never alter past shift earnings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                // Currency Selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    currencies.forEach { symbol ->
                        val selected = state.currencySymbol == symbol
                        FilterChip(
                            selected = selected,
                            onClick = {
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                viewModel.selectCurrency(symbol)
                                onShowMessage("Currency set to $symbol")
                            },
                            label = {
                                Text(
                                    text = symbol,
                                    fontWeight = FontWeight.Bold
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
                }

                // Hourly Rate Input + Save Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = rateInput,
                        onValueChange = {
                            rateInput = it
                            savedBanner = false
                        },
                        label = { Text("Default Hourly Rate (${state.currencySymbol}/hr)") },
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

                    Button(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.saveDefaultRate(rateInput)
                            savedBanner = true
                            onShowMessage("Hourly rate saved")
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = ElectricBlue),
                        modifier = Modifier.height(56.dp)
                    ) {
                        Icon(Icons.Rounded.Check, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Save", fontWeight = FontWeight.SemiBold)
                    }
                }

                if (savedBanner) {
                    Text(
                        text = "Saved! Active rate set to ${state.currencySymbol}${"%.2f".format(state.defaultHourlyRate)}/hr.",
                        style = MaterialTheme.typography.labelMedium,
                        color = EmeraldSuccess
                    )
                }
            }
        }

        // 3. Offline Backup Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SlateSurface,
            border = BorderStroke(1.dp, BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "Offline Data Backup",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
                Text(
                    text = "Export a plain-text snapshot of your shifts and expenses to save locally or share.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
                OutlinedButton(
                    onClick = {
                        viewModel.exportBackup { reportText ->
                            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Shiftly Offline Backup")
                                putExtra(Intent.EXTRA_TEXT, reportText)
                            }
                            context.startActivity(Intent.createChooser(sendIntent, "Export Shiftly Backup"))
                        }
                    },
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.IosShare, contentDescription = null, tint = ElectricBlue)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export Offline Summary", color = ElectricBlue, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun SnapshotStatBox(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SlateSurfaceVariant.copy(alpha = 0.6f),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.tabular(),
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
    }
}