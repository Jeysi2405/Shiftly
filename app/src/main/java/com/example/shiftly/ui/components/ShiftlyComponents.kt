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
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * Subtle transparent `?` quick tutorial icon button for headers and tool sheets.
 */
@Composable
fun ShiftlyHelpIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String = "Quick guide",
    tint: Color = Color(0xFF8E8E93)
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .size(40.dp)
            .shiftlyPressEffect(interactionSource)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_lucide_circle_help),
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(20.dp)
        )
    }
}

/**
 * Standardized sheet title header:
 * Centered bold title with optional trailing `?` quick guide icon (no top corner Cancel/Save buttons).
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShiftlySheetHeader(
    title: String,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
    cancelLabel: String = "Cancel",
    actionLabel: String? = "Done",
    actionEnabled: Boolean = true,
    onAction: (() -> Unit)? = null,
    onHelpClick: (() -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp)
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
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
                .then(if (onHelpClick != null) Modifier.padding(horizontal = 44.dp) else Modifier)
                .semantics { heading() }
        )
        if (onHelpClick != null) {
            ShiftlyHelpIconButton(
                onClick = onHelpClick,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

/**
 * Standardized modal bottom sheet shell for tool sheets supporting an optional `?` tutorial guide button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftlyToolSheetContainer(
    title: String,
    onDismiss: () -> Unit,
    onHelpClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = SheetTopShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            ShiftlySheetHeader(
                title = title,
                onHelpClick = onHelpClick
            )
            content()
        }
    }
}

/**
 * Standardized sheet bottom button row:
 * Cancel (left, cardElevated fill) | Action (right, azurePrimary fill).
 */
@Composable
fun ShiftlySheetBottomButtons(
    onCancel: () -> Unit,
    onAction: () -> Unit,
    cancelLabel: String = "Cancel",
    actionLabel: String = "Save",
    actionEnabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .shiftlyPressClickable(onClick = onCancel)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = cancelLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(14.dp),
            color = if (actionEnabled) colors.azurePrimary else colors.disabledContainer,
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .shiftlyPressClickable(enabled = actionEnabled, onClick = onAction)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = actionLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = if (actionEnabled) PureWhite else colors.disabledContent
                )
            }
        }
    }
}

/**
 * Standardized full-width secondary close/done button for read-only or utility sheets.
 */
@Composable
fun ShiftlySheetCloseButton(
    label: String = "Close",
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(RoundedCornerShape(14.dp))
            .shiftlyPressClickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp
                ),
                color = colors.textPrimary
            )
        }
    }
}

/**
 * Standardized iOS-style modal dialog wrapper with dimmed scrim, 20dp radius, grabber,
 * centered bold title, and bottom action row.
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
                    ShiftlySheetHeader(title = title)
                    content()
                    if (onAction != null && actionLabel != null) {
                        Spacer(modifier = Modifier.height(4.dp))
                        if (onAction === onDismiss) {
                            ShiftlySheetCloseButton(
                                label = actionLabel,
                                onClick = onDismiss
                            )
                        } else {
                            ShiftlySheetBottomButtons(
                                onCancel = onDismiss,
                                onAction = onAction,
                                cancelLabel = "Cancel",
                                actionLabel = actionLabel,
                                actionEnabled = actionEnabled
                            )
                        }
                    }
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
 * iOS-style segmented pill control for switching between compact options.
 */
@Composable
fun ShiftlyIosSegmentedControl(
    options: List<String>,
    selectedOption: String,
    onOptionSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
    containerColor: Color = ShiftlyColors.current.cardElevated
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(containerColor)
            .selectableGroup()
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        options.forEach { option ->
            val isSelected = selectedOption == option
            val segmentBg by animateColorAsState(
                targetValue = if (isSelected) colors.azurePrimary else Color.Transparent,
                animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow),
                label = "iosSegmentBg"
            )
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(segmentBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        role = Role.Tab
                    ) {
                        if (!isSelected) {
                            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            onOptionSelected(option)
                        }
                    }
                    .semantics { selected = isSelected },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = option,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        fontSize = 14.sp
                    ),
                    color = if (isSelected) PureWhite else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
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
 * Transparent iOS-style icon layout box holding an accent-tinted glyph.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShiftlyIosIconTile(
    @DrawableRes iconRes: Int,
    backgroundColor: Color,
    modifier: Modifier = Modifier,
    size: Dp = 28.dp,
    cornerRadius: Dp = 9.dp,
    iconSize: Dp = 22.dp,
    iconTint: Color = backgroundColor,
    contentDescription: String? = null
) {
    Box(
        modifier = modifier.size(size),
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
                size = 36.dp,
                cornerRadius = 12.dp,
                iconSize = 32.dp
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
 * Standardized iOS-style confirmation & delete prompt dialog without top header buttons.
 * Displays centered drag handle, centered bold title, icon, question text, and a bottom row of No / Yes buttons.
 */
@Composable
fun ShiftlyConfirmDialog(
    title: String,
    message: String,
    iconRes: Int = R.drawable.ic_lucide_trash_2,
    iconBgColor: Color = ShiftlyColors.current.coralBright,
    iconTint: Color = iconBgColor,
    noLabel: String = "No",
    yesLabel: String = "Yes",
    isDestructive: Boolean = true,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = DialogCardShape,
                color = colors.surface,
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 380.dp)
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
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    ShiftlySheetDragHandle()
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
                        overflow = TextOverflow.Ellipsis
                    )
                    ShiftlyIosIconTile(
                        iconRes = iconRes,
                        backgroundColor = iconBgColor,
                        size = 36.dp,
                        cornerRadius = 11.dp,
                        iconSize = 28.dp,
                        iconTint = iconTint
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = colors.cardElevated,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shiftlyPressClickable(onClick = onDismiss)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = noLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    ),
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isDestructive) Color(0xFFFF453A) else colors.azurePrimary,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .shiftlyPressClickable(onClick = onConfirm)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = yesLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 15.sp
                                    ),
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Clean iOS-style delete confirmation dialog preventing accidental record deletions.
 */
@Composable
fun ShiftlyDeleteConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String = "Delete",
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
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
                .padding(horizontal = 24.dp, vertical = 20.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                shape = DialogCardShape,
                color = colors.surface,
                border = BorderStroke(0.5.dp, colors.separator),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 360.dp)
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
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        ),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.cardElevated,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shiftlyPressClickable(onClick = onDismiss)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Cancel",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 15.sp
                                    ),
                                    color = colors.textPrimary
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFFF453A),
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .shiftlyPressClickable(onClick = onConfirm)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = confirmLabel,
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp
                                    ),
                                    color = PureWhite
                                )
                            }
                        }
                    }
                }
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
    ShiftlyDeleteConfirmDialog(
        title = title,
        message = message,
        confirmLabel = "Delete",
        onConfirm = onConfirm,
        onDismiss = onDismiss
    )
}

private data class FeatureGuideItem(
    val title: String,
    val description: String
)

private data class FeatureGuideCategory(
    val title: String,
    @DrawableRes val iconRes: Int,
    val accentColor: Color,
    val features: List<FeatureGuideItem>
)

private val SHIFTLY_FEATURE_GUIDE_CATEGORIES = listOf(
    FeatureGuideCategory(
        title = "Shifts & Work Pay",
        iconRes = R.drawable.ic_lucide_clock,
        accentColor = Color(0xFF30D158),
        features = listOf(
            FeatureGuideItem(
                title = "Easy Shift Logging",
                description = "Record your work hours in seconds using quick time presets."
            ),
            FeatureGuideItem(
                title = "Automatic Pay Computation",
                description = "Let the app calculate your regular pay, night differential, overtime, and holiday rates for you."
            ),
            FeatureGuideItem(
                title = "Upcoming Shift Reminders",
                description = "Save your future schedules and get gentle reminders before your shift starts."
            ),
            FeatureGuideItem(
                title = "Monthly Work Goals",
                description = "Set a personal target for your hours or earnings and watch your progress grow."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Daily Spending & Budgets",
        iconRes = R.drawable.ic_lucide_wallet,
        accentColor = Color(0xFFFF453A),
        features = listOf(
            FeatureGuideItem(
                title = "Simple Expense Tracking",
                description = "Log where your money goes by category and save photos of your receipts."
            ),
            FeatureGuideItem(
                title = "Flexible Monthly Budgets",
                description = "Set a comfortable spending limit for the month or for specific categories."
            ),
            FeatureGuideItem(
                title = "Daily Safe-to-Spend Guide",
                description = "Know how much you can comfortably spend today while staying on track."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Allowance & Take-Home Pay",
        iconRes = R.drawable.ic_lucide_banknote,
        accentColor = Color(0xFF0A84FF),
        features = listOf(
            FeatureGuideItem(
                title = "Allowance & Extra Income",
                description = "Keep track of allowances, stipends, scholarships, and side income in one place."
            ),
            FeatureGuideItem(
                title = "Take-Home Pay Estimator",
                description = "See a clear estimate of your net pay after SSS, PhilHealth, Pag-IBIG, and tax deductions."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Shared Bills & Savings",
        iconRes = R.drawable.ic_lucide_split,
        accentColor = Color(0xFFFF9F0A),
        features = listOf(
            FeatureGuideItem(
                title = "Fair Bill Splitting",
                description = "Split meals or shared bills with friends and easily see who has paid."
            ),
            FeatureGuideItem(
                title = "Borrow & Lend Tracker (IOUs)",
                description = "Remember who owes you and who you need to pay back, stress-free."
            ),
            FeatureGuideItem(
                title = "Installment Loan Tracker",
                description = "Track financed items like a laptop or phone, see how much balance and time is left in your contract, and log payments in one tap."
            ),
            FeatureGuideItem(
                title = "Recurring Bills",
                description = "Keep track of daily, weekly, or monthly bills so you never miss a due date."
            ),
            FeatureGuideItem(
                title = "Personal Savings Goals",
                description = "Set aside money little by little for the things that matter to you."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Clear Insights & Calendar",
        iconRes = R.drawable.ic_lucide_chart_column,
        accentColor = Color(0xFFBF5AF2),
        features = listOf(
            FeatureGuideItem(
                title = "Calm Home Overview",
                description = "See your current balance, total earnings, and spending at a glance, with a privacy toggle to hide amounts anytime."
            ),
            FeatureGuideItem(
                title = "Visual Spending & Earning Trends",
                description = "Understand your habits through clean, easy-to-read charts."
            ),
            FeatureGuideItem(
                title = "All-in-One Activity Calendar",
                description = "View your work days, scheduled shifts, expenses, and allowance days on a single calendar."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Privacy & Peace of Mind",
        iconRes = R.drawable.ic_lucide_lock,
        accentColor = Color(0xFF64D2FF),
        features = listOf(
            FeatureGuideItem(
                title = "Private & Works Offline",
                description = "Your data stays safely on your phone—no sign-up or internet needed."
            ),
            FeatureGuideItem(
                title = "PIN & Biometric App Lock",
                description = "Keep your personal finances for your eyes only."
            ),
            FeatureGuideItem(
                title = "Easy Backup & Spreadsheet Export",
                description = "Save a backup of your records or export them to CSV anytime."
            )
        )
    )
)

/**
 * 6-category expandable feature guide sheet shown post-setup and from Profile Settings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftlyFeatureGuideSheet(
    onDismiss: () -> Unit,
    primaryButtonText: String = "Start Exploring"
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var expandedIndices by remember { mutableStateOf(setOf(0)) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = SheetTopShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = "What You Can Do Here",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = "Tap any topic to explore, or jump right in.",
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color(0xFF8E8E93),
                        textAlign = TextAlign.Center
                    )
                }

                SHIFTLY_FEATURE_GUIDE_CATEGORIES.forEachIndexed { index, category ->
                    val isExpanded = index in expandedIndices
                    FeatureGuideCategoryCard(
                        category = category,
                        isExpanded = isExpanded,
                        onToggle = {
                            expandedIndices = if (isExpanded) {
                                expandedIndices - index
                            } else {
                                expandedIndices + index
                            }
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Surface(
                shape = PrimaryButtonShape,
                color = Color(0xFF0A84FF),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(PrimaryButtonShape)
                    .shiftlyPressClickable(onClick = onDismiss)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = primaryButtonText,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = PureWhite
                    )
                }
            }
        }
    }
}

@Composable
private fun FeatureGuideCategoryCard(
    category: FeatureGuideCategory,
    isExpanded: Boolean,
    onToggle: () -> Unit
) {
    val colors = ShiftlyColors.current
    val chevronRotation by animateFloatAsState(
        targetValue = if (isExpanded) 90f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "featureGuideChevron"
    )

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardElevated,
        border = BorderStroke(0.5.dp, colors.separator),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(category.iconRes),
                    contentDescription = null,
                    tint = category.accentColor,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = category.title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp
                    ),
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_right),
                    contentDescription = if (isExpanded) "Collapse ${category.title}" else "Expand ${category.title}",
                    tint = colors.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .graphicsLayer { rotationZ = chevronRotation }
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ShiftlyHairlineDivider(startInset = 16.dp)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        category.features.forEach { feature ->
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = feature.title,
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp
                                    ),
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = feature.description,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontSize = 12.sp,
                                        lineHeight = 17.sp
                                    ),
                                    color = Color(0xFF8E8E93)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Reusable quick tutorial bottom sheet opened from `?` buttons on tool screens & utility sheets.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftlyScreenTutorialSheet(
    title: String,
    subtitle: String,
    steps: List<Pair<String, String>>,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = SheetTopShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp
                        ),
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { heading() }
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            fontFamily = InterFontFamily,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        ),
                        color = Color(0xFF8E8E93),
                        textAlign = TextAlign.Center
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        steps.forEachIndexed { index, (stepTitle, stepDescription) ->
                            if (index > 0) {
                                ShiftlyHairlineDivider(startInset = 52.dp)
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 14.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0A84FF).copy(alpha = 0.14f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ),
                                        color = Color(0xFF0A84FF)
                                    )
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Text(
                                        text = stepTitle,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 14.sp
                                        ),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = stepDescription,
                                        style = MaterialTheme.typography.bodySmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontSize = 13.sp,
                                            lineHeight = 18.sp
                                        ),
                                        color = Color(0xFF8E8E93)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = PrimaryButtonShape,
                color = Color(0xFF0A84FF),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .clip(PrimaryButtonShape)
                    .shiftlyPressClickable(onClick = onDismiss)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "Got It",
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = PureWhite
                    )
                }
            }
        }
    }
}

/**
 * 5-step guided Coachmark / Tour overlay rendered above the main navigation shell.
 */
@Composable
fun ShiftlyMainTabTourOverlay(
    stepIndex: Int,
    totalSteps: Int = 5,
    title: String,
    description: String,
    onBack: () -> Unit,
    onNextOrFinish: () -> Unit,
    onSkip: () -> Unit,
    highlightedBottomBar: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val cardColor = if (colors.isDark) Color(0xFF1C1C1E) else colors.surface
    val isLastStep = stepIndex >= totalSteps - 1

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.65f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = DialogCardShape,
                    color = cardColor,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .widthIn(max = 392.dp)
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(dampingRatio = 0.85f, stiffness = Spring.StiffnessMediumLow)
                        )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = PillShape,
                                color = Color(0xFF0A84FF).copy(alpha = 0.14f),
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = "${stepIndex + 1} of $totalSteps",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ),
                                    color = Color(0xFF0A84FF),
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Text(
                                text = "Skip",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp
                                ),
                                color = Color(0xFF8E8E93),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .shiftlyPressClickable(onClick = onSkip)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = title,
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 18.sp
                                ),
                                color = colors.textPrimary,
                                modifier = Modifier.semantics { heading() }
                            )
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontSize = 14.sp,
                                    lineHeight = 20.sp
                                ),
                                color = colors.textSecondary
                            )
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (stepIndex > 0) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.cardElevated,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .shiftlyPressClickable(onClick = onBack)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            text = "Back",
                                            style = MaterialTheme.typography.labelLarge.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 15.sp
                                            ),
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFF0A84FF),
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .shiftlyPressClickable(onClick = onNextOrFinish)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Text(
                                        text = if (isLastStep) "Finish" else "Next",
                                        style = MaterialTheme.typography.labelLarge.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp
                                        ),
                                        color = PureWhite
                                    )
                                }
                            }
                        }
                    }
                }
            }

            highlightedBottomBar()
        }
    }
}
