package com.tmc.mobile.domain.port

import com.tmc.mobile.domain.attendance.AttendanceRecord
import java.time.YearMonth

interface AttendanceApi {

    suspend fun monthlyAttendance(token: String, month: YearMonth): List<AttendanceRecord>
}
