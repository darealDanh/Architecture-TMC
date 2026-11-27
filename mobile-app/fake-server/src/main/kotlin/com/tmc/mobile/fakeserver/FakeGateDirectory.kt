package com.tmc.mobile.fakeserver

object FakeGateDirectory {
    val publicKey: ByteArray get() = FakeServerKey.publicKey

    fun isAuthorized(employeeId: String, factory: String): Boolean =
        FakeAccounts.find(employeeId)?.factory == factory
}
