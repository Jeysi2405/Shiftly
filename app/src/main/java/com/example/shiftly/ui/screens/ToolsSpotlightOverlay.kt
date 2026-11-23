package com.example.shiftly.ui.screens

import android.os.Build
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp as lerpColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.util.lerp
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.icons.isSystemAnimationEnabled
import com.example.shiftly.ui.theme.ShiftlyColors
import kotlin.math.roundToInt
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

internal data class ToolSpotlightRequest(
    val id: Long,
    val rowKey: String,
    @DrawableRes val iconRes: Int,
    val sourceBoundsInRoot: Rect,
    val isBottomSheet: Boolean,
    val onLaunch: () -> Unit
)

@Composable
internal fun ToolsSpotlightOverlay(
    request: ToolSpotlightRequest?,
    flightProgress: Animatable<Float, AnimationVector1D>,
    containerOriginInRoot: Offset,
    containerSize: IntSize,
    onFinished: () -> Unit
) {
    if (request == null) return

    val context = LocalContext.current
    val animationsEnabled = remember(context) { isSystemAnimationEnabled(context) }
    val zoomThroughProgress = remember(request.id) { Animatable(0f) }
    val overlayAlpha = remember(request.id) { Animatable(1f) }
    val animationDoneSignal = remember(request.id) { CompletableDeferred<Unit>() }
    var playIconTrigger by remember(request.id) { mutableIntStateOf(0) }
    var launchedTarget by remember(request.id) { mutableStateOf(false) }

    DisposableEffect(request.id) {
        onDispose {
            animationDoneSignal.complete(Unit)
            if (!launchedTarget) {
                launchedTarget = true
            }
        }
    }

    LaunchedEffect(request.id) {
        if (!animationsEnabled || request.sourceBoundsInRoot.isEmpty || containerSize.width <= 0) {
            flightProgress.snapTo(0f)
            zoomThroughProgress.snapTo(0f)
            overlayAlpha.snapTo(0f)
            launchedTarget = true
            request.onLaunch()
            onFinished()
            return@LaunchedEffect
        }

        flightProgress.snapTo(0f)
        zoomThroughProgress.snapTo(0f)
        overlayAlpha.snapTo(1f)

        // Stage 1: Fly bare icon with smooth velocity from row position to visual center.
        flightProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = 0.76f,
                stiffness = 220f
            ),
            initialVelocity = 2.0f
        )

        // Stage 2: Play the complete Lucide icon animation at center without cutting it short.
        playIconTrigger++
        withTimeoutOrNull(2000L) {
            animationDoneSignal.await()
        }

        // Stage 3: Full-screen zoom rushing toward the viewer while launching target.
        coroutineScope {
            launch {
                zoomThroughProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(
                        durationMillis = 360,
                        easing = FastOutLinearInEasing
                    )
                )
            }
            launch {
                delay(220L)
                launchedTarget = true
                request.onLaunch()
            }
        }

        // Clean release of background scrim & blur after zoom-through handoff.
        if (request.isBottomSheet) {
            coroutineScope {
                launch { flightProgress.animateTo(0f, tween(durationMillis = 140)) }
                launch { overlayAlpha.animateTo(0f, tween(durationMillis = 120)) }
            }
        } else {
            delay(160L)
            flightProgress.snapTo(0f)
            zoomThroughProgress.snapTo(0f)
            overlayAlpha.snapTo(0f)
        }
        onFinished()
    }

    val colors = ShiftlyColors.current
    val density = LocalDensity.current
    val navBarBottomPx = with(density) {
        (68.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()).toPx()
    }

    val flightP = flightProgress.value
    val clampedFlightP = flightP.coerceIn(0f, 1f)
    val z = zoomThroughProgress.value.coerceIn(0f, 1f)
    val alpha = overlayAlpha.value.coerceIn(0f, 1f)
    val scrimMaxAlpha = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) 0.44f else 0.60f

    val currentBaseDp = lerp(18.dp, 64.dp, flightP.coerceIn(0f, 1.12f)).coerceAtLeast(18.dp)
    val currentBasePx = with(density) { currentBaseDp.toPx() }

    val startCenter = Offset(
        x = request.sourceBoundsInRoot.center.x - containerOriginInRoot.x,
        y = request.sourceBoundsInRoot.center.y - containerOriginInRoot.y
    )
    val visualCenter = Offset(
        x = containerSize.width / 2f,
        y = ((containerSize.height - navBarBottomPx).coerceAtLeast(currentBasePx)) / 2f
    )
    val currentCenter = Offset(
        x = lerp(startCenter.x, visualCenter.x, flightP),
        y = lerp(startCenter.y, visualCenter.y, flightP)
    )
    val currentTopLeft = Offset(
        x = currentCenter.x - currentBasePx / 2f,
        y = currentCenter.y - currentBasePx / 2f
    )

    val zoomScale = lerp(1f, 15f, z * z)
    val zoomFade = 1f - ((z - 0.35f) / 0.65f).coerceIn(0f, 1f)
    val iconTint = lerpColor(colors.iconNeutral, colors.textPrimary, clampedFlightP)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .pointerInput(request.id) {
                // Consume pointer events while spotlight animation is active (double-tap guard).
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    }
                }
            }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = scrimMaxAlpha * clampedFlightP * alpha))
        )

        Box(
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
                    this.alpha = zoomFade * alpha
                },
            contentAlignment = Alignment.Center
        ) {
            ShiftlyAnimatedIcon(
                iconRes = request.iconRes,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(currentBaseDp),
                triggerKey = if (playIconTrigger > 0) playIconTrigger else null,
                animateOnMount = false,
                repeatIntervalMillis = 0L,
                onAnimationEnd = {
                    if (playIconTrigger > 0) {
                        animationDoneSignal.complete(Unit)
                    }
                }
            )
        }
    }
}
