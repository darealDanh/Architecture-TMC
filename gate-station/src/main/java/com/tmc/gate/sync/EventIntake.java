package com.tmc.gate.sync;

import com.tmc.gate.access.AccessEvent;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface EventIntake {

    Set<UUID> submit(List<AccessEvent> events);
}
