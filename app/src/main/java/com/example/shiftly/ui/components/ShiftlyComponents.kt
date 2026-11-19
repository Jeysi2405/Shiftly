package com.example.shiftly.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.shiftly.R
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val PillShape = RoundedCornerShape(50)
private val SheetTopShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)

/**
 * Top cap modifier for unrolled LazyColumn sheet bands: rounds the top 28dp corners
 * and fills with [surfaceColor] with zero border strokes.
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.shiftlySheetTopBand(
    surfaceColor: Color,
    borderColor: Color = Color.Transparent
): Modifier = this
    .clip(SheetTopShape)
    .background(surfaceColor)

/**
 * Body row modifier for unrolled LazyColumn sheet items (pure tonal fill, zero border strokes).
 */
@Suppress("UNUSED_PARAMETER")
fun Modifier.shiftlySheetBodyBand(
    surfaceColor: Color,
    borderColor: Color = Color.Transparent
): Modifier = this.background(surfaceColor)

/**
 * Full-bleed screen backdrop reacting to the active Shiftly theme.
 */
@Composable
fun ShiftlyBlueGradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        content()
    }
}

/**
 * Standardized top drag handle for anchored sheet containers with uniform vertical rhythm.
 */
@Composable
fun ShiftlySheetDragHandle(modifier: Modifier = Modifier) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .clip(PillShape)
                .background(colors.textSecondary.copy(alpha = 0.42f))
        )
    }
}

/**
 * Elevated borderless container separated strictly by background tone.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShiftlyGlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 18.dp,
    contentPadding: PaddingValues = PaddingValues(18.dp),
    showBorder: Boolean = false,
    animateSize: Boolean = false,
    containerColor: Color = ShiftlyColors.current.card,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(containerColor)
            .then(if (animateSize) Modifier.animateContentSize() else Modifier)
            .padding(contentPadding)
    ) {
        Column(content = content)
    }
}

/**
 * Standardized Shiftly card container with 18dp corners and zero borders/shadows.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShiftlyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ShiftlyColors.current.card,
    borderColor: Color = Color.Transparent,
    cornerRadius: Dp = 18.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = shape,
        color = containerColor,
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(contentPadding),
            content = content
        )
    }
}

/**
 * Solid-color rounded-square iOS Settings style icon tile holding a pure white glyph.
 */
@Composable
fun ShiftlyIosIconTile(
    @DrawableRes iconRes: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 36.dp,
    cornerRadius: Dp = 9.dp,
    iconSize: Dp = 18.dp,
    iconTint: Color = PureWhite,
    contentDescription: String? = null
) {
    val shape = remember(cornerRadius) { RoundedCornerShape(cornerRadius) }
    Box(
        modifier = modifier
            .size(size)
            .clip(shape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Subtle 0.5dp horizontal hairline divider with optional left/right inset.
 */
@Composable
fun ShiftlyHairlineDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
    endInset: Dp = 0.dp
) {
    val colors = ShiftlyColors.current
    HorizontalDivider(
        modifier = modifier.padding(start = startInset, end = endInset),
        thickness = 0.5.dp,
        color = colors.separator
    )
}

/**
 * Subtle 0.5dp vertical hairline divider for flattened metric columns.
 */
@Composable
fun ShiftlyVerticalHairlineDivider(
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .width(0.5.dp)
            .background(colors.separator)
    )
}

/**
 * Thin, fully rounded, dotless iOS-style progress bar (4–6dp height).
 */
@Composable
fun ShiftlyProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = ShiftlyColors.current.cardElevated,
    height: Dp = 5.dp
) {
    LinearProgressIndicator(
        progress = { progress.coerceIn(0f, 1f) },
        color = color,
        trackColor = trackColor,
        strokeCap = StrokeCap.Round,
        gapSize = 0.dp,
        drawStopIndicator = {},
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(PillShape)
    )
}

/**
 * Full-width pill primary action button.
 */
@Composable
fun ShiftlyPrimaryPillButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val buttonScaleState = animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pillButtonScale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = PillShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.azurePrimary,
            contentColor = colors.onAzurePrimary,
            disabledContainerColor = colors.disabledContainer,
            disabledContentColor = colors.disabledContent
        ),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = 0.dp,
            pressedElevation = 0.dp
        ),
        contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .graphicsLayer {
                val scale = buttonScaleState.value
                scaleX = scale
                scaleY = scale
            }
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(10.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (enabled) colors.onAzurePrimary else colors.disabledContent
        )
    }
}

/**
 * Screen-level header rendered over the active Shiftly backdrop.
 */
@Composable
fun ShiftlyScreenHeader(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = "",
    eyebrow: String? = null,
    largeTitle: Boolean = false,
    onBackClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            if (onBackClick != null) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_left),
                        contentDescription = "Back",
                        tint = colors.azureAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                if (!eyebrow.isNullOrBlank()) {
                    Text(
                        text = eyebrow.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                }
                Text(
                    text = title,
                    style = if (largeTitle) {
                        MaterialTheme.typography.displaySmall
                    } else {
                        MaterialTheme.typography.headlineSmall
                    },
                    color = colors.textPrimary,
                    modifier = Modifier.semantics { heading() }
                )
                if (!subtitle.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(12.dp))
            trailing()
        }
    }
}

/**
 * Reusable `< MMM yyyy >` + `"All"` month filter bar for Expenses & Split Bill screens.
 */
@Composable
fun ShiftlyMonthFilterBar(
    selectedMonth: YearMonth?,
    onPreviousMonth: () -> Unit,
    onNextMonth: () -> Unit,
    onToggleAllMonths: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val monthLabel = remember(selectedMonth, monthFormatter) {
        selectedMonth?.format(monthFormatter) ?: "All Months"
    }
    val isAllSelected = selectedMonth == null

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = PillShape,
            color = colors.card,
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 46.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onPreviousMonth,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_left),
                        contentDescription = "Previous month",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = onNextMonth,
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_chevron_right),
                        contentDescription = "Next month",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .heightIn(min = 46.dp)
                .clip(PillShape)
                .background(if (isAllSelected) colors.azurePrimary else colors.card)
                .clickable(role = Role.Tab) { onToggleAllMonths() }
                .semantics { selected = isAllSelected }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "All",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = if (isAllSelected) FontWeight.SemiBold else FontWeight.Medium
                ),
                color = if (isAllSelected) colors.onAzurePrimary else colors.textSecondary
            )
        }
    }
}

/**
 * Consistent section header with optional count badge or trailing label/action.
 */
@Composable
fun ShiftlySectionHeader(
    title: String,
    modifier: Modifier = Modifier,
    count: Int? = null,
    trailingText: String? = null,
    onTrailingClick: (() -> Unit)? = null,
    onLightSurface: Boolean = false
) {
    val colors = ShiftlyColors.current
    val titleColor = colors.textPrimary
    val countBg = if (onLightSurface) colors.cardElevated else colors.card
    val countTextColor = colors.textSecondary

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f, fill = false)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = titleColor,
                modifier = Modifier.semantics { heading() }
            )
            if (count != null) {
                Surface(
                    shape = PillShape,
                    color = countBg,
                    shadowElevation = 0.dp
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = countTextColor,
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                    )
                }
            }
        }
        if (trailingText != null) {
            if (onTrailingClick != null) {
                Box(
                    modifier = Modifier
                        .heightIn(min = 44.dp)
                        .clip(PillShape)
                        .clickable(onClick = onTrailingClick)
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = trailingText,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = colors.azureInk
                    )
                }
            } else {
                Text(
                    text = trailingText,
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

/**
 * Consistent destructive delete confirmation modal.
 */
@Composable
fun ShiftlyConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = ShiftlyColors.current
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        icon = {
            ShiftlyIosIconTile(
                iconRes = R.drawable.ic_lucide_trash_2,
                backgroundColor = colors.coralBright,
                size = 44.dp,
                cornerRadius = 11.dp,
                iconSize = 22.dp
            )
        },
        title = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary
            )
        },
        text = {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                shape = PillShape,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.coralBright,
                    contentColor = colors.onAccentDark
                ),
                elevation = ButtonDefaults.buttonElevation(
                    defaultElevation = 0.dp,
                    pressedElevation = 0.dp
                )
            ) {
                Text(
                    text = "Delete",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily
                    ),
                    color = colors.textSecondary
                )
            }
        }
    )
}
