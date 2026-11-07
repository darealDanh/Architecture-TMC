package com.tmc.mobile.domain.pass

import java.util.Base64
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test

class SecureRandomNonceSourceTest {

    private val source = SecureRandomNonceSource()

    @Test
    fun noncesAreSixteenRandomBytesInBase64Url() {
        val nonce = source.next()

        assertEquals(16, Base64.getUrlDecoder().decode(nonce).size)
    }

    @Test
    fun noncesDoNotRepeat() {
        assertEquals(1000, List(1000) { source.next() }.toSet().size)
    }
}
