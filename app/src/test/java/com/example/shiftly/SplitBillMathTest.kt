package com.example.shiftly

import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.utils.SplitBillMath
import org.junit.Assert.assertEquals
import org.junit.Test

class SplitBillMathTest {

    @Test
    fun distributeEqualShares_handlesRepeatingDecimalsExactToCents() {
        val shares = SplitBillMath.distributeEqualShares(100.0, 3)
        assertEquals(3, shares.size)
        assertEquals(33.34, shares[0], 0.0001)
        assertEquals(33.33, shares[1], 0.0001)
        assertEquals(33.33, shares[2], 0.0001)
        assertEquals(100.0, shares.sum(), 0.0001)
    }

    @Test
    fun summarizeFriendsOwed_excludesCurrentUserFromPendingAndCollected() {
        val bill = SplitBillEntity(
            id = 1L,
            title = "Group Dinner",
            totalAmount = 1200.0,
            personCount = 4,
            perPersonAmount = 300.0,
            dateEpochDay = 20500L,
            isLoggedToExpense = true
        )
        val participants = listOf(
            SplitParticipantEntity(1L, 1L, "You", 300.0, hasPaid = true, isCurrentUser = true),
            SplitParticipantEntity(2L, 1L, "Jules", 300.0, hasPaid = true, isCurrentUser = false),
            SplitParticipantEntity(3L, 1L, "Kian", 300.0, hasPaid = false, isCurrentUser = false),
            SplitParticipantEntity(4L, 1L, "Bea", 300.0, hasPaid = false, isCurrentUser = false)
        )

        val metrics = SplitBillMath.summarizeFriendsOwed(
            listOf(SplitBillWithParticipants(bill = bill, participants = participants))
        )

        assertEquals(600.0, metrics.pendingFriendsAmount, 0.001)
        assertEquals(300.0, metrics.collectedFriendsAmount, 0.001)
        assertEquals(1, metrics.paidFriendsCount)
        assertEquals(3, metrics.totalFriendsCount)
        assertEquals(1, metrics.pendingBillCount)
    }
}
