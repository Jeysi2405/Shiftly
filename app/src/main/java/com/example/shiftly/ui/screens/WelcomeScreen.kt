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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.shiftly.R
import com.example.shiftly.data.local.ProfilePhotoHelper
import com.example.shiftly.ui.components.ShiftlyFeatureGuideSheet
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.screens.profile.LegalInfoBottomSheet
import com.example.shiftly.ui.screens.profile.LegalTopic
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.tabular
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ObsidianBackground = Color(0xFF0B0F17)
private val ElectricBlue = Color(0xFF3B82F6)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextSecondary = Color(0xFF94A3B8)

private data class PendingSetupProfile(
    val name: String,
    val rate: Double,
    val monthlyBudget: Double,
    val profilePhotoPath: String?
)

private fun filterDecimal(raw: String, current: String): String {
    val filtered = raw.filter { it.isDigit() || it == '.' }
    return if (filtered.count { it == '.' } <= 1) filtered else current
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelcomeScreen(
    onSaveSetupProfile: (
        name: String,
        rate: Double,
        monthlyBudget: Double,
        profilePhotoPath: String?
    ) -> Unit = { _, _, _, _ -> },
    onGetStartedComplete: (
        name: String,
        rate: Double,
        monthlyBudget: Double,
        profilePhotoPath: String?
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    var showSetupSheet by rememberSaveable { mutableStateOf(false) }
    var showLegalSheet by rememberSaveable { mutableStateOf(false) }
    var showFeatureGuideSheet by rememberSaveable { mutableStateOf(false) }
    var pendingSetupProfile by remember { mutableStateOf<PendingSetupProfile?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(44.dp))

            Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                Text(
                    text = buildAnnotatedString {
                        append("Earn ")
                        withStyle(
                            SpanStyle(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = ElectricBlue
                            )
                        ) {
                            append("smarter,")
                        }
                        append("\nspend clearer.")
                    },
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp,
                        lineHeight = 43.sp
                    ),
                    color = TextPrimary
                )

                Text(
                    text = "Log your shifts, track your spend, and split the bill.",
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        lineHeight = 24.sp
                    ),
                    color = TextSecondary
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showSetupSheet = true },
                    shape = RoundedCornerShape(50),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = ElectricBlue,
                        contentColor = PureWhite
                    ),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text(
                        text = "Get Started",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 17.sp
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "By continuing, you agree to our Terms & Privacy Policy",
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    ),
                    color = Color(0xFF8E8E93),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLegalSheet = true }
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }

    if (showLegalSheet) {
        LegalInfoBottomSheet(
            topic = LegalTopic.TERMS_AND_PRIVACY,
            onDismiss = { showLegalSheet = false }
        )
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
                    pendingSetupProfile = PendingSetupProfile(cleanName, parsedRate, parsedBudget, photoPath)
                    onSaveSetupProfile(cleanName, parsedRate, parsedBudget, photoPath)
                    showFeatureGuideSheet = true
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
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 44.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Set Up Tracker",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        ),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }

                // Clean Avatar Picker Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
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
                                contentDescription = "Choose photo",
                                tint = colors.azureAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

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
                                text = if (photoPath != null) "Change Photo" else "Choose Photo",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
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
                                Text(
                                    text = "Remove",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.coralTextOnSurface
                                )
                            }
                        }
                    }
                }

                // Unified iOS Inset-Grouped Form Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        // Row 1: Display Name
                        TrackerFormInsetRow(
                            label = "Display Name",
                            value = nameInput,
                            onValueChange = {
                                nameInput = it
                                validationError = null
                            },
                            placeholder = "Your name"
                        )
                        ShiftlyHairlineDivider(startInset = 16.dp)

                        // Row 2: Hourly Rate
                        TrackerFormInsetRow(
                            label = "Hourly Rate",
                            value = rateInput,
                            onValueChange = {
                                rateInput = filterDecimal(it, rateInput)
                                validationError = null
                            },
                            placeholder = "₱ 0.00",
                            prefix = "₱",
                            isNumeric = true
                        )
                        ShiftlyHairlineDivider(startInset = 16.dp)

                        // Row 3: Monthly Budget
                        TrackerFormInsetRow(
                            label = "Monthly Budget",
                            value = budgetInput,
                            onValueChange = {
                                budgetInput = filterDecimal(it, budgetInput)
                                validationError = null
                            },
                            placeholder = "₱ Optional",
                            prefix = "₱",
                            isNumeric = true
                        )
                    }
                }

                if (!validationError.isNullOrBlank()) {
                    Text(
                        text = validationError.orEmpty(),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.coralTextOnSurface
                    )
                }

                ShiftlySheetBottomButtons(
                    onCancel = { showSetupSheet = false },
                    onAction = submitSetup,
                    actionLabel = "Save"
                )
            }
        }
    }

    if (showFeatureGuideSheet) {
        val savedProfile = pendingSetupProfile
        ShiftlyFeatureGuideSheet(
            primaryButtonText = "Start Exploring",
            onDismiss = {
                showFeatureGuideSheet = false
                onGetStartedComplete(
                    savedProfile?.name.orEmpty(),
                    savedProfile?.rate ?: 0.0,
                    savedProfile?.monthlyBudget ?: 0.0,
                    savedProfile?.profilePhotoPath
                )
            }
        )
    }
}

@Composable
private fun TrackerFormInsetRow(
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
            color = colors.textPrimary
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
                modifier = Modifier.width(160.dp)
            )
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
