package com.example.shiftly.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val dateEpochDay: Long,
    val timeInMillis: Long,
    val timeOutMillis: Long,
    val durationMinutes: Int,
    val lockedHourlyRate: Double,
    val totalEarned: Double,
    val overtimeNote: String = ""
)

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val amount: Double,
    val category: String,
    val dateEpochDay: Long,
    val receiptImagePath: String? = null,
    val linkedSplitBillId: Long? = null
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
    val hasPaid: Boolean = false
)

data class SplitBillWithParticipants(
    @Embedded val bill: SplitBillEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "billId"
    )
    val participants: List<SplitParticipantEntity>
)