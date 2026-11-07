package com.tmc.mobile.domain.port

import kotlinx.coroutines.flow.StateFlow

interface NetworkStatus {

    val isOnline: StateFlow<Boolean>
}
