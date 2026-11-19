package com.example.shiftly.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.InteractionSource
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.shiftly.R
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

private val PillShape = RoundedCornerShape(50)
private val PrimaryButtonShape = RoundedCornerShape(14.dp)
private val SheetTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val DialogCardShape = RoundedCornerShape(20.dp)

/**
 * iOS-style press feedback: scale 0.97 + slight alpha dip with a spring restore.
 */
@Composable
fun Modifier.shiftlyPressEffect(
    interactionSource: InteractionSource,
    enabled: Boolean = true
): Modifier {
    val isPressed by interactionSource.collectIsPressedAsState()
    val pressed = enabled && isPressed
    val scaleState = animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "shiftlyPressScale"
    )
    val alphaState = animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium),
        label = "shiftlyPressAlpha"
    )
    return this.graphicsLayer {
        val s = scaleState.value
        scaleX = s
        scaleY = s
        alpha = alphaState.value
    }
}

/**
 * Ripple-free clickable with the shared iOS press effect.
 */
@Composable
fun Modifier.shiftlyPressClickable(
    enabled: Boolean = true,
    onClickLabel: String? = null,
    role: Role? = null,
    onClick: () -> Unit
): Modifier {
    val interactionSource = remember { MutableInteractionSource() }
    return this
        .shiftlyPressEffect(interactionSource = interactionSource, enabled = enabled)
        .clickable(
            interactionSource = interactionSource,
            indication = null,
            enabled = enabled,
            onClickLabel = onClickLabel,
            role = role,
            onClick = onClick
        )
}

/**
 * Small centered translucent top bar that fades in once a large title has collapsed.
 */
@Composable
fun ShiftlyCollapsedTopBar(
    title: String,
    isCollapsed: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    AnimatedVisibility(
        visible = isCollapsed,
        enter = fadeIn(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)),
        exit = fadeOut(animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (colors.isDark) Color(0xEB1C1C1E) else colors.surface.copy(alpha = 0.94f)
                )
                .statusBarsPadding()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .padding(horizontal = 20.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.semantics { heading() }
                )
            }
            ShiftlyHairlineDivider()
        }
    }
}

@Composable
fun shiftlySheetScrimColor(): Color {
    val colors = ShiftlyColors.current
    return Color.Black.copy(alpha = if (colors.isDark) 0.6f else 0.4f)
}

/**
 * Top cap modifier for unrolled LazyColumn sheet bands: rounds the top 20dp corners
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
 * Compact top grabber for anchored sheets and dialogs.
 */
@Composable
fun ShiftlySheetDragHandle(modifier: Modifier = Modifier) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 2.dp),
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
 * Standardized iOS-style sheet/dialog navigation header:
 * "Cancel" on the left, centered bold title, and a blue text action on the right.
 */
@Composable
fun ShiftlySheetHeader(
    title: String,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelLabel: String = "Cancel",
    actionLabel: String? = "Done",
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = onCancel
) {
    val colors = ShiftlyColors.current
    val cancelInteraction = remember { MutableInteractionSource() }
    val actionInteraction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 48.dp)
    ) {
        TextButton(
            onClick = onCancel,
            interactionSource = cancelInteraction,
            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .heightIn(min = 44.dp)
                .shiftlyPressEffect(cancelInteraction)
        ) {
            Text(
                text = cancelLabel,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Normal
                ),
                color = colors.azureAccent,
                maxLines = 1
            )
        }

        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp
            ),
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 78.dp)
                .semantics { heading() }
        )

        if (actionLabel != null && onAction != null) {
            TextButton(
                onClick = onAction,
                enabled = actionEnabled,
                interactionSource = actionInteraction,
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 4.dp),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .heightIn(min = 44.dp)
                    .shiftlyPressEffect(actionInteraction, enabled = actionEnabled)
            ) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = if (actionEnabled) colors.azureAccent else colors.disabledContent,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Standardized iOS-style modal dialog wrapper with dimmed scrim, 20dp radius, grabber,
 * and "Cancel" | Bold Title | Blue Action header.
 */
@Composable
fun ShiftlyDialogShell(
    title: String,
    onDismiss: () -> Unit,
    actionLabel: String? = "Done",
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = onDismiss,
    maxWidth: Dp = 392.dp,
    horizontalOuterPadding: Dp = 16.dp,
    contentHorizontalPadding: Dp = 18.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ShiftlyColors.current
    val slideOffsetY = remember { Animatable(140f) }
    val cardAlpha = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch {
            slideOffsetY.animateTo(
                targetValue = 0f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
            )
        }
        launch {
            cardAlpha.animateTo(
                targetValue = 1f,
                animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
            )
        }
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(shiftlySheetScrimColor())
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss
                )
                .padding(horizontal = horizontalOuterPadding, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = DialogCardShape,
                color = colors.surface,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = maxWidth)
                    .fillMaxWidth()
                    .graphicsLayer {
                        translationY = slideOffsetY.value
                        alpha = cardAlpha.value
                    }
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = contentHorizontalPadding)
                        .padding(bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlySheetDragHandle()
                    ShiftlySheetHeader(
                        title = title,
                        onCancel = onDismiss,
                        actionLabel = actionLabel,
                        actionEnabled = actionEnabled,
                        onAction = onAction
                    )
                    content()
                }
            }
        }
    }
}

/**
 * iOS-style 51x31dp toggle switch with white thumb, #30D158 when on, dark gray when off.
 */
@Composable
fun ShiftlyIosSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    val trackColor by animateColorAsState(
        targetValue = when {
            checked -> Color(0xFF30D158)
            colors.isDark -> Color(0xFF39393D)
            else -> Color(0xFF48484A)
        },
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "iosSwitchTrack"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 22.dp else 2.dp,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "iosSwitchThumb"
    )

    Box(
        modifier = modifier
            .size(width = 51.dp, height = 31.dp)
            .clip(PillShape)
            .background(trackColor.copy(alpha = if (enabled) 1f else 0.5f))
            .clickable(
                enabled = enabled,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Switch,
                onClick = {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onCheckedChange(!checked)
                }
            ),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                .size(27.dp)
                .clip(CircleShape)
                .background(PureWhite.copy(alpha = if (enabled) 1f else 0.85f))
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
            .then(
                if (animateSize) {
                    Modifier.animateContentSize(
                        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow)
                    )
                } else {
                    Modifier
                }
            )
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
 * Full-width primary action button with 14dp corner radius.
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

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = PrimaryButtonShape,
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
            .heightIn(min = 50.dp)
            .shiftlyPressEffect(interactionSource = interactionSource, enabled = enabled)
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
 * Plain iOS-style blue text secondary button.
 */
@Composable
fun ShiftlySecondaryTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    val interactionSource = remember { MutableInteractionSource() }
    TextButton(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        modifier = modifier
            .heightIn(min = 44.dp)
            .shiftlyPressEffect(interactionSource = interactionSource, enabled = enabled)
    ) {
        if (leadingIcon != null) {
            leadingIcon()
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ),
            color = if (enabled) colors.azureAccent else colors.disabledContent
        )
    }
}

/**
 * Centered iOS empty state: icon tile, bold title, gray subtitle, and one plain blue text button.
 */
@Composable
fun ShiftlyEmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = ShiftlyColors.current.azurePrimary,
    containerColor: Color = ShiftlyColors.current.cardElevated
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            ShiftlyIosIconTile(
                iconRes = iconRes,
                backgroundColor = iconBackgroundColor,
                size = 48.dp,
                cornerRadius = 12.dp,
                iconSize = 24.dp
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
            ShiftlySecondaryTextButton(
                text = actionText,
                onClick = onAction
            )
        }
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
                val backInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onBackClick,
                    interactionSource = backInteraction,
                    modifier = Modifier
                        .size(40.dp)
                        .shiftlyPressEffect(backInteraction)
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
    val haptics = LocalHapticFeedback.current
    val locale = LocalConfiguration.current.locales[0]
    val monthFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM yyyy", locale) }
    val monthLabel = remember(selectedMonth, monthFormatter) {
        selectedMonth?.format(monthFormatter) ?: "All Months"
    }
    val isAllSelected = selectedMonth == null
    val prevInteraction = remember { MutableInteractionSource() }
    val nextInteraction = remember { MutableInteractionSource() }

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
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .size(44.dp)
                        .shiftlyPressEffect(prevInteraction)
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
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .size(44.dp)
                        .shiftlyPressEffect(nextInteraction)
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
                .shiftlyPressClickable(role = Role.Tab) {
                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onToggleAllMonths()
                }
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
                        .shiftlyPressClickable(onClick = onTrailingClick)
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
 * Consistent destructive delete confirmation modal with iOS sheet header.
 */
@Composable
fun ShiftlyConfirmDeleteDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val colors = ShiftlyColors.current
    ShiftlyDialogShell(
        title = title,
        onDismiss = onDismiss,
        actionLabel = "Done",
        onAction = onConfirm,
        maxWidth = 380.dp,
        horizontalOuterPadding = 24.dp,
        contentHorizontalPadding = 20.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp, bottom = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShiftlyIosIconTile(
                iconRes = R.drawable.ic_lucide_trash_2,
                backgroundColor = colors.coralBright,
                size = 44.dp,
                cornerRadius = 11.dp,
                iconSize = 22.dp
            )
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                textAlign = TextAlign.Center
            )
        }
    }
}
