package com.tmc.mobile.domain.device

import com.tmc.mobile.domain.fakes.DeviceKeyStoreContract
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.session.SignInAndEnroll
import java.time.Duration
import java.time.Instant
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class DeviceUseCasesTest {

    private val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
    private val server = StubServer(clock)
    private val keyStore = InMemoryDeviceKeyStore()
    private val sessions = InMemorySessionStore()
    private val certificates = InMemoryCertificateStore()
    private val signIn = SignInAndEnroll(server, server, keyStore, sessions, certificates)

    @Test
    fun renewedCertificateHasLaterExpiryAndSameKey() = runTest {
        signIn("E001", "password")
        val original = certificates.current()!!
        clock.advance(Duration.ofDays(3))

        val renewed = RenewCertificate(sessions, server, keyStore, certificates)()

        assertTrue(renewed.expiresAt.isAfter(original.expiresAt))
        assertContentEquals(original.devicePublicKey, renewed.devicePublicKey)
        assertEquals(renewed, certificates.current())
    }

    @Test
    fun reportsActiveUntilExpiry() = runTest {
        signIn("E001", "password")
        val status = GetDeviceStatus(keyStore, certificates, clock)

        val active = status()
        clock.advance(Duration.ofDays(7))
        val expired = status()

        assertTrue(active.certificateActive)
        assertEquals("ECDSA P-256", active.algorithm)
        assertEquals(certificates.current()!!.expiresAt, active.certificateExpiresAt)
        assertFalse(expired.certificateActive)
    }

    @Test
    fun inMemoryKeyStoreMeetsTheContract() {
        val store = InMemoryDeviceKeyStore()
        DeviceKeyStoreContract(store, store).runAll(reset = store::delete)
    }
}
