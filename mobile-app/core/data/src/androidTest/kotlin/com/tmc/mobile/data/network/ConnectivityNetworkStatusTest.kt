package com.tmc.mobile.data.network

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ConnectivityNetworkStatusTest {

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val status = ConnectivityNetworkStatus(instrumentation.targetContext, scope)

    @After
    fun restoreNetwork() {
        airplaneMode(false)
        scope.cancel()
    }

    @Test
    fun reportsOnlineWhenTheDeviceHasInternet() = runBlocking {
        assertTrue(status.isOnline.value)
    }

    @Test
    fun followsAirplaneModeOffAndOn() = runBlocking {
        airplaneMode(true)
        assertEquals(false, settle(expected = false))

        airplaneMode(false)
        assertEquals(true, settle(expected = true))
    }

    private suspend fun settle(expected: Boolean): Boolean =
        withTimeoutOrNull(SETTLE_MILLIS) { status.isOnline.first { it == expected } } ?: status.isOnline.value

    private fun airplaneMode(enabled: Boolean) {
        val state = if (enabled) "enable" else "disable"
        instrumentation.uiAutomation.executeShellCommand("cmd connectivity airplane-mode $state").close()
    }

    private companion object {
        const val SETTLE_MILLIS = 15_000L
    }
}
