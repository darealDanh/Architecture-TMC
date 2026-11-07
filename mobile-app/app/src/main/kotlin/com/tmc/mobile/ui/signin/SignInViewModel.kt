package com.tmc.mobile.ui.signin

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.port.InvalidCredentialsException
import com.tmc.mobile.domain.port.ServerUnreachableException
import com.tmc.mobile.domain.session.SignInAndEnroll
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SignInUiState(
    val employeeId: String = "",
    val password: String = "",
    val loading: Boolean = false,
    val error: String? = null,
    val signedIn: Boolean = false,
)

@HiltViewModel
class SignInViewModel @Inject constructor(
    private val signInAndEnroll: SignInAndEnroll,
) : ViewModel() {

    private val mutableState = MutableStateFlow(SignInUiState())
    val state: StateFlow<SignInUiState> = mutableState.asStateFlow()

    fun onEmployeeIdChange(value: String) {
        mutableState.update { it.copy(employeeId = value.trim().uppercase(), error = null) }
    }

    fun onPasswordChange(value: String) {
        mutableState.update { it.copy(password = value, error = null) }
    }

    fun submit() {
        val current = mutableState.value
        if (current.loading) {
            return
        }
        if (current.employeeId.isBlank() || current.password.isBlank()) {
            mutableState.update { it.copy(error = EMPTY_FIELDS) }
            return
        }
        mutableState.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val error = try {
                signInAndEnroll(current.employeeId, current.password)
                null
            } catch (e: InvalidCredentialsException) {
                WRONG_CREDENTIALS
            } catch (e: ServerUnreachableException) {
                UNREACHABLE
            }
            mutableState.update { it.copy(loading = false, error = error, signedIn = error == null) }
        }
    }

    private companion object {
        const val EMPTY_FIELDS = "Enter your employee ID and password"
        const val WRONG_CREDENTIALS = "Wrong employee ID or password"
        const val UNREACHABLE = "Can't reach the server"
    }
}
