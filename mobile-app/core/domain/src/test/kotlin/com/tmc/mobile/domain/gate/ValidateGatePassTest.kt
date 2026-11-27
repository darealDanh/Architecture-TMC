package com.tmc.mobile.domain.gate

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.pass.PassPayloadCodec
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.security.KeyPair
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class ValidateGatePassTest {
    private val now = Instant.parse("2026-11-07T07:52:00Z")
    private val serverKeys = EcdsaP256.generateKeyPair()
    private val deviceKeys = EcdsaP256.generateKeyPair()
    private val nonce = "bm9uY2U"
    private val validator = ValidateGatePass(serverKeys.public.encoded, Clock.fixed(now, ZoneOffset.UTC)) { employee, factory ->
        employee == "E001" && factory == "Binh Duong"
    }

    private fun certificate(
        expiry: Instant = now.plus(Duration.ofDays(7)),
        keys: KeyPair = deviceKeys,
    ): DeviceCertificate {
        val unsigned = DeviceCertificate("serial-1", "E001", keys.public.encoded, expiry, byteArrayOf())
        return unsigned.withServerSignature(EcdsaP256.sign(serverKeys.private, unsigned.signedContent()))
    }

    private fun pass(
        certificate: DeviceCertificate = certificate(),
        issuedAt: Instant = now,
        keys: KeyPair = deviceKeys,
    ): String = PassPayloadCodec.encode(
        certificate,
        issuedAt,
        nonce,
        EcdsaP256.sign(keys.private, PassPayloadCodec.signedContent(certificate, issuedAt, nonce)),
    )

    @Test
    fun acceptsSignedPassForAuthorizedFactory() {
        assertEquals(GatePassDecision.Allowed("E001", nonce, "serial-1"), validator(pass(), "Binh Duong"))
    }

    @Test
    fun rejectsTamperedCertificateFieldsAndSignature() {
        val original = certificate()
        val changed = DeviceCertificate(original.serial, "E002", original.devicePublicKey, original.expiresAt, original.serverSignature)
        assertEquals(GatePassDecision.Rejected(GateRejectReason.INVALID_CERTIFICATE), validator(pass(changed), "Binh Duong"))
        val forged = original.withServerSignature(byteArrayOf(1, 2, 3))
        assertEquals(GatePassDecision.Rejected(GateRejectReason.INVALID_CERTIFICATE), validator(pass(forged), "Binh Duong"))
    }

    @Test
    fun rejectsExpiredCertificateAtBoundary() {
        assertEquals(GatePassDecision.Rejected(GateRejectReason.EXPIRED_CERTIFICATE), validator(pass(certificate(now)), "Binh Duong"))
    }

    @Test
    fun rejectsTamperedDeviceSignatureAndWrongDevice() {
        val signed = pass()
        val altered = signed.substringBeforeLast('.') + ".AQID"
        assertEquals(GatePassDecision.Rejected(GateRejectReason.INVALID_SIGNATURE), validator(altered, "Binh Duong"))
        assertEquals(
            GatePassDecision.Rejected(GateRejectReason.INVALID_SIGNATURE),
            validator(pass(keys = EcdsaP256.generateKeyPair()), "Binh Duong"),
        )
    }

    @Test
    fun acceptsTwoSecondsOfFutureClockSkewButNotMore() {
        assertEquals(
            GatePassDecision.Allowed("E001", nonce, "serial-1"),
            validator(pass(issuedAt = now.plusSeconds(2)), "Binh Duong"),
        )
        assertEquals(
            GatePassDecision.Rejected(GateRejectReason.EXPIRED_PASS),
            validator(pass(issuedAt = now.plusMillis(2001)), "Binh Duong"),
        )
    }

    @Test
    fun rejectsPassAtFifteenSecondBoundary() {
        assertEquals(GatePassDecision.Allowed("E001", nonce, "serial-1"), validator(pass(issuedAt = now.minusMillis(14999)), "Binh Duong"))
        assertEquals(GatePassDecision.Rejected(GateRejectReason.EXPIRED_PASS), validator(pass(issuedAt = now.minusSeconds(15)), "Binh Duong"))
    }

    @Test
    fun rejectsMalformedQrWithoutThrowing() {
        for (raw in listOf("", "v1", "v2.bad", pass().replaceRange(3, 5, "!!"), pass().substringBeforeLast('.') + ".!!")) {
            assertEquals(GatePassDecision.Rejected(GateRejectReason.INVALID_FORMAT), validator(raw, "Binh Duong"))
        }
    }

    @Test
    fun rejectsUnauthorizedFactoryAndInactiveEmployee() {
        assertEquals(GatePassDecision.Rejected(GateRejectReason.UNAUTHORIZED), validator(pass(), "Long An"))
        val denyAll = ValidateGatePass(serverKeys.public.encoded, Clock.fixed(now, ZoneOffset.UTC)) { _, _ -> false }
        assertEquals(GatePassDecision.Rejected(GateRejectReason.UNAUTHORIZED), denyAll(pass(), "Binh Duong"))
    }
}
