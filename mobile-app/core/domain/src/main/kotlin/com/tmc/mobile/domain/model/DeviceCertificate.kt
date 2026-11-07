package com.tmc.mobile.domain.model

import com.tmc.mobile.domain.pass.PayloadSegments
import java.time.Instant

class DeviceCertificate(
    val serial: String,
    val employeeId: String,
    val devicePublicKey: ByteArray,
    val expiresAt: Instant,
    val serverSignature: ByteArray,
) {

    fun signedContent(): ByteArray = PayloadSegments.join(contentSegments())

    fun withServerSignature(signature: ByteArray): DeviceCertificate =
        DeviceCertificate(serial, employeeId, devicePublicKey, expiresAt, signature)

    fun isValidAt(instant: Instant): Boolean = instant.isBefore(expiresAt)

    internal fun contentSegments(): List<String> = listOf(
        PayloadSegments.ofText(serial),
        PayloadSegments.ofText(employeeId),
        PayloadSegments.ofBytes(devicePublicKey),
        PayloadSegments.ofText(expiresAt.epochSecond.toString()),
    )

    override fun equals(other: Any?): Boolean =
        other is DeviceCertificate &&
            serial == other.serial &&
            employeeId == other.employeeId &&
            devicePublicKey.contentEquals(other.devicePublicKey) &&
            expiresAt == other.expiresAt &&
            serverSignature.contentEquals(other.serverSignature)

    override fun hashCode(): Int {
        var result = serial.hashCode()
        result = 31 * result + employeeId.hashCode()
        result = 31 * result + devicePublicKey.contentHashCode()
        result = 31 * result + expiresAt.hashCode()
        result = 31 * result + serverSignature.contentHashCode()
        return result
    }

    override fun toString(): String = "DeviceCertificate(serial=$serial, employeeId=$employeeId, expiresAt=$expiresAt)"
}
