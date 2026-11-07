package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.fakes.InMemoryAttendanceCache
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.StubServer
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class SignOutTest {

    private val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
    private val server = StubServer(clock)
    private val keyStore = InMemoryDeviceKeyStore()
    private val sessions = InMemorySessionStore()
    private val certificates = InMemoryCertificateStore()
    private val cache = InMemoryAttendanceCache()
    private val signIn = SignInAndEnroll(server, server, keyStore, sessions, certificates)
    private val signOut = SignOut(keyStore, sessions, certificates, cache)
    private val month = YearMonth.of(2026, 11)
    private val record = AttendanceRecord(
        LocalDate.of(2026, 11, 3), "BD-01", LocalTime.of(7, 52), LocalTime.of(17, 6), EntrySource.GATE,
    )

    @Test
    fun removesKeySessionCertificateAndCache() = runTest {
        signIn("E001", "password")
        cache.write("E001", month, listOf(record))

        signOut()

        assertFalse(keyStore.hasKey())
        assertNull(sessions.current())
        assertNull(certificates.current())
        assertNull(cache.read("E001", month))
    }

    @Test
    fun nextEmployeeSeesOnlyOwnData() = runTest {
        signIn("E001", "password")
        cache.write("E001", month, listOf(record))
        signOut()

        val session = signIn("E002", "password")

        assertEquals("E002", session.employee.id)
        assertEquals("E002", certificates.current()?.employeeId)
        assertNull(cache.read("E001", month))
    }
}
