package com.tmc.mobile.di

import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.PayloadSigner
import com.tmc.mobile.security.AndroidKeystoreDeviceKeyStore
import com.tmc.mobile.security.AndroidKeystorePayloadSigner
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SecurityModule {

    @Provides
    @Singleton
    fun deviceKeyStore(): DeviceKeyStore = AndroidKeystoreDeviceKeyStore()

    @Provides
    @Singleton
    fun payloadSigner(): PayloadSigner = AndroidKeystorePayloadSigner()
}
