package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.gate.GatePassDecision
import com.tmc.mobile.domain.gate.GateRejectReason
import com.tmc.mobile.domain.gate.ValidateGatePass
import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.pass.PassPayloadCodec
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FakeGateDirectoryTest {
    private val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
    private val server = FakeTmcServer(MutableNetworkStatus(), clock, Duration.ZERO)
    private val validator = ValidateGatePass(FakeGateDirectory.publicKey, clock, FakeGateDirectory::isAuthorized)

    @Test
    fun realServerCertificateOpensOnlyEmployeesFactory() = runTest {
        for ((employee, factory) in listOf("E001" to "Binh Duong", "E002" to "Long An")) {
            val device = EcdsaP256.generateKeyPair()
            val token = server.signIn(employee, "password").token
            val certificate = server.issueCertificate(token, device.public.encoded)
            val nonce = "bm9uY2U"
            val signature = EcdsaP256.sign(device.private, PassPayloadCodec.signedContent(certificate, clock.instant(), nonce))
            val raw = PassPayloadCodec.encode(certificate, clock.instant(), nonce, signature)

            assertEquals(GatePassDecision.Allowed(employee, nonce, certificate.serial), validator(raw, factory))
            assertEquals(GatePassDecision.Rejected(GateRejectReason.UNAUTHORIZED), validator(raw, "Unknown Factory"))
        }
        assertEquals(false, FakeGateDirectory.isAuthorized("E999", "Binh Duong"))
    }
}
