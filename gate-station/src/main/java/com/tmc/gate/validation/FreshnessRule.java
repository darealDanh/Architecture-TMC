package com.tmc.gate.validation;

import com.tmc.gate.access.RejectionReason;
import com.tmc.gate.config.ConfigReader;
import com.tmc.gate.credential.ScannedCredential;
import java.time.Instant;
import java.util.Optional;

public final class FreshnessRule implements ValidationRule {

    private final ConfigReader config;

    public FreshnessRule(ConfigReader config) {
        this.config = config;
    }

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        Instant issuedAt = credential.issuedAt();
        Instant latestAccepted = context.now().plus(config.clockTolerance());
        Instant earliestAccepted = context.now().minus(config.qrValidityWindow()).minus(config.clockTolerance());
        boolean fresh = !issuedAt.isAfter(latestAccepted) && !issuedAt.isBefore(earliestAccepted);
        return fresh ? Optional.empty() : Optional.of(RejectionReason.QR_EXPIRED);
    }
}
