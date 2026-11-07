package com.tmc.mobile.domain.attendance

import java.time.LocalDate
import java.time.LocalTime

data class AttendanceRecord(
    val date: LocalDate,
    val gateId: String?,
    val entry: LocalTime?,
    val exit: LocalTime?,
    val source: EntrySource,
)
