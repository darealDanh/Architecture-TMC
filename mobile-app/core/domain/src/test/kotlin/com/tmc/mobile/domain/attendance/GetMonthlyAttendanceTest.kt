package com.tmc.mobile.domain.attendance

import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.port.SessionExpiredException
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class GetMonthlyAttendanceTest {

    private val month = YearMonth.of(2026, 11)
    private val session = Session("token", Employee("E001", "Nguyen Van A", "Assembly", "Binh Duong"))
    private val record = AttendanceRecord(
        LocalDate.of(2026, 11, 3), "BD-01", LocalTime.of(7, 52), LocalTime.of(17, 6), EntrySource.GATE,
    )
    private val repository = object : AttendanceRepository {
        var requested: Pair<Session, YearMonth>? = null

        override suspend fun load(session: Session, month: YearMonth): AttendanceLoad {
            requested = session to month
            return AttendanceLoad(listOf(record), fromCache = true)
        }
    }

    @Test
    fun loadsTheSignedInEmployeesMonth() = runTest {
        val summary = GetMonthlyAttendance(InMemorySessionStore(session), repository, AttendanceSummarizer(listOf(MissingExitRule(), ManualEntryRule())))(month)

        assertEquals(session to month, repository.requested)
        assertEquals(1, summary.daysWorked)
        assertEquals(true, summary.fromCache)
    }

    @Test
    fun withoutSessionFails() = runTest {
        assertThrows<SessionExpiredException> {
            GetMonthlyAttendance(InMemorySessionStore(), repository, AttendanceSummarizer(listOf(MissingExitRule(), ManualEntryRule())))(month)
        }
    }
}
