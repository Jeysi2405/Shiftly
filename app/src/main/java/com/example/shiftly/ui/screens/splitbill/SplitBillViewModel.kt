package com.example.shiftly.ui.screens.splitbill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SplitBillUiState(
    val currencySymbol: String = "₱",
    val billsWithParticipants: List<SplitBillWithParticipants> = emptyList(),
    val totalBillsCount: Int = 0,
    val totalCollectedAmount: Double = 0.0,
    val totalPendingAmount: Double = 0.0,
    val paidParticipantsCount: Int = 0,
    val totalParticipantsCount: Int = 0
)

class SplitBillViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    val uiState: StateFlow<SplitBillUiState> = combine(
        repository.currencySymbol,
        repository.allSplitBills
    ) { currency, bills ->
        val allParticipants = bills.flatMap { it.participants }
        val collected = allParticipants.filter { it.hasPaid }.sumOf { it.amountOwed }
        val pending = allParticipants.filter { !it.hasPaid }.sumOf { it.amountOwed }
        val paidCount = allParticipants.count { it.hasPaid }

        SplitBillUiState(
            currencySymbol = currency,
            billsWithParticipants = bills,
            totalBillsCount = bills.size,
            totalCollectedAmount = collected,
            totalPendingAmount = pending,
            paidParticipantsCount = paidCount,
            totalParticipantsCount = allParticipants.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SplitBillUiState()
    )

    fun createSplitBill(
        rawTitle: String,
        totalAmount: Double,
        personCount: Int,
        participantNames: List<String>,
        alsoLogMyShareAsExpense: Boolean
    ) {
        if (totalAmount <= 0.0 || personCount < 2) return

        val cleanTitle = rawTitle.trim().ifBlank { "Group Bill" }
        val perPerson = totalAmount / personCount
        val todayEpoch = LocalDate.now().toEpochDay()

        val billEntity = SplitBillEntity(
            title = cleanTitle,
            totalAmount = totalAmount,
            personCount = personCount,
            perPersonAmount = perPerson,
            dateEpochDay = todayEpoch,
            isLoggedToExpense = false
        )

        val participants = (0 until personCount).map { index ->
            val fallbackName = if (index == 0) "You" else "Person ${index + 1}"
            val label = participantNames.getOrNull(index)?.trim().orEmpty().ifBlank { fallbackName }
            SplitParticipantEntity(
                billId = 0L,
                personLabel = label,
                amountOwed = perPerson,
                hasPaid = false
            )
        }

        viewModelScope.launch {
            val insertedBillId = repository.createSplitBill(billEntity, participants)
            if (alsoLogMyShareAsExpense) {
                repository.logSplitShareAsExpense(billEntity.copy(id = insertedBillId))
            }
        }
    }

    fun toggleParticipantPaid(participant: SplitParticipantEntity) {
        viewModelScope.launch {
            repository.toggleParticipantPaid(
                participantId = participant.id,
                hasPaid = !participant.hasPaid
            )
        }
    }

    fun logBillShareAsExpense(bill: SplitBillEntity) {
        if (bill.isLoggedToExpense) return
        viewModelScope.launch {
            repository.logSplitShareAsExpense(bill)
        }
    }

    fun deleteSplitBill(bill: SplitBillEntity) {
        viewModelScope.launch {
            repository.deleteSplitBill(bill)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SplitBillViewModel(repository) as T
        }
    }
}