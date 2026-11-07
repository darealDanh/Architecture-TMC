package com.tmc.mobile.data.attendance

import com.tmc.mobile.domain.attendance.AttendanceRecord
import com.tmc.mobile.domain.attendance.EntrySource
import com.tmc.mobile.domain.port.AttendanceCache
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth

class RoomAttendanceCache(private val dao: AttendanceDao) : AttendanceCache {

    override suspend fun read(employeeId: String, month: YearMonth): List<AttendanceRecord>? {
        if (dao.isCached(employeeId, month.toString()) == 0) {
            return null
        }
        return dao.records(employeeId, month.toString()).map(::toRecord)
    }

    override suspend fun write(employeeId: String, month: YearMonth, records: List<AttendanceRecord>) {
        dao.replaceMonth(employeeId, month.toString(), records.map { toEntity(employeeId, month, it) })
    }

    override suspend fun clear() {
        dao.deleteAll()
    }

    private fun toEntity(employeeId: String, month: YearMonth, record: AttendanceRecord) = AttendanceRecordEntity(
        employeeId = employeeId,
        month = month.toString(),
        date = record.date.toString(),
        gateId = record.gateId,
        entry = record.entry?.toString(),
        exit = record.exit?.toString(),
        source = record.source.name,
    )

    private fun toRecord(entity: AttendanceRecordEntity) = AttendanceRecord(
        date = LocalDate.parse(entity.date),
        gateId = entity.gateId,
        entry = entity.entry?.let(LocalTime::parse),
        exit = entity.exit?.let(LocalTime::parse),
        source = EntrySource.valueOf(entity.source),
    )
}
