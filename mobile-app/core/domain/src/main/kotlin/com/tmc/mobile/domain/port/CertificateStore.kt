package com.tmc.mobile.domain.port

import com.tmc.mobile.domain.model.DeviceCertificate

interface CertificateStore {

    suspend fun current(): DeviceCertificate?

    suspend fun save(certificate: DeviceCertificate)

    suspend fun clear()
}
