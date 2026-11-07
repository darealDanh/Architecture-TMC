package com.tmc.mobile.ui

import kotlinx.coroutines.CancellationException

object UserMessages {
    const val UNREACHABLE = "Can't reach the server"
    const val SESSION_EXPIRED = "Your session has expired. Sign in again."
    const val UNEXPECTED = "Something went wrong. Try again."
}

fun Exception.rethrowIfCancelled() {
    if (this is CancellationException) {
        throw this
    }
}
