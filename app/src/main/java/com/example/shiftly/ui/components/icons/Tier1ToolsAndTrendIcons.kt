package com.example.shiftly.ui.components.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform

// -----------------------------------------------------------------------------
// Tools (11) & Trend / Streak (3) Icons
// -----------------------------------------------------------------------------

private val clockFacePath by lazy {
    parseLucidePath("M2,12 a10,10 0 1,0 20,0 a10,10 0 1,0 -20,0")
}
private val clockHourHandPath by lazy { parseLucidePath("M12,6 v6") }
private val clockMinuteHandPath by lazy { parseLucidePath("M12,12 l4,2") }

internal fun DrawScope.drawClock(ctx: LucideDrawContext) {
    val facePop = 1f + 0.08f * archEnvelope(ctx)
    withTransform({ scale(facePop, facePop, pivot = Offset(12f, 12f)) }) {
        drawPath(clockFacePath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    val active = ctx.isAnimating
    val minuteAngle = if (active) 360f * ctx.progress else 0f
    val hourAngle = if (active) 45f * archEnvelope(ctx) else 0f
    withTransform({ rotate(minuteAngle, pivot = Offset(12f, 12f)) }) {
        drawPath(clockHourHandPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ rotate(hourAngle, pivot = Offset(12f, 12f)) }) {
        drawPath(clockMinuteHandPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val receiptBodyPath by lazy {
    parseLucidePath("M4,2 v20 l2,-1 l2,1 l2,-1 l2,1 l2,-1 l2,1 l2,-1 l2,1 V2 l-2,1 l-2,-1 l-2,1 l-2,-1 l-2,1 l-2,-1 l-2,1 Z")
}
private val receiptLine1Path by lazy { parseLucidePath("M8,8 H14") }
private val receiptLine2Path by lazy { parseLucidePath("M8,12 H16") }
private val receiptLine3Path by lazy { parseLucidePath("M8,16 H13") }

internal fun DrawScope.drawReceiptText(ctx: LucideDrawContext) {
    val lift = -1.5f * archEnvelope(ctx)
    withTransform({ translate(top = lift) }) {
        drawPath(receiptBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawTrimmed(receiptLine1Path, ctx, stopFraction = stagger(ctx, 0.04f, 0.48f))
        drawTrimmed(receiptLine2Path, ctx, stopFraction = stagger(ctx, 0.24f, 0.72f))
        drawTrimmed(receiptLine3Path, ctx, stopFraction = stagger(ctx, 0.44f, 0.94f))
    }
}

private val splitRightStemPath by lazy { parseLucidePath("M15,9 l6,-6") }
private val splitRightHeadPath by lazy { parseLucidePath("M16,3 h5 v5") }
private val splitLeftStemPath by lazy {
    parseLucidePath("M12,22 v-8.3 a4,4 0 0,0 -1.172,-2.872 L3,3")
}
private val splitLeftHeadPath by lazy { parseLucidePath("M8,3 H3 v5") }

internal fun DrawScope.drawSplit(ctx: LucideDrawContext) {
    val lift = -0.8f * archEnvelope(ctx)
    withTransform({ translate(top = lift) }) {
        drawTrimmed(splitLeftStemPath, ctx, stopFraction = stagger(ctx, 0f, 0.76f))
        drawTrimmed(splitLeftHeadPath, ctx, stopFraction = stagger(ctx, 0.54f, 0.95f))
        drawTrimmed(splitRightStemPath, ctx, stopFraction = stagger(ctx, 0.14f, 0.85f))
        drawTrimmed(splitRightHeadPath, ctx, stopFraction = stagger(ctx, 0.60f, 0.98f))
    }
}

private val coinRingPath by lazy {
    parseLucidePath("M2,12 a10,10 0 1,0 20,0 a10,10 0 1,0 -20,0")
}
private val coinDollarSPath by lazy {
    parseLucidePath("M16,8 h-6 a2,2 0 1,0 0,4 h4 a2,2 0 1,1 0,4 H8")
}
private val coinDollarBarPath by lazy { parseLucidePath("M12,18 V6") }

internal fun DrawScope.drawCircleDollarSign(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val coinScaleX = (1f - 0.55f * env).coerceAtLeast(0.22f)
    withTransform({ scale(coinScaleX, 1f + 0.10f * env, pivot = Offset(12f, 12f)) }) {
        drawPath(coinRingPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    val innerScale = 1f + 0.16f * env
    withTransform({ scale(innerScale * (1f - 0.25f * env).coerceAtLeast(0.4f), innerScale, pivot = Offset(12f, 12f)) }) {
        drawPath(coinDollarBarPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(coinDollarSPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val fileBodyPath by lazy {
    parseLucidePath("M15,2 H6 a2,2 0 0,0 -2,2 v16 a2,2 0 0,0 2,2 h12 a2,2 0 0,0 2,-2 V7 Z")
}
private val fileFoldPath by lazy { parseLucidePath("M14,2 v4 a2,2 0 0,0 2,2 h4") }
private val fileLine1Path by lazy { parseLucidePath("M8,9 H10") }
private val fileLine2Path by lazy { parseLucidePath("M8,13 H16") }
private val fileLine3Path by lazy { parseLucidePath("M8,17 H16") }

internal fun DrawScope.drawFileText(ctx: LucideDrawContext) {
    val lift = -1.0f * archEnvelope(ctx)
    withTransform({ translate(top = lift) }) {
        drawPath(fileBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(fileFoldPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawTrimmed(fileLine1Path, ctx, stopFraction = stagger(ctx, 0.04f, 0.45f))
        drawTrimmed(fileLine2Path, ctx, stopFraction = stagger(ctx, 0.24f, 0.72f))
        drawTrimmed(fileLine3Path, ctx, stopFraction = stagger(ctx, 0.46f, 0.95f))
    }
}

private val tagsBackPath by lazy {
    parseLucidePath("M15,5 l6.3,6.3 a2.4,2.4 0 0,1 0,3.4 L17,19")
}
private val tagsFrontPath by lazy {
    parseLucidePath("M9.586,5.586 A2,2 0 0,0 8.172,5 H3 a1,1 0 0,0 -1,1 v5.172 a2,2 0 0,0 0.586,1.414 L8.29,18.29 a2.426,2.426 0 0,0 3.42,0 l3.58,-3.58 a2.426,2.426 0 0,0 0,-3.42 z")
}
private val tagsEyeletPath by lazy {
    parseLucidePath("M6,9.5 a0.5,0.5 0 1,0 1,0 a0.5,0.5 0 1,0 -1,0")
}

internal fun DrawScope.drawTags(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val swing = dampedWave(ctx, halfCycles = 3f)
    withTransform({
        translate(left = 1.2f * env, top = -0.8f * env)
        rotate(14f * env, pivot = Offset(10f, 10f))
    }) {
        drawPath(tagsBackPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ rotate(-13f * swing, pivot = Offset(7.5f, 7.5f)) }) {
        drawPath(tagsFrontPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(tagsEyeletPath, color = ctx.tint, style = Fill)
        drawPath(tagsEyeletPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val piggyBodyPath by lazy {
    parseLucidePath("M19,5 c-1.5,0 -2.8,1.4 -3,2 c-3.5,-1.5 -11,-0.3 -11,5 c0,1.8 0,3 2,4.5 V20 h4 v-2 h3 v2 h4 v-4 c1,-0.5 1.7,-1 2,-2 h2 v-4 h-2 c0,-1 -0.5,-1.5 -1,-2 V5 z")
}
private val piggySnoutPath by lazy { parseLucidePath("M2,9 v1 c0,1.1 0.9,2 2,2 h1") }

internal fun DrawScope.drawPiggyBank(ctx: LucideDrawContext) {
    val hop = archEnvelope(ctx)
    val sx = 1f - 0.08f * hop
    val sy = 1f + 0.12f * hop
    withTransform({
        translate(top = -3.2f * hop)
        scale(sx, sy, pivot = Offset(12f, 20f))
    }) {
        drawPath(piggyBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(piggySnoutPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawCircle(color = ctx.tint, radius = LUCIDE_STROKE.width / 2f, center = Offset(16f, 11f))
    }
}

private val repeatTopHeadPath by lazy { parseLucidePath("M17,2 l4,4 l-4,4") }
private val repeatTopStemPath by lazy { parseLucidePath("M3,11 v-1 a4,4 0 0,1 4,-4 h14") }
private val repeatBottomHeadPath by lazy { parseLucidePath("M7,22 l-4,-4 l4,-4") }
private val repeatBottomStemPath by lazy { parseLucidePath("M21,13 v1 a4,4 0 0,1 -4,4 H3") }

internal fun DrawScope.drawRepeat(ctx: LucideDrawContext) {
    val shift = 2.8f * archEnvelope(ctx)
    withTransform({ translate(left = shift) }) {
        drawPath(repeatTopStemPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(repeatTopHeadPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ translate(left = -shift) }) {
        drawPath(repeatBottomStemPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(repeatBottomHeadPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val walletFlapPath by lazy {
    parseLucidePath("M19,7 V4 a1,1 0 0,0 -1,-1 H5 a2,2 0 0,0 0,4 h15 a1,1 0 0,1 1,1 v4 h-3 a2,2 0 0,0 0,4 h3 a1,1 0 0,0 1,-1 v-2 a1,1 0 0,0 -1,-1")
}
private val walletPocketPath by lazy {
    parseLucidePath("M3,5 v14 a2,2 0 0,0 2,2 h15 a1,1 0 0,0 1,-1 v-4")
}

internal fun DrawScope.drawWallet(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val tilt = -9f * dampedWave(ctx, halfCycles = 2.5f)
    withTransform({ rotate(tilt, pivot = Offset(12f, 14f)) }) {
        drawPath(walletPocketPath, color = ctx.tint, style = LUCIDE_STROKE)
        withTransform({ translate(top = -1.8f * env) }) {
            drawPath(walletFlapPath, color = ctx.tint, style = LUCIDE_STROKE)
        }
    }
}

private val banknoteBodyPath by lazy {
    parseLucidePath("M4,6 H20 A2,2 0 0,1 22,8 V16 A2,2 0 0,1 20,18 H4 A2,2 0 0,1 2,16 V8 A2,2 0 0,1 4,6 Z")
}
private val banknoteCirclePath by lazy {
    parseLucidePath("M10,12 a2,2 0 1,0 4,0 a2,2 0 1,0 -4,0")
}

internal fun DrawScope.drawBanknote(ctx: LucideDrawContext) {
    val wave = dampedWave(ctx, halfCycles = 2.5f)
    val env = archEnvelope(ctx)
    withTransform({ rotate(9f * wave, pivot = Offset(12f, 12f)) }) {
        drawPath(banknoteBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawCircle(color = ctx.tint, radius = LUCIDE_STROKE.width / 2f, center = Offset(6f, 12f))
        drawCircle(color = ctx.tint, radius = LUCIDE_STROKE.width / 2f, center = Offset(18f, 12f))
        val pulse = 1f + 0.24f * env
        withTransform({ scale(pulse, pulse, pivot = Offset(12f, 12f)) }) {
            drawPath(banknoteCirclePath, color = ctx.tint, style = LUCIDE_STROKE)
        }
    }
}

private val calendarLeftRingPath by lazy { parseLucidePath("M8,2 v4") }
private val calendarRightRingPath by lazy { parseLucidePath("M16,2 v4") }
private val calendarFramePath by lazy {
    parseLucidePath("M5,4 H19 A2,2 0 0,1 21,6 V20 A2,2 0 0,1 19,22 H5 A2,2 0 0,1 3,20 V6 A2,2 0 0,1 5,4 Z")
}
private val calendarHeaderRulePath by lazy { parseLucidePath("M3,10 h18") }

internal fun DrawScope.drawCalendar(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val leftRingBounce = if (!ctx.isAnimating) 0f else {
        kotlin.math.sin(stagger(ctx, 0f, 0.55f) * kotlin.math.PI).toFloat()
    }
    drawPath(calendarFramePath, color = ctx.tint, style = LUCIDE_STROKE)
    withTransform({ translate(top = -2.0f * leftRingBounce) }) {
        drawPath(calendarLeftRingPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ translate(top = -2.0f * env) }) {
        drawPath(calendarRightRingPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    drawTrimmed(calendarHeaderRulePath, ctx, stopFraction = stagger(ctx, 0.14f, 0.92f))
}

private val trendingUpLinePath by lazy { parseLucidePath("M2,17 L8.5,10.5 L13.5,15.5 L22,7") }
private val trendingUpHeadPath by lazy { parseLucidePath("M16,7 h6 v6") }
private val trendingDownLinePath by lazy { parseLucidePath("M2,7 L8.5,13.5 L13.5,8.5 L22,17") }
private val trendingDownHeadPath by lazy { parseLucidePath("M16,17 h6 v-6") }

internal fun DrawScope.drawTrendingUp(ctx: LucideDrawContext) {
    drawTrimmed(trendingUpLinePath, ctx, stopFraction = stagger(ctx, 0f, 0.78f))
    drawTrimmed(trendingUpHeadPath, ctx, stopFraction = stagger(ctx, 0.62f, 0.98f))
}

internal fun DrawScope.drawTrendingDown(ctx: LucideDrawContext) {
    drawTrimmed(trendingDownLinePath, ctx, stopFraction = stagger(ctx, 0f, 0.78f))
    drawTrimmed(trendingDownHeadPath, ctx, stopFraction = stagger(ctx, 0.62f, 0.98f))
}

private val flamePath by lazy {
    parseLucidePath("M8.5,14.5 A2.5,2.5 0 0,0 11,12 c0,-1.38 -0.5,-2 -1,-3 c-1.072,-2.143 -0.224,-4.054 2,-6 c0.5,2.5 2,4.9 4,6.5 c2,1.6 3,3.5 3,5.5 a7,7 0 1,1 -14,0 c0,-1.153 0.433,-2.294 1,-3 a2.5,2.5 0 0,0 2.5,2.5 z")
}

internal fun DrawScope.drawFlame(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val flicker = dampedWave(ctx, halfCycles = 3.5f)
    withTransform({
        scale(1f - 0.08f * env, 1f + 0.22f * env, pivot = Offset(12f, 21f))
        rotate(9f * flicker, pivot = Offset(12f, 21f))
    }) {
        drawPath(flamePath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}
