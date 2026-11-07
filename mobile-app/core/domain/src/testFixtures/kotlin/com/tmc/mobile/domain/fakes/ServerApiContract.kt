package com.tmc.mobile.domain.fakes

import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.AttendanceApi
import com.tmc.mobile.domain.port.AuthApi
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.port.InvalidCredentialsException
import com.tmc.mobile.domain.port.ServerUnreachableException
import com.tmc.mobile.domain.port.SessionExpiredException
import java.time.Clock
import java.time.YearMonth
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ServerApiContract<T>(
    private val server: T,
    private val network: MutableNetworkStatus,
    private val clock: Clock,
    private val serverPublicKey: ByteArray,
) where T : AuthApi, T : EnrollmentApi, T : AttendanceApi {

    private val deviceKey = EcdsaP256.generateKeyPair().public.encoded

    suspend fun signInReturnsTheEmployee() {
        val session = server.signIn("E001", "password")

        assertEquals("E001", session.employee.id)
        assertEquals("Nguyen Van A", session.employee.name)
        assertTrue(session.token.isNotBlank())
    }

    suspend fun wrongPasswordIsRejected() {
        assertFailsWith<InvalidCredentialsException> { server.signIn("E001", "wrong") }
    }

    suspend fun offlineCallsFailAsUnreachable() {
        val token = server.signIn("E001", "password").token
        network.goOffline()

        assertFailsWith<ServerUnreachableException> { server.signIn("E001", "password") }
        assertFailsWith<ServerUnreachableException> { server.issueCertificate(token, deviceKey) }
        assertFailsWith<ServerUnreachableException> { server.monthlyAttendance(token, YearMonth.now(clock)) }
        network.goOnline()
    }

    suspend fun certificateBindsTheDeviceKeyAndVerifies() {
        val certificate = server.issueCertificate(server.signIn("E002", "password").token, deviceKey)

        assertEquals("E002", certificate.employeeId)
        assertContentEquals(deviceKey, certificate.devicePublicKey)
        assertTrue(certificate.isValidAt(clock.instant()))
        assertTrue(EcdsaP256.verify(serverPublicKey, certificate.signedContent(), certificate.serverSignature))
    }

    suspend fun forgedTokenIsRejected() {
        assertFailsWith<SessionExpiredException> { server.issueCertificate("E001.4102444800.forged", deviceKey) }
        assertFailsWith<SessionExpiredException> { server.monthlyAttendance("not-a-token", YearMonth.now(clock)) }
    }

    suspend fun attendanceStaysInTheRequestedMonth() {
        val token = server.signIn("E001", "password").token
        val month = YearMonth.now(clock)

        assertTrue(server.monthlyAttendance(token, month).all { YearMonth.from(it.date) == month })
    }

    suspend fun runAll() {
        signInReturnsTheEmployee()
        wrongPasswordIsRejected()
        offlineCallsFailAsUnreachable()
        certificateBindsTheDeviceKeyAndVerifies()
        forgedTokenIsRejected()
        attendanceStaysInTheRequestedMonth()
    }
}
