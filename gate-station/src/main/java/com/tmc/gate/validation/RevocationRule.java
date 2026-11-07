package com.tmc.gate.validation;

import com.tmc.gate.credential.DeviceCertificate;
import com.tmc.gate.credential.ScannedCredential;
import java.util.Optional;

public final class RevocationRule implements ValidationRule {

    @Override
    public Optional<RejectionReason> check(ScannedCredential credential, ValidationContext context) {
        ReferenceData data = context.referenceData();
        DeviceCertificate certificate = credential.certificate();
        boolean revoked = data.revokedCertificateSerials().contains(certificate.serial())
                || data.revokedEmployeeIds().contains(certificate.employeeId());
        return revoked ? Optional.of(RejectionReason.REVOKED) : Optional.empty();
    }
}
