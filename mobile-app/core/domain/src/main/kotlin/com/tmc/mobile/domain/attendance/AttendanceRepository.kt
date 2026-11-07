package com.tmc.mobile.domain.attendance

import com.tmc.mobile.domain.model.Session
import java.time.YearMonth

data class AttendanceLoad(val records: List<AttendanceRecord>, val fromCache: Boolean)

class NoAttendanceDataException : Exception("No attendance data available offline")

interface AttendanceRepository {

    suspend fun load(session: Session, month: YearMonth): AttendanceLoad
}
