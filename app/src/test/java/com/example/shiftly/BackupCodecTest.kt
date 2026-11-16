package com.example.shiftly

import com.example.shiftly.data.backup.BackupCodec
import com.example.shiftly.data.backup.BackupExpenseRecord
import com.example.shiftly.data.backup.BackupPayload
import com.example.shiftly.data.backup.BackupSavingsGoal
import com.example.shiftly.data.backup.BackupSplitBill
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.utils.WageCalculator
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
            minimumWagePerDay = 645.0,
            monthlyBudget = 4500.0,
            savingsBuffer = 1500.0,
            settings = ShiftlySettingsSnapshot(
                payCycleSchedule = "Weekly (Fri)",
                paydayReminderEnabled = true,
                billReminderEnabled = true,
                shiftReminderEnabled = false,
                savingsReminderEnabled = false,
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
                ),
                shiftTimePresets = listOf(
                    ShiftTimePreset("p1", "12 AM – 6 AM", 0, 0, 6, 0)
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
                ),
                BackupExpenseRecord(
                    expense = ExpenseEntity(
                        id = 2,
                        title = "Savings: Laptop",
                        amount = 500.0,
                        category = "Savings",
                        dateEpochDay = 20400L,
                        linkedSavingsDepositId = 99L,
                        createdAtMillis = 1_763_000_200_000L
                    )
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
            savingsGoals = listOf(
                BackupSavingsGoal(
                    originalId = 7L,
                    goal = SavingsGoalEntity(
                        id = 7L,
                        title = "Laptop",
                        targetAmount = 25000.0,
                        targetDateEpochDay = 20450L,
                        isActive = true,
                        createdAtMillis = 1_763_000_000_000L
                    ),
                    deposits = listOf(
                        SavingsDepositEntity(
                            id = 99L,
                            goalId = 7L,
                            amount = 500.0,
                            timestampMillis = 1_763_000_200_000L,
                            note = "First cutoff savings"
                        )
                    )
                )
            ),
            receiptFiles = mapOf("receipt_dinner.jpg" to receiptBytes)
        )

        val zipBytes = BackupCodec.encodeZipBytes(payload)
        val decoded = BackupCodec.decodeZipOrJsonBytes(zipBytes)

        assertEquals("Jeyzi", decoded.userName)
        assertEquals("₱", decoded.currencySymbol)
        assertEquals(645.0, decoded.minimumWagePerDay ?: 0.0, 0.001)
        assertEquals(4500.0, decoded.monthlyBudget ?: 0.0, 0.001)
        assertEquals(1500.0, decoded.savingsBuffer ?: 0.0, 0.001)
        assertNotNull(decoded.settings)
        // Fixed DOLE shift calculation rules enforced regardless of legacy fields
        assertEquals(WageCalculator.FIXED_NIGHT_DIFF_START_HOUR, decoded.settings?.nightDiffStartHour)
        assertEquals(WageCalculator.FIXED_NIGHT_DIFF_MULTIPLIER, decoded.settings?.nightDiffMultiplier ?: 0.0, 0.001)
        assertEquals(WageCalculator.FIXED_ORDINARY_OVERTIME_MULTIPLIER, decoded.settings?.overtimeMultiplier ?: 0.0, 0.001)
        // Notification toggles round-trip cleanly
        assertTrue(decoded.settings?.billReminderEnabled == true)
        assertFalse(decoded.settings?.shiftReminderEnabled ?: true)
        assertFalse(decoded.settings?.savingsReminderEnabled ?: true)
        assertEquals(1, decoded.settings?.shiftTimePresets?.size)
        assertEquals("12 AM – 6 AM", decoded.settings?.shiftTimePresets?.firstOrNull()?.label)
        assertEquals(1, decoded.shifts.size)
        assertEquals("Evening Café", decoded.shifts.first().overtimeNote)
        assertEquals(2, decoded.expenses.size)
        assertEquals(42L, decoded.expenses[0].expense.linkedSplitBillId)
        assertEquals("receipt_dinner.jpg", decoded.expenses[0].receiptFileName)
        assertEquals(99L, decoded.expenses[1].expense.linkedSavingsDepositId)
        assertEquals(1, decoded.extraIncomes.size)
        assertEquals("Weekly Allowance", decoded.extraIncomes.first().title)
        assertEquals("r1", decoded.extraIncomes.first().sourceRuleId)
        assertEquals(1, decoded.splitBills.size)
        assertEquals(2, decoded.splitBills.first().participants.size)
        assertTrue(decoded.splitBills.first().participants[0].isCurrentUser)
        assertEquals(1, decoded.savingsGoals.size)
        assertEquals("Laptop", decoded.savingsGoals.first().goal.title)
        assertEquals(20450L, decoded.savingsGoals.first().goal.targetDateEpochDay)
        assertEquals(1, decoded.savingsGoals.first().deposits.size)
        assertEquals(99L, decoded.savingsGoals.first().deposits.first().id)
        assertEquals("First cutoff savings", decoded.savingsGoals.first().deposits.first().note)
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
        assertEquals(0, decoded.savingsGoals.size)

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

    @Test
    fun csvExport_escapesRfc4180FieldsAndIncludesExtraIncomes() {
        val csv = BackupCodec.buildCsvExport(
            currencySymbol = "₱",
            shifts = listOf(
                ShiftEntity(
                    id = 1L,
                    dateEpochDay = 20400L,
                    timeInMillis = 1000L,
                    timeOutMillis = 7201000L,
                    durationMinutes = 120,
                    lockedHourlyRate = 100.0,
                    totalEarned = 200.5,
                    overtimeNote = "Late, \"Desk\"\nNote",
                    dayType = "ORDINARY"
                )
            ),
            expenses = listOf(
                ExpenseEntity(
                    id = 1L,
                    title = "Cafe, \"Morning\"\nSnack",
                    amount = 125.75,
                    category = "Food",
                    dateEpochDay = 20400L
                )
            ),
            extraIncomes = listOf(
                ExtraIncomeEntity(
                    id = 2L,
                    title = "Bonus, \"Q4\"",
                    amount = 1500.0,
                    dateEpochDay = 20400L,
                    sourceRuleId = "r1"
                )
            )
        )

        assertTrue(csv.contains("Type,Date,Title_Or_Note,Category_Or_DayType,Duration_Min,${BackupCodec.escapeCsvField("Amount (₱)")}"))
        assertTrue(csv.contains("\"Cafe, \"\"Morning\"\"\nSnack\""))
        assertTrue(csv.contains("\"Late, \"\"Desk\"\"\nNote\""))
        assertTrue(csv.contains("\"Bonus, \"\"Q4\"\"\""))
        assertTrue(csv.contains("ExtraIncome,"))
        assertTrue(csv.contains("125.75"))
        assertTrue(csv.contains("1500.00"))
    }

    @Test
    fun decodeJson_filtersInvalidRecordsAndPreservesSmallSavingsGoalTarget() {
        val json = """
            {
              "app": "Shiftly",
              "version": 3,
              "userName": "Jeyzi",
              "currencySymbol": "$",
              "defaultHourlyRate": 15.0,
              "settings": {
                "savingsGoalTarget": 50.0
              },
              "shifts": [
                { "dateEpochDay": 20400, "timeInMillis": 1000, "timeOutMillis": 7201000, "durationMinutes": 120, "lockedHourlyRate": 15.0, "totalEarned": 30.0 },
                { "dateEpochDay": 20400, "timeInMillis": 1000, "timeOutMillis": 2000, "durationMinutes": 60, "lockedHourlyRate": 15.0, "totalEarned": -10.0 }
              ],
              "expenses": [
                { "title": "Lunch", "amount": 12.5, "category": "Food", "dateEpochDay": 20400 },
                { "title": "Invalid", "amount": -50.0, "category": "Food", "dateEpochDay": 20400 }
              ],
              "splitBills": [
                { "originalId": 1, "title": "Bad", "totalAmount": 200.0, "personCount": 1, "perPersonAmount": 200.0, "dateEpochDay": 20400 }
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decodeJson(json)
        assertEquals(50.0, decoded.settings?.savingsGoalTarget ?: 0.0, 0.001)
        assertEquals(1, decoded.shifts.size)
        assertEquals(1, decoded.expenses.size)
        assertEquals("Lunch", decoded.expenses.first().expense.title)
        assertEquals(0, decoded.splitBills.size)
    }

    @Test
    fun sanitizeReceiptFileName_andZipDecode_preventPathTraversal() {
        assertEquals("evil.jpg", BackupCodec.sanitizeReceiptFileName("../../evil.jpg"))
        assertEquals("photo.png", BackupCodec.sanitizeReceiptFileName("receipts\\nested\\photo.png"))
        assertNull(BackupCodec.sanitizeReceiptFileName(".."))
        assertNull(BackupCodec.sanitizeReceiptFileName("."))

        val jsonBytes = """{"app":"Shiftly","version":3}""".toByteArray(Charsets.UTF_8)
        val imageBytes = byteArrayOf(9, 8, 7)
        val zipBytes = ByteArrayOutputStream().use { baos ->
            ZipOutputStream(baos).use { zos ->
                zos.putNextEntry(ZipEntry("shiftly_backup.json"))
                zos.write(jsonBytes)
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("receipts/../../traversal.jpg"))
                zos.write(imageBytes)
                zos.closeEntry()

                zos.putNextEntry(ZipEntry("receipts/.."))
                zos.write(imageBytes)
                zos.closeEntry()
            }
            baos.toByteArray()
        }

        val decoded = BackupCodec.decodeZipOrJsonBytes(zipBytes)
        assertEquals(setOf("traversal.jpg"), decoded.receiptFiles.keys)
        assertArrayEquals(imageBytes, decoded.receiptFiles["traversal.jpg"])
    }
}
