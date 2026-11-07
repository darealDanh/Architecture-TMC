package com.tmc.mobile.domain.pass

import java.time.Instant

sealed interface PassState {

    data object NotEnrolled : PassState

    data object DeviceKeyMissing : PassState

    data class CertificateExpired(val expiredAt: Instant) : PassState

    data class Ready(val pass: Pass, val secondsLeft: Int, val certificateExpiresAt: Instant) : PassState
}
