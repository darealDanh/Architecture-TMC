package com.tmc.gate.credential;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.tmc.gate.demo.Phone;
import com.tmc.gate.demo.ServerAuthority;
import java.time.Instant;
import java.util.Base64;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CredentialCodecTest {

    private static final Instant NOW = Instant.parse("2026-11-07T07:00:00Z");

    private final CredentialCodec codec = new CredentialCodec();

    @Test
    void roundTripPreservesEveryField() {
        ServerAuthority authority = new ServerAuthority();
        Phone phone = new Phone("E001", authority, NOW.plusSeconds(3600));
        ScannedCredential original = codec.decode(phone.showQr(NOW));

        ScannedCredential decoded = codec.decode(codec.encode(original));

        DeviceCertificate certificate = decoded.certificate();
        assertThat(certificate.serial()).isEqualTo(original.certificate().serial());
        assertThat(certificate.employeeId()).isEqualTo("E001");
        assertThat(certificate.devicePublicKey().getEncoded())
                .isEqualTo(phone.certificate().devicePublicKey().getEncoded());
        assertThat(certificate.expiresAt()).isEqualTo(NOW.plusSeconds(3600));
        assertThat(certificate.serverSignature()).isEqualTo(original.certificate().serverSignature());
        assertThat(decoded.issuedAt()).isEqualTo(NOW);
        assertThat(decoded.nonce()).isEqualTo(original.nonce());
        assertThat(decoded.deviceSignature()).isEqualTo(original.deviceSignature());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "v1.a.b", "v2.a.b.c.d.e.f.g.h", "v1.!!.b.c.d.e.f.g.h"})
    void rejectsMalformedInput(String raw) {
        assertThatThrownBy(() -> codec.decode(raw)).isInstanceOf(MalformedCredentialException.class);
    }

    @Test
    void rejectsNonNumericTimestamp() {
        ServerAuthority authority = new ServerAuthority();
        Phone phone = new Phone("E001", authority, NOW.plusSeconds(3600));
        String[] segments = phone.showQr(NOW).split(Pattern.quote("."));
        segments[6] = Base64.getUrlEncoder().withoutPadding().encodeToString("soon".getBytes());

        assertThatThrownBy(() -> codec.decode(String.join(".", segments)))
                .isInstanceOf(MalformedCredentialException.class);
    }
}
