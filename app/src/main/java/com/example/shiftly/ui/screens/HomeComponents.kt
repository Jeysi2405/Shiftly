package com.example.shiftly.ui.screens

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.shiftly.R
import com.example.shiftly.data.local.InAppNotificationItem
import com.example.shiftly.data.local.InAppNotificationType
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.PlusJakartaSans
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val HERO_PAGE_LABELS = listOf("Total Balance", "Shift Earnings", "Savings Goals")
private val HomePillShape = RoundedCornerShape(50)
private val HomeCardShape = RoundedCornerShape(22.dp)
private val HomeBannerShape = RoundedCornerShape(20.dp)
private val HomeMetricTileShape = RoundedCornerShape(18.dp)
private val HomeCompactTileShape = RoundedCornerShape(16.dp)
private val HomeTimelineRowShape = RoundedCornerShape(14.dp)
private val InboxDropdownShape = RoundedCornerShape(24.dp)
private val HeadsUpBannerShape = RoundedCornerShape(22.dp)
private const val HEADS_UP_AUTO_DISMISS_MS = 4_200L

internal fun formatNotificationTime(
    timestampMillis: Long,
    referenceTimeMillis: Long? = null,
    locale: Locale = Locale.ENGLISH,
    zoneId: ZoneId = ZoneId.systemDefault()
): String {
    if (timestampMillis <= 0L) return ""
    val nowMillis = referenceTimeMillis ?: System.currentTimeMillis()
    val diffMillis = (nowMillis - timestampMillis).coerceAtLeast(0L)
    if (diffMillis < 60_000L) return "Just now"
    val diffMins = diffMillis / 60_000L
    if (diffMins < 60L) return "${diffMins}m ago"

    val itemZoned = Instant.ofEpochMilli(timestampMillis).atZone(zoneId)
    val nowZoned = Instant.ofEpochMilli(nowMillis).atZone(zoneId)
    val itemDate = itemZoned.toLocalDate()
    val nowDate = nowZoned.toLocalDate()

    return when {
        itemDate == nowDate -> {
            val diffHours = diffMillis / 3_600_000L
            if (diffHours < 6L) "${diffHours}h ago"
            else itemZoned.format(DateTimeFormatter.ofPattern("h:mm a", locale))
        }
        itemDate == nowDate.minusDays(1) -> "Yesterday"
        else -> itemZoned.format(DateTimeFormatter.ofPattern("MMM d", locale))
    }
}

private data class NotificationVisualStyle(
    @DrawableRes val iconRes: Int,
    val containerColor: Color,
    val accentColor: Color
)

@Composable
private fun notificationVisualStyle(type: InAppNotificationType): NotificationVisualStyle {
    val colors = ShiftlyColors.current
    return when (type) {
        InAppNotificationType.BILL_DUE -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_repeat,
            containerColor = colors.coralContainer,
            accentColor = colors.coralInk
        )
        InAppNotificationType.BILL_POSTED -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_receipt_text,
            containerColor = colors.coralContainer,
            accentColor = colors.coralInk
        )
        InAppNotificationType.SHIFT_REMINDER -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_clock,
            containerColor = colors.azureContainer,
            accentColor = colors.azureAccent
        )
        InAppNotificationType.SAVINGS_REMINDER -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_piggy_bank,
            containerColor = colors.mintContainer,
            accentColor = colors.mintInk
        )
        InAppNotificationType.PAYDAY_CUTOFF -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_wallet,
            containerColor = colors.mintContainer,
            accentColor = colors.mintInk
        )
        InAppNotificationType.SPLIT_BILL -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_split,
            containerColor = colors.azureContainer,
            accentColor = colors.azureAccent
        )
    }
}

/**
 * Top header displaying a refined muted date subtitle (e.g. "Wednesday, Nov 18, 2026"),
 * plus notifications and profile avatar.
 */
@Composable
fun HomeGreetingHeader(
    profilePhotoPath: String? = null,
    unreadCount: Int = 0,
    isInboxOpen: Boolean = false,
    todayOverride: LocalDate? = null,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember(todayOverride) { todayOverride ?: LocalDate.now() }
    val fullDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", locale) }
    val formattedDate = remember(today, fullDateFormatter) { today.format(fullDateFormatter) }

    val notificationDescription = remember(unreadCount, isInboxOpen) {
        when {
            isInboxOpen -> "Close notifications inbox"
            unreadCount > 0 -> "Notifications, $unreadCount unread"
            else -> "Notifications, all caught up"
        }
    }

    val photoFile by produceState<File?>(initialValue = null, profilePhotoPath) {
        value = withContext(Dispatchers.IO) {
            profilePhotoPath?.let(::File)?.takeIf { it.exists() && it.isFile }
        }
    }
    val avatarSizePx = with(LocalDensity.current) { 48.dp.roundToPx() }
    val avatarRequest = remember(photoFile, avatarSizePx, context) {
        photoFile?.let { file ->
            ImageRequest.Builder(context)
                .data(file)
                .size(avatarSizePx, avatarSizePx)
                .scale(Scale.FILL)
                .crossfade(true)
                .build()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = formattedDate,
            style = MaterialTheme.typography.titleSmall.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Medium
            ).tabular(),
            color = colors.textMuted,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp)
        )

        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(if (isInboxOpen) colors.azureContainer else Color.Transparent)
                        .semantics { contentDescription = notificationDescription }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_bell),
                        contentDescription = null,
                        tint = if (isInboxOpen) colors.azureAccent else colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 6.dp)
                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(CircleShape)
                            .background(colors.coralBright)
                            .border(1.5.dp, colors.background, CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = colors.onAccentDark,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.azureContainer)
            ) {
                if (avatarRequest != null) {
                    AsyncImage(
                        model = avatarRequest,
                        contentDescription = "Open Profile and Settings",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                    )
                } else {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_user),
                        contentDescription = "Open Profile and Settings",
                        tint = colors.azureAccent,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

/**
 * Top-anchored notification inbox dropdown panel triggered by the Home bell icon.
 */
@Composable
fun NotificationInboxDropdown(
    visible: Boolean,
    notifications: List<InAppNotificationItem>,
    unreadCount: Int,
    onDismiss: () -> Unit,
    onClearAll: () -> Unit,
    onItemClick: (InAppNotificationItem) -> Unit,
    modifier: Modifier = Modifier,
    referenceTimeMillis: Long? = null
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(180)),
            exit = fadeOut(animationSpec = tween(160))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (colors.isDark) 0.56f else 0.32f))
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDismiss
                    )
            )
        }

        AnimatedVisibility(
            visible = visible,
            enter = slideInVertically(
                initialOffsetY = { -it / 4 },
                animationSpec = spring(
                    dampingRatio = 0.88f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + expandVertically(
                expandFrom = Alignment.Top,
                animationSpec = tween(220, easing = FastOutSlowInEasing)
            ) + fadeIn(animationSpec = tween(180)),
            exit = slideOutVertically(
                targetOffsetY = { -it / 4 },
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) + shrinkVertically(
                shrinkTowards = Alignment.Top,
                animationSpec = tween(180, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(150)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 64.dp, start = 16.dp, end = 16.dp)
        ) {
            Surface(
                shape = InboxDropdownShape,
                color = colors.surface,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 12.dp,
                tonalElevation = 0.dp,
                modifier = Modifier
                    .widthIn(max = 420.dp)
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {}
                    )
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Notifications",
                                style = MaterialTheme.typography.titleMedium.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colors.textPrimary
                            )
                            if (unreadCount > 0) {
                                Box(
                                    modifier = Modifier
                                        .clip(HomePillShape)
                                        .background(colors.coralContainer)
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$unreadCount new",
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        color = colors.coralInk
                                    )
                                }
                            }
                        }

                        TextButton(
                            onClick = onClearAll,
                            enabled = notifications.isNotEmpty(),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                            modifier = Modifier.heightIn(min = 36.dp)
                        ) {
                            Text(
                                text = "Clear all",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (notifications.isNotEmpty()) colors.azureAccent else colors.disabledContent
                            )
                        }
                    }

                    HorizontalDivider(color = colors.border)

                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(colors.card)
                                    .border(1.dp, colors.cardBorder, CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_bell),
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Text(
                                text = "All caught up",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Bill alerts, shift reminders, and payday updates will appear here.",
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 360.dp),
                            contentPadding = PaddingValues(vertical = 6.dp)
                        ) {
                            itemsIndexed(
                                items = notifications,
                                key = { _, item -> item.id },
                                contentType = { _, _ -> "inbox_notification" }
                            ) { idx, item ->
                                NotificationInboxRow(
                                    item = item,
                                    locale = locale,
                                    referenceTimeMillis = referenceTimeMillis,
                                    onClick = { onItemClick(item) }
                                )
                                if (idx < notifications.lastIndex) {
                                    HorizontalDivider(
                                        color = colors.border.copy(alpha = 0.65f),
                                        modifier = Modifier.padding(horizontal = 16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationInboxRow(
    item: InAppNotificationItem,
    locale: Locale,
    referenceTimeMillis: Long?,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val visual = notificationVisualStyle(item.type)
    val timeLabel = remember(item.timestampMillis, referenceTimeMillis, locale) {
        formatNotificationTime(
            timestampMillis = item.timestampMillis,
            referenceTimeMillis = referenceTimeMillis,
            locale = locale
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (!item.isRead) colors.azureContainer.copy(alpha = if (colors.isDark) 0.18f else 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(visual.containerColor),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(visual.iconRes),
                contentDescription = null,
                tint = visual.accentColor,
                modifier = Modifier.size(18.dp)
            )
        }

        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = if (!item.isRead) FontWeight.Bold else FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (timeLabel.isNotEmpty()) {
                        Text(
                            text = timeLabel,
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textMuted
                        )
                    }
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.coralBright)
                        )
                    }
                }
            }
            Text(
                text = item.message,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Top heads-up toast-style notification bubble that slides in smoothly, auto-dismisses after
 * a few seconds, and supports swipe up or swipe left/right dismissal.
 */
@Composable
fun NotificationHeadsUpBanner(
    notification: InAppNotificationItem?,
    onTap: (InAppNotificationItem) -> Unit,
    onDismiss: (String) -> Unit,
    modifier: Modifier = Modifier,
    referenceTimeMillis: Long? = null
) {
    var displayedItem by remember { mutableStateOf(notification) }
    if (notification != null) {
        displayedItem = notification
    }

    LaunchedEffect(notification?.id) {
        val active = notification ?: return@LaunchedEffect
        delay(HEADS_UP_AUTO_DISMISS_MS)
        onDismiss(active.id)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(top = 8.dp, start = 16.dp, end = 16.dp),
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedVisibility(
            visible = notification != null,
            enter = slideInVertically(
                initialOffsetY = { -it - 40 },
                animationSpec = spring(
                    dampingRatio = 0.85f,
                    stiffness = Spring.StiffnessMediumLow
                )
            ) + fadeIn(animationSpec = tween(220)),
            exit = slideOutVertically(
                targetOffsetY = { -it - 40 },
                animationSpec = tween(200, easing = FastOutSlowInEasing)
            ) + fadeOut(animationSpec = tween(180))
        ) {
            val item = displayedItem ?: return@AnimatedVisibility
            key(item.id) {
                NotificationHeadsUpCard(
                    item = item,
                    referenceTimeMillis = referenceTimeMillis,
                    onTap = { onTap(item) },
                    onDismiss = { onDismiss(item.id) }
                )
            }
        }
    }
}

@Composable
private fun NotificationHeadsUpCard(
    item: InAppNotificationItem,
    referenceTimeMillis: Long?,
    onTap: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val visual = notificationVisualStyle(item.type)
    val scope = rememberCoroutineScope()
    val offsetX = remember { Animatable(0f) }
    val offsetY = remember { Animatable(0f) }
    var dismissedBySwipe by remember { mutableStateOf(false) }

    val timeLabel = remember(item.timestampMillis, referenceTimeMillis, locale) {
        formatNotificationTime(
            timestampMillis = item.timestampMillis,
            referenceTimeMillis = referenceTimeMillis,
            locale = locale
        )
    }

    Surface(
        shape = HeadsUpBannerShape,
        color = colors.card,
        border = BorderStroke(1.dp, visual.accentColor.copy(alpha = 0.45f)),
        shadowElevation = 12.dp,
        tonalElevation = 0.dp,
        modifier = Modifier
            .widthIn(max = 420.dp)
            .fillMaxWidth()
            .graphicsLayer {
                translationX = offsetX.value
                translationY = offsetY.value
                val dismissProgress = maxOf(
                    abs(offsetX.value) / 260f,
                    (-offsetY.value) / 140f
                ).coerceIn(0f, 1f)
                alpha = 1f - dismissProgress * 0.85f
            }
            .pointerInput(item.id) {
                detectDragGestures(
                    onDrag = { change, dragAmount ->
                        change.consume()
                        scope.launch {
                            offsetX.snapTo(offsetX.value + dragAmount.x)
                            offsetY.snapTo((offsetY.value + dragAmount.y).coerceAtMost(16f))
                        }
                    },
                    onDragEnd = {
                        if (dismissedBySwipe) return@detectDragGestures
                        val curX = offsetX.value
                        val curY = offsetY.value
                        if (curY < -54f || abs(curX) > 130f) {
                            dismissedBySwipe = true
                            scope.launch {
                                if (curY < -54f) {
                                    offsetY.animateTo(-220f, tween(150))
                                } else {
                                    val targetX = if (curX > 0f) 600f else -600f
                                    offsetX.animateTo(targetX, tween(150))
                                }
                                onDismiss()
                            }
                        } else {
                            scope.launch {
                                launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                                launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            }
                        }
                    },
                    onDragCancel = {
                        scope.launch {
                            launch { offsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                            launch { offsetY.animateTo(0f, spring(stiffness = Spring.StiffnessMedium)) }
                        }
                    }
                )
            }
            .clip(HeadsUpBannerShape)
            .clickable(onClick = onTap)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 14.dp, end = 14.dp, top = 12.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(visual.containerColor),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(visual.iconRes),
                        contentDescription = null,
                        tint = visual.accentColor,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        if (timeLabel.isNotEmpty()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = timeLabel,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = PlusJakartaSans,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textMuted
                            )
                        }
                    }
                    Text(
                        text = item.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 28.dp, height = 3.dp)
                        .clip(HomePillShape)
                        .background(colors.border)
                )
            }
        }
    }
}

@Composable
fun DashboardGreetingBanner(
    userName: String,
    greetingHour: Int? = null
) {
    val colors = ShiftlyColors.current
    val resolvedHour = remember(greetingHour) { greetingHour ?: LocalTime.now().hour }
    val greeting = remember(resolvedHour) { greetingForHour(resolvedHour) }
    val display = remember(userName) { userName.trim().ifEmpty { "Shiftly" } }

    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = greeting,
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = PlusJakartaSans),
            color = colors.textSecondary
        )
        Text(
            text = display,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontFamily = PlusJakartaSans,
                fontWeight = FontWeight.Bold
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.semantics { heading() }
        )
    }
}

@Composable
fun HomeHeroPager(
    state: HomeUiState,
    onOpenGoalDetail: (goalId: Long?) -> Unit,
    onOpenAddGoal: () -> Unit,
    onSelectActiveGoal: (Long) -> Unit
) {
    val colors = ShiftlyColors.current
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val currency = state.currencySymbol
    val uniformHeroHeight = 204.dp

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        HorizontalPager(
            state = pagerState,
            key = { it },
            contentPadding = PaddingValues(horizontal = 20.dp),
            pageSpacing = 12.dp
        ) { page ->
            when (page) {
                0 -> {
                    ShiftlyGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, state.totalBalance),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = colors.textPrimary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                HeroMetricPill(
                                    label = "Income",
                                    amount = ShiftlyFormatters.formatCurrency(currency, state.totalIncome, showPlus = true),
                                    isIncome = true,
                                    modifier = Modifier.weight(1f)
                                )
                                HeroMetricPill(
                                    label = "Expenses",
                                    amount = ShiftlyFormatters.formatCurrency(currency, state.totalExpenses, showMinus = true),
                                    isIncome = false,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                1 -> {
                    val cutoff = state.cutoffSummary
                    ShiftlyGlassCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Shift Earnings",
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = cutoff.window.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Medium
                                        ).tabular(),
                                        color = colors.mintTextOnSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, cutoff.totalEarnings),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = colors.mintTextOnSurface
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                val daysLabel = if (cutoff.shiftDaysCount == 1) "1 day" else "${cutoff.shiftDaysCount} days"
                                GlassSubStatPill(
                                    label = "Shift Days",
                                    value = daysLabel,
                                    modifier = Modifier.weight(1f)
                                )
                                GlassSubStatPill(
                                    label = "Shift Hours",
                                    value = ShiftlyFormatters.formatHours(cutoff.totalHours),
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }
                }

                else -> {
                    SavingsGoalsHeroCard(
                        goals = state.savingsGoals,
                        currencySymbol = currency,
                        onOpenGoalDetail = onOpenGoalDetail,
                        onOpenAddGoal = onOpenAddGoal,
                        onSelectActiveGoal = onSelectActiveGoal,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight)
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { index ->
                val active = pagerState.currentPage == index
                Box(
                    modifier = Modifier
                        .clip(HomePillShape)
                        .clickable(
                            role = Role.Tab,
                            onClick = {
                                scope.launch { pagerState.animateScrollToPage(index) }
                            }
                        )
                        .semantics {
                            selected = active
                            contentDescription = "${HERO_PAGE_LABELS[index]}, card ${index + 1} of 3"
                        }
                        .padding(horizontal = 6.dp, vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = if (active) 20.dp else 7.dp, height = 7.dp)
                            .clip(CircleShape)
                            .background(if (active) colors.azureAccent else colors.border)
                    )
                }
            }
        }
    }
}

@Composable
private fun SavingsGoalsHeroCard(
    goals: List<SavingsGoalCardItem>,
    currencySymbol: String,
    onOpenGoalDetail: (goalId: Long?) -> Unit,
    onOpenAddGoal: () -> Unit,
    onSelectActiveGoal: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    var selectedGoalId by remember { mutableStateOf<Long?>(null) }

    val resolved = remember(goals, selectedGoalId) {
        val effectiveGoalId = selectedGoalId?.takeIf { id ->
            goals.any { it.goalWithDeposits.goal.id == id }
        } ?: goals.firstOrNull { it.goalWithDeposits.goal.isActive }?.goalWithDeposits?.goal?.id
            ?: goals.firstOrNull()?.goalWithDeposits?.goal?.id
        val idx = goals.indexOfFirst { it.goalWithDeposits.goal.id == effectiveGoalId }.coerceAtLeast(0)
        idx to goals.getOrNull(idx)
    }
    val safeIdx = resolved.first
    val current = resolved.second

    ShiftlyGlassCard(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp)
    ) {
        if (current == null) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Savings Goals",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                            color = colors.textSecondary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_piggy_bank),
                            contentDescription = null,
                            tint = colors.mintTextOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, 0.0),
                        style = MaterialTheme.typography.displayMedium.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 30.sp
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Set a target amount and date goal to track daily savings.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        maxLines = 2
                    )
                }

                Surface(
                    shape = HomePillShape,
                    color = colors.azurePrimary,
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .clip(HomePillShape)
                        .clickable(onClick = onOpenAddGoal)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_plus),
                            contentDescription = null,
                            tint = colors.onAzurePrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add Savings Goal",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSans),
                            color = colors.onAzurePrimary
                        )
                    }
                }
            }
        } else {
            val goal = current.goalWithDeposits.goal
            val progress = current.progress
            val targetDateLabel = remember(current, dateFormatter) {
                current.targetDate.format(dateFormatter)
            }

            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = goal.title,
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (goals.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.card)
                                        .border(1.dp, colors.cardBorder, CircleShape)
                                        .clickable {
                                            val prev = if (safeIdx > 0) safeIdx - 1 else goals.lastIndex
                                            val targetId = goals[prev].goalWithDeposits.goal.id
                                            selectedGoalId = targetId
                                            onSelectActiveGoal(targetId)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_chevron_left),
                                        contentDescription = "Previous goal",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                                Text(
                                    text = "${safeIdx + 1}/${goals.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Medium
                                    ).tabular(),
                                    color = colors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.card)
                                        .border(1.dp, colors.cardBorder, CircleShape)
                                        .clickable {
                                            val next = if (safeIdx < goals.lastIndex) safeIdx + 1 else 0
                                            val targetId = goals[next].goalWithDeposits.goal.id
                                            selectedGoalId = targetId
                                            onSelectActiveGoal(targetId)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_lucide_chevron_right),
                                        contentDescription = "Next goal",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Goal • $targetDateLabel",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = PlusJakartaSans,
                                        fontWeight = FontWeight.Medium
                                    ).tabular(),
                                    color = colors.mintTextOnSurface
                                )
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = ShiftlyFormatters.formatCurrency(currencySymbol, progress.savedAmount),
                            style = MaterialTheme.typography.displayMedium.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ).tabular(),
                            color = colors.mintTextOnSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        LinearProgressIndicator(
                            progress = { progress.progressFraction },
                            color = colors.mintBright,
                            trackColor = colors.border,
                            strokeCap = StrokeCap.Round,
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        GlassSubStatPill(
                            label = "Left to Save",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, progress.leftToSave),
                            modifier = Modifier.weight(1f),
                            compact = true
                        )
                        val perDayValue = if (progress.isCompleted) {
                            "Goal reached"
                        } else {
                            ShiftlyFormatters.formatCurrency(currencySymbol, progress.neededPerDay)
                        }
                        GlassSubStatPill(
                            label = "Per Day",
                            value = perDayValue,
                            modifier = Modifier.weight(1f),
                            compact = true
                        )
                    }

                    Surface(
                        shape = HomePillShape,
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.cardBorder),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HomePillShape)
                            .clickable { onOpenGoalDetail(goal.id) }
                    ) {
                        Text(
                            text = "View Goals",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = PlusJakartaSans,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HeroMetricPill(
    label: String,
    amount: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier.heightIn(min = 82.dp),
        shape = HomeMetricTileShape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 13.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(
                    if (isIncome) R.drawable.ic_lucide_trending_up else R.drawable.ic_lucide_trending_down
                ),
                contentDescription = null,
                tint = if (isIncome) colors.mintTextOnSurface else colors.coralTextOnSurface,
                modifier = Modifier.size(20.dp)
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.textSecondary
                )
                Text(
                    text = amount,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    ).tabular(),
                    color = if (isIncome) colors.mintTextOnSurface else colors.coralTextOnSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun GlassSubStatPill(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier.heightIn(min = if (compact) 60.dp else 82.dp),
        shape = if (compact) HomeCompactTileShape else HomeMetricTileShape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = if (compact) 13.dp else 14.dp,
                vertical = if (compact) 10.dp else 16.dp
            ),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textSecondary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 15.sp else 16.sp
                ).tabular(),
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Monthly top expense categories analytics card with "View insights" placed at the top-right corner.
 */
@Composable
fun MonthlyTopCategoriesCard(
    monthName: String,
    monthlyTotalSpent: Double,
    topCategories: List<CategorySpendSummary>,
    currencySymbol: String,
    onViewInsights: () -> Unit
) {
    val colors = ShiftlyColors.current
    val topThree = remember(topCategories) { topCategories.take(3) }
    Surface(
        shape = HomeCardShape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 6.dp, end = 6.dp)
                    .heightIn(min = 40.dp)
                    .clip(HomePillShape)
                    .clickable(onClick = onViewInsights)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chart_column),
                    contentDescription = null,
                    tint = colors.azureInk,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = "View insights",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = PlusJakartaSans),
                    color = colors.azureInk
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.padding(end = 118.dp)) {
                    Text(
                        text = "Top Spending • $monthName",
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalSpent),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                }

                if (topThree.isEmpty()) {
                    Text(
                        text = "No expenses logged in $monthName yet.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        topThree.forEach { item ->
                            val pct = (item.fraction * 100f).roundToInt()
                            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(colors.coralContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                painter = painterResource(categoryIcon(item.category)),
                                                contentDescription = null,
                                                tint = colors.coralInk,
                                                modifier = Modifier.size(15.dp)
                                            )
                                        }
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "$pct% • ${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)}",
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontFamily = PlusJakartaSans,
                                            fontWeight = FontWeight.Bold
                                        ).tabular(),
                                        color = colors.textPrimary
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { item.fraction.coerceIn(0.05f, 1f) },
                                    color = colors.coralBright,
                                    trackColor = colors.cardElevated,
                                    strokeCap = StrokeCap.Round,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
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
 * Full-month hours indicator card placed below Top Spending (Regular/Total = primary text, Night Diff = Purple, Overtime = Red).
 */
@Composable
fun MonthlyHoursIndicatorCard(
    monthName: String,
    summary: MonthlyHoursSummary
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = HomeCardShape,
        color = colors.card,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
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
                    text = "Shift Hours • $monthName",
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = PlusJakartaSans),
                    color = colors.textSecondary
                )
                Text(
                    text = "Full month",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.textSecondary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MonthlyHoursMetricTile(
                    label = "Total Hours",
                    hours = summary.totalHours,
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                MonthlyHoursMetricTile(
                    label = "Night Diff",
                    hours = summary.nightDiffHours,
                    valueColor = colors.nightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                MonthlyHoursMetricTile(
                    label = "Overtime",
                    hours = summary.overtimeHours,
                    valueColor = colors.coralBright,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun MonthlyHoursMetricTile(
    label: String,
    hours: Double,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Surface(
        modifier = modifier,
        shape = HomeCompactTileShape,
        color = colors.surface,
        border = BorderStroke(1.dp, colors.cardBorder),
        shadowElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = ShiftlyFormatters.formatHours(hours, compact = true),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = valueColor
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = PlusJakartaSans,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun PendingSplitsBanner(
    pendingAmount: Double,
    billCount: Int,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    Surface(
        shape = HomeBannerShape,
        color = colors.azureContainer,
        border = BorderStroke(1.dp, colors.azureAccent.copy(alpha = 0.35f)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(HomeBannerShape)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_split),
                    contentDescription = null,
                    tint = colors.azureInk,
                    modifier = Modifier.size(21.dp)
                )
                Column {
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, pendingAmount)} owed to you",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = PlusJakartaSans,
                            fontWeight = FontWeight.Bold
                        ).tabular(),
                        color = colors.textPrimary
                    )
                    Text(
                        text = "$billCount split bill(s) awaiting payment",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
            Icon(
                painter = painterResource(R.drawable.ic_lucide_arrow_right),
                contentDescription = "Open Split Bill",
                tint = colors.azureInk,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
fun HomeTimelineRow(
    item: TimelineActivityItem,
    currencySymbol: String,
    onClick: () -> Unit = {}
) {
    val colors = ShiftlyColors.current
    val isIncome = item is TimelineActivityItem.ShiftActivity || item is TimelineActivityItem.ExtraIncomeActivity
    val iconRes = when (item) {
        is TimelineActivityItem.ShiftActivity -> R.drawable.ic_lucide_clock
        is TimelineActivityItem.ExtraIncomeActivity -> R.drawable.ic_lucide_trending_up
        is TimelineActivityItem.ExpenseActivity -> R.drawable.ic_lucide_receipt_text
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(HomeTimelineRowShape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 4.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .clip(CircleShape)
                .background(if (isIncome) colors.mintContainer else colors.coralContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = if (isIncome) colors.mintInk else colors.coralInk,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            when (item) {
                is TimelineActivityItem.ShiftActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.timeRange} • ${item.durationLabel}",
                        style = MaterialTheme.typography.bodySmall.tabular(),
                        color = colors.textSecondary
                    )
                }

                is TimelineActivityItem.ExtraIncomeActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.subtitleTag} • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                is TimelineActivityItem.ExpenseActivity -> {
                    Text(
                        text = item.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${item.category} • ${item.timeLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }
            }
        }

        when (item) {
            is TimelineActivityItem.ShiftActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showPlus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            is TimelineActivityItem.ExtraIncomeActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showPlus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            is TimelineActivityItem.ExpenseActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showMinus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = PlusJakartaSans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.coralInk
                )
            }
        }
    }
}
