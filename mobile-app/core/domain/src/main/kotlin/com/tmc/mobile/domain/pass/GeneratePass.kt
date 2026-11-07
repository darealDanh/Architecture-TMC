package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.port.NonceSource
import com.tmc.mobile.domain.port.PayloadSigner
import java.time.Clock
import java.time.Duration

class GeneratePass(
    private val signer: PayloadSigner,
    private val nonces: NonceSource,
    private val clock: Clock,
    private val refreshInterval: Duration = REFRESH_INTERVAL,
) {

    operator fun invoke(certificate: DeviceCertificate): Pass {
        val issuedAt = clock.instant()
        val nonce = nonces.next()
        val signature = signer.sign(PassPayloadCodec.signedContent(certificate, issuedAt, nonce))
        return Pass(
            payload = PassPayloadCodec.encode(certificate, issuedAt, nonce, signature),
            issuedAt = issuedAt,
            refreshAt = issuedAt.plus(refreshInterval),
        )
    }

    companion object {
        val REFRESH_INTERVAL: Duration = Duration.ofSeconds(15)
    }
}
