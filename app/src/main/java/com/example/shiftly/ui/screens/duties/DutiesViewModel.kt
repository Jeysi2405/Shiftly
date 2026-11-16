package com.example.shiftly.ui.screens.duties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.repository.ShiftlyRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DailyEarningsBar(
    val date: LocalDate,
    val dayLabel: String,
    val earned: Double,
    val hours: Double,
    val fillFraction: Float,
    val isToday: Boolean
)

data class DutiesUiState(
    val activeTimer: ActiveTimerState = ActiveTimerState(),
    val defaultHourlyRate: Double = 0.0,
    val currencySymbol: String = "₱",
    val selectedDutyDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedCalendarDate: LocalDate? = null,
    val weeklyTotalEarnings: Double = 0.0,
    val weeklyTotalHours: Double = 0.0,
    val weeklyBars: List<DailyEarningsBar> = emptyList(),
    val loggedEpochDays: Set<Long> = emptySet(),
    val displayedShifts: List<ShiftEntity> = emptyList(),
    val totalAllShiftsCount: Int = 0
)

class DutiesViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val _selectedDutyDate = MutableStateFlow(LocalDate.now())
    private val _displayedMonth = MutableStateFlow(YearMonth.now())
    private val _selectedCalendarDate = MutableStateFlow<LocalDate?>(null)
    private var isCheckingOut = false

    val uiState: StateFlow<DutiesUiState> = combine(
        repository.activeTimerState,
        repository.defaultHourlyRate,
        repository.currencySymbol,
        repository.allShifts,
        _selectedDutyDate,
        _displayedMonth,
        _selectedCalendarDate
    ) { values ->
        val activeTimer = values[0] as ActiveTimerState
        val defaultRate = values[1] as Double
        val currency = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val allShifts = values[3] as List<ShiftEntity>
        val dutyDate = values[4] as LocalDate
        val month = values[5] as YearMonth
        val filterDate = values[6] as LocalDate?

        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val endOfWeek = startOfWeek.plusDays(6)

        val shiftsByDay = allShifts.groupBy { it.dateEpochDay }

        // Build Mon-Sun weekly chart data
        val rawDays = (0..6).map { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            val dayShifts = shiftsByDay[date.toEpochDay()].orEmpty()
            val earned = dayShifts.sumOf { it.totalEarned }
            val hours = dayShifts.sumOf { it.durationMinutes } / 60.0
            Triple(date, earned, hours)
        }

        val maxDailyEarned = rawDays.maxOfOrNull { it.second }?.takeIf { it > 0.0 } ?: 1.0
        val weeklyBars = rawDays.map { (date, earned, hours) ->
            DailyEarningsBar(
                date = date,
                dayLabel = date.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()).take(3),
                earned = earned,
                hours = hours,
                fillFraction = (earned / maxDailyEarned).toFloat().coerceIn(0f, 1f),
                isToday = date == today
            )
        }

        val weeklyTotalEarnings = rawDays.sumOf { it.second }
        val weeklyTotalHours = rawDays.sumOf { it.third }
        val loggedEpochDays = shiftsByDay.keys

        val filteredShifts = if (filterDate != null) {
            allShifts.filter { it.dateEpochDay == filterDate.toEpochDay() }
        } else {
            allShifts
        }

        DutiesUiState(
            activeTimer = activeTimer,
            defaultHourlyRate = defaultRate,
            currencySymbol = currency,
            selectedDutyDate = dutyDate,
            displayedMonth = month,
            selectedCalendarDate = filterDate,
            weeklyTotalEarnings = weeklyTotalEarnings,
            weeklyTotalHours = weeklyTotalHours,
            weeklyBars = weeklyBars,
            loggedEpochDays = loggedEpochDays,
            displayedShifts = filteredShifts,
            totalAllShiftsCount = allShifts.size
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = DutiesUiState()
    )

    fun setDutyDate(date: LocalDate) {
        _selectedDutyDate.value = date
    }

    fun startTimer() {
        val currentState = uiState.value
        if (currentState.activeTimer.isRunning) return

        viewModelScope.launch {
            repository.startShiftTimer(
                startMillis = System.currentTimeMillis(),
                dateEpochDay = currentState.selectedDutyDate.toEpochDay(),
                lockedRate = currentState.defaultHourlyRate
            )
        }
    }

    fun checkOutAndSave(overtimeNote: String) {
        val timer = uiState.value.activeTimer
        if (!timer.isRunning || isCheckingOut) return
        isCheckingOut = true

        val nowMillis = System.currentTimeMillis()
        val elapsedMillis = (nowMillis - timer.startTimeMillis).coerceAtLeast(0L)
        val durationMinutes = (elapsedMillis / 60_000L).toInt().coerceAtLeast(1)
        val totalEarned = (durationMinutes / 60.0) * timer.lockedRate

        viewModelScope.launch {
            try {
                repository.insertShift(
                    ShiftEntity(
                        dateEpochDay = timer.shiftDateEpochDay,
                        timeInMillis = timer.startTimeMillis,
                        timeOutMillis = nowMillis,
                        durationMinutes = durationMinutes,
                        lockedHourlyRate = timer.lockedRate,
                        totalEarned = totalEarned,
                        overtimeNote = overtimeNote.trim()
                    )
                )
                repository.stopShiftTimer()
            } finally {
                isCheckingOut = false
            }
        }
    }

    fun cancelActiveTimer() {
        viewModelScope.launch {
            repository.stopShiftTimer()
        }
    }

    fun logManualShift(
        date: LocalDate,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        overtimeNote: String
    ) {
        val zone = ZoneId.systemDefault()
        val startDateTime = date.atTime(LocalTime.of(startHour, startMinute))
        var endDateTime = date.atTime(LocalTime.of(endHour, endMinute))
        // Handle overnight shifts crossing midnight
        if (!endDateTime.isAfter(startDateTime)) {
            endDateTime = endDateTime.plusDays(1)
        }

        val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(zone).toInstant().toEpochMilli()
        val durationMinutes = ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(1)
        val lockedRate = uiState.value.defaultHourlyRate
        val totalEarned = (durationMinutes / 60.0) * lockedRate

        viewModelScope.launch {
            repository.insertShift(
                ShiftEntity(
                    dateEpochDay = date.toEpochDay(),
                    timeInMillis = startMillis,
                    timeOutMillis = endMillis,
                    durationMinutes = durationMinutes,
                    lockedHourlyRate = lockedRate,
                    totalEarned = totalEarned,
                    overtimeNote = overtimeNote.trim()
                )
            )
        }
    }

    fun updateOvertimeNote(shift: ShiftEntity, updatedNote: String) {
        viewModelScope.launch {
            repository.updateShift(shift.copy(overtimeNote = updatedNote.trim()))
        }
    }

    fun deleteShift(shift: ShiftEntity) {
        viewModelScope.launch {
            repository.deleteShift(shift)
        }
    }

    fun changeCalendarMonth(offsetMonths: Long) {
        _displayedMonth.value = _displayedMonth.value.plusMonths(offsetMonths)
    }

    fun toggleCalendarDateFilter(date: LocalDate) {
        _selectedCalendarDate.value = if (_selectedCalendarDate.value == date) null else date
    }

    fun clearCalendarFilter() {
        _selectedCalendarDate.value = null
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DutiesViewModel(repository) as T
        }
    }
}