package com.tmc.mobile.security

import android.os.Build
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyInfo
import android.security.keystore.KeyProperties
import com.tmc.mobile.domain.port.DeviceKeyStore
import java.security.KeyFactory
import java.security.KeyPairGenerator
import java.security.PrivateKey
import java.security.spec.ECGenParameterSpec

class AndroidKeystoreDeviceKeyStore(
    private val alias: String = AndroidKeystore.DEFAULT_ALIAS,
) : DeviceKeyStore {

    override fun hasKey(): Boolean = AndroidKeystore.load().containsAlias(alias)

    override fun createKey(): ByteArray {
        publicKey()?.let { return it }
        val spec = KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_SIGN)
            .setAlgorithmParameterSpec(ECGenParameterSpec("secp256r1"))
            .setDigests(KeyProperties.DIGEST_SHA256)
            .build()
        return KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_EC, AndroidKeystore.PROVIDER)
            .apply { initialize(spec) }
            .generateKeyPair()
            .public
            .encoded
    }

    override fun publicKey(): ByteArray? = AndroidKeystore.load().getCertificate(alias)?.publicKey?.encoded

    override fun isHardwareBacked(): Boolean {
        val key = AndroidKeystore.load().getKey(alias, null) as? PrivateKey ?: return false
        val info = KeyFactory.getInstance(key.algorithm, AndroidKeystore.PROVIDER).getKeySpec(key, KeyInfo::class.java)
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            info.securityLevel == KeyProperties.SECURITY_LEVEL_TRUSTED_ENVIRONMENT ||
                info.securityLevel == KeyProperties.SECURITY_LEVEL_STRONGBOX
        } else {
            @Suppress("DEPRECATION")
            info.isInsideSecureHardware
        }
    }

    override fun delete() {
        val keyStore = AndroidKeystore.load()
        if (keyStore.containsAlias(alias)) {
            keyStore.deleteEntry(alias)
        }
    }
}
