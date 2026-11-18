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

    @Query("SELECT * FROM shifts ORDER BY dateEpochDay DESC, timeInMillis DESC")
    suspend fun getAllShiftsOnce(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE dateEpochDay = :dateEpochDay LIMIT 1")
    suspend fun getShiftByDateOnce(dateEpochDay: Long): ShiftEntity?

    @Query("SELECT * FROM shifts WHERE dateEpochDay BETWEEN :startEpochDay AND :endEpochDay ORDER BY dateEpochDay ASC")
    fun getShiftsBetween(startEpochDay: Long, endEpochDay: Long): Flow<List<ShiftEntity>>

    @Query("SELECT COUNT(*) FROM shifts")
    fun getTotalShiftCount(): Flow<Int>

    @Query("SELECT SUM(durationMinutes) FROM shifts")
    fun getTotalMinutesWorked(): Flow<Int?>

    @Query("SELECT SUM(totalEarned) FROM shifts")
    fun getTotalLifetimeEarnings(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)

    @Query("DELETE FROM shifts")
    suspend fun deleteAllShifts()
}

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id DESC")
    fun getAllExpenses(): Flow<List<ExpenseEntity>>

    @Query("SELECT * FROM expenses ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAllExpensesOnce(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE linkedSplitBillId = :billId")
    suspend fun getExpensesByLinkedSplitBillIdOnce(billId: Long): List<ExpenseEntity>

    @Query("SELECT SUM(amount) FROM expenses")
    fun getTotalExpenseAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: ExpenseEntity): Long

    @Update
    suspend fun updateExpense(expense: ExpenseEntity)

    @Delete
    suspend fun deleteExpense(expense: ExpenseEntity)

    @Query("DELETE FROM expenses WHERE linkedSplitBillId = :billId")
    suspend fun deleteExpensesByLinkedSplitBillId(billId: Long)

    @Query("UPDATE expenses SET linkedSplitBillId = NULL WHERE linkedSplitBillId = :billId")
    suspend fun unlinkExpensesBySplitBillId(billId: Long)

    @Query("DELETE FROM expenses WHERE linkedSavingsDepositId = :depositId")
    suspend fun deleteExpensesByLinkedSavingsDepositId(depositId: Long)

    @Query("DELETE FROM expenses WHERE linkedSavingsDepositId IN (:depositIds)")
    suspend fun deleteExpensesByLinkedSavingsDepositIds(depositIds: List<Long>)

    @Query("DELETE FROM expenses")
    suspend fun deleteAllExpenses()
}

@Dao
interface ExtraIncomeDao {
    @Query("SELECT * FROM extra_incomes ORDER BY dateEpochDay DESC, id DESC")
    fun getAllExtraIncomes(): Flow<List<ExtraIncomeEntity>>

    @Query("SELECT * FROM extra_incomes ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAllExtraIncomesOnce(): List<ExtraIncomeEntity>

    @Query("SELECT SUM(amount) FROM extra_incomes")
    fun getTotalExtraIncomeAmount(): Flow<Double?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExtraIncome(income: ExtraIncomeEntity): Long

    @Update
    suspend fun updateExtraIncome(income: ExtraIncomeEntity)

    @Delete
    suspend fun deleteExtraIncome(income: ExtraIncomeEntity)

    @Query("DELETE FROM extra_incomes")
    suspend fun deleteAllExtraIncomes()
}

@Dao
interface SplitBillDao {
    @Transaction
    @Query("SELECT * FROM split_bills ORDER BY dateEpochDay DESC, id DESC")
    fun getAllBillsWithParticipants(): Flow<List<SplitBillWithParticipants>>

    @Transaction
    @Query("SELECT * FROM split_bills ORDER BY dateEpochDay DESC, id DESC")
    suspend fun getAllBillsWithParticipantsOnce(): List<SplitBillWithParticipants>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: SplitBillEntity): Long

    @Update
    suspend fun updateBill(bill: SplitBillEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertParticipants(participants: List<SplitParticipantEntity>)

    @Update
    suspend fun updateParticipant(participant: SplitParticipantEntity)

    @Query("DELETE FROM split_participants WHERE billId = :billId")
    suspend fun deleteParticipantsForBill(billId: Long)

    @Query("UPDATE split_participants SET hasPaid = :hasPaid WHERE id = :participantId")
    suspend fun updateParticipantPaidStatus(participantId: Long, hasPaid: Boolean)

    @Query("UPDATE split_bills SET isLoggedToExpense = :isLogged WHERE id = :billId")
    suspend fun setBillLoggedToExpense(billId: Long, isLogged: Boolean)

    @Delete
    suspend fun deleteBill(bill: SplitBillEntity)

    @Query("DELETE FROM split_bills")
    suspend fun deleteAllSplitBills()
}

@Dao
interface SavingsGoalDao {
    @Transaction
    @Query("SELECT * FROM savings_goals ORDER BY isActive DESC, createdAtMillis DESC, id DESC")
    fun getAllGoalsWithDeposits(): Flow<List<SavingsGoalWithDeposits>>

    @Transaction
    @Query("SELECT * FROM savings_goals ORDER BY isActive DESC, createdAtMillis DESC, id DESC")
    suspend fun getAllGoalsWithDepositsOnce(): List<SavingsGoalWithDeposits>

    @Query("SELECT * FROM savings_goals WHERE id = :goalId LIMIT 1")
    suspend fun getGoalByIdOnce(goalId: Long): SavingsGoalEntity?

    @Query("SELECT * FROM savings_deposits WHERE goalId = :goalId ORDER BY timestampMillis DESC, id DESC")
    suspend fun getDepositsForGoalOnce(goalId: Long): List<SavingsDepositEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: SavingsGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: SavingsGoalEntity)

    @Query("UPDATE savings_goals SET isActive = (id = :activeGoalId)")
    suspend fun setActiveGoal(activeGoalId: Long)

    @Query("UPDATE savings_goals SET isActive = 0")
    suspend fun clearAllActiveFlags()

    @Delete
    suspend fun deleteGoal(goal: SavingsGoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeposit(deposit: SavingsDepositEntity): Long

    @Update
    suspend fun updateDeposit(deposit: SavingsDepositEntity)

    @Delete
    suspend fun deleteDeposit(deposit: SavingsDepositEntity)

    @Query("DELETE FROM savings_deposits WHERE id = :depositId")
    suspend fun deleteDepositById(depositId: Long)

    @Query("DELETE FROM savings_goals")
    suspend fun deleteAllSavingsGoals()
}
