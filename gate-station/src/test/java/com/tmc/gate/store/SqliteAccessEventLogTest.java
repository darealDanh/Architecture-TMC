package com.tmc.gate.store;

import static org.assertj.core.api.Assertions.assertThat;

import com.tmc.gate.access.AccessEvent;
import com.tmc.gate.access.Direction;
import com.tmc.gate.access.GateIdentity;
import com.tmc.gate.access.UnlockStatus;
import com.tmc.gate.validation.RejectionReason;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SqliteAccessEventLogTest {

    private static final GateIdentity GATE = new GateIdentity("BD-GATE-01", "BINH_DUONG", Direction.ENTRY);
    private static final Instant T0 = Instant.parse("2026-11-07T07:00:00Z");

    @TempDir
    Path dir;

    private LocalDatabase database;
    private SqliteAccessEventLog log;

    @BeforeEach
    void open() {
        database = LocalDatabase.open(dir.resolve("gate.db"));
        log = new SqliteAccessEventLog(database);
    }

    @AfterEach
    void close() {
        database.close();
    }

    private static AccessEvent unlocked(GateIdentity gate, String employeeId, Instant at) {
        return AccessEvent.accepted(gate, employeeId, at).withUnlockStatus(UnlockStatus.UNLOCKED);
    }

    @Test
    void appendedEventSurvivesReopen() {
        AccessEvent event = AccessEvent.accepted(GATE, "E001", T0);
        log.append(event);
        database.close();

        database = LocalDatabase.open(dir.resolve("gate.db"));
        log = new SqliteAccessEventLog(database);

        assertThat(log.find(event.id())).contains(event);
    }

    @Test
    void storesRejectedEventWithoutEmployee() {
        AccessEvent event = AccessEvent.rejected(GATE, null, RejectionReason.MALFORMED, T0);
        log.append(event);

        assertThat(log.find(event.id())).contains(event);
    }

    @Test
    void updatesUnlockStatus() {
        AccessEvent event = AccessEvent.accepted(GATE, "E001", T0);
        log.append(event);

        log.updateUnlockStatus(event.id(), UnlockStatus.UNLOCKED);

        assertThat(log.find(event.id())).contains(event.withUnlockStatus(UnlockStatus.UNLOCKED));
    }

    @Test
    void pendingReturnsUnsyncedEventsOldestFirstUpToLimit() {
        AccessEvent third = unlocked(GATE, "E003", T0.plusSeconds(2));
        AccessEvent first = unlocked(GATE, "E001", T0);
        AccessEvent second = unlocked(GATE, "E002", T0.plusSeconds(1));
        log.append(third);
        log.append(first);
        log.append(second);

        assertThat(log.pending(2)).containsExactly(first, second);
    }

    @Test
    void markSyncedRemovesEventsFromOutbox() {
        AccessEvent first = unlocked(GATE, "E001", T0);
        AccessEvent second = unlocked(GATE, "E002", T0.plusSeconds(1));
        AccessEvent third = unlocked(GATE, "E003", T0.plusSeconds(2));
        log.append(first);
        log.append(second);
        log.append(third);
        assertThat(log.pendingCount()).isEqualTo(3);

        log.markSynced(List.of(first.id(), second.id()));

        assertThat(log.pendingCount()).isEqualTo(1);
        assertThat(log.pending(10)).containsExactly(third);
    }

    @Test
    void resolveAsUnknownTurnsPendingUnlocksIntoUnknown() {
        AccessEvent interrupted = AccessEvent.accepted(GATE, "E001", T0);
        AccessEvent completed = AccessEvent.accepted(GATE, "E002", T0.plusSeconds(1));
        log.append(interrupted);
        log.append(completed);
        log.updateUnlockStatus(completed.id(), UnlockStatus.UNLOCKED);

        assertThat(log.resolveAsUnknown()).containsExactly(interrupted.id());
        assertThat(log.find(interrupted.id())).map(AccessEvent::unlockStatus).contains(UnlockStatus.UNLOCK_UNKNOWN);
        assertThat(log.find(completed.id())).map(AccessEvent::unlockStatus).contains(UnlockStatus.UNLOCKED);
    }

    @Test
    void eventWaitingForUnlockResultIsNotSentUntilTheResultIsKnown() {
        AccessEvent event = AccessEvent.accepted(GATE, "E001", T0);
        log.append(event);

        assertThat(log.pending(10)).isEmpty();

        log.updateUnlockStatus(event.id(), UnlockStatus.UNLOCKED);

        assertThat(log.pending(10)).containsExactly(event.withUnlockStatus(UnlockStatus.UNLOCKED));
    }

    @Test
    void writersWaitForEachOtherInsteadOfFailing() throws SQLException {
        try (Statement statement = database.connection().createStatement();
                ResultSet result = statement.executeQuery("PRAGMA busy_timeout")) {
            result.next();
            assertThat(result.getInt(1)).isEqualTo(1000);
        }
    }

    @Test
    void walModeIsEnabled() throws SQLException {
        try (Statement statement = database.connection().createStatement();
                ResultSet result = statement.executeQuery("PRAGMA journal_mode")) {
            result.next();
            assertThat(result.getString(1)).isEqualTo("wal");
        }
    }
}
