package com.tmc.gate.demo;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.sync.EventIntake;
import com.tmc.gate.sync.GateStatus;
import com.tmc.gate.sync.GateStatusReceiver;
import com.tmc.gate.sync.IntakeUnavailableException;
import com.tmc.gate.sync.ReferenceDataSource;
import com.tmc.gate.validation.ReferenceData;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class InMemoryCentral implements EventIntake, ReferenceDataSource, GateStatusReceiver {

    private static final int NO_FAILURE = -1;

    private final ReferenceData referenceData;
    private final Map<UUID, AccessEvent> stored = new LinkedHashMap<>();
    private final List<GateStatus> statusReports = new ArrayList<>();
    private boolean online = true;
    private int acceptBeforeFailure = NO_FAILURE;

    public InMemoryCentral(ReferenceData referenceData) {
        this.referenceData = referenceData;
    }

    public synchronized void goOffline() {
        online = false;
    }

    public synchronized void goOnline() {
        online = true;
        acceptBeforeFailure = NO_FAILURE;
    }

    public synchronized void failAfter(int acceptedEvents) {
        acceptBeforeFailure = acceptedEvents;
    }

    @Override
    public synchronized Set<UUID> submit(List<AccessEvent> events) {
        requireOnline();
        Set<UUID> acknowledged = new HashSet<>();
        for (AccessEvent event : events) {
            if (acceptBeforeFailure == 0) {
                online = false;
                throw new IntakeUnavailableException("Connection lost before acknowledgement");
            }
            stored.putIfAbsent(event.id(), event);
            acknowledged.add(event.id());
            if (acceptBeforeFailure > 0) {
                acceptBeforeFailure--;
            }
        }
        return acknowledged;
    }

    @Override
    public synchronized ReferenceData fetch() {
        requireOnline();
        return referenceData;
    }

    @Override
    public synchronized void report(GateStatus status) {
        requireOnline();
        statusReports.add(status);
    }

    public synchronized int storedEventCount() {
        return stored.size();
    }

    public synchronized List<GateStatus> statusReports() {
        return List.copyOf(statusReports);
    }

    private void requireOnline() {
        if (!online) {
            throw new IntakeUnavailableException("Central system unreachable");
        }
    }
}
