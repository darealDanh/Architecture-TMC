package com.tmc.gate.access;

import java.util.List;
import java.util.UUID;

public interface InterruptedUnlocks {

    List<UUID> resolveAsUnknown();
}
