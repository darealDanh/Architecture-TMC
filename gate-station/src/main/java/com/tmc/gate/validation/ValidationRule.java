package com.tmc.gate.validation;

import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

@FunctionalInterface
public interface ValidationRule {

    Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context);
}
