@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tmc.mobile.ui.device

import com.tmc.mobile.MainDispatcherRule
import com.tmc.mobile.domain.device.EnrollDevice
import com.tmc.mobile.domain.device.GetDeviceStatus
import com.tmc.mobile.domain.device.RenewCertificate
import com.tmc.mobile.domain.fakes.InMemoryAttendanceCache
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.session.SignInAndEnroll
import com.tmc.mobile.domain.session.SignOut
import java.time.Duration
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class DeviceViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-11-07T07:52:00Z"))
    private val serverNetwork = MutableNetworkStatus()
    private val server = StubServer(clock, serverNetwork)
    private val keyStore = InMemoryDeviceKeyStore()
    private val sessions = InMemorySessionStore()
    private val certificates = InMemoryCertificateStore()

    private suspend fun TestScope.started(): DeviceViewModel {
        SignInAndEnroll(server, EnrollDevice(server, keyStore, certificates), sessions)("E001", "password")
        val viewModel = DeviceViewModel(
            GetDeviceStatus(keyStore, certificates, clock),
            RenewCertificate(sessions, EnrollDevice(server, keyStore, certificates)),
            SignOut(keyStore, sessions, certificates, InMemoryAttendanceCache()),
        )
        advanceUntilIdle()
        return viewModel
    }

    @Test
    fun showsKeyAndCertificateStatus() = runTest {
        val status = started().state.value.status!!

        assertEquals("ECDSA P-256", status.algorithm)
        assertTrue(status.certificateActive)
    }

    @Test
    fun renewUpdatesExpiry() = runTest {
        val viewModel = started()
        val before = viewModel.state.value.status!!.certificateExpiresAt!!
        clock.advance(Duration.ofDays(2))

        viewModel.renew()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.status!!.certificateExpiresAt!!.isAfter(before))
        assertEquals("Certificate renewed", viewModel.state.value.message)
        assertFalse(viewModel.state.value.renewing)
    }

    @Test
    fun renewOfflineShowsCantReachServer() = runTest {
        val viewModel = started()
        serverNetwork.goOffline()

        viewModel.renew()
        advanceUntilIdle()

        assertEquals("Can't reach the server", viewModel.state.value.message)
        assertFalse(viewModel.state.value.renewing)
    }

    @Test
    fun messageShownClearsIt() = runTest {
        val viewModel = started()
        viewModel.renew()
        advanceUntilIdle()

        viewModel.messageShown()

        assertNull(viewModel.state.value.message)
    }

    @Test
    fun unexpectedRenewFailureShowsGenericMessage() = runTest {
        SignInAndEnroll(server, EnrollDevice(server, keyStore, certificates), sessions)("E001", "password")
        val failingEnrollment = object : EnrollmentApi {
            override suspend fun issueCertificate(token: String, devicePublicKey: ByteArray): DeviceCertificate =
                throw IllegalStateException("unexpected response")
        }
        val viewModel = DeviceViewModel(
            GetDeviceStatus(keyStore, certificates, clock),
            RenewCertificate(sessions, EnrollDevice(failingEnrollment, keyStore, certificates)),
            SignOut(keyStore, sessions, certificates, InMemoryAttendanceCache()),
        )
        advanceUntilIdle()

        viewModel.renew()
        advanceUntilIdle()

        assertEquals("Something went wrong. Try again.", viewModel.state.value.message)
        assertFalse(viewModel.state.value.renewing)
    }

    @Test
    fun signOutMarksSignedOut() = runTest {
        val viewModel = started()

        viewModel.signOut()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.signedOut)
        assertFalse(keyStore.hasKey())
        assertNull(sessions.current())
    }
}
