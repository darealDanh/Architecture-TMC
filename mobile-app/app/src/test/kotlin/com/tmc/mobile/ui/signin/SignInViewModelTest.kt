@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tmc.mobile.ui.signin

import com.tmc.mobile.MainDispatcherRule
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.session.SignInAndEnroll
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class SignInViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val network = MutableNetworkStatus()
    private val server = StubServer(MutableClock(Instant.parse("2026-11-07T07:00:00Z")), network)
    private val sessions = InMemorySessionStore()
    private val viewModel = SignInViewModel(
        SignInAndEnroll(server, server, InMemoryDeviceKeyStore(), sessions, InMemoryCertificateStore()),
    )

    private fun enter(employeeId: String, password: String) {
        viewModel.onEmployeeIdChange(employeeId)
        viewModel.onPasswordChange(password)
    }

    @Test
    fun successMarksSignedIn() = runTest {
        enter("e001", "password")

        viewModel.submit()
        advanceUntilIdle()

        assertTrue(viewModel.state.value.signedIn)
        assertFalse(viewModel.state.value.loading)
        assertEquals("E001", sessions.current()?.employee?.id)
    }

    @Test
    fun wrongPasswordShowsError() = runTest {
        enter("E001", "wrong")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Wrong employee ID or password", viewModel.state.value.error)
        assertFalse(viewModel.state.value.signedIn)
    }

    @Test
    fun offlineShowsCantReachServer() = runTest {
        network.goOffline()
        enter("E001", "password")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Can't reach the server", viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun emptyFieldsAskForInput() = runTest {
        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Enter your employee ID and password", viewModel.state.value.error)
        assertNull(sessions.current())
    }

    @Test
    fun editingClearsTheError() = runTest {
        enter("E001", "wrong")
        viewModel.submit()
        advanceUntilIdle()

        viewModel.onPasswordChange("password")

        assertNull(viewModel.state.value.error)
    }

    @Test
    fun unexpectedFailureShowsGenericErrorAndStopsLoading() = runTest {
        val brokenKeyStore = object : DeviceKeyStore by InMemoryDeviceKeyStore() {
            override fun createKey(): ByteArray = throw IllegalStateException("keystore unavailable")
        }
        val viewModel = SignInViewModel(
            SignInAndEnroll(server, server, brokenKeyStore, sessions, InMemoryCertificateStore()),
        )
        viewModel.onEmployeeIdChange("E001")
        viewModel.onPasswordChange("password")

        viewModel.submit()
        advanceUntilIdle()

        assertEquals("Something went wrong. Try again.", viewModel.state.value.error)
        assertFalse(viewModel.state.value.loading)
    }

    @Test
    fun submitIgnoredWhileLoading() = runTest {
        enter("E001", "password")

        viewModel.submit()
        viewModel.submit()
        advanceUntilIdle()

        assertEquals(1, server.certificatesIssued)
    }
}
