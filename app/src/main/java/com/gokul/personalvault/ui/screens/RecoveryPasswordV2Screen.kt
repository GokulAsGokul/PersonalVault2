package com.gokul.personalvault.ui.screens

import com.gokul.personalvault.ui.components.VaultSnackbarOverlay
import com.gokul.personalvault.ui.components.VaultSnackbarController
import com.gokul.personalvault.ui.components.VaultSnackbarType
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
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

@Composable
fun RecoveryPasswordV2Screen(
    hasUnlockedVault: Boolean,
    onCreateBackup: (recoveryPassword: CharArray, onComplete: (Result<String>) -> Unit) -> Unit,
    onRestoreBackup: (
        recoveryPassword: CharArray,
        newMasterPassword: CharArray,
        onComplete: (Result<Unit>) -> Unit
    ) -> Unit,
    onBack: (() -> Unit)? = null
) {
    var selectedMode by remember { mutableStateOf("create") }
    var recoveryPassword by remember { mutableStateOf("") }
    var confirmRecoveryPassword by remember { mutableStateOf("") }
    var newMasterPassword by remember { mutableStateOf("") }
    var confirmMasterPassword by remember { mutableStateOf("") }
    var showPasswords by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }
    var fieldError by remember { mutableStateOf<String?>(null) }

    val recoveryValid = isStrongPassword(recoveryPassword)
    val recoveryMatches = recoveryPassword == confirmRecoveryPassword
    val masterValid = isStrongPassword(newMasterPassword)
    val masterMatches = newMasterPassword == confirmMasterPassword

    fun clearRecoveryFields() {
        recoveryPassword = ""
        confirmRecoveryPassword = ""
    }
    fun clearRestoreFields() {
        recoveryPassword = ""
        newMasterPassword = ""
        confirmMasterPassword = ""
    }
    fun showFieldError(value: String) {
        isError = true
        fieldError = value
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text("Recovery protection", style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Create an encrypted online backup of your vault key, or restore it on a device where the local vault key is not configured.",
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(Modifier.height(20.dp))

            Card(
                Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("Choose an action", style = MaterialTheme.typography.titleMedium)
                    TextButton(
                        onClick = {
                            selectedMode = "create"
                            clearRecoveryFields()
                            clearRestoreFields()
                            message = ""
                            isError = false
                            fieldError = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedMode == "create") "✓  Create / update recovery backup" else "Create / update recovery backup")
                    }
                    TextButton(
                        onClick = {
                            selectedMode = "restore"
                            clearRecoveryFields()
                            clearRestoreFields()
                            message = ""
                            isError = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (selectedMode == "restore") "✓  Restore from online backup" else "Restore from online backup")
                    }

                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = recoveryPassword,
                        onValueChange = { recoveryPassword = it; message = ""; fieldError = null },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Recovery password") },
                        singleLine = true,
                        visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = fieldError?.startsWith("Recovery password must") == true,
                        supportingText = {
                            if (fieldError?.startsWith("Recovery password must") == true) {
                                Text(fieldError!!, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    if (selectedMode == "create") {
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = confirmRecoveryPassword,
                            onValueChange = { confirmRecoveryPassword = it; message = ""; fieldError = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm recovery password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = fieldError == "Recovery passwords do not match.",
                            supportingText = {
                                if (fieldError == "Recovery passwords do not match.") {
                                    Text(fieldError!!, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        PasswordRequirements()
                    } else {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            "Set a new master password for this device. The recovered vault key will be encrypted locally with it.",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = newMasterPassword,
                            onValueChange = { newMasterPassword = it; message = ""; fieldError = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("New master password") },
                            singleLine = true,
                            visualTransformation = if (showPasswords) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = fieldError?.startsWith("New master password must") == true,
                            supportingText = {
                                if (fieldError?.startsWith("New master password must") == true) {
                                    Text(fieldError!!, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        Spacer(Modifier.height(10.dp))
                        OutlinedTextField(
                            value = confirmMasterPassword,
                            onValueChange = { confirmMasterPassword = it; message = ""; fieldError = null },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Confirm new master password") },
                            singleLine = true,
                            visualTransformation = PasswordVisualTransformation(),
                            isError = fieldError == "New master passwords do not match.",
                            supportingText = {
                                if (fieldError == "New master passwords do not match.") {
                                    Text(fieldError!!, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        )
                        PasswordRequirements()
                    }

                    TextButton(onClick = { showPasswords = !showPasswords }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (showPasswords) "Hide passwords" else "Show passwords")
                    }
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            message = ""
                            isError = false
                            fieldError = null
                            when {
                                !recoveryValid -> showFieldError("Recovery password must meet all requirements.")
                                selectedMode == "create" && !recoveryMatches -> showFieldError("Recovery passwords do not match.")
                                selectedMode == "create" && !hasUnlockedVault -> {
                                    VaultSnackbarController.show(
                                        "Unlock your vault before creating a recovery backup.",
                                        VaultSnackbarType.INFO
                                    )
                                }
                                selectedMode == "restore" && !masterValid -> showFieldError("New master password must meet all requirements.")
                                selectedMode == "restore" && !masterMatches -> showFieldError("New master passwords do not match.")
                                else -> {
                                    isLoading = true
                                    if (selectedMode == "create") {
                                        onCreateBackup(recoveryPassword.toCharArray()) { result ->
                                            isLoading = false
                                            result.fold(
                                                onSuccess = {
                                                    message = "Recovery backup created and verified."
                                                    isError = false
                                                    clearRecoveryFields()
                                                },
                                                onFailure = {
                                                    showError(it.localizedMessage ?: "Could not create recovery backup.")
                                                    clearRecoveryFields()
                                                }
                                            )
                                        }
                                    } else {
                                        onRestoreBackup(
                                            recoveryPassword.toCharArray(),
                                            newMasterPassword.toCharArray()
                                        ) { result ->
                                            isLoading = false
                                            result.fold(
                                                onSuccess = {
                                                    message = "Vault key restored, encrypted locally and unlocked."
                                                    isError = false
                                                    clearRestoreFields()
                                                },
                                                onFailure = {
                                                    showError(it.localizedMessage ?: "Could not restore recovery backup.")
                                                    clearRestoreFields()
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        enabled = !isLoading,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (isLoading) CircularProgressIndicator()
                        else Text(if (selectedMode == "create") "Create secure backup" else "Restore and unlock vault")
                    }

                    VaultSnackbarOverlay(message = message, onDismiss = { message = "" })
                }
            }

            if (onBack != null) {
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back") }
            }
        }
    }
}

private fun isStrongPassword(value: String): Boolean =
    value.length >= 12 &&
        value.any(Char::isUpperCase) &&
        value.any(Char::isLowerCase) &&
        value.any(Char::isDigit) &&
        value.any { !it.isLetterOrDigit() }

@Composable
private fun PasswordRequirements() {
    Spacer(Modifier.height(10.dp))
    Text("Password requirements", style = MaterialTheme.typography.titleSmall)
    Text("• At least 12 characters")
    Text("• Uppercase and lowercase letters")
    Text("• At least one number")
    Text("• At least one special character")
}
