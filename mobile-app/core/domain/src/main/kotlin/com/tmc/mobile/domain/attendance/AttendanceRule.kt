package com.tmc.mobile.domain.attendance

fun interface AttendanceRule {

    fun flags(record: AttendanceRecord): List<DayFlag>
}
