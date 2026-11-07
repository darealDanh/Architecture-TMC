package com.tmc.gate.turnstile;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class HttpTurnstileGatewayTest {

    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void successfulResponseUnlocks() {
        UUID eventId = UUID.randomUUID();
        try (FakeTurnstileServer server = FakeTurnstileServer.responding(200)) {
            TurnstileGateway gateway = new HttpTurnstileGateway(client, server.baseUrl(), Duration.ofSeconds(1));

            assertThat(gateway.unlock("G1", eventId)).isEqualTo(UnlockResult.UNLOCKED);
            assertThat(server.requests())
                    .containsExactly(new FakeTurnstileServer.Request("POST", "/gates/G1/unlock", eventId.toString()));
        }
    }

    @Test
    void serverErrorFails() {
        try (FakeTurnstileServer server = FakeTurnstileServer.responding(500)) {
            TurnstileGateway gateway = new HttpTurnstileGateway(client, server.baseUrl(), Duration.ofSeconds(1));

            assertThat(gateway.unlock("G1", UUID.randomUUID())).isEqualTo(UnlockResult.FAILED);
        }
    }

    @Test
    void slowResponseIsUnknown() {
        try (FakeTurnstileServer server = FakeTurnstileServer.respondingAfter(Duration.ofMillis(500))) {
            TurnstileGateway gateway = new HttpTurnstileGateway(client, server.baseUrl(), Duration.ofMillis(100));

            assertThat(gateway.unlock("G1", UUID.randomUUID())).isEqualTo(UnlockResult.UNKNOWN);
        }
    }

    @Test
    void unreachableTurnstileIsUnknown() throws IOException {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        TurnstileGateway gateway = new HttpTurnstileGateway(
                client, URI.create("http://127.0.0.1:" + closedPort), Duration.ofSeconds(1));

        assertThat(gateway.unlock("G1", UUID.randomUUID())).isEqualTo(UnlockResult.UNKNOWN);
    }
}
