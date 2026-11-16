package com.example.shiftly.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {
    @Query("SELECT * FROM shifts ORDER BY dateEpochDay DESC, timeInMillis DESC")
    fun getAllShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    fun getShiftsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<ShiftEntity>>

    @Query("SELECT COUNT(*) FROM shifts")
    fun getTotalShiftCount(): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM shifts")
    fun getTotalMinutesWorked(): Flow<Int?>

    @Query("SELECT SUM(totalEarned) FROM shifts")
    fun getTotalLifetimeEarnings(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenseAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)
}

@Dao
interface SplitBillDao {
    @Transaction
    @Query("SELECT * FROM split_bills ORDER BY dateEpochDay DESC, id DESC")
    fun getAllBillsWithParticipants(): Flow<List<SplitBillWithParticipants>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: SplitBillEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<SplitParticipantEntity>)

    @Query("UPDATE split_participants SET hasPaid = :hasPaid WHERE id = :participantId")
    suspend fun updateParticipantPaidStatus(participantId: Long, hasPaid: Boolean)

    @Query("UPDATE split_bills SET isLoggedToExpense = 1 WHERE id = :billId")
    suspend fun markBillLoggedToExpense(billId: Long)

    @Delete
    suspend fun deleteBill(bill: SplitBillEntity)
}