package com.tmc.mobile.domain.session

import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.domain.port.SessionStore

class GetSignedInEmployee(private val sessions: SessionStore) {

    suspend operator fun invoke(): Employee? = sessions.current()?.employee
}
