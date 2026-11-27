package com.tmc.mobile.gate

import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Executors

class GateHttpServer(private val gate: GateStation, port: Int, scannerAssets: Path) : AutoCloseable {
    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", port), 0)
    private val executor = Executors.newFixedThreadPool(2)
    private val page = checkNotNull(javaClass.getResourceAsStream("/gate/index.html")).use { it.readAllBytes() }
    private val scanner = Files.readAllBytes(scannerAssets.resolve("qr-scanner.min.js"))
    private val worker = Files.readAllBytes(scannerAssets.resolve("qr-scanner-worker.min.js"))

    val address: String get() = "http://localhost:${server.address.port}"

    init {
        server.executor = executor
        server.createContext("/") { exchange ->
            exchange.use {
                if (exchange.requestMethod != "GET" || exchange.requestURI.path != "/") {
                    send(exchange, 404, "Not found".toByteArray(), "text/plain; charset=utf-8")
                } else {
                    send(exchange, 200, page, "text/html; charset=utf-8")
                }
            }
        }
        server.createContext("/qr-scanner.min.js") { exchange -> static(exchange, scanner) }
        server.createContext("/qr-scanner-worker.min.js") { exchange -> static(exchange, worker) }
        server.createContext("/config") { exchange ->
            exchange.use {
                if (exchange.requestMethod != "GET") {
                    send(exchange, 405, ByteArray(0), "application/json")
                } else {
                    val body = """{"gate":${json(gate.gateId)},"factory":${json(gate.factory)},"direction":${json(gate.direction)}}"""
                    sendJson(exchange, 200, body)
                }
            }
        }
        server.createContext("/events") { exchange ->
            exchange.use {
                if (exchange.requestMethod != "GET") {
                    send(exchange, 405, ByteArray(0), "application/json")
                } else {
                    sendJson(exchange, 200, gate.recent().joinToString(",", "[", "]", transform = ::eventJson))
                }
            }
        }
        server.createContext("/scan") { exchange ->
            exchange.use {
                if (exchange.requestMethod != "POST") {
                    send(exchange, 405, ByteArray(0), "application/json")
                } else if (exchange.requestHeaders.getFirst("Origin") != address &&
                    exchange.requestHeaders.getFirst("Origin") != "http://127.0.0.1:${server.address.port}"
                ) {
                    sendJson(exchange, 403, """{"error":"This gate accepts scans from its own page only"}""")
                } else if (!exchange.requestHeaders.getFirst("Content-Type").orEmpty().startsWith("text/plain")) {
                    sendJson(exchange, 415, """{"error":"Expected a QR code"}""")
                } else {
                    val payload = exchange.requestBody.readNBytes(MAX_PAYLOAD_BYTES + 1)
                    if (payload.isEmpty() || payload.size > MAX_PAYLOAD_BYTES) {
                        sendJson(exchange, 413, """{"error":"QR code is empty or too large"}""")
                    } else {
                        sendJson(exchange, 200, eventJson(gate.scan(String(payload, StandardCharsets.UTF_8))))
                    }
                }
            }
        }
    }

    fun start() = server.start()

    override fun close() {
        server.stop(0)
        executor.shutdownNow()
    }

    private fun static(exchange: HttpExchange, body: ByteArray) {
        exchange.use {
            if (exchange.requestMethod != "GET") {
                send(exchange, 405, ByteArray(0), "text/javascript")
            } else {
                send(exchange, 200, body, "text/javascript; charset=utf-8")
            }
        }
    }

    private fun sendJson(exchange: HttpExchange, status: Int, body: String) =
        send(exchange, status, body.toByteArray(StandardCharsets.UTF_8), "application/json; charset=utf-8")

    private fun send(exchange: HttpExchange, status: Int, body: ByteArray, type: String) {
        exchange.responseHeaders.set("Content-Type", type)
        exchange.responseHeaders.set("Cache-Control", "no-store")
        exchange.responseHeaders.set("X-Content-Type-Options", "nosniff")
        exchange.sendResponseHeaders(status, body.size.toLong())
        exchange.responseBody.write(body)
    }

    private fun eventJson(event: GateEvent): String =
        """{"id":${json(event.id)},"employeeId":${json(event.employeeId)},"timestamp":${json(event.timestamp.toString())},"status":${json(event.status.name)},"reason":${json(event.reason)}}"""

    private fun json(value: String?): String = if (value == null) "null" else buildString {
        append('"')
        for (character in value) {
            when (character) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (character.code < 32) {
                    append("\\u")
                    append(character.code.toString(16).padStart(4, '0'))
                } else {
                    append(character)
                }
            }
        }
        append('"')
    }

    private companion object {
        const val MAX_PAYLOAD_BYTES = 8192
    }
}
