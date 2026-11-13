package com.tmc.mobile.domain.device

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.port.SessionExpiredException
import com.tmc.mobile.domain.port.SessionStore

class RenewCertificate(
    private val sessions: SessionStore,
    private val enrollDevice: EnrollDevice,
) {

    suspend operator fun invoke(): DeviceCertificate {
        val session = sessions.current() ?: throw SessionExpiredException()
        return enrollDevice(session.token)
    }
}
