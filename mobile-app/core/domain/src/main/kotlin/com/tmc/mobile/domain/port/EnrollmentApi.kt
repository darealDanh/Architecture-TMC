package com.tmc.mobile.domain.port

import com.tmc.mobile.domain.model.DeviceCertificate

interface EnrollmentApi {

    suspend fun issueCertificate(token: String, devicePublicKey: ByteArray): DeviceCertificate
}
