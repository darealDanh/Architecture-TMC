package com.tmc.gate.sync;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.access.GateIdentity;
import com.tmc.gate.alert.Alert;
import com.tmc.gate.alert.AlertType;
import com.tmc.gate.alert.Alerts;
import com.tmc.gate.config.ConfigReader;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class SyncClient {

    private final GateIdentity gate;
    private final Outbox outbox;
    private final ReferenceDataWriter referenceData;
    private final EventIntake intake;
    private final ReferenceDataSource source;
    private final GateStatusReceiver statusReceiver;
    private final Alerts alerts;
    private final ConfigReader config;
    private final Clock clock;
    private Instant lastSuccessfulSync;

    public SyncClient(
            GateIdentity gate,
            Outbox outbox,
            ReferenceDataWriter referenceData,
            EventIntake intake,
            ReferenceDataSource source,
            GateStatusReceiver statusReceiver,
            Alerts alerts,
            ConfigReader config,
            Clock clock) {
        this.gate = gate;
        this.outbox = outbox;
        this.referenceData = referenceData;
        this.intake = intake;
        this.source = source;
        this.statusReceiver = statusReceiver;
        this.alerts = alerts;
        this.config = config;
        this.clock = clock;
        this.lastSuccessfulSync = clock.instant();
    }

    public SyncReport syncOnce() {
        SyncReport upload = uploadPending();
        boolean downloaded = downloadReferenceData();
        Instant now = clock.instant();
        boolean reachedCentral = upload.connected() && downloaded;
        if (reachedCentral) {
            lastSuccessfulSync = now;
        }
        checkOfflineDuration(now);
        checkPendingEvents(now);
        boolean connected = reachedCentral && reportStatus(now);
        return new SyncReport(connected, upload.uploaded());
    }

    private SyncReport uploadPending() {
        int uploaded = 0;
        try {
            List<AccessEvent> batch = outbox.pending(config.syncBatchSize());
            while (!batch.isEmpty()) {
                Set<UUID> acknowledged = acknowledgedFromBatch(batch, intake.submit(batch));
                if (acknowledged.isEmpty()) {
                    return new SyncReport(true, uploaded);
                }
                outbox.markSynced(acknowledged);
                uploaded += acknowledged.size();
                batch = outbox.pending(config.syncBatchSize());
            }
            return new SyncReport(true, uploaded);
        } catch (IntakeUnavailableException e) {
            return new SyncReport(false, uploaded);
        }
    }

    private static Set<UUID> acknowledgedFromBatch(List<AccessEvent> batch, Set<UUID> acknowledged) {
        Set<UUID> inBatch = new HashSet<>();
        for (AccessEvent event : batch) {
            if (acknowledged.contains(event.id())) {
                inBatch.add(event.id());
            }
        }
        return inBatch;
    }

    private boolean downloadReferenceData() {
        try {
            referenceData.replace(source.fetch());
            return true;
        } catch (IntakeUnavailableException e) {
            return false;
        }
    }

    private void checkOfflineDuration(Instant now) {
        Duration offline = Duration.between(lastSuccessfulSync, now);
        if (offline.compareTo(config.maxOfflineDuration()) > 0) {
            alerts.raise(new Alert(AlertType.GATE_OFFLINE_TOO_LONG,
                    "No sync since " + lastSuccessfulSync, now));
        } else {
            alerts.resolve(AlertType.GATE_OFFLINE_TOO_LONG);
        }
    }

    private void checkPendingEvents(Instant now) {
        int pending = outbox.pendingCount();
        alerts.resolve(AlertType.PENDING_EVENTS_HIGH);
        if (pending > config.pendingEventsAlertThreshold()) {
            alerts.raise(new Alert(AlertType.PENDING_EVENTS_HIGH, pending + " events waiting to sync", now));
        }
    }

    private boolean reportStatus(Instant now) {
        GateStatus status = new GateStatus(gate.gateId(), now, lastSuccessfulSync, outbox.pendingCount(), alerts.active());
        try {
            statusReceiver.report(status);
            return true;
        } catch (IntakeUnavailableException e) {
            return false;
        }
    }
}
