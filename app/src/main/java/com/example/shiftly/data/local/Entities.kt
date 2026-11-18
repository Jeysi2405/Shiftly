package com.example.shiftly.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "shifts",
    indices = [Index(value = ["dateEpochDay"], unique = true)]
)
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val timeInMillis: Long,
    val timeOutMillis: Long,
    val durationMinutes: Int,
    val lockedHourlyRate: Double,
    val totalEarned: Double,
    val overtimeNote: String = "",
    val dayType: String = "ORDINARY"
)

@Entity(
    tableName = "expenses",
    indices = [Index(value = ["linkedRecurringBillId"])]
)
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dateEpochDay: Long,
    val receiptImagePath: String? = null,
    val linkedSplitBillId: Long? = null,
    val createdAtMillis: Long = 0L,
    val linkedSavingsDepositId: Long? = null,
    val linkedRecurringBillId: Long? = null,
    val recurringOccurrenceEpochDay: Long? = null
)

@Entity(tableName = "extra_incomes")
data class ExtraIncomeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val dateEpochDay: Long,
    val sourceRuleId: String? = null,
    val createdAtMillis: Long = 0L
)

@Entity(tableName = "recurring_bills")
data class RecurringBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String,
    val dueEpochDay: Long,
    val dueHour: Int = 9,
    val dueMinute: Int = 0,
    val isEnabled: Boolean = true,
    val lastExecutedEpochDay: Long = 0L,
    val lastExecutedAtMillis: Long = 0L,
    val createdAtMillis: Long = 0L
)

object AllowanceSource {
    const val SALARY = "Salary (from Duties / Income)"
    const val BUSINESS = "Business"
    const val OTHERS = "Others"

    val ALL = listOf(SALARY, BUSINESS, OTHERS)
}

@Entity(
    tableName = "allowance_entries",
    indices = [Index(value = ["cutoffKey"], unique = true)]
)
data class AllowanceEntryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val source: String,
    val dateEpochDay: Long,
    val cutoffKey: String? = null,
    val createdAtMillis: Long = 0L
)

@Entity(tableName = "split_bills")
data class SplitBillEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val totalAmount: Double,
    val personCount: Int,
    val perPersonAmount: Double,
    val dateEpochDay: Long,
    val isLoggedToExpense: Boolean = false
)

@Entity(
    tableName = "split_participants",
    foreignKeys = [
        ForeignKey(
            entity = SplitBillEntity::class,
            parentColumns = ["id"],
            childColumns = ["billId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["billId"])]
)
data class SplitParticipantEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val billId: Long,
    val personLabel: String,
    val amountOwed: Double,
    val hasPaid: Boolean = false,
    val isCurrentUser: Boolean = false
)

data class SplitBillWithParticipants(
    @Embedded val bill: SplitBillEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val participants: List<SplitParticipantEntity>
)

@Entity(tableName = "savings_goals")
data class SavingsGoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val targetAmount: Double,
    val targetDateEpochDay: Long,
    val isActive: Boolean = true,
    val createdAtMillis: Long = 0L
)

@Entity(
    tableName = "savings_deposits",
    foreignKeys = [
        ForeignKey(
            entity = SavingsGoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["goalId"])]
)
data class SavingsDepositEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val goalId: Long,
    val amount: Double,
    val timestampMillis: Long,
    val note: String = ""
)

data class SavingsGoalWithDeposits(
    @Embedded val goal: SavingsGoalEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "goalId"
    )
    val deposits: List<SavingsDepositEntity>
)
