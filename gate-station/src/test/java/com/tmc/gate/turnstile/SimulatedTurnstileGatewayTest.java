package com.tmc.gate.turnstile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class SimulatedTurnstileGatewayTest {

    @Test
    void sequenceRepeatsItsLastResultOnceExhausted() {
        SimulatedTurnstileGateway turnstile =
                SimulatedTurnstileGateway.sequence(UnlockResult.UNLOCKED, UnlockResult.FAILED);

        assertThat(turnstile.unlock("G1", UUID.randomUUID())).isEqualTo(UnlockResult.UNLOCKED);
        assertThat(turnstile.unlock("G1", UUID.randomUUID())).isEqualTo(UnlockResult.FAILED);
        assertThat(turnstile.unlock("G1", UUID.randomUUID())).isEqualTo(UnlockResult.FAILED);
    }

    @Test
    void recordsEveryRequest() {
        SimulatedTurnstileGateway turnstile = SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED);
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();

        turnstile.unlock("G1", first);
        turnstile.unlock("G1", second);

        assertThat(turnstile.requests()).containsExactly(first, second);
    }
}
