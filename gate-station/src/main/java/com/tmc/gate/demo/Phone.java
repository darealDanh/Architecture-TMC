package com.tmc.gate.demo;

import com.tmc.gate.credential.CredentialCodec;
import com.tmc.gate.credential.DeviceCertificate;
import com.tmc.gate.credential.Ed25519;
import com.tmc.gate.credential.ScannedCredential;
import java.security.KeyPair;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

public final class Phone {

    private static final int NONCE_BYTES = 16;

    private final KeyPair deviceKeys = Ed25519.generateKeyPair();
    private final SecureRandom random = new SecureRandom();
    private final CredentialCodec codec = new CredentialCodec();
    private final DeviceCertificate certificate;

    public Phone(String employeeId, ServerAuthority authority, Instant certificateExpiry) {
        this.certificate = authority.certify(employeeId, deviceKeys.getPublic(), certificateExpiry);
    }

    public DeviceCertificate certificate() {
        return certificate;
    }

    public String showQr(Instant now) {
        ScannedCredential unsigned = new ScannedCredential(certificate, now, newNonce(), new byte[0]);
        byte[] signature = Ed25519.sign(deviceKeys.getPrivate(), unsigned.signedContent());
        return codec.encode(unsigned.withDeviceSignature(signature));
    }

    private String newNonce() {
        byte[] bytes = new byte[NONCE_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
