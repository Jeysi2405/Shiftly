package com.example.shiftly.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core iOS Dark Surfaces & Separators
val DarkBackground = Color(0xFF000000)       // Pure black primary canvas
val DarkSurface = Color(0xFF1C1C1E)          // Grouped cards, secondary surfaces, sheets
val DarkCard = Color(0xFF1C1C1E)             // Top-level cards on #000000
val DarkCardElevated = Color(0xFF2C2C2E)     // Nested / elevated elements & cards inside sheets
val DarkSeparator = Color(0xFF38383A)        // 0.5dp hairline dividers
val DarkDisabledContainer = Color(0xFF2C2C2E)
val DarkDisabledContent = Color(0xFF8E8E93)

// Dark Mode Text Hierarchy (Strict 2-tone: Primary #FFFFFF, Secondary #8E8E93)
val TextLightPrimary = Color(0xFFFFFFFF)     // Primary text
val TextLightSecondary = Color(0xFF8E8E93)   // Secondary text
val TextLightMuted = Color(0xFF8E8E93)       // Captions & placeholders (same secondary tone)

// iOS Dark System Accents
val AzurePrimaryButton = Color(0xFF0A84FF)   // Interactive blue
val AzureAccent = Color(0xFF0A84FF)          // Interactive blue
val AzureDarkContainer = Color(0xFF14273D)   // Subtle tinted blue container

// Core Neutrals
val PureWhite = Color(0xFFFFFFFF)

// Green — Income, Positive, Safe Progress
val MintBright = Color(0xFF30D158)
val MintTextOnBlue = Color(0xFF30D158)
val MintInkOnWhite = Color(0xFF30D158)
val MintSoftContainer = Color(0xFF14301D)

// Purple — Night Differential Pay & Hours
val NightDiffPurple = Color(0xFFBF5AF2)
val NightDiffPurpleContainer = Color(0xFF2D1B3B)

// Red — Expenses, Overtime, Destructive
val CoralBright = Color(0xFFFF453A)
val CoralTextOnBlue = Color(0xFFFF453A)
val CoralInkOnWhite = Color(0xFFFF453A)
val CoralSoftContainer = Color(0xFF3A1816)

// Orange — Highlights, Warnings, Secondary Categories
val IosOrange = Color(0xFFFF9F0A)
val IosOrangeContainer = Color(0xFF38260B)

// Azure Accent for Neutral / Split Bill Chips
val AzureInkOnWhite = AzureAccent

// =============================================================================
// Shiftly Light Mode Tokens
// =============================================================================
val LightBackground = Color(0xFFF2F2F7)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFE5E5EA)
val LightBorder = Color(0xFFC6C6C8)
val LightDisabledContainer = Color(0xFFE5E5EA)
val LightDisabledContent = Color(0xFF8E8E93)

val TextDarkPrimary = Color(0xFF000000)
val TextDarkSecondary = Color(0xFF8E8E93)
val TextDarkMuted = Color(0xFF8E8E93)

val AzureAccentLight = Color(0xFF007AFF)
val MintBrightLight = Color(0xFF34C759)
val MintInkLight = Color(0xFF248A3D)
val NightDiffPurpleLight = Color(0xFFAF52DE)
val CoralBrightLight = Color(0xFFFF3B30)
val CoralInkLight = Color(0xFFD70015)
val IosOrangeLight = Color(0xFFFF9500)

val MintSoftContainerLight = Color(0xFFE5F7EA)
val CoralSoftContainerLight = Color(0xFFFFEBEA)
val AzureSoftContainerLight = Color(0xFFE5F1FF)
val NightDiffPurpleContainerLight = Color(0xFFF5E8FD)
val IosOrangeContainerLight = Color(0xFFFFF3E0)

// =============================================================================
// Theme-Reactive Palette
// =============================================================================
@Immutable
data class ShiftlyColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardElevated: Color,
    val border: Color,
    val separator: Color,
    val cardBorder: Color,
    val disabledContainer: Color,
    val disabledContent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val azurePrimary: Color,
    val azureAccent: Color,
    val azureInk: Color,
    val azureContainer: Color,
    val mintBright: Color,
    val mintInk: Color,
    val mintTextOnSurface: Color,
    val mintContainer: Color,
    val nightDiffPurple: Color,
    val nightDiffPurpleContainer: Color,
    val coralBright: Color,
    val coralInk: Color,
    val coralTextOnSurface: Color,
    val coralContainer: Color,
    val iosOrange: Color,
    val orangeContainer: Color,
    val navIndicator: Color,
    val onAzurePrimary: Color = PureWhite,
    val onAccentDark: Color = PureWhite
) {
    companion object {
        val current: ShiftlyColors
            @Composable
            @ReadOnlyComposable
            get() = LocalShiftlyColors.current
    }
}

val DarkShiftlyColors = ShiftlyColors(
    isDark = true,
    background = DarkBackground,
    surface = DarkSurface,
    card = DarkCard,
    cardElevated = DarkCardElevated,
    border = DarkSeparator,
    separator = DarkSeparator,
    cardBorder = Color.Transparent,
    disabledContainer = DarkDisabledContainer,
    disabledContent = DarkDisabledContent,
    textPrimary = TextLightPrimary,
    textSecondary = TextLightSecondary,
    textMuted = TextLightMuted,
    azurePrimary = AzurePrimaryButton,
    azureAccent = AzureAccent,
    azureInk = AzureInkOnWhite,
    azureContainer = AzureDarkContainer,
    mintBright = MintBright,
    mintInk = MintInkOnWhite,
    mintTextOnSurface = MintTextOnBlue,
    mintContainer = MintSoftContainer,
    nightDiffPurple = NightDiffPurple,
    nightDiffPurpleContainer = NightDiffPurpleContainer,
    coralBright = CoralBright,
    coralInk = CoralInkOnWhite,
    coralTextOnSurface = CoralTextOnBlue,
    coralContainer = CoralSoftContainer,
    iosOrange = IosOrange,
    orangeContainer = IosOrangeContainer,
    navIndicator = AzureAccent,
    onAzurePrimary = PureWhite,
    onAccentDark = PureWhite
)

val LightShiftlyColors = ShiftlyColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    card = LightCard,
    cardElevated = LightCardElevated,
    border = LightBorder,
    separator = LightBorder,
    cardBorder = Color.Transparent,
    disabledContainer = LightDisabledContainer,
    disabledContent = LightDisabledContent,
    textPrimary = TextDarkPrimary,
    textSecondary = TextDarkSecondary,
    textMuted = TextDarkMuted,
    azurePrimary = AzureAccentLight,
    azureAccent = AzureAccentLight,
    azureInk = AzureAccentLight,
    azureContainer = AzureSoftContainerLight,
    mintBright = MintBrightLight,
    mintInk = MintInkLight,
    mintTextOnSurface = MintInkLight,
    mintContainer = MintSoftContainerLight,
    nightDiffPurple = NightDiffPurpleLight,
    nightDiffPurpleContainer = NightDiffPurpleContainerLight,
    coralBright = CoralBrightLight,
    coralInk = CoralInkLight,
    coralTextOnSurface = CoralInkLight,
    coralContainer = CoralSoftContainerLight,
    iosOrange = IosOrangeLight,
    orangeContainer = IosOrangeContainerLight,
    navIndicator = AzureAccentLight,
    onAzurePrimary = PureWhite,
    onAccentDark = PureWhite
)

val LocalShiftlyColors = staticCompositionLocalOf { DarkShiftlyColors }
