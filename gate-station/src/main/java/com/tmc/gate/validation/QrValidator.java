package com.tmc.gate.validation;

import com.tmc.gate.access.RejectionReason;
import com.tmc.gate.config.ConfigReader;
import com.tmc.gate.credential.ScannedCredential;
import java.util.List;
import java.util.Optional;

public final class QrValidator {

    private final List<ValidationRule> rules;

    public QrValidator(List<ValidationRule> rules) {
        this.rules = List.copyOf(rules);
    }

    public static QrValidator standard(ConfigReader config, NonceRegistry nonces) {
        return new QrValidator(List.of(
                new CertificateSignatureRule(),
                new CertificateExpiryRule(),
                new DeviceSignatureRule(),
                new FreshnessRule(config),
                new RevocationRule(),
                new FactoryAuthorisationRule(),
                new ReplayRule(nonces)));
    }

    public List<ValidationRule> rules() {
        return rules;
    }

    public Optional<RejectionReason> firstViolation(ScannedCredential credential, ValidationContext context) {
        return rules.stream()
                .map(rule -> rule.check(credential, context))
                .flatMap(Optional::stream)
                .findFirst();
    }
}
