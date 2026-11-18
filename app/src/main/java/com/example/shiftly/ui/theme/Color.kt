package com.example.shiftly.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Core Dark Surfaces & Borders
val DarkBackground = Color(0xFF121212)       // Main screen background
val DarkSurface = Color(0xFF1E1E1E)          // Secondary background, sheets, cards
val DarkCard = Color(0xFF252525)             // Elevated cards, tiles, chips
val DarkCardElevated = Color(0xFF2C2C2C)     // Nested elevated rows / hover-like containers
val DarkBorder = Color(0xFF333333)           // Subtle dark gray borders and dividers
val DarkDisabledContainer = Color(0xFF2C2F36)
val DarkDisabledContent = Color(0xFF8A909A)

// Dark Mode Text Hierarchy
val TextLightPrimary = Color(0xFFF5F5F5)     // Soft white primary text
val TextLightSecondary = Color(0xFFA0A0A0)   // Muted gray secondary text
val TextLightMuted = Color(0xFF9399A2)       // Subtle captions & placeholders (WCAG AA >= 4.5:1 on DarkCard)

// Brand Azure Accents (Preserved & Adapted for Dark Mode)
val AzurePrimaryButton = Color(0xFF2173D6)   // Primary pill CTA button fill
val AzureAccent = Color(0xFF4C9AFF)          // High-contrast azure text/icons/borders on dark
val AzureDarkContainer = Color(0xFF1A2B42)   // Dark tinted azure container
val OrbSkyLight = Color(0xFF68BEFF)

// Core Neutrals (Kept for explicit semantic/foreground or contrast usage)
val PureWhite = Color(0xFFFFFFFF)
val InkBlack = Color(0xFF0B0D12)

// Dark Frosted Bottom Bar Surfaces
val GlassNavTop = Color(0xE024272E)
val GlassNavBottom = Color(0xEB17191E)

// Mint / Aqua Green — Charts, Progress, Income, Success ONLY
val MintBright = Color(0xFF34E5B8)           // Bars, dots, selected nav indicator, progress safe
val MintTextOnBlue = Color(0xFF7CF7D8)       // High-contrast mint on dark
val MintInkOnWhite = Color(0xFF34E5B8)       // High-contrast mint on dark cards
val MintSoftContainer = Color(0xFF132B24)    // Dark tinted mint pill/icon well

// Purple — Night Differential Pay & Hours
val NightDiffPurple = Color(0xFFC084FC)      // High-contrast violet/purple for Night Diff on dark
val NightDiffPurpleContainer = Color(0xFF2B1F3D)

// Soft Coral / Red — Expenses, Overtime Pay/Hours & Near-Limit Budget
val CoralBright = Color(0xFFFF6B6B)          // Bars, dots, warning progress, Overtime red
val CoralTextOnBlue = Color(0xFFFF9E96)      // High-contrast coral on dark
val CoralInkOnWhite = Color(0xFFFF7A7A)      // High-contrast coral on dark cards
val CoralSoftContainer = Color(0xFF331B1D)   // Dark tinted coral pill/icon well

// Azure Accent for Neutral / Split Bill Chips
val AzureInkOnWhite = AzureAccent
val AzureSoftContainer = AzureDarkContainer

// =============================================================================
// Token Aliases (Mapped to Dark Mode Hierarchy)
// =============================================================================
val SlateSurface = DarkCard
val TextPrimary = TextLightPrimary
val TextSecondary = TextLightSecondary
val BorderSubtle = DarkBorder
val RoseAlert = CoralInkOnWhite
val RoseContainer = CoralSoftContainer

// =============================================================================
// Shiftly Light Mode Tokens
// =============================================================================
val LightBackground = Color(0xFFF8F9FA)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightCardElevated = Color(0xFFF1F3F5)
val LightBorder = Color(0xFFE2E8F0)
val LightDisabledContainer = Color(0xFFE5E7EB)
val LightDisabledContent = Color(0xFF6B7280)

val TextDarkPrimary = Color(0xFF111827)
val TextDarkSecondary = Color(0xFF4B5563)
val TextDarkMuted = Color(0xFF6B7280)

val AzureAccentLight = Color(0xFF1D63C4)
val MintBrightLight = Color(0xFF059669)
val MintInkLight = Color(0xFF047857)
val NightDiffPurpleLight = Color(0xFF7E22CE)
val CoralBrightLight = Color(0xFFDC2626)
val CoralInkLight = Color(0xFFB91C1C)

val MintSoftContainerLight = Color(0xFFE6F4EA)
val CoralSoftContainerLight = Color(0xFFFCE8E6)
val AzureSoftContainerLight = Color(0xFFE8F0FE)
val NightDiffPurpleContainerLight = Color(0xFFF3E8FF)

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
    val navIndicator: Color,
    val onAzurePrimary: Color = PureWhite,
    val onAccentDark: Color
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
    border = DarkBorder,
    cardBorder = Color.White.copy(alpha = 0.06f),
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
    navIndicator = MintBright,
    onAzurePrimary = PureWhite,
    onAccentDark = InkBlack
)

val LightShiftlyColors = ShiftlyColors(
    isDark = false,
    background = LightBackground,
    surface = LightSurface,
    card = LightCard,
    cardElevated = LightCardElevated,
    border = LightBorder,
    cardBorder = LightBorder,
    disabledContainer = LightDisabledContainer,
    disabledContent = LightDisabledContent,
    textPrimary = TextDarkPrimary,
    textSecondary = TextDarkSecondary,
    textMuted = TextDarkMuted,
    azurePrimary = AzurePrimaryButton,
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
    navIndicator = AzurePrimaryButton,
    onAzurePrimary = PureWhite,
    onAccentDark = PureWhite
)

val LocalShiftlyColors = staticCompositionLocalOf { DarkShiftlyColors }
