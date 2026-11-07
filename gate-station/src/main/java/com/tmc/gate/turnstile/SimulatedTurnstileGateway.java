package com.tmc.gate.turnstile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class SimulatedTurnstileGateway implements TurnstileGateway {

    private final List<UnlockResult> script;
    private final List<UUID> requests = new ArrayList<>();

    private SimulatedTurnstileGateway(List<UnlockResult> script) {
        if (script.isEmpty()) {
            throw new IllegalArgumentException("At least one result is required");
        }
        this.script = List.copyOf(script);
    }

    public static SimulatedTurnstileGateway always(UnlockResult result) {
        return new SimulatedTurnstileGateway(List.of(result));
    }

    public static SimulatedTurnstileGateway sequence(UnlockResult... results) {
        return new SimulatedTurnstileGateway(List.of(results));
    }

    @Override
    public synchronized UnlockResult unlock(String gateId, UUID eventId) {
        requests.add(eventId);
        return script.get(Math.min(requests.size(), script.size()) - 1);
    }

    public synchronized List<UUID> requests() {
        return List.copyOf(requests);
    }
}
