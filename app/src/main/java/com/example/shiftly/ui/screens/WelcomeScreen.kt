package com.example.shiftly.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.shiftly.R
import com.example.shiftly.data.local.ProfilePhotoHelper
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private fun filterDecimal(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onGetStartedComplete: (
        name: String,
        rate: Double,
        monthlyBudget: Double,
        profilePhotoPath: String?
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    var showSetupSheet by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
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
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.azurePrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "₱",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.onAzurePrimary
                    )
                }
                Text(
                    text = "Shiftly",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("Track every ")
                        withStyle(
                            SpanStyle(
                                fontFamily = InterFontFamily,
                                fontStyle = FontStyle.Italic,
                                fontWeight = FontWeight.Medium,
                                color = colors.azureAccent
                            )
                        ) {
                            append("earned")
                        }
                        append("\npeso between classes.")
                    },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        lineHeight = 43.sp
                    ),
                    color = colors.textPrimary
                )

                Text(
                    text = "Log duty shifts in seconds, watch your pay add up automatically, and keep campus expenses and split bills effortless.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                ShiftlyPrimaryPillButton(
                    text = "Get Started",
                    onClick = { showSetupSheet = true }
                )
            }
        }
    }

    if (showSetupSheet) {
        val context = LocalContext.current
        val coroutineScope = rememberCoroutineScope()
        val suppressNextRelock = LocalSuppressNextRelock.current
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var nameInput by remember { mutableStateOf("") }
        var rateInput by remember { mutableStateOf("") }
        var budgetInput by remember { mutableStateOf("") }
        var photoPath by remember { mutableStateOf<String?>(null) }
        var validationError by remember { mutableStateOf<String?>(null) }

        val submitSetup: () -> Unit = {
            val cleanName = nameInput.trim()
            val parsedRate = rateInput.toDoubleOrNull()
            val parsedBudget = if (budgetInput.isBlank()) {
                0.0
            } else {
                budgetInput.toDoubleOrNull()
            }

            when {
                cleanName.isEmpty() -> {
                    validationError = "Please enter your display name."
                }
                parsedRate == null || !parsedRate.isFinite() || parsedRate <= 0.0 -> {
                    validationError = "Please enter a valid hourly rate above 0."
                }
                parsedBudget == null || !parsedBudget.isFinite() || parsedBudget < 0.0 -> {
                    validationError = "Please enter a valid monthly budget."
                }
                else -> {
                    showSetupSheet = false
                    onGetStartedComplete(cleanName, parsedRate, parsedBudget, photoPath)
                }
            }
        }

        val photoPicker = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.PickVisualMedia()
        ) { uri ->
            if (uri != null) {
                coroutineScope.launch {
                    val saved = withContext(Dispatchers.IO) {
                        ProfilePhotoHelper.saveProfilePhotoFromUri(context, uri)
                    }
                    if (saved != null) {
                        photoPath = saved
                    }
                }
            }
        }

        val photoFile by produceState<File?>(initialValue = null, photoPath) {
            value = withContext(Dispatchers.IO) {
                photoPath?.let(::File)?.takeIf { it.exists() && it.isFile }
            }
        }
        val avatarSizePx = with(LocalDensity.current) { 68.dp.roundToPx() }
        val avatarRequest = remember(photoFile, avatarSizePx, context) {
            photoFile?.let { file ->
                ImageRequest.Builder(context)
                    .data(file)
                    .size(avatarSizePx, avatarSizePx)
                    .scale(Scale.FILL)
                    .crossfade(true)
                    .build()
            }
        }

        val fieldColors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.azureAccent,
            unfocusedBorderColor = Color.Transparent,
            focusedLabelColor = colors.azureAccent,
            unfocusedLabelColor = colors.textSecondary,
            focusedTextColor = colors.textPrimary,
            unfocusedTextColor = colors.textPrimary,
            focusedContainerColor = colors.cardElevated,
            unfocusedContainerColor = colors.cardElevated
        )

        ModalBottomSheet(
            onDismissRequest = { showSetupSheet = false },
            sheetState = sheetState,
            containerColor = colors.surface,
            scrimColor = shiftlySheetScrimColor(),
            tonalElevation = 0.dp,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
            dragHandle = { ShiftlySheetDragHandle() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .imePadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                ShiftlySheetHeader(
                    title = "Set Up Tracker",
                    onCancel = { showSetupSheet = false },
                    actionLabel = "Save",
                    onAction = submitSetup
                )
                Text(
                    text = "You can adjust your name, hourly rate, and monthly budget anytime in Profile.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                // Profile Photo Picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(colors.cardElevated)
                            .clickable {
                                suppressNextRelock()
                                photoPicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (avatarRequest != null) {
                            AsyncImage(
                                model = avatarRequest,
                                contentDescription = "Selected profile photo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_camera),
                                contentDescription = "Add profile photo",
                                tint = colors.azureAccent,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (photoPath != null) "Profile photo added" else "Add profile photo (optional)",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    suppressNextRelock()
                                    photoPicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                    )
                                }
                            ) {
                                Text(
                                    text = if (photoPath != null) "Change photo" else "Choose photo",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily
                                    ),
                                    color = colors.azureAccent
                                )
                            }
                            if (photoPath != null) {
                                TextButton(
                                    onClick = {
                                        val oldPath = photoPath
                                        photoPath = null
                                        coroutineScope.launch(Dispatchers.IO) {
                                            ProfilePhotoHelper.deleteProfilePhoto(oldPath)
                                        }
                                    }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_x),
                                        contentDescription = null,
                                        tint = colors.coralTextOnSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.size(4.dp))
                                    Text(
                                        text = "Remove",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = InterFontFamily
                                        ),
                                        color = colors.coralTextOnSurface
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = nameInput,
                    onValueChange = {
                        nameInput = it
                        validationError = null
                    },
                    label = { Text("Display Name", fontFamily = InterFontFamily) },
                    placeholder = {
                        Text(
                            text = "What I gonna call you?",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = rateInput,
                    onValueChange = {
                        rateInput = filterDecimal(it, rateInput)
                        validationError = null
                    },
                    label = { Text("Hourly Rate (₱)", fontFamily = InterFontFamily) },
                    placeholder = {
                        Text(
                            text = "What is your hourly rate?",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                    },
                    prefix = {
                        Text(
                            text = "₱",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = budgetInput,
                    onValueChange = {
                        budgetInput = filterDecimal(it, budgetInput)
                        validationError = null
                    },
                    label = { Text("Monthly Budget (₱)", fontFamily = InterFontFamily) },
                    placeholder = {
                        Text(
                            text = "What is your monthly budget?",
                            style = MaterialTheme.typography.bodyLarge.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary
                        )
                    },
                    prefix = {
                        Text(
                            text = "₱",
                            style = MaterialTheme.typography.bodyLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary
                        )
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    colors = fieldColors,
                    modifier = Modifier.fillMaxWidth()
                )

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.coralTextOnSurface
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Welcome Screen")
@Composable
private fun WelcomeScreenPreview() {
    ShiftlyTheme {
        WelcomeScreen(
            onGetStartedComplete = { _, _, _, _ -> }
        )
    }
}
