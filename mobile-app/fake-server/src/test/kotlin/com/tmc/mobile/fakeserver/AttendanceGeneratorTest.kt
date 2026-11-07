package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.model.Employee
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class AttendanceGeneratorTest {

    private val employee = Employee("E001", "Nguyen Van A", "Assembly", "Binh Duong")
    private val october = YearMonth.of(2026, 10)
    private val today = LocalDate.of(2026, 11, 7)

    private fun weekdays(month: YearMonth) = (1..month.lengthOfMonth())
        .map(month::atDay)
        .filter { it.dayOfWeek != DayOfWeek.SATURDAY && it.dayOfWeek != DayOfWeek.SUNDAY }

    @Test
    fun attendanceIsDeterministic() {
        assertEquals(
            AttendanceGenerator.forMonth(employee, october, today),
            AttendanceGenerator.forMonth(employee, october, today),
        )
    }

    @Test
    fun pastMonthHasEveryWeekday() {
        val records = AttendanceGenerator.forMonth(employee, october, today)

        assertEquals(weekdays(october), records.map { it.date })
    }

    @Test
    fun noFutureDays() {
        val november = AttendanceGenerator.forMonth(employee, YearMonth.of(2026, 11), today)

        assertTrue(november.all { it.date.isBefore(today) })
        assertEquals(emptyList(), AttendanceGenerator.forMonth(employee, YearMonth.of(2026, 12), today))
    }

    @Test
    fun weekendsAreSkipped() {
        val records = AttendanceGenerator.forMonth(employee, october, today)

        assertTrue(records.none { it.date.dayOfWeek == DayOfWeek.SATURDAY || it.date.dayOfWeek == DayOfWeek.SUNDAY })
    }

    @Test
    fun thirdWeekdayHasMissingExit() {
        val third = AttendanceGenerator.forMonth(employee, october, today).single { it.date == weekdays(october)[2] }

        assertNull(third.exit)
        assertEquals(EntrySource.GATE, third.source)
    }

    @Test
    fun secondWeekdayIsManual() {
        val second = AttendanceGenerator.forMonth(employee, october, today).single { it.date == weekdays(october)[1] }

        assertEquals(EntrySource.MANUAL, second.source)
        assertNull(second.gateId)
        assertEquals(LocalTime.of(8, 0), second.entry)
        assertEquals(LocalTime.of(17, 0), second.exit)
    }

    @Test
    fun gateTimesStayInTheShiftWindow() {
        val gateDays = AttendanceGenerator.forMonth(employee, october, today).filter { it.source == EntrySource.GATE }

        assertTrue(gateDays.all { it.entry!! in LocalTime.of(7, 45)..LocalTime.of(7, 59) })
        assertTrue(gateDays.mapNotNull { it.exit }.all { it in LocalTime.of(17, 0)..LocalTime.of(17, 9) })
        assertTrue(gateDays.all { it.gateId == "BD-01" || it.gateId == "BD-02" })
    }

    @Test
    fun longAnEmployeesUseLongAnGates() {
        val longAn = Employee("E002", "Tran Thi B", "Packing", "Long An")

        val gates = AttendanceGenerator.forMonth(longAn, october, today).mapNotNull { it.gateId }.toSet()

        assertEquals(setOf("LA-01", "LA-02"), gates)
    }
}
