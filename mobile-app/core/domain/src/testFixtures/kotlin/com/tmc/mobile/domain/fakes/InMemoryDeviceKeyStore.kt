package com.tmc.mobile.domain.fakes

import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.PayloadSigner
import java.security.KeyPair

class InMemoryDeviceKeyStore : DeviceKeyStore, PayloadSigner {

    private var keyPair: KeyPair? = null

    var keysCreated = 0
        private set

    override fun hasKey(): Boolean = keyPair != null

    override fun createKey(): ByteArray {
        val existing = keyPair
        if (existing != null) {
            return existing.public.encoded
        }
        keysCreated++
        return EcdsaP256.generateKeyPair().also { keyPair = it }.public.encoded
    }

    override fun publicKey(): ByteArray? = keyPair?.public?.encoded

    override fun isHardwareBacked(): Boolean = false

    override fun delete() {
        keyPair = null
    }

    override fun sign(data: ByteArray): ByteArray {
        val key = checkNotNull(keyPair) { "No device key" }
        return EcdsaP256.sign(key.private, data)
    }
}
