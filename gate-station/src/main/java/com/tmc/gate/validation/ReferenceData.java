package com.tmc.gate.validation;

import java.security.PublicKey;
import java.util.Map;
import java.util.Set;

public record ReferenceData(
        PublicKey serverPublicKey,
        Set<String> revokedCertificateSerials,
        Set<String> revokedEmployeeIds,
        Map<String, String> activeEmployeeFactories) {

    public ReferenceData {
        revokedCertificateSerials = Set.copyOf(revokedCertificateSerials);
        revokedEmployeeIds = Set.copyOf(revokedEmployeeIds);
        activeEmployeeFactories = Map.copyOf(activeEmployeeFactories);
    }

    public static ReferenceData empty() {
        return new ReferenceData(null, Set.of(), Set.of(), Map.of());
    }

    public boolean hasServerKey() {
        return serverPublicKey != null;
    }
}
