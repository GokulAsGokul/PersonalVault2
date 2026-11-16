package com.gokul.personalvault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun VaultUnlockGate(
    userUid: String,
    onUnlocked: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var isUnlocked by remember(userUid) { mutableStateOf(false) }
    var masterPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }
    var isBusy by remember { mutableStateOf(false) }
    var setupComplete by remember(userUid) {
        mutableStateOf(VaultCrypto.isSetupComplete(context, userUid))
    }

    if (isUnlocked) {
        LaunchedEffect(userUid) { onUnlocked() }
        return
    }

    val navy = Color(0xFF14213D)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF4F6FB))
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            "PERSONAL VAULT",
            color = navy,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(10.dp))
        Text(
            if (setupComplete) "Unlock your vault" else "Create Master Password",
            style = MaterialTheme.typography.headlineMedium,
            color = navy,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(8.dp))
        Text(
            if (setupComplete)
                "Enter your Master Password to decrypt your personal vault key."
            else
                "This password protects your personal vault encryption key. Use at least 10 characters.",
            color = Color(0xFF64748B),
            style = MaterialTheme.typography.bodyMedium
        )

        Spacer(Modifier.height(24.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(Modifier.padding(22.dp)) {
                OutlinedTextField(
                    value = masterPassword,
                    onValueChange = { masterPassword = it; error = "" },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text(if (setupComplete) "Master Password" else "Create Master Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    shape = RoundedCornerShape(14.dp)
                )

                if (!setupComplete) {
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it; error = "" },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Confirm Master Password") },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(14.dp)
                    )
                }

                if (error.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(error, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(18.dp))
                Button(
                    onClick = {
                        when {
                            masterPassword.length < 10 -> error = "Use at least 10 characters."
                            !setupComplete && masterPassword != confirmPassword ->
                                error = "The passwords do not match."
                            else -> {
                                isBusy = true
                                error = ""
                                val passwordChars = masterPassword.toCharArray()
                                masterPassword = ""
                                confirmPassword = ""
                                scope.launch {
                                    try {
                                        withContext(Dispatchers.IO) {
                                            if (setupComplete) {
                                                VaultCrypto.unlockVault(context, userUid, passwordChars)
                                            } else {
                                                VaultCrypto.createVault(context, userUid, passwordChars)
                                            }
                                        }
                                        setupComplete = true
                                        isUnlocked = true
                                    } catch (e: Exception) {
                                        error = e.message ?: "Unable to unlock the vault."
                                    } finally {
                                        passwordChars.fill('\u0000')
                                        isBusy = false
                                    }
                                }
                            }
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        when {
                            isBusy -> "Please wait…"
                            setupComplete -> "Unlock Vault"
                            else -> "Create Master Password"
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(16.dp))
        Text(
            "Your vault key is encrypted locally. Your Master Password is not stored.",
            color = Color(0xFF64748B),
            style = MaterialTheme.typography.bodySmall
        )
    }
}
