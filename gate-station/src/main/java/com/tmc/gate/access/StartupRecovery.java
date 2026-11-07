package com.tmc.gate.access;

import com.tmc.gate.alert.Alert;
import com.tmc.gate.alert.AlertSink;
import com.tmc.gate.alert.AlertType;
import java.time.Clock;
import java.util.UUID;

public final class StartupRecovery {

    private final InterruptedUnlocks interrupted;
    private final AlertSink alerts;
    private final Clock clock;

    public StartupRecovery(InterruptedUnlocks interrupted, AlertSink alerts, Clock clock) {
        this.interrupted = interrupted;
        this.alerts = alerts;
        this.clock = clock;
    }

    public void run() {
        for (UUID eventId : interrupted.resolveAsUnknown()) {
            alerts.raise(new Alert(AlertType.UNLOCK_UNKNOWN, eventId.toString(), clock.instant()));
        }
    }
}
