@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tmc.mobile.ui.pass

import com.tmc.mobile.MainDispatcherRule
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.SequenceNonceSource
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.pass.GeneratePass
import com.tmc.mobile.domain.pass.ObservePassState
import com.tmc.mobile.domain.pass.PassPayloadCodec
import com.tmc.mobile.domain.session.SignInAndEnroll
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class PassViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val clock = MutableClock(Instant.parse("2026-11-07T07:52:00Z"))
    private val network = MutableNetworkStatus()
    private val server = StubServer(clock)
    private val keyStore = InMemoryDeviceKeyStore()
    private val sessions = InMemorySessionStore()
    private val certificates = InMemoryCertificateStore()

    private suspend fun TestScope.started(): PassViewModel {
        SignInAndEnroll(server, server, keyStore, sessions, certificates)("E001", "password")
        val observe = ObservePassState(certificates, GeneratePass(keyStore, SequenceNonceSource(), clock), clock)
        val viewModel = PassViewModel(observe, network, sessions)
        backgroundScope.launch { viewModel.state.collect {} }
        runCurrent()
        return viewModel
    }

    @Test
    fun passShowsCodeAndCountdown() = runTest {
        val state = started().state.value

        assertEquals("Nguyen Van A", state.employee?.name)
        assertEquals("E001", PassPayloadCodec.decode(assertNotNull(state.payload)).certificate.employeeId)
        assertEquals(15, state.secondsLeft)
        assertEquals(15, state.totalSeconds)
        assertTrue(state.online)
        assertFalse(state.certificateExpired)
    }

    @Test
    fun passMarksOffline() = runTest {
        val viewModel = started()

        network.goOffline()
        runCurrent()

        assertFalse(viewModel.state.value.online)
        assertNotNull(viewModel.state.value.payload)
    }

    @Test
    fun passShowsExpired() = runTest {
        SignInAndEnroll(server, server, keyStore, sessions, certificates)("E001", "password")
        clock.advance(StubServer.CERTIFICATE_LIFETIME)
        val observe = ObservePassState(certificates, GeneratePass(keyStore, SequenceNonceSource(), clock), clock)
        val viewModel = PassViewModel(observe, network, sessions)
        backgroundScope.launch { viewModel.state.collect {} }
        runCurrent()

        assertTrue(viewModel.state.value.certificateExpired)
        assertNull(viewModel.state.value.payload)
    }
}
