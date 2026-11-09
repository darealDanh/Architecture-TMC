package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.port.NonceSource
import java.security.SecureRandom
import java.util.Base64

class SecureRandomNonceSource : NonceSource {

    private val random = SecureRandom()
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    override fun next(): String = encoder.encodeToString(ByteArray(NONCE_BYTES).also(random::nextBytes))

    private companion object {
        const val NONCE_BYTES = 16
    }
}
