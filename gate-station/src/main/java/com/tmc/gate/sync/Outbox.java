package com.tmc.gate.sync;

import com.tmc.gate.access.AccessEvent;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface Outbox {

    List<AccessEvent> pending(int limit);

    void markSynced(Collection<UUID> eventIds);

    int pendingCount();
}
