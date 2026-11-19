package com.example.shiftly.utils

import androidx.compose.runtime.Immutable
import com.example.shiftly.data.local.SplitBillWithParticipants
import kotlin.math.roundToLong

@Immutable
data class SplitSummaryMetrics(
    val pendingFriendsAmount: Double = 0.0,
    val collectedFriendsAmount: Double = 0.0,
    val paidFriendsCount: Int = 0,
    val totalFriendsCount: Int = 0,
    val pendingBillCount: Int = 0
)

object SplitBillMath {
    /**
     * Distributes [totalAmount] across [personCount] shares using exact integer-cent
     * division so the sum of all shares strictly equals [totalAmount] rounded to cents.
     * Any remainder cents (+0.01) are distributed one per share starting from index 0.
     */
    fun distributeEqualShares(totalAmount: Double, personCount: Int): List<Double> {
        if (personCount <= 0) return emptyList()
        if (totalAmount <= 0.0) return List(personCount) { 0.0 }

        val totalCents = (totalAmount * 100.0).roundToLong().coerceAtLeast(0L)
        val baseCents = totalCents / personCount
        val remainder = (totalCents % personCount).toInt()

        return List(personCount) { index ->
            val cents = baseCents + if (index < remainder) 1L else 0L
            cents / 100.0
        }
    }

    /**
     * Summarizes amounts owed by and collected from friends (participants where `!isCurrentUser`).
     */
    fun summarizeFriendsOwed(bills: List<SplitBillWithParticipants>): SplitSummaryMetrics {
        val friendParticipants = bills.flatMap { billWithParts ->
            billWithParts.participants.filter { !it.isCurrentUser }
        }
        val collected = friendParticipants.filter { it.hasPaid }.sumOf { it.amountOwed }
        val pending = friendParticipants.filter { !it.hasPaid }.sumOf { it.amountOwed }
        val paidCount = friendParticipants.count { it.hasPaid }
        val pendingBillCount = bills.count { billWithParts ->
            billWithParts.participants.any { !it.isCurrentUser && !it.hasPaid }
        }

        return SplitSummaryMetrics(
            pendingFriendsAmount = pending,
            collectedFriendsAmount = collected,
            paidFriendsCount = paidCount,
            totalFriendsCount = friendParticipants.size,
            pendingBillCount = pendingBillCount
        )
    }
}
