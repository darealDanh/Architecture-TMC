package com.tmc.mobile.domain.attendance

class MissingExitRule : AttendanceRule {

    override fun flags(record: AttendanceRecord): List<DayFlag> =
        if (record.entry != null && record.exit == null) listOf(FLAG) else emptyList()

    private companion object {
        val FLAG = DayFlag("Missing exit", Severity.NEEDS_REVIEW)
    }
}
