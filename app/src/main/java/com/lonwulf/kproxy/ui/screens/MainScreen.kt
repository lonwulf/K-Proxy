package com.lonwulf.kproxy.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.lonwulf.kproxy.ProxyConfiguration
import com.lonwulf.kproxy.ProxyType
import com.lonwulf.kproxy.R
import com.lonwulf.kproxy.viewmodel.ConnectionState
import com.lonwulf.kproxy.viewmodel.MainViewModel

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    onConfigurationComplete: () -> Unit
) {
    var host by remember { mutableStateOf("") }
    var port by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var proxyType by remember { mutableStateOf(ProxyType.HTTP) }
    var showPassword by remember { mutableStateOf(false) }

    var hasError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val connectionState by viewModel.connectionState.collectAsState()
    val currentIp by viewModel.currentIp.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            elevation = CardDefaults.elevatedCardElevation(4.dp)
        ) {
            Column(
                modifier = modifier
                    .padding(16.dp)
            ) {
                Text(
                    text = "Connection Status",
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Status: ${connectionState.toDisplayString()}",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        currentIp?.let {
                            Text(
                                text = "Current IP: $it",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                    if (connectionState is ConnectionState.Connected) {
                        Button(
                            onClick = { viewModel.rotateIp() },
                            enabled = connectionState !is ConnectionState.Rotating,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Rotate IP",
                                    modifier = modifier.size(20.dp)
                                )
                                Text("Rotate IP")
                            }
                        }
                    }
                }
                if (connectionState is ConnectionState.Rotating) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                    )
                }
            }
        }
        Text(
            text = "Proxy Configuration",
            style = MaterialTheme.typography.headlineLarge,
            modifier = modifier.padding(bottom = 16.dp)
        )

        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text("Host") },
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = hasError && host.isEmpty()
        )
        OutlinedTextField(
            value = port,
            onValueChange = {
                if (it.isEmpty() || it.matches(Regex("^\\d*\$"))) {
                    port = it
                }
            },
            label = { Text("Port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            isError = hasError && (port.isEmpty() || port.toIntOrNull() == null)
        )
        OutlinedTextField(
            value = username,
            onValueChange = { username = it },
            label = { Text("Username") },
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        tint = colorResource(R.color.white),
                        painter = if (showPassword) painterResource(R.drawable.visibility_off_50dp_eye) else painterResource(
                            R.drawable.visibility_50dp_eye
                        ),
                        contentDescription = if (showPassword) "Hide password" else "Show password"
                    )
                }
            },
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        )

        Text(
            text = "Proxy Type",
            style = MaterialTheme.typography.headlineMedium,
            modifier = modifier.padding(vertical = 8.dp)
        )
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp)
        ) {
            ProxyType.values().forEach { type ->
                Row(
                    modifier = modifier
                        .weight(1f)
                        .selectable(
                            selected = proxyType == type,
                            onClick = { proxyType = type }
                        )
                        .padding(horizontal = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = proxyType == type,
                        onClick = { proxyType = type }
                    )
                    Text(
                        text = type.name,
                        modifier = modifier.padding(start = 8.dp)
                    )
                }
            }
        }
        if (hasError) {
            Text(
                text = errorMessage,
                color = MaterialTheme.colorScheme.error,
                modifier = modifier.padding(vertical = 8.dp)
            )
        }
        Row(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    if (validateInput(host, port)) {
                        val config = ProxyConfiguration(
                            host = host,
                            port = port.toInt(),
                            username = username.takeIf { it.isNotEmpty() },
                            password = password.takeIf { it.isNotEmpty() },
                            proxyType = proxyType
                        )
                        viewModel.connectToProxy(config)
                        onConfigurationComplete()
                    } else {
                        hasError = true
                        errorMessage = "Please fill in all required fields correctly"
                    }
                },
                modifier = modifier.weight(1f)
            ) {
                Text("Connect")
            }
            if (connectionState is ConnectionState.Connected) {
                Button(
                    onClick = { viewModel.disconnectProxy() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Disconnect")
                }
            }
        }
    }
}

private fun ConnectionState.toDisplayString(): String = when (this) {
    is ConnectionState.Connected -> "Connected"
    is ConnectionState.Connecting -> "Connecting..."
    is ConnectionState.Disconnected -> "Disconnected"
    is ConnectionState.Disconnecting -> "Disconnecting..."
    is ConnectionState.Rotating -> "Rotating IP..."
    is ConnectionState.Error -> "Error: ${this.message}"
}

private fun validateInput(host: String, port: String): Boolean {
    if (host.isEmpty()) return false
    if (port.isEmpty() || port.toIntOrNull() == null) return false
    val portNumber = port.toInt()
    return portNumber in 1..65535
}