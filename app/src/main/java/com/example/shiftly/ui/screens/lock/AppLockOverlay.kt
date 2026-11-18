package com.example.shiftly.ui.screens.lock

import android.content.Context
import android.os.Build
import androidx.annotation.DrawableRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.utils.PinSecurity

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
    val biometricsSupported = remember(context) { canUseBiometrics(context) }
    val supportsBiometric = method != "4-Digit PIN" && biometricsSupported
    val supportsPin = method != "Fingerprint Only" && pinHash.isNotBlank()

    var enteredPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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
        if (enteredPin.length >= 4) return
        errorMessage = null
        val updated = enteredPin + digit
        enteredPin = updated
        if (updated.length == 4) {
            if (PinSecurity.verifyPin(updated, pinHash)) {
                enteredPin = ""
                onUnlocked()
            } else {
                enteredPin = ""
                errorMessage = "Incorrect 4-digit PIN. Try again."
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
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(colors.azureContainer)
                    .border(1.dp, colors.azureAccent.copy(alpha = 0.45f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(
                        if (method == "Fingerprint Only") R.drawable.ic_lucide_fingerprint else R.drawable.ic_lucide_lock
                    ),
                    contentDescription = "Shiftly locked",
                    tint = colors.azureAccent,
                    modifier = Modifier.size(30.dp)
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Shiftly Locked",
                    style = MaterialTheme.typography.headlineSmall.copy(
                        fontFamily = PlusJakartaSans,
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
                    modifier = Modifier.padding(vertical = 8.dp)
                ) {
                    repeat(4) { idx ->
                        val filled = idx < enteredPin.length
                        Box(
                            modifier = Modifier
                                .size(16.dp)
                                .clip(CircleShape)
                                .background(if (filled) colors.mintBright else colors.card)
                                .border(
                                    width = 1.5.dp,
                                    color = if (filled) colors.mintBright else colors.border,
                                    shape = CircleShape
                                )
                        )
                    }
                }
            }

            if (!errorMessage.isNullOrBlank()) {
                Text(
                    text = errorMessage.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.coralTextOnSurface,
                    textAlign = TextAlign.Center
                )
            }

            if (supportsPin) {
                Spacer(modifier = Modifier.height(4.dp))
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("BIO", "0", "DEL")
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    rows.forEach { row ->
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
                OutlinedButton(
                    onClick = { triggerBiometric() },
                    shape = RoundedCornerShape(50),
                    border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_fingerprint),
                        contentDescription = null,
                        tint = colors.azureAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = "Unlock with Biometrics",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold
                        )
                    )
                }
                if (!biometricsSupported && method == "Fingerprint Only") {
                    Text(
                        text = "No biometrics detected—verify using your device screen lock.",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
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
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = "Digit $digit" },
        shape = CircleShape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = digit,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = PlusJakartaSans,
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
    val colors = ShiftlyColors.current
    Surface(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        shape = CircleShape,
        color = colors.card.copy(alpha = 0.6f),
        border = BorderStroke(1.dp, colors.border),
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
