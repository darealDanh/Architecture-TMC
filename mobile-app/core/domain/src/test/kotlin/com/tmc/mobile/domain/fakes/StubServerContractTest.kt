package com.tmc.mobile.domain.fakes

import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class StubServerContractTest {

    @Test
    fun stubServerMeetsTheServerApiContract() = runTest {
        val clock = MutableClock(Instant.parse("2026-11-07T07:00:00Z"))
        val network = MutableNetworkStatus()
        val server = StubServer(clock, network)

        ServerApiContract(server, network, clock, server.serverPublicKey()).runAll()
    }
}
