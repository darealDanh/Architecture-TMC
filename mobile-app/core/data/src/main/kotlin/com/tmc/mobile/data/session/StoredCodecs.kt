package com.tmc.mobile.data.session

import com.tmc.mobile.domain.model.DeviceCertificate
import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.domain.model.Session
import java.time.Instant
import java.util.Base64

private object Fields {

    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun join(vararg values: ByteArray): String = values.joinToString(".") { encoder.encodeToString(it) }

    fun split(stored: String, count: Int): List<ByteArray>? =
        stored.split(".").takeIf { it.size == count }?.map(decoder::decode)
}

internal object SessionCodec {

    fun encode(session: Session): String = with(session) {
        Fields.join(
            token.toByteArray(),
            employee.id.toByteArray(),
            employee.name.toByteArray(),
            employee.department.toByteArray(),
            employee.factory.toByteArray(),
        )
    }

    fun decode(stored: String): Session? = runCatching {
        val (token, id, name, department, factory) = Fields.split(stored, 5)!!.map(::String)
        Session(token, Employee(id, name, department, factory))
    }.getOrNull()
}

internal object CertificateCodec {

    fun encode(certificate: DeviceCertificate): String = with(certificate) {
        Fields.join(
            serial.toByteArray(),
            employeeId.toByteArray(),
            devicePublicKey,
            expiresAt.epochSecond.toString().toByteArray(),
            serverSignature,
        )
    }

    fun decode(stored: String): DeviceCertificate? = runCatching {
        val (serial, employeeId, publicKey, expiresAt, signature) = Fields.split(stored, 5)!!
        DeviceCertificate(
            serial = String(serial),
            employeeId = String(employeeId),
            devicePublicKey = publicKey,
            expiresAt = Instant.ofEpochSecond(String(expiresAt).toLong()),
            serverSignature = signature,
        )
    }.getOrNull()
}
