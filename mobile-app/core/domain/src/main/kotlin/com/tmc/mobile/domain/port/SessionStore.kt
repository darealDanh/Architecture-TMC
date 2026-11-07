package com.tmc.mobile.domain.port

import com.tmc.mobile.domain.model.Session

interface SessionStore {

    suspend fun current(): Session?

    suspend fun save(session: Session)

    suspend fun clear()
}
