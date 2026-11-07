package com.tmc.gate.turnstile;

import java.util.UUID;

public final class RetryingTurnstileGateway implements TurnstileGateway {

    private final TurnstileGateway delegate;
    private final int maxRetries;

    public RetryingTurnstileGateway(TurnstileGateway delegate, int maxRetries) {
        if (maxRetries < 0) {
            throw new IllegalArgumentException("maxRetries must not be negative");
        }
        this.delegate = delegate;
        this.maxRetries = maxRetries;
    }

    @Override
    public UnlockResult unlock(String gateId, UUID eventId) {
        UnlockResult result = delegate.unlock(gateId, eventId);
        for (int retry = 0; retry < maxRetries && result == UnlockResult.UNKNOWN; retry++) {
            result = delegate.unlock(gateId, eventId);
        }
        return result;
    }
}
