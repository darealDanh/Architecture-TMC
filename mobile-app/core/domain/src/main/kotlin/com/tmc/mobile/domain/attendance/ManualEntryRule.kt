package com.tmc.mobile.domain.attendance

class ManualEntryRule : AttendanceRule {

    override fun flags(record: AttendanceRecord): List<DayFlag> =
        if (record.source == EntrySource.MANUAL) listOf(FLAG) else emptyList()

    private companion object {
        val FLAG = DayFlag("Manual", Severity.INFO)
    }
}
