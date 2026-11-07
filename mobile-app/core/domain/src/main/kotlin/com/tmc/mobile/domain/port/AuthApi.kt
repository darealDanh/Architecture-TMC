package com.tmc.mobile.domain.port

import com.tmc.mobile.domain.model.Session

interface AuthApi {

    suspend fun signIn(employeeId: String, password: String): Session
}
