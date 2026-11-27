package com.tmc.mobile.gate

import com.tmc.mobile.domain.gate.ValidateGatePass
import com.tmc.mobile.domain.pass.EcdsaP256
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.time.Clock
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GateHttpServerTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun browserCanScanAndReadLoggedRejectionButAnotherOriginCannotSubmit() {
        val scannerAssets = tempDir.resolve("scanner")
        Files.createDirectories(scannerAssets)
        Files.writeString(scannerAssets.resolve("qr-scanner.min.js"), "export default class Scanner {}")
        Files.writeString(scannerAssets.resolve("qr-scanner-worker.min.js"), "")
        val clock = Clock.systemUTC()
        GateLog(tempDir.resolve("gate.db")).use { log ->
            val station = GateStation(
                "BD-ENTRY-01", "Binh Duong", "ENTRY",
                ValidateGatePass(EcdsaP256.generateKeyPair().public.encoded, clock) { _, _ -> true },
                log, Turnstile { true }, clock,
            )
            GateHttpServer(station, 0, scannerAssets).use { server ->
                server.start()
                val address = server.address.replace("localhost", "127.0.0.1")
                val client = HttpClient.newHttpClient()
                val page = client.send(HttpRequest.newBuilder(URI.create(address)).GET().build(), HttpResponse.BodyHandlers.ofString())
                assertEquals(200, page.statusCode())
                assertTrue(page.body().contains("Start camera"))

                fun scan(origin: String): HttpResponse<String> = client.send(
                    HttpRequest.newBuilder(URI.create("$address/scan"))
                        .header("Origin", origin)
                        .header("Content-Type", "text/plain")
                        .POST(HttpRequest.BodyPublishers.ofString("not-a-pass"))
                        .build(),
                    HttpResponse.BodyHandlers.ofString(),
                )

                assertEquals(403, scan("https://unrelated.example").statusCode())
                val rejected = scan(address)
                assertEquals(200, rejected.statusCode())
                assertTrue(rejected.body().contains("\"reason\":\"INVALID_FORMAT\""))
                val events = client.send(
                    HttpRequest.newBuilder(URI.create("$address/events")).GET().build(),
                    HttpResponse.BodyHandlers.ofString(),
                )
                assertTrue(events.body().contains("\"status\":\"REJECTED\""))
                assertEquals(1, log.recent().size)
            }
        }
    }
}
