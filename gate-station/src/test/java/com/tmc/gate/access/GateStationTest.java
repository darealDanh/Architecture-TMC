package com.tmc.gate.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.alert.AlertType;
import com.tmc.gate.alert.Alerts;
import com.tmc.gate.config.GateSettings;
import com.tmc.gate.credential.CredentialCodec;
import com.tmc.gate.demo.Phone;
import com.tmc.gate.demo.ServerAuthority;
import com.tmc.gate.store.LocalDatabase;
import com.tmc.gate.store.SqliteAccessEventLog;
import com.tmc.gate.store.SqliteNonceRegistry;
import com.tmc.gate.store.SqliteReferenceData;
import com.tmc.gate.turnstile.SimulatedTurnstileGateway;
import com.tmc.gate.turnstile.TurnstileGateway;
import com.tmc.gate.turnstile.UnlockResult;
import com.tmc.gate.validation.QrValidator;
import com.tmc.gate.validation.ReferenceData;
import com.tmc.gate.validation.RejectionReason;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class GateStationTest {

    private static final Instant NOW = Instant.parse("2026-11-07T07:00:00Z");
    private static final GateIdentity GATE = new GateIdentity("BD-GATE-01", "BINH_DUONG", Direction.ENTRY);

    @TempDir
    Path dir;

    private final ServerAuthority authority = new ServerAuthority();
    private final Phone phone = new Phone("E001", authority, NOW.plusSeconds(3600));
    private final Alerts alerts = new Alerts();
    private LocalDatabase database;
    private SqliteAccessEventLog events;
    private SqliteReferenceData referenceData;
    private SqliteNonceRegistry nonces;

    @BeforeEach
    void open() {
        database = LocalDatabase.open(dir.resolve("gate.db"));
        events = new SqliteAccessEventLog(database);
        referenceData = new SqliteReferenceData(database);
        nonces = new SqliteNonceRegistry(database);
        referenceData.replace(new ReferenceData(authority.publicKey(), Set.of(), Set.of(), Map.of("E001", "BINH_DUONG")));
    }

    @AfterEach
    void close() {
        database.close();
    }

    private GateStation station(TurnstileGateway turnstile) {
        return new GateStation(GATE, new CredentialCodec(), QrValidator.standard(GateSettings.defaults(), nonces),
                referenceData, nonces, events, turnstile, alerts, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void validQrUnlocksAndRecordsEvent() {
        AccessEvent event = station(SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED)).handleScan(phone.showQr(NOW));

        assertThat(event.result()).isEqualTo(AccessResult.ACCEPTED);
        assertThat(event.employeeId()).isEqualTo("E001");
        assertThat(event.unlockStatus()).isEqualTo(UnlockStatus.UNLOCKED);
        assertThat(events.find(event.id())).contains(event);
    }

    @Test
    void eventIsStoredBeforeUnlockIsRequested() {
        TurnstileGateway turnstile = (gateId, eventId) -> {
            assertThat(events.find(eventId)).map(AccessEvent::unlockStatus).contains(UnlockStatus.PENDING);
            return UnlockResult.UNLOCKED;
        };

        AccessEvent event = station(turnstile).handleScan(phone.showQr(NOW));

        assertThat(event.unlockStatus()).isEqualTo(UnlockStatus.UNLOCKED);
    }

    @Test
    void screenshotCannotBeUsedTwice() {
        SimulatedTurnstileGateway turnstile = SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED);
        GateStation station = station(turnstile);
        String qr = phone.showQr(NOW);

        station.handleScan(qr);
        AccessEvent second = station.handleScan(qr);

        assertThat(second.result()).isEqualTo(AccessResult.REJECTED);
        assertThat(second.rejectionReason()).isEqualTo(RejectionReason.REPLAYED);
        assertThat(turnstile.requests()).hasSize(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "abc", "v1.a.b"})
    void garbageInputIsRecordedAsMalformedWithoutUnlock(String raw) {
        SimulatedTurnstileGateway turnstile = SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED);

        AccessEvent event = station(turnstile).handleScan(raw);

        assertThat(event.result()).isEqualTo(AccessResult.REJECTED);
        assertThat(event.rejectionReason()).isEqualTo(RejectionReason.MALFORMED);
        assertThat(event.unlockStatus()).isEqualTo(UnlockStatus.NOT_REQUESTED);
        assertThat(turnstile.requests()).isEmpty();
        assertThat(events.pendingCount()).isEqualTo(1);
    }

    @Test
    void revokedEmployeeIsRejectedAndRecorded() {
        referenceData.replace(new ReferenceData(authority.publicKey(), Set.of(), Set.of("E001"), Map.of("E001", "BINH_DUONG")));
        SimulatedTurnstileGateway turnstile = SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED);

        AccessEvent event = station(turnstile).handleScan(phone.showQr(NOW));

        assertThat(event.rejectionReason()).isEqualTo(RejectionReason.REVOKED);
        assertThat(event.employeeId()).isEqualTo("E001");
        assertThat(events.find(event.id())).contains(event);
        assertThat(turnstile.requests()).isEmpty();
    }

    @Test
    void turnstileTimeoutIsRecordedAsUnknownAndAlertsGuard() {
        AccessEvent event = station(SimulatedTurnstileGateway.always(UnlockResult.UNKNOWN)).handleScan(phone.showQr(NOW));

        assertThat(event.unlockStatus()).isEqualTo(UnlockStatus.UNLOCK_UNKNOWN);
        assertThat(events.find(event.id())).map(AccessEvent::unlockStatus).contains(UnlockStatus.UNLOCK_UNKNOWN);
        assertThat(alerts.isActive(AlertType.UNLOCK_UNKNOWN)).isTrue();
    }

    @Test
    void turnstileFailureIsRecordedWithoutAlert() {
        AccessEvent event = station(SimulatedTurnstileGateway.always(UnlockResult.FAILED)).handleScan(phone.showQr(NOW));

        assertThat(event.unlockStatus()).isEqualTo(UnlockStatus.UNLOCK_FAILED);
        assertThat(alerts.active()).isEmpty();
    }
}
