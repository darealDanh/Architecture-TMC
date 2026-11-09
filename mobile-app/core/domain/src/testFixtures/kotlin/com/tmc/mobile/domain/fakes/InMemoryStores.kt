package com.tmc.mobile.domain.fakes

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.port.AttendanceCache
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.SessionStore
import java.time.YearMonth

class InMemorySessionStore(private var session: Session? = null) : SessionStore {

    override suspend fun current(): Session? = session

    override suspend fun save(session: Session) {
        this.session = session
    }

    override suspend fun clear() {
        session = null
    }
}

class InMemoryCertificateStore(private var certificate: DeviceCertificate? = null) : CertificateStore {

    override suspend fun current(): DeviceCertificate? = certificate

    override suspend fun save(certificate: DeviceCertificate) {
        this.certificate = certificate
    }

    override suspend fun clear() {
        certificate = null
    }
}

class InMemoryAttendanceCache : AttendanceCache {

    private val months = mutableMapOf<Pair<String, YearMonth>, List<AttendanceRecord>>()

    override suspend fun read(employeeId: String, month: YearMonth): List<AttendanceRecord>? = months[employeeId to month]

    override suspend fun write(employeeId: String, month: YearMonth, records: List<AttendanceRecord>) {
        months[employeeId to month] = records
    }

    override suspend fun clear() {
        months.clear()
    }
}
