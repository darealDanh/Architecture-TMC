package com.tmc.gate.demo;

import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.credential.CredentialCodec;
import com.tmc.gate.credential.DeviceCertificate;
import com.tmc.gate.credential.Ed25519;
import com.tmc.gate.credential.ScannedCredential;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class PhoneTest {

    private static final Instant NOW = Instant.parse("2026-11-07T07:00:00Z");

    private final CredentialCodec codec = new CredentialCodec();
    private final ServerAuthority authority = new ServerAuthority();
    private final Phone phone = new Phone("E001", authority, NOW.plusSeconds(3600));

    @Test
    void qrCodesAreSignedByTheDeviceAndCertifiedByTheServer() {
        ScannedCredential credential = codec.decode(phone.showQr(NOW));
        DeviceCertificate certificate = credential.certificate();

        assertThat(Ed25519.verify(authority.publicKey(), certificate.signedContent(), certificate.serverSignature()))
                .isTrue();
        assertThat(Ed25519.verify(certificate.devicePublicKey(), credential.signedContent(), credential.deviceSignature()))
                .isTrue();
    }

    @Test
    void everyQrHasAFreshNonce() {
        ScannedCredential first = codec.decode(phone.showQr(NOW));
        ScannedCredential second = codec.decode(phone.showQr(NOW));

        assertThat(first.nonce()).isNotEqualTo(second.nonce());
    }
}
