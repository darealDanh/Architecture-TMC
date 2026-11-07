package com.tmc.gate.validation;

import com.tmc.gate.access.RejectionReason;
import com.tmc.gate.credential.DeviceCertificate;
import com.tmc.gate.credential.Ed25519;
import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

public final class CertificateSignatureRule implements ValidationRule {

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        ReferenceData data = context.referenceData();
        DeviceCertificate certificate = credential.certificate();
        boolean trusted = data.hasServerKey()
                && Ed25519.verify(data.serverPublicKey(), certificate.signedContent(), certificate.serverSignature());
        return trusted ? Optional.empty() : Optional.of(RejectionReason.INVALID_CERTIFICATE);
    }
}
