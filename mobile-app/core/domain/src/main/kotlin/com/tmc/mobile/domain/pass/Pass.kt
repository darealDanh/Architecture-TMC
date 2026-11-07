package com.tmc.mobile.domain.pass

import java.time.Instant

data class Pass(
    val payload: String,
    val issuedAt: Instant,
    val refreshAt: Instant,
)
