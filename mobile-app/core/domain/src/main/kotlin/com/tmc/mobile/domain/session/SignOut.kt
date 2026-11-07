package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.port.AttendanceCache
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.SessionStore

class SignOut(
    private val keyStore: DeviceKeyStore,
    private val sessions: SessionStore,
    private val certificates: CertificateStore,
    private val attendanceCache: AttendanceCache,
) {

    suspend operator fun invoke() {
        sessions.clear()
        certificates.clear()
        attendanceCache.clear()
        keyStore.delete()
    }
}
