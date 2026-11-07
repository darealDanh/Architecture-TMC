@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tmc.mobile.ui.attendance

import com.tmc.mobile.MainDispatcherRule
import com.tmc.mobile.data.attendance.OfflineFirstAttendanceRepository
import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.attendance.AttendanceSummarizer
import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.attendance.GetMonthlyAttendance
import com.tmc.mobile.domain.fakes.InMemoryAttendanceCache
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.session.SignInAndEnroll
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class AttendanceViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-11-07T07:52:00Z"))
    private val network = MutableNetworkStatus()
    private val server = StubServer(clock)
    private val sessions = InMemorySessionStore()
    private val november = YearMonth.of(2026, 11)

    init {
        server.setAttendance(
            "E001",
            november,
            listOf(AttendanceRecord(LocalDate.of(2026, 11, 3), "BD-01", LocalTime.of(7, 52), LocalTime.of(17, 6), EntrySource.GATE)),
        )
    }

    private suspend fun TestScope.started(): AttendanceViewModel {
        SignInAndEnroll(server, server, InMemoryDeviceKeyStore(), sessions, InMemoryCertificateStore())("E001", "password")
        val repository = OfflineFirstAttendanceRepository(server, InMemoryAttendanceCache(), network)
        val viewModel = AttendanceViewModel(GetMonthlyAttendance(sessions, repository, AttendanceSummarizer.standard()), clock)
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun attendanceLoadsCurrentMonthFirst() = runTest {
        val state = started().state.value

        assertEquals(november, state.selected)
        val loaded = assertIs<AttendanceContent.Loaded>(state.content)
        assertEquals(1, loaded.attendance.daysWorked)
    }

    @Test
    fun monthListIsCurrentPlusTwoPrevious() = runTest {
        assertEquals(
            listOf(november, YearMonth.of(2026, 10), YearMonth.of(2026, 9)),
            started().state.value.months,
        )
    }

    @Test
    fun selectingAMonthLoadsIt() = runTest {
        val viewModel = started()

        viewModel.select(YearMonth.of(2026, 10))
        advanceUntilIdle()

        assertEquals(YearMonth.of(2026, 10), viewModel.state.value.selected)
        assertEquals(0, assertIs<AttendanceContent.Loaded>(viewModel.state.value.content).attendance.daysWorked)
    }

    @Test
    fun offlineWithoutCacheShowsNoData() = runTest {
        network.goOffline()

        assertEquals(AttendanceContent.NoData, started().state.value.content)
    }
}
