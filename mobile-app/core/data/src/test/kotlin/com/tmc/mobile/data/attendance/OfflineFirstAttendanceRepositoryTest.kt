package com.tmc.mobile.data.attendance

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.attendance.NoAttendanceDataException
import com.tmc.mobile.domain.fakes.InMemoryAttendanceCache
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.StubServer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OfflineFirstAttendanceRepositoryTest {

    private val month = YearMonth.of(2026, 11)
    private val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
    private val deviceNetwork = MutableNetworkStatus()
    private val serverNetwork = MutableNetworkStatus()
    private val server = StubServer(clock, serverNetwork)
    private val cache = InMemoryAttendanceCache()
    private val repository = OfflineFirstAttendanceRepository(server, cache, deviceNetwork)
    private val records = listOf(
        AttendanceRecord(LocalDate.of(2026, 11, 3), "BD-01", LocalTime.of(7, 52), LocalTime.of(17, 6), EntrySource.GATE),
    )

    init {
        server.setAttendance("E001", month, records)
    }

    private suspend fun session() = server.signIn("E001", "password")

    @Test
    fun onlineFetchesAndCaches() = runTest {
        val load = repository.load(session(), month)

        assertEquals(records, load.records)
        assertEquals(false, load.fromCache)
        assertEquals(records, cache.read("E001", month))
    }

    @Test
    fun offlineReturnsCacheMarkedFromCache() = runTest {
        val session = session()
        repository.load(session, month)
        deviceNetwork.goOffline()

        val load = repository.load(session, month)

        assertEquals(records, load.records)
        assertEquals(true, load.fromCache)
    }

    @Test
    fun unreachableFallsBackToCache() = runTest {
        val session = session()
        repository.load(session, month)
        serverNetwork.goOffline()

        val load = repository.load(session, month)

        assertEquals(records, load.records)
        assertEquals(true, load.fromCache)
    }

    @Test
    fun offlineWithoutCacheThrowsNoData() = runTest {
        val session = session()
        deviceNetwork.goOffline()

        assertFailsWith<NoAttendanceDataException> { repository.load(session, month) }
    }
}
