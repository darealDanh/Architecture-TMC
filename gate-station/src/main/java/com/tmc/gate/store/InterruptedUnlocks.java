package com.tmc.gate.store;

import java.util.List;
import java.util.UUID;

public interface InterruptedUnlocks {

    List<UUID> resolveAsUnknown();
}
