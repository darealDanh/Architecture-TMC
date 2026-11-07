package com.tmc.mobile.security

import com.tmc.mobile.domain.pass.EcdsaP256
import com.tmc.mobile.domain.port.PayloadSigner
import java.security.PrivateKey
import java.security.Signature

class AndroidKeystorePayloadSigner(
    private val alias: String = AndroidKeystore.DEFAULT_ALIAS,
) : PayloadSigner {

    override fun sign(data: ByteArray): ByteArray {
        val key = checkNotNull(AndroidKeystore.load().getKey(alias, null) as? PrivateKey) { "No device key" }
        return Signature.getInstance(EcdsaP256.SIGNATURE_ALGORITHM).run {
            initSign(key)
            update(data)
            sign()
        }
    }
}
