package com.example.shiftly.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.example.shiftly.R
import com.example.shiftly.ui.components.ShiftlyFeatureGuideSheet
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosSegmentedControl
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.icons.isSystemAnimationEnabled
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.profile.LegalInfoBottomSheet
import com.example.shiftly.ui.screens.profile.LegalTopic
import com.example.shiftly.ui.theme.DarkBackground
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.ShiftlyTheme
import com.example.shiftly.ui.theme.TextLightSecondary
import com.example.shiftly.ui.theme.tabular
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SETUP_CURRENCY_OPTIONS = listOf("₱", "$", "€", "£", "¥")
private val SETUP_CUTOFF_OPTIONS = listOf("15th & 30th", "Monthly", "Weekly")

private data class PendingSetupProfile(
    val name: String,
    val rate: Double,
    val monthlyBudget: Double,
    val currencySymbol: String = "₱",
    val cutoffSchedule: String = "15th & 30th"
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
        currencySymbol: String,
        cutoffSchedule: String
    ) -> Unit = { _, _, _, _, _ -> },
    onGetStartedComplete: (
        name: String,
        rate: Double,
        monthlyBudget: Double
    ) -> Unit
) {
    val colors = ShiftlyColors.current
    var showSetupSheet by rememberSaveable { mutableStateOf(false) }
    var showLegalSheet by rememberSaveable { mutableStateOf(false) }
    var showFeatureGuideSheet by rememberSaveable { mutableStateOf(false) }
    var showIntroSequence by rememberSaveable { mutableStateOf(false) }
    var pendingSetupProfile by remember { mutableStateOf<PendingSetupProfile?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        WelcomeDarkNavyBackdrop()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(12.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.ic_shiftly_logo_mark),
                        contentDescription = "Shiftly logo",
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = "Shiftly",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PureWhite
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = buildAnnotatedString {
                            append("Earn ")
                            withStyle(
                                SpanStyle(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.azurePrimary
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
                            lineHeight = 42.sp
                        ),
                        color = PureWhite
                    )

                    Text(
                        text = "Shifts, cutoffs, expenses, and split bills — organized offline.",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 15.sp,
                            lineHeight = 22.sp
                        ),
                        color = TextLightSecondary
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 24.dp, bottom = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = { showSetupSheet = true },
                    shape = ShiftlyShapes.Control,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.azurePrimary,
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

                Spacer(modifier = Modifier.height(6.dp))

                Box(
                    modifier = Modifier
                        .heightIn(min = 48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showLegalSheet = true }
                        .padding(horizontal = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Terms & Privacy Policy",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        color = TextLightSecondary,
                        textAlign = TextAlign.Center
                    )
                }
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
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        var nameInput by rememberSaveable { mutableStateOf("") }
        var rateInput by rememberSaveable { mutableStateOf("") }
        var budgetInput by rememberSaveable { mutableStateOf("") }
        var selectedCurrency by rememberSaveable { mutableStateOf("₱") }
        var selectedCutoff by rememberSaveable { mutableStateOf("15th & 30th") }
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
                    pendingSetupProfile = PendingSetupProfile(
                        name = cleanName,
                        rate = parsedRate,
                        monthlyBudget = parsedBudget,
                        currencySymbol = selectedCurrency,
                        cutoffSchedule = selectedCutoff
                    )
                    onSaveSetupProfile(
                        cleanName,
                        parsedRate,
                        parsedBudget,
                        selectedCurrency,
                        selectedCutoff
                    )
                    showFeatureGuideSheet = true
                }
            }
        }

        ModalBottomSheet(
            onDismissRequest = { showSetupSheet = false },
            sheetState = sheetState,
            containerColor = colors.surface,
            scrimColor = shiftlySheetScrimColor(),
            tonalElevation = 0.dp,
            shape = ShiftlyShapes.Sheet,
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
                        .heightIn(min = 48.dp),
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

                // Unified iOS Inset-Grouped Form Container
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
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

                        ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                        // Row 2: Hourly Rate
                        TrackerFormInsetRow(
                            label = "Hourly Rate",
                            value = rateInput,
                            onValueChange = {
                                rateInput = filterDecimal(it, rateInput)
                                validationError = null
                            },
                            placeholder = "$selectedCurrency 0.00",
                            prefix = selectedCurrency,
                            isNumeric = true
                        )

                        ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                        // Row 3: Monthly Budget
                        TrackerFormInsetRow(
                            label = "Monthly Budget",
                            value = budgetInput,
                            onValueChange = {
                                budgetInput = filterDecimal(it, budgetInput)
                                validationError = null
                            },
                            placeholder = "$selectedCurrency Optional",
                            prefix = selectedCurrency,
                            isNumeric = true
                        )

                        ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                        // Row 4: Currency Selector (below Monthly Budget)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Currency",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = colors.textPrimary
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .selectableGroup(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                SETUP_CURRENCY_OPTIONS.forEach { symbol ->
                                    val isSelected = selectedCurrency == symbol
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) colors.azurePrimary else colors.surface,
                                        shadowElevation = 0.dp,
                                        modifier = Modifier
                                            .weight(1f)
                                            .heightIn(min = 48.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .shiftlyPressClickable(role = Role.RadioButton) {
                                                selectedCurrency = symbol
                                            }
                                            .semantics { selected = isSelected }
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Text(
                                                text = symbol,
                                                style = MaterialTheme.typography.titleSmall.copy(
                                                    fontFamily = InterFontFamily,
                                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                                    fontSize = 15.sp
                                                ),
                                                color = if (isSelected) PureWhite else colors.textPrimary
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                        // Row 5: Payday Cutoff Schedule (below Currency)
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Payday Cutoff",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 15.sp
                                ),
                                color = colors.textPrimary
                            )
                            ShiftlyIosSegmentedControl(
                                options = SETUP_CUTOFF_OPTIONS,
                                selectedOption = selectedCutoff,
                                onOptionSelected = { selectedCutoff = it },
                                containerColor = colors.surface
                            )
                        }
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
        ShiftlyFeatureGuideSheet(
            primaryButtonText = "Start Exploring",
            onDismiss = {
                showFeatureGuideSheet = false
                showIntroSequence = true
            }
        )
    }

    if (showIntroSequence) {
        val savedProfile = pendingSetupProfile
        WelcomeIntroSequenceOverlay(
            onFinished = {
                showIntroSequence = false
                onGetStartedComplete(
                    savedProfile?.name.orEmpty(),
                    savedProfile?.rate ?: 0.0,
                    savedProfile?.monthlyBudget ?: 0.0
                )
            }
        )
    }
}

@Composable
private fun WelcomeDarkNavyBackdrop(modifier: Modifier = Modifier) {
    val primaryGlow = ShiftlyColors.current.azurePrimary
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val glowRadius = size.maxDimension * 0.65f
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.14f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.22f, size.height * 0.28f),
                    radius = glowRadius
                ),
                radius = glowRadius,
                center = Offset(size.width * 0.22f, size.height * 0.28f)
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        primaryGlow.copy(alpha = 0.08f),
                        Color.Transparent
                    ),
                    center = Offset(size.width * 0.84f, size.height * 0.72f),
                    radius = glowRadius * 0.8f
                ),
                radius = glowRadius * 0.8f,
                center = Offset(size.width * 0.84f, size.height * 0.72f)
            )
        }
    }
}

@Composable
private fun WelcomeIntroSequenceOverlay(
    onFinished: () -> Unit
) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) { isSystemAnimationEnabled(context) }
    val latestOnFinished by rememberUpdatedState(onFinished)

    var stage by remember { mutableIntStateOf(0) }
    val contentAlpha = remember { Animatable(0f) }
    val contentScale = remember { Animatable(0.94f) }
    val zoomProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!animationsEnabled) {
            latestOnFinished()
            return@LaunchedEffect
        }

        // Stage 0: "Welcome" (1.5s)
        stage = 0
        contentAlpha.snapTo(0f)
        contentScale.snapTo(0.94f)
        zoomProgress.snapTo(0f)
        coroutineScope {
            launch { contentAlpha.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
            launch { contentScale.animateTo(1f, tween(260, easing = FastOutSlowInEasing)) }
        }
        delay(1_040L)
        contentAlpha.animateTo(0f, tween(200, easing = FastOutSlowInEasing))

        // Stage 1: "to" (1.0s)
        stage = 1
        contentAlpha.snapTo(0f)
        contentScale.snapTo(0.94f)
        coroutineScope {
            launch { contentAlpha.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
            launch { contentScale.animateTo(1f, tween(200, easing = FastOutSlowInEasing)) }
        }
        delay(620L)
        contentAlpha.animateTo(0f, tween(180, easing = FastOutSlowInEasing))

        // Stage 2: "Shiftly" + Logo (2.0s slow expand) then sudden 1.0s fast zoom across screen
        stage = 2
        contentAlpha.snapTo(0f)
        contentScale.snapTo(1.0f)
        zoomProgress.snapTo(0f)
        coroutineScope {
            launch { contentAlpha.animateTo(1f, tween(240, easing = FastOutSlowInEasing)) }
            launch { contentScale.animateTo(1.55f, tween(durationMillis = 2_000, easing = LinearEasing)) }
        }
        zoomProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1_000, easing = FastOutLinearInEasing)
        )
        latestOnFinished()
    }

    val z = zoomProgress.value.coerceIn(0f, 1f)
    val zoomScale = if (stage == 2) lerp(1f, 18f, z * z * z) else 1f
    val zoomFade = if (stage == 2) 1f - ((z - 0.60f) / 0.40f).coerceIn(0f, 1f) else 1f

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        WelcomeDarkNavyBackdrop()

        Box(
            modifier = Modifier.graphicsLayer {
                val combinedScale = contentScale.value * zoomScale
                scaleX = combinedScale
                scaleY = combinedScale
                alpha = contentAlpha.value * zoomFade
            },
            contentAlignment = Alignment.Center
        ) {
            when (stage) {
                0 -> {
                    Text(
                        text = "Welcome",
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 42.sp,
                            letterSpacing = (-0.5).sp
                        ),
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )
                }
                1 -> {
                    Text(
                        text = "to",
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 34.sp
                        ),
                        color = Color(0xFFB8C7E0),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_shiftly_logo_mark),
                            contentDescription = "Shiftly logo",
                            modifier = Modifier.size(84.dp)
                        )
                        Text(
                            text = "Shiftly",
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 40.sp,
                                letterSpacing = (-0.5).sp
                            ),
                            color = PureWhite,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
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
            .heightIn(min = 52.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
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
                modifier = Modifier.width(150.dp)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 393, heightDp = 852, name = "Welcome Screen")
@Composable
private fun WelcomeScreenPreview() {
    ShiftlyTheme {
        WelcomeScreen(
            onGetStartedComplete = { _, _, _ -> }
        )
    }
}
