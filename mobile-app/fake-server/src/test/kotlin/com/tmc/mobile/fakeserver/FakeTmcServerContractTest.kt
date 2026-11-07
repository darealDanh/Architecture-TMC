package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.fakes.MutableClock
import com.tmc.mobile.domain.fakes.MutableNetworkStatus
import com.tmc.mobile.domain.fakes.ServerApiContract
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class FakeTmcServerContractTest {

    @Test
    fun fakeServerMeetsTheServerApiContract() = runTest {
        val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
        val network = MutableNetworkStatus()
        val server = FakeTmcServer(network, clock, latency = Duration.ZERO)

        ServerApiContract(server, network, clock, server.serverPublicKey()).runAll()
    }
}
