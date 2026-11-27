package com.tmc.mobile.domain.gate

import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.pass.GeneratePass
import com.tmc.mobile.domain.pass.PassPayloadCodec
import java.time.Clock
import java.time.Duration
import java.time.DateTimeException

class ValidateGatePass(
    private val serverPublicKey: ByteArray,
    private val clock: Clock,
    private val authorizedForFactory: (String, String) -> Boolean,
) {
    operator fun invoke(raw: String, factory: String): GatePassDecision {
        val pass = try {
            PassPayloadCodec.decode(raw)
        } catch (e: IllegalArgumentException) {
            return GatePassDecision.Rejected(GateRejectReason.INVALID_FORMAT)
        } catch (e: DateTimeException) {
            return GatePassDecision.Rejected(GateRejectReason.INVALID_FORMAT)
        }
        val certificate = pass.certificate
        if (!EcdsaP256.verify(serverPublicKey, certificate.signedContent(), certificate.serverSignature)) {
            return GatePassDecision.Rejected(GateRejectReason.INVALID_CERTIFICATE)
        }
        val now = clock.instant()
        if (!certificate.isValidAt(now)) {
            return GatePassDecision.Rejected(GateRejectReason.EXPIRED_CERTIFICATE)
        }
        if (!EcdsaP256.verify(
                certificate.devicePublicKey,
                PassPayloadCodec.signedContent(certificate, pass.issuedAt, pass.nonce),
                pass.deviceSignature,
            )
        ) {
            return GatePassDecision.Rejected(GateRejectReason.INVALID_SIGNATURE)
        }
        if (pass.issuedAt.isAfter(now.plus(FUTURE_TOLERANCE)) ||
            !now.isBefore(pass.issuedAt.plus(GeneratePass.REFRESH_INTERVAL))
        ) {
            return GatePassDecision.Rejected(GateRejectReason.EXPIRED_PASS)
        }
        if (!authorizedForFactory(certificate.employeeId, factory)) {
            return GatePassDecision.Rejected(GateRejectReason.UNAUTHORIZED)
        }
        return GatePassDecision.Allowed(certificate.employeeId, pass.nonce, certificate.serial)
    }

    private companion object {
        val FUTURE_TOLERANCE: Duration = Duration.ofSeconds(2)
    }
}

sealed interface GatePassDecision {
    data class Allowed(val employeeId: String, val nonce: String, val certificateSerial: String) : GatePassDecision
    data class Rejected(val reason: GateRejectReason) : GatePassDecision
}

enum class GateRejectReason {
    INVALID_FORMAT,
    INVALID_CERTIFICATE,
    EXPIRED_CERTIFICATE,
    INVALID_SIGNATURE,
    EXPIRED_PASS,
    UNAUTHORIZED,
}
