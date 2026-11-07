package com.tmc.mobile.domain.fakes

import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.PayloadSigner
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeviceKeyStoreContract(private val store: DeviceKeyStore, private val signer: PayloadSigner) {

    fun createdKeyHasPublicKey() {
        val created = store.createKey()

        assertTrue(store.hasKey())
        assertContentEquals(created, store.publicKey())
    }

    fun signaturesVerifyWithPublicKey() {
        store.createKey()
        val data = "pass content".toByteArray()

        val signature = signer.sign(data)

        assertTrue(EcdsaP256.verify(assertNotNull(store.publicKey()), data, signature))
    }

    fun createKeyWhenOneExistsKeepsTheSameKey() {
        val first = store.createKey()

        assertContentEquals(first, store.createKey())
    }

    fun deleteRemovesKey() {
        store.createKey()

        store.delete()

        assertFalse(store.hasKey())
        assertNull(store.publicKey())
    }

    fun runAll(reset: () -> Unit) {
        listOf(
            ::createdKeyHasPublicKey,
            ::signaturesVerifyWithPublicKey,
            ::createKeyWhenOneExistsKeepsTheSameKey,
            ::deleteRemovesKey,
        ).forEach { check ->
            reset()
            check()
        }
    }
}
