package com.tmc.mobile.gate

import com.tmc.mobile.domain.gate.ValidateGatePass
import com.tmc.mobile.fakeserver.FakeGateDirectory
import java.nio.file.Path
import java.time.Clock

fun main(args: Array<String>) {
    val (factory, gateId) = when (args.singleOrNull() ?: "bd") {
        "bd" -> "Binh Duong" to "BD-ENTRY-01"
        "la" -> "Long An" to "LA-ENTRY-01"
        else -> error("Use bd for Binh Duong or la for Long An")
    }
    val clock = Clock.systemUTC()
    val log = GateLog(Path.of("gate-events.db"))
    val gate = GateStation(
        gateId = gateId,
        factory = factory,
        direction = "ENTRY",
        validator = ValidateGatePass(FakeGateDirectory.publicKey, clock, FakeGateDirectory::isAuthorized),
        log = log,
        turnstile = Turnstile { true },
        clock = clock,
    )
    val server = GateHttpServer(gate, 8765, Path.of("node_modules", "qr-scanner"))
    Runtime.getRuntime().addShutdownHook(Thread {
        server.close()
        log.close()
    })
    server.start()
    println("$gateId listening at ${server.address}")
}
