package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.model.DeviceCertificate
import java.time.Instant
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class PassPayloadCodecTest {

    private val serverKeys = EcdsaP256.generateKeyPair()
    private val deviceKeys = EcdsaP256.generateKeyPair()
    private val issuedAt = Instant.parse("2026-11-07T07:52:00Z")

    private fun certificate(): DeviceCertificate {
        val unsigned = DeviceCertificate(
            serial = "serial-1",
            employeeId = "E001",
            devicePublicKey = deviceKeys.public.encoded,
            expiresAt = Instant.parse("2026-11-14T07:52:00Z"),
            serverSignature = ByteArray(0),
        )
        return unsigned.withServerSignature(EcdsaP256.sign(serverKeys.private, unsigned.signedContent()))
    }

    @Test
    fun roundTripPreservesEveryField() {
        val certificate = certificate()
        val signature = EcdsaP256.sign(deviceKeys.private, PassPayloadCodec.signedContent(certificate, issuedAt, "bm9uY2U"))

        val decoded = PassPayloadCodec.decode(PassPayloadCodec.encode(certificate, issuedAt, "bm9uY2U", signature))

        assertEquals(certificate, decoded.certificate)
        assertEquals(issuedAt, decoded.issuedAt)
        assertEquals("bm9uY2U", decoded.nonce)
        assertContentEquals(signature, decoded.deviceSignature)
    }

    @Test
    fun encodedPassStartsWithVersionTwo() {
        val payload = PassPayloadCodec.encode(certificate(), issuedAt, "bm9uY2U", byteArrayOf(1, 2, 3))

        assertEquals("v2", payload.substringBefore('.'))
        assertEquals(9, payload.split('.').size)
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "abc", "v1.a.b.c.d.e.f.g.h", "v2.!!.b.c.d.e.f.g.h"])
    fun rejectsMalformedInput(raw: String) {
        assertThrows<MalformedPassException> { PassPayloadCodec.decode(raw) }
    }
}
