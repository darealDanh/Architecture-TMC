package com.tmc.gate.validation;

import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

public final class CertificateExpiryRule implements ValidationRule {

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        boolean expired = !context.now().isBefore(credential.certificate().expiresAt());
        return expired ? Optional.of(RejectionReason.CERTIFICATE_EXPIRED) : Optional.empty();
    }
}
