package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.InvalidCredentialsException
import com.tmc.mobile.domain.port.ServerUnreachableException
import java.time.Instant
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SignInAndEnrollTest {

    private val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
    private val network = MutableNetworkStatus()
    private val server = StubServer(clock, network)
    private val keyStore = InMemoryDeviceKeyStore()
    private val sessions = InMemorySessionStore()
    private val certificates = InMemoryCertificateStore()
    private val signIn = SignInAndEnroll(server, server, keyStore, sessions, certificates)

    @Test
    fun firstSignInCreatesKeyAndStoresCertificate() = runTest {
        val session = signIn("E001", "password")

        assertEquals("Nguyen Van A", session.employee.name)
        assertEquals(session, sessions.current())
        val certificate = assertNotNull(certificates.current())
        assertContentEquals(keyStore.publicKey(), certificate.devicePublicKey)
        assertTrue(EcdsaP256.verify(server.serverPublicKey(), certificate.signedContent(), certificate.serverSignature))
    }

    @Test
    fun secondSignInReusesExistingKey() = runTest {
        signIn("E001", "password")
        val firstKey = keyStore.publicKey()

        signIn("E001", "password")

        assertEquals(1, keyStore.keysCreated)
        assertContentEquals(firstKey, keyStore.publicKey())
    }

    @Test
    fun wrongPasswordStoresNothing() = runTest {
        assertThrows<InvalidCredentialsException> { signIn("E001", "wrong") }

        assertNull(sessions.current())
        assertNull(certificates.current())
        assertFalse(keyStore.hasKey())
    }

    @Test
    fun unreachableServerStoresNothing() = runTest {
        network.goOffline()

        assertThrows<ServerUnreachableException> { signIn("E001", "password") }

        assertNull(sessions.current())
        assertNull(certificates.current())
    }
}
