package com.tmc.gate.validation;

import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

public final class FactoryAuthorisationRule implements ValidationRule {

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        String employeeFactory = context.referenceData().activeEmployeeFactories()
                .get(credential.certificate().employeeId());
        boolean authorised = context.factoryId().equals(employeeFactory);
        return authorised ? Optional.empty() : Optional.of(RejectionReason.NOT_AUTHORISED);
    }
}
