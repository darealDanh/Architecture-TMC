package com.tmc.gate.config;

import java.time.Duration;

public interface ConfigReader {

    Duration qrValidityWindow();

    Duration clockTolerance();

    int unlockRetries();

    Duration turnstileTimeout();

    int syncBatchSize();

    Duration maxOfflineDuration();

    int pendingEventsAlertThreshold();
}
