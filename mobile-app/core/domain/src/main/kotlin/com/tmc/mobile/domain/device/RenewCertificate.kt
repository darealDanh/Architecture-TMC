package com.tmc.mobile.domain.device

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.port.SessionExpiredException
import com.tmc.mobile.domain.port.SessionStore

class RenewCertificate(
    private val sessions: SessionStore,
    private val enrollment: EnrollmentApi,
    private val keyStore: DeviceKeyStore,
    private val certificates: CertificateStore,
) {

    suspend operator fun invoke(): DeviceCertificate {
        val session = sessions.current() ?: throw SessionExpiredException()
        val publicKey = keyStore.publicKey() ?: keyStore.createKey()
        return enrollment.issueCertificate(session.token, publicKey).also { certificates.save(it) }
    }
}
