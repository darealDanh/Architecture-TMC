package com.tmc.mobile.domain.pass

import java.util.Base64

internal object PayloadSegments {

    const val SEPARATOR = "."

    private val encoder = Base64.getUrlEncoder().withoutPadding()
    private val decoder = Base64.getUrlDecoder()

    fun ofBytes(value: ByteArray): String = encoder.encodeToString(value)

    fun ofText(value: String): String = ofBytes(value.toByteArray(Charsets.UTF_8))

    fun toBytes(segment: String): ByteArray = decoder.decode(segment)

    fun toText(segment: String): String = String(toBytes(segment), Charsets.UTF_8)

    fun join(segments: List<String>): ByteArray = segments.joinToString(SEPARATOR).toByteArray(Charsets.UTF_8)
}
