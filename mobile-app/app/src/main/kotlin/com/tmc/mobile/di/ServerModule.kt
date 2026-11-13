package com.tmc.mobile.di

import com.tmc.mobile.domain.port.AttendanceApi
import com.tmc.mobile.domain.port.AuthApi
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.fakeserver.FakeTmcServer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ServerModule {

    @Provides
    @Singleton
    fun server(network: NetworkStatus, clock: Clock): FakeTmcServer = FakeTmcServer(network, clock)

    @Provides
    fun authApi(server: FakeTmcServer): AuthApi = server

    @Provides
    fun enrollmentApi(server: FakeTmcServer): EnrollmentApi = server

    @Provides
    fun attendanceApi(server: FakeTmcServer): AttendanceApi = server
}
