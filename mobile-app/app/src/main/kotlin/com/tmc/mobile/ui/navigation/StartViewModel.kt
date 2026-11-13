package com.tmc.mobile.ui.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.tmc.mobile.domain.session.IsDeviceEnrolled
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
    private val isDeviceEnrolled: IsDeviceEnrolled,
) : ViewModel() {

    private val mutableStart = MutableStateFlow<StartDestination?>(null)
    val start: StateFlow<StartDestination?> = mutableStart.asStateFlow()

    init {
        viewModelScope.launch {
            mutableStart.value = if (isDeviceEnrolled()) StartDestination.Main else StartDestination.SignIn
        }
    }
}
