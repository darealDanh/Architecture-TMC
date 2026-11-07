package com.tmc.mobile.domain.fakes

import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.domain.port.NonceSource
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class MutableNetworkStatus(online: Boolean = true) : NetworkStatus {

    private val state = MutableStateFlow(online)

    override val isOnline: StateFlow<Boolean> = state

    fun goOffline() {
        state.value = false
    }

    fun goOnline() {
        state.value = true
    }
}

class MutableClock(private var now: Instant, private val zone: ZoneId = ZoneOffset.UTC) : Clock() {

    fun advance(duration: Duration) {
        now = now.plus(duration)
    }

    override fun instant(): Instant = now

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now, zone)
}

class SequenceNonceSource : NonceSource {

    private var counter = 0

    override fun next(): String =
        Base64.getUrlEncoder().withoutPadding().encodeToString("nonce-${counter++}".toByteArray())
}
