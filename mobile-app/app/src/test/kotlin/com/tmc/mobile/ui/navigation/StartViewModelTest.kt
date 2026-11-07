@file:OptIn(ExperimentalCoroutinesApi::class)

package com.tmc.mobile.ui.navigation

import com.tmc.mobile.MainDispatcherRule
import com.tmc.mobile.domain.fakes.InMemoryCertificateStore
import com.tmc.mobile.domain.fakes.InMemoryDeviceKeyStore
import com.tmc.mobile.domain.fakes.InMemorySessionStore
import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.StubServer
import com.tmc.mobile.domain.session.SignInAndEnroll
import java.time.Instant
import kotlin.test.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

class StartViewModelTest {

    @get:Rule
    val mainDispatcher = MainDispatcherRule()

    private val sessions = InMemorySessionStore()
    private val certificates = InMemoryCertificateStore()

    @Test
    fun startsAtMainWhenSessionAndCertificateExist() = runTest {
        val server = StubServer(MutableClock(Instant.parse("2026-11-07T07:00:00Z")))
        SignInAndEnroll(server, server, InMemoryDeviceKeyStore(), sessions, certificates)("E001", "password")

        val viewModel = StartViewModel(sessions, certificates)
        advanceUntilIdle()

        assertEquals(StartDestination.Main, viewModel.start.value)
    }

    @Test
    fun startsAtSignInOtherwise() = runTest {
        val viewModel = StartViewModel(sessions, certificates)
        advanceUntilIdle()

        assertEquals(StartDestination.SignIn, viewModel.start.value)
    }
}
