package com.example.shiftly.ui.components.icons

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.withTransform
import kotlin.math.PI
import kotlin.math.sin

// -----------------------------------------------------------------------------
// Bottom Nav Icons (4 + 4 selected variants)
// -----------------------------------------------------------------------------

private val houseOutlinePath by lazy {
    parseLucidePath("M3,10 a2,2 0 0,1 0.709,-1.528 l7,-5.999 a2,2 0 0,1 2.582,0 l7,5.999 A2,2 0 0,1 21,10 v9 a2,2 0 0,1 -2,2 H5 a2,2 0 0,1 -2,-2 Z")
}
private val houseDoorPath by lazy {
    parseLucidePath("M15,21 v-8 a1,1 0 0,0 -1,-1 h-4 a1,1 0 0,0 -1,1 v8")
}
private val houseSelectedPath by lazy {
    parseLucidePath("M3,10 a2,2 0 0,1 0.709,-1.528 l7,-5.999 a2,2 0 0,1 2.582,0 l7,5.999 A2,2 0 0,1 21,10 v9 a2,2 0 0,1 -2,2 H15 v-7 a1,1 0 0,0 -1,-1 h-4 a1,1 0 0,0 -1,1 v7 H5 a2,2 0 0,1 -2,-2 Z")
}

internal fun DrawScope.drawHouse(ctx: LucideDrawContext) {
    val lift = -1.8f * archEnvelope(ctx)
    val doorProgress = stagger(ctx, 0.12f, 0.90f)
    withTransform({ translate(top = lift) }) {
        drawPath(houseOutlinePath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    drawTrimmed(houseDoorPath, ctx, stopFraction = doorProgress)
}

internal fun DrawScope.drawHouseSelected(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val scale = 1f + 0.13f * env
    withTransform({
        translate(top = -1.8f * env)
        scale(scale, scale, pivot = Offset(12f, 13f))
    }) {
        drawPath(houseSelectedPath, color = ctx.tint, style = Fill)
        drawPath(houseSelectedPath, color = ctx.tint, style = LUCIDE_SELECTED_FILL_STROKE)
    }
}

private val chartAxisPath by lazy {
    parseLucidePath("M3,3 v16 a2,2 0 0,0 2,2 h16")
}
private val chartBarLeftPath by lazy { parseLucidePath("M8,17 v-3") }
private val chartBarMidPath by lazy { parseLucidePath("M13,17 V5") }
private val chartBarRightPath by lazy { parseLucidePath("M18,17 V9") }

private val chartSelectedLeftPath by lazy {
    parseLucidePath("M6,13.5 C6,12.67 6.67,12 7.5,12 C8.33,12 9,12.67 9,13.5 V18 H6 Z")
}
private val chartSelectedMidPath by lazy {
    parseLucidePath("M11,5.5 C11,4.67 11.67,4 12.5,4 C13.33,4 14,4.67 14,5.5 V18 H11 Z")
}
private val chartSelectedRightPath by lazy {
    parseLucidePath("M16,9.5 C16,8.67 16.67,8 17.5,8 C18.33,8 19,8.67 19,9.5 V18 H16 Z")
}

internal fun DrawScope.drawChartColumn(ctx: LucideDrawContext) {
    drawPath(chartAxisPath, color = ctx.tint, style = LUCIDE_STROKE)
    val s1 = bounceGrow(ctx, 0.0f, 0.56f)
    val s2 = bounceGrow(ctx, 0.14f, 0.74f)
    val s3 = bounceGrow(ctx, 0.28f, 0.94f)
    withTransform({ scale(1f, s1, pivot = Offset(8f, 17f)) }) {
        drawPath(chartBarLeftPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ scale(1f, s2, pivot = Offset(13f, 17f)) }) {
        drawPath(chartBarMidPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ scale(1f, s3, pivot = Offset(18f, 17f)) }) {
        drawPath(chartBarRightPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

internal fun DrawScope.drawChartColumnSelected(ctx: LucideDrawContext) {
    drawPath(chartAxisPath, color = ctx.tint, style = LUCIDE_STROKE)
    val s1 = bounceGrow(ctx, 0.0f, 0.56f)
    val s2 = bounceGrow(ctx, 0.14f, 0.74f)
    val s3 = bounceGrow(ctx, 0.28f, 0.94f)
    withTransform({ scale(1f, s1, pivot = Offset(7.5f, 18f)) }) {
        drawPath(chartSelectedLeftPath, color = ctx.tint, style = Fill)
    }
    withTransform({ scale(1f, s2, pivot = Offset(12.5f, 18f)) }) {
        drawPath(chartSelectedMidPath, color = ctx.tint, style = Fill)
    }
    withTransform({ scale(1f, s3, pivot = Offset(17.5f, 18f)) }) {
        drawPath(chartSelectedRightPath, color = ctx.tint, style = Fill)
    }
}

private val gridTopLeftPath by lazy {
    parseLucidePath("M4,3 H9 A1,1 0 0,1 10,4 V9 A1,1 0 0,1 9,10 H4 A1,1 0 0,1 3,9 V4 A1,1 0 0,1 4,3 Z")
}
private val gridTopRightPath by lazy {
    parseLucidePath("M15,3 H20 A1,1 0 0,1 21,4 V9 A1,1 0 0,1 20,10 H15 A1,1 0 0,1 14,9 V4 A1,1 0 0,1 15,3 Z")
}
private val gridBottomRightPath by lazy {
    parseLucidePath("M15,14 H20 A1,1 0 0,1 21,15 V20 A1,1 0 0,1 20,21 H15 A1,1 0 0,1 14,20 V15 A1,1 0 0,1 15,14 Z")
}
private val gridBottomLeftPath by lazy {
    parseLucidePath("M4,14 H9 A1,1 0 0,1 10,15 V20 A1,1 0 0,1 9,21 H4 A1,1 0 0,1 3,20 V15 A1,1 0 0,1 4,14 Z")
}

private fun DrawScope.drawGridTiles(ctx: LucideDrawContext, filled: Boolean) {
    val tileStyle = if (filled) LUCIDE_SELECTED_FILL_STROKE else LUCIDE_STROKE
    val tiles = arrayOf(
        Triple(gridTopLeftPath, Offset(-1f, -1f), Offset(6.5f, 6.5f)),
        Triple(gridTopRightPath, Offset(1f, -1f), Offset(17.5f, 6.5f)),
        Triple(gridBottomRightPath, Offset(1f, 1f), Offset(17.5f, 17.5f)),
        Triple(gridBottomLeftPath, Offset(-1f, 1f), Offset(6.5f, 17.5f))
    )
    tiles.forEachIndexed { idx, (path, dir, center) ->
        val start = idx * 0.11f
        val end = start + 0.62f
        val t = stagger(ctx, start, end)
        val arch = if (!ctx.isAnimating) 0f else sin(t * PI).toFloat()
        withTransform({
            translate(left = dir.x * 1.7f * arch, top = dir.y * 1.7f * arch)
            val s = 1f + 0.13f * arch
            scale(s, s, pivot = center)
        }) {
            if (filled) drawPath(path, color = ctx.tint, style = Fill)
            drawPath(path, color = ctx.tint, style = tileStyle)
        }
    }
}

internal fun DrawScope.drawLayoutGrid(ctx: LucideDrawContext) = drawGridTiles(ctx, filled = false)

internal fun DrawScope.drawLayoutGridSelected(ctx: LucideDrawContext) = drawGridTiles(ctx, filled = true)

private val userShouldersPath by lazy {
    parseLucidePath("M19,21 v-2 a4,4 0 0,0 -4,-4 H9 a4,4 0 0,0 -4,4 v2")
}
private val userHeadPath by lazy {
    parseLucidePath("M8,7 a4,4 0 1,0 8,0 a4,4 0 1,0 -8,0")
}
private val userShouldersSelectedPath by lazy {
    parseLucidePath("M19,21 v-2 a4,4 0 0,0 -4,-4 H9 a4,4 0 0,0 -4,4 v2 Z")
}

internal fun DrawScope.drawUser(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val shoulderStop = stagger(ctx, 0.05f, 0.85f)
    withTransform({ translate(top = -2.0f * env) }) {
        drawPath(userHeadPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ translate(top = -0.8f * env) }) {
        drawTrimmed(userShouldersPath, ctx, stopFraction = shoulderStop)
    }
}

internal fun DrawScope.drawUserSelected(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    withTransform({ translate(top = -2.0f * env) }) {
        drawPath(userHeadPath, color = ctx.tint, style = Fill)
        drawPath(userHeadPath, color = ctx.tint, style = LUCIDE_SELECTED_FILL_STROKE)
    }
    withTransform({
        val sx = 1f + 0.08f * env
        scale(sx, 1f, pivot = Offset(12f, 21f))
    }) {
        drawPath(userShouldersSelectedPath, color = ctx.tint, style = Fill)
        drawPath(userShouldersSelectedPath, color = ctx.tint, style = LUCIDE_SELECTED_FILL_STROKE)
    }
}

// -----------------------------------------------------------------------------
// Header & Controls Icons (7)
// -----------------------------------------------------------------------------

private val plusHorizPath by lazy { parseLucidePath("M5,12 h14") }
private val plusVertPath by lazy { parseLucidePath("M12,5 v14") }

internal fun DrawScope.drawPlus(ctx: LucideDrawContext) {
    val rot = if (!ctx.isAnimating) 0f else 180f * ctx.progress
    val scale = 1f + 0.20f * archEnvelope(ctx)
    withTransform({
        rotate(rot, pivot = Offset(12f, 12f))
        scale(scale, scale, pivot = Offset(12f, 12f))
    }) {
        drawPath(plusHorizPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(plusVertPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val bellBodyPath by lazy {
    parseLucidePath("M6,8 a6,6 0 0,1 12,0 c0,7 3,9 3,9 H3 s3,-2 3,-9")
}
private val bellClapperPath by lazy {
    parseLucidePath("M10.3,21 a1.94,1.94 0 0,0 3.4,0")
}

internal fun DrawScope.drawBell(ctx: LucideDrawContext) {
    val swing = dampedWave(ctx, halfCycles = 4f)
    val clapperLag = dampedWave(ctx, halfCycles = 3f)
    val rot = -22f * swing
    withTransform({ rotate(rot, pivot = Offset(12f, 3f)) }) {
        drawPath(bellBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({
        translate(left = 2.4f * clapperLag)
        rotate(rot * 0.55f, pivot = Offset(12f, 3f))
    }) {
        drawPath(bellClapperPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val settingsGearPath by lazy {
    parseLucidePath("M12.22,2 h-0.44 a2,2 0 0,0 -2,2 v0.18 a2,2 0 0,1 -1,1.73 l-0.43,0.25 a2,2 0 0,1 -2,0 l-0.15,-0.08 a2,2 0 0,0 -2.73,0.73 l-0.22,0.38 a2,2 0 0,0 0.73,2.73 l0.15,0.1 a2,2 0 0,1 1,1.72 v0.51 a2,2 0 0,1 -1,1.74 l-0.15,0.09 a2,2 0 0,0 -0.73,2.73 l0.22,0.38 a2,2 0 0,0 2.73,0.73 l0.15,-0.08 a2,2 0 0,1 2,0 l0.43,0.25 a2,2 0 0,1 1,1.73 V20 a2,2 0 0,0 2,2 h0.44 a2,2 0 0,0 2,-2 v-0.18 a2,2 0 0,1 1,-1.73 l0.43,-0.25 a2,2 0 0,1 2,0 l0.15,0.08 a2,2 0 0,0 2.73,-0.73 l0.22,-0.39 a2,2 0 0,0 -0.73,-2.73 l-0.15,-0.08 a2,2 0 0,1 -1,-1.74 v-0.5 a2,2 0 0,1 1,-1.74 l0.15,-0.09 a2,2 0 0,0 0.73,-2.73 l-0.22,-0.38 a2,2 0 0,0 -2.73,-0.73 l-0.15,0.08 a2,2 0 0,1 -2,0 l-0.43,-0.25 a2,2 0 0,1 -1,-1.73 V4 a2,2 0 0,0 -2,-2 z")
}
private val settingsHubPath by lazy {
    parseLucidePath("M9,12 a3,3 0 1,0 6,0 a3,3 0 1,0 -6,0")
}

internal fun DrawScope.drawSettings(ctx: LucideDrawContext) {
    val rot = if (!ctx.isAnimating) 0f else 180f * ctx.progress
    withTransform({ rotate(rot, pivot = Offset(12f, 12f)) }) {
        drawPath(settingsGearPath, color = ctx.tint, style = LUCIDE_STROKE)
        drawPath(settingsHubPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val eyeOuterPath by lazy {
    parseLucidePath("M2,12s3,-7 10,-7 10,7 10,7 -3,7 -10,7 -10,-7 -10,-7Z")
}
private val eyePupilPath by lazy {
    parseLucidePath("M12,12m-3,0a3,3 0 1,0 6,0a3,3 0 1,0 -6,0")
}

internal fun DrawScope.drawEye(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val lidScaleY = (1f - 0.78f * env).coerceAtLeast(0.12f)
    withTransform({ scale(1f, lidScaleY, pivot = Offset(12f, 12f)) }) {
        drawPath(eyeOuterPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    val pupilScale = (1f - 0.45f * env).coerceAtLeast(0.25f)
    withTransform({ scale(pupilScale, pupilScale, pivot = Offset(12f, 12f)) }) {
        drawPath(eyePupilPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val eyeOffTopPath by lazy {
    parseLucidePath("M10.73,5.08A10.43,10.43 0,0,1 12,5c7,0 10,7 10,7a13.16,13.16 0,0,1-1.67,2.68")
}
private val eyeOffBottomPath by lazy {
    parseLucidePath("M6.61,6.61A13.52,13.52 0,0,0 2,12s3,7 10,7a9.74,9.74 0,0,0 5.39,-1.61")
}
private val eyeOffPupilArcPath by lazy {
    parseLucidePath("M9.88,9.88a3,3 0 1,0 4.24,4.24")
}
private val eyeOffSlashPath by lazy {
    parseLucidePath("M2,2l20,20")
}

internal fun DrawScope.drawEyeOff(ctx: LucideDrawContext) {
    drawPath(eyeOffTopPath, color = ctx.tint, style = LUCIDE_STROKE)
    drawPath(eyeOffBottomPath, color = ctx.tint, style = LUCIDE_STROKE)
    drawPath(eyeOffPupilArcPath, color = ctx.tint, style = LUCIDE_STROKE)
    drawTrimmed(eyeOffSlashPath, ctx, stopFraction = stagger(ctx, 0.04f, 0.90f))
}

private val lockBodyPath by lazy {
    parseLucidePath("M5,11 H19 A2,2 0 0,1 21,13 V20 A2,2 0 0,1 19,22 H5 A2,2 0 0,1 3,20 V13 A2,2 0 0,1 5,11 Z")
}
private val lockShacklePath by lazy {
    parseLucidePath("M7,11 V7 a5,5 0 0,1 10,0 v4")
}

internal fun DrawScope.drawLock(ctx: LucideDrawContext) {
    val env = archEnvelope(ctx)
    val bodyNudge = 1.0f * env
    val shackleLift = -3.2f * env
    withTransform({ translate(top = bodyNudge) }) {
        drawPath(lockBodyPath, color = ctx.tint, style = LUCIDE_STROKE)
    }
    withTransform({ translate(top = shackleLift) }) {
        drawPath(lockShacklePath, color = ctx.tint, style = LUCIDE_STROKE)
    }
}

private val fingerprintPaths by lazy {
    listOf(
        parseLucidePath("M12,10 a2,2 0 0,0 -2,2 c0,1.02 -0.1,2.51 -0.26,4"),
        parseLucidePath("M14,13.12 c0,2.38 0,6.38 -1,8.88"),
        parseLucidePath("M17.29,21.02 c0.12,-0.6 0.43,-2.3 0.5,-3.02"),
        parseLucidePath("M9,6.8 a6,6 0 0,1 9,5.2 v2"),
        parseLucidePath("M5,19.5 C5.5,18 6,15 6,12 a6,6 0 0,1 0.34,-2"),
        parseLucidePath("M8.65,22 c0.21,-0.66 0.45,-1.32 0.57,-2"),
        parseLucidePath("M2,12 a10,10 0 0,1 18,-6"),
        parseLucidePath("M21.8,16 c0.2,-2 0.131,-5.354 0,-6")
    )
}

internal fun DrawScope.drawFingerprint(ctx: LucideDrawContext) {
    val total = fingerprintPaths.size
    fingerprintPaths.forEachIndexed { idx, path ->
        val start = (idx.toFloat() / total) * 0.48f
        val end = (start + 0.52f).coerceAtMost(1f)
        drawTrimmed(path, ctx, stopFraction = stagger(ctx, start, end))
    }
    val dotFrac = stagger(ctx, 0.55f, 0.88f)
    if (dotFrac > 0.2f) {
        drawCircle(color = ctx.tint, radius = LUCIDE_STROKE.width / 2f, center = Offset(2f, 16f))
    }
}
