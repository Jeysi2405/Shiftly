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
interface InstallmentLoanDao {
    @Query("SELECT * FROM installment_loans ORDER BY id DESC")
    fun getAllInstallmentLoans(): Flow<List<InstallmentLoanEntity>>

    @Query("SELECT * FROM installment_loans ORDER BY id DESC")
    suspend fun getAllInstallmentLoansOnce(): List<InstallmentLoanEntity>

    @Transaction
    @Query("SELECT * FROM installment_loans ORDER BY id DESC")
    fun getAllInstallmentLoansWithPayments(): Flow<List<InstallmentLoanWithPayments>>

    @Transaction
    @Query("SELECT * FROM installment_loans ORDER BY id DESC")
    suspend fun getAllInstallmentLoansWithPaymentsOnce(): List<InstallmentLoanWithPayments>

    @Query("SELECT * FROM installment_loans WHERE id = :loanId LIMIT 1")
    suspend fun getLoanByIdOnce(loanId: Long): InstallmentLoanEntity?

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY monthNumber ASC, id ASC")
    fun getPaymentsForLoan(loanId: Long): Flow<List<LoanPaymentEntity>>

    @Query("SELECT * FROM loan_payments WHERE loanId = :loanId ORDER BY monthNumber ASC, id ASC")
    suspend fun getPaymentsForLoanOnce(loanId: Long): List<LoanPaymentEntity>

    // ABORT (not REPLACE): replacing a parent row would cascade-delete its payment history.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertInstallmentLoan(loan: InstallmentLoanEntity): Long

    @Update
    suspend fun updateInstallmentLoan(loan: InstallmentLoanEntity)

    @Delete
    suspend fun deleteInstallmentLoan(loan: InstallmentLoanEntity)

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertLoanPayment(payment: LoanPaymentEntity): Long

    @Update
    suspend fun updateLoanPayment(payment: LoanPaymentEntity)

    @Query("DELETE FROM loan_payments WHERE loanId = :loanId")
    suspend fun deletePaymentsForLoan(loanId: Long)

    @Query("DELETE FROM loan_payments")
    suspend fun deleteAllLoanPayments()

    @Query("DELETE FROM installment_loans")
    suspend fun deleteAllInstallmentLoans()
}
