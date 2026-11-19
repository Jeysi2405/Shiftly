package com.example.shiftly.utils

import androidx.compose.runtime.Immutable
import com.example.shiftly.data.local.CutoffScheduleConfig
import com.example.shiftly.data.local.ShiftEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Immutable
data class CutoffWindow(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val isFirstCutoff: Boolean,
    val label: String,
    val key: String = "SEMI_$endDate"
)

@Immutable
data class CutoffShiftSummary(
    val window: CutoffWindow,
    val totalEarnings: Double,
    val shiftDaysCount: Int,
    val totalHours: Double
)

object CutoffCalculator {

    private fun formatWindowRange(
        startDate: LocalDate,
        endDate: LocalDate,
        locale: Locale
    ): String {
        return if (startDate.year == endDate.year && startDate.month == endDate.month) {
            val monthShort = endDate.month.getDisplayName(TextStyle.SHORT, locale)
            "$monthShort ${startDate.dayOfMonth}–${endDate.dayOfMonth}"
        } else {
            "${formatShortDate(startDate, locale)}–${formatShortDate(endDate, locale)}"
        }
    }

    /**
     * Resolves the cutoff window containing [date] under the given [config].
     * Defaults to standard semi-monthly (1st–15th, 16th–End of Month).
     */
    fun resolveCutoffWindow(
        date: LocalDate,
        config: CutoffScheduleConfig = CutoffScheduleConfig(),
        locale: Locale = Locale.ENGLISH
    ): CutoffWindow {
        val norm = config.normalized()
        return when (norm.mode) {
            CutoffScheduleConfig.MODE_WEEKLY -> {
                val targetDow = DayOfWeek.of(norm.weeklyDayOfWeek)
                val endDate = date.with(TemporalAdjusters.nextOrSame(targetDow))
                val startDate = endDate.minusDays(6)
                CutoffWindow(
                    startDate = startDate,
                    endDate = endDate,
                    isFirstCutoff = true,
                    label = "Weekly • ${formatWindowRange(startDate, endDate, locale)}",
                    key = "WEEKLY_$endDate"
                )
            }

            CutoffScheduleConfig.MODE_MONTHLY -> {
                val ym = YearMonth.from(date)
                val thisMonthAnchor = ym.atDay(norm.day1.coerceAtMost(ym.lengthOfMonth()))
                val (startDate, endDate) = if (!date.isAfter(thisMonthAnchor)) {
                    val prevYm = ym.minusMonths(1)
                    val prevAnchor = prevYm.atDay(norm.day1.coerceAtMost(prevYm.lengthOfMonth()))
                    prevAnchor.plusDays(1) to thisMonthAnchor
                } else {
                    val nextYm = ym.plusMonths(1)
                    val nextAnchor = nextYm.atDay(norm.day1.coerceAtMost(nextYm.lengthOfMonth()))
                    thisMonthAnchor.plusDays(1) to nextAnchor
                }
                CutoffWindow(
                    startDate = startDate,
                    endDate = endDate,
                    isFirstCutoff = true,
                    label = "Monthly • ${formatWindowRange(startDate, endDate, locale)}",
                    key = "MONTHLY_$endDate"
                )
            }

            else -> {
                val ym = YearMonth.from(date)
                val anchor1 = ym.atDay(norm.day1.coerceAtMost(ym.lengthOfMonth()))
                val anchor2 = ym.atDay(norm.day2.coerceAtMost(ym.lengthOfMonth()))
                val prevYm = ym.minusMonths(1)
                val prevAnchor2 = prevYm.atDay(norm.day2.coerceAtMost(prevYm.lengthOfMonth()))

                val (startDate, endDate, isFirst) = when {
                    !date.isAfter(anchor1) -> {
                        Triple(prevAnchor2.plusDays(1), anchor1, true)
                    }
                    !date.isAfter(anchor2) -> {
                        Triple(anchor1.plusDays(1), anchor2, false)
                    }
                    else -> {
                        val nextYm = ym.plusMonths(1)
                        val nextAnchor1 = nextYm.atDay(norm.day1.coerceAtMost(nextYm.lengthOfMonth()))
                        Triple(anchor2.plusDays(1), nextAnchor1, true)
                    }
                }
                val cutoffOrder = if (isFirst) "1st Cutoff" else "2nd Cutoff"
                CutoffWindow(
                    startDate = startDate,
                    endDate = endDate,
                    isFirstCutoff = isFirst,
                    label = "$cutoffOrder • ${formatWindowRange(startDate, endDate, locale)}",
                    key = "SEMI_$endDate"
                )
            }
        }
    }

    fun resolveCutoffWindow(
        date: LocalDate,
        locale: Locale = Locale.ENGLISH
    ): CutoffWindow = resolveCutoffWindow(date = date, config = CutoffScheduleConfig(), locale = locale)

    fun summarizeCutoffShifts(
        shifts: List<ShiftEntity>,
        referenceDate: LocalDate = LocalDate.now(),
        config: CutoffScheduleConfig = CutoffScheduleConfig(),
        locale: Locale = Locale.ENGLISH
    ): CutoffShiftSummary {
        val window = resolveCutoffWindow(referenceDate, config, locale)
        val startEpoch = window.startDate.toEpochDay()
        val endEpoch = window.endDate.toEpochDay()
        val cutoffShifts = shifts.filter { it.dateEpochDay in startEpoch..endEpoch }
        val totalEarnings = cutoffShifts.sumOf { it.totalEarned }
        val shiftDaysCount = cutoffShifts.map { it.dateEpochDay }.distinct().size
        val totalHours = cutoffShifts.sumOf { it.durationMinutes } / 60.0
        return CutoffShiftSummary(
            window = window,
            totalEarnings = totalEarnings,
            shiftDaysCount = shiftDaysCount,
            totalHours = totalHours
        )
    }

    fun summarizeCutoffShifts(
        shifts: List<ShiftEntity>,
        referenceDate: LocalDate = LocalDate.now(),
        locale: Locale = Locale.ENGLISH
    ): CutoffShiftSummary = summarizeCutoffShifts(
        shifts = shifts,
        referenceDate = referenceDate,
        config = CutoffScheduleConfig(),
        locale = locale
    )

    /**
     * Groups [shifts] by their resolved cutoff window under [config] and returns summaries
     * for all cutoff windows whose payday ([CutoffWindow.endDate]) has been reached (`endDate <= today`).
     */
    fun resolveReachedCutoffWindows(
        shifts: List<ShiftEntity>,
        today: LocalDate = LocalDate.now(),
        config: CutoffScheduleConfig = CutoffScheduleConfig(),
        locale: Locale = Locale.ENGLISH
    ): List<CutoffShiftSummary> {
        if (shifts.isEmpty()) return emptyList()
        val grouped = LinkedHashMap<String, Pair<CutoffWindow, MutableList<ShiftEntity>>>()
        shifts.forEach { shift ->
            val shiftDate = LocalDate.ofEpochDay(shift.dateEpochDay)
            val window = resolveCutoffWindow(shiftDate, config, locale)
            if (!window.endDate.isAfter(today)) {
                val existing = grouped[window.key]
                if (existing == null) {
                    grouped[window.key] = window to mutableListOf(shift)
                } else {
                    existing.second.add(shift)
                }
            }
        }
        return grouped.values
            .map { (window, bucketShifts) ->
                CutoffShiftSummary(
                    window = window,
                    totalEarnings = bucketShifts.sumOf { it.totalEarned },
                    shiftDaysCount = bucketShifts.map { it.dateEpochDay }.distinct().size,
                    totalHours = bucketShifts.sumOf { it.durationMinutes } / 60.0
                )
            }
            .sortedBy { it.window.endDate }
    }

    fun formatShortDate(date: LocalDate, locale: Locale = Locale.ENGLISH): String =
        date.format(DateTimeFormatter.ofPattern("MMM d", locale))
}
