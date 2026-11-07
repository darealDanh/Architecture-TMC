package com.tmc.gate.turnstile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class RetryingTurnstileGatewayTest {

    private final UUID eventId = UUID.randomUUID();

    @Test
    void retriesUnknownUntilResolved() {
        SimulatedTurnstileGateway turnstile =
                SimulatedTurnstileGateway.sequence(UnlockResult.UNKNOWN, UnlockResult.UNLOCKED);

        UnlockResult result = new RetryingTurnstileGateway(turnstile, 2).unlock("G1", eventId);

        assertThat(result).isEqualTo(UnlockResult.UNLOCKED);
        assertThat(turnstile.requests()).containsExactly(eventId, eventId);
    }

    @Test
    void givesUpAfterMaxRetries() {
        SimulatedTurnstileGateway turnstile = SimulatedTurnstileGateway.always(UnlockResult.UNKNOWN);

        UnlockResult result = new RetryingTurnstileGateway(turnstile, 2).unlock("G1", eventId);

        assertThat(result).isEqualTo(UnlockResult.UNKNOWN);
        assertThat(turnstile.requests()).hasSize(3);
    }

    @Test
    void doesNotRetryFailure() {
        SimulatedTurnstileGateway turnstile = SimulatedTurnstileGateway.always(UnlockResult.FAILED);

        UnlockResult result = new RetryingTurnstileGateway(turnstile, 2).unlock("G1", eventId);

        assertThat(result).isEqualTo(UnlockResult.FAILED);
        assertThat(turnstile.requests()).hasSize(1);
    }
}
