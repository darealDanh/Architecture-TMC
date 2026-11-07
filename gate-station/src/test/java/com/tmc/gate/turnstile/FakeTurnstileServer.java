package com.tmc.gate.turnstile;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

final class FakeTurnstileServer implements AutoCloseable {

    record Request(String method, String path, String idempotencyKey) {
    }

    private final HttpServer server;
    private final List<Request> requests = new CopyOnWriteArrayList<>();

    private FakeTurnstileServer(int status, Duration delay) {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", exchange -> {
            requests.add(new Request(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                    exchange.getRequestHeaders().getFirst("Idempotency-Key")));
            pause(delay);
            exchange.sendResponseHeaders(status, -1);
            exchange.close();
        });
        server.start();
    }

    static FakeTurnstileServer responding(int status) {
        return new FakeTurnstileServer(status, Duration.ZERO);
    }

    static FakeTurnstileServer respondingAfter(Duration delay) {
        return new FakeTurnstileServer(200, delay);
    }

    URI baseUrl() {
        return URI.create("http://127.0.0.1:" + server.getAddress().getPort());
    }

    List<Request> requests() {
        return requests;
    }

    @Override
    public void close() {
        server.stop(0);
    }

    private static void pause(Duration delay) {
        try {
            Thread.sleep(delay.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
