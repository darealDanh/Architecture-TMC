package com.tmc.mobile.ui.device

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tmc.mobile.domain.device.DeviceStatus
import com.tmc.mobile.ui.components.ChipTone
import com.tmc.mobile.ui.components.KeyValueRows
import com.tmc.mobile.ui.components.ScreenTitle
import com.tmc.mobile.ui.components.SectionLabel
import com.tmc.mobile.ui.components.StatusChip
import com.tmc.mobile.ui.components.TmcCard
import com.tmc.mobile.ui.theme.TmcColors
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val expiryFormat = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

@Composable
fun DeviceRoute(onSignedOut: () -> Unit, viewModel: DeviceViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.signedOut) {
        if (state.signedOut) onSignedOut()
    }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            viewModel.messageShown()
        }
    }
    Box(Modifier.fillMaxSize()) {
        DeviceScreen(state, onRenew = viewModel::renew, onSignOut = viewModel::signOut)
        SnackbarHost(snackbar, Modifier.align(Alignment.BottomCenter))
    }
}

@Composable
fun DeviceScreen(state: DeviceUiState, onRenew: () -> Unit, onSignOut: () -> Unit) {
    var confirmSignOut by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().statusBarsPadding().verticalScroll(rememberScrollState())) {
        ScreenTitle("Device", "Security of this phone")
        Column(
            modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            state.status?.let { StatusCards(it) }
            FilledTonalButton(
                onClick = onRenew,
                enabled = !state.renewing,
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                if (state.renewing) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Text("Renew certificate", maxLines = 1)
                }
            }
            OutlinedButton(
                onClick = { confirmSignOut = true },
                border = BorderStroke(1.dp, TmcColors.OutlineVariant),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TmcColors.Error),
                modifier = Modifier.fillMaxWidth().height(44.dp),
            ) {
                Text("Sign out and remove key", maxLines = 1)
            }
        }
    }
    if (confirmSignOut) {
        AlertDialog(
            onDismissRequest = { confirmSignOut = false },
            title = { Text("Sign out?") },
            text = { Text("This removes the device key and saved attendance from this phone. You will need to sign in again to get a pass.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmSignOut = false
                    onSignOut()
                }) { Text("Sign out", color = TmcColors.Error) }
            },
            dismissButton = { TextButton(onClick = { confirmSignOut = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun StatusCards(status: DeviceStatus) {
    SectionLabel("Device key")
    TmcCard {
        KeyValueRows(
            listOf(
                "Storage" to { Value("Android Keystore") },
                "Algorithm" to { Value(status.algorithm) },
                "Hardware-backed" to {
                    if (status.hardwareBacked) StatusChip("Yes", ChipTone.Ok) else StatusChip("No", ChipTone.Warning)
                },
            ),
        )
    }
    SectionLabel("Certificate")
    TmcCard {
        KeyValueRows(
            listOf(
                "Status" to {
                    if (status.certificateActive) StatusChip("Active", ChipTone.Ok) else StatusChip("Expired", ChipTone.Error)
                },
                "Expires" to {
                    Value(status.certificateExpiresAt?.atZone(ZoneId.systemDefault())?.format(expiryFormat) ?: "–")
                },
            ),
        )
    }
}

@Composable
private fun Value(text: String) {
    Text(text, style = MaterialTheme.typography.bodyMedium, maxLines = 1)
}
