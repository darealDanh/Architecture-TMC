package com.tmc.mobile.domain.port

interface DeviceKeyStore {

    fun hasKey(): Boolean

    fun createKey(): ByteArray

    fun publicKey(): ByteArray?

    fun isHardwareBacked(): Boolean

    fun delete()
}
