package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.AccountEntity
import com.example.ui.theme.OwnCloudBlue
import com.example.ui.theme.StatusErrorRed
import com.example.ui.theme.StatusSuccessGreen
import com.example.ui.viewmodel.TestConnectionState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountScreen(
    account: AccountEntity?,
    testState: TestConnectionState,
    onTestConnection: (String, String, String) -> Unit,
    onResetTestState: () -> Unit,
    onSaveConfig: (
        serverUrl: String,
        username: String,
        password: String,
        syncIntervalMinutes: Long,
        autoSyncEnabled: Boolean,
        syncWifiOnly: Boolean,
        syncChargingOnly: Boolean
    ) -> Unit
) {
    var serverUrl by remember { mutableStateOf(account?.serverUrl ?: "") }
    var username by remember { mutableStateOf(account?.username ?: "") }
    var password by remember { mutableStateOf("") }
    var isPasswordVisible by remember { mutableStateOf(false) }

    var syncIntervalMinutes by remember { mutableLongStateOf(account?.syncIntervalMinutes ?: 60) }
    var autoSyncEnabled by remember { mutableStateOf(account?.autoSyncEnabled ?: true) }
    var syncWifiOnly by remember { mutableStateOf(account?.syncWifiOnly ?: false) }
    var syncChargingOnly by remember { mutableStateOf(account?.syncChargingOnly ?: false) }

    var isSavedMessageVisible by remember { mutableStateOf(false) }

    LaunchedEffect(account) {
        if (account != null) {
            serverUrl = account.serverUrl
            username = account.username
            syncIntervalMinutes = account.syncIntervalMinutes
            autoSyncEnabled = account.autoSyncEnabled
            syncWifiOnly = account.syncWifiOnly
            syncChargingOnly = account.syncChargingOnly
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Account Configuration",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // Account Input Credentials Form Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "ownCloud Credentials",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Server URL
                OutlinedTextField(
                    value = serverUrl,
                    onValueChange = {
                        serverUrl = it
                        onResetTestState()
                    },
                    label = { Text("Server URL") },
                    placeholder = { Text("https://cloud.example.com") },
                    leadingIcon = {
                        Icon(Icons.Default.Cloud, contentDescription = "Server", tint = OwnCloudBlue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("server_url_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Next
                    )
                )

                // Username
                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        onResetTestState()
                    },
                    label = { Text("Username") },
                    placeholder = { Text("user123") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = "Username", tint = OwnCloudBlue)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("username_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Text,
                        imeAction = ImeAction.Next
                    )
                )

                // Password
                OutlinedTextField(
                    value = password,
                    onValueChange = {
                        password = it
                        onResetTestState()
                    },
                    label = { Text(if (account != null && password.isEmpty()) "Password (unchanged)" else "Password") },
                    placeholder = { Text("Password or App Password") },
                    leadingIcon = {
                        Icon(Icons.Default.Lock, contentDescription = "Password", tint = OwnCloudBlue)
                    },
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = "Toggle Password Visibility"
                            )
                        }
                    },
                    visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("password_input"),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Password,
                        imeAction = ImeAction.Done
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onTestConnection(serverUrl, username, password) },
                        enabled = serverUrl.isNotBlank() && username.isNotBlank() && testState !is TestConnectionState.Loading,
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("test_connection_button"),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (testState is TestConnectionState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Test Connection")
                        }
                    }

                    Button(
                        onClick = {
                            onSaveConfig(
                                serverUrl,
                                username,
                                password,
                                syncIntervalMinutes,
                                autoSyncEnabled,
                                syncWifiOnly,
                                syncChargingOnly
                            )
                            isSavedMessageVisible = true
                        },
                        enabled = serverUrl.isNotBlank() && username.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("save_config_button"),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = OwnCloudBlue)
                    ) {
                        Text("Save Account")
                    }
                }

                if (isSavedMessageVisible) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = StatusSuccessGreen.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = "Saved", tint = StatusSuccessGreen)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Account & KeyStore security settings saved!",
                                color = StatusSuccessGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        // Test Connection Trace Output Card
        if (testState !is TestConnectionState.Idle) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(
                    containerColor = when (testState) {
                        is TestConnectionState.Success -> StatusSuccessGreen.copy(alpha = 0.1f)
                        is TestConnectionState.Error -> StatusErrorRed.copy(alpha = 0.1f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = when (testState) {
                                is TestConnectionState.Success -> Icons.Default.CheckCircle
                                is TestConnectionState.Error -> Icons.Default.Error
                                else -> Icons.Default.Info
                            },
                            contentDescription = "Test Result",
                            tint = when (testState) {
                                is TestConnectionState.Success -> StatusSuccessGreen
                                is TestConnectionState.Error -> StatusErrorRed
                                else -> OwnCloudBlue
                            }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when (testState) {
                                is TestConnectionState.Success -> "Connection Test Succeeded!"
                                is TestConnectionState.Error -> "Connection Test Failed"
                                is TestConnectionState.Loading -> "Testing Handshake & Auth..."
                                else -> ""
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    val traceText = when (testState) {
                        is TestConnectionState.Success -> testState.trace
                        is TestConnectionState.Error -> testState.trace
                        else -> ""
                    }

                    if (traceText.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text(
                                text = traceText,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 11.sp,
                                modifier = Modifier.padding(10.dp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }

        // Scheduled Sync & Battery Strategy Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text(
                    text = "Scheduled Sync & Battery Strategy",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Auto Sync Enable Switch
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable Automatic Background Sync", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                        Text(
                            "Periodically runs in background via WorkManager",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                        )
                    }
                    Switch(
                        checked = autoSyncEnabled,
                        onCheckedChange = { autoSyncEnabled = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = OwnCloudBlue)
                    )
                }

                if (autoSyncEnabled) {
                    // Sync Interval Dropdown
                    var expandedDropdown by remember { mutableStateOf(false) }
                    val intervalOptions = listOf(
                        15L to "Every 15 Minutes",
                        30L to "Every 30 Minutes",
                        60L to "Every 1 Hour",
                        360L to "Every 6 Hours",
                        720L to "Every 12 Hours",
                        1440L to "Every 24 Hours (Daily)"
                    )
                    val selectedText = intervalOptions.find { it.first == syncIntervalMinutes }?.second ?: "Every 1 Hour"

                    ExposedDropdownMenuBox(
                        expanded = expandedDropdown,
                        onExpandedChange = { expandedDropdown = !expandedDropdown }
                    ) {
                        OutlinedTextField(
                            value = selectedText,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Sync Interval") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedDropdown) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedDropdown,
                            onDismissRequest = { expandedDropdown = false }
                        ) {
                            intervalOptions.forEach { (mins, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        syncIntervalMinutes = mins
                                        expandedDropdown = false
                                    }
                                )
                            }
                        }
                    }

                    // Wi-Fi Only Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Only on Wi-Fi Network", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Prevents using cellular data", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                        }
                        Switch(
                            checked = syncWifiOnly,
                            onCheckedChange = { syncWifiOnly = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OwnCloudBlue)
                        )
                    }

                    // Charging Only Switch
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Only When Charging", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                            Text("Eliminates battery consumption during battery operation", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f))
                        }
                        Switch(
                            checked = syncChargingOnly,
                            onCheckedChange = { syncChargingOnly = it },
                            colors = SwitchDefaults.colors(checkedThumbColor = OwnCloudBlue)
                        )
                    }
                }
            }
        }

        // Security Vault Info Banner
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "KeyStore Security",
                    tint = OwnCloudBlue,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Android KeyStore Vault",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Your password is encrypted using AES-256 GCM backed by hardware Security Module.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}
