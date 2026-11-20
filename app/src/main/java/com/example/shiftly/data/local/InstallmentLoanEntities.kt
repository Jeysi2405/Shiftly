package com.example.shiftly.data.local

import androidx.compose.runtime.Immutable
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import kotlin.math.ceil
import kotlin.math.roundToLong

/**
 * Contract & installment loan (e.g. a financed laptop or phone).
 *
 * `startDateMillis` and `firstDueDateMillis` are stored as UTC-midnight millis of the picked
 * calendar date (the same value Material 3 `DatePicker` produces), so the chosen day never shifts
 * with the device time zone. Use [utcMillisToLocalDate] / [toUtcMillis] to convert.
 */
@Immutable
@Entity(tableName = "installment_loans")
data class InstallmentLoanEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val gadgetName: String,
    val cashPrice: Double,
    val downpayment: Double,
    val installmentPerMonth: Double,
    val termsInMonths: Int,
    val paidMonths: Int = 0,
    val startDateMillis: Long,
    val monthlyDueDay: Int,
    val firstDueDateMillis: Long
)

@Immutable
@Entity(
    tableName = "loan_payments",
    foreignKeys = [
        ForeignKey(
            entity = InstallmentLoanEntity::class,
            parentColumns = ["id"],
            childColumns = ["loanId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["loanId"])]
)
data class LoanPaymentEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val loanId: Long,
    val monthNumber: Int,
    val amountPaid: Double,
    val datePaidMillis: Long
)

data class InstallmentLoanWithPayments(
    @Embedded val loan: InstallmentLoanEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "loanId"
    )
    val payments: List<LoanPaymentEntity>
)

enum class LoanDueStatus {
    COMPLETED,
    OVERDUE,
    DUE_TODAY,
    DUE_SOON,
    UPCOMING
}

const val LOAN_DUE_SOON_WINDOW_DAYS = 7L

private fun Double.nonNegativeOrZero(): Double = if (isFinite()) coerceAtLeast(0.0) else 0.0

private fun roundMoney(value: Double): Double = (value * 100.0).roundToLong() / 100.0

fun LocalDate.toUtcMillis(): Long = atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

fun utcMillisToLocalDate(millis: Long): LocalDate =
    Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()

// ---------------------------------------------------------------------------------------------
// Automatic calculations
// ---------------------------------------------------------------------------------------------

/** Total Amount Payable = downpayment + (installment per month * terms). */
val InstallmentLoanEntity.finalPrice: Double
    get() = downpayment.nonNegativeOrZero() +
        installmentPerMonth.nonNegativeOrZero() * termsInMonths.coerceAtLeast(0)

val InstallmentLoanEntity.totalInterest: Double
    get() = (finalPrice - cashPrice.nonNegativeOrZero()).coerceAtLeast(0.0)

/** Effective interest as a percentage of cash price, or null when cash price is unknown. */
val InstallmentLoanEntity.interestPercent: Double?
    get() = cashPrice.nonNegativeOrZero().takeIf { it > 0.0 }?.let { totalInterest / it * 100.0 }

val InstallmentLoanEntity.paidMonthsClamped: Int
    get() = paidMonths.coerceIn(0, termsInMonths.coerceAtLeast(0))

val InstallmentLoanEntity.totalPaid: Double
    get() = downpayment.nonNegativeOrZero() +
        installmentPerMonth.nonNegativeOrZero() * paidMonthsClamped

val InstallmentLoanEntity.remainingBalance: Double
    get() = (finalPrice - totalPaid).coerceAtLeast(0.0)

val InstallmentLoanEntity.remainingMonths: Int
    get() = (termsInMonths.coerceAtLeast(0) - paidMonths.coerceAtLeast(0)).coerceAtLeast(0)

val InstallmentLoanEntity.progressFraction: Float
    get() = (paidMonthsClamped.toFloat() / termsInMonths.coerceAtLeast(1)).coerceIn(0f, 1f)

val InstallmentLoanEntity.isCompleted: Boolean
    get() = termsInMonths > 0 && remainingMonths == 0

val InstallmentLoanEntity.startDate: LocalDate
    get() = utcMillisToLocalDate(startDateMillis)

val InstallmentLoanEntity.firstDueDate: LocalDate
    get() = utcMillisToLocalDate(firstDueDateMillis)

/**
 * Due date for the payment that is [monthsAfterFirst] months after the first due date. The day of
 * month stays on [InstallmentLoanEntity.monthlyDueDay] and is clamped to the month length
 * (e.g. a due day of 31 falls on Feb 28/29 in February).
 */
fun InstallmentLoanEntity.dueDateAfterMonths(monthsAfterFirst: Int): LocalDate {
    if (monthsAfterFirst <= 0) return firstDueDate
    val shifted = firstDueDate.plusMonths(monthsAfterFirst.toLong())
    val day = monthlyDueDay.coerceIn(1, 31)
    return shifted.withDayOfMonth(minOf(day, shifted.lengthOfMonth()))
}

/** First due date advanced by the number of months already paid. */
fun InstallmentLoanEntity.nextDueDate(): LocalDate = dueDateAfterMonths(paidMonths.coerceAtLeast(0))

/** Estimated end date of the contract: the due date of the final installment. */
fun InstallmentLoanEntity.contractEndDate(): LocalDate =
    dueDateAfterMonths((termsInMonths - 1).coerceAtLeast(0))

fun InstallmentLoanEntity.daysUntilNextDue(today: LocalDate = LocalDate.now()): Long =
    ChronoUnit.DAYS.between(today, nextDueDate())

fun InstallmentLoanEntity.dueStatus(today: LocalDate = LocalDate.now()): LoanDueStatus {
    if (isCompleted) return LoanDueStatus.COMPLETED
    val days = daysUntilNextDue(today)
    return when {
        days < 0L -> LoanDueStatus.OVERDUE
        days == 0L -> LoanDueStatus.DUE_TODAY
        days <= LOAN_DUE_SOON_WINDOW_DAYS -> LoanDueStatus.DUE_SOON
        else -> LoanDueStatus.UPCOMING
    }
}

/** Active loans that need attention: overdue, due today, or due within a week. */
fun InstallmentLoanEntity.needsDueAttention(today: LocalDate = LocalDate.now()): Boolean =
    when (dueStatus(today)) {
        LoanDueStatus.OVERDUE, LoanDueStatus.DUE_TODAY, LoanDueStatus.DUE_SOON -> true
        else -> false
    }

/**
 * Returns a sanitized copy ready to be stored, or null when the loan is not valid
 * (blank name, non-positive installment, or no terms).
 */
fun InstallmentLoanEntity.normalizedOrNull(): InstallmentLoanEntity? {
    val name = gadgetName.trim()
    if (name.isEmpty() || !installmentPerMonth.isFinite() || installmentPerMonth <= 0.0 || termsInMonths <= 0) {
        return null
    }
    val safeTerms = termsInMonths.coerceAtMost(MAX_LOAN_TERMS_IN_MONTHS)
    return copy(
        gadgetName = name,
        cashPrice = roundMoney(cashPrice.nonNegativeOrZero()),
        downpayment = roundMoney(downpayment.nonNegativeOrZero()),
        installmentPerMonth = roundMoney(installmentPerMonth),
        termsInMonths = safeTerms,
        paidMonths = paidMonths.coerceIn(0, safeTerms),
        monthlyDueDay = monthlyDueDay.coerceIn(1, 31)
    )
}

const val MAX_LOAN_TERMS_IN_MONTHS = 600

// ---------------------------------------------------------------------------------------------
// Legacy (DataStore JSON) -> Room conversion
// ---------------------------------------------------------------------------------------------

/**
 * Converts a loan saved by the earlier DataStore-based tracker into the Room schema. Non-monthly
 * frequencies are folded into monthly equivalents (semi-monthly = 2, weekly = 4, everyday = 30
 * payments per month).
 */
fun InstallmentLoan.toInstallmentLoanEntity(zone: ZoneId = ZoneId.systemDefault()): InstallmentLoanEntity {
    val paymentsPerMonth = when (frequency) {
        InstallmentLoan.FREQ_SEMI_MONTHLY -> 2
        InstallmentLoan.FREQ_WEEKLY -> 4
        InstallmentLoan.FREQ_EVERYDAY -> 30
        else -> 1
    }
    val terms = ceil(totalInstallments.coerceAtLeast(1).toDouble() / paymentsPerMonth)
        .toInt()
        .coerceIn(1, MAX_LOAN_TERMS_IN_MONTHS)
    val paid = (paidInstallments.coerceAtLeast(0) / paymentsPerMonth).coerceIn(0, terms)
    val start = Instant.ofEpochMilli(startDateMillis).atZone(zone).toLocalDate()
    val labelDay = Regex("\\d{1,2}").find(dueDayLabel)?.value?.toIntOrNull()?.takeIf { it in 1..31 }
    val firstDue = if (labelDay != null) {
        val candidate = start.withDayOfMonth(minOf(labelDay, start.lengthOfMonth()))
        if (candidate.isAfter(start)) candidate else start.plusMonths(1).let {
            it.withDayOfMonth(minOf(labelDay, it.lengthOfMonth()))
        }
    } else {
        start.plusMonths(1)
    }
    val name = itemName.trim().let { base ->
        if (lenderOrStore.isNotBlank()) "$base (${lenderOrStore.trim()})" else base
    }
    return InstallmentLoanEntity(
        gadgetName = name,
        cashPrice = roundMoney(totalContractAmount),
        downpayment = roundMoney(downPayment.nonNegativeOrZero()),
        installmentPerMonth = roundMoney(installmentAmount.nonNegativeOrZero() * paymentsPerMonth),
        termsInMonths = terms,
        paidMonths = paid,
        startDateMillis = start.toUtcMillis(),
        monthlyDueDay = labelDay ?: firstDue.dayOfMonth,
        firstDueDateMillis = firstDue.toUtcMillis()
    )
}
