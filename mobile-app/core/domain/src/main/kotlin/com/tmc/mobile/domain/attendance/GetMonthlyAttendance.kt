package com.tmc.mobile.domain.attendance

import com.tmc.mobile.domain.port.SessionExpiredException
import com.tmc.mobile.domain.port.SessionStore
import java.time.YearMonth

class GetMonthlyAttendance(
    private val sessions: SessionStore,
    private val repository: AttendanceRepository,
    private val summarizer: AttendanceSummarizer,
) {

    suspend operator fun invoke(month: YearMonth): MonthlyAttendance {
        val session = sessions.current() ?: throw SessionExpiredException()
        val load = repository.load(session, month)
        return summarizer.summarize(month, load.records, load.fromCache)
    }
}
