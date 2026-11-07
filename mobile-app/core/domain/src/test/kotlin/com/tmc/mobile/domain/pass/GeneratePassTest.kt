package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.SequenceNonceSource
import com.tmc.mobile.domain.fakes.StubServer
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class GeneratePassTest {

    private val clock = MutableClock(Instant.parse("2026-11-07T07:52:00Z"))
    private val server = StubServer(clock)
    private val keyStore = InMemoryDeviceKeyStore()
    private val generatePass = GeneratePass(keyStore, SequenceNonceSource(), clock)

    private suspend fun certificate() =
        server.issueCertificate(server.signIn("E001", "password").token, keyStore.createKey())

    @Test
    fun passIsSignedByDeviceAndDecodes() = runTest {
        val certificate = certificate()

        val decoded = PassPayloadCodec.decode(generatePass(certificate).payload)

        assertEquals(certificate, decoded.certificate)
        assertEquals(clock.instant(), decoded.issuedAt)
        val signedContent = PassPayloadCodec.signedContent(decoded.certificate, decoded.issuedAt, decoded.nonce)
        assertTrue(EcdsaP256.verify(certificate.devicePublicKey, signedContent, decoded.deviceSignature))
    }

    @Test
    fun refreshAtIsFifteenSecondsAfterIssue() = runTest {
        val pass = generatePass(certificate())

        assertEquals(Duration.ofSeconds(15), Duration.between(pass.issuedAt, pass.refreshAt))
    }

    @Test
    fun eachPassHasNewNonce() = runTest {
        val certificate = certificate()

        val first = PassPayloadCodec.decode(generatePass(certificate).payload)
        val second = PassPayloadCodec.decode(generatePass(certificate).payload)

        assertNotEquals(first.nonce, second.nonce)
    }
}
