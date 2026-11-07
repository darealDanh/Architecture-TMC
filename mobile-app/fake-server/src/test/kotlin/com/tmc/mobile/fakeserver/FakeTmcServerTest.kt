package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.InvalidCredentialsException
import com.tmc.mobile.domain.port.ServerUnreachableException
import com.tmc.mobile.domain.port.SessionExpiredException
import java.time.Duration
import java.time.Instant
import java.time.YearMonth
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class FakeTmcServerTest {

    private val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
    private val network = MutableNetworkStatus()
    private val server = FakeTmcServer(network, clock, latency = Duration.ZERO)
    private val deviceKey = EcdsaP256.generateKeyPair().public.encoded

    @Test
    fun signInReturnsEmployeeAndToken() = runTest {
        val session = server.signIn("E002", "password")

        assertEquals("Tran Thi B", session.employee.name)
        assertEquals("Packing", session.employee.department)
        assertEquals("Long An", session.employee.factory)
        assertTrue(session.token.isNotBlank())
    }

    @Test
    fun wrongPasswordIsRejected() = runTest {
        assertThrows<InvalidCredentialsException> { server.signIn("E001", "letmein") }
        assertThrows<InvalidCredentialsException> { server.signIn("E999", "password") }
    }

    @Test
    fun offlineCallsFailAsUnreachable() = runTest {
        val token = server.signIn("E001", "password").token
        network.goOffline()

        assertThrows<ServerUnreachableException> { server.signIn("E001", "password") }
        assertThrows<ServerUnreachableException> { server.issueCertificate(token, deviceKey) }
        assertThrows<ServerUnreachableException> { server.monthlyAttendance(token, YearMonth.of(2026, 11)) }
    }

    @Test
    fun issuedCertificateVerifiesWithServerKey() = runTest {
        val certificate = server.issueCertificate(server.signIn("E001", "password").token, deviceKey)

        assertEquals("E001", certificate.employeeId)
        assertContentEquals(deviceKey, certificate.devicePublicKey)
        assertTrue(EcdsaP256.verify(server.serverPublicKey(), certificate.signedContent(), certificate.serverSignature))
    }

    @Test
    fun certificateLastsSevenDays() = runTest {
        val certificate = server.issueCertificate(server.signIn("E001", "password").token, deviceKey)

        assertEquals(clock.instant().plus(Duration.ofDays(7)), certificate.expiresAt)
    }

    @Test
    fun serverKeyIsTheSameAcrossInstances() {
        val other = FakeTmcServer(network, clock, latency = Duration.ZERO)

        assertContentEquals(server.serverPublicKey(), other.serverPublicKey())
    }

    @Test
    fun tokensSurviveAServerRestart() = runTest {
        val token = server.signIn("E001", "password").token
        val restarted = FakeTmcServer(network, clock, latency = Duration.ZERO)

        assertEquals("E001", restarted.issueCertificate(token, deviceKey).employeeId)
    }

    @Test
    fun invalidTokenIsRejected() = runTest {
        assertThrows<SessionExpiredException> { server.issueCertificate("E001.123.forged", deviceKey) }
        assertThrows<SessionExpiredException> { server.monthlyAttendance("garbage", YearMonth.of(2026, 11)) }
    }

    @Test
    fun attendanceComesFromTheGenerator() = runTest {
        val session = server.signIn("E001", "password")

        val records = server.monthlyAttendance(session.token, YearMonth.of(2026, 11))

        assertEquals(AttendanceGenerator.forMonth(session.employee, YearMonth.of(2026, 11), clock.instant().atZone(clock.zone).toLocalDate()), records)
    }
}
