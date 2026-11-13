package com.tmc.mobile.data.attendance

import com.tmc.mobile.domain.attendance.AttendanceLoad
import com.tmc.mobile.domain.attendance.AttendanceRepository
import com.tmc.mobile.domain.attendance.NoAttendanceDataException
import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.port.AttendanceApi
import com.tmc.mobile.domain.port.AttendanceCache
import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.domain.port.ServerUnreachableException
import java.time.YearMonth

class OfflineFirstAttendanceRepository(
    private val api: AttendanceApi,
    private val cache: AttendanceCache,
    private val network: NetworkStatus,
) : AttendanceRepository {

    override suspend fun load(session: Session, month: YearMonth): AttendanceLoad =
        fetchIfOnline(session, month) ?: loadFromCache(session, month)

    private suspend fun fetchIfOnline(session: Session, month: YearMonth): AttendanceLoad? {
        if (!network.isOnline.value) {
            return null
        }
        return try {
            val records = api.monthlyAttendance(session.token, month)
            cache.write(session.employee.id, month, records)
            AttendanceLoad(records, fromCache = false)
        } catch (e: ServerUnreachableException) {
            null
        }
    }

    private suspend fun loadFromCache(session: Session, month: YearMonth): AttendanceLoad {
        val cached = cache.read(session.employee.id, month) ?: throw NoAttendanceDataException()
        return AttendanceLoad(cached, fromCache = true)
    }
}
