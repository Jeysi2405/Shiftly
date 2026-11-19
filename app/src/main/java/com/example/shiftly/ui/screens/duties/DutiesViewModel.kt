package com.example.shiftly.ui.screens.duties

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.shiftly.data.local.ShiftEntity
import com.example.shiftly.data.local.ShiftTimePreset
import com.example.shiftly.data.local.ShiftlySettingsSnapshot
import com.example.shiftly.data.repository.ShiftlyRepository
import com.example.shiftly.utils.DayType
import com.example.shiftly.utils.WageCalculator
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class TimeBreakdown(
    val regularHours: Double = 0.0,
    val nightDiffHours: Double = 0.0,
    val overtimeHours: Double = 0.0
)

@Immutable
data class DutiesUiState(
    val defaultHourlyRate: Double = 0.0,
    val minimumWagePerDay: Double = 0.0,
    val currencySymbol: String = "₱",
    val shiftTimePresets: List<ShiftTimePreset> = ShiftlySettingsSnapshot.DEFAULT_SHIFT_TIME_PRESETS,
    val selectedDutyDate: LocalDate = LocalDate.now(),
    val displayedMonth: YearMonth = YearMonth.now(),
    val monthlyTotalEarnings: Double = 0.0,
    val monthlyTotalHours: Double = 0.0,
    val monthlyTimeBreakdown: TimeBreakdown = TimeBreakdown(),
    val loggedEpochDays: Set<Long> = emptySet(),
    val shiftsByEpochDay: Map<Long, ShiftEntity> = emptyMap(),
    val displayedShifts: List<ShiftEntity> = emptyList(),
    val totalAllShiftsCount: Int = 0
)

class DutiesViewModel(
    private val repository: ShiftlyRepository
) : ViewModel() {

    private val _selectedDutyDate = MutableStateFlow(LocalDate.now())
    private val _displayedMonth = MutableStateFlow(YearMonth.now())

    val uiState: StateFlow<DutiesUiState> = combine(
        repository.defaultHourlyRate,
        repository.minimumWagePerDay,
        repository.currencySymbol,
        repository.allShifts,
        repository.settingsSnapshot,
        _selectedDutyDate,
        _displayedMonth
    ) { values ->
        val defaultRate = values[0] as Double
        val minWage = values[1] as Double
        val currency = values[2] as String
        @Suppress("UNCHECKED_CAST")
        val allShifts = values[3] as List<ShiftEntity>
        val settings = values[4] as ShiftlySettingsSnapshot
        val dutyDate = values[5] as LocalDate
        val month = values[6] as YearMonth

        val shiftsByEpochDay = allShifts.associateBy { it.dateEpochDay }
        val monthShifts = allShifts.filter { shift ->
            YearMonth.from(LocalDate.ofEpochDay(shift.dateEpochDay)) == month
        }

        var monthTotalEarned = 0.0
        var monthTotalHours = 0.0
        var monthRegMins = 0L
        var monthNightMins = 0L
        var monthOtMins = 0L

        monthShifts.forEach { shift ->
            val details = WageCalculator.calculateTotalEarned(
                startMillis = shift.timeInMillis,
                endMillis = shift.timeOutMillis,
                hourlyRate = shift.lockedHourlyRate,
                dayTypeStr = shift.dayType
            )
            monthTotalEarned += shift.totalEarned
            monthTotalHours += shift.durationMinutes / 60.0
            monthRegMins += details.regularMinutes
            monthNightMins += details.nightDiffMinutes
            monthOtMins += details.overtimeMinutes
        }

        val monthlyTimeBreakdown = TimeBreakdown(
            regularHours = monthRegMins / 60.0,
            nightDiffHours = monthNightMins / 60.0,
            overtimeHours = monthOtMins / 60.0
        )

        DutiesUiState(
            defaultHourlyRate = defaultRate,
            minimumWagePerDay = minWage,
            currencySymbol = currency,
            shiftTimePresets = settings.shiftTimePresets,
            selectedDutyDate = dutyDate,
            displayedMonth = month,
            monthlyTotalEarnings = monthTotalEarned,
            monthlyTotalHours = monthTotalHours,
            monthlyTimeBreakdown = monthlyTimeBreakdown,
            loggedEpochDays = shiftsByEpochDay.keys,
            shiftsByEpochDay = shiftsByEpochDay,
            displayedShifts = monthShifts,
            totalAllShiftsCount = allShifts.size
        )
    }
        .flowOn(Dispatchers.IO)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DutiesUiState()
        )

    fun setDutyDate(date: LocalDate) {
        _selectedDutyDate.value = date
    }

    fun updateWageRates(hourlyRate: Double, minimumWagePerDay: Double) {
        viewModelScope.launch {
            if (hourlyRate.isFinite() && hourlyRate > 0.0) {
                repository.updateDefaultHourlyRate(hourlyRate)
            }
            if (minimumWagePerDay.isFinite() && minimumWagePerDay >= 0.0) {
                repository.updateMinimumWagePerDay(minimumWagePerDay)
            }
        }
    }

    fun logManualShift(
        date: LocalDate,
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        overtimeNote: String,
        dayType: DayType,
        onResult: (Boolean) -> Unit = {}
    ) {
        val current = uiState.value
        val epochDay = date.toEpochDay()
        if (current.shiftsByEpochDay.containsKey(epochDay)) {
            onResult(false)
            return
        }

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
            dayTypeStr = dayType.name
        ).totalEarned

        viewModelScope.launch {
            val insertedId = repository.insertShift(
                ShiftEntity(
                    dateEpochDay = epochDay,
                    timeInMillis = startMillis,
                    timeOutMillis = endMillis,
                    durationMinutes = durationMinutes,
                    lockedHourlyRate = lockedRate,
                    totalEarned = totalEarned,
                    overtimeNote = overtimeNote.trim(),
                    dayType = dayType.name
                )
            )
            onResult(insertedId > 0L)
        }
    }

    fun editShift(
        shift: ShiftEntity,
        date: LocalDate = LocalDate.ofEpochDay(shift.dateEpochDay),
        startHour: Int,
        startMinute: Int,
        endHour: Int,
        endMinute: Int,
        dayType: DayType,
        overtimeNote: String,
        onResult: (Boolean) -> Unit = {}
    ) {
        val epochDay = date.toEpochDay()
        val conflict = uiState.value.shiftsByEpochDay[epochDay]
        if (conflict != null && conflict.id != shift.id) {
            onResult(false)
            return
        }

        val zone = ZoneId.systemDefault()

        val startDateTime = date.atTime(LocalTime.of(startHour, startMinute))
        var endDateTime = date.atTime(LocalTime.of(endHour, endMinute))
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
            dayTypeStr = dayType.name
        ).totalEarned

        viewModelScope.launch {
            val updated = repository.updateShift(
                shift.copy(
                    dateEpochDay = epochDay,
                    timeInMillis = startMillis,
                    timeOutMillis = endMillis,
                    durationMinutes = durationMinutes,
                    totalEarned = totalEarned,
                    dayType = dayType.name,
                    overtimeNote = overtimeNote.trim()
                )
            )
            onResult(updated)
        }
    }

    fun deleteShift(shift: ShiftEntity) {
        viewModelScope.launch {
            repository.deleteShift(shift)
        }
    }

    fun saveShiftTimePreset(
        startHour24: Int,
        startMinute: Int,
        endHour24: Int,
        endMinute: Int
    ) {
        val currentPresets = uiState.value.shiftTimePresets
        val exists = currentPresets.any {
            it.startHour24 == startHour24 &&
                it.startMinute == startMinute &&
                it.endHour24 == endHour24 &&
                it.endMinute == endMinute
        }
        if (exists) return

        val label = "${formatCompact12h(startHour24, startMinute)} – ${formatCompact12h(endHour24, endMinute)}"
        val updated = currentPresets + ShiftTimePreset(
            id = "p_${System.currentTimeMillis()}",
            label = label,
            startHour24 = startHour24.coerceIn(0, 23),
            startMinute = startMinute.coerceIn(0, 59),
            endHour24 = endHour24.coerceIn(0, 23),
            endMinute = endMinute.coerceIn(0, 59)
        )
        viewModelScope.launch {
            repository.updateShiftTimePresets(updated)
        }
    }

    fun deleteShiftTimePreset(presetId: String) {
        val currentPresets = uiState.value.shiftTimePresets
        val updated = currentPresets.filterNot { it.id == presetId }
        viewModelScope.launch {
            repository.updateShiftTimePresets(updated)
        }
    }

    fun changeCalendarMonth(offsetMonths: Long) {
        _displayedMonth.value = _displayedMonth.value.plusMonths(offsetMonths)
    }

    private fun formatCompact12h(hour24: Int, minute: Int): String {
        val h24 = hour24.coerceIn(0, 23)
        val m = minute.coerceIn(0, 59)
        val isPm = h24 >= 12
        val h12 = when {
            h24 == 0 -> 12
            h24 > 12 -> h24 - 12
            else -> h24
        }
        val amPm = if (isPm) "PM" else "AM"
        return if (m == 0) "$h12 $amPm" else "$h12:${m.toString().padStart(2, '0')} $amPm"
    }

    class Factory(private val repository: ShiftlyRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return DutiesViewModel(repository) as T
        }
    }
}
