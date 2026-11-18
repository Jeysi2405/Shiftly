package com.example.shiftly.ui.screens.splitbill

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.SplitBillEntity
import com.example.shiftly.data.local.SplitBillWithParticipants
import com.example.shiftly.data.local.SplitParticipantEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.SplitBillMath
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SplitBillUiState(
    val currencySymbol: String = "₱",
    val selectedMonth: YearMonth? = YearMonth.now(),
    val billsWithParticipants: List<SplitBillWithParticipants> = emptyList(),
    val totalBillsCount: Int = 0,
    val hasAnyBillsAtAll: Boolean = false,
    val totalCollectedAmount: Double = 0.0,
    val totalPendingAmount: Double = 0.0,
    val paidParticipantsCount: Int = 0,
    val totalParticipantsCount: Int = 0
)

class SplitBillViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    companion object {
        fun filterBillsByMonth(
            bills: List<SplitBillWithParticipants>,
            selectedMonth: YearMonth?
        ): List<SplitBillWithParticipants> {
            if (selectedMonth == null) return bills
            val startEpoch = selectedMonth.atDay(1).toEpochDay()
            val endEpoch = selectedMonth.atEndOfMonth().toEpochDay()
            return bills.filter { it.bill.dateEpochDay in startEpoch..endEpoch }
        }
    }

    private val _selectedMonth = MutableStateFlow<YearMonth?>(YearMonth.now())

    val uiState: StateFlow<SplitBillUiState> = combine(
        repository.currencySymbol,
        repository.allSplitBills,
        _selectedMonth
    ) { currency, allBills, selectedMonth ->
        val filteredBills = filterBillsByMonth(allBills, selectedMonth)
        val friendSummary = SplitBillMath.summarizeFriendsOwed(filteredBills)

        SplitBillUiState(
            currencySymbol = currency,
            selectedMonth = selectedMonth,
            billsWithParticipants = filteredBills,
            totalBillsCount = filteredBills.size,
            hasAnyBillsAtAll = allBills.isNotEmpty(),
            totalCollectedAmount = friendSummary.collectedFriendsAmount,
            totalPendingAmount = friendSummary.pendingFriendsAmount,
            paidParticipantsCount = friendSummary.paidFriendsCount,
            totalParticipantsCount = friendSummary.totalFriendsCount
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SplitBillUiState()
    )

    fun shiftMonth(delta: Long) {
        val base = _selectedMonth.value ?: YearMonth.now()
        _selectedMonth.value = base.plusMonths(delta)
    }

    fun toggleAllMonths() {
        _selectedMonth.value = if (_selectedMonth.value == null) YearMonth.now() else null
    }

    fun showAllMonths() {
        _selectedMonth.value = null
    }

    fun createSplitBill(
        rawTitle: String,
        totalAmount: Double,
        personCount: Int,
        participantNames: List<String>,
        participantPaidStates: List<Boolean> = emptyList(),
        alsoLogMyShareAsExpense: Boolean
    ) {
        if (totalAmount <= 0.0 || personCount < 2) return

        val cleanTitle = rawTitle.trim().ifBlank { "Group Bill" }
        val shares = SplitBillMath.distributeEqualShares(totalAmount, personCount)
        val perPerson = shares.firstOrNull() ?: 0.0
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
            val isSelf = index == 0
            val isPaid = participantPaidStates.getOrElse(index) { isSelf }
            SplitParticipantEntity(
                billId = 0L,
                personLabel = label,
                amountOwed = shares.getOrElse(index) { perPerson },
                hasPaid = isPaid,
                isCurrentUser = isSelf
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

    fun deleteSplitBill(bill: SplitBillEntity, deleteLinkedExpense: Boolean = false) {
        viewModelScope.launch {
            repository.deleteSplitBill(bill, deleteLinkedExpense)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SplitBillViewModel(repository) as T
        }
    }
}
