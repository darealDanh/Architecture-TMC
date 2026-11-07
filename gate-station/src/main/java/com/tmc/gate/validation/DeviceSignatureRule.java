package com.tmc.gate.validation;

import com.tmc.gate.credential.Ed25519;
import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

public final class DeviceSignatureRule implements ValidationRule {

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        boolean signedByDevice = Ed25519.verify(
                credential.certificate().devicePublicKey(), credential.signedContent(), credential.deviceSignature());
        return signedByDevice ? Optional.empty() : Optional.of(RejectionReason.INVALID_SIGNATURE);
    }
}
