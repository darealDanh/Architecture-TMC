package com.tmc.gate.validation;

import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

public final class ReplayRule implements ValidationRule {

    private final NonceRegistry nonces;

    public ReplayRule(NonceRegistry nonces) {
        this.nonces = nonces;
    }

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        return nonces.isUsed(credential.nonce()) ? Optional.of(RejectionReason.REPLAYED) : Optional.empty();
    }
}
