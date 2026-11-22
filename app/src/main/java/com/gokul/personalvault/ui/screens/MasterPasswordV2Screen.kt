package com.gokul.personalvault.ui.screens

import com.gokul.personalvault.ui.components.VaultSnackbarHost
import com.gokul.personalvault.ui.components.classifyVaultSnackbar
import com.gokul.personalvault.ui.components.triggerVaultErrorVibration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.gokul.personalvault.data.local.VaultKeyLocalStoreV2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun MasterPasswordV2Screen(
    vaultId: String,
    memberUid: String,
    onUnlocked: (ByteArray) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val isSetup = remember(vaultId, memberUid) {
        !VaultKeyLocalStoreV2.hasMasterPassword(context, memberUid, vaultId)
    }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(message) {
        if (message.isNotBlank()) {
            if (classifyVaultSnackbar(message) == com.gokul.personalvault.ui.components.VaultSnackbarType.ERROR) {
                triggerVaultErrorVibration(context)
            }
            snackbarHostState.showSnackbar(message)
            message = ""
        }
    }

    val strongEnough = password.length >= 12 && password.any(Char::isUpperCase) &&
            password.any(Char::isLowerCase) && password.any(Char::isDigit) &&
            password.any { !it.isLetterOrDigit() }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            snackbarHost = {
                VaultSnackbarHost(snackbarHostState, classifyVaultSnackbar(message))
            }
        ) { contentPadding ->
        Column(
            Modifier.fillMaxSize().padding(contentPadding).verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(if (isSetup) "Create master password" else "Unlock your vault",
                style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (isSetup) "Create a separate master password to protect this device's copy of your V2 vault key. It is different from your Firebase login and recovery passwords."
                else "Enter your master password to unlock your encrypted V2 vault key on this device.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))
            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Column(Modifier.padding(18.dp)) {
                    OutlinedTextField(
                        value = password, onValueChange = { password = it; message = ""; fieldError = null },
                        modifier = Modifier.fillMaxWidth(), singleLine = true,
                        label = { Text("Master password") },
                        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = fieldError != null,
                        supportingText = { fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) } }
                    )
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Text(if (showPassword) "Hide password" else "Show password")
                    }
                    if (isSetup) {
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = confirm, onValueChange = { confirm = it; message = ""; fieldError = null },
                            modifier = Modifier.fillMaxWidth(), singleLine = true,
                            label = { Text("Confirm master password") },
                            visualTransformation = PasswordVisualTransformation()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text("Use at least 12 characters, including uppercase, lowercase, a number and a special character.",
                            style = MaterialTheme.typography.bodySmall)
                    }
                    Spacer(Modifier.height(18.dp))
                    Button(
                        modifier = Modifier.fillMaxWidth(), enabled = !loading,
                        onClick = {
                            if (password.isBlank()) {
                                error = true; fieldError = "Enter your master password."
                            } else if (isSetup && !strongEnough) {
                                error = true; fieldError = "Master password does not meet the requirements."
                            } else if (isSetup && password != confirm) {
                                error = true; fieldError = "The passwords do not match."
                            } else {
                                loading = true; error = false; fieldError = null; message = ""
                                val submitted = password.toCharArray()
                                password = ""; confirm = ""
                                scope.launch {
                                    val result = withContext(Dispatchers.IO) {
                                        if (isSetup) VaultKeyLocalStoreV2.create(context, memberUid, vaultId, submitted)
                                        else VaultKeyLocalStoreV2.unlock(context, memberUid, vaultId, submitted)
                                    }
                                    loading = false
                                    result.fold(
                                        onSuccess = { key -> onUnlocked(key) },
                                        onFailure = { e ->
                                            error = true
                                            message = "Unable to unlock the vault. ${e.localizedMessage ?: "Check your master password."}"
                                        }
                                    )
                                }
                            }
                        }
                    ) {
                        if (loading) CircularProgressIndicator() else Text(if (isSetup) "Create and unlock" else "Unlock vault")
                    }
                    TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth(), enabled = !loading) { Text("Back to dashboard") }
                }
            }
        }
        }
    }
}
