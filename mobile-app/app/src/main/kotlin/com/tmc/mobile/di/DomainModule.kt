package com.tmc.mobile.di

import com.tmc.mobile.domain.attendance.AttendanceRepository
import com.tmc.mobile.domain.attendance.AttendanceRule
import com.tmc.mobile.domain.attendance.AttendanceSummarizer
import com.tmc.mobile.domain.attendance.GetMonthlyAttendance
import com.tmc.mobile.domain.attendance.ManualEntryRule
import com.tmc.mobile.domain.attendance.MissingExitRule
import com.tmc.mobile.domain.device.EnrollDevice
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
import com.tmc.mobile.domain.session.GetSignedInEmployee
import com.tmc.mobile.domain.session.IsDeviceEnrolled
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
    fun attendanceRules(): List<AttendanceRule> = listOf(MissingExitRule(), ManualEntryRule())

    @Provides
    fun enrollDevice(enrollment: EnrollmentApi, keyStore: DeviceKeyStore, certificates: CertificateStore) =
        EnrollDevice(enrollment, keyStore, certificates)

    @Provides
    fun signInAndEnroll(auth: AuthApi, enrollDevice: EnrollDevice, sessions: SessionStore) =
        SignInAndEnroll(auth, enrollDevice, sessions)

    @Provides
    fun isDeviceEnrolled(sessions: SessionStore, certificates: CertificateStore, keyStore: DeviceKeyStore) =
        IsDeviceEnrolled(sessions, certificates, keyStore)

    @Provides
    fun getSignedInEmployee(sessions: SessionStore) = GetSignedInEmployee(sessions)

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
    fun getMonthlyAttendance(
        sessions: SessionStore,
        repository: AttendanceRepository,
        rules: List<@JvmSuppressWildcards AttendanceRule>,
    ) =
        GetMonthlyAttendance(sessions, repository, AttendanceSummarizer(rules))

    @Provides
    fun getDeviceStatus(keyStore: DeviceKeyStore, certificates: CertificateStore, clock: Clock) =
        GetDeviceStatus(keyStore, certificates, clock)

    @Provides
    fun renewCertificate(sessions: SessionStore, enrollDevice: EnrollDevice) = RenewCertificate(sessions, enrollDevice)
}
