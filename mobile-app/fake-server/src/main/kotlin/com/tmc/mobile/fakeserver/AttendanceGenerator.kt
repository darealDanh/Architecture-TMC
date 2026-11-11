package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.model.Employee
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

object AttendanceGenerator {

    private val shiftStart = LocalTime.of(7, 45)
    private val shiftEnd = LocalTime.of(17, 0)
    private val manualEntry = LocalTime.of(8, 0)
    private const val MANUAL_WEEKDAY = 2
    private const val MISSING_EXIT_WEEKDAY = 3

    fun forMonth(employee: Employee, month: YearMonth, today: LocalDate): List<AttendanceRecord> =
        (1..month.lengthOfMonth())
            .map(month::atDay)
            .filter { it.isBefore(today) && it.isWeekday() }
            .mapIndexed { index, date -> recordFor(employee, date, weekdayNumber = index + 1) }

    private fun recordFor(employee: Employee, date: LocalDate, weekdayNumber: Int): AttendanceRecord {
        if (weekdayNumber == MANUAL_WEEKDAY) {
            return AttendanceRecord(date, null, manualEntry, shiftEnd, EntrySource.MANUAL)
        }
        val variation = Math.floorMod("${employee.id}$date".hashCode(), 150)
        val entry = shiftStart.plusMinutes((variation % 15).toLong())
        val exit = shiftEnd.plusMinutes((variation / 15).toLong()).takeUnless { weekdayNumber == MISSING_EXIT_WEEKDAY }
        return AttendanceRecord(date, gateFor(employee, date), entry, exit, EntrySource.GATE)
    }

    private fun gateFor(employee: Employee, date: LocalDate): String {
        val prefix = if (employee.factory == "Long An") "LA" else "BD"
        return "$prefix-0${1 + date.dayOfMonth % 2}"
    }

    private fun LocalDate.isWeekday(): Boolean = dayOfWeek != DayOfWeek.SATURDAY && dayOfWeek != DayOfWeek.SUNDAY
}
