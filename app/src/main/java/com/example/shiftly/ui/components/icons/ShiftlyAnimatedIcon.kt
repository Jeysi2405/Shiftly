package com.example.shiftly.ui.components.icons

import android.content.Context
import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.example.shiftly.R
import kotlin.math.PI
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

internal fun isSystemAnimationEnabled(context: Context): Boolean = runCatching {
    Settings.Global.getFloat(
        context.contentResolver,
        Settings.Global.ANIMATOR_DURATION_SCALE,
        1f
    ) > 0f
}.getOrDefault(true)

internal fun parseLucidePath(pathData: String): Path =
    PathParser().parsePathString(pathData).toPath()

internal class LucideDrawContext(
    val tint: Color,
    val progress: Float,
    val isAnimating: Boolean,
    val scratchPath: Path,
    val pathMeasure: PathMeasure
)

internal val LUCIDE_STROKE = Stroke(
    width = 2f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
)

internal val LUCIDE_SELECTED_FILL_STROKE = Stroke(
    width = 1.6f,
    cap = StrokeCap.Round,
    join = StrokeJoin.Round
)

internal fun DrawScope.drawLucide24(block: DrawScope.() -> Unit) {
    val sx = size.width / 24f
    val sy = size.height / 24f
    withTransform({ scale(sx, sy, Offset.Zero) }) {
        block()
    }
}

internal fun DrawScope.drawTrimmed(
    source: Path,
    ctx: LucideDrawContext,
    stopFraction: Float,
    startFraction: Float = 0f,
    stroke: Stroke = LUCIDE_STROKE
) {
    val start = startFraction.coerceIn(0f, 1f)
    val stop = stopFraction.coerceIn(start, 1f)
    if (stop <= start) return
    if (start <= 0f && stop >= 1f) {
        drawPath(source, color = ctx.tint, style = stroke)
        return
    }
    ctx.pathMeasure.setPath(source, false)
    val length = ctx.pathMeasure.length
    if (length <= 0f) return
    ctx.scratchPath.reset()
    ctx.pathMeasure.getSegment(
        startDistance = length * start,
        stopDistance = length * stop,
        destination = ctx.scratchPath,
        startWithMoveTo = true
    )
    drawPath(ctx.scratchPath, color = ctx.tint, style = stroke)
}

/** Smooth 0 -> 1 -> 0 arch across the 0..1 window with elastic follow-through on spring overshoot. */
internal fun archEnvelope(ctx: LucideDrawContext): Float =
    if (!ctx.isAnimating) 0f else sin(ctx.progress * PI).toFloat()

/** Damped oscillation wave starting and ending cleanly at 0 (0 when idle). */
internal fun dampedWave(ctx: LucideDrawContext, halfCycles: Float = 3f): Float {
    if (!ctx.isAnimating) return 0f
    val decay = (1f - ctx.progress * 0.85f).coerceAtLeast(0f)
    return (sin(ctx.progress * halfCycles * PI) * decay).toFloat()
}

/** Staggered 0..1 progress for stroke-draw icons (returns 1f when idle). */
internal fun stagger(ctx: LucideDrawContext, start: Float, end: Float): Float {
    if (!ctx.isAnimating) return 1f
    val span = (end - start).coerceAtLeast(0.01f)
    return ((ctx.progress - start) / span).coerceIn(0f, 1f)
}

/** Eased 0..1 curve with expressive spring overshoot that rests at 1f when idle. */
internal fun bounceGrow(ctx: LucideDrawContext, start: Float, end: Float): Float {
    if (!ctx.isAnimating) return 1f
    val t = stagger(ctx, start, end)
    val c1 = 2.15f
    val c3 = c1 + 1f
    val u = t - 1f
    return (1f + c3 * u * u * u + c1 * u * u).coerceIn(0.12f, 1.25f)
}

private fun usesStrokeDrawTiming(@DrawableRes iconRes: Int): Boolean = when (iconRes) {
    R.drawable.ic_lucide_check,
    R.drawable.ic_lucide_circle_check,
    R.drawable.ic_lucide_eye_off,
    R.drawable.ic_lucide_fingerprint,
    R.drawable.ic_lucide_receipt_text,
    R.drawable.ic_lucide_file_text,
    R.drawable.ic_lucide_split,
    R.drawable.ic_lucide_trending_up,
    R.drawable.ic_lucide_trending_down,
    R.drawable.ic_lucide_calendar -> true
    else -> false
}

private typealias LucideCanvasDrawer = DrawScope.(LucideDrawContext) -> Unit

private fun tier1DrawerFor(@DrawableRes iconRes: Int): LucideCanvasDrawer? = when (iconRes) {
    // Bottom Nav (4 + 4 selected variants)
    R.drawable.ic_lucide_house -> DrawScope::drawHouse
    R.drawable.ic_lucide_house_selected -> DrawScope::drawHouseSelected
    R.drawable.ic_lucide_chart_column -> DrawScope::drawChartColumn
    R.drawable.ic_lucide_chart_column_selected -> DrawScope::drawChartColumnSelected
    R.drawable.ic_lucide_layout_grid -> DrawScope::drawLayoutGrid
    R.drawable.ic_lucide_layout_grid_selected -> DrawScope::drawLayoutGridSelected
    R.drawable.ic_lucide_user -> DrawScope::drawUser
    R.drawable.ic_lucide_user_selected -> DrawScope::drawUserSelected

    // Header & Controls (7)
    R.drawable.ic_lucide_plus -> DrawScope::drawPlus
    R.drawable.ic_lucide_bell -> DrawScope::drawBell
    R.drawable.ic_lucide_settings -> DrawScope::drawSettings
    R.drawable.ic_lucide_eye -> DrawScope::drawEye
    R.drawable.ic_lucide_eye_off -> DrawScope::drawEyeOff
    R.drawable.ic_lucide_lock -> DrawScope::drawLock
    R.drawable.ic_lucide_fingerprint -> DrawScope::drawFingerprint

    // Actions & Feedback (9)
    R.drawable.ic_lucide_check -> DrawScope::drawCheck
    R.drawable.ic_lucide_circle_check -> DrawScope::drawCircleCheck
    R.drawable.ic_lucide_trash_2 -> DrawScope::drawTrash2
    R.drawable.ic_lucide_pencil -> DrawScope::drawPencil
    R.drawable.ic_lucide_square_pen -> DrawScope::drawSquarePen
    R.drawable.ic_lucide_rotate_ccw -> DrawScope::drawRotateCcw
    R.drawable.ic_lucide_arrow_left -> DrawScope::drawArrowLeft
    R.drawable.ic_lucide_chevron_left -> DrawScope::drawChevronLeft
    R.drawable.ic_lucide_chevron_right -> DrawScope::drawChevronRight

    // Tools (11) + Trend & Streak (3)
    R.drawable.ic_lucide_clock -> DrawScope::drawClock
    R.drawable.ic_lucide_receipt_text -> DrawScope::drawReceiptText
    R.drawable.ic_lucide_split -> DrawScope::drawSplit
    R.drawable.ic_lucide_circle_dollar_sign -> DrawScope::drawCircleDollarSign
    R.drawable.ic_lucide_file_text -> DrawScope::drawFileText
    R.drawable.ic_lucide_tags -> DrawScope::drawTags
    R.drawable.ic_lucide_piggy_bank -> DrawScope::drawPiggyBank
    R.drawable.ic_lucide_repeat -> DrawScope::drawRepeat
    R.drawable.ic_lucide_wallet -> DrawScope::drawWallet
    R.drawable.ic_lucide_banknote -> DrawScope::drawBanknote
    R.drawable.ic_lucide_calendar -> DrawScope::drawCalendar
    R.drawable.ic_lucide_trending_up -> DrawScope::drawTrendingUp
    R.drawable.ic_lucide_trending_down -> DrawScope::drawTrendingDown
    R.drawable.ic_lucide_flame -> DrawScope::drawFlame

    else -> null
}

/**
 * Centralized Lucide icon renderer with two performance-safe tiers:
 * - Tier 1: multi-part & PathMeasure Canvas animations styled after lucide-animated (pqoqubbw/icons).
 * - Tier 2: static vector drawable with spring velocity bounce/tilt on first appear or tap.
 *
 * Animations run once when first shown ([animateOnMount] defaults to true) or when [triggerKey]
 * changes, never loop continuously while idle, and respect system animator duration scale.
 */
@Composable
fun ShiftlyAnimatedIcon(
    @DrawableRes iconRes: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
    triggerKey: Any? = null,
    animateOnMount: Boolean = true,
    repeatIntervalMillis: Long = 2000L,
    burstCount: Int = 3,
    cooldownMillis: Long = 10_000L,
    onAnimationEnd: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val animationsEnabled = remember(context) { isSystemAnimationEnabled(context) }
    val progress = remember { Animatable(0f) }
    var isAnimating by remember { mutableStateOf(false) }
    var hasMounted by remember { mutableStateOf(false) }
    val latestOnAnimationEnd by rememberUpdatedState(onAnimationEnd)

    val isTier1 = remember(iconRes) { tier1DrawerFor(iconRes) != null }

    LaunchedEffect(
        triggerKey,
        animateOnMount,
        repeatIntervalMillis,
        burstCount,
        cooldownMillis,
        animationsEnabled
    ) {
        val shouldAnimate = if (!hasMounted) {
            hasMounted = true
            animateOnMount
        } else {
            triggerKey != null
        }
        if (!animationsEnabled) {
            isAnimating = false
            progress.snapTo(0f)
            if (shouldAnimate) {
                latestOnAnimationEnd?.invoke()
            }
            return@LaunchedEffect
        }

        suspend fun runOnce() {
            isAnimating = true
            progress.snapTo(0f)
            try {
                if (!isTier1) {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.60f,
                            stiffness = 120f
                        ),
                        initialVelocity = 1.6f
                    )
                } else if (usesStrokeDrawTiming(iconRes)) {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing)
                    )
                } else {
                    progress.animateTo(
                        targetValue = 1f,
                        animationSpec = spring(
                            dampingRatio = 0.62f,
                            stiffness = 100f
                        ),
                        initialVelocity = 1.4f
                    )
                }
                latestOnAnimationEnd?.invoke()
            } finally {
                isAnimating = false
                progress.snapTo(0f)
            }
        }

        var playedInBurst = 0
        if (shouldAnimate) {
            runOnce()
            playedInBurst = 1
        }
        if (repeatIntervalMillis > 0L) {
            val safeBurst = burstCount.coerceAtLeast(1)
            while (isActive) {
                if (playedInBurst < safeBurst) {
                    delay(repeatIntervalMillis)
                    runOnce()
                    playedInBurst++
                } else {
                    delay(cooldownMillis.coerceAtLeast(0L))
                    playedInBurst = 0
                    runOnce()
                    playedInBurst = 1
                }
            }
        }
    }

    val semanticsModifier = if (contentDescription != null) {
        Modifier.semantics {
            this.contentDescription = contentDescription
            this.role = Role.Image
        }
    } else {
        Modifier
    }

    val tier1Drawer = remember(iconRes) { tier1DrawerFor(iconRes) }
    if (tier1Drawer != null) {
        val scratchPath = remember { Path() }
        val pathMeasure = remember { PathMeasure() }
        Canvas(modifier = modifier.then(semanticsModifier)) {
            val drawCtx = LucideDrawContext(
                tint = tint,
                progress = progress.value,
                isAnimating = isAnimating,
                scratchPath = scratchPath,
                pathMeasure = pathMeasure
            )
            drawLucide24 {
                tier1Drawer(drawCtx)
            }
        }
    } else {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = contentDescription,
            tint = tint,
            modifier = modifier.graphicsLayer {
                if (isAnimating) {
                    val p = progress.value
                    val arch = sin(p * PI).toFloat()
                    val decay = (1f - p * 0.85f).coerceAtLeast(0f)
                    val tilt = (sin(p * 2f * PI) * decay).toFloat()
                    val s = 1f + 0.18f * arch
                    scaleX = s
                    scaleY = s
                    rotationZ = 9f * tilt
                }
            }
        )
    }
}
