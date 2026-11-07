package com.tmc.mobile.domain.port

fun interface NonceSource {

    fun next(): String
}
