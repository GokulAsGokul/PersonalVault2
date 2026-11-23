package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.gokul.personalvault.data.local.VaultKeyLocalStoreV2
import com.gokul.personalvault.ui.components.VaultSnackbarHost
import com.gokul.personalvault.ui.components.classifyVaultSnackbar
import com.gokul.personalvault.ui.components.triggerVaultErrorVibration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val UnlockNavy = Color(0xFF111B4B)
private val UnlockPurple = Color(0xFF7652D4)
private val UnlockBlue = Color(0xFF354BC4)
private val UnlockMuted = Color(0xFF626A7D)

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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFFF4F2FF), Color(0xFFFAFBFF), Color(0xFFF0F2FF))
                )
            )
    ) {
        UnlockBottomWaves(Modifier.align(Alignment.BottomCenter))
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            snackbarHost = {
                VaultSnackbarHost(snackbarHostState, classifyVaultSnackbar(message))
            }
        ) { contentPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(listOf(Color(0xFF35C6B0), Color(0xFF159A9A))),
                                RoundedCornerShape(13.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color.White, modifier = Modifier.size(29.dp))
                    }
                    Spacer(Modifier.size(10.dp))
                    Text("Personal Vault", color = UnlockNavy, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                }
                Spacer(Modifier.height(5.dp))
                Text(
                    "Your private space for documents and passwords",
                    color = UnlockMuted,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(Modifier.height(12.dp))
                VaultSafeIllustration(Modifier.fillMaxWidth().height(238.dp))
                Spacer(Modifier.height(8.dp))

                Text(
                    if (isSetup) "Set up your vault" else "Unlock your vault",
                    color = UnlockNavy,
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (isSetup) "Create a Master Password to securely protect your documents and passwords."
                    else "Enter your Master Password to securely access your documents and passwords.",
                    color = UnlockMuted,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )

                Spacer(Modifier.height(22.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(19.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xF9FFFFFF)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it; message = ""; fieldError = null },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text(if (isSetup) "Create Master Password" else "Enter Master Password") },
                            leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = UnlockMuted) },
                            trailingIcon = {
                                Icon(
                                    if (showPassword) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                                    contentDescription = if (showPassword) "Hide password" else "Show password",
                                    tint = UnlockMuted,
                                    modifier = Modifier.clickable { showPassword = !showPassword }
                                )
                            },
                            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                            isError = fieldError != null,
                            supportingText = { fieldError?.let { Text(it, color = MaterialTheme.colorScheme.error) } },
                            shape = RoundedCornerShape(15.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = UnlockPurple,
                                unfocusedBorderColor = Color(0xFFD6D9E8),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color.White
                            )
                        )

                        if (isSetup) {
                            Spacer(Modifier.height(10.dp))
                            OutlinedTextField(
                                value = confirm,
                                onValueChange = { confirm = it; fieldError = null },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Confirm Master Password") },
                                leadingIcon = { Icon(Icons.Outlined.Lock, contentDescription = null, tint = UnlockMuted) },
                                visualTransformation = PasswordVisualTransformation(),
                                shape = RoundedCornerShape(15.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = UnlockPurple,
                                    unfocusedBorderColor = Color(0xFFD6D9E8),
                                    focusedContainerColor = Color.White,
                                    unfocusedContainerColor = Color.White
                                )
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Use at least 12 characters, including uppercase, lowercase, a number and a special character.",
                                color = UnlockMuted,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Spacer(Modifier.height(18.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(58.dp)
                                .shadow(5.dp, RoundedCornerShape(16.dp))
                                .background(
                                    Brush.horizontalGradient(listOf(UnlockPurple, UnlockBlue)),
                                    RoundedCornerShape(16.dp)
                                )
                                .clickable(enabled = !loading) {
                                    when {
                                        password.isBlank() -> fieldError = "Enter your Master Password."
                                        isSetup && !strongEnough -> fieldError = "Master Password does not meet the requirements."
                                        isSetup && password != confirm -> fieldError = "The passwords do not match."
                                        else -> {
                                            loading = true
                                            fieldError = null
                                            val submitted = password.toCharArray()
                                            password = ""
                                            confirm = ""
                                            scope.launch {
                                                val result = withContext(Dispatchers.IO) {
                                                    if (isSetup) VaultKeyLocalStoreV2.create(context, memberUid, vaultId, submitted)
                                                    else VaultKeyLocalStoreV2.unlock(context, memberUid, vaultId, submitted)
                                                }
                                                loading = false
                                                result.fold(
                                                    onSuccess = { key -> onUnlocked(key) },
                                                    onFailure = { e ->
                                                        message = "Unable to unlock the vault. ${e.localizedMessage ?: "Check your Master Password."}"
                                                    }
                                                )
                                            }
                                        }
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (loading) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                            } else {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                                    Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(23.dp))
                                    Spacer(Modifier.size(12.dp))
                                    Text(
                                        if (isSetup) "Create & Unlock Vault" else "Unlock Vault",
                                        color = Color.White,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                    Icon(Icons.Outlined.Shield, contentDescription = null, tint = Color(0xFF1B9F92), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.size(8.dp))
                    Text(
                        "Your documents & passwords stay encrypted.",
                        color = UnlockMuted,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun VaultSafeIllustration(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(210.dp)
                .background(Color(0x22A6A5FF), CircleShape)
        )
        Box(
            Modifier
                .size(width = 178.dp, height = 150.dp)
                .shadow(16.dp, RoundedCornerShape(23.dp))
                .background(
                    Brush.linearGradient(listOf(Color(0xFF59C6FF), Color(0xFF1766C7), Color(0xFF253D9C))),
                    RoundedCornerShape(23.dp)
                )
                .padding(9.dp)
                .background(
                    Brush.linearGradient(listOf(Color(0xFF286DBA), Color(0xFF123C83))),
                    RoundedCornerShape(17.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .size(76.dp)
                    .background(
                        Brush.radialGradient(listOf(Color(0xFF63F0F0), Color(0xFF1496C5))),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Outlined.Lock, contentDescription = null, tint = Color.White, modifier = Modifier.size(42.dp))
            }
            Column(
                Modifier.align(Alignment.CenterEnd).padding(end = 5.dp),
                verticalArrangement = Arrangement.spacedBy(30.dp)
            ) {
                Box(Modifier.size(width = 5.dp, height = 24.dp).background(Color(0xFFB7E8FF), RoundedCornerShape(3.dp)))
                Box(Modifier.size(width = 5.dp, height = 24.dp).background(Color(0xFFB7E8FF), RoundedCornerShape(3.dp)))
            }
        }
        FloatingVaultTile(
            modifier = Modifier.align(Alignment.CenterStart).padding(start = 10.dp, top = 16.dp).rotate(-10f),
            color = Color(0xFFFFF1D4),
            icon = { Icon(Icons.Outlined.Folder, contentDescription = null, tint = Color(0xFFFFB52E), modifier = Modifier.size(34.dp)) }
        )
        FloatingVaultTile(
            modifier = Modifier.align(Alignment.CenterEnd).padding(end = 9.dp, top = 48.dp).rotate(11f),
            color = Color(0xFFF0E9FF),
            icon = { Icon(Icons.Outlined.Key, contentDescription = null, tint = Color(0xFF8060E8), modifier = Modifier.size(34.dp)) }
        )
        Box(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 13.dp).size(width = 220.dp, height = 15.dp)
                .background(Brush.horizontalGradient(listOf(Color(0x004E8FFF), Color(0x554E8FFF), Color(0x004E8FFF))), CircleShape)
        )
    }
}

@Composable
private fun FloatingVaultTile(
    modifier: Modifier = Modifier,
    color: Color,
    icon: @Composable () -> Unit
) {
    Box(
        modifier
            .size(68.dp)
            .shadow(9.dp, RoundedCornerShape(18.dp))
            .background(Color.White, RoundedCornerShape(18.dp))
            .padding(6.dp)
            .background(color, RoundedCornerShape(14.dp)),
        contentAlignment = Alignment.Center
    ) { icon() }
}

@Composable
private fun UnlockBottomWaves(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(78.dp)) {
        val first = Path().apply {
            moveTo(0f, size.height * 0.38f)
            cubicTo(size.width * 0.22f, size.height * 0.02f, size.width * 0.34f, size.height * 0.95f, size.width * 0.58f, size.height * 0.52f)
            cubicTo(size.width * 0.78f, size.height * 0.15f, size.width * 0.88f, size.height * 0.05f, size.width, size.height * 0.30f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(first, Brush.horizontalGradient(listOf(Color(0x667D9EFF), Color(0x667C68EF))), style = Fill)
        val second = Path().apply {
            moveTo(0f, size.height * 0.70f)
            cubicTo(size.width * 0.25f, size.height * 0.22f, size.width * 0.42f, size.height * 1.05f, size.width * 0.67f, size.height * 0.60f)
            cubicTo(size.width * 0.82f, size.height * 0.34f, size.width * 0.93f, size.height * 0.50f, size.width, size.height * 0.42f)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(second, Brush.horizontalGradient(listOf(Color(0x447CA5FF), Color(0x447F69F2))), style = Fill)
    }
}
