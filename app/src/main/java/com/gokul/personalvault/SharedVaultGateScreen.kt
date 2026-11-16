package com.gokul.personalvault

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import android.widget.Toast
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.gokul.personalvault.ui.screens.MainAppScreen
import java.security.SecureRandom
import java.util.Date
import java.util.Locale

@Composable
fun SharedVaultGateScreen(email: String, onSignOut: () -> Unit) {
    val auth = remember { FirebaseAuth.getInstance() }
    val db = remember { FirebaseFirestore.getInstance() }
    val user = auth.currentUser
    val clipboard = LocalClipboardManager.current
    val context = LocalContext.current
    val uid = user?.uid.orEmpty()
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var vaultId by remember { mutableStateOf<String?>(null) }
    var ownerUid by remember { mutableStateOf("") }
    var members by remember { mutableStateOf<List<String>>(emptyList()) }
    var inviteEmail by remember { mutableStateOf("") }
    var inviteCode by remember { mutableStateOf("") }
    var joinCode by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var openVault by remember { mutableStateOf(false) }
    var refreshKey by remember { mutableIntStateOf(0) }

    fun refresh() {
        if (uid.isBlank()) { loading = false; message = "Please sign in again."; return }
        loading = true
        db.collection("sharedVaults").whereArrayContains("memberUids", uid).limit(1).get()
            .addOnSuccessListener { result ->
                val doc = result.documents.firstOrNull()
                vaultId = doc?.id
                ownerUid = doc?.getString("ownerUid").orEmpty()
                members = (doc?.get("memberUids") as? List<*>)?.filterIsInstance<String>().orEmpty()
                loading = false
            }
            .addOnFailureListener { e -> loading = false; message = "Unable to check shared vault access: ${e.localizedMessage}" }
    }
    LaunchedEffect(uid, refreshKey) { refresh() }

    val id = vaultId
    if (id != null && openVault && HouseholdCrypto.isUnlocked(id)) {
        MainAppScreen(email = email, householdId = id, currentUid = uid, ownerUid = ownerUid,
            memberUids = members, onSignOut = onSignOut)
        return
    }

    if (loading) {
        Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center) {
            CircularProgressIndicator(); Spacer(Modifier.height(12.dp)); Text("Checking vault access…")
        }
        return
    }

    val isOwner = id != null && ownerUid == uid
    Column(Modifier.fillMaxSize().padding(22.dp), verticalArrangement = Arrangement.Center) {
        Text("Personal Vault", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(4.dp)); Text("Signed in as $email")
        Spacer(Modifier.height(16.dp))

        if (id == null) {
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("Create a shared vault", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Text("Create the vault once. Then generate a one-time code for the second account.")
                    Spacer(Modifier.height(12.dp))
                    Button(enabled = !busy && user?.isEmailVerified == true, onClick = {
                        busy = true; message = ""
                        db.collection("sharedVaults").document(uid).set(mapOf(
                            "ownerUid" to uid, "memberUids" to listOf(uid),
                            "createdAt" to FieldValue.serverTimestamp()
                        )).addOnSuccessListener { busy = false; refreshKey++ }
                            .addOnFailureListener { e -> busy = false; message = e.localizedMessage ?: "Could not create vault." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Create vault") }
                }
            }
            Spacer(Modifier.height(14.dp))
            Card(shape = RoundedCornerShape(18.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("Join a shared vault", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp)); Text("Enter the one-time code shared by the vault owner.")
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(value = joinCode, onValueChange = { joinCode = it.filter(Char::isLetterOrDigit).uppercase(Locale.ROOT).take(20) },
                        label = { Text("20-character pairing code") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                    Spacer(Modifier.height(10.dp))
                    Button(enabled = !busy, onClick = {
                        val code = joinCode.trim().uppercase(Locale.ROOT)
                        val cleanEmail = user?.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
                        if (code.length != 20) { message = "Enter the complete pairing code."; return@Button }
                        if (user?.isEmailVerified != true) { message = "Verify your email before joining."; return@Button }
                        busy = true; message = ""
                        val codeRef = db.collection("vaultPairingCodes").document(code)
                        db.runTransaction { tx ->
                            val invite = tx.get(codeRef)
                            val targetEmail = invite.getString("inviteeEmail").orEmpty()
                            val expiry = invite.getTimestamp("expiresAt")
                            val usedBy = invite.getString("usedBy")
                            val targetVaultId = invite.getString("vaultId") ?: error("Pairing code is invalid.")
                            if (targetEmail != cleanEmail) error("This code was issued to a different email address.")
                            if (usedBy != null) error("This pairing code has already been used.")
                            if (expiry == null || expiry.toDate().before(Date())) error("This pairing code has expired.")
                            val vaultRef = db.collection("sharedVaults").document(targetVaultId)
                            tx.update(vaultRef, mapOf("memberUids" to FieldValue.arrayUnion(uid), "lastJoinCode" to code))
                            tx.update(codeRef, "usedBy", uid)
                            null
                        }.addOnSuccessListener { busy = false; joinCode = ""; message = "You joined the shared vault."; refreshKey++ }
                            .addOnFailureListener { e -> busy = false; message = e.localizedMessage ?: "Unable to join vault." }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Join vault") }
                }
            }
        } else {
            Text(if (isOwner) "You are the vault owner" else "You have joined the shared vault", style = MaterialTheme.typography.titleMedium)
            Text("${members.size} of 2 accounts connected")
            Spacer(Modifier.height(12.dp))
            if (isOwner && members.size < 2) {
                OutlinedTextField(value = inviteEmail, onValueChange = { inviteEmail = it },
                    label = { Text("Second account email address") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Spacer(Modifier.height(8.dp))
                Button(enabled = !busy, onClick = {
                    val target = inviteEmail.trim().lowercase(Locale.ROOT)
                    val own = user?.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
                    if (!Patterns.EMAIL_ADDRESS.matcher(target).matches() || target == own) { message = "Enter a valid different email address."; return@Button }
                    busy = true; message = ""
                    val code = newPairingCode()
                    db.collection("vaultPairingCodes").document(code).set(mapOf(
                        "vaultId" to id, "createdBy" to uid, "inviteeEmail" to target,
                        "expiresAt" to Timestamp(Date(System.currentTimeMillis() + 7L * 24 * 60 * 60 * 1000)),
                        "usedBy" to null, "createdAt" to FieldValue.serverTimestamp()
                    )).addOnSuccessListener { inviteCode = code; busy = false; message = "Pairing code created. It expires in 7 days." }
                        .addOnFailureListener { e -> busy = false; message = e.localizedMessage ?: "Could not create pairing code." }
                }, modifier = Modifier.fillMaxWidth()) { Text("Generate pairing code") }
                if (inviteCode.isNotBlank()) {
                    Text("One-time code: $inviteCode", style = MaterialTheme.typography.titleMedium)
                    TextButton(onClick = {
                        clipboard.setText(AnnotatedString(inviteCode))
                        Toast.makeText(context, "Pairing code copied", Toast.LENGTH_SHORT).show()
                    }) { Text("Copy code") }
                }
            }
            Spacer(Modifier.height(12.dp))
            Card {
                Column(Modifier.padding(16.dp)) {
                    Text("Encryption setup", style = MaterialTheme.typography.titleMedium)
                    Text("Register each account's device. After both are registered, the owner creates and shares the vault key once.")
                    Spacer(Modifier.height(8.dp))
                    OutlinedButton(enabled = !busy, onClick = {
                        busy = true
                        SharedVaultKeyExchange.registerThisDevice(db, id, uid) { _, result -> busy = false; message = result }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Register this device") }
                    Button(enabled = !busy, onClick = {
                        busy = true
                        SharedVaultKeyExchange.unlockForThisMember(db, id, uid) { ok, result ->
                            busy = false; message = result
                            if (ok) openVault = true
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Unlock shared vault") }
                    if (isOwner) Button(enabled = !busy, onClick = {
                        busy = true
                        SharedVaultKeyExchange.createAndDistributeVaultKey(db, id, uid) { ok, result ->
                            busy = false; message = result
                            if (ok) openVault = true
                        }
                    }, modifier = Modifier.fillMaxWidth()) { Text("Create and share vault key") }
                }
            }
            if (HouseholdCrypto.isUnlocked(id)) {
                Spacer(Modifier.height(12.dp))
                Button(onClick = { openVault = true }, modifier = Modifier.fillMaxWidth()) { Text("Open Personal Vault") }
            }
        }
        if (busy) { Spacer(Modifier.height(12.dp)); CircularProgressIndicator() }
        if (message.isNotBlank()) { Spacer(Modifier.height(10.dp)); Text(message, color = MaterialTheme.colorScheme.primary) }
        Spacer(Modifier.height(14.dp))
        TextButton(onClick = onSignOut, modifier = Modifier.fillMaxWidth()) { Text("Sign out") }
    }
}

private fun newPairingCode(): String {
    val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
    val random = SecureRandom()
    return buildString { repeat(20) { append(chars[random.nextInt(chars.length)]) } }
}
