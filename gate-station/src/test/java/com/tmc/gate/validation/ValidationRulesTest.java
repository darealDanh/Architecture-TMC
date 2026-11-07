package com.tmc.gate.validation;

import static com.tmc.gate.validation.ValidationFixture.NOW;
import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.access.RejectionReason;
import com.tmc.gate.config.GateSettings;
import com.tmc.gate.credential.ScannedCredential;
import com.tmc.gate.demo.Phone;
import com.tmc.gate.demo.ServerAuthority;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class ValidationRulesTest {

    private final ValidationFixture fixture = new ValidationFixture();

    @Nested
    class CertificateSignature {

        private final ValidationRule rule = new CertificateSignatureRule();

        @Test
        void acceptsCertificateSignedByTheServer() {
            assertThat(rule.check(fixture.credential(), fixture.context())).isEmpty();
        }

        @Test
        void rejectsCertificateSignedByAnotherServer() {
            Phone impostor = new Phone("E001", new ServerAuthority(), NOW.plusSeconds(3600));
            ScannedCredential forged = fixture.codec.decode(impostor.showQr(NOW));

            assertThat(rule.check(forged, fixture.context())).contains(RejectionReason.INVALID_CERTIFICATE);
        }

        @Test
        void rejectsEverythingBeforeReferenceDataIsDownloaded() {
            assertThat(rule.check(fixture.credential(), fixture.context(ReferenceData.empty())))
                    .contains(RejectionReason.INVALID_CERTIFICATE);
        }
    }

    @Nested
    class CertificateExpiry {

        private final ValidationRule rule = new CertificateExpiryRule();

        @Test
        void acceptsUnexpiredCertificate() {
            assertThat(rule.check(fixture.credential(), fixture.context())).isEmpty();
        }

        @Test
        void rejectsExpiredCertificate() {
            Phone phone = new Phone("E001", fixture.authority, NOW.minusSeconds(1));
            ScannedCredential credential = fixture.codec.decode(phone.showQr(NOW));

            assertThat(rule.check(credential, fixture.context())).contains(RejectionReason.CERTIFICATE_EXPIRED);
        }
    }

    @Nested
    class DeviceSignature {

        private final ValidationRule rule = new DeviceSignatureRule();

        @Test
        void acceptsCodeSignedByTheCertifiedDevice() {
            assertThat(rule.check(fixture.credential(), fixture.context())).isEmpty();
        }

        @Test
        void rejectsTamperedSignature() {
            ScannedCredential credential = fixture.credential();
            byte[] tampered = credential.deviceSignature().clone();
            tampered[0] ^= 1;

            assertThat(rule.check(credential.withDeviceSignature(tampered), fixture.context()))
                    .contains(RejectionReason.INVALID_SIGNATURE);
        }
    }

    @Nested
    class Freshness {

        private final ValidationRule rule = new FreshnessRule(GateSettings.defaults());

        @Test
        void acceptsCodeAtTheEndOfWindowPlusTolerance() {
            assertThat(rule.check(fixture.credentialIssuedAt(NOW.minusSeconds(35)), fixture.context())).isEmpty();
        }

        @Test
        void rejectsCodeOlderThanWindowPlusTolerance() {
            assertThat(rule.check(fixture.credentialIssuedAt(NOW.minusSeconds(36)), fixture.context()))
                    .contains(RejectionReason.QR_EXPIRED);
        }

        @Test
        void acceptsPhoneClockAheadWithinTolerance() {
            assertThat(rule.check(fixture.credentialIssuedAt(NOW.plusSeconds(5)), fixture.context())).isEmpty();
        }

        @Test
        void rejectsPhoneClockAheadBeyondTolerance() {
            assertThat(rule.check(fixture.credentialIssuedAt(NOW.plusSeconds(6)), fixture.context()))
                    .contains(RejectionReason.QR_EXPIRED);
        }
    }

    @Nested
    class Revocation {

        private final ValidationRule rule = new RevocationRule();

        @Test
        void acceptsCredentialThatIsNotRevoked() {
            assertThat(rule.check(fixture.credential(), fixture.context())).isEmpty();
        }

        @Test
        void rejectsRevokedCertificate() {
            ScannedCredential credential = fixture.credential();
            ReferenceData data = new ReferenceData(fixture.authority.publicKey(),
                    Set.of(credential.certificate().serial()), Set.of(), Map.of("E001", "BINH_DUONG"));

            assertThat(rule.check(credential, fixture.context(data))).contains(RejectionReason.REVOKED);
        }

        @Test
        void rejectsRevokedEmployee() {
            ReferenceData data = new ReferenceData(fixture.authority.publicKey(),
                    Set.of(), Set.of("E001"), Map.of("E001", "BINH_DUONG"));

            assertThat(rule.check(fixture.credential(), fixture.context(data))).contains(RejectionReason.REVOKED);
        }
    }

    @Nested
    class FactoryAuthorisation {

        private final ValidationRule rule = new FactoryAuthorisationRule();

        @Test
        void acceptsActiveEmployeeOfThisFactory() {
            assertThat(rule.check(fixture.credential(), fixture.context())).isEmpty();
        }

        @Test
        void rejectsEmployeeOfAnotherFactory() {
            ReferenceData data = new ReferenceData(fixture.authority.publicKey(),
                    Set.of(), Set.of(), Map.of("E001", "LONG_AN"));

            assertThat(rule.check(fixture.credential(), fixture.context(data)))
                    .contains(RejectionReason.NOT_AUTHORISED);
        }

        @Test
        void rejectsUnknownOrInactiveEmployee() {
            ReferenceData data = new ReferenceData(fixture.authority.publicKey(), Set.of(), Set.of(), Map.of());

            assertThat(rule.check(fixture.credential(), fixture.context(data)))
                    .contains(RejectionReason.NOT_AUTHORISED);
        }
    }

    @Nested
    class Replay {

        private final ValidationFixture.InMemoryNonces nonces = new ValidationFixture.InMemoryNonces();
        private final ValidationRule rule = new ReplayRule(nonces);

        @Test
        void acceptsUnusedNonce() {
            assertThat(rule.check(fixture.credential(), fixture.context())).isEmpty();
        }

        @Test
        void rejectsNonceThatWasAlreadyUsed() {
            ScannedCredential credential = fixture.credential();
            nonces.markUsed(credential.nonce());

            Optional<RejectionReason> result = rule.check(credential, fixture.context());

            assertThat(result).contains(RejectionReason.REPLAYED);
        }
    }
}
