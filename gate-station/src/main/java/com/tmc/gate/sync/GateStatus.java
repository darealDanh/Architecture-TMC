package com.tmc.gate.sync;

import com.tmc.gate.alert.Alert;
import java.time.Instant;
import java.util.List;

public record GateStatus(
        String gateId,
        Instant reportedAt,
        Instant lastSuccessfulSync,
        int pendingEvents,
        List<Alert> alerts) {

    public GateStatus {
        alerts = List.copyOf(alerts);
    }
}
