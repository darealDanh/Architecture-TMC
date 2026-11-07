package com.tmc.gate.alert;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class AlertsTest {

    private static final Instant NOW = Instant.parse("2026-11-07T07:00:00Z");

    private final Alerts alerts = new Alerts();

    @Test
    void raiseAndResolve() {
        alerts.raise(new Alert(AlertType.GATE_OFFLINE_TOO_LONG, "offline", NOW));
        assertThat(alerts.isActive(AlertType.GATE_OFFLINE_TOO_LONG)).isTrue();

        alerts.resolve(AlertType.GATE_OFFLINE_TOO_LONG);

        assertThat(alerts.isActive(AlertType.GATE_OFFLINE_TOO_LONG)).isFalse();
        assertThat(alerts.active()).isEmpty();
    }

    @Test
    void duplicateRaiseIsIgnored() {
        alerts.raise(new Alert(AlertType.PENDING_EVENTS_HIGH, "1001 pending", NOW));
        alerts.raise(new Alert(AlertType.PENDING_EVENTS_HIGH, "1001 pending", NOW.plusSeconds(60)));

        assertThat(alerts.active()).hasSize(1);
    }

    @Test
    void alertsOfTheSameTypeWithDifferentDetailsAreKept() {
        alerts.raise(new Alert(AlertType.UNLOCK_UNKNOWN, "event-1", NOW));
        alerts.raise(new Alert(AlertType.UNLOCK_UNKNOWN, "event-2", NOW));

        assertThat(alerts.active()).hasSize(2);
    }
}
