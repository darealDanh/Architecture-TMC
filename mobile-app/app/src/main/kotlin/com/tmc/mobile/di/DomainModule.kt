package com.tmc.mobile.di

import com.tmc.mobile.domain.attendance.AttendanceRepository
import com.tmc.mobile.domain.attendance.AttendanceSummarizer
import com.tmc.mobile.domain.attendance.GetMonthlyAttendance
import com.tmc.mobile.domain.device.GetDeviceStatus
import com.tmc.mobile.domain.device.RenewCertificate
import com.tmc.mobile.domain.pass.GeneratePass
import com.tmc.mobile.domain.pass.ObservePassState
import com.tmc.mobile.domain.pass.SecureRandomNonceSource
import com.tmc.mobile.domain.port.AttendanceCache
import com.tmc.mobile.domain.port.AuthApi
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.EnrollmentApi
import com.tmc.mobile.domain.port.NonceSource
import com.tmc.mobile.domain.port.PayloadSigner
import com.tmc.mobile.domain.port.SessionStore
import com.tmc.mobile.domain.session.SignInAndEnroll
import com.tmc.mobile.domain.session.SignOut
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DomainModule {

    @Provides
    @Singleton
    fun clock(): Clock = Clock.systemDefaultZone()

    @Provides
    @Singleton
    fun nonceSource(): NonceSource = SecureRandomNonceSource()

    @Provides
    fun signInAndEnroll(
        auth: AuthApi,
        enrollment: EnrollmentApi,
        keyStore: DeviceKeyStore,
        sessions: SessionStore,
        certificates: CertificateStore,
    ) = SignInAndEnroll(auth, enrollment, keyStore, sessions, certificates)

    @Provides
    fun signOut(
        keyStore: DeviceKeyStore,
        sessions: SessionStore,
        certificates: CertificateStore,
        cache: AttendanceCache,
    ) = SignOut(keyStore, sessions, certificates, cache)

    @Provides
    fun observePassState(
        certificates: CertificateStore,
        signer: PayloadSigner,
        nonces: NonceSource,
        clock: Clock,
    ) = ObservePassState(certificates, GeneratePass(signer, nonces, clock), clock)

    @Provides
    fun getMonthlyAttendance(sessions: SessionStore, repository: AttendanceRepository) =
        GetMonthlyAttendance(sessions, repository, AttendanceSummarizer.standard())

    @Provides
    fun getDeviceStatus(keyStore: DeviceKeyStore, certificates: CertificateStore, clock: Clock) =
        GetDeviceStatus(keyStore, certificates, clock)

    @Provides
    fun renewCertificate(
        sessions: SessionStore,
        enrollment: EnrollmentApi,
        keyStore: DeviceKeyStore,
        certificates: CertificateStore,
    ) = RenewCertificate(sessions, enrollment, keyStore, certificates)
}
