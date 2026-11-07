package com.tmc.mobile.domain.pass

import app.cash.turbine.test
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.SequenceNonceSource
import com.tmc.mobile.domain.fakes.StubServer
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class ObservePassStateTest {

    private val clock = MutableClock(Instant.parse("2026-11-07T07:52:00Z"))
    private val server = StubServer(clock)
    private val keyStore = InMemoryDeviceKeyStore()
    private val certificates = InMemoryCertificateStore()
    private val observe = ObservePassState(certificates, GeneratePass(keyStore, SequenceNonceSource(), clock), clock)

    private suspend fun enroll() {
        certificates.save(server.issueCertificate(server.signIn("E001", "password").token, keyStore.createKey()))
    }

    @Test
    fun countsDownAndRefreshesAfterFifteenSeconds() = runTest {
        enroll()

        observe().test {
            val first = assertIs<PassState.Ready>(awaitItem())
            assertEquals(15, first.secondsLeft)

            clock.advance(Duration.ofSeconds(14))
            val last = assertIs<PassState.Ready>(awaitItem())
            assertEquals(1, last.secondsLeft)
            assertEquals(first.pass, last.pass)

            clock.advance(Duration.ofSeconds(1))
            val refreshed = assertIs<PassState.Ready>(awaitItem())
            assertEquals(15, refreshed.secondsLeft)
            assertNotEquals(first.pass.payload, refreshed.pass.payload)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun expiredCertificateStopsShowingCode() = runTest {
        enroll()
        val expiresAt = certificates.current()!!.expiresAt
        clock.advance(Duration.between(clock.instant(), expiresAt))

        observe().test {
            assertEquals(PassState.CertificateExpired(expiresAt), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun noCertificateIsNotEnrolled() = runTest {
        observe().test {
            assertEquals(PassState.NotEnrolled, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
