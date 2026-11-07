package com.tmc.mobile.domain.device

import java.time.Instant

data class DeviceStatus(
    val hardwareBacked: Boolean,
    val algorithm: String,
    val certificateExpiresAt: Instant?,
    val certificateActive: Boolean,
)
