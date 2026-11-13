package com.tmc.mobile.data.session

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.domain.model.Session
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class StoredCodecsTest {

    @Test
    fun sessionSurvivesSerialisationRoundTrip() {
        val session = Session("E001.1795000000.c2ln", Employee("E001", "Nguyen Van A", "Assembly", "Binh Duong"))

        assertEquals(session, SessionCodec.decode(SessionCodec.encode(session)))
    }

    @Test
    fun certificateSurvivesSerialisationRoundTrip() {
        val certificate = DeviceCertificate(
            serial = "0f8c1a2b-7c41-4a2e-9b1d-3e5f6a7b8c9d",
            employeeId = "E001",
            devicePublicKey = byteArrayOf(48, 89, 19, 7, 42),
            expiresAt = Instant.parse("2026-11-14T07:52:00Z"),
            serverSignature = byteArrayOf(9, 8, 7, 6),
        )

        assertEquals(certificate, CertificateCodec.decode(CertificateCodec.encode(certificate)))
    }

    @Test
    fun corruptedValueReadsAsMissing() {
        assertNull(SessionCodec.decode("not a stored session"))
        assertNull(CertificateCodec.decode("not.a.certificate"))
    }
}
