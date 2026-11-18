package com.example.shiftly.ui.screens.expense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.shiftly.R
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ReceiptStorageHelper
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.screens.lock.LocalSuppressNextRelock
import com.example.shiftly.ui.theme.OrbSkyLight
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

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
private fun categorySegmentColor(index: Int): Color {
    val colors = ShiftlyColors.current
    val palette = listOf(
        colors.coralBright,
        Color(0xFFFF9F7F),
        OrbSkyLight,
        colors.mintBright,
        Color(0xFF93C5FD)
    )
    return palette[index % palette.size]
}

@Composable
fun ExpenseSummaryCard(
    monthlyTotal: Double,
    allTimeTotal: Double,
    monthlyBudget: Double = 0.0,
    totalReceiptCount: Int,
    categoryBreakdown: List<CategorySpendSummary>,
    currencySymbol: String
) {
    val colors = ShiftlyColors.current
    ShiftlyGlassCard(contentPadding = PaddingValues(18.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = "THIS MONTH",
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontFamily = ShiftlySerif,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.8.sp
                        ),
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotal),
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = ShiftlySans,
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
                        shape = RoundedCornerShape(50),
                        color = colors.card,
                        border = BorderStroke(1.dp, colors.border),
                        shadowElevation = 0.dp
                    ) {
                        Text(
                            text = "All-time: ${ShiftlyFormatters.formatCurrency(currencySymbol, allTimeTotal)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (totalReceiptCount > 0) {
                        Text(
                            text = "$totalReceiptCount receipt(s) saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            if (monthlyBudget > 0.0) {
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
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif).tabular(),
                            color = colors.textSecondary
                        )
                        Text(
                            text = if (isOver) {
                                "Over by ${ShiftlyFormatters.formatCurrency(currencySymbol, monthlyTotal - monthlyBudget)}"
                            } else {
                                "${ShiftlyFormatters.formatCurrency(currencySymbol, remaining)} left"
                            },
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold).tabular(),
                            color = if (isNear) colors.coralTextOnSurface else colors.mintTextOnSurface
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                            .background(colors.border)
                    ) {
                        if (budgetUsed > 0f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(budgetUsed)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(if (isNear) colors.coralBright else colors.mintBright)
                            )
                        }
                    }
                }
            }

            if (categoryBreakdown.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(colors.border),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    categoryBreakdown.forEachIndexed { index, item ->
                        if (item.fraction > 0f) {
                            Box(
                                modifier = Modifier
                                    .weight(item.fraction.coerceAtLeast(0.05f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(50))
                                    .background(categorySegmentColor(index))
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
                    categoryBreakdown.take(4).forEachIndexed { index, item ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(categorySegmentColor(index))
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${item.category} (${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)})",
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                val budgetedCategories = categoryBreakdown.filter { (it.monthlyLimit ?: 0.0) > 0.0 }
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
                                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = if (isOver) {
                                            "${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)} / ${ShiftlyFormatters.formatCurrency(currencySymbol, limit)} (Over)"
                                        } else {
                                            "${ShiftlyFormatters.formatCurrency(currencySymbol, item.totalAmount)} / ${ShiftlyFormatters.formatCurrency(currencySymbol, limit)}"
                                        },
                                        style = MaterialTheme.typography.labelSmall.tabular(),
                                        color = if (isNearOrOver) colors.coralTextOnSurface else colors.mintTextOnSurface
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(50))
                                        .background(colors.border)
                                ) {
                                    if (usedFraction > 0f) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(usedFraction)
                                                .fillMaxHeight()
                                                .clip(RoundedCornerShape(50))
                                                .background(if (isNearOrOver) colors.coralBright else colors.mintBright)
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
}

@Composable
fun ExpenseCategoryFilterRow(
    categories: List<String>,
    selectedCategory: String,
    onSelectCategory: (String) -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.horizontalGradient(
                        0.0f to Color.Black,
                        0.88f to Color.Black,
                        1.0f to Color.Transparent
                    ),
                    blendMode = BlendMode.DstIn
                )
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
                    .clip(RoundedCornerShape(50))
                    .background(if (isSelected) colors.azurePrimary else colors.surface)
                    .border(
                        width = 1.dp,
                        color = if (isSelected) colors.azureAccent else colors.border,
                        shape = RoundedCornerShape(50)
                    )
                    .clickable(role = Role.Tab) { onSelectCategory(category) }
                    .semantics(mergeDescendants = true) {
                        selected = isSelected
                        contentDescription = "$category expenses filter"
                    }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ShiftlySerif,
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
    onShowAll: () -> Unit
) {
    val colors = ShiftlyColors.current
    val isFiltered = selectedCategory != "All"
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(CircleShape)
                    .background(colors.coralContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_receipt_text),
                    contentDescription = null,
                    tint = colors.coralInk,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = if (isFiltered) "No $selectedCategory expenses" else "No expenses logged yet",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                color = colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            ShiftlyPrimaryPillButton(
                text = if (isFiltered) "Show All Expenses" else "Log your first expense",
                onClick = if (isFiltered) onShowAll else onAddExpense
            )
        }
    }
}

@Composable
fun ExpenseItemCard(
    expense: ExpenseEntity,
    currencySymbol: String,
    onViewReceipt: (ExpenseEntity) -> Unit,
    onDelete: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val dateLabel = remember(expense.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(expense.dateEpochDay).format(dateFormatter)
    }
    val hasReceipt = remember(expense.receiptImagePath) {
        !expense.receiptImagePath.isNullOrBlank() && File(expense.receiptImagePath).exists()
    }
    val isSplitItem = expense.category.equals("Split Bill", ignoreCase = true)
    val isSavingsItem = expense.category.equals("Savings", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.card,
        border = BorderStroke(1.dp, colors.border),
        shadowElevation = 0.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isSavingsItem -> colors.mintContainer
                            isSplitItem -> colors.azureContainer
                            else -> colors.coralContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(categoryIcon(expense.category)),
                    contentDescription = expense.category,
                    tint = when {
                        isSavingsItem -> colors.mintInk
                        isSplitItem -> colors.azureInk
                        else -> colors.coralInk
                    },
                    modifier = Modifier.size(21.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.category}  •  $dateLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )

                if (hasReceipt) {
                    Box(
                        modifier = Modifier
                            .defaultMinSize(minWidth = 48.dp, minHeight = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClickLabel = "View receipt for ${expense.title}") {
                                onViewReceipt(expense)
                            }
                            .semantics(mergeDescendants = true) {
                                contentDescription = "View receipt for ${expense.title}"
                            },
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Surface(
                            shape = RoundedCornerShape(50),
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
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
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
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = colors.coralInk
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_trash_2),
                        contentDescription = "Delete Expense ${expense.title}",
                        tint = colors.coralInk.copy(alpha = 0.85f),
                        modifier = Modifier.size(20.dp)
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
    onDismiss: () -> Unit,
    onSaveExpense: (title: String, amount: Double, category: String, date: LocalDate, receiptPath: String?) -> Unit
) {
    val colors = ShiftlyColors.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var savedReceiptPath by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            ReceiptStorageHelper.deleteReceiptFile(savedReceiptPath)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = colors.surface,
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        AddExpenseSheetContent(
            currencySymbol = currencySymbol,
            savedReceiptPath = savedReceiptPath,
            onReceiptChanged = { savedReceiptPath = it },
            onDismiss = {
                ReceiptStorageHelper.deleteReceiptFile(savedReceiptPath)
                onDismiss()
            },
            onSaveExpense = { title, amount, category, date, path ->
                onSaveExpense(title, amount, category, date, path)
                onDismiss()
            },
            availableCategories = availableCategories
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
    initialDate: LocalDate = LocalDate.now()
) {
    val colors = ShiftlyColors.current
    val context = LocalContext.current
    val suppressNextRelock = LocalSuppressNextRelock.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }
    val categoryOptions = remember(availableCategories) {
        availableCategories.ifEmpty { ExpenseViewModel.EXPENSE_CATEGORIES }
    }

    var title by remember { mutableStateOf(initialTitle) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var selectedCategory by remember(categoryOptions) {
        mutableStateOf(
            categoryOptions.firstOrNull { it.equals(initialCategory, ignoreCase = true) }
                ?: categoryOptions.firstOrNull()
                ?: "Other"
        )
    }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var selectedTime by remember { mutableStateOf(LocalTime.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showCategoryPrompt by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

    val amountFocusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        amountFocusRequester.requestFocus()
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            ReceiptStorageHelper.deleteReceiptFile(savedReceiptPath)
            onReceiptChanged(ReceiptStorageHelper.saveUriToLocalReceipts(context, uri))
        }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        val file = pendingCameraFile
        if (success && file != null && file.exists() && file.length() > 0L) {
            ReceiptStorageHelper.deleteReceiptFile(savedReceiptPath)
            onReceiptChanged(file.absolutePath)
        } else {
            file?.delete()
        }
        pendingCameraFile = null
    }

    val parsedAmount = amountText.toDoubleOrNull() ?: 0.0
    val isValid = title.isNotBlank() && parsedAmount.isFinite() && parsedAmount > 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
            .padding(bottom = 26.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Add Expense",
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", fontFamily = ShiftlySerif, color = colors.textSecondary)
            }
        }

        OutlinedTextField(
            value = amountText,
            onValueChange = { input ->
                val filtered = input.filter { it.isDigit() || it == '.' }
                if (filtered.count { it == '.' } <= 1) {
                    amountText = filtered
                }
            },
            label = { Text("Amount", fontFamily = ShiftlySerif) },
            placeholder = {
                Text(
                    text = "0",
                    style = MaterialTheme.typography.headlineLarge.tabular(),
                    color = colors.textMuted
                )
            },
            prefix = {
                Text(
                    text = "$currencySymbol ",
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ),
                    color = colors.coralInk
                )
            },
            textStyle = MaterialTheme.typography.headlineLarge.copy(
                fontFamily = ShiftlySans,
                fontWeight = FontWeight.Bold
            ).tabular(),
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(18.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.coralInk,
                unfocusedBorderColor = colors.border,
                focusedLabelColor = colors.coralInk,
                unfocusedLabelColor = colors.textSecondary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(amountFocusRequester)
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title", fontFamily = ShiftlySerif) },
            placeholder = { Text("e.g., Tapsilog & Iced Coffee") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.azureAccent,
                unfocusedBorderColor = colors.border,
                focusedLabelColor = colors.azureAccent,
                unfocusedLabelColor = colors.textSecondary,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Category selection prompt card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.card,
            border = BorderStroke(1.dp, colors.border),
            shadowElevation = 0.dp,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showCategoryPrompt = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(colors.coralContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(categoryIcon(selectedCategory)),
                            contentDescription = null,
                            tint = colors.coralInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "Category",
                            style = MaterialTheme.typography.labelSmall.copy(fontFamily = ShiftlySerif),
                            color = colors.textSecondary
                        )
                        Text(
                            text = selectedCategory,
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = colors.textPrimary
                        )
                    }
                }
                Icon(
                    painter = painterResource(R.drawable.ic_lucide_chevron_right),
                    contentDescription = "Select Category",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Date and Time selection row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showDatePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_calendar),
                        contentDescription = null,
                        tint = colors.azureInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.card,
                border = BorderStroke(1.dp, colors.border),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { showTimePicker = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_clock),
                        contentDescription = null,
                        tint = colors.azureInk,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = selectedTime.format(DateTimeFormatter.ofPattern("hh:mm a", locale)),
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Receipt (optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                color = colors.textSecondary
            )

            if (savedReceiptPath != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.card,
                    border = BorderStroke(1.dp, colors.azureAccent),
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
                            model = File(savedReceiptPath),
                            contentDescription = "Receipt Preview",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(10.dp))
                        )
                        Text(
                            text = "Receipt attached",
                            style = MaterialTheme.typography.titleSmall,
                            color = colors.textPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = {
                                ReceiptStorageHelper.deleteReceiptFile(savedReceiptPath)
                                onReceiptChanged(null)
                            },
                            modifier = Modifier.size(48.dp)
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
                    OutlinedButton(
                        onClick = {
                            suppressNextRelock()
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_image),
                            contentDescription = null,
                            tint = colors.azureInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontFamily = ShiftlySerif, color = colors.textPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            suppressNextRelock()
                            val created = runCatching {
                                ReceiptStorageHelper.createCameraReceiptTarget(context)
                            }.getOrNull() ?: return@OutlinedButton
                            pendingCameraFile = created.first
                            val launched = runCatching {
                                cameraLauncher.launch(created.second)
                            }.isSuccess
                            if (!launched) {
                                created.first.delete()
                                pendingCameraFile = null
                            }
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, colors.border),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_camera),
                            contentDescription = null,
                            tint = colors.azureInk,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", fontFamily = ShiftlySerif, color = colors.textPrimary)
                    }
                }
            }
        }

        Button(
            onClick = {
                onSaveExpense(title, parsedAmount, selectedCategory, selectedDate, savedReceiptPath)
            },
            enabled = isValid,
            shape = RoundedCornerShape(50),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = 0.dp,
                pressedElevation = 0.dp
            ),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.azurePrimary,
                contentColor = colors.onAzurePrimary,
                disabledContainerColor = colors.disabledContainer,
                disabledContentColor = colors.disabledContent
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                text = "Add Expense",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                )
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
        DatePickerDialog(
            onDismissRequest = { showTimePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        selectedTime = LocalTime.of(timePickerState.hour, timePickerState.minute)
                        showTimePicker = false
                    }
                ) {
                    Text("OK", color = colors.azureAccent, fontFamily = ShiftlySerif, fontWeight = FontWeight.SemiBold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
                }
            }
        ) {
            Box(
                modifier = Modifier
                    .padding(20.dp)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                TimePicker(
                    state = timePickerState,
                    colors = TimePickerDefaults.colors(
                        clockDialColor = colors.card,
                        clockDialSelectedContentColor = colors.onAzurePrimary,
                        clockDialUnselectedContentColor = colors.textSecondary,
                        selectorColor = colors.azurePrimary,
                        containerColor = colors.surface,
                        periodSelectorBorderColor = colors.border,
                        periodSelectorSelectedContainerColor = colors.azurePrimary,
                        periodSelectorUnselectedContainerColor = colors.surface,
                        periodSelectorSelectedContentColor = colors.onAzurePrimary,
                        periodSelectorUnselectedContentColor = colors.textSecondary,
                        timeSelectorSelectedContainerColor = colors.cardElevated,
                        timeSelectorUnselectedContainerColor = colors.surface,
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

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            tonalElevation = 0.dp,
            colors = DatePickerDefaults.colors(containerColor = colors.surface),
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            selectedDate = Instant.ofEpochMilli(millis)
                                .atZone(ZoneOffset.UTC)
                                .toLocalDate()
                        }
                        showDatePicker = false
                    }
                ) {
                    Text(
                        text = "Select",
                        color = colors.azureAccent,
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = colors.textSecondary, fontFamily = ShiftlySerif)
                }
            }
        ) {
            DatePicker(
                state = datePickerState,
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
                    disabledDayContentColor = colors.textMuted,
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
        tonalElevation = 0.dp,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Category",
                    style = MaterialTheme.typography.headlineSmall.copy(fontFamily = ShiftlySerif),
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(44.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.ic_lucide_x),
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            categoryOptions.forEach { category ->
                val isSelected = category.equals(selectedCategory, ignoreCase = true)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isSelected) colors.coralContainer else colors.card,
                    border = BorderStroke(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colors.coralInk else colors.border
                    ),
                    shadowElevation = 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.RadioButton) {
                            onSelectCategory(category)
                            onDismiss()
                        }
                        .semantics(mergeDescendants = true) {
                            this.selected = isSelected
                        }
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
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) colors.coralInk.copy(alpha = 0.2f) else colors.surface),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(categoryIcon(category)),
                                    contentDescription = null,
                                    tint = if (isSelected) colors.coralInk else colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Text(
                                text = category,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    fontFamily = ShiftlySans,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                color = colors.textPrimary
                            )
                        }
                        if (isSelected) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(colors.coralInk),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_lucide_check),
                                    contentDescription = "Selected",
                                    tint = colors.onAccentDark,
                                    modifier = Modifier.size(14.dp)
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
fun ReceiptViewerDialog(
    expense: ExpenseEntity,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val path = expense.receiptImagePath ?: return
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val dateLabel = remember(expense.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(expense.dateEpochDay).format(dateFormatter)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            color = colors.background.copy(alpha = 0.96f),
            shadowElevation = 0.dp,
            modifier = Modifier.fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = expense.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${ShiftlyFormatters.formatCurrency(currencySymbol, expense.amount)}  •  $dateLabel",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = colors.coralTextOnSurface
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(colors.card)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_lucide_x),
                            contentDescription = "Close",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = File(path),
                        contentDescription = "Receipt Proof",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }
    }
}
