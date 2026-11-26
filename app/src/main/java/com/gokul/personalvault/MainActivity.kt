
package com.gokul.personalvault

import android.os.Bundle
import android.os.Build
import android.view.Window
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.google.firebase.auth.FirebaseAuth
import com.gokul.personalvault.ui.theme.VaultTheme
import com.gokul.personalvault.ui.screens.VaultDashboardNavigationV2
import com.gokul.personalvault.ui.screens.VaultV2Destination
import androidx.compose.runtime.key
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.gokul.personalvault.data.firebase.VaultRepositoryV2
import com.gokul.personalvault.data.local.VaultKeySessionV2
import com.gokul.personalvault.ui.screens.MasterPasswordV2Screen

// Tracks external system activities that temporarily cover the vault.
object VaultExternalActivityState {
    @Volatile var documentPickerActive: Boolean = false
    @Volatile var documentViewerActive: Boolean = false
}

@Composable
private fun VaultSystemBars(window: Window) {
    val darkTheme = isSystemInDarkTheme()
    val view = LocalView.current

    SideEffect {
        // Android 15+ enforces edge-to-edge for target SDK 35+. Keep the system bars
        // transparent so the app's theme surface is visible behind both inset areas.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT

        WindowCompat.getInsetsController(window, view).apply {
            isAppearanceLightStatusBars = !darkTheme
            isAppearanceLightNavigationBars = !darkTheme
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.navigationBarDividerColor = android.graphics.Color.TRANSPARENT
        }
    }
}

class MainActivity : ComponentActivity() {
    override fun onDestroy() {
        if (isFinishing) {
            DocumentUploadForegroundService.cancelAndStop(this)
        }
        super.onDestroy()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            VaultTheme {
                VaultSystemBars(window)
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    FirebaseLoginScreen()
                }
            }
        }
    }
}

@Composable
fun FirebaseLoginScreen() {
    val auth = remember { FirebaseAuth.getInstance() }

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isRegisterMode by remember { mutableStateOf(false) }
    var showPassword by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf("") }

    var signedInEmail by remember {
        mutableStateOf(
            auth.currentUser
                ?.takeIf { it.isEmailVerified }
                ?.email
        )
    }

    LaunchedEffect(Unit) {
        val user = auth.currentUser

        if (user != null && !user.isEmailVerified) {
            auth.signOut()
        } else {
            signedInEmail = user?.email
        }
    }

    if (signedInEmail != null) {
        val currentUid = auth.currentUser?.uid
        if (currentUid == null) {
            auth.signOut()
            signedInEmail = null
            return
        }

        key(currentUid) {
            var requestedUnlockDestination by remember {
                mutableStateOf<VaultV2Destination?>(null)
            }
            var cancelVaultUnlock by remember { mutableStateOf<(() -> Unit)?>(null) }
            var vaultId by remember { mutableStateOf<String?>(null) }
            var vaultLoading by remember { mutableStateOf(true) }
            var vaultError by remember { mutableStateOf("") }
            var sessionUnlocked by remember {
                mutableStateOf(VaultKeySessionV2.isUnlocked(vaultId.orEmpty(), currentUid))
            }

            
LaunchedEffect(currentUid) {
    vaultLoading = true
    vaultError = ""

    // First, ensure this account's personal V2 vault exists.
    VaultRepositoryV2.createOrLoadOwnerVault { personalResult ->
        personalResult.fold(
            onSuccess = {
                // Then resolve the actual active vault.
                // An approved household member will receive the owner's vault.
                VaultRepositoryV2.getMyActiveVault { activeResult ->
                    activeResult.fold(
                        onSuccess = { vault ->
                            vaultId = vault.vaultId
                            sessionUnlocked = VaultKeySessionV2.isUnlocked(
                                vault.vaultId,
                                currentUid
                            )
                            vaultLoading = false
                        },
                        onFailure = { failure ->
                            vaultError = failure.localizedMessage
                                ?: "Unable to resolve your active V2 vault."
                            vaultLoading = false
                        }
                    )
                }
            },
            onFailure = { failure ->
                vaultError = failure.localizedMessage
                    ?: "Unable to prepare your personal V2 vault."
                vaultLoading = false
            }
        )
    }
}


            val lifecycleOwner = LocalLifecycleOwner.current
            DisposableEffect(lifecycleOwner, currentUid) {
                val observer = LifecycleEventObserver { _, event ->
                    if (event == Lifecycle.Event.ON_RESUME) {
                        VaultExternalActivityState.documentViewerActive = false
                    }
                    if (event == Lifecycle.Event.ON_STOP &&
                        !VaultExternalActivityState.documentPickerActive &&
                        !VaultExternalActivityState.documentViewerActive
                    ) {
                        // Keep the vault unlocked while an external document picker or viewer is temporarily open.
                        VaultKeySessionV2.clear()
                        HouseholdCrypto.clearSession()
                        sessionUnlocked = false
                        requestedUnlockDestination = null
                        cancelVaultUnlock = null
                    }
                }
                lifecycleOwner.lifecycle.addObserver(observer)
                onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
            }

            // Keep the dashboard composed underneath the master-password screen.
            // This preserves its selected tab (for example, PASSWORDS) while unlocking.
            Box(modifier = Modifier.fillMaxSize()) {
                VaultDashboardNavigationV2(
                    email = signedInEmail.orEmpty(),
                    vaultId = vaultId.orEmpty(),
                    memberUid = currentUid,
                    isVaultUnlocked = sessionUnlocked,
                    onRequestVaultUnlock = { destination, cancelUnlock ->
                        cancelVaultUnlock = cancelUnlock
                        if (vaultId == null) {
                            vaultError = "Your V2 vault is not ready yet. Please retry after the connection completes."
                        } else {
                            requestedUnlockDestination = destination
                        }
                    },
                    onSignOut = {
                        VaultKeySessionV2.clear()
                        VaultCrypto.clearSession()
                        HouseholdCrypto.clearSession()
                        auth.signOut()
                        signedInEmail = null
                        email = ""
                        password = ""
                        message = ""
                    }
                )

                if (requestedUnlockDestination != null && vaultId != null && !sessionUnlocked) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background
                    ) {
                        MasterPasswordV2Screen(
                            vaultId = vaultId!!,
                            memberUid = currentUid,
                            onUnlocked = { keyBytes ->
                                VaultKeySessionV2.install(vaultId!!, currentUid, keyBytes)
                                keyBytes.fill(0)
                                sessionUnlocked = true
                                // Do not clear the selected destination. The dashboard
                                // underneath will now display the requested protected module.
                                requestedUnlockDestination = null
                            },
                            onBack = {
                                requestedUnlockDestination = null
                                cancelVaultUnlock?.invoke()
                                cancelVaultUnlock = null
                            }
                        )
                    }
                }
            }

            if (vaultLoading || vaultError.isNotBlank()) {
                AlertDialog(
                    onDismissRequest = { if (!vaultLoading) vaultError = "" },
                    title = { Text(if (vaultLoading) "Preparing your vault" else "V2 vault status") },
                    text = {
                        if (vaultLoading) {
                            Column {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(12.dp))
                                Text("Checking your secure V2 vault...")
                            }
                        } else Text(vaultError)
                    },
                    confirmButton = {
                        TextButton(onClick = { vaultError = "" }) { Text("OK") }
                    }
                )
            }
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Premium navy header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.onSurface,
                    RoundedCornerShape(
                        bottomStart = 34.dp,
                        bottomEnd = 34.dp
                    )
                )
                .padding(
                    start = 26.dp,
                    end = 26.dp,
                    top = 38.dp,
                    bottom = 42.dp
                )
        ) {
            Box(
                modifier = Modifier
                    .size(58.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "PV",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleLarge
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "PERSONAL VAULT",
                color = Color(0xFFB9C9E2),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Your private world.",
                color = Color.White,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "One secure place.",
                color = Color(0xFF9EBBFF),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Passwords, documents, notes and finances — organised in one place.",
                color = Color(0xFFCBD5E1),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        // Login card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(top = 24.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(
                defaultElevation = 3.dp
            )
        ) {
            Column(
                modifier = Modifier.padding(22.dp)
            ) {
                Text(
                    text = if (isRegisterMode) {
                        "Create your account"
                    } else {
                        "Welcome back"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = if (isRegisterMode) {
                        "Set up your personal vault account."
                    } else {
                        "Sign in to access your secure vault."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = email,
                    onValueChange = { email = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Email address") },
                    placeholder = { Text("Enter your email") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Password") },
                    placeholder = { Text("Enter your password") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    visualTransformation = if (showPassword) {
                        VisualTransformation.None
                    } else {
                        PasswordVisualTransformation()
                    },
                    trailingIcon = {
                        TextButton(
                            onClick = { showPassword = !showPassword }
                        ) {
                            Text(if (showPassword) "Hide" else "Show")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (!isRegisterMode && !isLoading) {
                    TextButton(
                        onClick = {
                            val cleanEmail = email.trim()

                            if (cleanEmail.isBlank()) {
                                message = "Enter your email address first."
                            } else {
                                isLoading = true
                                message = ""

                                auth.sendPasswordResetEmail(cleanEmail)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        message = if (task.isSuccessful) {
                                            "If the account is eligible, a reset email has been sent."
                                        } else {
                                            "Unable to send reset email. Please try again."
                                        }
                                    }
                            }
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text("Forgot password?")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        val cleanEmail = email.trim()

                        if (cleanEmail.isBlank() || password.isBlank()) {
                            message = "Please enter your email and password."
                        } else if (isRegisterMode && password.length < 8) {
                            message = "Password must contain at least 8 characters."
                        } else {
                            isLoading = true
                            message = ""

                            if (isRegisterMode) {
                                auth.createUserWithEmailAndPassword(
                                    cleanEmail,
                                    password
                                ).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser

                                        if (user == null) {
                                            isLoading = false
                                            message = "Account created, but user session was unavailable."
                                            return@addOnCompleteListener
                                        }

                                        user.sendEmailVerification()
                                            .addOnCompleteListener { verifyTask ->
                                                auth.signOut()
                                                isLoading = false
                                                password = ""

                                                message = if (verifyTask.isSuccessful) {
                                                    "Account created! Verify your email, then sign in."
                                                } else {
                                                    "Account created, but verification email could not be sent. Please try again."
                                                }
                                            }
                                    } else {
                                        isLoading = false
                                        message = task.exception?.localizedMessage
                                            ?: "Account creation failed."
                                    }
                                }
                            } else {
                                auth.signInWithEmailAndPassword(
                                    cleanEmail,
                                    password
                                ).addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val user = auth.currentUser

                                        if (user?.isEmailVerified == true) {
                                            signedInEmail = user.email
                                            message = ""
                                        } else if (user != null) {
                                            user.sendEmailVerification()
                                                .addOnCompleteListener { verifyTask ->
                                                    auth.signOut()
                                                    isLoading = false
                                                    password = ""

                                                    message = if (verifyTask.isSuccessful) {
                                                        "Please verify your email. We sent another verification link."
                                                    } else {
                                                        "Your email is not verified. Check your inbox and try again."
                                                    }
                                                }
                                            return@addOnCompleteListener
                                        } else {
                                            message = "Unable to identify your account."
                                        }
                                    } else {
                                        message = task.exception?.localizedMessage
                                            ?: "Sign-in failed. Check your details."
                                    }

                                    isLoading = false
                                }
                            }
                        }
                    },
                    enabled = !isLoading,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(
                            text = if (isRegisterMode) {
                                "Create Account"
                            } else {
                                "Sign In"
                            },
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(
                    onClick = {
                        isRegisterMode = !isRegisterMode
                        password = ""
                        message = ""
                    },
                    enabled = !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isRegisterMode) {
                            "Already have an account? Sign in"
                        } else {
                            "New to Personal Vault? Create account"
                        },
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (message.isNotBlank()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "YOUR PRIVACY  •  YOUR CONTROL",
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
    }
}