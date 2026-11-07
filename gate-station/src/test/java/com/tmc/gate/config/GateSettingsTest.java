package com.tmc.gate.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class GateSettingsTest {

    @Test
    void defaultsMatchTheDesign() {
        GateSettings settings = GateSettings.defaults();

        assertThat(settings.qrValidityWindow()).isEqualTo(Duration.ofSeconds(30));
        assertThat(settings.clockTolerance()).isEqualTo(Duration.ofSeconds(5));
        assertThat(settings.unlockRetries()).isEqualTo(2);
        assertThat(settings.turnstileTimeout()).isEqualTo(Duration.ofSeconds(1));
        assertThat(settings.syncBatchSize()).isEqualTo(100);
        assertThat(settings.maxOfflineDuration()).isEqualTo(Duration.ofHours(4));
        assertThat(settings.pendingEventsAlertThreshold()).isEqualTo(1000);
    }

    @Test
    void rejectsZeroBatchSize() {
        assertThatThrownBy(() -> new GateSettings(
                Duration.ofSeconds(30), Duration.ofSeconds(5), 2, Duration.ofSeconds(1), 0, Duration.ofHours(4), 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsNegativeTolerance() {
        assertThatThrownBy(() -> new GateSettings(
                Duration.ofSeconds(30), Duration.ofSeconds(-1), 2, Duration.ofSeconds(1), 100, Duration.ofHours(4), 1000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
