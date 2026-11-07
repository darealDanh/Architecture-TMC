package com.tmc.gate.turnstile;

import java.net.http.HttpClient;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;

class TurnstileGatewayContractTest {

    @Nested
    class Simulated extends TurnstileGatewayContract {

        @Override
        TurnstileGateway gatewayThatUnlocks() {
            return SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED);
        }
    }

    @Nested
    class Http extends TurnstileGatewayContract {

        private final FakeTurnstileServer server = FakeTurnstileServer.responding(200);

        @Override
        TurnstileGateway gatewayThatUnlocks() {
            return new HttpTurnstileGateway(HttpClient.newHttpClient(), server.baseUrl(), Duration.ofSeconds(1));
        }

        @AfterEach
        void stopServer() {
            server.close();
        }
    }

    @Nested
    class Retrying extends TurnstileGatewayContract {

        @Override
        TurnstileGateway gatewayThatUnlocks() {
            return new RetryingTurnstileGateway(SimulatedTurnstileGateway.always(UnlockResult.UNLOCKED), 2);
        }
    }
}
