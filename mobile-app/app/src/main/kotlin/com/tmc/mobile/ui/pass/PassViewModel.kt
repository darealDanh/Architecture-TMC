package com.tmc.mobile.ui.pass

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.domain.pass.GeneratePass
import com.tmc.mobile.domain.pass.ObservePassState
import com.tmc.mobile.domain.pass.PassState
import com.tmc.mobile.domain.port.NetworkStatus
import com.tmc.mobile.domain.port.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn

data class PassUiState(
    val employee: Employee? = null,
    val payload: String? = null,
    val secondsLeft: Int = 0,
    val totalSeconds: Int = GeneratePass.REFRESH_INTERVAL.seconds.toInt(),
    val online: Boolean = true,
    val certificateExpired: Boolean = false,
    val certificateExpiresAt: Instant? = null,
)

@HiltViewModel
class PassViewModel @Inject constructor(
    observePassState: ObservePassState,
    network: NetworkStatus,
    sessions: SessionStore,
) : ViewModel() {

    val state: StateFlow<PassUiState> = combine(
        observePassState(),
        network.isOnline,
        flow { emit(sessions.current()?.employee) },
    ) { pass, online, employee -> toUiState(pass, online, employee) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), PassUiState())

    private fun toUiState(pass: PassState, online: Boolean, employee: Employee?): PassUiState {
        val base = PassUiState(employee = employee, online = online)
        return when (pass) {
            PassState.NotEnrolled -> base
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
