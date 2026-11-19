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
import androidx.compose.foundation.background
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlyVerticalHairlineDivider
import com.example.shiftly.ui.screens.expense.CategorySpendSummary
import com.example.shiftly.ui.screens.expense.categoryIcon
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.PureWhite
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
private val HomeCardShape = RoundedCornerShape(18.dp)
private val HomeBannerShape = RoundedCornerShape(18.dp)
private val HomeButtonShape = RoundedCornerShape(12.dp)
private val InboxDropdownShape = RoundedCornerShape(20.dp)
private val HeadsUpBannerShape = RoundedCornerShape(18.dp)
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
    val tileColor: Color
)

@Composable
private fun notificationVisualStyle(type: InAppNotificationType): NotificationVisualStyle {
    val colors = ShiftlyColors.current
    return when (type) {
        InAppNotificationType.BILL_DUE -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_repeat,
            tileColor = colors.iosOrange
        )
        InAppNotificationType.BILL_POSTED -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_receipt_text,
            tileColor = colors.coralBright
        )
        InAppNotificationType.SHIFT_REMINDER -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_clock,
            tileColor = colors.azurePrimary
        )
        InAppNotificationType.SAVINGS_REMINDER -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_piggy_bank,
            tileColor = colors.mintBright
        )
        InAppNotificationType.PAYDAY_CUTOFF -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_wallet,
            tileColor = colors.mintBright
        )
        InAppNotificationType.SPLIT_BILL -> NotificationVisualStyle(
            iconRes = R.drawable.ic_lucide_split,
            tileColor = colors.azurePrimary
        )
    }
}

/**
 * Top header displaying small uppercase date above the Home Large Title,
 * plus notifications and profile avatar.
 */
@Composable
fun HomeGreetingHeader(
    profilePhotoPath: String? = null,
    unreadCount: Int = 0,
    isInboxOpen: Boolean = false,
    todayOverride: LocalDate? = null,
    onNotificationClick: () -> Unit,
    onProfileClick: () -> Unit,
    userName: String = "",
    greetingHourOverride: Int? = null
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val today = remember(todayOverride) { todayOverride ?: LocalDate.now() }
    val fullDateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEEE, MMM d", locale) }
    val formattedDate = remember(today, fullDateFormatter) { today.format(fullDateFormatter).uppercase(locale) }
    val resolvedHour = remember(greetingHourOverride) { greetingHourOverride ?: LocalTime.now().hour }
    val greeting = remember(resolvedHour) { greetingForHour(resolvedHour) }
    val display = remember(userName) { userName.trim().ifEmpty { "Shiftly" } }

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
    val avatarSizePx = with(LocalDensity.current) { 44.dp.roundToPx() }
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
        verticalAlignment = Alignment.Top
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(top = 4.dp, end = 12.dp)
        ) {
            Text(
                text = formattedDate,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    letterSpacing = 0.5.sp
                ).tabular(),
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = if (userName.isNotBlank()) "$greeting $display" else greeting.trimEnd(','),
                style = MaterialTheme.typography.displaySmall.copy(lineHeight = 38.sp),
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.semantics { heading() }
            )
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(contentAlignment = Alignment.Center) {
                IconButton(
                    onClick = onNotificationClick,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(if (isInboxOpen) colors.azurePrimary else colors.card)
                        .semantics { contentDescription = notificationDescription }
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_bell),
                        contentDescription = null,
                        tint = if (isInboxOpen) PureWhite else colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 4.dp, end = 4.dp)
                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(CircleShape)
                            .background(colors.coralBright)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (unreadCount > 9) "9+" else unreadCount.toString(),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Bold
                            ).tabular(),
                            color = PureWhite,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            IconButton(
                onClick = onProfileClick,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.card)
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
                        modifier = Modifier.size(20.dp)
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
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
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
                                            fontFamily = InterFontFamily,
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
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = if (notifications.isNotEmpty()) colors.azureAccent else colors.disabledContent
                            )
                        }
                    }

                    ShiftlyHairlineDivider()

                    if (notifications.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 24.dp, vertical = 28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            ShiftlyIosIconTile(
                                iconRes = R.drawable.ic_lucide_bell,
                                backgroundColor = colors.cardElevated,
                                size = 44.dp,
                                cornerRadius = 11.dp,
                                iconSize = 20.dp,
                                iconTint = colors.textSecondary
                            )
                            Text(
                                text = "All caught up",
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = InterFontFamily,
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
                                    ShiftlyHairlineDivider(startInset = 64.dp)
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
                if (!item.isRead) colors.azureContainer.copy(alpha = if (colors.isDark) 0.25f else 0.14f)
                else Color.Transparent
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        ShiftlyIosIconTile(
            iconRes = visual.iconRes,
            backgroundColor = visual.tileColor,
            size = 36.dp,
            cornerRadius = 9.dp,
            iconSize = 18.dp
        )

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
                        fontFamily = InterFontFamily,
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
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary
                        )
                    }
                    if (!item.isRead) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(colors.azureAccent)
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
        color = colors.cardElevated,
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
                ShiftlyIosIconTile(
                    iconRes = visual.iconRes,
                    backgroundColor = visual.tileColor,
                    size = 36.dp,
                    cornerRadius = 9.dp,
                    iconSize = 18.dp
                )

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
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
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
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textSecondary
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
                        .background(colors.separator)
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
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = InterFontFamily),
            color = colors.textSecondary
        )
        Text(
            text = display,
            style = MaterialTheme.typography.displaySmall,
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
    val uniformHeroHeight = 196.dp

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
                        cornerRadius = 18.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight),
                        contentPadding = PaddingValues(18.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Total Balance",
                                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                                    color = colors.textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, state.totalBalance),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = colors.textPrimary
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ShiftlyHairlineDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HeroMetricColumn(
                                        label = "Income",
                                        amount = ShiftlyFormatters.formatCurrency(currency, state.totalIncome, showPlus = true),
                                        isIncome = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ShiftlyVerticalHairlineDivider(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .height(34.dp)
                                    )
                                    HeroMetricColumn(
                                        label = "Expenses",
                                        amount = ShiftlyFormatters.formatCurrency(currency, state.totalExpenses, showMinus = true),
                                        isIncome = false,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                1 -> {
                    val cutoff = state.cutoffSummary
                    ShiftlyGlassCard(
                        cornerRadius = 18.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(uniformHeroHeight),
                        contentPadding = PaddingValues(18.dp)
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
                                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                                        color = colors.textSecondary
                                    )
                                    Text(
                                        text = cutoff.window.label,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium
                                        ).tabular(),
                                        color = colors.mintTextOnSurface
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currency, cutoff.totalEarnings),
                                    style = MaterialTheme.typography.displayMedium.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 32.sp
                                    ).tabular(),
                                    color = colors.mintTextOnSurface
                                )
                            }

                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                ShiftlyHairlineDivider()
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val daysLabel = if (cutoff.shiftDaysCount == 1) "1 day" else "${cutoff.shiftDaysCount} days"
                                    FlatSubStatColumn(
                                        label = "Shift Days",
                                        value = daysLabel,
                                        modifier = Modifier.weight(1f)
                                    )
                                    ShiftlyVerticalHairlineDivider(
                                        modifier = Modifier
                                            .padding(horizontal = 12.dp)
                                            .height(34.dp)
                                    )
                                    FlatSubStatColumn(
                                        label = "Shift Hours",
                                        value = ShiftlyFormatters.formatHours(cutoff.totalHours),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
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
                            .background(if (active) colors.azureAccent else colors.separator)
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
        cornerRadius = 18.dp,
        modifier = modifier,
        contentPadding = PaddingValues(18.dp)
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
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
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
                            fontFamily = InterFontFamily,
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
                            tint = PureWhite,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Add Savings Goal",
                            style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                            color = PureWhite
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
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (goals.size > 1) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.cardElevated)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                Text(
                                    text = "${safeIdx + 1}/${goals.size}",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Medium
                                    ).tabular(),
                                    color = colors.textSecondary
                                )
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .clip(CircleShape)
                                        .background(colors.cardElevated)
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
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Goal • $targetDateLabel",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
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
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            ).tabular(),
                            color = colors.mintTextOnSurface,
                            maxLines = 1,
                            softWrap = false
                        )
                        ShiftlyProgressBar(
                            progress = progress.progressFraction,
                            color = colors.mintBright,
                            trackColor = colors.cardElevated,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        FlatSubStatColumn(
                            label = "Left to Save",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, progress.leftToSave),
                            modifier = Modifier.weight(1f)
                        )
                        ShiftlyVerticalHairlineDivider(
                            modifier = Modifier
                                .padding(horizontal = 12.dp)
                                .height(30.dp)
                        )
                        val perDayValue = if (progress.isCompleted) {
                            "Goal reached"
                        } else {
                            ShiftlyFormatters.formatCurrency(currencySymbol, progress.neededPerDay)
                        }
                        FlatSubStatColumn(
                            label = "Per Day",
                            value = perDayValue,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Surface(
                        shape = HomeButtonShape,
                        color = colors.cardElevated,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(HomeButtonShape)
                            .clickable { onOpenGoalDetail(goal.id) }
                    ) {
                        Text(
                            text = "View Goals",
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.azureAccent,
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
private fun HeroMetricColumn(
    label: String,
    amount: String,
    isIncome: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            painter = painterResource(
                if (isIncome) R.drawable.ic_lucide_trending_up else R.drawable.ic_lucide_trending_down
            ),
            contentDescription = null,
            tint = if (isIncome) colors.mintTextOnSurface else colors.coralTextOnSurface,
            modifier = Modifier.size(18.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ),
                color = colors.textSecondary
            )
            Text(
                text = amount,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Bold
                ).tabular(),
                color = if (isIncome) colors.mintTextOnSurface else colors.coralTextOnSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun FlatSubStatColumn(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Medium
            ),
            color = colors.textSecondary
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        color = colors.cardElevated,
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
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
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
                        style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                        color = colors.textSecondary
                    )
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotalSpent),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontFamily = InterFontFamily,
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
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        ShiftlyIosIconTile(
                                            iconRes = categoryIcon(item.category),
                                            backgroundColor = colors.coralBright,
                                            size = 28.dp,
                                            cornerRadius = 8.dp,
                                            iconSize = 14.dp
                                        )
                                        Text(
                                            text = item.category,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = colors.textPrimary
                                        )
                                    }
                                    Text(
                                        text = "$pct% • ${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)}",
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
                                    trackColor = colors.separator
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
        color = colors.cardElevated,
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
                    style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                    color = colors.textSecondary
                )
                Text(
                    text = "Full month",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colors.textSecondary
                )
            }

            ShiftlyHairlineDivider()

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                MonthlyHoursMetricColumn(
                    label = "Total Hours",
                    hours = summary.totalHours,
                    valueColor = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                ShiftlyVerticalHairlineDivider(modifier = Modifier.height(34.dp))
                MonthlyHoursMetricColumn(
                    label = "Night Diff",
                    hours = summary.nightDiffHours,
                    valueColor = colors.nightDiffPurple,
                    modifier = Modifier.weight(1f)
                )
                ShiftlyVerticalHairlineDivider(modifier = Modifier.height(34.dp))
                MonthlyHoursMetricColumn(
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
private fun MonthlyHoursMetricColumn(
    label: String,
    hours: Double,
    valueColor: Color,
    modifier: Modifier = Modifier
) {
    val colors = ShiftlyColors.current
    Column(
        modifier = modifier.padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = ShiftlyFormatters.formatHours(hours, compact = true),
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.Bold
            ).tabular(),
            color = valueColor
        )
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
        color = colors.cardElevated,
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
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                ShiftlyIosIconTile(
                    iconRes = R.drawable.ic_lucide_split,
                    backgroundColor = colors.azurePrimary,
                    size = 36.dp,
                    cornerRadius = 9.dp,
                    iconSize = 18.dp
                )
                Column {
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, pendingAmount)} owed to you",
                        style = MaterialTheme.typography.titleSmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold
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
                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                contentDescription = "Open Split Bill",
                tint = colors.textSecondary,
                modifier = Modifier.size(18.dp)
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
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ShiftlyIosIconTile(
            iconRes = iconRes,
            backgroundColor = if (isIncome) colors.mintBright else colors.coralBright,
            size = 38.dp,
            cornerRadius = 9.dp,
            iconSize = 18.dp
        )

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
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            is TimelineActivityItem.ExtraIncomeActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showPlus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.mintInk
                )
            }

            is TimelineActivityItem.ExpenseActivity -> {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, item.amount, showMinus = true),
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ).tabular(),
                    color = colors.coralInk
                )
            }
        }
    }
}
