package com.tmc.gate.turnstile;

import java.util.UUID;

@FunctionalInterface
public interface TurnstileGateway {

    UnlockResult unlock(String gateId, UUID eventId);
}
