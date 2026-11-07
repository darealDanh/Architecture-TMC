package com.tmc.gate.validation;

import com.tmc.gate.access.GateIdentity;
import java.time.Instant;

public record ValidationContext(ReferenceData referenceData, GateIdentity gate, Instant now) {
}
