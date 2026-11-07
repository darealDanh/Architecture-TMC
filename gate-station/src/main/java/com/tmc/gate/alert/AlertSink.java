package com.tmc.gate.alert;

@FunctionalInterface
public interface AlertSink {

    void raise(Alert alert);
}
