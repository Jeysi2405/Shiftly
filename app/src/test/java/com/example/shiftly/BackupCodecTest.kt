package com.example.shiftly

import com.example.shiftly.data.backup.BackupCodec
import com.example.shiftly.data.backup.BackupExpenseRecord
import com.example.shiftly.data.backup.BackupPayload
import com.example.shiftly.data.backup.BackupSplitBill
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitParticipantEntity
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCodecTest {

    @Test
    fun jsonAndZipRoundTrip_preservesAllEntitiesSettingsAndReceipts() {
        val receiptBytes = byteArrayOf(1, 2, 3, 4, 5, 6, 7)
        val payload = BackupPayload(
            exportedAtMillis = 1_763_000_000_000L,
            userName = "Jeyzi",
            currencySymbol = "₱",
            defaultHourlyRate = 110.0,
            monthlyBudget = 4500.0,
            savingsBuffer = 1500.0,
            settings = ShiftlySettingsSnapshot(
                payCycleSchedule = "Weekly (Fri)",
                paydayReminderEnabled = true,
                shiftReminderEnabled = false,
                notificationsEnabled = true,
                nightDiffStartHour = 21,
                nightDiffMultiplier = 1.15,
                overtimeMultiplier = 1.30,
                savingsGoalTitle = "Laptop",
                savingsGoalTarget = 25000.0,
                categoryBudgets = listOf(CategoryBudgetSetting("Food", 1800.0)),
                managedCategories = listOf(ManagedCategorySetting("Food", true)),
                recurringEntries = listOf(
                    RecurringEntrySetting("r1", "Allowance", 1200.0, "Weekly", isIncome = true, isEnabled = true)
                )
            ),
            shifts = listOf(
                ShiftEntity(
                    id = 1,
                    dateEpochDay = 20400L,
                    timeInMillis = 1000L,
                    timeOutMillis = 15400000L,
                    durationMinutes = 256,
                    lockedHourlyRate = 110.0,
                    totalEarned = 469.33,
                    overtimeNote = "Evening Café",
                    dayType = "ORDINARY"
                )
            ),
            expenses = listOf(
                BackupExpenseRecord(
                    expense = ExpenseEntity(
                        id = 1,
                        title = "Split: Group Dinner",
                        amount = 250.0,
                        category = "Split Bill",
                        dateEpochDay = 20400L,
                        linkedSplitBillId = 42L,
                        createdAtMillis = 1_763_000_123_000L
                    ),
                    receiptFileName = "receipt_dinner.jpg"
                )
            ),
            extraIncomes = listOf(
                ExtraIncomeEntity(
                    id = 10L,
                    title = "Weekly Allowance",
                    amount = 1200.0,
                    dateEpochDay = 20400L,
                    sourceRuleId = "r1",
                    createdAtMillis = 1_763_000_456_000L
                )
            ),
            splitBills = listOf(
                BackupSplitBill(
                    originalId = 42L,
                    bill = SplitBillEntity(
                        id = 42L,
                        title = "Group Dinner",
                        totalAmount = 500.0,
                        personCount = 2,
                        perPersonAmount = 250.0,
                        dateEpochDay = 20400L,
                        isLoggedToExpense = true
                    ),
                    participants = listOf(
                        SplitParticipantEntity(billId = 42L, personLabel = "Jeyzi", amountOwed = 250.0, hasPaid = true, isCurrentUser = true),
                        SplitParticipantEntity(billId = 42L, personLabel = "Alex", amountOwed = 250.0, hasPaid = false, isCurrentUser = false)
                    )
                )
            ),
            receiptFiles = mapOf("receipt_dinner.jpg" to receiptBytes)
        )

        val zipBytes = BackupCodec.encodeZipBytes(payload)
        val decoded = BackupCodec.decodeZipOrJsonBytes(zipBytes)

        assertEquals("Jeyzi", decoded.userName)
        assertEquals("₱", decoded.currencySymbol)
        assertEquals(4500.0, decoded.monthlyBudget ?: 0.0, 0.001)
        assertEquals(1500.0, decoded.savingsBuffer ?: 0.0, 0.001)
        assertNotNull(decoded.settings)
        assertEquals(21, decoded.settings?.nightDiffStartHour)
        assertEquals(1, decoded.shifts.size)
        assertEquals("Evening Café", decoded.shifts.first().overtimeNote)
        assertEquals(1, decoded.expenses.size)
        assertEquals(42L, decoded.expenses.first().expense.linkedSplitBillId)
        assertEquals("receipt_dinner.jpg", decoded.expenses.first().receiptFileName)
        assertEquals(1, decoded.extraIncomes.size)
        assertEquals("Weekly Allowance", decoded.extraIncomes.first().title)
        assertEquals("r1", decoded.extraIncomes.first().sourceRuleId)
        assertEquals(1, decoded.splitBills.size)
        assertEquals(2, decoded.splitBills.first().participants.size)
        assertTrue(decoded.splitBills.first().participants[0].isCurrentUser)
        assertArrayEquals(receiptBytes, decoded.receiptFiles["receipt_dinner.jpg"])
    }

    @Test
    fun legacyV2Json_decodesCleanlyAndSignaturesMatchDuplicates() {
        val v2Json = """
            {
              "app": "Shiftly",
              "version": 2,
              "userName": "Maya",
              "currencySymbol": "₱",
              "defaultHourlyRate": 96.38,
              "shifts": [
                {
                  "dateEpochDay": 20400,
                  "timeInMillis": 1000,
                  "timeOutMillis": 7201000,
                  "durationMinutes": 120,
                  "lockedHourlyRate": 96.38,
                  "totalEarned": 192.76,
                  "overtimeNote": "Desk"
                }
              ],
              "expenses": [
                {
                  "title": "Coffee",
                  "amount": 95.0,
                  "category": "Food",
                  "dateEpochDay": 20400
                }
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decodeJson(v2Json)
        assertEquals(1, decoded.shifts.size)
        assertEquals(1, decoded.expenses.size)
        assertEquals(0, decoded.extraIncomes.size)
        assertEquals(0, decoded.splitBills.size)

        val e1 = decoded.expenses.first().expense
        val e2 = ExpenseEntity(title = " coffee ", amount = 95.00, category = "food", dateEpochDay = 20400L)
        assertEquals(BackupCodec.expenseSignature(e1), BackupCodec.expenseSignature(e2))

        val inc1 = ExtraIncomeEntity(title = " Weekly Allowance ", amount = 1200.0, dateEpochDay = 20400L, sourceRuleId = "r1")
        val inc2 = ExtraIncomeEntity(title = "weekly allowance", amount = 1200.00, dateEpochDay = 20400L, sourceRuleId = "r1")
        assertEquals(BackupCodec.extraIncomeSignature(inc1), BackupCodec.extraIncomeSignature(inc2))
    }

    @Test(expected = IllegalArgumentException::class)
    fun nonShiftlyJson_throwsHelpfulError() {
        BackupCodec.decodeJson("""{"app":"OtherApp","shifts":[]}""")
    }
}
