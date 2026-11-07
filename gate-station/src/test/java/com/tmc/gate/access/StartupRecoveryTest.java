package com.tmc.gate.access;

import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.alert.Alert;
import com.tmc.gate.alert.AlertType;
import com.tmc.gate.alert.Alerts;
import com.tmc.gate.store.LocalDatabase;
import com.tmc.gate.store.SqliteAccessEventLog;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class StartupRecoveryTest {

    private static final Instant NOW = Instant.parse("2026-11-07T07:00:00Z");

    @TempDir
    Path dir;

    @Test
    void interruptedUnlockBecomesUnknownAndAlertsGuard() {
        try (LocalDatabase database = LocalDatabase.open(dir.resolve("gate.db"))) {
            SqliteAccessEventLog events = new SqliteAccessEventLog(database);
            AccessEvent interrupted = AccessEvent.accepted(
                    new GateIdentity("BD-GATE-01", "BINH_DUONG", Direction.ENTRY), "E001", NOW);
            events.append(interrupted);
            Alerts alerts = new Alerts();

            new StartupRecovery(events, alerts, Clock.fixed(NOW, ZoneOffset.UTC)).run();

            assertThat(events.find(interrupted.id())).map(AccessEvent::unlockStatus).contains(UnlockStatus.UNLOCK_UNKNOWN);
            assertThat(alerts.active())
                    .containsExactly(new Alert(AlertType.UNLOCK_UNKNOWN, interrupted.id().toString(), NOW));
        }
    }
}
