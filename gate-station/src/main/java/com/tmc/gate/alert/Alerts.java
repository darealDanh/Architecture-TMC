package com.tmc.gate.alert;

import java.util.ArrayList;
import java.util.List;

public final class Alerts implements AlertSink {

    private final List<Alert> active = new ArrayList<>();

    @Override
    public synchronized void raise(Alert alert) {
        boolean alreadyActive = active.stream()
                .anyMatch(existing -> existing.type() == alert.type() && existing.detail().equals(alert.detail()));
        if (!alreadyActive) {
            active.add(alert);
        }
    }

    public synchronized void resolve(AlertType type) {
        active.removeIf(alert -> alert.type() == type);
    }

    public synchronized boolean isActive(AlertType type) {
        return active.stream().anyMatch(alert -> alert.type() == type);
    }

    public synchronized List<Alert> active() {
        return List.copyOf(active);
    }
}
