package com.example.shiftly.ui.screens.expense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.size.Scale
import com.example.shiftly.R
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ReceiptStorageHelper
import com.example.shiftly.ui.components.ShiftlyDialogShell
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyHairlineDivider
import com.example.shiftly.ui.components.ShiftlyIosIconTile
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlySecondaryTextButton
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlyPressEffect
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val ExpensePillShape = RoundedCornerShape(50)
private val ExpenseItemShape = RoundedCornerShape(18.dp)
private val ExpenseFieldShape = RoundedCornerShape(12.dp)
private val ExpenseChipShape = RoundedCornerShape(10.dp)
private val ExpenseThumbShape = RoundedCornerShape(10.dp)
private val ExpenseSheetTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
private val ExpenseCategorySheetTopShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

@DrawableRes
fun categoryIcon(category: String): Int {
    return when (category.lowercase()) {
        "food" -> R.drawable.ic_lucide_utensils
        "transport" -> R.drawable.ic_lucide_car
        "bills" -> R.drawable.ic_lucide_receipt
        "supplies" -> R.drawable.ic_lucide_package
        "savings" -> R.drawable.ic_lucide_piggy_bank
        "split bill" -> R.drawable.ic_lucide_split
        else -> R.drawable.ic_lucide_shopping_bag
    }
}

@Composable
fun ExpenseSummaryCard(
    monthlyTotal: Double,
    allTimeTotal: Double,
    monthlyBudget: Double = 0.0,
    totalReceiptCount: Int,
    categoryBreakdown: List<CategorySpendSummary>,
    currencySymbol: String,
    selectedMonth: YearMonth? = YearMonth.now()
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val periodLabel = remember(selectedMonth, locale) {
        if (selectedMonth == null) {
            "ALL TIME"
        } else {
            selectedMonth.format(DateTimeFormatter.ofPattern("MMM yyyy", locale)).uppercase()
        }
    }
    val topLegendCategories = remember(categoryBreakdown) { categoryBreakdown.take(4) }
    val budgetedCategories = remember(categoryBreakdown) {
        categoryBreakdown.filter { (it.monthlyLimit ?: 0.0) > 0.0 }
    }
    val segmentPalette = remember(
        colors.coralBright,
        colors.azureAccent,
        colors.mintBright,
        colors.nightDiffPurple,
        colors.iosOrange
    ) {
        listOf(
            colors.coralBright,
            colors.azureAccent,
            colors.mintBright,
            colors.nightDiffPurple,
            colors.iosOrange
        )
    }

    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = periodLabel,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotal),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        ).tabular(),
                        color = colors.coralTextOnSurface
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = ExpensePillShape,
                        color = colors.cardElevated,
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "All-time: ${ShiftlyFormatters.formatCurrency(currencySymbol, allTimeTotal)}",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (totalReceiptCount > 0) {
                        Text(
                            text = "$totalReceiptCount receipt(s) saved",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (selectedMonth != null && monthlyBudget > 0.0) {
                val budgetUsed = (monthlyTotal / monthlyBudget).toFloat().coerceIn(0f, 1f)
                val isOver = monthlyTotal > monthlyBudget
                val isNear = monthlyTotal >= monthlyBudget * 0.85
                val remaining = monthlyBudget - monthlyTotal

                Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Monthly Budget",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (isOver) {
                                "Over by ${ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotal - monthlyBudget)}"
                            } else {
                                "${ShiftlyFormatters.formatCurrency(currencySymbol, remaining)} left"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.SemiBold
                            ).tabular(),
                            color = if (isNear) colors.coralTextOnSurface else colors.mintTextOnSurface
                        )
                    }
                    ShiftlyProgressBar(
                        progress = budgetUsed,
                        color = if (isNear) colors.coralBright else colors.mintBright,
                        trackColor = colors.cardElevated
                    )
                }
            }

            if (categoryBreakdown.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(ExpensePillShape)
                        .background(colors.cardElevated),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categoryBreakdown.forEachIndexed { index, item ->
                        if (item.fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(item.fraction.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .clip(ExpensePillShape)
                                    .background(segmentPalette[index % segmentPalette.size])
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    topLegendCategories.forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(segmentPalette[index % segmentPalette.size])
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.category} (${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)})",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                if (budgetedCategories.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        budgetedCategories.forEach { item ->
                            val limit = item.monthlyLimit ?: return@forEach
                            val usedFraction = (item.totalAmount / limit).toFloat().coerceIn(0f, 1f)
                            val isNearOrOver = item.totalAmount >= limit * 0.85
                            val isOver = item.totalAmount > limit
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = item.category,
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium
                                        ),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = if (isOver) {
                                            "${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)} / ${ShiftlyFormatters.formatCurrency(currencySymbol, limit)} (Over)"
                                        } else {
                                            "${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)} / ${ShiftlyFormatters.formatCurrency(currencySymbol, limit)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.copy(
                                            fontFamily = InterFontFamily,
                                            fontWeight = FontWeight.Medium
                                        ).tabular(),
                                        color = if (isNearOrOver) colors.coralTextOnSurface else colors.mintTextOnSurface
                                    )
                                }
                                ShiftlyProgressBar(
                                    progress = usedFraction,
                                    color = if (isNearOrOver) colors.coralBright else colors.mintBright,
                                    trackColor = colors.cardElevated
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpenseCategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    val haptics = LocalHapticFeedback.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithCache {
                val fadeBrush = Brush.horizontalGradient(
                    0.0f to Color.Black,
                    0.88f to Color.Black,
                    1.0f to Color.Transparent
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(
                        brush = fadeBrush,
                        blendMode = BlendMode.DstIn
                    )
                }
            }
            .horizontalScroll(rememberScrollState())
            .selectableGroup()
            .padding(start = 20.dp, end = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val isSelected = selectedCategory == category
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(ExpensePillShape)
                    .background(if (isSelected) colors.azurePrimary else colors.card)
                    .shiftlyPressClickable(role = Role.Tab) {
                        if (!isSelected) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onSelectCategory(category)
                    }
                    .semantics(mergeDescendants = true) {
                        selected = isSelected
                        contentDescription = "$category expenses filter"
                    }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (isSelected) colors.onAzurePrimary else colors.textSecondary
                )
            }
        }
    }
}

@Composable
fun ExpenseEmptyCard(
    selectedCategory: String,
    onAddExpense: () -> Unit,
    onShowAll: () -> Unit,
    selectedMonth: YearMonth? = YearMonth.now()
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val monthLabel = remember(selectedMonth, locale) {
        selectedMonth?.format(DateTimeFormatter.ofPattern("MMM yyyy", locale))
    }
    val isCategoryFiltered = selectedCategory != "All"
    val isMonthFiltered = selectedMonth != null
    val isFiltered = isCategoryFiltered || isMonthFiltered
    val emptyTitle = when {
        isCategoryFiltered && monthLabel != null -> "No $selectedCategory expenses in $monthLabel"
        isCategoryFiltered -> "No $selectedCategory expenses"
        monthLabel != null -> "No expenses in $monthLabel"
        else -> "No expenses logged yet"
    }
    val subtitle = if (isFiltered) {
        "Try clearing the active filter or tap + Add above."
    } else {
        "Start tracking your daily spending and save receipts."
    }
    val buttonText = when {
        isCategoryFiltered -> "Show All Expenses"
        isMonthFiltered -> "Show All Months"
        else -> "Log your first expense"
    }
    ShiftlyEmptyState(
        iconRes = R.drawable.ic_lucide_receipt_text,
        title = emptyTitle,
        subtitle = subtitle,
        actionText = buttonText,
        onAction = if (isFiltered) onShowAll else onAddExpense,
        iconBackgroundColor = colors.coralBright
    )
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    currencySymbol: String,
    onViewReceipt: (ExpenseEntity) -> Unit,
    onDelete: () -> Unit,
    onClick: () -> Unit = {},
    dateFormatter: DateTimeFormatter? = null,
    containerColor: Color = ShiftlyColors.current.cardElevated,
    shape: Shape = ExpenseItemShape
) {
    val colors = ShiftlyColors.current
    val resolvedFormatter = dateFormatter ?: run {
        val locale = LocalConfiguration.current.locales[0]
        remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    }
    val dateLabel = remember(expense.dateEpochDay, resolvedFormatter) {
        LocalDate.ofEpochDay(expense.dateEpochDay).format(resolvedFormatter)
    }
    val hasReceipt by produceState(initialValue = false, expense.receiptImagePath) {
        value = withContext(Dispatchers.IO) {
            val path = expense.receiptImagePath
            !path.isNullOrBlank() && File(path).exists()
        }
    }
    val isSplitItem = expense.category.equals("Split Bill", ignoreCase = true)
    val isSavingsItem = expense.category.equals("Savings", ignoreCase = true)

    Surface(
        shape = shape,
        color = containerColor,
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shiftlyPressClickable(onClickLabel = "Edit ${expense.title}", onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShiftlyIosIconTile(
                iconRes = categoryIcon(expense.category),
                backgroundColor = when {
                    isSavingsItem -> colors.mintBright
                    isSplitItem -> colors.azurePrimary
                    else -> colors.coralBright
                },
                size = 28.dp,
                cornerRadius = 9.dp,
                iconSize = 22.dp,
                contentDescription = expense.category
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.category}  •  $dateLabel",
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily),
                    color = colors.textSecondary
                )

                if (hasReceipt) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(ExpenseChipShape)
                            .shiftlyPressClickable(onClickLabel = "View receipt for ${expense.title}") {
                                onViewReceipt(expense)
                            }
                            .semantics(mergeDescendants = true) {
                                contentDescription = "View receipt for ${expense.title}"
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            shape = ExpensePillShape,
                            color = colors.azureContainer,
                            shadowElevation = 0.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_paperclip),
                                    contentDescription = null,
                                    tint = colors.azureInk,
                                    modifier = Modifier.size(13.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Receipt",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.azureInk
                                )
                            }
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = ShiftlyFormatters.formatCurrency(currencySymbol, expense.amount, showMinus = true),
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.coralInk
                )
                val deleteInteraction = remember { MutableInteractionSource() }
                IconButton(
                    onClick = onDelete,
                    interactionSource = deleteInteraction,
                    modifier = Modifier
                        .size(48.dp)
                        .shiftlyPressEffect(deleteInteraction)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_trash_2),
                        contentDescription = "Delete Expense ${expense.title}",
                        tint = colors.coralInk.copy(alpha = 0.85f),
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseBottomSheet(
    currencySymbol: String,
    availableCategories: List<String> = ExpenseViewModel.EXPENSE_CATEGORIES,
    editingExpense: ExpenseEntity? = null,
    onDismiss: () -> Unit,
    onSaveExpense: (title: String, amount: Double, category: String, date: LocalDate, receiptPath: String?) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    val originalReceiptPath = editingExpense?.receiptImagePath
    var savedReceiptPath by remember(editingExpense?.id) {
        mutableStateOf(originalReceiptPath)
    }

    val effectiveCategories = remember(availableCategories, editingExpense) {
        val base = availableCategories.ifEmpty { ExpenseViewModel.EXPENSE_CATEGORIES }
        val existingCat = editingExpense?.category?.trim().orEmpty()
        if (existingCat.isNotBlank() && base.none { it.equals(existingCat, ignoreCase = true) }) {
            base + existingCat
        } else {
            base
        }
    }

    val initialAmount = remember(editingExpense?.id) {
        editingExpense?.let {
            val rounded = (it.amount * 100.0).roundToLong()
            if (rounded % 100L == 0L) (rounded / 100L).toString()
            else "%.2f".format(Locale.US, it.amount)
        }.orEmpty()
    }
    val initialDate = remember(editingExpense?.id) {
        editingExpense?.let { LocalDate.ofEpochDay(it.dateEpochDay) } ?: LocalDate.now()
    }
    val initialTime = remember(editingExpense?.id) {
        editingExpense?.createdAtMillis
            ?.takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(ZoneId.systemDefault()).toLocalTime() }
            ?: LocalTime.now()
    }

    val dismissWithoutSaving: () -> Unit = {
        val draftPath = savedReceiptPath
        if (!draftPath.isNullOrBlank() && draftPath != originalReceiptPath) {
            scope.launch(Dispatchers.IO) {
                ReceiptStorageHelper.deleteReceiptFile(draftPath)
            }
        }
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = dismissWithoutSaving,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        shape = ExpenseSheetTopShape,
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        AddExpenseSheetContent(
            currencySymbol = currencySymbol,
            savedReceiptPath = savedReceiptPath,
            onReceiptChanged = { savedReceiptPath = it },
            onDismiss = dismissWithoutSaving,
            onSaveExpense = { title, amount, category, date, path ->
                onSaveExpense(title, amount, category, date, path)
                onDismiss()
            },
            availableCategories = effectiveCategories,
            initialAmount = initialAmount,
            initialTitle = editingExpense?.title.orEmpty(),
            initialCategory = editingExpense?.category ?: "Food",
            initialDate = initialDate,
            initialTime = initialTime,
            isEditing = editingExpense != null,
            editKey = editingExpense?.id,
            originalReceiptPath = originalReceiptPath
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddExpenseSheetContent(
    currencySymbol: String,
    savedReceiptPath: String?,
    onReceiptChanged: (String?) -> Unit,
    onDismiss: () -> Unit,
    onSaveExpense: (title: String, amount: Double, category: String, date: LocalDate, receiptPath: String?) -> Unit,
    availableCategories: List<String> = ExpenseViewModel.EXPENSE_CATEGORIES,
    initialAmount: String = "",
    initialTitle: String = "",
    initialCategory: String = "Food",
    initialDate: LocalDate = LocalDate.now(),
    initialTime: LocalTime = LocalTime.now(),
    isEditing: Boolean = false,
    editKey: Any? = null,
    originalReceiptPath: String? = null
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val suppressNextRelock = LocalSuppressNextRelock.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("hh:mm a", locale) }
    val categoryOptions = remember(availableCategories) {
        availableCategories.ifEmpty { ExpenseViewModel.EXPENSE_CATEGORIES }
    }

    var title by remember(editKey) { mutableStateOf(initialTitle) }
    var amountText by remember(editKey) { mutableStateOf(initialAmount) }
    var selectedCategory by remember(editKey, categoryOptions) {
        mutableStateOf(
            categoryOptions.firstOrNull { it.equals(initialCategory, ignoreCase = true) }
                ?: categoryOptions.firstOrNull()
                ?: "Other"
        )
    }
    var selectedDate by remember(editKey) { mutableStateOf(initialDate) }
    var selectedTime by remember(editKey) { mutableStateOf(initialTime) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryPrompt by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }
    var isProcessingCameraPhoto by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    val amountFocusRequester = remember { FocusRequester() }
    LaunchedEffect(editKey, isEditing) {
        if (!isEditing) {
            amountFocusRequester.requestFocus()
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val previousPath = savedReceiptPath
            coroutineScope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    if (!previousPath.isNullOrBlank() && previousPath != originalReceiptPath) {
                        ReceiptStorageHelper.deleteReceiptFile(previousPath)
                    }
                    ReceiptStorageHelper.saveUriToLocalReceipts(context, uri)
                }
                onReceiptChanged(savedPath)
            }
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        pendingCameraFile = null
        if (success && file != null) {
            isProcessingCameraPhoto = true
            val previousPath = savedReceiptPath
            coroutineScope.launch {
                try {
                    val savedPath = withContext(Dispatchers.IO) {
                        if (file.exists() && file.length() > 0L) {
                            ReceiptStorageHelper.applyTimestampWatermark(file)
                            if (!previousPath.isNullOrBlank() && previousPath != originalReceiptPath) {
                                ReceiptStorageHelper.deleteReceiptFile(previousPath)
                            }
                            file.absolutePath
                        } else {
                            file.delete()
                            null
                        }
                    }
                    if (savedPath != null) {
                        onReceiptChanged(savedPath)
                    }
                } finally {
                    isProcessingCameraPhoto = false
                }
            }
        } else if (file != null) {
            coroutineScope.launch(Dispatchers.IO) {
                file.delete()
            }
        }
    }

    val thumbSizePx = with(LocalDensity.current) { 56.dp.roundToPx() }
    val receiptThumbRequest = remember(savedReceiptPath, thumbSizePx, context) {
        savedReceiptPath?.let { path ->
            ImageRequest.Builder(context)
                .data(File(path))
                .size(thumbSizePx, thumbSizePx)
                .scale(Scale.FILL)
                .crossfade(true)
                .build()
        }
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = title.isNotBlank() && parsedAmount.isFinite() && parsedAmount > 0.0
    val canSubmit = isValid && !isProcessingCameraPhoto

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
        ShiftlySheetHeader(title = if (isEditing) "Edit Expense" else "Add Expense")

        // Card 1: Amount & Title
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Row 1: Amount
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 52.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Amount",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "$currencySymbol ",
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.coralInk
                            )
                        )
                        BasicTextField(
                            value = amountText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) amountText = filtered
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            cursorBrush = SolidColor(colors.coralInk),
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                textAlign = TextAlign.End
                            ).tabular(),
                            decorationBox = { inner ->
                                Box(contentAlignment = Alignment.CenterEnd) {
                                    if (amountText.isEmpty()) {
                                        Text(
                                            text = "0.00",
                                            style = MaterialTheme.typography.titleLarge.copy(
                                                fontFamily = InterFontFamily,
                                                color = colors.textSecondary
                                            ),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                    inner()
                                }
                            },
                            modifier = Modifier
                                .width(120.dp)
                                .focusRequester(amountFocusRequester)
                        )
                    }
                }

                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                // Row 2: Title / Description
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Title",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        cursorBrush = SolidColor(colors.azureAccent),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary,
                            textAlign = TextAlign.End
                        ),
                        decorationBox = { inner ->
                            Box(contentAlignment = Alignment.CenterEnd) {
                                if (title.isEmpty()) {
                                    Text(
                                        text = "e.g., Tapsilog & Coffee",
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = InterFontFamily,
                                            color = colors.textSecondary
                                        ),
                                        textAlign = TextAlign.End
                                    )
                                }
                                inner()
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 16.dp)
                    )
                }
            }
        }

        // Card 2: Category, Date & Time
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.cardElevated,
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Category Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .shiftlyPressClickable { showCategoryPrompt = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ShiftlyIosIconTile(
                            iconRes = categoryIcon(selectedCategory),
                            backgroundColor = colors.coralBright,
                            size = 24.dp,
                            cornerRadius = 6.dp,
                            iconSize = 20.dp
                        )
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ),
                            color = colors.textPrimary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_chevron_right),
                            contentDescription = "Select Category",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                // Date Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .shiftlyPressClickable { showDatePicker = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Date",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_calendar),
                            contentDescription = null,
                            tint = colors.azureInk,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = selectedDate.format(dateFormatter),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }

                ShiftlyHairlineDivider(startInset = 16.dp, endInset = 16.dp)

                // Time Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 50.dp)
                        .shiftlyPressClickable { showTimePicker = true }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Time",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        ),
                        color = colors.textPrimary
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_clock),
                            contentDescription = null,
                            tint = colors.azureInk,
                            modifier = Modifier.size(17.dp)
                        )
                        Text(
                            text = selectedTime.format(timeFormatter),
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Medium
                            ).tabular(),
                            color = colors.textPrimary
                        )
                    }
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Receipt (optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = InterFontFamily),
                color = colors.textSecondary
            )

            if (receiptThumbRequest != null) {
                Surface(
                    shape = ExpenseFieldShape,
                    color = colors.cardElevated,
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AsyncImage(
                            model = receiptThumbRequest,
                            contentDescription = "Receipt Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(ExpenseThumbShape)
                        )
                        Text(
                            text = "Receipt attached",
                            style = MaterialTheme.typography.titleSmall.copy(fontFamily = InterFontFamily),
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        val removeReceiptInteraction = remember { MutableInteractionSource() }
                        IconButton(
                            onClick = {
                                val oldPath = savedReceiptPath
                                onReceiptChanged(null)
                                if (!oldPath.isNullOrBlank() && oldPath != originalReceiptPath) {
                                    coroutineScope.launch(Dispatchers.IO) {
                                        ReceiptStorageHelper.deleteReceiptFile(oldPath)
                                    }
                                }
                            },
                            interactionSource = removeReceiptInteraction,
                            modifier = Modifier
                                .size(48.dp)
                                .shiftlyPressEffect(removeReceiptInteraction)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_x),
                                contentDescription = "Remove Receipt",
                                tint = colors.coralInk,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ShiftlySecondaryTextButton(
                        text = "Gallery",
                        enabled = !isProcessingCameraPhoto,
                        onClick = {
                            suppressNextRelock()
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_image),
                                contentDescription = null,
                                tint = colors.azureAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )

                    ShiftlySecondaryTextButton(
                        text = if (isProcessingCameraPhoto) "Saving..." else "Camera",
                        enabled = !isProcessingCameraPhoto,
                        onClick = {
                            suppressNextRelock()
                            coroutineScope.launch {
                                val created = withContext(Dispatchers.IO) {
                                    runCatching {
                                        ReceiptStorageHelper.createCameraReceiptTarget(context)
                                    }.getOrNull()
                                } ?: return@launch
                                pendingCameraFile = created.first
                                val launched = runCatching {
                                    cameraLauncher.launch(created.second)
                                }.isSuccess
                                if (!launched) {
                                    pendingCameraFile = null
                                    withContext(Dispatchers.IO) { created.first.delete() }
                                }
                            }
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_camera),
                                contentDescription = null,
                                tint = colors.azureAccent,
                                modifier = Modifier.size(18.dp)
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    if (canSubmit) {
                        onSaveExpense(title, parsedAmount, selectedCategory, selectedDate, savedReceiptPath)
                    }
                },
                actionLabel = "Save",
                actionEnabled = canSubmit
            )
        }
    }

    if (showCategoryPrompt) {
        CategoryPromptSheet(
            categoryOptions = categoryOptions,
            selectedCategory = selectedCategory,
            onSelectCategory = { selectedCategory = it },
            onDismiss = { showCategoryPrompt = false }
        )
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedTime.hour,
            initialMinute = selectedTime.minute,
            is24Hour = false
        )
        ShiftlyDialogShell(
            title = "Select Time",
            onDismiss = { showTimePicker = false },
            actionLabel = "Done",
            onAction = {
                selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                showTimePicker = false
            }
        ) {
            Box(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = colors.cardElevated,
                        clockDialSelectedContentColor = colors.onAzurePrimary,
                        clockDialUnselectedContentColor = colors.textSecondary,
                        selectorColor = colors.azurePrimary,
                        containerColor = colors.surface,
                        periodSelectorBorderColor = Color.Transparent,
                        periodSelectorSelectedContainerColor = colors.azurePrimary,
                        periodSelectorUnselectedContainerColor = colors.cardElevated,
                        periodSelectorSelectedContentColor = colors.onAzurePrimary,
                        periodSelectorUnselectedContentColor = colors.textSecondary,
                        timeSelectorSelectedContainerColor = colors.cardElevated,
                        timeSelectorUnselectedContainerColor = colors.cardElevated,
                        timeSelectorSelectedContentColor = colors.textPrimary,
                        timeSelectorUnselectedContentColor = colors.textSecondary
                    )
                )
            }
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        ShiftlyDialogShell(
            title = "Select Date",
            onDismiss = { showDatePicker = false },
            actionLabel = "Done",
            onAction = {
                datePickerState.selectedDateMillis?.let { millis ->
                    selectedDate = Instant.ofEpochMilli(millis)
                        .atZone(ZoneOffset.UTC)
                        .toLocalDate()
                }
                showDatePicker = false
            },
            horizontalOuterPadding = 12.dp,
            contentHorizontalPadding = 8.dp
        ) {
            DatePicker(
                state = datePickerState,
                title = null,
                headline = null,
                showModeToggle = false,
                colors = DatePickerDefaults.colors(
                    containerColor = colors.surface,
                    titleContentColor = colors.textPrimary,
                    headlineContentColor = colors.textPrimary,
                    weekdayContentColor = colors.textSecondary,
                    subheadContentColor = colors.textSecondary,
                    yearContentColor = colors.textPrimary,
                    currentYearContentColor = colors.azureAccent,
                    selectedYearContentColor = colors.onAzurePrimary,
                    selectedYearContainerColor = colors.azurePrimary,
                    dayContentColor = colors.textPrimary,
                    disabledDayContentColor = colors.textSecondary.copy(alpha = 0.45f),
                    selectedDayContentColor = colors.onAzurePrimary,
                    selectedDayContainerColor = colors.azurePrimary,
                    todayContentColor = colors.azureAccent,
                    todayDateBorderColor = colors.azureAccent
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPromptSheet(
    categoryOptions: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit,
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
        shape = ExpenseCategorySheetTopShape,
        dragHandle = { ShiftlySheetDragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            ShiftlySheetHeader(title = "Select Category")

            Surface(
                shape = RoundedCornerShape(18.dp),
                color = colors.cardElevated,
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    categoryOptions.forEachIndexed { idx, category ->
                        val isSelected = category.equals(selectedCategory, ignoreCase = true)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(min = 52.dp)
                                .background(if (isSelected) colors.coralContainer else Color.Transparent)
                                .shiftlyPressClickable(role = Role.RadioButton) {
                                    onSelectCategory(category)
                                    onDismiss()
                                }
                                .semantics(mergeDescendants = true) {
                                    this.selected = isSelected
                                }
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                ShiftlyIosIconTile(
                                    iconRes = categoryIcon(category),
                                    backgroundColor = if (isSelected) colors.coralBright else colors.textSecondary,
                                    size = 28.dp,
                                    cornerRadius = 8.dp,
                                    iconSize = 22.dp
                                )
                                Text(
                                    text = category,
                                    style = MaterialTheme.typography.titleSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.SemiBold
                                    ),
                                    color = colors.textPrimary
                                )
                            }
                            if (isSelected) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_check),
                                    contentDescription = "Selected",
                                    tint = colors.coralBright,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        if (idx < categoryOptions.lastIndex) {
                            ShiftlyHairlineDivider(startInset = 56.dp)
                        }
                    }
                }
            }

            ShiftlySheetCloseButton(
                label = "Close",
                onClick = onDismiss
            )
        }
    }
}

@Composable
fun ReceiptViewerDialog(
    expense: ExpenseEntity,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val path = expense.receiptImagePath ?: return
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val dateLabel = remember(expense.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(expense.dateEpochDay).format(dateFormatter)
    }
    val receiptRequest = remember(path, context) {
        ImageRequest.Builder(context)
            .data(File(path))
            .size(1440, 1440)
            .scale(Scale.FIT)
            .crossfade(true)
            .build()
    }

    ShiftlyDialogShell(
        title = expense.title,
        onDismiss = onDismiss,
        actionLabel = "Done",
        onAction = onDismiss,
        maxWidth = 420.dp,
        horizontalOuterPadding = 16.dp,
        contentHorizontalPadding = 16.dp
    ) {
        Text(
            text = "${ShiftlyFormatters.formatCurrency(currencySymbol, expense.amount)}  •  $dateLabel",
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
            color = colors.coralTextOnSurface
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 260.dp, max = 460.dp)
                .clip(ExpenseItemShape)
                .background(colors.cardElevated),
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = receiptRequest,
                contentDescription = "Receipt Proof",
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
