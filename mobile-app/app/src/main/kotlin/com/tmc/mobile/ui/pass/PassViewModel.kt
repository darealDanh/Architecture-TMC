package com.tmc.mobile.ui.pass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.device.RenewCertificate
import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.domain.pass.GeneratePass
import com.tmc.mobile.domain.pass.ObservePassState
import com.tmc.mobile.domain.pass.PassState
import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.domain.port.ServerUnreachableException
import com.tmc.mobile.domain.port.SessionExpiredException
import com.tmc.mobile.domain.session.GetSignedInEmployee
import com.tmc.mobile.ui.UserMessages
import com.tmc.mobile.ui.rethrowIfCancelled
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PassUiState(
    val employee: Employee? = null,
    val payload: String? = null,
    val secondsLeft: Int = 0,
    val totalSeconds: Int = GeneratePass.REFRESH_INTERVAL.seconds.toInt(),
    val online: Boolean = true,
    val certificateExpired: Boolean = false,
    val certificateExpiresAt: Instant? = null,
    val deviceKeyMissing: Boolean = false,
    val renewing: Boolean = false,
    val message: String? = null,
)

private data class RenewState(val renewing: Boolean = false, val message: String? = null)

@HiltViewModel
class PassViewModel @Inject constructor(
    observePassState: ObservePassState,
    network: NetworkStatus,
    getSignedInEmployee: GetSignedInEmployee,
    private val renewCertificate: RenewCertificate,
) : ViewModel() {

    private val renewState = MutableStateFlow(RenewState())

    val state: StateFlow<PassUiState> = combine(
        observePassState(),
        network.isOnline,
        flow { emit(getSignedInEmployee()) },
        renewState,
    ) { pass, online, employee, renew -> toUiState(pass, online, employee, renew) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PassUiState())

    fun renew() {
        if (renewState.value.renewing) {
            return
        }
        renewState.update { it.copy(renewing = true, message = null) }
        viewModelScope.launch {
            val message = try {
                renewCertificate()
                null
            } catch (e: ServerUnreachableException) {
                UserMessages.UNREACHABLE
            } catch (e: SessionExpiredException) {
                UserMessages.SESSION_EXPIRED
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                UserMessages.UNEXPECTED
            }
            renewState.value = RenewState(renewing = false, message = message)
        }
    }

    fun messageShown() {
        renewState.update { it.copy(message = null) }
    }

    private fun toUiState(pass: PassState, online: Boolean, employee: Employee?, renew: RenewState): PassUiState {
        val base = PassUiState(employee = employee, online = online, renewing = renew.renewing, message = renew.message)
        return when (pass) {
            PassState.NotEnrolled -> base
            PassState.DeviceKeyMissing -> base.copy(deviceKeyMissing = true)
            is PassState.CertificateExpired -> base.copy(certificateExpired = true, certificateExpiresAt = pass.expiredAt)
            is PassState.Ready -> base.copy(
                payload = pass.pass.payload,
                secondsLeft = pass.secondsLeft,
                certificateExpiresAt = pass.certificateExpiresAt,
            )
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
