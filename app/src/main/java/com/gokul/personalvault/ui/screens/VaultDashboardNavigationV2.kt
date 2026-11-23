package com.gokul.personalvault.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import com.gokul.personalvault.data.firebase.RecoveryBackupRepositoryV2
import com.gokul.personalvault.data.local.VaultKeyLocalStoreV2
import com.gokul.personalvault.data.local.VaultKeySessionV2
import com.google.firebase.auth.FirebaseAuth
import com.gokul.personalvault.ui.components.VaultDashboardHeader
import com.gokul.personalvault.ui.components.VaultCompactHeader
import com.gokul.personalvault.ui.components.VaultSnackbarController
import com.gokul.personalvault.ui.components.VaultSnackbarHost
import com.gokul.personalvault.ui.components.VaultSnackbarType
import com.gokul.personalvault.ui.components.triggerVaultErrorVibration

enum class VaultV2Destination { HOME, PASSWORDS, DOCUMENTS, PERSONAL_INFO, HOUSEHOLD, SETTINGS }

@Composable
fun VaultDashboardNavigationV2(
    email: String,
    vaultId: String,
    memberUid: String,
    isVaultUnlocked: Boolean,
    onRequestVaultUnlock: (VaultV2Destination, () -> Unit) -> Unit,
    onSignOut: () -> Unit
) {
    var selected by rememberSaveable { mutableStateOf(VaultV2Destination.HOME) }
    val protected = selected == VaultV2Destination.PASSWORDS ||
            selected == VaultV2Destination.DOCUMENTS ||
            selected == VaultV2Destination.PERSONAL_INFO
    var vaultUnlocked by remember { mutableStateOf(isVaultUnlocked) }
    LaunchedEffect(isVaultUnlocked) { vaultUnlocked = isVaultUnlocked }
    LaunchedEffect(selected, vaultUnlocked) {
        if ((selected == VaultV2Destination.PASSWORDS ||
                selected == VaultV2Destination.DOCUMENTS ||
                selected == VaultV2Destination.PERSONAL_INFO) && !vaultUnlocked) {
            onRequestVaultUnlock(selected) { selected = VaultV2Destination.HOME }
        }
    }
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val activeNotice = VaultSnackbarController.notice

    LaunchedEffect(activeNotice?.id) {
        val notice = activeNotice ?: return@LaunchedEffect
        if (notice.type == VaultSnackbarType.ERROR) {
            triggerVaultErrorVibration(context)
        }
        snackbarHostState.showSnackbar(notice.message)
        VaultSnackbarController.clear(notice.id)
    }

    Scaffold(
        snackbarHost = {
            VaultSnackbarHost(
                hostState = snackbarHostState,
                type = activeNotice?.type ?: VaultSnackbarType.INFO
            )
        },
        containerColor = Color(0xFFF4F6FB),
        bottomBar = {
            NavigationBar(containerColor = Color.White) {
                NavigationBarItem(
                    selected == VaultV2Destination.HOME,
                    { selected = VaultV2Destination.HOME },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected == VaultV2Destination.PASSWORDS,
                    { selected = VaultV2Destination.PASSWORDS },
                    icon = { Icon(Icons.Outlined.Key, contentDescription = "Passwords") },
                    label = { Text("Passwords") }
                )
                NavigationBarItem(
                    selected == VaultV2Destination.DOCUMENTS,
                    { selected = VaultV2Destination.DOCUMENTS },
                    icon = { Icon(Icons.Outlined.Description, contentDescription = "Documents") },
                    label = { Text("Documents") }
                )
                NavigationBarItem(
                    selected == VaultV2Destination.SETTINGS,
                    { selected = VaultV2Destination.SETTINGS },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                selected == VaultV2Destination.HOME -> DashboardOverviewV2(
                    email = email,
                    onOpen = { selected = it },
                    onSignOut = onSignOut
                )
                selected == VaultV2Destination.HOUSEHOLD -> Column(Modifier.fillMaxSize()) {
                    VaultCompactHeader(
                        title = "Household",
                        onBack = { selected = VaultV2Destination.HOME }
                    )
                    Box(Modifier.weight(1f)) {
                        HouseholdInvitationV2Screen(
                            onBack = { selected = VaultV2Destination.HOME },
                            vaultId = vaultId,
                            memberUid = memberUid
                        )
                    }
                }
                selected == VaultV2Destination.SETTINGS -> Column(Modifier.fillMaxSize()) {
                    VaultCompactHeader(
                        title = "Settings",
                        onBack = { selected = VaultV2Destination.HOME }
                    )
                    Box(Modifier.weight(1f)) {
                        RecoveryPasswordV2Screen(
                            hasUnlockedVault = vaultUnlocked,
                    onCreateBackup = { recoveryPassword, complete ->
                        val key = VaultKeySessionV2.copyKey(vaultId, memberUid)
                        if (key == null) {
                            recoveryPassword.fill('\u0000')
                            complete(Result.failure(IllegalStateException("Unlock your vault before creating a recovery backup.")))
                        } else {
                            RecoveryBackupRepositoryV2.createAndActivateBackup(
                                vaultId = vaultId,
                                vaultKey = key,
                                recoveryPassword = recoveryPassword
                            ) { result ->
                                key.fill(0)
                                complete(result.map { it.backupId })
                            }
                        }
                    },
                    onRestoreBackup = { recoveryPassword, newMasterPassword, complete ->
                        RecoveryBackupRepositoryV2.restoreActiveBackup(
                            vaultId = vaultId,
                            recoveryPassword = recoveryPassword
                        ) { result ->
                            result.fold(
                                onSuccess = { recoveredKey ->
                                    val imported = VaultKeyLocalStoreV2.importExistingKey(
                                        context = context,
                                        uid = memberUid,
                                        vaultId = vaultId,
                                        existingVaultKey = recoveredKey,
                                        newMasterPassword = newMasterPassword
                                    )
                                    imported.fold(
                                        onSuccess = {
                                            VaultKeySessionV2.install(vaultId, memberUid, recoveredKey)
                                            recoveredKey.fill(0)
                                            vaultUnlocked = true
                                            complete(Result.success(Unit))
                                        },
                                        onFailure = { error ->
                                            recoveredKey.fill(0)
                                            complete(Result.failure(error))
                                        }
                                    )
                                },
                                onFailure = { error ->
                                    newMasterPassword.fill('\u0000')
                                    complete(Result.failure(error))
                                }
                            )
                        }
                    },
                            onBack = { selected = VaultV2Destination.HOME }
                        )
                    }
                }
                protected && !vaultUnlocked -> Box(Modifier.fillMaxSize())
                selected == VaultV2Destination.PASSWORDS -> PasswordManagerV2Screen(
                    vaultId = vaultId,
                    memberUid = memberUid
                )
                selected == VaultV2Destination.DOCUMENTS -> DocumentsManagerV2ScreenNew(
                    vaultId = vaultId,
                    memberUid = memberUid
                )
                else -> V2ModulePlaceholder(
                    title = when (selected) {
                        VaultV2Destination.DOCUMENTS -> "Documents"
                        else -> "Personal Information"
                    },
                    description = "This V2 section will be connected in a later implementation stage.",
                    icon = when (selected) {
                        VaultV2Destination.DOCUMENTS -> Icons.Outlined.Description
                        else -> Icons.Outlined.Person
                    }
                ) { selected = VaultV2Destination.HOME }
            }
        }
    }
}

@Composable
private fun DashboardOverviewV2(
    email: String,
    onOpen: (VaultV2Destination) -> Unit,
    onSignOut: () -> Unit
) {
    Column(Modifier.fillMaxSize().background(Color(0xFFF4F6FB))) {
        VaultDashboardHeader(onSignOut = onSignOut)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            Text("Welcome back!", color = Color(0xFF14213D),
                style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(email, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(22.dp))
        Card(
            Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF14213D))
        ) {
            Column(Modifier.padding(20.dp)) {
                Text("YOUR PRIVATE SPACE", color = Color(0xFFB9C9E2),
                    style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Text("Everything important, securely organised.", color = Color.White,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                Text("Choose a section below to get started.", color = Color(0xFFCBD5E1),
                    style = MaterialTheme.typography.bodySmall)
            }
        }
        Spacer(Modifier.height(22.dp))
        Text("YOUR VAULT", color = Color(0xFF64748B),
            style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { V2DashboardTile("Passwords", "Personal and shared", Icons.Outlined.Key) { onOpen(VaultV2Destination.PASSWORDS) } }
            item { V2DashboardTile("Documents", "Your secure files", Icons.Outlined.Description) { onOpen(VaultV2Destination.DOCUMENTS) } }
            item { V2DashboardTile("Personal Info", "Important details", Icons.Outlined.Person) { onOpen(VaultV2Destination.PERSONAL_INFO) } }
            item { V2DashboardTile("Household", "Invite and manage access", Icons.Outlined.People) { onOpen(VaultV2Destination.HOUSEHOLD) } }
            item { V2DashboardTile("Settings", "Security and recovery", Icons.Outlined.Settings) { onOpen(VaultV2Destination.SETTINGS) } }
        }
        }
    }
}

@Composable
private fun V2DashboardTile(title: String, subtitle: String, icon: ImageVector, onClick: () -> Unit) {
    Card(
        Modifier.fillMaxWidth().height(142.dp).clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, contentDescription = null, tint = Color(0xFF315CDB), modifier = Modifier.size(27.dp))
            Column {
                Text(title, color = Color(0xFF14213D), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Color(0xFF64748B), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun V2ModulePlaceholder(
    title: String,
    description: String,
    icon: ImageVector,
    onBack: () -> Unit
) {
    Column(
        Modifier.fillMaxSize().background(Color(0xFFF4F6FB)).padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF315CDB), modifier = Modifier.size(52.dp))
        Spacer(Modifier.height(16.dp))
        Text(title, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold, color = Color(0xFF14213D))
        Spacer(Modifier.height(8.dp))
        Text(description, color = Color(0xFF64748B), style = MaterialTheme.typography.bodyMedium)
        Spacer(Modifier.height(20.dp))
        TextButton(onClick = onBack) { Text("Back to Dashboard") }
    }
}
