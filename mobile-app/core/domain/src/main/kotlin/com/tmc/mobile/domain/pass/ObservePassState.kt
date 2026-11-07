package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.port.CertificateStore
import java.time.Clock
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class ObservePassState(
    private val certificates: CertificateStore,
    private val generatePass: GeneratePass,
    private val clock: Clock,
    private val tick: Duration = Duration.ofSeconds(1),
) {

    operator fun invoke(): Flow<PassState> = flow {
        var current: Pair<DeviceCertificate, Pass>? = null
        while (true) {
            val certificate = certificates.current()
            val now = clock.instant()
            val state = when {
                certificate == null -> PassState.NotEnrolled
                !certificate.isValidAt(now) -> PassState.CertificateExpired(certificate.expiresAt)
                else -> {
                    val pass = current
                        ?.takeIf { (shownFor, pass) -> shownFor == certificate && now.isBefore(pass.refreshAt) }
                        ?.second
                        ?: generatePass(certificate).also { current = certificate to it }
                    PassState.Ready(pass, secondsUntil(now, pass.refreshAt), certificate.expiresAt)
                }
            }
            emit(state)
            delay(tick.toMillis())
        }
    }

    private fun secondsUntil(now: Instant, refreshAt: Instant): Int {
        val millis = Duration.between(now, refreshAt).toMillis()
        return ((millis + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }

    private companion object {
        const val MILLIS_PER_SECOND = 1000L
    }
}
