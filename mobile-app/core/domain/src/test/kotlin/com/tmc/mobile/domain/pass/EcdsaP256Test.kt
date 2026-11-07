package com.tmc.mobile.domain.pass

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test

class EcdsaP256Test {

    private val keys = EcdsaP256.generateKeyPair()
    private val data = "signed content".toByteArray()

    @Test
    fun signatureMadeOverSignedContentVerifies() {
        assertTrue(EcdsaP256.verify(keys.public.encoded, data, EcdsaP256.sign(keys.private, data)))
    }

    @Test
    fun tamperedSignatureDoesNotVerify() {
        val signature = EcdsaP256.sign(keys.private, data)
        signature[signature.size - 1] = (signature.last().toInt() xor 1).toByte()

        assertFalse(EcdsaP256.verify(keys.public.encoded, data, signature))
    }

    @Test
    fun verifyReturnsFalseForGarbageKey() {
        assertFalse(EcdsaP256.verify(byteArrayOf(1, 2, 3), data, EcdsaP256.sign(keys.private, data)))
    }

    @Test
    fun keysUseTheP256Curve() {
        assertTrue(keys.public.encoded.size == 91)
    }
}
