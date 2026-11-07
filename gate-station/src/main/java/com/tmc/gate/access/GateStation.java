package com.tmc.gate.access;

import com.tmc.gate.alert.Alert;
import com.tmc.gate.alert.AlertSink;
import com.tmc.gate.alert.AlertType;
import com.tmc.gate.credential.CredentialCodec;
import com.tmc.gate.credential.MalformedCredentialException;
import com.tmc.gate.credential.ScannedCredential;
import com.tmc.gate.turnstile.TurnstileGateway;
import com.tmc.gate.turnstile.UnlockResult;
import com.tmc.gate.validation.NonceRegistry;
import com.tmc.gate.validation.QrValidator;
import com.tmc.gate.validation.RejectionReason;
import com.tmc.gate.validation.ValidationContext;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

public final class GateStation {

    private final GateIdentity gate;
    private final CredentialCodec codec;
    private final QrValidator validator;
    private final ReferenceDataReader referenceData;
    private final NonceRegistry nonces;
    private final AccessEventLog events;
    private final TurnstileGateway turnstile;
    private final AlertSink alerts;
    private final Clock clock;

    public GateStation(
            GateIdentity gate,
            CredentialCodec codec,
            QrValidator validator,
            ReferenceDataReader referenceData,
            NonceRegistry nonces,
            AccessEventLog events,
            TurnstileGateway turnstile,
            AlertSink alerts,
            Clock clock) {
        this.gate = gate;
        this.codec = codec;
        this.validator = validator;
        this.referenceData = referenceData;
        this.nonces = nonces;
        this.events = events;
        this.turnstile = turnstile;
        this.alerts = alerts;
        this.clock = clock;
    }

    public AccessEvent handleScan(String rawQr) {
        Instant now = clock.instant();
        ScannedCredential credential;
        try {
            credential = codec.decode(rawQr);
        } catch (MalformedCredentialException e) {
            return record(AccessEvent.rejected(gate, null, RejectionReason.MALFORMED, now));
        }
        String employeeId = credential.certificate().employeeId();
        Optional<RejectionReason> violation =
                validator.firstViolation(credential, new ValidationContext(referenceData.current(), gate.factoryId(), now));
        if (violation.isPresent()) {
            return record(AccessEvent.rejected(gate, employeeId, violation.get(), now));
        }
        nonces.markUsed(credential.nonce());
        return unlock(record(AccessEvent.accepted(gate, employeeId, now)));
    }

    private AccessEvent record(AccessEvent event) {
        events.append(event);
        return event;
    }

    private AccessEvent unlock(AccessEvent event) {
        UnlockStatus status = toStatus(requestUnlock(event));
        events.updateUnlockStatus(event.id(), status);
        if (status == UnlockStatus.UNLOCK_UNKNOWN) {
            alerts.raise(new Alert(AlertType.UNLOCK_UNKNOWN, event.id().toString(), clock.instant()));
        }
        return event.withUnlockStatus(status);
    }

    private UnlockResult requestUnlock(AccessEvent event) {
        try {
            return turnstile.unlock(gate.gateId(), event.id());
        } catch (RuntimeException e) {
            return UnlockResult.UNKNOWN;
        }
    }

    private static UnlockStatus toStatus(UnlockResult result) {
        return switch (result) {
            case UNLOCKED -> UnlockStatus.UNLOCKED;
            case FAILED -> UnlockStatus.UNLOCK_FAILED;
            case UNKNOWN -> UnlockStatus.UNLOCK_UNKNOWN;
        };
    }
}
