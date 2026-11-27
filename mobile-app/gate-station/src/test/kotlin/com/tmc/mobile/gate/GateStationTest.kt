package com.tmc.mobile.gate

import com.tmc.mobile.domain.gate.ValidateGatePass
import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.pass.PassPayloadCodec
import java.nio.file.Path
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

class GateStationTest {
    @TempDir
    lateinit var tempDir: Path

    private val now = Instant.parse("2026-11-27T12:00:00Z")
    private val clock = Clock.fixed(now, ZoneOffset.UTC)
    private val serverKeys = EcdsaP256.generateKeyPair()
    private val deviceKeys = EcdsaP256.generateKeyPair()

    private fun pass(employeeId: String = "E001"): String {
        val certificate = DeviceCertificate(
            "serial-1",
            employeeId,
            deviceKeys.public.encoded,
            now.plusSeconds(3600),
            byteArrayOf(),
        ).let { it.withServerSignature(EcdsaP256.sign(serverKeys.private, it.signedContent())) }
        val nonce = "bm9uY2U"
        val signature = EcdsaP256.sign(deviceKeys.private, PassPayloadCodec.signedContent(certificate, now, nonce))
        return PassPayloadCodec.encode(certificate, now, nonce, signature)
    }

    private fun gate(log: GateLog, turnstile: Turnstile): GateStation = GateStation(
        gateId = "BD-ENTRY-01",
        factory = "Binh Duong",
        direction = "ENTRY",
        validator = ValidateGatePass(serverKeys.public.encoded, clock) { employee, factory ->
            employee == "E001" && factory == "Binh Duong"
        },
        log = log,
        turnstile = turnstile,
        clock = clock,
    )

    @Test
    fun acceptedPassIsPersistedBeforeUnlockAndReplayIsRejectedAfterRestart() {
        val path = tempDir.resolve("gate.db")
        val unlocks = AtomicInteger()
        val code = pass()
        GateLog(path).use { log ->
            val station = gate(log, Turnstile {
                assertEquals(GateStatus.PENDING, log.recent().single().status)
                unlocks.incrementAndGet()
                true
            })
            assertEquals(GateStatus.PASSED, station.scan(code).status)
            assertEquals(GateStatus.PASSED, log.recent().single().status)
        }
        GateLog(path).use { log ->
            val rejected = gate(log, Turnstile { unlocks.incrementAndGet(); true }).scan(code)
            assertEquals(GateStatus.REJECTED, rejected.status)
            assertEquals("REPLAY", rejected.reason)
            assertEquals(listOf(GateStatus.REJECTED, GateStatus.PASSED), log.recent().map(GateEvent::status))
        }
        assertEquals(1, unlocks.get())
    }

    @Test
    fun invalidPassAndWrongFactoryAreLoggedWithoutUnlock() {
        GateLog(tempDir.resolve("gate.db")).use { log ->
            val station = gate(log, Turnstile { error("Should never unlock") })
            assertEquals("INVALID_FORMAT", station.scan("not-a-pass").reason)
            assertEquals("UNAUTHORIZED", station.scan(pass("E002")).reason)
            assertEquals(2, log.recent().size)
            assertEquals(listOf(GateStatus.REJECTED, GateStatus.REJECTED), log.recent().map(GateEvent::status))
        }
    }

    @Test
    fun failedOrUnknownUnlockNeverAppearsAsPassed() {
        GateLog(tempDir.resolve("failed.db")).use { log ->
            val result = gate(log, Turnstile { false }).scan(pass())
            assertEquals(GateStatus.UNLOCK_FAILED, result.status)
            assertEquals(GateStatus.UNLOCK_FAILED, log.recent().single().status)
        }
        GateLog(tempDir.resolve("unknown.db")).use { log ->
            val result = gate(log, Turnstile { throw IllegalStateException("No turnstile response") }).scan(pass())
            assertEquals(GateStatus.UNLOCK_UNKNOWN, result.status)
            assertEquals(GateStatus.UNLOCK_UNKNOWN, assertNotNull(log.recent().single()).status)
        }
    }
}
