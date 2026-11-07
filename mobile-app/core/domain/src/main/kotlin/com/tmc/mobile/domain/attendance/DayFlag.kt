package com.tmc.mobile.domain.attendance

enum class Severity {
    NEEDS_REVIEW,
    INFO,
}

data class DayFlag(val label: String, val severity: Severity)
