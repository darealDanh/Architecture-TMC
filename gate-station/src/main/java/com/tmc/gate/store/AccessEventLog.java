package com.tmc.gate.store;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.access.UnlockStatus;
import java.util.Optional;
import java.util.UUID;

public interface AccessEventLog {

    void append(AccessEvent event);

    void updateUnlockStatus(UUID eventId, UnlockStatus status);

    Optional<AccessEvent> find(UUID eventId);
}
