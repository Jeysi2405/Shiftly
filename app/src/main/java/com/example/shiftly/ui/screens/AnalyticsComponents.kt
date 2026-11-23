package com.example.shiftly.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.ui.components.ShiftlyAnimatedCurrencyText
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlyScreenHeader
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.components.icons.ShiftlyAnimatedIcon
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlyShapes
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters

private val ChartDashEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
private val SegmentedOuterShape = RoundedCornerShape(10.dp)
private val SegmentedThumbShape = RoundedCornerShape(8.dp)
private val AnalyticsCardShape = ShiftlyShapes.Card

@Composable
fun AnalyticsTopBar(
    onBack: (() -> Unit)? = null
) {
    ShiftlyScreenHeader(
        title = "Analytics",
        largeTitle = true,
        onBackClick = onBack,
        modifier = Modifier.padding(horizontal = 20.dp)
    )
}

@Composable
fun AnalyticsRangeSegmentedControl(
    selectedRange: AnalyticsRange,
    onSelectRange: (AnalyticsRange) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    val entries = AnalyticsRange.entries
    val selectedIndex = entries.indexOf(selectedRange).coerceAtLeast(0)
    val trackColor = if (colors.isDark) colors.card else colors.cardElevated
    val thumbColor = colors.azurePrimary

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = SegmentedOuterShape,
        color = trackColor,
        shadowElevation = 0.dp
    ) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(3.dp)
        ) {
            val segmentWidth = maxWidth / entries.size
            val thumbOffset by animateDpAsState(
                targetValue = segmentWidth * selectedIndex,
                animationSpec = spring(dampingRatio = 0.8f),
                label = "analyticsSegmentThumbOffset"
            )

            Box(
                modifier = Modifier
                    .offset { IntOffset(thumbOffset.roundToPx(), 0) }
                    .width(segmentWidth)
                    .height(42.dp)
                    .clip(SegmentedThumbShape)
                    .background(thumbColor)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectableGroup(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                entries.forEach { range ->
                    val isSelected = range == selectedRange
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 42.dp)
                            .clip(SegmentedThumbShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Tab
                            ) {
                                if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onSelectRange(range)
                            }
                            .semantics(mergeDescendants = true) {
                                selected = isSelected
                                contentDescription = range.fullLabel
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = range.shortLabel,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                            ).tabular(),
                            color = if (isSelected) PureWhite else colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun IncomeVsExpenseGlassChartCard(
    state: AnalyticsUiState
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    val currency = state.currencySymbol
    val buckets = state.chartBuckets
    val net = remember(state.periodIncome, state.periodExpenses) {
        state.periodIncome - state.periodExpenses
    }
    val maxVal = remember(buckets) {
        buckets
            .maxOfOrNull { maxOf(it.income, it.expense) }
            ?.coerceAtLeast(100.0)
            ?: 100.0
    }
    val chartDescription = remember(
        state.selectedRange,
        currency,
        state.periodIncome,
        state.periodExpenses
    ) {
        buildString {
            append("Income versus expenses trend chart for ${state.selectedRange.fullLabel}. ")
            append("Total income ${ShiftlyFormatters.formatCurrency(currency, state.periodIncome)}, ")
            append("total expenses ${ShiftlyFormatters.formatCurrency(currency, state.periodExpenses)}.")
        }
    }

    val revealProgress = remember { Animatable(1f) }
    var lastRange by remember { mutableStateOf(state.selectedRange) }
    var scrubbedIndex by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(state.selectedRange) {
        scrubbedIndex = null
        if (lastRange != state.selectedRange) {
            lastRange = state.selectedRange
            revealProgress.snapTo(0f)
            revealProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(durationMillis = 460, easing = FastOutSlowInEasing)
            )
        }
    }

    LaunchedEffect(buckets.size) {
        if ((scrubbedIndex ?: -1) >= buckets.size) {
            scrubbedIndex = null
        }
    }

    fun resolveBucket(x: Float, width: Float, count: Int): Int? {
        if (count <= 0 || width <= 0f) return null
        return ((x / width) * count).toInt().coerceIn(0, count - 1)
    }

    ShiftlyGlassCard(
        cornerRadius = 16.dp,
        contentPadding = PaddingValues(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val scrubbedBucket = scrubbedIndex?.let { buckets.getOrNull(it) }
            AnimatedContent(
                targetState = scrubbedBucket,
                transitionSpec = {
                    fadeIn(tween(150)) togetherWith fadeOut(tween(120))
                },
                label = "chartScrubHeader"
            ) { bucket ->
                if (bucket == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = state.selectedRange.fullLabel,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 24.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = colors.azureContainer,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = bucket.label,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Bold
                                ).tabular(),
                                color = colors.azureInk,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currency, bucket.income, showPlus = true),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = colors.mintTextOnSurface
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currency, bucket.expense, showMinus = true),
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = colors.coralTextOnSurface
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                    Column {
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                        ShiftlyAnimatedCurrencyText(
                            currencySymbol = currency,
                            amount = state.periodIncome,
                            showPlus = true,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = colors.mintTextOnSurface
                        )
                    }
                    Column {
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                        ShiftlyAnimatedCurrencyText(
                            currencySymbol = currency,
                            amount = state.periodExpenses,
                            showMinus = true,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp
                            ),
                            color = colors.coralTextOnSurface
                        )
                    }
                }

                val netPillContainer by animateColorAsState(
                    targetValue = if (net >= 0) colors.mintContainer else colors.coralContainer,
                    animationSpec = spring(dampingRatio = 0.85f),
                    label = "analyticsNetPillBg"
                )
                val netPillText by animateColorAsState(
                    targetValue = if (net >= 0) colors.mintTextOnSurface else colors.coralTextOnSurface,
                    animationSpec = spring(dampingRatio = 0.85f),
                    label = "analyticsNetPillText"
                )
                Surface(
                    shape = RoundedCornerShape(50),
                    color = netPillContainer,
                    shadowElevation = 0.dp
                ) {
                    ShiftlyAnimatedCurrencyText(
                        currencySymbol = currency,
                        amount = net,
                        showPlus = net > 0.0,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = netPillText,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }

            ShiftlyHairlineDivider()

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics(mergeDescendants = true) {
                        contentDescription = chartDescription
                    },
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                val gridBorderColor = colors.separator
                val cardDotBg = colors.card
                val mintLineColor = colors.mintBright
                val coralLineColor = colors.coralBright
                val dotCenterColor = colors.surface
                val accentCrosshairColor = colors.azureAccent.copy(alpha = 0.65f)

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp)
                        .pointerInput(buckets.size) {
                            detectTapGestures { offset ->
                                val idx = resolveBucket(offset.x, size.width.toFloat(), buckets.size)
                                if (idx != null) {
                                    scrubbedIndex = if (scrubbedIndex == idx) null else idx
                                    haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                }
                            }
                        }
                        .pointerInput(buckets.size) {
                            detectHorizontalDragGestures(
                                onDragStart = { offset ->
                                    val idx = resolveBucket(offset.x, size.width.toFloat(), buckets.size)
                                    if (idx != null && idx != scrubbedIndex) {
                                        scrubbedIndex = idx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onHorizontalDrag = { change, _ ->
                                    change.consume()
                                    val idx = resolveBucket(change.position.x, size.width.toFloat(), buckets.size)
                                    if (idx != null && idx != scrubbedIndex) {
                                        scrubbedIndex = idx
                                        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                },
                                onDragEnd = { scrubbedIndex = null },
                                onDragCancel = { scrubbedIndex = null }
                            )
                        }
                ) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxSize()
                            .drawWithCache {
                                val w = size.width
                                val h = size.height
                                val paddingTop = 12.dp.toPx()
                                val paddingBottom = 12.dp.toPx()
                                val gridStrokePx = 0.5.dp.toPx()
                                val crosshairStrokePx = 1.5.dp.toPx()
                                val lineStroke = Stroke(
                                    width = 3.dp.toPx(),
                                    cap = StrokeCap.Round,
                                    join = StrokeJoin.Round
                                )
                                val dotOuterRadius = 5.dp.toPx()
                                val dotMidRadius = 2.5.dp.toPx()
                                val dotInnerRadius = 1.5.dp.toPx()
                                val activeHaloRadius = 10.dp.toPx()
                                val activeOuterRadius = 6.5.dp.toPx()
                                val activeMidRadius = 3.2.dp.toPx()
                                val activeInnerRadius = 1.8.dp.toPx()

                                val chartHeight = h - paddingTop - paddingBottom
                                val gridLines = 3
                                val horizontalYs = FloatArray(gridLines + 1) { i ->
                                    paddingTop + (chartHeight / gridLines) * i
                                }

                                val count = buckets.size
                                val slotWidth = if (count > 0) w / count else 0f
                                val verticalXs = FloatArray(count) { idx ->
                                    slotWidth * (idx + 0.5f)
                                }
                                val incPoints = if (count > 0) {
                                    List(count) { idx ->
                                        val b = buckets[idx]
                                        Offset(
                                            verticalXs[idx],
                                            h - paddingBottom - ((b.income / maxVal) * chartHeight).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                                val expPoints = if (count > 0) {
                                    List(count) { idx ->
                                        val b = buckets[idx]
                                        Offset(
                                            verticalXs[idx],
                                            h - paddingBottom - ((b.expense / maxVal) * chartHeight).toFloat()
                                        )
                                    }
                                } else {
                                    emptyList()
                                }
                                val incPath = Path().apply {
                                    incPoints.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                    }
                                }
                                val expPath = Path().apply {
                                    expPoints.forEachIndexed { i, p ->
                                        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                    }
                                }
                                val hGridColor = gridBorderColor.copy(alpha = 0.75f)
                                val vGridColor = gridBorderColor.copy(alpha = 0.45f)
                                val gridStartX = 6.dp.toPx()
                                val gridEndX = (w - 6.dp.toPx()).coerceAtLeast(gridStartX)
                                val bottomY = h - paddingBottom
                                val incAreaPath = if (incPoints.size >= 2) {
                                    Path().apply {
                                        incPoints.forEachIndexed { i, p ->
                                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                        }
                                        lineTo(incPoints.last().x, bottomY)
                                        lineTo(incPoints.first().x, bottomY)
                                        close()
                                    }
                                } else {
                                    null
                                }
                                val expAreaPath = if (expPoints.size >= 2) {
                                    Path().apply {
                                        expPoints.forEachIndexed { i, p ->
                                            if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
                                        }
                                        lineTo(expPoints.last().x, bottomY)
                                        lineTo(expPoints.first().x, bottomY)
                                        close()
                                    }
                                } else {
                                    null
                                }
                                val incAreaBrush = Brush.verticalGradient(
                                    colors = listOf(mintLineColor.copy(alpha = 0.22f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = bottomY
                                )
                                val expAreaBrush = Brush.verticalGradient(
                                    colors = listOf(coralLineColor.copy(alpha = 0.18f), Color.Transparent),
                                    startY = paddingTop,
                                    endY = bottomY
                                )

                                onDrawBehind {
                                    for (y in horizontalYs) {
                                        drawLine(
                                            color = hGridColor,
                                            start = Offset(gridStartX, y),
                                            end = Offset(gridEndX, y),
                                            strokeWidth = gridStrokePx,
                                            pathEffect = ChartDashEffect
                                        )
                                    }

                                    if (count > 0) {
                                        for (x in verticalXs) {
                                            drawLine(
                                                color = vGridColor,
                                                start = Offset(x, paddingTop),
                                                end = Offset(x, bottomY),
                                                strokeWidth = gridStrokePx
                                            )
                                        }

                                        val sweepFraction = revealProgress.value.coerceIn(0f, 1f)
                                        clipRect(
                                            left = 0f,
                                            top = 0f,
                                            right = size.width * sweepFraction,
                                            bottom = size.height
                                        ) {
                                            incAreaPath?.let { drawPath(path = it, brush = incAreaBrush) }
                                            expAreaPath?.let { drawPath(path = it, brush = expAreaBrush) }

                                            drawPath(
                                                path = incPath,
                                                color = mintLineColor,
                                                style = lineStroke
                                            )
                                            for (p in incPoints) {
                                                drawCircle(color = mintLineColor, radius = dotOuterRadius, center = p)
                                                drawCircle(color = cardDotBg, radius = dotMidRadius, center = p)
                                                drawCircle(color = dotCenterColor, radius = dotInnerRadius, center = p)
                                            }

                                            drawPath(
                                                path = expPath,
                                                color = coralLineColor,
                                                style = lineStroke
                                            )
                                            for (p in expPoints) {
                                                drawCircle(color = coralLineColor, radius = dotOuterRadius, center = p)
                                                drawCircle(color = cardDotBg, radius = dotMidRadius, center = p)
                                                drawCircle(color = dotCenterColor, radius = dotInnerRadius, center = p)
                                            }
                                        }

                                        val sel = scrubbedIndex
                                        if (sel != null && sel in 0 until count) {
                                            drawLine(
                                                color = accentCrosshairColor,
                                                start = Offset(verticalXs[sel], paddingTop),
                                                end = Offset(verticalXs[sel], bottomY),
                                                strokeWidth = crosshairStrokePx
                                            )

                                            val incPoint = incPoints[sel]
                                            drawCircle(color = mintLineColor.copy(alpha = 0.22f), radius = activeHaloRadius, center = incPoint)
                                            drawCircle(color = mintLineColor, radius = activeOuterRadius, center = incPoint)
                                            drawCircle(color = cardDotBg, radius = activeMidRadius, center = incPoint)
                                            drawCircle(color = dotCenterColor, radius = activeInnerRadius, center = incPoint)

                                            val expPoint = expPoints[sel]
                                            drawCircle(color = coralLineColor.copy(alpha = 0.22f), radius = activeHaloRadius, center = expPoint)
                                            drawCircle(color = coralLineColor, radius = activeOuterRadius, center = expPoint)
                                            drawCircle(color = cardDotBg, radius = activeMidRadius, center = expPoint)
                                            drawCircle(color = dotCenterColor, radius = activeInnerRadius, center = expPoint)
                                        }
                                    }
                                }
                            }
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    buckets.forEachIndexed { idx, bucket ->
                        val isScrubbed = scrubbedIndex == idx
                        Text(
                            text = bucket.label,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = if (isScrubbed) FontWeight.Bold else FontWeight.Medium
                            ).tabular(),
                            color = if (isScrubbed) colors.azureAccent else colors.textSecondary,
                            textAlign = TextAlign.Center,
                            maxLines = 1,
                            softWrap = false,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.mintBright)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Income",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(18.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(colors.coralBright)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Expenses",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textSecondary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun InsightCardContainer(
    title: String,
    trailingActionText: String? = null,
    onTrailingAction: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = AnalyticsCardShape,
        color = colors.card,
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(AnalyticsCardShape)
                        .shiftlyPressClickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary
                )
                if (trailingActionText != null && onTrailingAction != null) {
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .shiftlyPressClickable(onClick = onTrailingAction)
                            .padding(horizontal = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = trailingActionText,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.azureInk
                        )
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun SmartTakeawaysCard(
    tips: List<String>
) {
    val colors = ShiftlyColors.current
    if (tips.isEmpty()) return

    InsightCardContainer(title = "Smart Takeaways") {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            tips.forEachIndexed { idx, tip ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.iconNeutralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_sparkles,
                            contentDescription = null,
                            tint = colors.iconNeutral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = tip,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                }
                if (idx < tips.lastIndex) {
                    ShiftlyHairlineDivider(startInset = 38.dp)
                }
            }
        }
    }
}

@Composable
fun TopSpendingCategoriesCard(
    categories: List<CategorySpendingInsight>,
    currencySymbol: String
) {
    val colors = ShiftlyColors.current
    InsightCardContainer(
        title = "Top Categories"
    ) {
        if (categories.isEmpty()) {
            Text(
                text = "No spending yet.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                categories.forEachIndexed { idx, item ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.iconNeutralContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ShiftlyAnimatedIcon(
                                        iconRes = categoryIcon(item.category),
                                        contentDescription = null,
                                        tint = colors.iconNeutral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = item.category,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "${item.percentage}%  •  ${ShiftlyFormatters.formatCurrency(currencySymbol, item.amount)}",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = colors.textPrimary
                            )
                        }
                        ShiftlyProgressBar(
                            progress = item.fraction.coerceIn(0.05f, 1f),
                            color = colors.coralBright,
                            trackColor = colors.cardElevated,
                            modifier = Modifier
                                .padding(start = 38.dp)
                                .semantics {
                                    contentDescription = "${item.category}, ${item.percentage}% of period spending"
                                }
                        )
                    }
                    if (idx < categories.lastIndex) {
                        ShiftlyHairlineDivider(startInset = 38.dp)
                    }
                }
            }
        }
    }
}

@Composable
fun EarningsHoursBreakdownCard(
    currencySymbol: String,
    earnings: Double,
    totalHours: Double,
    regularHours: Double,
    nightDiffHours: Double,
    overtimeHours: Double
) {
    val colors = ShiftlyColors.current
    val totalSplitHours = remember(regularHours, nightDiffHours, overtimeHours) {
        (regularHours + nightDiffHours + overtimeHours).coerceAtLeast(0.1)
    }
    InsightCardContainer(title = "Work Hours") {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            ShiftlyAnimatedCurrencyText(
                currencySymbol = currencySymbol,
                amount = earnings,
                style = MaterialTheme.typography.headlineSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ),
                color = colors.mintInk
            )
            Text(
                text = "${ShiftlyFormatters.formatHours(totalHours)} logged",
                style = MaterialTheme.typography.labelLarge.tabular(),
                color = colors.textSecondary
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(5.dp)
                .clip(RoundedCornerShape(50))
                .background(colors.cardElevated)
                .semantics {
                    contentDescription = "Hours split: ${ShiftlyFormatters.formatHours(regularHours)} regular, ${ShiftlyFormatters.formatHours(nightDiffHours)} night diff, ${ShiftlyFormatters.formatHours(overtimeHours)} overtime"
                },
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            if (regularHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((regularHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.textPrimary)
                )
            }
            if (nightDiffHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((nightDiffHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.nightDiffPurple)
                )
            }
            if (overtimeHours > 0.0) {
                Box(
                    modifier = Modifier
                        .weight((overtimeHours / totalSplitHours).toFloat().coerceAtLeast(0.08f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(50))
                        .background(colors.coralBright)
                )
            }
        }

        ShiftlyHairlineDivider()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HoursSplitMiniColumn(
                label = "Regular",
                hours = regularHours,
                accentColor = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            ShiftlyVerticalHairlineDivider(modifier = Modifier.height(34.dp))
            HoursSplitMiniColumn(
                label = "Night Diff",
                hours = nightDiffHours,
                accentColor = colors.nightDiffPurple,
                modifier = Modifier.weight(1f)
            )
            ShiftlyVerticalHairlineDivider(modifier = Modifier.height(34.dp))
            HoursSplitMiniColumn(
                label = "Overtime",
                hours = overtimeHours,
                accentColor = colors.coralBright,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun HoursSplitMiniColumn(
    label: String,
    hours: Double,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = ShiftlyFormatters.formatHours(hours, compact = true),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = accentColor
        )
    }
}

@Composable
fun HoursOfWorkCostCard(
    currencySymbol: String,
    hourlyRate: Double,
    items: List<HoursCostInsight>
) {
    val colors = ShiftlyColors.current
    val maxHours = remember(items) {
        (items.maxOfOrNull { it.dutyHours } ?: 1.0).coerceAtLeast(1.0)
    }

    InsightCardContainer(
        title = "Work Cost"
    ) {
        if (hourlyRate <= 0.0) {
            Text(
                text = "Set hourly rate in Profile.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else if (items.isEmpty()) {
            Text(
                text = "No expenses yet.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items.forEach { entry ->
                    val ratio = (entry.dutyHours / maxHours).toFloat().coerceIn(0.08f, 1f)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(colors.iconNeutralContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    ShiftlyAnimatedIcon(
                                        iconRes = R.drawable.ic_lucide_clock,
                                        contentDescription = null,
                                        tint = colors.iconNeutral,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Text(
                                    text = "${entry.category} • ${ShiftlyFormatters.formatHours(entry.dutyHours)} of duty",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ).tabular(),
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = ShiftlyFormatters.formatCurrency(currencySymbol, entry.amount),
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = colors.textSecondary,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                        ShiftlyProgressBar(
                            progress = ratio,
                            color = colors.mintBright,
                            trackColor = colors.cardElevated,
                            modifier = Modifier
                                .padding(start = 38.dp)
                                .semantics {
                                    contentDescription = "${entry.category} equals ${ShiftlyFormatters.formatHours(entry.dutyHours)} of duty"
                                }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetAndSavingsSummaryCard(
    streakDays: Int,
    streakTargetDays: Int,
    dailyAllowance: Double,
    ratePercentage: Int,
    netAmount: Double,
    progressFraction: Float,
    projectedSpend: Double,
    dailyPace: Double,
    isWithinBudget: Boolean?,
    currencySymbol: String,
    onOpenBudgetSettings: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val hasBudget = dailyAllowance > 0.0
    val streakFraction = remember(hasBudget, streakDays, streakTargetDays) {
        if (hasBudget) {
            (streakDays.toFloat() / streakTargetDays.coerceAtLeast(1)).coerceIn(0.05f, 1f)
        } else {
            0f
        }
    }
    val isPositive = netAmount >= 0.0

    Surface(
        shape = AnalyticsCardShape,
        color = colors.card,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // 1. On-Budget Streak (tappable to edit Monthly Limits)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .shiftlyPressClickable(onClick = onOpenBudgetSettings)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = if (hasBudget) "$streakDays-day budget streak" else "Monthly budget streak",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = colors.textPrimary
                        )
                        Text(
                            text = if (hasBudget) {
                                "Safe pace: ${ShiftlyFormatters.formatCurrency(currencySymbol, dailyAllowance)}/day"
                            } else {
                                "Set monthly cap"
                            },
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = if (hasBudget) colors.textSecondary else colors.azureInk,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Surface(
                            shape = ShiftlyShapes.Pill,
                            color = colors.mintContainer,
                            shadowElevation = 0.dp
                        ) {
                            Text(
                                text = "$streakDays / ${streakTargetDays}d",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = colors.mintInk,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_chevron_right,
                            contentDescription = "Edit monthly budget",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(15.dp)
                        )
                    }
                }
                ShiftlyProgressBar(
                    progress = streakFraction,
                    color = colors.mintBright,
                    trackColor = colors.cardElevated,
                    modifier = Modifier.semantics {
                        contentDescription = "$streakDays of $streakTargetDays days on budget"
                    }
                )
            }

            ShiftlyHairlineDivider()

            // 2. Net Savings Rate
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "$ratePercentage% saved",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary
                    )

                    Surface(
                        shape = ShiftlyShapes.Pill,
                        color = if (isPositive) colors.mintContainer else colors.coralContainer,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = if (isPositive) {
                                "Net +${ShiftlyFormatters.formatCurrency(currencySymbol, netAmount)}"
                            } else {
                                "Deficit -${ShiftlyFormatters.formatCurrency(currencySymbol, -netAmount)}"
                            },
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = if (isPositive) colors.mintInk else colors.coralInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                ShiftlyProgressBar(
                    progress = progressFraction.coerceIn(0.02f, 1f),
                    color = if (isPositive) colors.mintBright else colors.coralBright,
                    trackColor = colors.cardElevated,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .semantics {
                            contentDescription = "Net savings rate $ratePercentage percent"
                        }
                )
            }

            ShiftlyHairlineDivider()

            // 3. Projected End-of-Month Spend
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "Projected month-end",
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textSecondary
                    )
                    ShiftlyAnimatedCurrencyText(
                        currencySymbol = currencySymbol,
                        amount = projectedSpend,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, dailyPace)}/day pace",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary
                    )
                }

                if (isWithinBudget != null) {
                    Surface(
                        shape = ShiftlyShapes.Pill,
                        color = if (isWithinBudget) colors.mintContainer else colors.coralContainer,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = if (isWithinBudget) "Within budget" else "Over budget",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = if (isWithinBudget) colors.mintInk else colors.coralInk,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PeakShiftSpendPatternCard(
    topEarningDayLabel: String,
    highestSpendDayLabel: String
) {
    val colors = ShiftlyColors.current

    InsightCardContainer(title = "Patterns") {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.iconNeutralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_trending_up,
                            contentDescription = null,
                            tint = colors.iconNeutral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Top Earn Day",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = topEarningDayLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(colors.iconNeutralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        ShiftlyAnimatedIcon(
                            iconRes = R.drawable.ic_lucide_receipt,
                            contentDescription = null,
                            tint = colors.iconNeutral,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Text(
                        text = "Top Spend Day",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium
                        ),
                        color = colors.textPrimary
                    )
                }

                Text(
                    text = highestSpendDayLabel,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.coralInk
                )
            }
        }
    }
}
