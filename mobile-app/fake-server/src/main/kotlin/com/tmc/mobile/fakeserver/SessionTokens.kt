package com.tmc.mobile.fakeserver

import java.security.MessageDigest
import java.time.Instant
import java.util.Base64
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

internal object SessionTokens {

    private const val ALGORITHM = "HmacSHA256"
    private val secret = SecretKeySpec("tmc-fake-server-session-secret".toByteArray(), ALGORITHM)
    private val encoder = Base64.getUrlEncoder().withoutPadding()

    fun issue(employeeId: String, expiresAt: Instant): String {
        val body = "$employeeId.${expiresAt.epochSecond}"
        return "$body.${mac(body)}"
    }

    fun verify(token: String, now: Instant): String? {
        val parts = token.split(".")
        if (parts.size != 3) {
            return null
        }
        val (employeeId, expiry, signature) = parts
        val expiresAt = expiry.toLongOrNull()?.let(Instant::ofEpochSecond) ?: return null
        val expected = mac("$employeeId.$expiry")
        val authentic = MessageDigest.isEqual(expected.toByteArray(), signature.toByteArray())
        return employeeId.takeIf { authentic && now.isBefore(expiresAt) }
    }

    private fun mac(body: String): String =
        encoder.encodeToString(Mac.getInstance(ALGORITHM).apply { init(secret) }.doFinal(body.toByteArray()))
}
