package com.tmc.gate.validation;

import com.tmc.gate.access.Direction;
import com.tmc.gate.access.GateIdentity;
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
    static final GateIdentity GATE = new GateIdentity("BD-GATE-01", "BINH_DUONG", Direction.ENTRY);

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
        return new ValidationContext(referenceData, GATE, NOW);
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
