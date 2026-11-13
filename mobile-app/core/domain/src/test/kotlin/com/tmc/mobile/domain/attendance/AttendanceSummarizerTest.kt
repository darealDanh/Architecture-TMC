package com.tmc.mobile.domain.attendance

import java.time.Duration
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class AttendanceSummarizerTest {

    private val month = YearMonth.of(2026, 11)
    private val summarizer = AttendanceSummarizer(listOf(MissingExitRule(), ManualEntryRule()))

    private fun gate(day: Int, entry: String, exit: String?) = AttendanceRecord(
        LocalDate.of(2026, 11, day), "BD-01", LocalTime.parse(entry), exit?.let(LocalTime::parse), EntrySource.GATE,
    )

    private val manual = AttendanceRecord(
        LocalDate.of(2026, 11, 4), null, LocalTime.of(8, 0), LocalTime.of(17, 0), EntrySource.MANUAL,
    )

    @Test
    fun missingExitNeedsReviewAndCountsZeroHours() {
        val day = summarizer.summarize(month, listOf(gate(5, "07:55", null)), fromCache = false).days.single()

        assertEquals(listOf(DayFlag("Missing exit", Severity.NEEDS_REVIEW)), day.flags)
        assertEquals(Duration.ZERO, day.worked)
    }

    @Test
    fun manualEntryIsInfoAndCountsItsHours() {
        val day = summarizer.summarize(month, listOf(manual), fromCache = false).days.single()

        assertEquals(listOf(DayFlag("Manual", Severity.INFO)), day.flags)
        assertEquals(Duration.ofHours(9), day.worked)
    }

    @Test
    fun totalsAddUp() {
        val records = listOf(gate(7, "07:52", "17:06"), gate(6, "07:48", "17:01"), gate(5, "07:55", null), manual)

        val summary = summarizer.summarize(month, records, fromCache = true)

        assertEquals(4, summary.daysWorked)
        assertEquals(Duration.ofHours(27).plusMinutes(27), summary.totalWorked)
        assertEquals(1, summary.toReview)
        assertEquals(true, summary.fromCache)
    }

    @Test
    fun newRuleAddsFlagWithoutChangingExistingRules() {
        val lateArrival = AttendanceRule { record ->
            if (record.entry != null && record.entry.isAfter(LocalTime.of(8, 0))) {
                listOf(DayFlag("Late", Severity.NEEDS_REVIEW))
            } else {
                emptyList()
            }
        }
        val extended = AttendanceSummarizer(listOf(MissingExitRule(), ManualEntryRule()) + lateArrival)

        val day = extended.summarize(month, listOf(gate(3, "08:20", null)), fromCache = false).days.single()

        assertEquals(listOf("Missing exit", "Late"), day.flags.map(DayFlag::label))
    }

    @Test
    fun daysAreNewestFirst() {
        val summary = summarizer.summarize(month, listOf(gate(3, "07:50", "17:00"), gate(7, "07:52", "17:06")), false)

        assertEquals(listOf(7, 3), summary.days.map { it.record.date.dayOfMonth })
    }
}
