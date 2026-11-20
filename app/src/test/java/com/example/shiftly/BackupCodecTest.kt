package com.example.shiftly

import com.example.shiftly.data.backup.BackupCodec
import com.example.shiftly.data.backup.BackupExpenseRecord
import com.example.shiftly.data.backup.BackupInstallmentLoan
import com.example.shiftly.data.backup.BackupPayload
import com.example.shiftly.data.backup.BackupSavingsGoal
import com.example.shiftly.data.backup.BackupSplitBill
import com.example.shiftly.data.local.AllowanceEntryEntity
import com.example.shiftly.data.local.AllowanceSource
import com.example.shiftly.data.local.CategoryBudgetSetting
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ExpenseEntity
import com.example.shiftly.data.local.ExtraIncomeEntity
import com.example.shiftly.data.local.InstallmentLoanEntity
import com.example.shiftly.data.local.LoanPaymentEntity
import com.example.shiftly.data.local.ManagedCategorySetting
import com.example.shiftly.data.local.RecurringBillEntity
import com.example.shiftly.data.local.RecurringEntrySetting
import com.example.shiftly.data.local.SavingsDepositEntity
import com.example.shiftly.data.local.SavingsGoalEntity
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.local.toUtcMillis
import com.example.shiftly.utils.WageCalculator
import java.io.ByteArrayOutputStream
import java.time.LocalDate
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
        val loanStartMillis = LocalDate.of(2026, 1, 15).toUtcMillis()
        val loanFirstDueMillis = LocalDate.of(2026, 2, 15).toUtcMillis()
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
                ),
                cutoffMode = CutoffScheduleConfig.MODE_MONTHLY,
                cutoffDay1 = 19,
                cutoffDay2 = 31,
                cutoffWeeklyDay = 5,
                legacyRecurringMigrated = true
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
            recurringBills = listOf(
                RecurringBillEntity(
                    id = 12L,
                    title = "Dorm Wi-Fi",
                    amount = 1299.0,
                    category = "Bills",
                    frequency = "Monthly",
                    dueEpochDay = 20415L,
                    dueHour = 14,
                    dueMinute = 30,
                    isEnabled = true,
                    lastExecutedEpochDay = 20385L,
                    lastExecutedAtMillis = 1_763_000_100_000L,
                    createdAtMillis = 1_763_000_000_000L
                )
            ),
            allowanceEntries = listOf(
                AllowanceEntryEntity(
                    id = 1L,
                    title = "Salary Payout (1st Cutoff)",
                    amount = 469.33,
                    source = AllowanceSource.SALARY,
                    dateEpochDay = 20406L,
                    cutoffKey = "SEMI_2025-11-15",
                    createdAtMillis = 1_763_000_500_000L
                ),
                AllowanceEntryEntity(
                    id = 2L,
                    title = "Freelance Logo",
                    amount = 2500.0,
                    source = AllowanceSource.BUSINESS,
                    dateEpochDay = 20408L,
                    cutoffKey = null,
                    createdAtMillis = 1_763_000_600_000L
                )
            ),
            installmentLoans = listOf(
                BackupInstallmentLoan(
                    originalId = 5L,
                    loan = InstallmentLoanEntity(
                        id = 5L,
                        gadgetName = "MacBook Air M3",
                        cashPrice = 55000.0,
                        downpayment = 5000.0,
                        installmentPerMonth = 4500.0,
                        termsInMonths = 12,
                        paidMonths = 2,
                        startDateMillis = loanStartMillis,
                        monthlyDueDay = 15,
                        firstDueDateMillis = loanFirstDueMillis
                    ),
                    payments = listOf(
                        LoanPaymentEntity(
                            id = 11L,
                            loanId = 5L,
                            monthNumber = 1,
                            amountPaid = 4500.0,
                            datePaidMillis = 1_771_113_600_000L
                        ),
                        LoanPaymentEntity(
                            id = 12L,
                            loanId = 5L,
                            monthNumber = 2,
                            amountPaid = 4500.0,
                            datePaidMillis = 1_773_532_800_000L
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
        assertEquals(CutoffScheduleConfig.MODE_MONTHLY, decoded.settings?.cutoffMode)
        assertEquals(19, decoded.settings?.cutoffDay1)
        assertTrue(decoded.settings?.legacyRecurringMigrated == true)
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
        assertEquals(1, decoded.recurringBills.size)
        assertEquals("Dorm Wi-Fi", decoded.recurringBills.first().title)
        assertEquals(14, decoded.recurringBills.first().dueHour)
        assertEquals(30, decoded.recurringBills.first().dueMinute)
        assertEquals(2, decoded.allowanceEntries.size)
        assertEquals("SEMI_2025-11-15", decoded.allowanceEntries[0].cutoffKey)
        assertEquals(AllowanceSource.BUSINESS, decoded.allowanceEntries[1].source)
        assertNull(decoded.allowanceEntries[1].cutoffKey)
        assertEquals(1, decoded.installmentLoans.size)
        val decodedLoan = decoded.installmentLoans[0]
        assertEquals(5L, decodedLoan.originalId)
        assertEquals("MacBook Air M3", decodedLoan.loan.gadgetName)
        assertEquals(55000.0, decodedLoan.loan.cashPrice, 0.001)
        assertEquals(5000.0, decodedLoan.loan.downpayment, 0.001)
        assertEquals(4500.0, decodedLoan.loan.installmentPerMonth, 0.001)
        assertEquals(12, decodedLoan.loan.termsInMonths)
        assertEquals(2, decodedLoan.loan.paidMonths)
        assertEquals(loanStartMillis, decodedLoan.loan.startDateMillis)
        assertEquals(15, decodedLoan.loan.monthlyDueDay)
        assertEquals(loanFirstDueMillis, decodedLoan.loan.firstDueDateMillis)
        assertEquals(2, decodedLoan.payments.size)
        assertEquals(1, decodedLoan.payments[0].monthNumber)
        assertEquals(4500.0, decodedLoan.payments[0].amountPaid, 0.001)
        assertEquals(2, decodedLoan.payments[1].monthNumber)
        assertArrayEquals(receiptBytes, decoded.receiptFiles["receipt_dinner.jpg"])
    }

    @Test
    fun legacyInstallmentLoansJson_isConvertedIntoRoomLoanContracts() {
        val legacyJson = """
            {
              "app": "Shiftly",
              "version": 6,
              "installmentLoans": [
                {
                  "id": "loan-uuid-1",
                  "itemName": "Laptop",
                  "lenderOrStore": "Home Credit",
                  "installmentAmount": 2500.0,
                  "totalInstallments": 12,
                  "paidInstallments": 3,
                  "downPayment": 5000.0,
                  "frequency": "Monthly",
                  "dueDayLabel": "Every 15th",
                  "startDateMillis": 1763000000000,
                  "autoLogExpense": true
                }
              ]
            }
        """.trimIndent()

        val decoded = BackupCodec.decodeJson(legacyJson)
        assertEquals(1, decoded.installmentLoans.size)
        val loan = decoded.installmentLoans.first().loan
        assertEquals("Laptop (Home Credit)", loan.gadgetName)
        assertEquals(2500.0, loan.installmentPerMonth, 0.001)
        assertEquals(12, loan.termsInMonths)
        assertEquals(3, loan.paidMonths)
        assertEquals(5000.0, loan.downpayment, 0.001)
        assertEquals(15, loan.monthlyDueDay)
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
        assertEquals(0, decoded.installmentLoans.size)

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
    fun csvExport_escapesRfc4180FieldsAndExcludesAutoCreditedAllowances() {
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
            ),
            allowanceEntries = listOf(
                AllowanceEntryEntity(
                    id = 1L,
                    title = "Auto Cutoff Salary",
                    amount = 200.5,
                    source = AllowanceSource.SALARY,
                    dateEpochDay = 20406L,
                    cutoffKey = "SEMI_2025-11-15"
                ),
                AllowanceEntryEntity(
                    id = 2L,
                    title = "Manual Business Side",
                    amount = 750.0,
                    source = AllowanceSource.BUSINESS,
                    dateEpochDay = 20407L,
                    cutoffKey = null
                )
            )
        )

        assertTrue(csv.contains("Type,Date,Title_Or_Note,Category_Or_DayType,Duration_Min,${BackupCodec.escapeCsvField("Amount (₱)")}"))
        assertTrue(csv.contains("\"Cafe, \"\"Morning\"\"\nSnack\""))
        assertTrue(csv.contains("\"Late, \"\"Desk\"\"\nNote\""))
        assertTrue(csv.contains("\"Bonus, \"\"Q4\"\"\""))
        assertTrue(csv.contains("ExtraIncome,"))
        assertTrue(csv.contains("Allowance,"))
        assertTrue(csv.contains("Manual Business Side"))
        assertFalse(csv.contains("Auto Cutoff Salary"))
        assertTrue(csv.contains("125.75"))
        assertTrue(csv.contains("1500.00"))
        assertTrue(csv.contains("750.00"))
    }

    @Test
    fun shiftsAndExpensesCsvExports_formatHeadersAndEscapeFields() {
        val shiftsCsv = BackupCodec.buildShiftsCsvExport(
            currencySymbol = "₱",
            shifts = listOf(
                ShiftEntity(
                    id = 1L,
                    dateEpochDay = 20400L,
                    timeInMillis = 1000L,
                    timeOutMillis = 7201000L,
                    durationMinutes = 120,
                    lockedHourlyRate = 96.38,
                    totalEarned = 192.76,
                    overtimeNote = "Campus, \"Cafe\"",
                    dayType = "ORDINARY"
                )
            )
        )
        assertTrue(shiftsCsv.contains("Date,DayType,Duration_Min,Hourly_Rate(₱),Earned (₱),Note"))
        assertTrue(shiftsCsv.contains("ORDINARY,120,96.38,192.76,\"Campus, \"\"Cafe\"\"\""))

        val expensesCsv = BackupCodec.buildExpensesCsvExport(
            currencySymbol = "₱",
            expenses = listOf(
                ExpenseEntity(
                    id = 2L,
                    title = "Lunch, \"Special\"",
                    amount = 145.5,
                    category = "Food",
                    dateEpochDay = 20400L
                )
            )
        )
        assertTrue(expensesCsv.contains("Date,Title,Category,Amount (₱)"))
        assertTrue(expensesCsv.contains("\"Lunch, \"\"Special\"\"\",Food,145.50"))
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
