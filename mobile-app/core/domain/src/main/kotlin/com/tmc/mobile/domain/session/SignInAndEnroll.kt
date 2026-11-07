package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.port.AuthApi
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.port.SessionStore

class SignInAndEnroll(
    private val auth: AuthApi,
    private val enrollment: EnrollmentApi,
    private val keyStore: DeviceKeyStore,
    private val sessions: SessionStore,
    private val certificates: CertificateStore,
) {

    suspend operator fun invoke(employeeId: String, password: String): Session {
        val session = auth.signIn(employeeId, password)
        val publicKey = keyStore.publicKey() ?: keyStore.createKey()
        val certificate = enrollment.issueCertificate(session.token, publicKey)
        certificates.save(certificate)
        sessions.save(session)
        return session
    }
}
