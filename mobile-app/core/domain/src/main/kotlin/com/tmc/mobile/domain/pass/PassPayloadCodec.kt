package com.tmc.mobile.domain.pass

import com.tmc.mobile.domain.model.DeviceCertificate
import java.time.DateTimeException
import java.time.Instant

object PassPayloadCodec {

    private const val VERSION = "v2"
    private const val SEGMENT_COUNT = 9

    fun signedContent(certificate: DeviceCertificate, issuedAt: Instant, nonce: String): ByteArray =
        PayloadSegments.join(passSegments(certificate, issuedAt, nonce))

    fun encode(certificate: DeviceCertificate, issuedAt: Instant, nonce: String, deviceSignature: ByteArray): String =
        (listOf(VERSION) + passSegments(certificate, issuedAt, nonce) + PayloadSegments.ofBytes(deviceSignature))
            .joinToString(PayloadSegments.SEPARATOR)

    fun decode(raw: String): DecodedPass {
        val segments = raw.split(PayloadSegments.SEPARATOR)
        if (segments.size != SEGMENT_COUNT || segments[0] != VERSION) {
            throw MalformedPassException("Unrecognised pass format")
        }
        return try {
            DecodedPass(
                certificate = DeviceCertificate(
                    serial = PayloadSegments.toText(segments[1]),
                    employeeId = PayloadSegments.toText(segments[2]),
                    devicePublicKey = PayloadSegments.toBytes(segments[3]),
                    expiresAt = Instant.ofEpochSecond(PayloadSegments.toText(segments[4]).toLong()),
                    serverSignature = PayloadSegments.toBytes(segments[5]),
                ),
                issuedAt = Instant.ofEpochMilli(PayloadSegments.toText(segments[6]).toLong()),
                nonce = segments[7].also(PayloadSegments::toBytes),
                deviceSignature = PayloadSegments.toBytes(segments[8]),
            )
        } catch (e: IllegalArgumentException) {
            throw MalformedPassException("Unreadable pass content", e)
        } catch (e: DateTimeException) {
            throw MalformedPassException("Unreadable pass content", e)
        }
    }

    private fun passSegments(certificate: DeviceCertificate, issuedAt: Instant, nonce: String): List<String> =
        certificate.contentSegments() +
            PayloadSegments.ofBytes(certificate.serverSignature) +
            PayloadSegments.ofText(issuedAt.toEpochMilli().toString()) +
            nonce
}
