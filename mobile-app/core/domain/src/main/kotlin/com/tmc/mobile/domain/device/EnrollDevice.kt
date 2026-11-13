package com.tmc.mobile.domain.device

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.EnrollmentApi

class EnrollDevice(
    private val enrollment: EnrollmentApi,
    private val keyStore: DeviceKeyStore,
    private val certificates: CertificateStore,
) {

    suspend operator fun invoke(token: String): DeviceCertificate =
        enrollment.issueCertificate(token, keyStore.createKey()).also { certificates.save(it) }
}
