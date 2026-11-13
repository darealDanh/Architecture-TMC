package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.SessionStore

class IsDeviceEnrolled(
    private val sessions: SessionStore,
    private val certificates: CertificateStore,
    private val keyStore: DeviceKeyStore,
) {

    suspend operator fun invoke(): Boolean =
        sessions.current() != null && certificates.current() != null && keyStore.hasKey()
}
