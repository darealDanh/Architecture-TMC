package com.tmc.gate.sync;

import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.access.Direction;
import com.tmc.gate.access.GateIdentity;
import com.tmc.gate.alert.Alert;
import com.tmc.gate.alert.AlertType;
import com.tmc.gate.alert.Alerts;
import com.tmc.gate.config.GateSettings;
import com.tmc.gate.demo.InMemoryCentral;
import com.tmc.gate.demo.ServerAuthority;
import com.tmc.gate.store.LocalDatabase;
import com.tmc.gate.store.SqliteAccessEventLog;
import com.tmc.gate.store.SqliteReferenceData;
import com.tmc.gate.validation.ReferenceData;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SyncClientTest {

    private static final Instant T0 = Instant.parse("2026-11-07T07:00:00Z");
    private static final GateIdentity GATE = new GateIdentity("BD-GATE-01", "BINH_DUONG", Direction.ENTRY);

    @TempDir
    Path dir;

    private final MutableClock clock = new MutableClock(T0);
    private final Alerts alerts = new Alerts();
    private final ReferenceData centralData = new ReferenceData(
            new ServerAuthority().publicKey(), Set.of(), Set.of("E009"), Map.of("E001", "BINH_DUONG"));
    private final InMemoryCentral central = new InMemoryCentral(centralData);
    private int sequence;
    private LocalDatabase database;
    private SqliteAccessEventLog events;
    private SqliteReferenceData referenceData;

    @BeforeEach
    void open() {
        openStore();
    }

    @AfterEach
    void close() {
        database.close();
    }

    private void openStore() {
        database = LocalDatabase.open(dir.resolve("gate.db"));
        events = new SqliteAccessEventLog(database);
        referenceData = new SqliteReferenceData(database);
    }

    private SyncClient client(EventIntake intake, GateSettings settings) {
        return new SyncClient(GATE, events, referenceData, intake, central, central, alerts, settings, clock);
    }

    private SyncClient client() {
        return client(central, GateSettings.defaults());
    }

    private List<AccessEvent> appendEvents(int count) {
        List<AccessEvent> appended = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            AccessEvent event = AccessEvent.accepted(GATE, "E001", T0.plusMillis(sequence++));
            events.append(event);
            appended.add(event);
        }
        return appended;
    }

    @Test
    void uploadsAllPendingEventsInBatches() {
        appendEvents(250);

        SyncReport report = client().syncOnce();

        assertThat(report).isEqualTo(new SyncReport(true, 250));
        assertThat(central.storedEventCount()).isEqualTo(250);
        assertThat(events.pendingCount()).isZero();
    }

    @Test
    void connectionDropMidBatchLosesNothingAndDuplicatesNothing() {
        appendEvents(10);
        central.failAfter(4);
        SyncClient client = client();

        SyncReport first = client.syncOnce();

        assertThat(first.connected()).isFalse();
        assertThat(events.pendingCount()).isEqualTo(10);

        central.goOnline();
        client.syncOnce();

        assertThat(central.storedEventCount()).isEqualTo(10);
        assertThat(events.pendingCount()).isZero();
    }

    @Test
    void acknowledgedEventsAreNotResentAfterRestart() {
        appendEvents(5);
        RecordingIntake intake = new RecordingIntake(central);
        client(intake, GateSettings.defaults()).syncOnce();
        database.close();

        openStore();
        List<AccessEvent> added = appendEvents(2);
        client(intake, GateSettings.defaults()).syncOnce();

        assertThat(intake.submissions().get(1)).containsExactlyElementsOf(added.stream().map(AccessEvent::id).toList());
        assertThat(central.storedEventCount()).isEqualTo(7);
    }

    @Test
    void downloadsReferenceDataWhenOnline() {
        client().syncOnce();

        assertThat(referenceData.current().revokedEmployeeIds()).containsExactly("E009");
        assertThat(referenceData.current().activeEmployeeFactories()).containsEntry("E001", "BINH_DUONG");
    }

    @Test
    void offlineTooLongRaisesAlertAndReconnectResolvesIt() {
        SyncClient client = client();
        central.goOffline();
        clock.advance(Duration.ofHours(4).plusMinutes(1));

        client.syncOnce();
        assertThat(alerts.isActive(AlertType.GATE_OFFLINE_TOO_LONG)).isTrue();

        central.goOnline();
        client.syncOnce();
        assertThat(alerts.isActive(AlertType.GATE_OFFLINE_TOO_LONG)).isFalse();
    }

    @Test
    void shortOutageRaisesNoAlert() {
        SyncClient client = client();
        central.goOffline();
        clock.advance(Duration.ofHours(4));

        client.syncOnce();

        assertThat(alerts.isActive(AlertType.GATE_OFFLINE_TOO_LONG)).isFalse();
    }

    @Test
    void tooManyPendingEventsRaisesAlert() {
        GateSettings settings = new GateSettings(Duration.ofSeconds(30), Duration.ofSeconds(5), 2,
                Duration.ofSeconds(1), 100, Duration.ofHours(4), 3);
        central.goOffline();
        appendEvents(4);

        client(central, settings).syncOnce();

        assertThat(alerts.isActive(AlertType.PENDING_EVENTS_HIGH)).isTrue();
    }

    @Test
    void reportsGateStatusWithAlerts() {
        Alert unknownUnlock = new Alert(AlertType.UNLOCK_UNKNOWN, UUID.randomUUID().toString(), T0);
        alerts.raise(unknownUnlock);
        appendEvents(3);

        client().syncOnce();

        assertThat(central.statusReports()).hasSize(1);
        GateStatus status = central.statusReports().get(0);
        assertThat(status.gateId()).isEqualTo("BD-GATE-01");
        assertThat(status.pendingEvents()).isZero();
        assertThat(status.lastSuccessfulSync()).isEqualTo(T0);
        assertThat(status.alerts()).containsExactly(unknownUnlock);
    }

    private static final class RecordingIntake implements EventIntake {

        private final EventIntake delegate;
        private final List<List<UUID>> submissions = new ArrayList<>();

        RecordingIntake(EventIntake delegate) {
            this.delegate = delegate;
        }

        @Override
        public Set<UUID> submit(List<AccessEvent> batch) {
            submissions.add(batch.stream().map(AccessEvent::id).toList());
            return delegate.submit(batch);
        }

        List<List<UUID>> submissions() {
            return submissions;
        }
    }
}
