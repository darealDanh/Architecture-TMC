package com.tmc.mobile.domain.port

import com.tmc.mobile.domain.attendance.AttendanceRecord
import java.time.YearMonth

interface AttendanceCache {

    suspend fun read(employeeId: String, month: YearMonth): List<AttendanceRecord>?

    suspend fun write(employeeId: String, month: YearMonth, records: List<AttendanceRecord>)

    suspend fun clear()
}
