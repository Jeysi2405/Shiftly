package com.example.shiftly.ui.screens.loans

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.shiftly.R
import com.example.shiftly.data.local.InstallmentLoanEntity
import com.example.shiftly.data.local.InstallmentLoanWithPayments
import com.example.shiftly.data.local.LoanDueStatus
import com.example.shiftly.data.local.LoanPaymentEntity
import com.example.shiftly.data.local.contractEndDate
import com.example.shiftly.data.local.daysUntilNextDue
import com.example.shiftly.data.local.dueStatus
import com.example.shiftly.data.local.finalPrice
import com.example.shiftly.data.local.firstDueDate
import com.example.shiftly.data.local.interestPercent
import com.example.shiftly.data.local.isCompleted
import com.example.shiftly.data.local.needsDueAttention
import com.example.shiftly.data.local.nextDueDate
import com.example.shiftly.data.local.paidMonthsClamped
import com.example.shiftly.data.local.progressFraction
import com.example.shiftly.data.local.remainingBalance
import com.example.shiftly.data.local.remainingMonths
import com.example.shiftly.data.local.startDate
import com.example.shiftly.data.local.toUtcMillis
import com.example.shiftly.data.local.totalInterest
import com.example.shiftly.data.local.totalPaid
import com.example.shiftly.ui.components.ShiftlyDeleteConfirmDialog
import com.example.shiftly.ui.components.ShiftlyDialogShell
import com.example.shiftly.ui.components.ShiftlyEmptyState
import com.example.shiftly.ui.components.ShiftlyPrimaryPillButton
import com.example.shiftly.ui.components.ShiftlyProgressBar
import com.example.shiftly.ui.components.ShiftlySheetBottomButtons
import com.example.shiftly.ui.components.ShiftlySheetCloseButton
import com.example.shiftly.ui.components.ShiftlySheetDragHandle
import com.example.shiftly.ui.components.ShiftlySheetHeader
import com.example.shiftly.ui.components.shiftlyPressClickable
import com.example.shiftly.ui.components.shiftlySheetScrimColor
import com.example.shiftly.ui.theme.InterFontFamily
import com.example.shiftly.ui.theme.ShiftlyColors
import com.example.shiftly.ui.theme.tabular
import com.example.shiftly.utils.ShiftlyFormatters
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val SheetTopRoundedShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentLoansBottomSheet(
    currencySymbol: String,
    loans: List<InstallmentLoanWithPayments>,
    onAddOrUpdateLoan: (InstallmentLoanEntity) -> Unit,
    onMarkMonthPaid: (InstallmentLoanEntity) -> Unit,
    onUpdateLoanPaymentDateTime: (LoanPaymentEntity, Long) -> Unit = { _, _ -> },
    onDeleteLoanPayment: (InstallmentLoanEntity, LoanPaymentEntity) -> Unit = { _, _ -> },
    onDeleteLoan: (InstallmentLoanEntity) -> Unit,
    onHelpClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val today = remember { LocalDate.now() }

    var showAddEditSheet by remember { mutableStateOf(false) }
    var editingLoan by remember { mutableStateOf<InstallmentLoanEntity?>(null) }
    var selectedLoanId by remember { mutableStateOf<Long?>(null) }

    val sortedLoans = remember(loans, today) {
        loans.sortedWith(
            compareBy<InstallmentLoanWithPayments> { it.loan.isCompleted }
                .thenBy { it.loan.nextDueDate() }
                .thenByDescending { it.loan.id }
        )
    }
    val attentionLoans = remember(sortedLoans, today) {
        sortedLoans.filter { it.loan.needsDueAttention(today) }
    }
    val selectedLoanEntry = remember(loans, selectedLoanId) {
        selectedLoanId?.let { id -> loans.firstOrNull { it.loan.id == id } }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = SheetTopRoundedShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            ShiftlySheetHeader(
                title = "Installment Loans",
                onHelpClick = onHelpClick
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (attentionLoans.isNotEmpty()) {
                    LoanDueReminderBanner(
                        attentionLoans = attentionLoans,
                        currencySymbol = currencySymbol,
                        today = today,
                        dateFormatter = dateFormatter,
                        onSelectLoan = { selectedLoanId = it.loan.id }
                    )
                }

                if (sortedLoans.isEmpty()) {
                    ShiftlyEmptyState(
                        iconRes = R.drawable.ic_lucide_file_text,
                        title = "No installment loans yet",
                        subtitle = "Track financed laptops, phones, or loans—see balances, due reminders, and payment history.",
                        actionText = "+ Add Loan",
                        onAction = {
                            editingLoan = null
                            showAddEditSheet = true
                        },
                        iconBackgroundColor = colors.azurePrimary,
                        containerColor = colors.cardElevated
                    )
                } else {
                    val activeTotal = remember(sortedLoans) {
                        sortedLoans.filter { !it.loan.isCompleted }.sumOf { it.loan.remainingBalance }
                    }
                    val activeCount = remember(sortedLoans) {
                        sortedLoans.count { !it.loan.isCompleted }
                    }
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.cardElevated,
                        border = BorderStroke(0.5.dp, colors.separator),
                        shadowElevation = 0.dp,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = "REMAINING BALANCE",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontFamily = InterFontFamily,
                                        letterSpacing = 0.5.sp
                                    ),
                                    color = colors.textSecondary
                                )
                                Text(
                                    text = ShiftlyFormatters.formatCurrency(currencySymbol, activeTotal),
                                    style = MaterialTheme.typography.headlineSmall.copy(
                                        fontFamily = InterFontFamily,
                                        fontWeight = FontWeight.Bold
                                    ).tabular(),
                                    color = if (activeCount == 0) colors.mintInk else colors.textPrimary
                                )
                            }
                            Text(
                                text = if (activeCount == 0) "All paid" else "$activeCount active",
                                style = MaterialTheme.typography.labelMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold
                                ).tabular(),
                                color = if (activeCount == 0) colors.mintInk else colors.textSecondary
                            )
                        }
                    }

                    sortedLoans.forEach { entry ->
                        InstallmentLoanSummaryCard(
                            loan = entry.loan,
                            currencySymbol = currencySymbol,
                            today = today,
                            dateFormatter = dateFormatter,
                            onClick = { selectedLoanId = entry.loan.id }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    editingLoan = null
                    showAddEditSheet = true
                },
                cancelLabel = "Close",
                actionLabel = "+ Add Loan"
            )
        }
    }

    selectedLoanEntry?.let { entry ->
        InstallmentLoanDetailsSheet(
            entry = entry,
            currencySymbol = currencySymbol,
            today = today,
            onMarkMonthPaid = { onMarkMonthPaid(entry.loan) },
            onUpdateLoanPaymentDateTime = onUpdateLoanPaymentDateTime,
            onDeleteLoanPayment = { payment -> onDeleteLoanPayment(entry.loan, payment) },
            onEdit = {
                editingLoan = entry.loan
                showAddEditSheet = true
            },
            onDelete = {
                selectedLoanId = null
                onDeleteLoan(entry.loan)
            },
            onDismiss = { selectedLoanId = null }
        )
    }

    if (showAddEditSheet) {
        AddEditInstallmentLoanSheet(
            currencySymbol = currencySymbol,
            editingLoan = editingLoan,
            onDismiss = {
                showAddEditSheet = false
                editingLoan = null
            },
            onSave = { savedLoan ->
                onAddOrUpdateLoan(savedLoan)
                showAddEditSheet = false
                editingLoan = null
            }
        )
    }
}

@Composable
private fun LoanDueReminderBanner(
    attentionLoans: List<InstallmentLoanWithPayments>,
    currencySymbol: String,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    onSelectLoan: (InstallmentLoanWithPayments) -> Unit
) {
    val colors = ShiftlyColors.current
    val overdue = attentionLoans.filter { it.loan.dueStatus(today) == LoanDueStatus.OVERDUE }
    val hasOverdue = overdue.isNotEmpty()
    val lead = overdue.firstOrNull() ?: attentionLoans.first()
    val accent = if (hasOverdue) colors.coralInk else colors.iosOrange
    val container = if (hasOverdue) colors.coralContainer else colors.orangeContainer

    val headline = when {
        overdue.size == 1 -> "${lead.loan.gadgetName} is overdue"
        overdue.size > 1 -> "${overdue.size} loans are overdue"
        attentionLoans.size == 1 && lead.loan.dueStatus(today) == LoanDueStatus.DUE_TODAY ->
            "${lead.loan.gadgetName} is due today"
        attentionLoans.size == 1 ->
            "${lead.loan.gadgetName} is due in ${lead.loan.daysUntilNextDue(today)} day(s)"
        else -> "${attentionLoans.size} loans due soon"
    }
    val subline = "${ShiftlyFormatters.formatCurrency(currencySymbol, lead.loan.installmentPerMonth)} • Due ${lead.loan.nextDueDate().format(dateFormatter)}"

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = container,
        border = BorderStroke(0.5.dp, accent.copy(alpha = 0.45f)),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shiftlyPressClickable { onSelectLoan(lead) }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_lucide_bell),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(20.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = headline,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    ),
                    color = accent,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = subline,
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                painter = painterResource(R.drawable.ic_lucide_chevron_right),
                contentDescription = "Open loan",
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun InstallmentLoanSummaryCard(
    loan: InstallmentLoanEntity,
    currencySymbol: String,
    today: LocalDate,
    dateFormatter: DateTimeFormatter,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    val status = loan.dueStatus(today)
    val monthsLeft = loan.remainingMonths
    val progressColor = when (status) {
        LoanDueStatus.COMPLETED -> colors.mintInk
        LoanDueStatus.OVERDUE -> colors.coralBright
        LoanDueStatus.DUE_TODAY, LoanDueStatus.DUE_SOON -> colors.iosOrange
        LoanDueStatus.UPCOMING -> colors.azureAccent
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.cardElevated,
        border = BorderStroke(0.5.dp, colors.separator),
        shadowElevation = 0.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .shiftlyPressClickable(onClick = onClick)
    ) {
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
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    Text(
                        text = loan.gadgetName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontFamily = InterFontFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp
                        ),
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${ShiftlyFormatters.formatCurrency(currencySymbol, loan.installmentPerMonth)} / mo",
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                        color = colors.textSecondary
                    )
                }

                LoanStatusBadge(
                    loan = loan,
                    today = today,
                    dateFormatter = dateFormatter
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (loan.isCompleted) {
                        "Balance settled"
                    } else {
                        "${ShiftlyFormatters.formatCurrency(currencySymbol, loan.remainingBalance)} left"
                    },
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Bold
                    ).tabular(),
                    color = if (loan.isCompleted) colors.mintInk else colors.textPrimary
                )

                Text(
                    text = if (loan.isCompleted) {
                        "Completed"
                    } else {
                        "$monthsLeft mo${if (monthsLeft == 1) "" else "s"} left"
                    },
                    style = MaterialTheme.typography.bodySmall.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.Medium
                    ).tabular(),
                    color = colors.textSecondary
                )
            }

            ShiftlyProgressBar(
                progress = loan.progressFraction,
                color = progressColor,
                trackColor = colors.surface,
                height = 6.dp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${loan.paidMonthsClamped} / ${loan.termsInMonths} months paid",
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily).tabular(),
                    color = colors.textSecondary
                )

                Text(
                    text = if (loan.isCompleted) {
                        "Ended ${loan.contractEndDate().format(dateFormatter)}"
                    } else {
                        "Next due ${loan.nextDueDate().format(dateFormatter)}"
                    },
                    style = MaterialTheme.typography.labelSmall.copy(fontFamily = InterFontFamily).tabular(),
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun LoanStatusBadge(
    loan: InstallmentLoanEntity,
    today: LocalDate,
    dateFormatter: DateTimeFormatter
) {
    val colors = ShiftlyColors.current
    val status = loan.dueStatus(today)
    val days = loan.daysUntilNextDue(today)

    val (label, textColor, bgColor) = when (status) {
        LoanDueStatus.COMPLETED -> Triple(
            "Fully Paid ✓",
            colors.mintInk,
            colors.mintContainer
        )
        LoanDueStatus.OVERDUE -> Triple(
            "Overdue",
            colors.coralInk,
            colors.coralContainer
        )
        LoanDueStatus.DUE_TODAY -> Triple(
            "Due Today",
            colors.iosOrange,
            colors.orangeContainer
        )
        LoanDueStatus.DUE_SOON -> Triple(
            "Due Soon • in ${days}d",
            colors.iosOrange,
            colors.orangeContainer
        )
        LoanDueStatus.UPCOMING -> Triple(
            loan.nextDueDate().format(dateFormatter),
            colors.textSecondary,
            colors.surface
        )
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp
            ).tabular(),
            color = textColor,
            maxLines = 1
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstallmentLoanDetailsSheet(
    entry: InstallmentLoanWithPayments,
    currencySymbol: String,
    today: LocalDate = LocalDate.now(),
    onMarkMonthPaid: () -> Unit,
    onUpdateLoanPaymentDateTime: (LoanPaymentEntity, Long) -> Unit = { _, _ -> },
    onDeleteLoanPayment: (LoanPaymentEntity) -> Unit = {},
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val dateTimeFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy • h:mm a", locale) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var paymentToEdit by remember { mutableStateOf<LoanPaymentEntity?>(null) }
    var paymentToDelete by remember { mutableStateOf<LoanPaymentEntity?>(null) }

    val loan = entry.loan
    val sortedPayments = remember(entry.payments) {
        entry.payments.sortedByDescending { it.monthNumber }
    }
    val status = loan.dueStatus(today)
    val progressColor = when (status) {
        LoanDueStatus.COMPLETED -> colors.mintInk
        LoanDueStatus.OVERDUE -> colors.coralBright
        LoanDueStatus.DUE_TODAY, LoanDueStatus.DUE_SOON -> colors.iosOrange
        LoanDueStatus.UPCOMING -> colors.azureAccent
    }
    val interestPctText = loan.interestPercent?.let { pct ->
        " (${String.format(locale, "%.1f", pct)}%)"
    }.orEmpty()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = SheetTopRoundedShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            ShiftlySheetHeader(title = loan.gadgetName)

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
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
                                text = "REMAINING BALANCE",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontFamily = InterFontFamily,
                                    letterSpacing = 0.5.sp
                                ),
                                color = colors.textSecondary
                            )
                            LoanStatusBadge(
                                loan = loan,
                                today = today,
                                dateFormatter = dateFormatter
                            )
                        }

                        Text(
                            text = ShiftlyFormatters.formatCurrency(currencySymbol, loan.remainingBalance),
                            style = MaterialTheme.typography.headlineLarge.copy(
                                fontFamily = InterFontFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 32.sp
                            ).tabular(),
                            color = if (loan.isCompleted) colors.mintInk else colors.textPrimary
                        )

                        ShiftlyProgressBar(
                            progress = loan.progressFraction,
                            color = progressColor,
                            trackColor = colors.surface,
                            height = 8.dp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${loan.paidMonthsClamped} of ${loan.termsInMonths} months paid",
                                style = MaterialTheme.typography.bodySmall.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textPrimary
                            )
                            Text(
                                text = if (loan.isCompleted) {
                                    "Ended ${loan.contractEndDate().format(dateFormatter)}"
                                } else {
                                    "Ends ${loan.contractEndDate().format(dateFormatter)}"
                                },
                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                                color = colors.textSecondary
                            )
                        }
                    }
                }

                if (!loan.isCompleted) {
                    ShiftlyPrimaryPillButton(
                        text = "Mark Month as Paid (${ShiftlyFormatters.formatCurrency(currencySymbol, loan.installmentPerMonth)})",
                        onClick = onMarkMonthPaid
                    )
                }

                Text(
                    text = "FINANCIAL BREAKDOWN",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        letterSpacing = 0.5.sp
                    ),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        LoanBreakdownRow(
                            label = "Cash Price",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, loan.cashPrice)
                        )
                        LoanBreakdownRow(
                            label = "Downpayment",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, loan.downpayment)
                        )
                        LoanBreakdownRow(
                            label = "Installment / Month",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, loan.installmentPerMonth)
                        )
                        LoanBreakdownRow(
                            label = "Terms",
                            value = "${loan.termsInMonths} months"
                        )
                        LoanBreakdownRow(
                            label = "Final Price",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, loan.finalPrice)
                        )
                        LoanBreakdownRow(
                            label = "Total Interest",
                            value = "${ShiftlyFormatters.formatCurrency(currencySymbol, loan.totalInterest)}$interestPctText",
                            valueColor = if (loan.totalInterest > 0.0) colors.iosOrange else colors.mintInk
                        )
                        LoanBreakdownRow(
                            label = "Total Paid",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, loan.totalPaid),
                            valueColor = colors.mintInk
                        )
                        LoanBreakdownRow(
                            label = "Remaining Balance",
                            value = ShiftlyFormatters.formatCurrency(currencySymbol, loan.remainingBalance),
                            valueColor = if (loan.isCompleted) colors.mintInk else colors.textPrimary
                        )
                        LoanBreakdownRow(
                            label = "Remaining Months",
                            value = "${loan.remainingMonths} mo${if (loan.remainingMonths == 1) "" else "s"}"
                        )
                        if (!loan.isCompleted) {
                            LoanBreakdownRow(
                                label = "Next Due Date",
                                value = loan.nextDueDate().format(dateFormatter)
                            )
                        }
                    }
                }

                Text(
                    text = "PAYMENT HISTORY",
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontFamily = InterFontFamily,
                        letterSpacing = 0.5.sp
                    ),
                    color = colors.textSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (sortedPayments.isEmpty()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = if (loan.paidMonthsClamped > 0) {
                                    "${loan.paidMonthsClamped} prior month(s) marked paid"
                                } else {
                                    "No payments recorded yet"
                                },
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.Medium
                                ).tabular(),
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        val zone = remember { ZoneId.systemDefault() }
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            sortedPayments.forEach { payment ->
                                val paidZoned = Instant.ofEpochMilli(payment.datePaidMillis).atZone(zone)
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { paymentToEdit = payment }
                                        .padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = "Month ${payment.monthNumber}",
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 14.sp
                                            ).tabular(),
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = "Paid on ${paidZoned.format(dateTimeFormatter)}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily).tabular(),
                                            color = colors.textSecondary
                                        )
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        Text(
                                            text = ShiftlyFormatters.formatCurrency(currencySymbol, payment.amountPaid),
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontFamily = InterFontFamily,
                                                fontWeight = FontWeight.Bold
                                            ).tabular(),
                                            color = colors.mintInk,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                        IconButton(
                                            onClick = { paymentToEdit = payment },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_lucide_pencil),
                                                contentDescription = "Edit Payment Date & Time",
                                                tint = colors.azureInk,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        IconButton(
                                            onClick = { paymentToDelete = payment },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_lucide_trash_2),
                                                contentDescription = "Delete Payment",
                                                tint = colors.coralBright,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.cardElevated,
                        border = BorderStroke(0.5.dp, colors.separator),
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shiftlyPressClickable(onClick = onEdit)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_pencil),
                                contentDescription = null,
                                tint = colors.azureInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Edit Loan",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = colors.azureInk
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.coralContainer,
                        shadowElevation = 0.dp,
                        modifier = Modifier
                            .weight(1f)
                            .heightIn(min = 48.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .shiftlyPressClickable { showDeleteConfirm = true }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_lucide_trash_2),
                                contentDescription = null,
                                tint = colors.coralInk,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Delete Loan",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontFamily = InterFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = colors.coralInk
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ShiftlySheetCloseButton(
                label = "Close",
                onClick = onDismiss
            )
        }
    }

    if (showDeleteConfirm) {
        ShiftlyDeleteConfirmDialog(
            title = "Delete Loan Contract?",
            message = "This installment loan for ${loan.gadgetName} and its payment history will be permanently removed.",
            onDismiss = { showDeleteConfirm = false },
            onConfirm = {
                showDeleteConfirm = false
                onDelete()
            }
        )
    }

    if (paymentToEdit != null) {
        EditPaymentDateTimeDialog(
            payment = paymentToEdit!!,
            onDismiss = { paymentToEdit = null },
            onSave = { newMillis ->
                onUpdateLoanPaymentDateTime(paymentToEdit!!, newMillis)
                paymentToEdit = null
            }
        )
    }

    if (paymentToDelete != null) {
        ShiftlyDeleteConfirmDialog(
            title = "Delete Payment?",
            message = "This payment record will be removed and your loan balance will be updated.",
            confirmLabel = "Delete",
            onConfirm = {
                onDeleteLoanPayment(paymentToDelete!!)
                paymentToDelete = null
            },
            onDismiss = { paymentToDelete = null }
        )
    }
}

@Composable
private fun LoanTimePickerRow(
    formattedTime: String,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Time Paid",
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary,
            maxLines = 1
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
                text = formattedTime,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ).tabular(),
                color = colors.textPrimary
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditPaymentDateTimeDialog(
    payment: LoanPaymentEntity,
    onDismiss: () -> Unit,
    onSave: (Long) -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val timeFormatter = remember(locale) { DateTimeFormatter.ofPattern("h:mm a", locale) }

    val zone = remember { ZoneId.systemDefault() }
    val initialZoned = remember(payment.datePaidMillis) {
        Instant.ofEpochMilli(payment.datePaidMillis).atZone(zone)
    }

    var selectedDate by remember { mutableStateOf(initialZoned.toLocalDate()) }
    var selectedTime by remember { mutableStateOf(initialZoned.toLocalTime()) }

    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    ShiftlyDialogShell(
        title = "Edit Payment Date & Time",
        onDismiss = onDismiss,
        actionLabel = "Save",
        onAction = {
            val newZoned = ZonedDateTime.of(selectedDate, selectedTime, zone)
            onSave(newZoned.toInstant().toEpochMilli())
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.cardElevated,
                border = BorderStroke(0.5.dp, colors.separator),
                shadowElevation = 0.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    LoanDatePickerRow(
                        label = "Date Paid",
                        formattedDate = selectedDate.format(dateFormatter),
                        onClick = { showDatePicker = true }
                    )
                    LoanTimePickerRow(
                        formattedTime = selectedTime.format(timeFormatter),
                        onClick = { showTimePicker = true }
                    )
                }
            }
        }
    }

    if (showDatePicker) {
        LoanDatePickerDialog(
            title = "Date Paid",
            initialDate = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = { pickedDate ->
                selectedDate = pickedDate
                showDatePicker = false
            }
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
                        periodSelectorUnselectedContentColor = colors.textPrimary,
                        timeSelectorSelectedContainerColor = colors.azurePrimary,
                        timeSelectorUnselectedContainerColor = colors.cardElevated,
                        timeSelectorSelectedContentColor = colors.onAzurePrimary,
                        timeSelectorUnselectedContentColor = colors.textPrimary
                    )
                )
            }
        }
    }
}

@Composable
private fun LoanBreakdownRow(
    label: String,
    value: String,
    valueColor: Color = ShiftlyColors.current.textPrimary
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = InterFontFamily),
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ).tabular(),
            color = valueColor
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditInstallmentLoanSheet(
    currencySymbol: String,
    editingLoan: InstallmentLoanEntity? = null,
    onDismiss: () -> Unit,
    onSave: (InstallmentLoanEntity) -> Unit
) {
    val colors = ShiftlyColors.current
    val locale = LocalConfiguration.current.locales[0]
    val dateFormatter = remember(locale) { DateTimeFormatter.ofPattern("MMM d, yyyy", locale) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val today = remember { LocalDate.now() }
    var gadgetName by remember(editingLoan) { mutableStateOf(editingLoan?.gadgetName.orEmpty()) }
    var cashPriceText by remember(editingLoan) {
        mutableStateOf(
            editingLoan?.cashPrice?.takeIf { it > 0.0 }?.let { formatEditableAmount(it) }.orEmpty()
        )
    }
    var downpaymentText by remember(editingLoan) {
        mutableStateOf(
            editingLoan?.downpayment?.takeIf { it > 0.0 }?.let { formatEditableAmount(it) }.orEmpty()
        )
    }
    var installmentPerMonthText by remember(editingLoan) {
        mutableStateOf(
            editingLoan?.installmentPerMonth?.takeIf { it > 0.0 }?.let { formatEditableAmount(it) }.orEmpty()
        )
    }
    var termsText by remember(editingLoan) {
        mutableStateOf(editingLoan?.termsInMonths?.toString() ?: "12")
    }
    var startDate by remember(editingLoan) {
        mutableStateOf(editingLoan?.startDate ?: today)
    }
    var firstDueDate by remember(editingLoan) {
        mutableStateOf(editingLoan?.firstDueDate ?: today.plusMonths(1))
    }
    var dueDateEditedManually by remember(editingLoan) { mutableStateOf(editingLoan != null) }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showDueDatePicker by remember { mutableStateOf(false) }

    val parsedCashPrice = remember(cashPriceText) {
        (cashPriceText.toDoubleOrNull() ?: 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    }
    val parsedDownpayment = remember(downpaymentText) {
        (downpaymentText.toDoubleOrNull() ?: 0.0).takeIf { it.isFinite() && it >= 0.0 } ?: 0.0
    }
    val parsedInstallment = remember(installmentPerMonthText) {
        (installmentPerMonthText.toDoubleOrNull() ?: 0.0).takeIf { it.isFinite() && it > 0.0 } ?: 0.0
    }
    val parsedTerms = remember(termsText) {
        (termsText.toIntOrNull() ?: 0).coerceAtLeast(0)
    }

    val isValid = gadgetName.trim().isNotEmpty() && parsedInstallment > 0.0 && parsedTerms > 0

    val previewLoan = remember(
        editingLoan,
        gadgetName,
        parsedCashPrice,
        parsedDownpayment,
        parsedInstallment,
        parsedTerms,
        startDate,
        firstDueDate
    ) {
        InstallmentLoanEntity(
            id = editingLoan?.id ?: 0L,
            gadgetName = gadgetName.trim().ifEmpty { "Loan" },
            cashPrice = parsedCashPrice,
            downpayment = parsedDownpayment,
            installmentPerMonth = parsedInstallment,
            termsInMonths = parsedTerms.coerceAtLeast(1),
            paidMonths = (editingLoan?.paidMonths ?: 0).coerceIn(0, parsedTerms.coerceAtLeast(1)),
            startDateMillis = startDate.toUtcMillis(),
            monthlyDueDay = firstDueDate.dayOfMonth,
            firstDueDateMillis = firstDueDate.toUtcMillis()
        )
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surface,
        scrimColor = shiftlySheetScrimColor(),
        tonalElevation = 0.dp,
        dragHandle = { ShiftlySheetDragHandle() },
        shape = SheetTopRoundedShape
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 20.dp)
        ) {
            ShiftlySheetHeader(
                title = if (editingLoan == null) "Add Loan" else "Edit Loan"
            )

            Spacer(modifier = Modifier.height(10.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        LoanInsetFieldRow(
                            label = "Product Name",
                            value = gadgetName,
                            onValueChange = { gadgetName = it },
                            placeholder = "e.g. Laptop"
                        )

                        LoanInsetFieldRow(
                            label = "Cash Price",
                            value = cashPriceText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) cashPriceText = filtered
                            },
                            placeholder = "0.00",
                            prefix = currencySymbol,
                            keyboardType = KeyboardType.Decimal
                        )

                        LoanInsetFieldRow(
                            label = "Downpayment",
                            value = downpaymentText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) downpaymentText = filtered
                            },
                            placeholder = "0.00",
                            prefix = currencySymbol,
                            keyboardType = KeyboardType.Decimal
                        )

                        LoanInsetFieldRow(
                            label = "Installment / Month",
                            value = installmentPerMonthText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() || it == '.' }
                                if (filtered.count { it == '.' } <= 1) installmentPerMonthText = filtered
                            },
                            placeholder = "0.00",
                            prefix = currencySymbol,
                            keyboardType = KeyboardType.Decimal
                        )

                        LoanInsetFieldRow(
                            label = "Terms (Months)",
                            value = termsText,
                            onValueChange = { input ->
                                val filtered = input.filter { it.isDigit() }.take(3)
                                termsText = filtered
                            },
                            placeholder = "12",
                            keyboardType = KeyboardType.Number
                        )

                        LoanDatePickerRow(
                            label = "Start Date",
                            formattedDate = startDate.format(dateFormatter),
                            onClick = { showStartDatePicker = true }
                        )

                        LoanDatePickerRow(
                            label = "Monthly Due Date",
                            formattedDate = firstDueDate.format(dateFormatter),
                            onClick = { showDueDatePicker = true }
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.cardElevated,
                    border = BorderStroke(0.5.dp, colors.separator),
                    shadowElevation = 0.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "SUMMARY",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontFamily = InterFontFamily,
                                letterSpacing = 0.5.sp
                            ),
                            color = colors.textSecondary
                        )
                        LoanPreviewRow(
                            label = "Final Price",
                            value = if (parsedTerms > 0 && parsedInstallment > 0.0) {
                                ShiftlyFormatters.formatCurrency(currencySymbol, previewLoan.finalPrice)
                            } else {
                                "—"
                            }
                        )
                        val interestPct = if (parsedTerms > 0 && parsedInstallment > 0.0) {
                            previewLoan.interestPercent
                        } else {
                            null
                        }
                        LoanPreviewRow(
                            label = "Total Interest",
                            value = if (parsedTerms > 0 && parsedInstallment > 0.0) {
                                val amt = ShiftlyFormatters.formatCurrency(currencySymbol, previewLoan.totalInterest)
                                if (interestPct != null) "$amt (${String.format(locale, "%.1f", interestPct)}%)" else amt
                            } else {
                                "—"
                            },
                            valueColor = if (previewLoan.totalInterest > 0.0) colors.iosOrange else colors.mintInk
                        )
                        LoanPreviewRow(
                            label = "Remaining Balance",
                            value = if (parsedTerms > 0 && parsedInstallment > 0.0) {
                                ShiftlyFormatters.formatCurrency(currencySymbol, previewLoan.remainingBalance)
                            } else {
                                "—"
                            },
                            valueColor = colors.azureInk
                        )
                        LoanPreviewRow(
                            label = "Estimated End Date",
                            value = if (parsedTerms > 0) {
                                previewLoan.contractEndDate().format(dateFormatter)
                            } else {
                                "—"
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            ShiftlySheetBottomButtons(
                onCancel = onDismiss,
                onAction = {
                    if (isValid) {
                        onSave(previewLoan)
                    }
                },
                cancelLabel = "Cancel",
                actionLabel = "Save",
                actionEnabled = isValid
            )
        }
    }

    if (showStartDatePicker) {
        LoanDatePickerDialog(
            title = "Start Date",
            initialDate = startDate,
            onDismiss = { showStartDatePicker = false },
            onConfirm = { picked ->
                startDate = picked
                if (!dueDateEditedManually) {
                    firstDueDate = picked.plusMonths(1)
                }
                showStartDatePicker = false
            }
        )
    }

    if (showDueDatePicker) {
        LoanDatePickerDialog(
            title = "Monthly Due Date",
            initialDate = firstDueDate,
            onDismiss = { showDueDatePicker = false },
            onConfirm = { picked ->
                firstDueDate = picked
                dueDateEditedManually = true
                showDueDatePicker = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoanDatePickerDialog(
    title: String,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (LocalDate) -> Unit
) {
    val colors = ShiftlyColors.current
    val initialMillis = remember(initialDate) {
        initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    }
    val pickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

    ShiftlyDialogShell(
        title = title,
        onDismiss = onDismiss,
        actionLabel = "Done",
        onAction = {
            val selected = pickerState.selectedDateMillis?.let { millis ->
                Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
            } ?: initialDate
            onConfirm(selected)
        },
        horizontalOuterPadding = 12.dp,
        contentHorizontalPadding = 8.dp
    ) {
        DatePicker(
            state = pickerState,
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

@Composable
private fun LoanDatePickerRow(
    label: String,
    formattedDate: String,
    onClick: () -> Unit
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary,
            maxLines = 1
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
                text = formattedDate,
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.Medium
                ).tabular(),
                color = colors.textPrimary
            )
        }
    }
}

@Composable
private fun LoanInsetFieldRow(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    prefix: String? = null,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    val colors = ShiftlyColors.current
    val isNumeric = keyboardType != KeyboardType.Text
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            ),
            color = colors.textPrimary,
            maxLines = 1,
            modifier = Modifier.padding(end = 10.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (prefix != null && value.isNotEmpty()) {
                Text(
                    text = prefix,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontFamily = InterFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary
                    )
                )
            }
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
                cursorBrush = SolidColor(colors.azureAccent),
                textStyle = MaterialTheme.typography.bodyMedium.copy(
                    fontFamily = InterFontFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    textAlign = TextAlign.End
                ).let { if (isNumeric) it.tabular() else it },
                decorationBox = { inner ->
                    Box(contentAlignment = Alignment.CenterEnd) {
                        if (value.isEmpty()) {
                            Text(
                                text = placeholder,
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
                modifier = Modifier.width(136.dp)
            )
        }
    }
}

@Composable
private fun LoanPreviewRow(
    label: String,
    value: String,
    valueColor: Color = ShiftlyColors.current.textPrimary
) {
    val colors = ShiftlyColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall.copy(fontFamily = InterFontFamily),
            color = colors.textSecondary
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontFamily = InterFontFamily,
                fontWeight = FontWeight.SemiBold
            ).tabular(),
            color = valueColor
        )
    }
}

private fun formatEditableAmount(value: Double): String {
    val rounded = value.toLong()
    return if (value == rounded.toDouble()) {
        rounded.toString()
    } else {
        String.format(Locale.US, "%.2f", value)
    }
}
