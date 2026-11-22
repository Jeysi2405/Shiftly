package com.example.shiftly.ui.screens.lock

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.shiftly.R
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.utils.PinSecurity
import kotlinx.coroutines.launch

private val KEYPAD_ROWS = listOf(
    listOf("1", "2", "3"),
    listOf("4", "5", "6"),
    listOf("7", "8", "9"),
    listOf("BIO", "0", "DEL")
)

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
        .setTitle("Unlock Shiftly")
        .setSubtitle("Verify your identity to view your balances")

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

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isPinErrorFlash by remember { mutableStateOf(false) }
    val shakeOffsetX = remember { Animatable(0f) }

    fun triggerBiometric() {
        if (activity == null) return
        launchBiometricUnlockPrompt(
            activity = activity,
            allowDeviceCredentialFallback = method == "Fingerprint Only" || !supportsPin,
            onSuccess = onUnlocked,
            onErrorMessage = { msg -> errorMessage = msg }
        )
    }

    LaunchedEffect(method, pinHash) {
        if (supportsBiometric || (!supportsPin && activity != null)) {
            triggerBiometric()
        }
    }

    fun submitDigit(digit: String) {
        if (isPinErrorFlash || enteredPin.length >= 4) return
        errorMessage = null
        val updated = enteredPin + digit
        enteredPin = updated
        if (updated.length == 4) {
            if (PinSecurity.verifyPin(updated, pinHash)) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                enteredPin = ""
                onUnlocked()
            } else {
                isPinErrorFlash = true
                errorMessage = "Incorrect 4-digit PIN. Try again."
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

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            ShiftlyIosIconTile(
                iconRes = if (method == "Fingerprint Only") R.drawable.ic_lucide_fingerprint else R.drawable.ic_lucide_lock,
                backgroundColor = colors.azurePrimary,
                size = 48.dp,
                cornerRadius = 14.dp,
                iconSize = 34.dp,
                contentDescription = "Shiftly locked"
            )

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
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
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .graphicsLayer { translationX = shakeOffsetX.value }
                ) {
                    repeat(4) { idx ->
                        val filled = idx < enteredPin.length
                        val dotScale by animateFloatAsState(
                            targetValue = if (filled) 1.18f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.55f,
                                stiffness = Spring.StiffnessMedium
                            ),
                            label = "pinDotScale"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = when {
                                isPinErrorFlash -> colors.coralBright
                                filled -> colors.mintBright
                                else -> colors.cardElevated
                            },
                            animationSpec = tween(durationMillis = 150),
                            label = "pinDotColor"
                        )
                        Box(
                            modifier = Modifier
                                .size(14.dp)
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
                visible = !errorMessage.isNullOrBlank(),
                enter = fadeIn(tween(180)) + expandVertically(),
                exit = fadeOut(tween(140)) + shrinkVertically()
            ) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.coralTextOnSurface,
                    textAlign = TextAlign.Center
                )
            }

            if (supportsPin) {
                Spacer(modifier = Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_fingerprint),
                        contentDescription = null,
                        tint = colors.onAzurePrimary,
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
}

@Composable
private fun KeypadDigitKey(
    digit: String,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .shiftlyPressClickable(role = Role.Button) {
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
    onClick: () -> Unit
) {
    val haptics = LocalHapticFeedback.current
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .shiftlyPressClickable(role = Role.Button) {
                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                onClick()
            }
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = Color.Transparent,
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = description,
                tint = tint,
                modifier = Modifier.size(26.dp)
            )
        }
    }
}
