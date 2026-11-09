package com.tmc.mobile.domain.pass

import java.security.GeneralSecurityException
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.Signature
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec

object EcdsaP256 {

    const val SIGNATURE_ALGORITHM = "SHA256withECDSA"
    private const val KEY_ALGORITHM = "EC"
    private const val CURVE = "secp256r1"

    fun generateKeyPair(): KeyPair =
        KeyPairGenerator.getInstance(KEY_ALGORITHM).apply { initialize(ECGenParameterSpec(CURVE)) }.generateKeyPair()

    fun sign(key: PrivateKey, data: ByteArray): ByteArray =
        Signature.getInstance(SIGNATURE_ALGORITHM).run {
            initSign(key)
            update(data)
            sign()
        }

    fun verify(publicKeyX509: ByteArray, data: ByteArray, signature: ByteArray): Boolean =
        try {
            val key = KeyFactory.getInstance(KEY_ALGORITHM).generatePublic(X509EncodedKeySpec(publicKeyX509))
            Signature.getInstance(SIGNATURE_ALGORITHM).run {
                initVerify(key)
                update(data)
                verify(signature)
            }
        } catch (e: GeneralSecurityException) {
            false
        } catch (e: IllegalArgumentException) {
            false
        }
}
