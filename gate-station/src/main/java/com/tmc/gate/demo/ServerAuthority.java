package com.tmc.gate.demo;

import com.tmc.gate.credential.DeviceCertificate;
import com.tmc.gate.credential.Ed25519;
import java.security.KeyPair;
import java.security.PublicKey;
import java.time.Instant;
import java.util.UUID;

public final class ServerAuthority {

    private final KeyPair keyPair = Ed25519.generateKeyPair();

    public PublicKey publicKey() {
        return keyPair.getPublic();
    }

    public DeviceCertificate certify(String employeeId, PublicKey devicePublicKey, Instant expiresAt) {
        DeviceCertificate unsigned = new DeviceCertificate(
                UUID.randomUUID().toString(), employeeId, devicePublicKey, expiresAt, new byte[0]);
        return unsigned.withServerSignature(Ed25519.sign(keyPair.getPrivate(), unsigned.signedContent()));
    }
}
