package com.tmc.mobile.ui.pass

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tmc.mobile.domain.model.Employee
import com.tmc.mobile.ui.components.ChipTone
import com.tmc.mobile.ui.components.StatusChip
import com.tmc.mobile.ui.components.TmcCard
import com.tmc.mobile.ui.theme.TmcColors

@Composable
fun PassRoute(viewModel: PassViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    Box(Modifier.fillMaxSize()) {
        PassScreen(state, onRenew = viewModel::renew)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun PassScreen(state: PassUiState, onRenew: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 16.dp),
    ) {
        state.employee?.let { EmployeeHeader(it) }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            TmcCard {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (state.payload != null) {
                        CountdownRing(progress = state.secondsLeft.toFloat() / state.totalSeconds) {
                            QrImage(state.payload, size = 156.dp)
                        }
                        Text("Show this at the gate", style = MaterialTheme.typography.titleSmall, modifier = Modifier.padding(top = 12.dp))
                        Text(
                            "New code in ${state.secondsLeft} s",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        PassUnavailable(state, onRenew)
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (state.online) StatusChip("Online", ChipTone.Ok) else StatusChip("Offline", ChipTone.Warning)
                if (state.certificateExpired) {
                    StatusChip("Certificate expired", ChipTone.Error)
                } else if (state.payload != null) {
                    StatusChip("Code valid", ChipTone.Ok)
                }
            }
            Text(
                "Signed on this phone. Works without signal.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun EmployeeHeader(employee: Employee) {
    Row(
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(40.dp).background(TmcColors.Avatar, CircleShape), contentAlignment = Alignment.Center) {
            Text(initials(employee.name), color = TmcColors.OnAvatar, style = MaterialTheme.typography.titleSmall)
        }
        Column {
            Text(employee.name, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                "${employee.id} · ${employee.department} · ${employee.factory}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun PassUnavailable(state: PassUiState, onRenew: () -> Unit) {
    val (title, body) = when {
        state.deviceKeyMissing -> "This phone's key is missing" to "Sign out from the Device tab and sign in again."
        state.certificateExpired -> "Your device certificate has expired" to "Renew it while you are online."
        else -> "Preparing your pass" to null
    }
    Column(
        modifier = Modifier.padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, textAlign = TextAlign.Center)
        body?.let {
            Text(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
        if (state.certificateExpired) {
            FilledTonalButton(
                onClick = onRenew,
                enabled = !state.renewing,
                modifier = Modifier.padding(top = 10.dp).height(44.dp),
            ) {
                if (state.renewing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Renew certificate", maxLines = 1)
                }
            }
        }
    }
}

private fun initials(name: String): String =
    name.split(" ").filter(String::isNotBlank).let { listOfNotNull(it.firstOrNull(), it.lastOrNull()) }
        .joinToString("") { it.first().uppercase() }
