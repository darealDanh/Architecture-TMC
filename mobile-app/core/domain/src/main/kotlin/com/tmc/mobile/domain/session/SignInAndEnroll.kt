package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.device.EnrollDevice
import com.tmc.mobile.domain.model.Session
import com.tmc.mobile.domain.port.AuthApi
import com.tmc.mobile.domain.port.SessionStore

class SignInAndEnroll(
    private val auth: AuthApi,
    private val enrollDevice: EnrollDevice,
    private val sessions: SessionStore,
) {

    suspend operator fun invoke(employeeId: String, password: String): Session {
        val session = auth.signIn(employeeId, password)
        enrollDevice(session.token)
        sessions.save(session)
        return session
    }
}
