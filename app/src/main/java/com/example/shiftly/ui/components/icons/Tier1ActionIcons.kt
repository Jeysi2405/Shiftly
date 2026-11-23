package com.example.shiftly.ui.components.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform

// -----------------------------------------------------------------------------
// Actions & Feedback Icons (9)
// -----------------------------------------------------------------------------

private val checkLeftToRightPath by lazy {
    parseLucidePath("M4,12 L9,17 L20,6")
}

internal fun DrawScope.drawCheck(ctx: LucideDrawContext) {
    drawTrimmed(checkLeftToRightPath, ctx, stopFraction = stagger(ctx, 0f, 0.92f))
}

private val circleCheckRingPath by lazy {
    parseLucidePath("M2,12 a10,10 0 1,0 20,0 a10,10 0 1,0 -20,0")
}
private val circleCheckTickPath by lazy {
    parseLucidePath("M9,12 l2,2 l4,-4")
}

internal fun DrawScope.drawCircleCheck(ctx: LucideDrawContext) {
    val pop = 1f + 0.14f * archEnvelope(ctx)
    withTransform({ scale(pop, pop, pivot = Offset(12f, 12f)) }) {
        drawPath(circleCheckRingPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    drawTrimmed(circleCheckTickPath, ctx, stopFraction = stagger(ctx, 0.16f, 0.92f))
}

private val trashBarPath by lazy { parseLucidePath("M3,6 h18") }
private val trashHandlePath by lazy { parseLucidePath("M8,6 V4 c0,-1 1,-2 2,-2 h4 c1,0 2,1 2,2 v2") }
private val trashBinPath by lazy { parseLucidePath("M19,6 v14 c0,1 -1,2 -2,2 H7 c-1,0 -2,-1 -2,-2 V6") }
private val trashLeftLinePath by lazy { parseLucidePath("M10,11 v6") }
private val trashRightLinePath by lazy { parseLucidePath("M14,11 v6") }

internal fun DrawScope.drawTrash2(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val lidRot = 28f * env
    withTransform({
        translate(top = -1.8f * env)
        rotate(lidRot, pivot = Offset(19f, 6f))
    }) {
        drawPath(trashBarPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(trashHandlePath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    val binScaleY = 1f - 0.06f * env
    withTransform({ scale(1f, binScaleY, pivot = Offset(12f, 22f)) }) {
        drawPath(trashBinPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(trashLeftLinePath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(trashRightLinePath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val pencilBodyPath by lazy {
    parseLucidePath("M21.174,6.812 a1,1 0 0,0 -3.986,-3.987 L3.842,16.174 a2,2 0 0,0 -0.5,0.83 l-1.321,4.352 a0.5,0.5 0 0,0 0.623,0.622 l4.353,-1.32 a2,2 0 0,0 0.83,-0.497 z")
}
private val pencilCollarPath by lazy { parseLucidePath("M15,5 l4,4") }

internal fun DrawScope.drawPencil(ctx: LucideDrawContext) {
    val wave = dampedWave(ctx, halfCycles = 3.5f)
    val env = archEnvelope(ctx)
    withTransform({
        translate(left = 1.4f * env, top = -1.4f * env)
        rotate(16f * wave, pivot = Offset(4f, 20f))
    }) {
        drawPath(pencilBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(pencilCollarPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val squarePenBoxPath by lazy {
    parseLucidePath("M12,3 H5 a2,2 0 0,0 -2,2 v14 a2,2 0 0,0 2,2 h14 a2,2 0 0,0 2,-2 v-7")
}
private val squarePenNibPath by lazy {
    parseLucidePath("M18.375,2.625 a1,1 0 0,1 3,3 l-9.013,9.014 a2,2 0 0,1 -0.853,0.505 l-2.873,0.84 a0.5,0.5 0 0,1 -0.62,-0.62 l0.84,-2.873 a2,2 0 0,1 0.506,-0.852 z")
}

internal fun DrawScope.drawSquarePen(ctx: LucideDrawContext) {
    drawPath(squarePenBoxPath, color = ctx.tint, style = LUCIDE_STROKE)
    val wave = dampedWave(ctx, halfCycles = 3.5f)
    val env = archEnvelope(ctx)
    withTransform({
        translate(left = 1.3f * wave, top = -1.3f * env)
        rotate(12f * wave, pivot = Offset(10f, 14f))
    }) {
        drawPath(squarePenNibPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val rotateCcwArcPath by lazy {
    parseLucidePath("M3,12 a9,9 0 1,0 9,-9 a9.75,9.75 0 0,0 -6.74,2.74 L3,8")
}
private val rotateCcwHeadPath by lazy { parseLucidePath("M3,3 v5 h5") }

internal fun DrawScope.drawRotateCcw(ctx: LucideDrawContext) {
    val rot = if (!ctx.isAnimating) 0f else -360f * ctx.progress
    val scale = 1f + 0.10f * archEnvelope(ctx)
    withTransform({
        rotate(rot, pivot = Offset(12f, 12f))
        scale(scale, scale, pivot = Offset(12f, 12f))
    }) {
        drawPath(rotateCcwArcPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(rotateCcwHeadPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val arrowLeftHeadPath by lazy { parseLucidePath("M12,19 L5,12 L12,5") }
private val arrowLeftStemPath by lazy { parseLucidePath("M19,12 L5,12") }
private val chevronLeftPath by lazy { parseLucidePath("M15,18 L9,12 L15,6") }
private val chevronRightPath by lazy { parseLucidePath("M9,18 L15,12 L9,6") }

internal fun DrawScope.drawArrowLeft(ctx: LucideDrawContext) {
    val nudge = -3.4f * archEnvelope(ctx)
    withTransform({ translate(left = nudge) }) {
        drawPath(arrowLeftHeadPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ translate(left = nudge * 0.55f) }) {
        drawPath(arrowLeftStemPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

internal fun DrawScope.drawChevronLeft(ctx: LucideDrawContext) {
    val nudge = -3.2f * archEnvelope(ctx)
    withTransform({ translate(left = nudge) }) {
        drawPath(chevronLeftPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

internal fun DrawScope.drawChevronRight(ctx: LucideDrawContext) {
    val nudge = 3.2f * archEnvelope(ctx)
    withTransform({ translate(left = nudge) }) {
        drawPath(chevronRightPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}
