package com.tmc.mobile.ui.device

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.device.DeviceStatus
import com.tmc.mobile.domain.device.GetDeviceStatus
import com.tmc.mobile.domain.device.RenewCertificate
import com.tmc.mobile.domain.port.ServerUnreachableException
import com.tmc.mobile.domain.port.SessionExpiredException
import com.tmc.mobile.domain.session.SignOut
import com.tmc.mobile.ui.UserMessages
import com.tmc.mobile.ui.rethrowIfCancelled
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DeviceUiState(
    val status: DeviceStatus? = null,
    val renewing: Boolean = false,
    val signedOut: Boolean = false,
    val message: String? = null,
)

@HiltViewModel
class DeviceViewModel @Inject constructor(
    private val getDeviceStatus: GetDeviceStatus,
    private val renewCertificate: RenewCertificate,
    private val signOutUseCase: SignOut,
) : ViewModel() {

    private val mutableState = MutableStateFlow(DeviceUiState())
    val state: StateFlow<DeviceUiState> = mutableState.asStateFlow()

    init {
        viewModelScope.launch { refreshStatus() }
    }

    fun renew() {
        if (mutableState.value.renewing) {
            return
        }
        mutableState.update { it.copy(renewing = true) }
        viewModelScope.launch {
            val message = try {
                renewCertificate()
                RENEWED
            } catch (e: ServerUnreachableException) {
                UserMessages.UNREACHABLE
            } catch (e: SessionExpiredException) {
                UserMessages.SESSION_EXPIRED
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                UserMessages.UNEXPECTED
            }
            refreshStatus()
            mutableState.update { it.copy(renewing = false, message = message) }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            try {
                signOutUseCase()
                mutableState.update { it.copy(signedOut = true) }
            } catch (e: Exception) {
                e.rethrowIfCancelled()
                mutableState.update { it.copy(message = UserMessages.UNEXPECTED) }
            }
        }
    }

    fun messageShown() {
        mutableState.update { it.copy(message = null) }
    }

    private suspend fun refreshStatus() {
        try {
            val status = getDeviceStatus()
            mutableState.update { it.copy(status = status) }
        } catch (e: Exception) {
            e.rethrowIfCancelled()
            mutableState.update { it.copy(message = UserMessages.UNEXPECTED) }
        }
    }

    private companion object {
        const val RENEWED = "Certificate renewed"
    }
}
