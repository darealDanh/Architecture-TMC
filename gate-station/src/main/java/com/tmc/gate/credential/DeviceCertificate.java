package com.tmc.gate.credential;

import java.security.PublicKey;
import java.time.Instant;

public record DeviceCertificate(
        String serial,
        String employeeId,
        PublicKey devicePublicKey,
        Instant expiresAt,
        byte[] serverSignature) {

    public byte[] signedContent() {
        return Segments.join(contentSegments());
    }

    String[] contentSegments() {
        return new String[] {
            Segments.ofText(serial),
            Segments.ofText(employeeId),
            Segments.ofBytes(devicePublicKey.getEncoded()),
            Segments.ofText(Long.toString(expiresAt.getEpochSecond()))
        };
    }

    public DeviceCertificate withServerSignature(byte[] signature) {
        return new DeviceCertificate(serial, employeeId, devicePublicKey, expiresAt, signature);
    }
}
