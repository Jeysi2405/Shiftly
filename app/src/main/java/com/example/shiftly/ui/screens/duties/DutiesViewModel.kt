package com.example.shiftly.ui.screens.duties

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ActiveTimerState
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.WageCalculator
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class TimeBreakdown(
    val regularHours: Double = 0.0,
    val nightDiffHours: Double = 0.0,
    val overtimeHours: Double = 0.0
)

data class DutiesUiState(
    val activeTimer: ActiveTimerState = ActiveTimerState(),
    val defaultHourlyRate: Double = 0.0,
    val currencySymbol: String = "₱",
    val nightDiffStartHour: Int = 22,
    val nightDiffMultiplier: Double = 1.10,
    val overtimeMultiplier: Double = 1.25,
    val selectedDutyDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val selectedCalendarDate: LocalDate? = null,
    val weeklyTotalEarnings: Double = 0.0,
    val weeklyTotalHours: Double = 0.0,
    val weeklyTimeBreakdown: TimeBreakdown = TimeBreakdown(),
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
        repository.settingsSnapshot,
        _selectedDutyDate,
        _displayedMonth,
        _selectedCalendarDate
    ) { values ->
        val activeTimer = values[0] as ActiveTimerState
        val defaultRate = values[1] as Double
        val currency = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val allShifts = values[3] as List<ShiftEntity>
        val settings = values[4] as ShiftlySettingsSnapshot
        val dutyDate = values[5] as LocalDate
        val month = values[6] as YearMonth
        val filterDate = values[7] as LocalDate?

        val today = LocalDate.now()
        val startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val shiftsByDay = allShifts.groupBy { it.dateEpochDay }

        var weekTotalEarned = 0.0
        var weekTotalHours = 0.0
        var weekRegMins = 0L
        var weekNightMins = 0L
        var weekOtMins = 0L

        (0..6).forEach { offset ->
            val date = startOfWeek.plusDays(offset.toLong())
            val dayShifts = shiftsByDay[date.toEpochDay()].orEmpty()

            dayShifts.forEach { shift ->
                val details = WageCalculator.calculateTotalEarned(
                    startMillis = shift.timeInMillis,
                    endMillis = shift.timeOutMillis,
                    hourlyRate = shift.lockedHourlyRate,
                    dayTypeStr = shift.dayType,
                    nightDiffStartHour = settings.nightDiffStartHour,
                    nightDiffMultiplier = settings.nightDiffMultiplier,
                    ordinaryOvertimeMultiplier = settings.overtimeMultiplier
                )
                weekTotalEarned += shift.totalEarned
                weekTotalHours += shift.durationMinutes / 60.0
                weekRegMins += details.regularMinutes
                weekNightMins += details.nightDiffMinutes
                weekOtMins += details.overtimeMinutes
            }
        }

        val weeklyTimeBreakdown = TimeBreakdown(
            regularHours = weekRegMins / 60.0,
            nightDiffHours = weekNightMins / 60.0,
            overtimeHours = weekOtMins / 60.0
        )

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
            nightDiffStartHour = settings.nightDiffStartHour,
            nightDiffMultiplier = settings.nightDiffMultiplier,
            overtimeMultiplier = settings.overtimeMultiplier,
            selectedDutyDate = dutyDate,
            displayedMonth = month,
            selectedCalendarDate = filterDate,
            weeklyTotalEarnings = weekTotalEarned,
            weeklyTotalHours = weekTotalHours,
            weeklyTimeBreakdown = weeklyTimeBreakdown,
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

    fun checkOutAndSave(overtimeNote: String, dayType: DayType) {
        val current = uiState.value
        val timer = current.activeTimer
        if (!timer.isRunning || isCheckingOut) return
        isCheckingOut = true

        val nowMillis = System.currentTimeMillis()
        val elapsedMillis = (nowMillis - timer.startTimeMillis).coerceAtLeast(0L)
        val durationMinutes = (elapsedMillis / 60_000L).toInt().coerceAtLeast(1)

        val totalEarned = WageCalculator.calculateTotalEarned(
            startMillis = timer.startTimeMillis,
            endMillis = nowMillis,
            hourlyRate = timer.lockedRate,
            dayTypeStr = dayType.name,
            nightDiffStartHour = current.nightDiffStartHour,
            nightDiffMultiplier = current.nightDiffMultiplier,
            ordinaryOvertimeMultiplier = current.overtimeMultiplier
        ).totalEarned

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
                        overtimeNote = overtimeNote.trim(),
                        dayType = dayType.name
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
        overtimeNote: String,
        dayType: DayType
    ) {
        val current = uiState.value
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
        val lockedRate = current.defaultHourlyRate

        val totalEarned = WageCalculator.calculateTotalEarned(
            startMillis = startMillis,
            endMillis = endMillis,
            hourlyRate = lockedRate,
            dayTypeStr = dayType.name,
            nightDiffStartHour = current.nightDiffStartHour,
            nightDiffMultiplier = current.nightDiffMultiplier,
            ordinaryOvertimeMultiplier = current.overtimeMultiplier
        ).totalEarned

        viewModelScope.launch {
            repository.insertShift(
                ShiftEntity(
                    dateEpochDay = date.toEpochDay(),
                    timeInMillis = startMillis,
                    timeOutMillis = endMillis,
                    durationMinutes = durationMinutes,
                    lockedHourlyRate = lockedRate,
                    totalEarned = totalEarned,
                    overtimeNote = overtimeNote.trim(),
                    dayType = dayType.name
                )
            )
        }
    }

    fun editShift(
        shift: ShiftEntity,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        dayType: DayType,
        overtimeNote: String
    ) {
        val current = uiState.value
        val zone = ZoneId.systemDefault()
        val shiftDate = LocalDate.ofEpochDay(shift.dateEpochDay)

        val startDateTime = shiftDate.atTime(LocalTime.of(startHour, startMinute))
        var endDateTime = shiftDate.atTime(LocalTime.of(endHour, endMinute))
        if (!endDateTime.isAfter(startDateTime)) {
            endDateTime = endDateTime.plusDays(1)
        }

        val startMillis = startDateTime.atZone(zone).toInstant().toEpochMilli()
        val endMillis = endDateTime.atZone(zone).toInstant().toEpochMilli()
        val durationMinutes = ((endMillis - startMillis) / 60_000L).toInt().coerceAtLeast(1)

        val totalEarned = WageCalculator.calculateTotalEarned(
            startMillis = startMillis,
            endMillis = endMillis,
            hourlyRate = shift.lockedHourlyRate,
            dayTypeStr = dayType.name,
            nightDiffStartHour = current.nightDiffStartHour,
            nightDiffMultiplier = current.nightDiffMultiplier,
            ordinaryOvertimeMultiplier = current.overtimeMultiplier
        ).totalEarned

        viewModelScope.launch {
            repository.updateShift(
                shift.copy(
                    timeInMillis = startMillis,
                    timeOutMillis = endMillis,
                    durationMinutes = durationMinutes,
                    totalEarned = totalEarned,
                    dayType = dayType.name,
                    overtimeNote = overtimeNote.trim()
                )
            )
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
        if (_selectedCalendarDate.value == date) {
            _selectedCalendarDate.value = null
        } else {
            _selectedCalendarDate.value = date
            _selectedDutyDate.value = date
        }
    }

    fun clearCalendarFilter() {
        _selectedCalendarDate.value = null
    }

    fun saveDefaultRate(rateText: String) {
        val parsed = rateText.toDoubleOrNull()
        if (parsed == null || !parsed.isFinite() || parsed <= 0.0) return
        viewModelScope.launch {
            repository.updateDefaultHourlyRate(parsed)
        }
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DutiesViewModel(repository) as T
        }
    }
}
