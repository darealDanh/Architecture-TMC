package com.tmc.mobile.security

import java.security.KeyStore

internal object AndroidKeystore {

    const val PROVIDER = "AndroidKeyStore"
    const val DEFAULT_ALIAS = "tmc_device_key"

    fun load(): KeyStore = KeyStore.getInstance(PROVIDER).apply { load(null) }
}
