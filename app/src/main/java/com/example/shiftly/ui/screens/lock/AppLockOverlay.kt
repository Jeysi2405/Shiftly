package com.example.shiftly.ui.screens.lock

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.shiftly.R
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.icons.isSystemAnimationEnabled
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.PinSecurity
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("BIO", "0", "DEL")
)

private const val MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN = 5
private const val COOLDOWN_DURATION_SECONDS = 30

/**
 * Call right before launching an external chooser/document/camera picker so Android's
 * lifecycle transition does not immediately re-lock an already-unlocked session.
 */
val LocalSuppressNextRelock = staticCompositionLocalOf<() -> Unit> { {} }

fun canUseBiometrics(context: Context): Boolean {
    val manager = BiometricManager.from(context)
    val result = manager.canAuthenticate(
        BiometricManager.Authenticators.BIOMETRIC_STRONG or
            BiometricManager.Authenticators.BIOMETRIC_WEAK
    )
    return result == BiometricManager.BIOMETRIC_SUCCESS
}

fun launchBiometricUnlockPrompt(
    activity: FragmentActivity,
    allowDeviceCredentialFallback: Boolean,
    title: String = "Unlock Shiftly",
    subtitle: String = "Verify your identity to view your balances",
    onSuccess: () -> Unit,
    onErrorMessage: (String) -> Unit
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                if (errorCode != BiometricPrompt.ERROR_USER_CANCELED &&
                    errorCode != BiometricPrompt.ERROR_NEGATIVE_BUTTON &&
                    errorCode != BiometricPrompt.ERROR_CANCELED
                ) {
                    onErrorMessage(errString.toString())
                }
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onErrorMessage("Biometric didn't match. Try again.")
            }
        }
    )

    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(title)
        .setSubtitle(subtitle)

    if (allowDeviceCredentialFallback) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            builder.setAllowedAuthenticators(
                BiometricManager.Authenticators.BIOMETRIC_STRONG or
                    BiometricManager.Authenticators.BIOMETRIC_WEAK or
                    BiometricManager.Authenticators.DEVICE_CREDENTIAL
            )
        } else {
            @Suppress("DEPRECATION")
            builder.setDeviceCredentialAllowed(true)
        }
    } else {
        builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        )
        builder.setNegativeButtonText("Use PIN")
    }

    prompt.authenticate(builder.build())
}

@Composable
fun AppLockGateScreen(
    method: String,
    pinHash: String,
    userName: String,
    onUnlocked: () -> Unit,
    onUpgradeLegacyPin: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val biometricsSupported = remember(context) { canUseBiometrics(context) }
    val supportsBiometric = method != "4-Digit PIN" && biometricsSupported
    val supportsPin = method != "Fingerprint Only" && pinHash.isNotBlank()

    val density = LocalDensity.current
    val animationsEnabled = remember(context) { isSystemAnimationEnabled(context) }
    val latestOnUnlocked by rememberUpdatedState(onUnlocked)

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinErrorFlash by remember { mutableStateOf(false) }
    var failedAttempts by rememberSaveable { mutableIntStateOf(0) }
    var cooldownRemainingSeconds by rememberSaveable { mutableIntStateOf(0) }
    val shakeOffsetX = remember { Animatable(0f) }

    var containerOriginInRoot by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var logoBoundsInRoot by remember { mutableStateOf(Rect.Zero) }
    var isUnlockingZoom by remember { mutableStateOf(false) }
    val flightProgress = remember { Animatable(0f) }
    val slowExpandProgress = remember { Animatable(0f) }
    val zoomThroughProgress = remember { Animatable(0f) }

    fun startUnlockTransition() {
        if (isUnlockingZoom) return
        failedAttempts = 0
        cooldownRemainingSeconds = 0
        if (!animationsEnabled || logoBoundsInRoot.isEmpty || containerSize.width <= 0) {
            latestOnUnlocked()
        } else {
            isUnlockingZoom = true
        }
    }

    LaunchedEffect(isUnlockingZoom) {
        if (!isUnlockingZoom) return@LaunchedEffect
        flightProgress.snapTo(0f)
        slowExpandProgress.snapTo(0f)
        zoomThroughProgress.snapTo(0f)
        flightProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 300,
                easing = FastOutSlowInEasing
            )
        )
        slowExpandProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1_100,
                easing = LinearEasing
            )
        )
        zoomThroughProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 600,
                easing = FastOutLinearInEasing
            )
        )
        latestOnUnlocked()
    }

    fun triggerBiometric() {
        if (activity == null || isUnlockingZoom) return
        launchBiometricUnlockPrompt(
            activity = activity,
            allowDeviceCredentialFallback = method == "Fingerprint Only" || !supportsPin,
            onSuccess = { startUnlockTransition() },
            onErrorMessage = { msg -> errorMessage = msg }
        )
    }

    LaunchedEffect(cooldownRemainingSeconds) {
        if (cooldownRemainingSeconds > 0) {
            delay(1_000L)
            cooldownRemainingSeconds = (cooldownRemainingSeconds - 1).coerceAtLeast(0)
            if (cooldownRemainingSeconds == 0) {
                errorMessage = null
            }
        }
    }

    LaunchedEffect(method, pinHash) {
        if (supportsBiometric || (!supportsPin && activity != null)) {
            triggerBiometric()
        }
    }

    fun submitDigit(digit: String) {
        if (isUnlockingZoom || cooldownRemainingSeconds > 0 || isPinErrorFlash || enteredPin.length >= 4) return
        errorMessage = null
        val updated = enteredPin + digit
        enteredPin = updated
        if (updated.length == 4) {
            if (PinSecurity.verifyPin(updated, pinHash)) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                enteredPin = ""
                if (!PinSecurity.isHashed(pinHash)) {
                    onUpgradeLegacyPin?.invoke(updated)
                }
                startUnlockTransition()
            } else {
                isPinErrorFlash = true
                failedAttempts += 1
                val isCooldownTriggered = failedAttempts % MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN == 0
                if (isCooldownTriggered) {
                    cooldownRemainingSeconds = COOLDOWN_DURATION_SECONDS
                    errorMessage = null
                } else {
                    val remainingTries = MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN -
                        (failedAttempts % MAX_PIN_ATTEMPTS_BEFORE_COOLDOWN)
                    errorMessage = "Incorrect 4-digit PIN ($remainingTries ${if (remainingTries == 1) "try" else "tries"} left)."
                }
                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                scope.launch {
                    val steps = listOf(-26f, 26f, -18f, 18f, -10f, 10f, 0f)
                    steps.forEach { target ->
                        shakeOffsetX.animateTo(
                            targetValue = target,
                            animationSpec = tween(durationMillis = 45)
                        )
                    }
                    enteredPin = ""
                    isPinErrorFlash = false
                }
            }
        }
    }

    val activeStatusMessage = when {
        cooldownRemainingSeconds > 0 ->
            "Too many failed attempts. Try again in ${cooldownRemainingSeconds}s."
        else -> errorMessage
    }

    val flightP = flightProgress.value
    val clampedFlightP = flightP.coerceIn(0f, 1f)
    val z = zoomThroughProgress.value.coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .onGloballyPositioned { containerOriginInRoot = it.positionInRoot() }
            .onSizeChanged { containerSize = it }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .then(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && clampedFlightP > 0f) {
                        Modifier.blur((16f * clampedFlightP).dp)
                    } else {
                        Modifier
                    }
                )
                .graphicsLayer {
                    val s = lerp(1f, 0.96f, clampedFlightP)
                    scaleX = s
                    scaleY = s
                    alpha = 1f - clampedFlightP
                }
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_shiftly_logo_mark),
                    contentDescription = "Shiftly locked",
                    modifier = Modifier
                        .size(64.dp)
                        .onGloballyPositioned { logoBoundsInRoot = it.boundsInRoot() }
                        .graphicsLayer { alpha = if (isUnlockingZoom) 0f else 1f }
                )

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Shiftly Locked",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.textPrimary
                )
                Text(
                    text = "Welcome back, $userName. Verify to continue.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }

            if (supportsPin) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .graphicsLayer { translationX = shakeOffsetX.value }
                ) {
                    repeat(4) { idx ->
                        val filled = idx < enteredPin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (filled) 1.2f else 1f,
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "pinDotScale"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isPinErrorFlash -> colors.coralBright
                                filled -> colors.azurePrimary
                                else -> if (colors.isDark) {
                                    colors.cardElevated
                                } else {
                                    colors.textSecondary.copy(alpha = 0.22f)
                                }
                            },
                            animationSpec = tween(durationMillis = 150),
                            label = "pinDotColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(15.dp)
                                .graphicsLayer {
                                    scaleX = dotScale
                                    scaleY = dotScale
                                }
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }
            }

            AnimatedVisibility(
                visible = !activeStatusMessage.isNullOrBlank(),
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(140)) + shrinkVertically()
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.coralContainer,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = activeStatusMessage.orEmpty(),
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ).tabular(),
                        color = colors.coralTextOnSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                    )
                }
            }

            if (supportsPin) {
                Spacer(modifier = Modifier.height(2.dp))
                val keypadEnabled = cooldownRemainingSeconds == 0
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.alpha(if (keypadEnabled) 1f else 0.45f)
                ) {
                    KEYPAD_ROWS.forEach { row ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(20.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            row.forEach { key ->
                                when (key) {
                                    "BIO" -> {
                                        if (supportsBiometric) {
                                            KeypadIconKey(
                                                iconRes = R.drawable.ic_lucide_fingerprint,
                                                description = "Unlock with fingerprint",
                                                tint = colors.azureAccent,
                                                enabled = true,
                                                onClick = { triggerBiometric() }
                                            )
                                        } else {
                                            Spacer(modifier = Modifier.size(72.dp))
                                        }
                                    }

                                    "DEL" -> {
                                        KeypadIconKey(
                                            iconRes = R.drawable.ic_lucide_delete,
                                            description = "Delete digit",
                                            tint = colors.textSecondary,
                                            enabled = keypadEnabled,
                                            onClick = {
                                                errorMessage = null
                                                if (enteredPin.isNotEmpty()) {
                                                    enteredPin = enteredPin.dropLast(1)
                                                }
                                            }
                                        )
                                    }

                                    else -> {
                                        KeypadDigitKey(
                                            digit = key,
                                            enabled = keypadEnabled,
                                            onClick = { submitDigit(key) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                val bioInteraction = remember { MutableInteractionSource() }
                Button(
                    onClick = { triggerBiometric() },
                    interactionSource = bioInteraction,
                    shape = RoundedCornerShape(14.dp),
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
                        .height(50.dp)
                        .shiftlyPressEffect(bioInteraction)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_fingerprint,
                        contentDescription = null,
                        tint = colors.onAzurePrimary,
                        animateOnMount = true,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.onAzurePrimary
                    )
                }
                if (!biometricsSupported && method == "Fingerprint Only") {
                    Text(
                        text = "No biometrics detected—verify using your device screen lock.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        }

        if (isUnlockingZoom) {
            val currentBaseDp = lerp(64.dp, 88.dp, clampedFlightP)
            val currentBasePx = with(density) { currentBaseDp.toPx() }
            val startCenter = if (!logoBoundsInRoot.isEmpty) {
                logoBoundsInRoot.center - containerOriginInRoot
            } else {
                Offset(containerSize.width / 2f, containerSize.height / 2f)
            }
            val visualCenter = Offset(containerSize.width / 2f, containerSize.height / 2f)
            val currentCenter = Offset(
                x = lerp(startCenter.x, visualCenter.x, flightP),
                y = lerp(startCenter.y, visualCenter.y, flightP)
            )
            val currentTopLeft = Offset(
                x = currentCenter.x - currentBasePx / 2f,
                y = currentCenter.y - currentBasePx / 2f
            )
            val slowScale = lerp(1f, 1.55f, slowExpandProgress.value.coerceIn(0f, 1f))
            val zoomScale = slowScale * lerp(1f, 18f, z * z * z)
            val zoomFade = 1f - ((z - 0.60f) / 0.40f).coerceIn(0f, 1f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(isUnlockingZoom) {
                        awaitPointerEventScope {
                            while (true) {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            }
                        }
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_shiftly_logo_mark),
                    contentDescription = "Unlocking Shiftly",
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                x = currentTopLeft.x.roundToInt(),
                                y = currentTopLeft.y.roundToInt()
                            )
                        }
                        .size(currentBaseDp)
                        .graphicsLayer {
                            scaleX = zoomScale
                            scaleY = zoomScale
                            alpha = zoomFade
                        }
                )
            }
        }
    }
}

@Composable
private fun KeypadDigitKey(
    digit: String,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .shiftlyPressClickable(enabled = enabled, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = "Digit $digit" },
        shape = CircleShape,
        color = colors.card,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 24.sp
                ),
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun KeypadIconKey(
    @DrawableRes iconRes: Int,
    description: String,
    tint: Color,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    var tapCount by remember { mutableIntStateOf(0) }
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .shiftlyPressClickable(enabled = enabled, role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                tapCount++
                onClick()
            }
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            ShiftlyAnimatedIcon(
                iconRes = iconRes,
                contentDescription = description,
                tint = tint,
                triggerKey = tapCount,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}

@Composable
fun ShiftlyPostUnlockSkeletonOverlay(
    route: String? = "home",
    modifier: Modifier = Modifier
) {
    ShiftlyScreenSkeletonOverlay(
        route = route,
        includeBottomBar = true,
        modifier = modifier
    )
}

@Composable
fun ShiftlyScreenSkeletonOverlay(
    route: String?,
    includeBottomBar: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val shimmerTransition = rememberInfiniteTransition(label = "screenSkeletonShimmer")
    val shimmerPhase by shimmerTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "screenSkeletonShimmerPhase"
    )
    val baseColor = if (colors.isDark) colors.cardElevated else colors.textSecondary.copy(alpha = 0.14f)
    val highlightColor = if (colors.isDark) Color.White.copy(alpha = 0.10f) else Color.White.copy(alpha = 0.65f)
    val shimmerBrush = Brush.linearGradient(
        colors = listOf(baseColor, highlightColor, baseColor),
        start = Offset((shimmerPhase * 1200f) - 400f, 0f),
        end = Offset(shimmerPhase * 1200f, 400f)
    )
    val cardColor = colors.card
    val resolvedRoute = route ?: "home"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .pointerInput(resolvedRoute) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        when (resolvedRoute) {
            "analytics" -> SkeletonAnalyticsContent(shimmerBrush, cardColor)
            "tools" -> SkeletonToolsContent(shimmerBrush, cardColor)
            "profile" -> SkeletonProfileContent(shimmerBrush, cardColor)
            "duties" -> SkeletonDutiesContent(shimmerBrush, cardColor)
            "expense" -> SkeletonExpenseContent(shimmerBrush, cardColor)
            "split_bill" -> SkeletonSplitBillContent(shimmerBrush, cardColor)
            "recurring_bills" -> SkeletonRecurringBillsContent(shimmerBrush, cardColor)
            "allowance" -> SkeletonAllowanceContent(shimmerBrush, cardColor)
            else -> SkeletonHomeContent(shimmerBrush, cardColor)
        }

        if (includeBottomBar) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.End
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(shimmerBrush)
                )
                Surface(
                    shape = ShiftlyShapes.Pill,
                    color = colors.navBarSurface,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .width(86.dp)
                                .height(36.dp)
                                .clip(ShiftlyShapes.Pill)
                                .background(shimmerBrush)
                        )
                        repeat(3) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(shimmerBrush)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonBar(
    width: Dp? = null,
    height: Dp,
    brush: Brush,
    shape: Shape = ShiftlyShapes.Pill,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .then(if (width != null) Modifier.width(width) else Modifier.fillMaxWidth())
            .height(height)
            .clip(shape)
            .background(brush)
    )
}

@Composable
private fun SkeletonSubScreenTopBar(
    brush: Brush,
    showExtraIcon: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(brush)
        )
        SkeletonBar(width = 112.dp, height = 18.dp, brush = brush)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(brush)
            )
            if (showExtraIcon) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
            SkeletonBar(width = 64.dp, height = 32.dp, brush = brush)
        }
    }
}

@Composable
private fun SkeletonGroupedRowsCard(
    rowCount: Int,
    rowHeight: Dp,
    brush: Brush,
    cardColor: Color
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = cardColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            repeat(rowCount) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(rowHeight)
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(brush)
                        )
                        SkeletonBar(width = 96.dp, height = 15.dp, brush = brush)
                    }
                    SkeletonBar(width = 68.dp, height = 14.dp, brush = brush)
                }
            }
        }
    }
}

@Composable
private fun SkeletonHomeContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 12.dp, bottom = 112.dp)
    ) {
        // HomeGreetingHeader
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                SkeletonBar(width = 196.dp, height = 28.dp, brush = brush, shape = RoundedCornerShape(10.dp))
                SkeletonBar(width = 92.dp, height = 13.dp, brush = brush)
            }
            Box(
                modifier = Modifier
                    .padding(top = 2.dp)
                    .size(48.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
            }
        }

        // HomeHeroPager (216.dp card + pager dots)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(216.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SkeletonBar(width = 104.dp, height = 14.dp, brush = brush)
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(brush)
                            )
                        }
                        SkeletonBar(width = 180.dp, height = 32.dp, brush = brush, shape = RoundedCornerShape(10.dp))
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SkeletonBar(width = 62.dp, height = 11.dp, brush = brush)
                            SkeletonBar(width = 94.dp, height = 18.dp, brush = brush)
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            SkeletonBar(width = 68.dp, height = 11.dp, brush = brush)
                            SkeletonBar(width = 94.dp, height = 18.dp, brush = brush)
                        }
                    }
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBar(width = 18.dp, height = 6.dp, brush = brush)
                SkeletonBar(width = 6.dp, height = 6.dp, brush = brush)
                SkeletonBar(width = 6.dp, height = 6.dp, brush = brush)
            }
        }

        // MonthlyBudgetCard
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                .height(112.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBar(width = 108.dp, height = 14.dp, brush = brush)
                    SkeletonBar(width = 52.dp, height = 24.dp, brush = brush)
                }
                SkeletonBar(height = 8.dp, brush = brush)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBar(width = 92.dp, height = 11.dp, brush = brush)
                    SkeletonBar(width = 56.dp, height = 11.dp, brush = brush)
                }
            }
        }

        // HomeMonthGlanceCard
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 14.dp)
                .height(132.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    SkeletonBar(width = 118.dp, height = 14.dp, brush = brush)
                    SkeletonBar(width = 64.dp, height = 12.dp, brush = brush)
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBar(width = 66.dp, height = 11.dp, brush = brush)
                        SkeletonBar(width = 96.dp, height = 20.dp, brush = brush)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBar(width = 54.dp, height = 11.dp, brush = brush)
                        SkeletonBar(width = 88.dp, height = 20.dp, brush = brush)
                    }
                }
            }
        }

        // HomeRecentActivityHeader + rows
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBar(width = 118.dp, height = 16.dp, brush = brush)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    SkeletonBar(width = 42.dp, height = 26.dp, brush = brush)
                    SkeletonBar(width = 50.dp, height = 26.dp, brush = brush)
                    SkeletonBar(width = 64.dp, height = 26.dp, brush = brush)
                }
            }
            SkeletonGroupedRowsCard(rowCount = 3, rowHeight = 56.dp, brush = brush, cardColor = cardColor)
        }
    }
}

@Composable
private fun SkeletonAnalyticsContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBar(width = 132.dp, height = 30.dp, brush = brush, shape = RoundedCornerShape(10.dp))

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = cardColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                repeat(3) {
                    SkeletonBar(
                        height = 32.dp,
                        brush = brush,
                        shape = RoundedCornerShape(9.dp),
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(236.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBar(width = 76.dp, height = 11.dp, brush = brush)
                        SkeletonBar(width = 108.dp, height = 20.dp, brush = brush)
                    }
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBar(width = 82.dp, height = 11.dp, brush = brush)
                        SkeletonBar(width = 108.dp, height = 20.dp, brush = brush)
                    }
                }
                val barHeights = listOf(58.dp, 88.dp, 70.dp, 96.dp, 50.dp, 82.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    barHeights.forEach { barHeight ->
                        SkeletonBar(
                            width = 20.dp,
                            height = barHeight,
                            brush = brush,
                            shape = RoundedCornerShape(6.dp)
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    SkeletonBar(width = 64.dp, height = 10.dp, brush = brush)
                    SkeletonBar(width = 68.dp, height = 10.dp, brush = brush)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBar(width = 58.dp, height = 12.dp, brush = brush)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(148.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBar(width = 110.dp, height = 14.dp, brush = brush)
                        SkeletonBar(width = 64.dp, height = 14.dp, brush = brush)
                    }
                    SkeletonBar(height = 8.dp, brush = brush)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBar(width = 96.dp, height = 12.dp, brush = brush)
                        SkeletonBar(width = 88.dp, height = 12.dp, brush = brush)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SkeletonBar(width = 68.dp, height = 12.dp, brush = brush)
            SkeletonGroupedRowsCard(rowCount = 3, rowHeight = 52.dp, brush = brush, cardColor = cardColor)
        }
    }
}

@Composable
private fun SkeletonToolsContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        SkeletonBar(width = 92.dp, height = 30.dp, brush = brush, shape = RoundedCornerShape(10.dp))

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBar(width = 64.dp, height = 12.dp, brush = brush)
            SkeletonGroupedRowsCard(rowCount = 5, rowHeight = 56.dp, brush = brush, cardColor = cardColor)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBar(width = 64.dp, height = 12.dp, brush = brush)
            SkeletonGroupedRowsCard(rowCount = 5, rowHeight = 56.dp, brush = brush, cardColor = cardColor)
        }
    }
}

@Composable
private fun SkeletonProfileContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .padding(top = 12.dp, start = 20.dp, end = 20.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        SkeletonBar(width = 104.dp, height = 30.dp, brush = brush, shape = RoundedCornerShape(10.dp))

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(brush)
                )
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    SkeletonBar(width = 132.dp, height = 17.dp, brush = brush)
                    SkeletonBar(width = 96.dp, height = 12.dp, brush = brush)
                    SkeletonBar(width = 76.dp, height = 12.dp, brush = brush)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(16.dp),
            color = cardColor,
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .height(92.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(4) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SkeletonBar(width = 48.dp, height = 18.dp, brush = brush)
                        SkeletonBar(width = 42.dp, height = 10.dp, brush = brush)
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBar(width = 78.dp, height = 12.dp, brush = brush)
            SkeletonGroupedRowsCard(rowCount = 4, rowHeight = 54.dp, brush = brush, cardColor = cardColor)
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBar(width = 62.dp, height = 12.dp, brush = brush)
            SkeletonGroupedRowsCard(rowCount = 2, rowHeight = 54.dp, brush = brush, cardColor = cardColor)
        }
    }
}

@Composable
private fun SkeletonDutiesContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SkeletonSubScreenTopBar(brush = brush, showExtraIcon = true)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .height(156.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBar(width = 118.dp, height = 14.dp, brush = brush)
                    SkeletonBar(width = 164.dp, height = 28.dp, brush = brush, shape = RoundedCornerShape(9.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) {
                            SkeletonBar(
                                height = 28.dp,
                                brush = brush,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(268.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(brush))
                        SkeletonBar(width = 112.dp, height = 16.dp, brush = brush)
                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(brush))
                    }
                    repeat(5) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            repeat(7) {
                                Box(
                                    modifier = Modifier
                                        .size(18.dp)
                                        .clip(CircleShape)
                                        .background(brush)
                                )
                            }
                        }
                    }
                }
            }

            SkeletonBar(
                width = 110.dp,
                height = 16.dp,
                brush = brush,
                modifier = Modifier.padding(bottom = 10.dp)
            )

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(84.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        SkeletonBar(width = 118.dp, height = 13.dp, brush = brush)
                        SkeletonBar(width = 82.dp, height = 22.dp, brush = brush)
                    }
                    Column(
                        horizontalAlignment = Alignment.End,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SkeletonBar(width = 64.dp, height = 11.dp, brush = brush)
                        SkeletonBar(width = 88.dp, height = 18.dp, brush = brush)
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonExpenseContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SkeletonSubScreenTopBar(brush = brush)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            SkeletonBar(
                height = 44.dp,
                brush = brush,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .height(148.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBar(width = 104.dp, height = 13.dp, brush = brush)
                    SkeletonBar(width = 164.dp, height = 28.dp, brush = brush, shape = RoundedCornerShape(9.dp))
                    SkeletonBar(height = 8.dp, brush = brush)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBar(width = 86.dp, height = 12.dp, brush = brush)
                        SkeletonBar(width = 74.dp, height = 12.dp, brush = brush)
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SkeletonBar(width = 62.dp, height = 34.dp, brush = brush)
                SkeletonBar(width = 76.dp, height = 34.dp, brush = brush)
                SkeletonBar(width = 84.dp, height = 34.dp, brush = brush)
                SkeletonBar(width = 68.dp, height = 34.dp, brush = brush)
            }
            SkeletonBar(
                width = 118.dp,
                height = 16.dp,
                brush = brush,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            SkeletonGroupedRowsCard(rowCount = 4, rowHeight = 58.dp, brush = brush, cardColor = cardColor)
        }
    }
}

@Composable
private fun SkeletonSplitBillContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SkeletonSubScreenTopBar(brush = brush)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            SkeletonBar(
                height = 44.dp,
                brush = brush,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            )
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(136.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBar(width = 108.dp, height = 14.dp, brush = brush)
                        SkeletonBar(width = 68.dp, height = 22.dp, brush = brush)
                    }
                    SkeletonBar(width = 154.dp, height = 26.dp, brush = brush, shape = RoundedCornerShape(9.dp))
                    SkeletonBar(height = 8.dp, brush = brush)
                }
            }
            SkeletonBar(
                width = 96.dp,
                height = 16.dp,
                brush = brush,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                repeat(3) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = cardColor,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(92.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                SkeletonBar(width = 128.dp, height = 15.dp, brush = brush)
                                SkeletonBar(width = 88.dp, height = 12.dp, brush = brush)
                            }
                            SkeletonBar(width = 74.dp, height = 18.dp, brush = brush)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SkeletonRecurringBillsContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SkeletonSubScreenTopBar(brush = brush)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(128.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBar(width = 112.dp, height = 13.dp, brush = brush)
                        SkeletonBar(width = 64.dp, height = 22.dp, brush = brush)
                    }
                    SkeletonBar(width = 152.dp, height = 26.dp, brush = brush, shape = RoundedCornerShape(9.dp))
                    SkeletonBar(width = 126.dp, height = 12.dp, brush = brush)
                }
            }
            SkeletonBar(
                width = 124.dp,
                height = 16.dp,
                brush = brush,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            SkeletonGroupedRowsCard(rowCount = 4, rowHeight = 60.dp, brush = brush, cardColor = cardColor)
        }
    }
}

@Composable
private fun SkeletonAllowanceContent(
    brush: Brush,
    cardColor: Color
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        SkeletonSubScreenTopBar(brush = brush)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, start = 20.dp, end = 20.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 14.dp)
                    .height(144.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    SkeletonBar(width = 116.dp, height = 13.dp, brush = brush)
                    SkeletonBar(width = 168.dp, height = 28.dp, brush = brush, shape = RoundedCornerShape(9.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        repeat(3) {
                            SkeletonBar(
                                height = 26.dp,
                                brush = brush,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardColor,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
                    .height(108.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        SkeletonBar(width = 110.dp, height = 14.dp, brush = brush)
                        SkeletonBar(width = 68.dp, height = 24.dp, brush = brush)
                    }
                    SkeletonBar(width = 176.dp, height = 12.dp, brush = brush)
                }
            }
            SkeletonBar(
                width = 132.dp,
                height = 16.dp,
                brush = brush,
                modifier = Modifier.padding(bottom = 10.dp)
            )
            SkeletonGroupedRowsCard(rowCount = 4, rowHeight = 58.dp, brush = brush, cardColor = cardColor)
        }
    }
}
