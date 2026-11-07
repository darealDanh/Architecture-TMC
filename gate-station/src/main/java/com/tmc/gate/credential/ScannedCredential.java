package com.tmc.gate.credential;

import java.time.Instant;

public record ScannedCredential(
        DeviceCertificate certificate,
        Instant issuedAt,
        String nonce,
        byte[] deviceSignature) {

    public byte[] signedContent() {
        return Segments.join(contentSegments());
    }

    String[] contentSegments() {
        String[] certificateSegments = certificate.contentSegments();
        return new String[] {
            certificateSegments[0],
            certificateSegments[1],
            certificateSegments[2],
            certificateSegments[3],
            Segments.ofBytes(certificate.serverSignature()),
            Segments.ofText(Long.toString(issuedAt.toEpochMilli())),
            nonce
        };
    }

    public ScannedCredential withDeviceSignature(byte[] signature) {
        return new ScannedCredential(certificate, issuedAt, nonce, signature);
    }
}
