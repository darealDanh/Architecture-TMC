package com.tmc.mobile.domain.fakes

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.model.Employee
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

class StubServer(
    private val clock: Clock,
    private val network: NetworkStatus = MutableNetworkStatus(),
) : AuthApi, EnrollmentApi, AttendanceApi {

    private val keys = EcdsaP256.generateKeyPair()
    private val employees = mapOf(
        "E001" to Employee("E001", "Nguyen Van A", "Assembly", "Binh Duong"),
        "E002" to Employee("E002", "Tran Thi B", "Packing", "Long An"),
    )
    private val attendance = mutableMapOf<Pair<String, YearMonth>, List<AttendanceRecord>>()

    var certificatesIssued = 0
        private set

    fun serverPublicKey(): ByteArray = keys.public.encoded

    fun setAttendance(employeeId: String, month: YearMonth, records: List<AttendanceRecord>) {
        attendance[employeeId to month] = records
    }

    override suspend fun signIn(employeeId: String, password: String): Session {
        requireOnline()
        val employee = employees[employeeId]
        if (employee == null || password != PASSWORD) {
            throw InvalidCredentialsException()
        }
        return Session(TOKEN_PREFIX + employee.id, employee)
    }

    override suspend fun issueCertificate(token: String, devicePublicKey: ByteArray): DeviceCertificate {
        requireOnline()
        val employeeId = employeeFor(token)
        val unsigned = DeviceCertificate(
            serial = UUID.randomUUID().toString(),
            employeeId = employeeId,
            devicePublicKey = devicePublicKey,
            expiresAt = clock.instant().plus(CERTIFICATE_LIFETIME),
            serverSignature = ByteArray(0),
        )
        certificatesIssued++
        return unsigned.withServerSignature(EcdsaP256.sign(keys.private, unsigned.signedContent()))
    }

    override suspend fun monthlyAttendance(token: String, month: YearMonth): List<AttendanceRecord> {
        requireOnline()
        return attendance[employeeFor(token) to month].orEmpty()
    }

    private fun employeeFor(token: String): String {
        val employeeId = token.removePrefix(TOKEN_PREFIX)
        if (!token.startsWith(TOKEN_PREFIX) || employeeId !in employees) {
            throw SessionExpiredException()
        }
        return employeeId
    }

    private fun requireOnline() {
        if (!network.isOnline.value) {
            throw ServerUnreachableException()
        }
    }

    companion object {
        const val PASSWORD = "password"
        val CERTIFICATE_LIFETIME: Duration = Duration.ofDays(7)
        private const val TOKEN_PREFIX = "stub-token-"
    }
}
