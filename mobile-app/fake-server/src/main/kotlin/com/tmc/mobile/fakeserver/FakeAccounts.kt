package com.tmc.mobile.fakeserver

import com.tmc.mobile.domain.model.Employee

internal object FakeAccounts {

    private const val PASSWORD = "password"

    private val employees = listOf(
        Employee("E001", "Nguyen Van A", "Assembly", "Binh Duong"),
        Employee("E002", "Tran Thi B", "Packing", "Long An"),
    ).associateBy(Employee::id)

    fun authenticate(employeeId: String, password: String): Employee? =
        employees[employeeId]?.takeIf { password == PASSWORD }

    fun find(employeeId: String): Employee? = employees[employeeId]
}
