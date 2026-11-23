package com.example.shiftly.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
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
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
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
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.math.sin
import kotlin.random.Random
import kotlinx.coroutines.launch

private val PillShape = ShiftlyShapes.Pill
private val PrimaryButtonShape = ShiftlyShapes.Control
private val SheetTopShape = ShiftlyShapes.Sheet
private val DialogCardShape = ShiftlyShapes.Dialog

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
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
        label = "shiftlyPressScale"
    )
    val alphaState = animateFloatAsState(
        targetValue = if (pressed) 0.84f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
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
                .background(colors.collapsedTopBar)
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
    tint: Color = ShiftlyColors.current.textSecondary
) {
    val interactionSource = remember { MutableInteractionSource() }
    val haptics = LocalHapticFeedback.current
    var tapCount by remember { mutableIntStateOf(0) }
    IconButton(
        onClick = {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            tapCount++
            onClick()
        },
        interactionSource = interactionSource,
        modifier = modifier
            .size(48.dp)
            .shiftlyPressEffect(interactionSource)
    ) {
        ShiftlyAnimatedIcon(
            iconRes = R.drawable.ic_lucide_circle_help,
            contentDescription = contentDescription,
            tint = tint,
            triggerKey = tapCount,
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
            .heightIn(min = 48.dp)
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
                .then(if (onHelpClick != null) Modifier.padding(horizontal = 48.dp) else Modifier)
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
            shape = PrimaryButtonShape,
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(PrimaryButtonShape)
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
            shape = PrimaryButtonShape,
            color = if (actionEnabled) colors.azurePrimary else colors.disabledContainer,
            shadowElevation = 0.dp,
            modifier = Modifier
                .weight(1f)
                .height(50.dp)
                .clip(PrimaryButtonShape)
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
        shape = PrimaryButtonShape,
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(50.dp)
            .clip(PrimaryButtonShape)
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
 * Standardized modal dialog wrapper with dimmed scrim, 20dp radius,
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
                        .padding(top = 16.dp, bottom = 18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
 * iOS-style 51x31dp toggle switch with white thumb, #30D158 when on, dark gray when off,
 * wrapped in a >= 48dp touch target container.
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
        targetValue = if (checked) colors.azurePrimary else colors.switchOffTrack,
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
            .defaultMinSize(minWidth = 51.dp, minHeight = 48.dp)
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
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(width = 51.dp, height = 31.dp)
                .clip(PillShape)
                .background(trackColor.copy(alpha = if (enabled) 1f else 0.5f)),
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
                    .heightIn(min = 48.dp)
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
    cornerRadius: Dp = 16.dp,
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
 * Standardized Shiftly card container with 16dp corners and zero borders/shadows.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShiftlyCard(
    modifier: Modifier = Modifier,
    containerColor: Color = ShiftlyColors.current.card,
    borderColor: Color = Color.Transparent,
    cornerRadius: Dp = 16.dp,
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
    contentDescription: String? = null,
    triggerKey: Any? = null,
    animateOnMount: Boolean = false
) {
    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center
    ) {
        ShiftlyAnimatedIcon(
            iconRes = iconRes,
            contentDescription = contentDescription,
            tint = iconTint,
            triggerKey = triggerKey,
            animateOnMount = animateOnMount,
            modifier = Modifier.size(iconSize)
        )
    }
}

/**
 * Hairline divider kept as a no-op spacer for clean borderless card separation.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun ShiftlyHairlineDivider(
    modifier: Modifier = Modifier,
    startInset: Dp = 0.dp,
    endInset: Dp = 0.dp
) {
    // Intentionally empty: clean borderless UI uses spacing and tone instead of thin dividers.
}

/**
 * Vertical spacing placeholder preserving column padding without drawing a hairline rule.
 */
@Composable
fun ShiftlyVerticalHairlineDivider(
    modifier: Modifier = Modifier
) {
    Spacer(modifier = modifier.width(0.dp))
}

/**
 * Thin, fully rounded, dotless iOS-style progress bar (4–6dp height) with spring animation.
 */
@Composable
fun ShiftlyProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    trackColor: Color = ShiftlyColors.current.cardElevated,
    height: Dp = 5.dp
) {
    val clampedTarget = progress.coerceIn(0f, 1f)
    val animatedProgress by animateFloatAsState(
        targetValue = clampedTarget,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "shiftlyProgress"
    )
    val animatedColor by animateColorAsState(
        targetValue = color,
        animationSpec = spring(
            dampingRatio = 0.85f,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "shiftlyProgressColor"
    )
    LinearProgressIndicator(
        progress = { animatedProgress.coerceIn(0f, 1f) },
        color = animatedColor,
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
 * Tabular currency text that rolls smoothly between numeric values and animates privacy masking.
 */
@Composable
fun ShiftlyAnimatedCurrencyText(
    currencySymbol: String,
    amount: Double,
    style: TextStyle,
    color: Color,
    modifier: Modifier = Modifier,
    showPlus: Boolean = false,
    showMinus: Boolean = false,
    isHidden: Boolean = false,
    hiddenText: String = "${currencySymbol}•••••",
    maxLines: Int = 1,
    softWrap: Boolean = true,
    overflow: TextOverflow = TextOverflow.Ellipsis
) {
    val safeTarget = if (amount.isFinite()) amount else 0.0
    var fromAmount by remember { mutableDoubleStateOf(safeTarget) }
    var targetAmount by remember { mutableDoubleStateOf(safeTarget) }
    val progress = remember { Animatable(1f) }

    LaunchedEffect(safeTarget) {
        val currentVal = fromAmount + (targetAmount - fromAmount) * progress.value.toDouble()
        if (abs(safeTarget - currentVal) < 0.005) {
            fromAmount = safeTarget
            targetAmount = safeTarget
            progress.snapTo(1f)
        } else {
            fromAmount = currentVal
            targetAmount = safeTarget
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing)
            )
            fromAmount = safeTarget
        }
    }

    val displayedAmount = if (progress.value >= 0.999f) {
        targetAmount
    } else {
        val raw = fromAmount + (targetAmount - fromAmount) * progress.value.toDouble()
        val fromWholeOnly = (abs(fromAmount) * 100.0).roundToLong() % 100L == 0L
        val targetWholeOnly = (abs(targetAmount) * 100.0).roundToLong() % 100L == 0L
        if (fromWholeOnly && targetWholeOnly) {
            raw.roundToLong().toDouble()
        } else {
            raw
        }
    }

    val formattedAmount = ShiftlyFormatters.formatCurrency(
        symbol = currencySymbol,
        amount = displayedAmount,
        showPlus = showPlus,
        showMinus = showMinus
    )

    AnimatedContent(
        targetState = isHidden,
        modifier = modifier,
        transitionSpec = {
            val dir = if (targetState) -1 else 1
            (fadeIn(tween(220)) + slideInVertically(
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) { (it / 5) * dir }) togetherWith
                (fadeOut(tween(160)) + slideOutVertically(
                    animationSpec = spring(
                        dampingRatio = 0.85f,
                        stiffness = Spring.StiffnessMedium
                    )
                ) { -(it / 5) * dir }) using
                SizeTransform(clip = false)
        },
        label = "shiftlyCurrencyPrivacy"
    ) { hidden ->
        Text(
            text = if (hidden) hiddenText else formattedAmount,
            style = style.tabular(),
            color = color,
            maxLines = maxLines,
            softWrap = softWrap,
            overflow = overflow
        )
    }
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
            .heightIn(min = 48.dp)
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
 * Centered empty state: subtle neutral icon badge, bold title, optional subtitle, and one clear action.
 */
@Composable
fun ShiftlyEmptyState(
    @DrawableRes iconRes: Int,
    title: String,
    subtitle: String? = null,
    actionText: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    iconBackgroundColor: Color = ShiftlyColors.current.iconNeutral,
    containerColor: Color = ShiftlyColors.current.card
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = ShiftlyShapes.Card,
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
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(ShiftlyShapes.Control)
                    .background(colors.iconNeutralContainer),
                contentAlignment = Alignment.Center
            ) {
                ShiftlyAnimatedIcon(
                    iconRes = iconRes,
                    contentDescription = null,
                    tint = iconBackgroundColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center
                )
            }
            ShiftlySecondaryTextButton(
                text = actionText,
                onClick = onAction
            )
        }
    }
}

/**
 * Calm sentence-case section label replacing ALL-CAPS eyebrow labels.
 */
@Composable
fun ShiftlyGroupLabel(
    text: String,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(
            fontFamily = InterFontFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 13.sp
        ),
        color = colors.textSecondary,
        modifier = modifier
            .padding(horizontal = 4.dp)
            .semantics { heading() }
    )
}

/**
 * Unified pinned sticky top app bar for sub-screens with back navigation,
 * optional trailing actions, and a scroll-reactive bottom hairline divider.
 */
@Composable
fun ShiftlySubScreenTopBar(
    title: String,
    onBack: () -> Unit,
    showDivider: Boolean = false,
    modifier: Modifier = Modifier,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = ShiftlyColors.current
    val backInteraction = remember { MutableInteractionSource() }
    var backTapCount by remember { mutableIntStateOf(0) }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = {
                    backTapCount++
                    onBack()
                },
                interactionSource = backInteraction,
                modifier = Modifier
                    .size(48.dp)
                    .shiftlyPressEffect(backInteraction)
            ) {
                ShiftlyAnimatedIcon(
                    iconRes = R.drawable.ic_lucide_arrow_left,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    triggerKey = backTapCount,
                    modifier = Modifier.size(22.dp)
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .semantics { heading() }
            )
            if (trailing != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                    content = trailing
                )
            }
        }
        AnimatedVisibility(
            visible = showDivider,
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(150))
        ) {
            ShiftlyHairlineDivider()
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
                var backTapCount by remember { mutableIntStateOf(0) }
                IconButton(
                    onClick = {
                        backTapCount++
                        onBackClick()
                    },
                    interactionSource = backInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .shiftlyPressEffect(backInteraction)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_chevron_left,
                        contentDescription = "Back",
                        tint = colors.azureAccent,
                        triggerKey = backTapCount,
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
    var prevTapCount by remember { mutableIntStateOf(0) }
    var nextTapCount by remember { mutableIntStateOf(0) }

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
                .heightIn(min = 48.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        prevTapCount++
                        onPreviousMonth()
                    },
                    interactionSource = prevInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .shiftlyPressEffect(prevInteraction)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_chevron_left,
                        contentDescription = "Previous month",
                        tint = colors.textPrimary,
                        triggerKey = prevTapCount,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Text(
                    text = monthLabel,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center
                )

                IconButton(
                    onClick = {
                        nextTapCount++
                        onNextMonth()
                    },
                    interactionSource = nextInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .shiftlyPressEffect(nextInteraction)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = R.drawable.ic_lucide_chevron_right,
                        contentDescription = "Next month",
                        tint = colors.textPrimary,
                        triggerKey = nextTapCount,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .heightIn(min = 48.dp)
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
                        style = MaterialTheme.typography.labelSmall.tabular(),
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
                        .heightIn(min = 48.dp)
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
 * Standardized confirmation & delete prompt dialog without top header buttons.
 * Displays centered bold title, icon, question text, and a bottom row of No / Yes buttons.
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
                        .padding(top = 16.dp, bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
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
                            shape = PrimaryButtonShape,
                            color = colors.cardElevated,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(PrimaryButtonShape)
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
                            shape = PrimaryButtonShape,
                            color = if (isDestructive) colors.coralBright else colors.azurePrimary,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .clip(PrimaryButtonShape)
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
                            shape = PrimaryButtonShape,
                            color = colors.cardElevated,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(PrimaryButtonShape)
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
                            shape = PrimaryButtonShape,
                            color = colors.coralBright,
                            shadowElevation = 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                                .clip(PrimaryButtonShape)
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

private data class FeatureGuideItem(
    val title: String,
    val description: String
)

private data class FeatureGuideCategory(
    val title: String,
    @DrawableRes val iconRes: Int,
    val features: List<FeatureGuideItem>
)

private val SHIFTLY_FEATURE_GUIDE_CATEGORIES = listOf(
    FeatureGuideCategory(
        title = "Shifts & Work Pay",
        iconRes = R.drawable.ic_lucide_clock,
        features = listOf(
            FeatureGuideItem(
                title = "Easy Shift Logging",
                description = "Tap Log Shift or a calendar date with quick time presets."
            ),
            FeatureGuideItem(
                title = "Automatic Pay Computation",
                description = "Auto-computes Regular Day, Rest Day, Holiday, OT & ND pay."
            ),
            FeatureGuideItem(
                title = "Upcoming Shift Reminders",
                description = "Get reminders before upcoming shifts."
            ),
            FeatureGuideItem(
                title = "Monthly Work Goals",
                description = "Track monthly hours and earnings targets."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Daily Spending & Budgets",
        iconRes = R.drawable.ic_lucide_wallet,
        features = listOf(
            FeatureGuideItem(
                title = "Simple Expense Tracking",
                description = "Log expenses with receipt photos; swipe to edit or delete."
            ),
            FeatureGuideItem(
                title = "Flexible Monthly Budgets",
                description = "Set overall monthly and category spending caps."
            ),
            FeatureGuideItem(
                title = "Daily Safe-to-Spend Guide",
                description = "See your daily safe-to-spend remaining limit."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Allowance & Take-Home Pay",
        iconRes = R.drawable.ic_lucide_banknote,
        features = listOf(
            FeatureGuideItem(
                title = "Allowance & Extra Income",
                description = "Track allowances, cutoffs, stipends, and side income."
            ),
            FeatureGuideItem(
                title = "Take-Home Pay Estimator",
                description = "Estimate net pay after statutory deductions."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Shared Bills & Savings",
        iconRes = R.drawable.ic_lucide_split,
        features = listOf(
            FeatureGuideItem(
                title = "Fair Bill Splitting",
                description = "Split group bills cleanly and track who paid."
            ),
            FeatureGuideItem(
                title = "Borrow & Lend Tracker (IOUs)",
                description = "Track who owes whom offline."
            ),
            FeatureGuideItem(
                title = "Installment Loan Tracker",
                description = "Add Monthly % or Total % interest and record custom payments."
            ),
            FeatureGuideItem(
                title = "Recurring Bills",
                description = "Auto-log repeating bills to Expenses on schedule."
            ),
            FeatureGuideItem(
                title = "Personal Savings Goals",
                description = "Save toward personal targets with daily pace."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Clear Insights & Calendar",
        iconRes = R.drawable.ic_lucide_chart_column,
        features = listOf(
            FeatureGuideItem(
                title = "Calm Home Overview",
                description = "View net balance, cutoff pay, and activity with privacy toggle."
            ),
            FeatureGuideItem(
                title = "Visual Spending & Earning Trends",
                description = "Compare income, expenses, and patterns clearly."
            ),
            FeatureGuideItem(
                title = "All-in-One Activity Calendar",
                description = "Inspect logged shifts and expenses on one calendar."
            )
        )
    ),
    FeatureGuideCategory(
        title = "Privacy & Peace of Mind",
        iconRes = R.drawable.ic_lucide_lock,
        features = listOf(
            FeatureGuideItem(
                title = "Private & Works Offline",
                description = "Works 100% offline on your device."
            ),
            FeatureGuideItem(
                title = "PIN & Biometric App Lock",
                description = "Protect your app with a clean PIN keypad or biometrics."
            ),
            FeatureGuideItem(
                title = "Easy Backup & Spreadsheet Export",
                description = "Back up JSON or export CSV spreadsheets anytime."
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
    var expandedIndices by remember { mutableStateOf(emptySet<Int>()) }

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
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp, bottom = 6.dp),
                    contentAlignment = Alignment.Center
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
                color = colors.azurePrimary,
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
        shape = ShiftlyShapes.Card,
        color = colors.cardElevated,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(ShiftlyShapes.Card)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 48.dp)
                    .clickable(onClick = onToggle)
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ShiftlyAnimatedIcon(
                    iconRes = category.iconRes,
                    contentDescription = null,
                    tint = if (isExpanded) colors.azureAccent else colors.iconNeutral,
                    triggerKey = isExpanded,
                    modifier = Modifier.size(20.dp)
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
                ShiftlyAnimatedIcon(
                    iconRes = R.drawable.ic_lucide_chevron_right,
                    contentDescription = if (isExpanded) "Collapse ${category.title}" else "Expand ${category.title}",
                    tint = colors.textSecondary,
                    triggerKey = isExpanded,
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
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 14.dp),
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
                                color = colors.textSecondary
                            )
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
    subtitle: String? = null,
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
                    if (!subtitle.isNullOrBlank()) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.bodySmall.copy(
                                fontFamily = InterFontFamily,
                                fontSize = 13.sp,
                                lineHeight = 18.sp
                            ),
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Surface(
                    shape = ShiftlyShapes.Card,
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        steps.forEachIndexed { index, (stepTitle, stepDescription) ->
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
                                        .background(colors.azureContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        ).tabular(),
                                        color = colors.azureAccent
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
                                        color = colors.textSecondary
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
                color = colors.azurePrimary,
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
    val cardColor = if (colors.isDark) colors.card else colors.surface
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
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .widthIn(max = 392.dp)
                        .fillMaxWidth()
                        .animateContentSize(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioNoBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
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
                                color = colors.azureContainer,
                                shadowElevation = 0.dp
                            ) {
                                Text(
                                    text = "${stepIndex + 1} of $totalSteps",
                                    style = MaterialTheme.typography.labelMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 12.sp
                                    ).tabular(),
                                    color = colors.azureAccent,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .shiftlyPressClickable(onClick = onSkip)
                                    .padding(horizontal = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Skip",
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    ),
                                    color = colors.textSecondary
                                )
                            }
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
                                    shape = PrimaryButtonShape,
                                    color = colors.cardElevated,
                                    shadowElevation = 0.dp,
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(48.dp)
                                        .clip(PrimaryButtonShape)
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
                                shape = PrimaryButtonShape,
                                color = colors.azurePrimary,
                                shadowElevation = 0.dp,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .clip(PrimaryButtonShape)
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

/**
 * Action descriptor for horizontal swipe gestures in [ShiftlySwipeActionRow].
 */
data class ShiftlySwipeAction(
    @DrawableRes val iconRes: Int,
    val label: String,
    val backgroundColor: Color,
    val contentColor: Color = PureWhite,
    val onTrigger: () -> Unit
)

/**
 * Horizontal swipe-to-act wrapper that reveals a tinted action strip with spring snap-back.
 */
@Composable
fun ShiftlySwipeActionRow(
    modifier: Modifier = Modifier,
    shape: Shape = ShiftlyShapes.Card,
    startToEndAction: ShiftlySwipeAction? = null,
    endToStartAction: ShiftlySwipeAction? = null,
    content: @Composable () -> Unit
) {
    val density = LocalDensity.current
    val haptics = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentStartAction by rememberUpdatedState(startToEndAction)
    val currentEndAction by rememberUpdatedState(endToStartAction)

    val maxSwipePx = remember(density) { with(density) { 112.dp.toPx() } }
    val triggerThresholdPx = remember(density) { with(density) { 72.dp.toPx() } }
    val offsetX = remember { Animatable(0f) }
    var crossedThreshold by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
    ) {
        val currentOffset = offsetX.value
        if (currentOffset > 2f && startToEndAction != null) {
            val isArmed = currentOffset >= triggerThresholdPx
            val iconScale by animateFloatAsState(
                targetValue = if (isArmed) 1.12f else 0.85f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "swipeStartIconScale"
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(startToEndAction.backgroundColor)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ShiftlyAnimatedIcon(
                        iconRes = startToEndAction.iconRes,
                        contentDescription = null,
                        tint = startToEndAction.contentColor,
                        triggerKey = if (isArmed) true else null,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                    Text(
                        text = startToEndAction.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = startToEndAction.contentColor
                    )
                }
            }
        } else if (currentOffset < -2f && endToStartAction != null) {
            val isArmed = -currentOffset >= triggerThresholdPx
            val iconScale by animateFloatAsState(
                targetValue = if (isArmed) 1.12f else 0.85f,
                animationSpec = spring(
                    dampingRatio = 0.7f,
                    stiffness = Spring.StiffnessMedium
                ),
                label = "swipeEndIconScale"
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(endToStartAction.backgroundColor)
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterEnd
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = endToStartAction.label,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = endToStartAction.contentColor
                    )
                    ShiftlyAnimatedIcon(
                        iconRes = endToStartAction.iconRes,
                        contentDescription = null,
                        tint = endToStartAction.contentColor,
                        triggerKey = if (isArmed) true else null,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer {
                                scaleX = iconScale
                                scaleY = iconScale
                            }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                .pointerInput(startToEndAction != null, endToStartAction != null) {
                    detectHorizontalDragGestures(
                        onDragStart = { crossedThreshold = false },
                        onHorizontalDrag = { change, dragAmount ->
                            val minBound = if (currentEndAction != null) -maxSwipePx else 0f
                            val maxBound = if (currentStartAction != null) maxSwipePx else 0f
                            val next = (offsetX.value + dragAmount).coerceIn(minBound, maxBound)
                            if (abs(next) > 0f) {
                                change.consume()
                            }
                            scope.launch { offsetX.snapTo(next) }
                            if (abs(next) >= triggerThresholdPx && !crossedThreshold) {
                                crossedThreshold = true
                                haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            } else if (abs(next) < triggerThresholdPx * 0.85f) {
                                crossedThreshold = false
                            }
                        },
                        onDragEnd = {
                            val finalOffset = offsetX.value
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.78f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                            val startAction = currentStartAction
                            val endAction = currentEndAction
                            if (finalOffset >= triggerThresholdPx && startAction != null) {
                                startAction.onTrigger()
                            } else if (finalOffset <= -triggerThresholdPx && endAction != null) {
                                endAction.onTrigger()
                            }
                        },
                        onDragCancel = {
                            scope.launch {
                                offsetX.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = 0.78f,
                                        stiffness = Spring.StiffnessMediumLow
                                    )
                                )
                            }
                        }
                    )
                }
        ) {
            content()
        }
    }
}

private data class ConfettiParticle(
    val angleRad: Float,
    val speedFactor: Float,
    val sizeDp: Float,
    val aspectRatio: Float,
    val isCircle: Boolean,
    val rotationOffsetDeg: Float,
    val rotationSpeedDeg: Float,
    val colorIndex: Int
)

/**
 * Pure-Compose celebratory confetti particle burst triggered whenever [triggerCount] increments above 0.
 */
@Composable
fun ShiftlyConfettiOverlay(
    triggerCount: Int,
    modifier: Modifier = Modifier
) {
    if (triggerCount <= 0) return

    val colors = ShiftlyColors.current
    val palette = remember(colors) {
        listOf(
            colors.mintBright,
            colors.azurePrimary,
            colors.iosOrange,
            colors.nightDiffPurple,
            colors.coralBright,
            PureWhite
        )
    }
    val particles = remember(triggerCount) {
        val rng = Random(triggerCount * 31 + 7)
        List(48) {
            val angleDeg = -165f + rng.nextFloat() * 150f
            ConfettiParticle(
                angleRad = Math.toRadians(angleDeg.toDouble()).toFloat(),
                speedFactor = 0.45f + rng.nextFloat() * 0.75f,
                sizeDp = 5f + rng.nextFloat() * 4.5f,
                aspectRatio = 0.5f + rng.nextFloat() * 0.4f,
                isCircle = rng.nextFloat() < 0.35f,
                rotationOffsetDeg = rng.nextFloat() * 360f,
                rotationSpeedDeg = (rng.nextFloat() - 0.5f) * 720f,
                colorIndex = rng.nextInt(6)
            )
        }
    }

    val anim = remember(triggerCount) { Animatable(0f) }
    LaunchedEffect(triggerCount) {
        anim.snapTo(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1500, easing = LinearEasing)
        )
    }

    val t = anim.value
    if (t <= 0.001f || t >= 0.999f) return

    Canvas(modifier = modifier) {
        val originX = size.width * 0.5f
        val originY = size.height * 0.28f
        val maxSpeed = size.minDimension * 1.15f
        val gravity = size.height * 1.65f
        val easeDrag = 1f - (1f - t) * (1f - t)
        val alpha = if (t < 0.65f) 1f else ((1f - t) / 0.35f).coerceIn(0f, 1f)

        particles.forEach { p ->
            val speed = maxSpeed * p.speedFactor
            val vx = cos(p.angleRad) * speed
            val vy = sin(p.angleRad) * speed
            val x = originX + vx * easeDrag
            val y = originY + vy * easeDrag + 0.5f * gravity * t * t
            val sizePx = p.sizeDp.dp.toPx()
            val particleColor = palette[p.colorIndex % palette.size].copy(alpha = alpha)

            if (p.isCircle) {
                drawCircle(
                    color = particleColor,
                    radius = sizePx * 0.5f,
                    center = Offset(x, y)
                )
            } else {
                val widthPx = sizePx * 1.35f
                val heightPx = sizePx * p.aspectRatio
                rotate(
                    degrees = p.rotationOffsetDeg + p.rotationSpeedDeg * t,
                    pivot = Offset(x, y)
                ) {
                    drawRect(
                        color = particleColor,
                        topLeft = Offset(x - widthPx * 0.5f, y - heightPx * 0.5f),
                        size = Size(widthPx, heightPx)
                    )
                }
            }
        }
    }
}
