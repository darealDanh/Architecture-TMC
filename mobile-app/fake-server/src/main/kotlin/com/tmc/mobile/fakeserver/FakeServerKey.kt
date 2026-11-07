package com.tmc.mobile.fakeserver

import java.security.KeyFactory
import java.security.PrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.util.Base64

internal object FakeServerKey {

    private const val PRIVATE_PKCS8 =
        "MEECAQAwEwYHKoZIzj0CAQYIKoZIzj0DAQcEJzAlAgEBBCALvRsEGQl/qf9Nafjnw/abUqJdOg6KX1H70n7OXOxCgw=="
    private const val PUBLIC_X509 =
        "MFkwEwYHKoZIzj0CAQYIKoZIzj0DAQcDQgAE1TzKw0WIsI//25AmoLzb07HCFhDm+9UprmzimHga5yMvxtUmPhlK2CrJQdP319VC8JlMqKx/B5Z0Kr1e51sjvQ=="

    val privateKey: PrivateKey by lazy {
        KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(Base64.getDecoder().decode(PRIVATE_PKCS8)))
    }

    val publicKey: ByteArray get() = Base64.getDecoder().decode(PUBLIC_X509)
}
