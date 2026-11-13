package com.tmc.mobile.domain.attendance

import java.time.Duration
import java.time.YearMonth

class AttendanceSummarizer(private val rules: List<AttendanceRule>) {

    fun summarize(month: YearMonth, records: List<AttendanceRecord>, fromCache: Boolean): MonthlyAttendance {
        val days = records
            .sortedByDescending(AttendanceRecord::date)
            .map { record -> AttendanceDay(record, rules.flatMap { it.flags(record) }, workedOn(record)) }
        return MonthlyAttendance(
            month = month,
            days = days,
            daysWorked = days.count { it.record.entry != null },
            totalWorked = days.fold(Duration.ZERO) { total, day -> total.plus(day.worked) },
            toReview = days.count(AttendanceDay::needsReview),
            fromCache = fromCache,
        )
    }

    private fun workedOn(record: AttendanceRecord): Duration {
        val entry = record.entry ?: return Duration.ZERO
        val exit = record.exit ?: return Duration.ZERO
        return Duration.between(entry, exit)
    }
}
