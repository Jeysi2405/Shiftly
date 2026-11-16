package com.example.shiftly.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.AzureAccent
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzurePrimaryButton
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.DarkBackground
import com.example.shiftly.ui.theme.DarkBorder
import com.example.shiftly.ui.theme.DarkCard
import com.example.shiftly.ui.theme.DarkSurface
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.MintInkOnWhite
import com.example.shiftly.ui.theme.MintSoftContainer
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightPrimary
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onGetStartedComplete: (name: String, rate: Double, seedSample: Boolean) -> Unit,
    onExistingAccount: () -> Unit,
) {
    var showSetupSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(AzurePrimaryButton),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₱",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold
                        ),
                        color = PureWhite
                    )
                }
                Text(
                    text = "Shiftly",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = TextLightPrimary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("Track every ")
                        withStyle(
                            SpanStyle(
                                fontFamily = ShiftlySerif,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = AzureAccent
                            )
                        ) {
                            append("earned")
                        }
                        append("\npeso between classes.")
                    },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        lineHeight = 43.sp
                    ),
                    color = TextLightPrimary
                )

                Text(
                    text = "Log duty shifts in seconds, watch your pay add up automatically, and keep campus expenses and split bills effortless.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = TextLightSecondary
                )

                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    WelcomeFeaturePill(
                        icon = Icons.Outlined.Schedule,
                        label = "Log shifts",
                        containerColor = MintSoftContainer,
                        accentColor = MintInkOnWhite
                    )
                    WelcomeFeaturePill(
                        icon = Icons.AutoMirrored.Outlined.ReceiptLong,
                        label = "Track expenses",
                        containerColor = CoralSoftContainer,
                        accentColor = CoralInkOnWhite
                    )
                    WelcomeFeaturePill(
                        icon = Icons.AutoMirrored.Outlined.CallSplit,
                        label = "Split bills",
                        containerColor = AzureSoftContainer,
                        accentColor = AzureInkOnWhite
                    )
                }

                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = DarkSurface,
                    border = BorderStroke(1.dp, DarkBorder),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Typical student shift",
                                style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                                color = TextLightSecondary
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "4.0 hrs × ₱96.38/hr",
                                style = MaterialTheme.typography.bodyMedium.tabular(),
                                color = TextLightPrimary
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = MintSoftContainer
                        ) {
                            Text(
                                text = "+₱385.52",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = MintInkOnWhite,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ShiftlyPrimaryPillButton(
                    text = "Get Started",
                    onClick = { showSetupSheet = true }
                )

                TextButton(
                    onClick = onExistingAccount,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                ) {
                    Text(
                        text = "I Already Have an Account",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.Medium
                        ),
                        color = TextLightSecondary
                    )
                }
            }
        }
    }

    if (showSetupSheet) {
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var nameInput by remember { mutableStateOf("Maya") }
        var rateInput by remember { mutableStateOf("96.38") }
        var seedSample by remember { mutableStateOf(true) }

        ModalBottomSheet(
            onDismissRequest = { showSetupSheet = false },
            sheetState = sheetState,
            containerColor = DarkSurface,
            shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 28.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Set up your tracker",
                    style = MaterialTheme.typography.headlineSmall,
                    color = TextLightPrimary
                )
                Text(
                    text = "You can adjust your name, hourly rate, and monthly budget anytime in Profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextLightSecondary
                )

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = { nameInput = it },
                    label = { Text("First Name", fontFamily = ShiftlySerif) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzureAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AzureAccent,
                        unfocusedLabelColor = TextLightSecondary,
                        focusedTextColor = TextLightPrimary,
                        unfocusedTextColor = TextLightPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = { rateInput = it },
                    label = { Text("Hourly Rate (₱)", fontFamily = ShiftlySerif) },
                    prefix = { Text("₱", fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AzureAccent,
                        unfocusedBorderColor = DarkBorder,
                        focusedLabelColor = AzureAccent,
                        unfocusedLabelColor = TextLightSecondary,
                        focusedTextColor = TextLightPrimary,
                        unfocusedTextColor = TextLightPrimary
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DarkCard,
                    border = BorderStroke(1.dp, DarkBorder),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .clickable { seedSample = !seedSample }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Include sample week",
                                style = MaterialTheme.typography.titleSmall,
                                color = TextLightPrimary
                            )
                            Text(
                                text = "Preloads shifts, snacks, fare & split bills",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextLightSecondary
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = seedSample,
                            onCheckedChange = { seedSample = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = InkBlack,
                                checkedTrackColor = MintBright,
                                uncheckedThumbColor = TextLightSecondary,
                                uncheckedTrackColor = DarkSurface,
                                uncheckedBorderColor = DarkBorder
                            )
                        )
                    }
                }

                ShiftlyPrimaryPillButton(
                    text = "Continue to Home",
                    onClick = {
                        val parsedRate = rateInput.toDoubleOrNull()?.coerceAtLeast(0.0) ?: 96.38
                        val finalName = nameInput.trim().ifEmpty { "Maya" }
                        showSetupSheet = false
                        onGetStartedComplete(finalName, parsedRate, seedSample)
                    }
                )
            }
        }
    }
}

@Composable
private fun WelcomeFeaturePill(
    icon: ImageVector,
    label: String,
    containerColor: Color,
    accentColor: Color
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = DarkCard,
        border = BorderStroke(1.dp, DarkBorder),
        modifier = Modifier
            .heightIn(min = 48.dp)
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(50),
                ambientColor = InkBlack.copy(alpha = 0.45f),
                spotColor = InkBlack.copy(alpha = 0.50f)
            )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(26.dp)
                    .clip(CircleShape)
                    .background(containerColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(15.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.Medium
                ),
                color = TextLightPrimary
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Welcome Screen")
@Composable
private fun WelcomeScreenPreview() {
    ShiftlyTheme {
        WelcomeScreen(
            onGetStartedComplete = { _, _, _ -> },
            onExistingAccount = {},
        )
    }
}
