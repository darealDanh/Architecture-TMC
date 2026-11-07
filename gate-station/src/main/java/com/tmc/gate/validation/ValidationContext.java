package com.tmc.gate.validation;

import java.time.Instant;

public record ValidationContext(ReferenceData referenceData, String factoryId, Instant now) {
}
