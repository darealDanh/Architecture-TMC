package com.tmc.gate.validation;

import com.tmc.gate.credential.CredentialCodec;
import com.tmc.gate.credential.ScannedCredential;
import com.tmc.gate.demo.Phone;
import com.tmc.gate.demo.ServerAuthority;
import java.time.Instant;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

final class ValidationFixture {

    static final Instant NOW = Instant.parse("2026-11-07T07:00:00Z");
    static final String FACTORY = "BINH_DUONG";

    final ServerAuthority authority = new ServerAuthority();
    final Phone phone = new Phone("E001", authority, NOW.plusSeconds(3600));
    final CredentialCodec codec = new CredentialCodec();

    ScannedCredential credentialIssuedAt(Instant issuedAt) {
        return codec.decode(phone.showQr(issuedAt));
    }

    ScannedCredential credential() {
        return credentialIssuedAt(NOW);
    }

    ReferenceData referenceData() {
        return new ReferenceData(authority.publicKey(), Set.of(), Set.of(), Map.of("E001", "BINH_DUONG"));
    }

    ValidationContext context(ReferenceData referenceData) {
        return new ValidationContext(referenceData, FACTORY, NOW);
    }

    ValidationContext context() {
        return context(referenceData());
    }

    static final class InMemoryNonces implements NonceRegistry {

        private final Set<String> used = new HashSet<>();

        @Override
        public boolean isUsed(String nonce) {
            return used.contains(nonce);
        }

        @Override
        public void markUsed(String nonce) {
            used.add(nonce);
        }
    }
}
