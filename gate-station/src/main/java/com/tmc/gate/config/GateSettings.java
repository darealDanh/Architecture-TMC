package com.tmc.gate.config;

import java.time.Duration;

public record GateSettings(
        Duration qrValidityWindow,
        Duration clockTolerance,
        int unlockRetries,
        Duration turnstileTimeout,
        int syncBatchSize,
        Duration maxOfflineDuration,
        int pendingEventsAlertThreshold) implements ConfigReader {

    public GateSettings {
        requireNotNegative(qrValidityWindow, "qrValidityWindow");
        requireNotNegative(clockTolerance, "clockTolerance");
        requireNotNegative(turnstileTimeout, "turnstileTimeout");
        requireNotNegative(maxOfflineDuration, "maxOfflineDuration");
        if (unlockRetries < 0) {
            throw new IllegalArgumentException("unlockRetries must not be negative");
        }
        if (syncBatchSize < 1) {
            throw new IllegalArgumentException("syncBatchSize must be at least 1");
        }
        if (pendingEventsAlertThreshold < 0) {
            throw new IllegalArgumentException("pendingEventsAlertThreshold must not be negative");
        }
    }

    public static GateSettings defaults() {
        return new GateSettings(
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                2,
                Duration.ofSeconds(1),
                100,
                Duration.ofHours(4),
                1000);
    }

    private static void requireNotNegative(Duration value, String name) {
        if (value == null || value.isNegative()) {
            throw new IllegalArgumentException(name + " must not be negative");
        }
    }
}
