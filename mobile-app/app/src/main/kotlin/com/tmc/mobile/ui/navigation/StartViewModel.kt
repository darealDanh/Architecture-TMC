package com.tmc.mobile.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.port.CertificateStore
import com.tmc.mobile.domain.port.DeviceKeyStore
import com.tmc.mobile.domain.port.SessionStore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class StartDestination {
    SignIn,
    Main,
}

@HiltViewModel
class StartViewModel @Inject constructor(
    private val sessions: SessionStore,
    private val certificates: CertificateStore,
    private val keyStore: DeviceKeyStore,
) : ViewModel() {

    private val mutableStart = MutableStateFlow<StartDestination?>(null)
    val start: StateFlow<StartDestination?> = mutableStart.asStateFlow()

    init {
        viewModelScope.launch {
            val enrolled = sessions.current() != null && certificates.current() != null && keyStore.hasKey()
            mutableStart.value = if (enrolled) StartDestination.Main else StartDestination.SignIn
        }
    }
}
