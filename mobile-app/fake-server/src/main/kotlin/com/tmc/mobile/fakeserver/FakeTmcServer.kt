package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.AttendanceApi
import com.tmc.mobile.domain.port.AuthApi
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.port.InvalidCredentialsException
import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.domain.port.ServerUnreachableException
import com.tmc.mobile.domain.port.SessionExpiredException
import java.time.Clock
import java.time.Duration
import java.time.YearMonth
import java.util.UUID
import kotlinx.coroutines.delay

class FakeTmcServer(
    private val network: NetworkStatus,
    private val clock: Clock,
    private val latency: Duration = DEFAULT_LATENCY,
) : AuthApi, EnrollmentApi, AttendanceApi {

    fun serverPublicKey(): ByteArray = FakeServerKey.publicKey

    override suspend fun signIn(employeeId: String, password: String): Session {
        respond()
        val employee = FakeAccounts.authenticate(employeeId, password) ?: throw InvalidCredentialsException()
        return Session(SessionTokens.issue(employee.id, clock.instant().plus(SESSION_LIFETIME)), employee)
    }

    override suspend fun issueCertificate(token: String, devicePublicKey: ByteArray): DeviceCertificate {
        respond()
        val employeeId = employeeFor(token)
        val unsigned = DeviceCertificate(
            serial = UUID.randomUUID().toString(),
            employeeId = employeeId,
            devicePublicKey = devicePublicKey,
            expiresAt = clock.instant().plus(CERTIFICATE_LIFETIME),
            serverSignature = ByteArray(0),
        )
        return unsigned.withServerSignature(EcdsaP256.sign(FakeServerKey.privateKey, unsigned.signedContent()))
    }

    override suspend fun monthlyAttendance(token: String, month: YearMonth): List<AttendanceRecord> {
        respond()
        val employee = FakeAccounts.find(employeeFor(token)) ?: throw SessionExpiredException()
        return AttendanceGenerator.forMonth(employee, month, clock.instant().atZone(clock.zone).toLocalDate())
    }

    private suspend fun respond() {
        delay(latency.toMillis())
        if (!network.isOnline.value) {
            throw ServerUnreachableException()
        }
    }

    private fun employeeFor(token: String): String =
        SessionTokens.verify(token, clock.instant()) ?: throw SessionExpiredException()

    companion object {
        val DEFAULT_LATENCY: Duration = Duration.ofMillis(400)
        val CERTIFICATE_LIFETIME: Duration = Duration.ofDays(7)
        val SESSION_LIFETIME: Duration = Duration.ofDays(30)
    }
}
