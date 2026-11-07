package com.tmc.mobile.domain.device

import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import java.time.Clock

class GetDeviceStatus(
    private val keyStore: DeviceKeyStore,
    private val certificates: CertificateStore,
    private val clock: Clock,
) {

    suspend operator fun invoke(): DeviceStatus {
        val certificate = certificates.current()
        return DeviceStatus(
            hardwareBacked = keyStore.hasKey() && keyStore.isHardwareBacked(),
            algorithm = ALGORITHM,
            certificateExpiresAt = certificate?.expiresAt,
            certificateActive = certificate?.isValidAt(clock.instant()) == true,
        )
    }

    private companion object {
        const val ALGORITHM = "ECDSA P-256"
    }
}
