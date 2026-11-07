package com.tmc.mobile.domain.attendance

import java.time.Duration
import java.time.YearMonth

data class AttendanceDay(
    val record: AttendanceRecord,
    val flags: List<DayFlag>,
    val worked: Duration,
) {

    val needsReview: Boolean get() = flags.any { it.severity == Severity.NEEDS_REVIEW }
}

data class MonthlyAttendance(
    val month: YearMonth,
    val days: List<AttendanceDay>,
    val daysWorked: Int,
    val totalWorked: Duration,
    val toReview: Int,
    val fromCache: Boolean,
)
