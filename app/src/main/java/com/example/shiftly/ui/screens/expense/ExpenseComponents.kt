package com.example.shiftly.ui.screens.expense

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.CallSplit
import androidx.compose.material.icons.automirrored.outlined.ReceiptLong
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Fastfood
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
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
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ReceiptStorageHelper
import com.example.shiftly.ui.components.ShiftlyGlassCard
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.theme.AzureInkOnWhite
import com.example.shiftly.ui.theme.AzureSoftContainer
import com.example.shiftly.ui.theme.CoralBright
import com.example.shiftly.ui.theme.CoralInkOnWhite
import com.example.shiftly.ui.theme.CoralSoftContainer
import com.example.shiftly.ui.theme.CoralTextOnBlue
import com.example.shiftly.ui.theme.DeepAzureBottom
import com.example.shiftly.ui.theme.HairlineLight
import com.example.shiftly.ui.theme.InkBlack
import com.example.shiftly.ui.theme.InkMuted
import com.example.shiftly.ui.theme.InkPrimary
import com.example.shiftly.ui.theme.InkSecondary
import com.example.shiftly.ui.theme.MintBright
import com.example.shiftly.ui.theme.OrbSkyLight
import com.example.shiftly.ui.theme.PureWhite
import com.example.shiftly.ui.theme.ShiftlySans
import com.example.shiftly.ui.theme.ShiftlySerif
import com.example.shiftly.ui.theme.SoftCloudWhite
import com.example.shiftly.ui.theme.tabular
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

fun categoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "food" -> Icons.Outlined.Fastfood
        "transport" -> Icons.Outlined.DirectionsCar
        "bills" -> Icons.Outlined.Receipt
        "supplies" -> Icons.Outlined.Inventory2
        "split bill" -> Icons.AutoMirrored.Outlined.CallSplit
        else -> Icons.Outlined.ShoppingBag
    }
}

private fun categorySegmentColor(index: Int): Color {
    val palette = listOf(
        CoralBright,
        Color(0xFFFF9F7F),
        OrbSkyLight,
        MintBright,
        Color(0xFFD8E8FF)
    )
    return palette[index % palette.size]
}

@Composable
fun ExpenseSummaryCard(
    monthlyTotal: Double,
    allTimeTotal: Double,
    totalReceiptCount: Int,
    categoryBreakdown: List<CategorySpendSummary>,
    currencySymbol: String
) {
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
                        color = PureWhite.copy(alpha = 0.88f)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$currencySymbol${"%,.2f".format(monthlyTotal)}",
                        style = MaterialTheme.typography.displaySmall.copy(
                            fontFamily = ShiftlySans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp
                        ).tabular(),
                        color = CoralTextOnBlue
                    )
                }

                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PureWhite.copy(alpha = 0.16f),
                        border = BorderStroke(1.dp, PureWhite.copy(alpha = 0.24f))
                    ) {
                        Text(
                            text = "All-time: $currencySymbol${"%,.2f".format(allTimeTotal)}",
                            style = MaterialTheme.typography.labelSmall.tabular(),
                            color = PureWhite,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (totalReceiptCount > 0) {
                        Text(
                            text = "$totalReceiptCount receipt(s) saved",
                            style = MaterialTheme.typography.labelSmall,
                            color = PureWhite.copy(alpha = 0.80f)
                        )
                    }
                }
            }

            if (categoryBreakdown.isNotEmpty()) {
                // Segmented Category Progress Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(50))
                        .background(PureWhite.copy(alpha = 0.18f)),
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

                // Category Legend Pills
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
                                text = "${item.category} ($currencySymbol${"%,.0f".format(item.totalAmount)})",
                                style = MaterialTheme.typography.labelSmall.tabular(),
                                color = PureWhite.copy(alpha = 0.92f)
                            )
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
            .padding(start = 20.dp, end = 36.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        categories.forEach { category ->
            val selected = selectedCategory == category
            Box(
                modifier = Modifier
                    .heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .background(if (selected) PureWhite else PureWhite.copy(alpha = 0.14f))
                    .border(
                        width = 1.dp,
                        color = if (selected) PureWhite else PureWhite.copy(alpha = 0.28f),
                        shape = RoundedCornerShape(50)
                    )
                    .clickable { onSelectCategory(category) }
                    .padding(horizontal = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontFamily = ShiftlySerif,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = if (selected) DeepAzureBottom else PureWhite
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
    val isFiltered = selectedCategory != "All"
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = SoftCloudWhite,
        border = BorderStroke(1.dp, HairlineLight),
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
                    .background(CoralSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ReceiptLong,
                    contentDescription = null,
                    tint = CoralInkOnWhite,
                    modifier = Modifier.size(26.dp)
                )
            }
            Text(
                text = if (isFiltered) "No $selectedCategory expenses" else "No expenses logged yet",
                style = MaterialTheme.typography.titleMedium.copy(fontFamily = ShiftlySerif),
                color = InkPrimary,
                textAlign = TextAlign.Center
            )
            Text(
                text = if (isFiltered) {
                    "Switch back to All or log a new $selectedCategory item."
                } else {
                    "Log meals, jeepney fares, and school supplies between classes."
                },
                style = MaterialTheme.typography.bodySmall,
                color = InkSecondary,
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
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d", locale) }
    val dateLabel = remember(expense.dateEpochDay, dateFormatter) {
        LocalDate.ofEpochDay(expense.dateEpochDay).format(dateFormatter)
    }
    val hasReceipt = remember(expense.receiptImagePath) {
        !expense.receiptImagePath.isNullOrBlank() && File(expense.receiptImagePath).exists()
    }
    val isSplitItem = expense.category.equals("Split Bill", ignoreCase = true)

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = PureWhite,
        border = BorderStroke(1.dp, HairlineLight),
        shadowElevation = 2.dp,
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
                    .background(if (isSplitItem) AzureSoftContainer else CoralSoftContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(expense.category),
                    contentDescription = expense.category,
                    tint = if (isSplitItem) AzureInkOnWhite else CoralInkOnWhite,
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
                    color = InkPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${expense.category}  •  $dateLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = InkSecondary
                )

                if (hasReceipt) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = AzureSoftContainer,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable { onViewReceipt(expense) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                tint = AzureInkOnWhite,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Receipt",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = AzureInkOnWhite
                            )
                        }
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "-$currencySymbol${"%,.2f".format(expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = CoralInkOnWhite
                )
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(48.dp)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Delete Expense",
                        tint = CoralInkOnWhite.copy(alpha = 0.85f),
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
    onDismiss: () -> Unit,
    onSaveExpense: (title: String, amount: Double, category: String, date: LocalDate, receiptPath: String?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var savedReceiptPath by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = {
            ReceiptStorageHelper.deleteReceiptFile(savedReceiptPath)
            onDismiss()
        },
        sheetState = sheetState,
        containerColor = PureWhite,
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
            }
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
    initialAmount: String = "",
    initialTitle: String = "",
    initialCategory: String = "Food",
    initialDate: LocalDate = LocalDate.now()
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("EEE, MMM d", locale) }

    var title by remember { mutableStateOf(initialTitle) }
    var amountText by remember { mutableStateOf(initialAmount) }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedDate by remember { mutableStateOf(initialDate) }
    var showDatePicker by remember { mutableStateOf(false) }
    var pendingCameraFile by remember { mutableStateOf<File?>(null) }

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
    val isValid = title.isNotBlank() && parsedAmount > 0.0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
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
                color = InkPrimary
            )
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.heightIn(min = 48.dp)
            ) {
                Text("Cancel", fontFamily = ShiftlySerif, color = InkSecondary)
            }
        }

        // Large Amount Input
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = { Text("Amount", fontFamily = ShiftlySerif) },
            placeholder = {
                Text(
                    text = "0.00",
                    style = MaterialTheme.typography.headlineLarge.tabular(),
                    color = InkMuted
                )
            },
            prefix = {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.headlineLarge.copy(
                        fontFamily = ShiftlySans,
                        fontWeight = FontWeight.Bold
                    ),
                    color = CoralInkOnWhite
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
                focusedBorderColor = CoralInkOnWhite,
                unfocusedBorderColor = HairlineLight,
                focusedLabelColor = CoralInkOnWhite,
                unfocusedLabelColor = InkSecondary,
                focusedTextColor = InkPrimary,
                unfocusedTextColor = InkPrimary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("Title", fontFamily = ShiftlySerif) },
            placeholder = { Text("e.g., Tapsilog & Iced Coffee") },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AzureInkOnWhite,
                unfocusedBorderColor = HairlineLight,
                focusedLabelColor = AzureInkOnWhite,
                unfocusedLabelColor = InkSecondary
            ),
            modifier = Modifier.fillMaxWidth()
        )

        // Category Pills
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "Category",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                color = InkSecondary
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ExpenseViewModel.EXPENSE_CATEGORIES.forEach { category ->
                    val selected = selectedCategory == category
                    Box(
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(50))
                            .background(if (selected) CoralSoftContainer else SoftCloudWhite)
                            .border(
                                width = 1.dp,
                                color = if (selected) CoralInkOnWhite else HairlineLight,
                                shape = RoundedCornerShape(50)
                            )
                            .clickable { selectedCategory = category }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = category,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontFamily = ShiftlySerif,
                                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium
                            ),
                            color = if (selected) CoralInkOnWhite else InkSecondary
                        )
                    }
                }
            }
        }

        // Date Selector Row
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = SoftCloudWhite,
            border = BorderStroke(1.dp, HairlineLight),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .clip(RoundedCornerShape(16.dp))
                .clickable { showDatePicker = true }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.CalendarToday,
                        contentDescription = null,
                        tint = AzureInkOnWhite,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = selectedDate.format(dateFormatter),
                        style = MaterialTheme.typography.titleSmall,
                        color = InkPrimary
                    )
                }
                Text(
                    text = "Change",
                    style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                    color = AzureInkOnWhite
                )
            }
        }

        // Receipt Section (Gallery or Camera)
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "Receipt (optional)",
                style = MaterialTheme.typography.labelMedium.copy(fontFamily = ShiftlySerif),
                color = InkSecondary
            )

            if (savedReceiptPath != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = SoftCloudWhite,
                    border = BorderStroke(1.dp, AzureInkOnWhite),
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
                            color = InkPrimary,
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
                                imageVector = Icons.Outlined.Close,
                                contentDescription = "Remove Receipt",
                                tint = CoralInkOnWhite
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
                            galleryLauncher.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                            )
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HairlineLight),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.PhotoLibrary,
                            contentDescription = null,
                            tint = AzureInkOnWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Gallery", fontFamily = ShiftlySerif, color = InkPrimary)
                    }

                    OutlinedButton(
                        onClick = {
                            val (file, uri) = ReceiptStorageHelper.createCameraReceiptTarget(context)
                            pendingCameraFile = file
                            cameraLauncher.launch(uri)
                        },
                        shape = RoundedCornerShape(50),
                        border = BorderStroke(1.dp, HairlineLight),
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CameraAlt,
                            contentDescription = null,
                            tint = AzureInkOnWhite,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Camera", fontFamily = ShiftlySerif, color = InkPrimary)
                    }
                }
            }
        }

        // Full-width Black Pill Save Button with clear enabled / disabled states
        Button(
            onClick = {
                onSaveExpense(title, parsedAmount, selectedCategory, selectedDate, savedReceiptPath)
            },
            enabled = isValid,
            shape = RoundedCornerShape(50),
            colors = ButtonDefaults.buttonColors(
                containerColor = InkBlack,
                contentColor = PureWhite,
                disabledContainerColor = HairlineLight,
                disabledContentColor = InkMuted
            ),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
        ) {
            Text(
                text = if (isValid) "Save Expense" else "Enter amount & title to save",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontFamily = ShiftlySerif,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }

    if (showDatePicker) {
        val initialMillis = selectedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
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
                        color = AzureInkOnWhite,
                        fontFamily = ShiftlySerif,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = InkSecondary, fontFamily = ShiftlySerif)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

@Composable
fun ReceiptViewerDialog(
    expense: ExpenseEntity,
    currencySymbol: String,
    onDismiss: () -> Unit
) {
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
            color = InkBlack.copy(alpha = 0.94f),
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
                            color = PureWhite
                        )
                        Text(
                            text = "$currencySymbol${"%,.2f".format(expense.amount)}  •  $dateLabel",
                            style = MaterialTheme.typography.bodySmall.tabular(),
                            color = CoralTextOnBlue
                        )
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(PureWhite.copy(alpha = 0.14f))
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = "Close",
                            tint = PureWhite
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(PureWhite.copy(alpha = 0.08f)),
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
