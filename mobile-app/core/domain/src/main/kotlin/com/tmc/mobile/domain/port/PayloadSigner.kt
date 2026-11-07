package com.tmc.mobile.domain.port

fun interface PayloadSigner {

    fun sign(data: ByteArray): ByteArray
}
