package com.tmc.gate.access;

import com.tmc.gate.validation.RejectionReason;
import java.time.Instant;
import java.util.UUID;

public record AccessEvent(
        UUID id,
        String gateId,
        String factoryId,
        Direction direction,
        String employeeId,
        Instant occurredAt,
        AccessResult result,
        RejectionReason rejectionReason,
        UnlockStatus unlockStatus) {

    public static AccessEvent accepted(GateIdentity gate, String employeeId, Instant at) {
        return new AccessEvent(UUID.randomUUID(), gate.gateId(), gate.factoryId(), gate.direction(),
                employeeId, at, AccessResult.ACCEPTED, null, UnlockStatus.PENDING);
    }

    public static AccessEvent rejected(GateIdentity gate, String employeeId, RejectionReason reason, Instant at) {
        return new AccessEvent(UUID.randomUUID(), gate.gateId(), gate.factoryId(), gate.direction(),
                employeeId, at, AccessResult.REJECTED, reason, UnlockStatus.NOT_REQUESTED);
    }

    public AccessEvent withUnlockStatus(UnlockStatus status) {
        return new AccessEvent(id, gateId, factoryId, direction, employeeId, occurredAt, result, rejectionReason, status);
    }
}
