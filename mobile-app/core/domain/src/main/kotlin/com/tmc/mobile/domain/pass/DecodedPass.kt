package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.model.DeviceCertificate
import java.time.Instant

class DecodedPass(
    val certificate: DeviceCertificate,
    val issuedAt: Instant,
    val nonce: String,
    val deviceSignature: ByteArray,
)
