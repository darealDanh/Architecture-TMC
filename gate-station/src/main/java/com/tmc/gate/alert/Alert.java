package com.tmc.gate.alert;

import java.time.Instant;

public record Alert(AlertType type, String detail, Instant raisedAt) {
}
