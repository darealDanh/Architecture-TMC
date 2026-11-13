package com.tmc.mobile.di

import android.content.Context
import com.tmc.mobile.data.LocalStorage
import com.tmc.mobile.data.attendance.OfflineFirstAttendanceRepository
import com.tmc.mobile.data.network.ConnectivityNetworkStatus
import com.tmc.mobile.domain.attendance.AttendanceRepository
import com.tmc.mobile.domain.port.AttendanceApi
import com.tmc.mobile.domain.port.AttendanceCache
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.domain.port.SessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun localStorage(@ApplicationContext context: Context): LocalStorage = LocalStorage(context)

    @Provides
    fun sessionStore(storage: LocalStorage): SessionStore = storage.sessionStore

    @Provides
    fun certificateStore(storage: LocalStorage): CertificateStore = storage.certificateStore

    @Provides
    fun attendanceCache(storage: LocalStorage): AttendanceCache = storage.attendanceCache

    @Provides
    @Singleton
    fun networkStatus(@ApplicationContext context: Context): NetworkStatus =
        ConnectivityNetworkStatus(context, CoroutineScope(SupervisorJob() + Dispatchers.Default))

    @Provides
    @Singleton
    fun attendanceRepository(api: AttendanceApi, cache: AttendanceCache, network: NetworkStatus): AttendanceRepository =
        OfflineFirstAttendanceRepository(api, cache, network)
}
