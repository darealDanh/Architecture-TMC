package com.tmc.gate.turnstile;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

public final class HttpTurnstileGateway implements TurnstileGateway {

    private final HttpClient client;
    private final URI baseUrl;
    private final Duration timeout;

    public HttpTurnstileGateway(HttpClient client, URI baseUrl, Duration timeout) {
        this.client = client;
        this.baseUrl = baseUrl;
        this.timeout = timeout;
    }

    @Override
    public UnlockResult unlock(String gateId, UUID eventId) {
        HttpRequest request = HttpRequest.newBuilder(baseUrl.resolve("/gates/" + gateId + "/unlock"))
                .timeout(timeout)
                .header("Idempotency-Key", eventId.toString())
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        try {
            int status = client.send(request, HttpResponse.BodyHandlers.discarding()).statusCode();
            return status >= 200 && status < 300 ? UnlockResult.UNLOCKED : UnlockResult.FAILED;
        } catch (IOException e) {
            return UnlockResult.UNKNOWN;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return UnlockResult.UNKNOWN;
        }
    }
}
