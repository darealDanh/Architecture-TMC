package com.tmc.gate.turnstile;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

abstract class TurnstileGatewayContract {

    abstract TurnstileGateway gatewayThatUnlocks();

    @Test
    void unlockReturnsUnlockedForAnyGateAndEvent() {
        TurnstileGateway gateway = gatewayThatUnlocks();

        assertThat(gateway.unlock("BD-GATE-01", UUID.randomUUID())).isEqualTo(UnlockResult.UNLOCKED);
        assertThat(gateway.unlock("LA-GATE-07", UUID.randomUUID())).isEqualTo(UnlockResult.UNLOCKED);
    }

    @Test
    void repeatedUnlockWithSameEventIdIsAccepted() {
        TurnstileGateway gateway = gatewayThatUnlocks();
        UUID eventId = UUID.randomUUID();

        assertThat(gateway.unlock("BD-GATE-01", eventId)).isEqualTo(UnlockResult.UNLOCKED);
        assertThat(gateway.unlock("BD-GATE-01", eventId)).isEqualTo(UnlockResult.UNLOCKED);
    }
}
