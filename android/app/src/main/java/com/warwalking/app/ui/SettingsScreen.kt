package com.warwalking.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.warwalking.app.WigleCredentialStore

data class ScanSettings(
    val scan24GHz: Boolean = true,
    val scan5GHz: Boolean = true,
    val scanBluetoothLe: Boolean = true
)

@Composable
fun SettingsScreen(
    viewModel: CredentialViewModel,
    scanSettings: ScanSettings,
    onScanSettingsChange: (ScanSettings) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    var apiName by remember { mutableStateOf(WigleCredentialStore.getApiName(context) ?: "") }
    var apiToken by remember { mutableStateOf(WigleCredentialStore.getApiToken(context) ?: "") }
    var signedIn by remember { mutableStateOf(WigleCredentialStore.hasCredentials(context)) }

    LaunchedEffect(state) {
        signedIn = WigleCredentialStore.hasCredentials(context)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text("WiGLE Account", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            "Get your API Name and Token from wigle.net -> Account. This is not your website password.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (signedIn) {
            Text(
                "Signed in${WigleCredentialStore.getUsername(context)?.let { " as $it" } ?: ""}",
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(onClick = {
                viewModel.signOut()
                apiName = ""
                apiToken = ""
            }) { Text("Sign Out") }
            Spacer(modifier = Modifier.height(16.dp))
        }

        OutlinedTextField(
            value = apiName, onValueChange = { apiName = it },
            label = { Text("WiGLE API Name") }, modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = apiToken, onValueChange = { apiToken = it },
            label = { Text("WiGLE API Token") },
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { viewModel.verifyAndSave(apiName, apiToken) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Verify & Save")
        }

        when (val current = state) {
            is CredentialUiState.Loading -> {
                Spacer(modifier = Modifier.height(12.dp))
                CircularProgressIndicator()
            }
            is CredentialUiState.Success -> {
                Spacer(modifier = Modifier.height(12.dp))
                Text("Credentials verified and saved.", color = MaterialTheme.colorScheme.primary)
            }
            is CredentialUiState.Error -> {
                Spacer(modifier = Modifier.height(12.dp))
                Text(current.errorMessage, color = MaterialTheme.colorScheme.error)
            }
            is CredentialUiState.Idle -> Unit
        }

        Spacer(modifier = Modifier.height(24.dp))
        Divider()
        Spacer(modifier = Modifier.height(16.dp))

        Text("Scanning", style = MaterialTheme.typography.titleLarge)
        Spacer(modifier = Modifier.height(8.dp))

        ScanToggleRow("2.4GHz Wi-Fi scanning", scanSettings.scan24GHz) {
            onScanSettingsChange(scanSettings.copy(scan24GHz = it))
        }
        ScanToggleRow("5GHz Wi-Fi scanning", scanSettings.scan5GHz) {
            onScanSettingsChange(scanSettings.copy(scan5GHz = it))
        }
        ScanToggleRow("Bluetooth Low Energy discovery", scanSettings.scanBluetoothLe) {
            onScanSettingsChange(scanSettings.copy(scanBluetoothLe = it))
        }
    }
}

@Composable
private fun ScanToggleRow(label: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
